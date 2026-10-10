---
feature: outbox-messaging
status: delivered
updated: 2026-10-10
branch: feature/outbox-messaging
commits: 3710a430..d8b69ffe
---

# 数据库 Outbox 替代 Artemis MQ

## Report

**What was built** — 事件传输从 Artemis/JMS 迁移为基于共享 PostgreSQL 的 transactional outbox：`V320RecreateOutboxEvents` 重建 `outbox_events`，`OutboxEventPublisher` 与业务同事务 INSERT（失败回滚业务），`OutboxPoller` 专用单线程按 `FOR UPDATE SKIP LOCKED` 认领并分发到 `OutboxDestinationHandler`（四业务队列经 `DomainEventOutboxHandler`→`EventProcessor`，通知落库/WS 广播/支付超时各为 handler），指数退避重试 8 次后 `DEAD` + 健康指标 DOWN 告警，广播按 subscribers 扇出行、延迟投递用未来 `next_run_at`，PROCESSING 超 5 分钟可见性复位；Artemis 全量移除，PRD v0.32 同步。

**修订交付（本轮，用户指令「outbox 对临时消息归档避免慢查询」+ 三决策：时间分区 / DETACH 归档 / 6 个月一区）** — `outbox_events` 经 `V342PartitionOutboxEvents` 重建为按半年 RANGE 分区表（分区键 `created_at`，PK 改 `(id, created_at)`，迁移 rename→DO 动态建半区→拷贝回流保留存量数据，三索引建在父表自动下发）。原「DONE 保留 7 天分批删除」（`deleteDoneBefore`/`doneRetentionDays`）整体移除，改为 `OutboxPartitionScheduler` 每日 04:40 单事务滚动维护：事务级 `pg_try_advisory_xact_lock` 串行化 admin/client 双实例 → 补建当前+下一半区 → 分区结束满 30 天（`aqua.outbox.partition-retention-days=30`）者同事务 `DETACH` + 改名 `outbox_events_archive_YYYYHn` 归档（数据不丢、主表恒小、可 SQL 手查）；DEAD 行随分区进归档表，置 DEAD 时告警仍即时。`OutboxPartitionRules` 纯函数（半区边界/命名/超期判定）对齐日志分区 `LogPartitionRules` 惯例。文档同步 PRD v0.34、agents/architecture.md、messaging README。

**Verification** — `./gradlew check` → PASS / BUILD SUCCESSFUL（exit 0，3m05s，2026-10-10）：**2590 个测试 0 失败**（含 Testcontainers PG17 集成测试与服务启动时全量 Liquibase 迁移，V342 在 fresh DB 实跑），全模块 koverVerify + ktlint PASS。outbox 专项 32 测试全绿（新增：超期半区 DETACH 归档且数据保留在归档表、重复执行幂等、缺失下一半区自动补建、advisory lock 被持有时跳过；移除：deleteDoneBefore 测试；规则纯函数 4 例含边界等值）。独立 reviewer 结论 **VERDICT: PASS**——T9–T13 验收全部 MET、无 CRITICAL/MAJOR；reviewer 另在 `postgres:17-alpine` 手工模拟 V320→播种 4 行（含两个半区边界）→V342 逐字执行验证非空迁移（4/4 行保留、relkind=p、PK/索引/分区覆盖 2025h1–2027h1、下一半区可写、越界 INSERT 正确拒绝）。4 条 MINOR：spec 分区数措辞与 IF NOT EXISTS 措辞已改为与实现一致；已知测试缺口——非空数据迁移无自动化测试、缺并发 SKIP LOCKED 双事务竞争测试、缺业务回滚→outbox 回滚的 Spring 事务级集成测试。

**Journey log**
- 仓库 `.gitignore` 显式排除 `docs/`（从未跟踪）——spec 文档按项目覆盖规则保留在工作区、不随分支提交；其要点已沉淀进 PRD v0.32/v0.33/v0.34 与 `agents/`/messaging README。
- 实现期曾在主检出误删文件（shell 默认目录是主检出而非 worktree）——所有 shell 操作必须显式传 `workdir` 到 `.worktrees/outbox-messaging`；主检出常有用户未提交的无关改动，不得触碰。
- **PG 未加引号标识符折叠为小写**：`CREATE TABLE outbox_events_2026H2` 实存为 `outbox_events_2026h2`，曾导致「存在性检查(大写)≠实际(小写)→重复 CREATE 报 relation exists」；`OutboxPartitionRules` 因此直接产出小写名与 PG 存储一致，凡与 `pg_class.relname` 比对的名字都要小写。
- 认领 SQL 从设计的 `UPDATE...RETURNING` 改为同事务 `SELECT FOR UPDATE SKIP LOCKED` + DSL UPDATE（避开 Exposed exec 对 RETURNING 的驱动行为不确定性）；分区 DDL 不能参数化，边界字面量用 `yyyy-MM-dd HH:mm:ss` 格式化（与 `LogPartitionRepositoryImpl` 同口径）。
- 非空数据迁移（T9）无自动化测试，由 reviewer 手工容器验证兜底；后续若 V3xx 迁移再涉及存量数据转换，值得补 seed-then-migrate 集成测试。

## [S1] Problem

AquaRush 的领域事件、通知落库、WebSocket 推送与支付超时延迟触发全部依赖 ActiveMQ Artemis（独立 broker 进程 + JMS）。单体应用为此背上外部 MQ 依赖：部署要额外拉起 `artemis/` broker、集成测试要启动嵌入式 broker、消息发送有流控阻塞业务请求的历史问题（`ArtemisEventPublisher` 有界发送即为此存在）。目标：改为基于共享 PostgreSQL 的 transactional outbox 方案，彻底移除 Artemis/JMS 依赖，保留既有语义（点对点队列、广播推送、延迟投递、失败重投、消费幂等）。

## [S2] Design

### 拓扑与范围（已确认决策）

- 保持两个可部署服务（aqua-admin :9090、aqua-client :8080），共享同一 Postgres 的 outbox 表；**不合并服务**。
- **彻底移除 Artemis**：代码、Gradle 依赖、application.yml 的 `spring.artemis`/`spring.jms`、`broker.xml`、仓库内 `artemis/` standalone 目录全部删除。
- 消费失败重试耗尽 → `DEAD` 状态保留 payload/错误信息 + 健康指标告警；**不建 redrive API**。
- 不改 GraphQL schema；业务行为（事件语义、支付超时兜底、幂等键）不变，仅替换传输层。

### 修订决策：按半年区 RANGE 分区 + DETACH 归档（2026-10-10 用户确认）

- `outbox_events` 改为 PostgreSQL 声明式 **RANGE 分区表**（分区键 `created_at`，**每 6 个月一个分区**——用户确认「数据量不大，6 个月分一个区」），从根源上避免临时消息堆积导致的慢查询。
- 超期分区 **DETACH 归档**（不 DROP）：从主表卸载并改名为归档表 `outbox_events_archive_<半区>`，数据不丢、主表恒小、历史可 SQL 手查。
- 卸载规则 **分区结束满 30 天**（`partition-retention-days=30`，分区上界 ≤ `now - 30d` 才卸载）；半年粒度下主表挂载 **2–3 个分区**（当前+下一半区恒在，当期前 30 天还挂着前一区）、实际滞留约 30 天–7 个月数据（数据量小，可接受；保留期不做成配置）。
- **DEAD 行随分区归档**（替代原「DEAD 永久留在主表」）：DEAD 不再有特殊清理路径，告警仍在置 DEAD 时即时触发（健康指标 DOWN），排查走归档表；主表健康统计只反映保留窗口内的 DEAD。
- 原「DONE 保留 7 天分批删除」（`deleteDoneBefore` + `done-retention-days`）**整体移除**，由分区滚动维护取代。

### 表结构：重建 `outbox_events`（Liquibase V320）

V100 创建的 `outbox_events` 从未被读写（Artemis 切换时被搁置），新 changeset `V320RecreateOutboxEvents`（注册进 `DbChangelogMaster`，置于 V310 之后）DROP 后重建：

| 列 | 类型 | 说明 |
|---|---|---|
| `id` | bigint PK | Snowflake（`DefaultIdGenerator`），同时作为 `DomainEvent.id` |
| `event_type` | varchar(100) NOT NULL | `DomainEventType.value` |
| `destination` | varchar(50) NOT NULL | 目标队列/主题名（沿用 `EventQueueConst` 7 队列 + `broadcast-events`） |
| `subscriber` | varchar(20) NULL | NULL = 竞争消费（任一服务认领）；非空 = 指定服务独享（广播扇出行，值 `admin`/`client`） |
| `payload` | text/jsonb NOT NULL | 目标 wire JSON（见「载荷」） |
| `next_run_at` | timestamp NOT NULL | 可认领时刻；延迟投递写未来时间（复用既有列名与索引语义） |
| `status` | varchar(20) NOT NULL | `PENDING` / `PROCESSING` / `DONE` / `DEAD` |
| `retry_count` | int NOT NULL DEFAULT 0 | 已失败次数 |
| `error_message` | text | 最近一次失败原因 |
| `locked_by` / `locked_at` | varchar / timestamp | PROCESSING 归属与时刻（可见性超时恢复用） |
| `created_at` / `updated_at` | timestamp NOT NULL | 审计 |

索引：`(status, next_run_at)`、`destination`、`(subscriber, status)`。Exposed 表映射放 `aqua-foundation` `common/domain/table/`（遵守规则 2）。

**分区化修订（Liquibase V342PartitionOutboxEvents，置于 V341 之后）**：

- 表改为 `PARTITION BY RANGE (created_at)`；**主键改为 `(id, created_at)`**（分区表 PK 必须含分区键；Snowflake id 仍全局唯一）。三索引在父表创建，自动下发到既有与未来分区（子分区索引名由 PG 自动编号）。
- 半年分区命名 `outbox_events_YYYYH1` / `outbox_events_YYYYH2`（如 `outbox_events_2026H2`）：H1 = `[YYYY-01-01, YYYY-07-01)`，H2 = `[YYYY-07-01, 次年-01-01)`；归档表同名加前缀 `outbox_events_archive_YYYYHn`。**PG 未加引号标识符折叠为小写**——实际关系名为 `outbox_events_2026h2`，`OutboxPartitionRules` 直接产出小写名与 PG 存储一致（避免大小写比对缺陷，集成测试已验证）。
- 迁移**保留现有数据**（在途 PENDING 不可丢）：rename 旧表与其索引/pkey 释放对象名 → 建分区父表 + 动态 PL/pgSQL DO 块创建覆盖 `min(created_at)` 所在半区起至「当前半区 + 下一半区」的分区 → `INSERT ... SELECT` 拷贝 → drop 旧表 → 三索引以原名建在父表上。Liquibase `sql` 需 `splitStatements=false` 包裹 DO 块。
- 重建后主表不设 DEFAULT 分区（避免掩盖缺分区问题；缺分区时 INSERT 显式报错，由滚动维护兜底）。

### 发布路径

- `OutboxEventPublisher implements EventPublisher`（替换 `ArtemisEventPublisher`）：向 `outbox_events` **同步 INSERT**，与调用方业务事务同库同事务（`SimplifiedEventPublishService.publishDomainEvent` 已有 `@Transactional`，REQUIRED 加入调用方事务）。
- **失败语义升级**：INSERT 失败向上传播、回滚业务事务（经典 outbox 原子性）。与旧的 catch→false 不同——旧语义存在的原因是 broker 独立故障域（流控/磁盘满）；outbox 与业务同库，INSERT 失败意味着事务本身已不可用，吞掉异常只会造成「业务成功但事件丢失」。payload 序列化在 INSERT 之前完成。
- `publishDelayed(event, deliveryTimeMillis)` → `next_run_at = deliveryTimeMillis` 对应时刻，取代 Artemis `_AMQ_SCHED_DELIVERY`。
- 广播（`PushEvent`）：`NotificationPushEventPublisher` 改为向 outbox 写 **N 行**（每个 `aqua.outbox.subscribers`（默认 `[admin, client]`）一行，`subscriber` 分别赋值），每服务只认领自己的行。
- `EventQueueConst` 队列名与 `getDestinationForEventType` 路由表原样保留（迁移为发布侧共用路由函数）。
- `EventTypePublisherType.ARTEMIS` → `OUTBOX`；`getSystemStatus()["strategy"]` → `"outbox"`。

### 载荷（payload wire 格式）

按 destination 保持既有 listener 的反序列化契约，dispatcher 原样投递字符串：

- `order/payment/delivery/user/payment-expiry/notifications-events` → `DomainEvent` JSON（含 id/aggregateId/eventType/payload 内层 JSON）
- `broadcast-events` → `PushEvent` JSON（String 载荷，与现 `NotificationWsPushConsumer.onPushEvent(String)` 一致）

### 轮询分发（替代 @JmsListener）

`OutboxPoller`（foundation，`@ConditionalOnProperty aqua.outbox.enabled`）：

1. **认领**：每周期在同一事务内执行
   `SELECT id, ... FROM outbox_events WHERE status='PENDING' AND next_run_at <= now() AND destination IN (:myDestinations) AND (subscriber IS NULL OR subscriber = :instance) ORDER BY next_run_at, id LIMIT :batch FOR UPDATE SKIP LOCKED`，
   再用 DSL 将选中 id 置为 `PROCESSING`（记录 locked_by/locked_at）后提交释放行锁。
   点对点行（subscriber NULL）被任一服务抢占一次；广播行按 subscriber 隔离。`instance` 取自 `aqua.outbox.instance`（缺省回退 `aqua.mode`，admin/client）。
2. **分发**：按 destination 查 handler 注册表（`Map<String, OutboxDestinationHandler>`，接口 `val destinations: Set<String>` + `fun handle(record: OutboxEventRecord)`），**顺序处理**本批（单行状态落库异常不中断整批）：
   - 4 个业务队列 → 复用现有 `EventProcessor`（`DomainEventType` → 处理器列表）分发逻辑（自 `MessageConsumer` 迁出）
   - `notifications-events` → `NotificationMessageConsumer`（改为 handler）
   - `payment-expiry-events` → `PaymentExpiryCheckConsumer`（保留 `@AdminService`，且仅 admin 服务的 `aqua.outbox.destinations` 包含该队列）
   - `broadcast-events` → `NotificationWsPushConsumer`（best-effort 语义不变：异常仅记日志，直接置 DONE，不重试）
3. **结果**：成功 → `DONE`；失败 → `retry_count+1`，未达 `max-attempts`（默认 8）→ 回 `PENDING` 且 `next_run_at = now + 指数退避(initial 1s ×2, cap 60s)`；达到 → `DEAD` + `error_message`。异常即失败（传播前先落状态），不再依赖 JMS 重投。
4. **崩溃恢复**：每周期顺带将 `PROCESSING` 且 `locked_at < now - visibility-timeout`（默认 5min）的行复位为 `PENDING`（进程被杀后行不卡死）。
5. **线程模型**：poller 用**专用单线程调度器**（私有 daemon `ScheduledExecutorService`，线程名 `aqua-outbox-poller`），不与现有 `@Scheduled` 单线程池共用，避免被长任务（快照/扫描）饿死。
6. **分区滚动维护（替代原 DONE 删除）**：`OutboxPartitionScheduler`（`OutboxRetentionScheduler` 改名换职）每日 04:40 单事务执行：
   - **建未来分区**：确保「当前半区 + 下一半区」存在——同事务内先查 `pg_inherits` 确认未挂载再 `CREATE TABLE ... PARTITION OF`（advisory lock 保护下幂等；刻意不用字面 `IF NOT EXISTS`：同名非分区表会让维护显式失败回滚而非静默跳过，次日重试。命名由日期确定性推导）。
   - **超期 DETACH 归档**：从 `pg_inherits` 取仍挂载的分区，凡上界 ≤ `now - partition-retention-days`（默认 30，即分区结束满 30 天）者，在**同一事务内** `DETACH PARTITION` + `RENAME TO outbox_events_archive_YYYYHn`（原子，不留半改名状态）。
   - **双服务并发安全**：事务内先取 `pg_advisory_xact_lock`（固定 key）串行化 admin/client 两实例的同刻执行；整个任务 try/catch 记日志不中断调度。
   - 归档表继承原分区的索引与数据，无应用侧查询；主表挂载分区数稳定在 2–3 个（当期前 30 天含前一区，之后恒为当前+下一半区）。

配置（`aqua.outbox.*`）：`enabled`、`destinations`（每服务订阅列表；admin = 6 个有生产者的队列含 payment-expiry + broadcast，client = 除 payment-expiry 外 + broadcast；`common-events` 无生产者/消费者故不订阅）、`subscribers`（广播扇出名单，默认 `[admin, client]`）、`poll-interval-ms=300`、`batch-size=10`、`max-attempts=8`、退避三参数、`visibility-timeout-ms=300000`、**`partition-retention-days=30`**（分区卸载保留期；**取代已删除的 `done-retention-days`**，分区粒度固定为半年、不做成配置）。`aqua.messaging.enabled` 继续作为发布侧总开关（语义不变）。

### 健康指标（DEAD 告警）

`MessagingHealthIndicator` 改查 outbox 统计：`pendingDue / processing / dead / oldestPendingAgeMs`；**`dead > 0` → `Health.DOWN`**（告警出口），否则 UP 且带 backlog 明细。

### 保持不变

消费端幂等（`claimExpired` CAS、`cancelAtomically`、通知幂等键）、支付超时兜底扫描 `PaymentExpiryScheduler`、事件类型/队列路由、`PaymentExpiryCheckConsumer` 仅 admin、WS 推送双向达能力（用户会话在 client、送水工会话在 admin）、GraphQL schema、`aqua.messaging.enabled` 开关。

### Artemis 移除清单

- Gradle：`gradle/libs.versions.toml`（artemis 版本 + `spring-boot-starter-artemis`/`artemis-jakarta-server` + `messaging` bundle）、`aqua-foundation`/`aqua-admin` 的 `libs.bundles.messaging`、`aqua-trade` 的 artemis starter、`aqua-platform` 的 `spring-jms`/`jakarta.jms-api`
- 资源：两服务 `application.yml` 的 `spring.artemis`/`spring.jms`、`services/aqua-admin/src/main/resources/broker.xml`、仓库根 `artemis/` 目录
- 代码：`ArtemisConfig`、`ArtemisEventPublisher`、`MessageConsumer`（JMS 部分，分发逻辑迁出后删除）、`RetryableMessageProcessor` + `MessagingProperties`（仅被自身测试引用的死代码）、`NotificationAutoConfiguration` 中 JMS 容器工厂、各消费类 `@JmsListener`、对应测试
- 文档：见 T8

### 测试边界

- 单测（foundation）：`OutboxEventPublisher`（路由/延迟/序列化/异常传播）、认领互斥（两 instance 不重复认领）、退避与 DEAD、可见性超时复位、广播扇出 N 行、分区名/边界与卸载候选计算（纯函数）。
- 集成测试（Testcontainers PG 17）：建表 DDL 对齐分区版 changelog（父表 + 前一/当期/下一半区分区）；分区维护——构造「结束已满 30 天」的旧半区，证明其被 DETACH 且改名归档表、行在归档表可查、主表不再含该行、重复执行幂等；原 `deleteDoneBefore` 测试删除（方法移除）。
- 单测（platform/trade）：各 destination handler 委托正确；`PaymentExpiryCheckConsumer` 失败上抛语义改写。
- 集成测试：不再启动嵌入式 Artemis；Testcontainers PG 不变；`application-test.yml` 本就未开 messaging，发布 Bean 缺省行为不变。
- 全量验证：`./gradlew check`（ktlint + tests + Kover 门槛）。

## [S3] Out of Scope

- 合并 aqua-admin / aqua-client 为单一进程
- DEAD 事件的 redrive 查询/重试 API（后续任务）
- PostgreSQL LISTEN/NOTIFY 等低延迟推送优化（轮询 300ms 足够）
- 改变任何消费端业务语义、幂等键、GraphQL schema、前端
- 消息顺序保证的增强（现状 Artemis 并发消费本就无全局序）
- 归档表 `outbox_events_archive_*` 的查询 API 或自动清理（无应用查询需求，清理由 DBA 按需自理）
- `pg_partman` 等分区管理扩展、按周/天再分片、其它业务表分区化
- LISTEN/NOTIFY、claim SQL 加 `created_at` 分区裁剪等性能微优化（挂载分区仅 4–6 个，现有索引足够）

## Tasks

- [x] T1: 重建 outbox 表 + Exposed 映射 — acceptance: `V320RecreateOutboxEvents` 注册进 `DbChangelogMaster`，Testcontainers 启动后表/索引存在，foundation 有对应 `common/domain/table/` 映射（covers: S2 表结构）
- [x] T2: OutboxEventPublisher 替换发布路径 — acceptance: 单测证明事件按既有路由写入正确 destination、`publishDelayed` 写未来 `next_run_at`、INSERT 异常传播（业务回滚语义）、`strategy=outbox`；`SimplifiedEventPublishServiceTest` 通过（covers: S2 发布路径）
- [x] T3: OutboxPoller 认领与分发核心 — acceptance: 单测/集成测试证明 SKIP LOCKED 单次认领、到期前不认领、成功置 DONE、失败指数退避、达上限置 DEAD、PROCESSING 超可见性复位、专用调度线程（covers: S2 轮询分发；depends: T1）
- [x] T4: 消费端迁移 — acceptance: 仓库内不再有 `@JmsListener`；4 业务队列经 `EventProcessor` 分发、`NotificationMessageConsumer`/`NotificationWsPushConsumer`/`PaymentExpiryCheckConsumer` 注册为 destination handler 且各有委托测试；`NotificationPushEventPublisher` 按 subscribers 扇出多行（covers: S2 轮询分发+广播；depends: T3）
- [x] T5: Artemis 全量移除 — acceptance: 源码/构建脚本/yml 中无 artemis、JMS、broker.xml 引用，`artemis/` 目录删除，死代码（RetryableMessageProcessor/MessagingProperties）清理，`./gradlew compileKotlin` 通过（covers: S2 Artemis 移除清单；depends: T4）
- [x] T6: 健康指标与配置收口 — acceptance: 单测证明 dead>0 → DOWN、UP 含 backlog 明细；两服务 application.yml 含 `aqua.outbox.*` 且 client 不订阅 payment-expiry（covers: S2 健康指标+配置）
- [x] T7: 全量验证 — acceptance: `./gradlew check` 全绿（或仅 PRE-EXISTING 失败并记录）（covers: S2 测试边界）
- [x] T8: 文档同步 — acceptance: AGENTS.md、agents/architecture.md、agents/messaging-cache-wechat.md、messaging README、PRD（00/04/13/14）中 Artemis 机制描述更新为 outbox 且行为陈述不变，无残留「embedded Artemis」过时断言（covers: S1/S2）
- [x] T9: V342 分区迁移 — acceptance: `V342PartitionOutboxEvents` 注册进 `DbChangelogMaster`；Testcontainers 上既有数据迁移后行数不变、父表为 `PARTITION BY RANGE (created_at)`、PK 为 `(id, created_at)`、三索引存在、半区分区覆盖 `min(created_at)` 至「当前+下一半区」（covers: S2 分区化修订；depends: T8）
- [x] T10: 分区滚动维护替换 DONE 删除 — acceptance: `OutboxPartitionScheduler` 每日任务自动补建「当前+下一半区」分区、卸载结束满 30 天的分区并改名 `outbox_events_archive_YYYYHn`（同事务原子）、advisory lock 串行化；`deleteDoneBefore`/`doneRetentionDays` 全仓无残留；PG 集成测试证明过期分区行进归档表且重复执行幂等（covers: S2 修订决策+分区滚动维护；depends: T9）
- [x] T11: 测试与 DDL 对齐 — acceptance: outbox PG 测试 DDL 为分区版、新增分区维护集成测试与边界纯函数单测、移除 DONE 删除测试，相关模块测试全绿（covers: S2 测试边界；depends: T10）
- [x] T12: 配置与文档同步 — acceptance: `OutboxProperties` 改 `partition-retention-days=30`；messaging README、agents/architecture.md、agents/messaging-cache-wechat.md、PRD 中「DONE 7 天删除」表述改为「按半年区分区 + DETACH 归档（结束满 30 天卸载）」，DEAD 归档口径一致（covers: S2 配置+修订决策；depends: T10）
- [x] T13: 全量验证 — acceptance: `./gradlew check` 全绿（或仅 PRE-EXISTING 失败并记录）（covers: S2 测试边界；depends: T11, T12）
