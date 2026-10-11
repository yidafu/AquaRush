# AquaRush 实现技术文档 · 总览与架构

> 本文档基于 `spec/prd/` v0.35（2026-10-11）编写，**只以 PRD 为需求来源推导实现方案，不引用任何现有代码**。
> 业务规则与验收用例以 PRD 为准（本文以 `F-域-序号` / `R-域-序号` / `AC-…` / `G-序号` 编号引用，不复制全文）；本文只管「怎么实现」。
> 覆盖范围为 PRD 目标形态：`[已实现]`、`[部分实现]`、`[规划中]` 一并纳入设计；`[暂缓]` 条目（如企业客户 F-ORD-027，2026-10-10 决策；周期订购 F-ORD-026 已于 2026-10-11 恢复为后台侧实现、用户端未开发）保留设计章节并标注状态、**暂缓项不进路线图批次**；PRD 登记的开放问题在本文中以 **【开放决策】** 标注并给出建议默认值。

## 文档导航

| 分册 | 内容 | 对应 PRD 域 |
| --- | --- | --- |
| [01-data-model.md](01-data-model.md) | 数据库全量设计（60+ 表、字段/索引/约束/状态机、分区与归档） | 14-misc §核心实体 + 各域实体 |
| [02-api-design.md](02-api-design.md) | GraphQL 双端 Schema、REST 端点、WebSocket 协议、权限接线 | 各域功能清单 |
| [03-core-flows.md](03-core-flows.md) | 16 个核心流程的时序与并发实现（状态机/计价/支付/对账/水票/押金…） | 02-business-flows + 各域 R 规则 |
| [04-frontend-design.md](04-frontend-design.md) | 三端一包前端架构与页面实现要点 | 附录 A 页面清单 |
| [05-security-and-nfr.md](05-security-and-nfr.md) | 认证授权、数据隔离、异常矩阵落地、非功能、部署运维 | 01-overview + 14-misc §NFR/异常 |
| [06-testing-and-roadmap.md](06-testing-and-roadmap.md) | 测试策略、验收追溯、实施路线图（G 缺口消化批次） | 14-misc §缺口/路线图 |
| [07-payment-reconciliation.md](07-payment-reconciliation.md) | 微信支付对账详细设计（账单下载解析/四类差异匹配/调度幂等/权限） | 04-order §R-PAY-04/F-ORD-031 |
| [outbox-messaging.md](outbox-messaging.md) | Transactional Outbox 特性设计（v0.32 架构决策 + v0.34 分区归档，已落地） | 04-order R-ORD-04 / 13-notification F-NTF-003 |
| [address-management.md](address-management.md) | 地址管理实现参考（表结构/级联/地理编码，实现视角） | 05-address-delivery |

## 1. 系统概述

AquaRush 是微信生态内桶装水**订购 + 配送履约 + 运营管理**一体化系统，面向**单门店、单服务器轻量运营**（多门店 G90、在线客服 IM G92 本期不做）。三端协同：

| 端 | 形态 | 定位 | 主要职责 |
| --- | --- | --- | --- |
| 用户端小程序 | Taro 4 微信小程序（`user-weapp`） | 订购入口 | 浏览/下单/支付/跟踪/评价/营销资产（券·水票·积分·会员） |
| 送水工端 App | React Native + Expo（`delivery-app`，dev build） | 履约工具 | 抢单/被派单、配送（拍照·轨迹·导航·转单）、现场收款、日报提交 |
| 管理后台 | React + Vite + Ant Design Web（`admin-client`） | 运营中枢 | 商品/订单/派单/用户/营销/对账/统计/日志/系统配置 |

业务主链路（跨三端）：

```
用户端: 浏览商品 → 下单支付(微信) → 等待派单 → 跟踪配送 → 送达即完成 → 评价
送水工端: 抢单/被派单 → 开始配送(5s轨迹上报) → 拍照验证+收款方式 → 完成配送 → 提交收款日报
管理后台: 维护商品/营销/配置 → 派单 → 核验照片 → 确认日报 → 按日对账 → 统计分析
```

## 2. 技术选型

| 层 | 选型 | 依据（PRD 约束） |
| --- | --- | --- |
| 后端语言/框架 | Kotlin + Spring Boot 4（Gradle 多项目） | PRD 明示 GraphQL + 方法级 `@PreAuthorize`（R-AUTH-04）、支付超时延迟触发（R-ORD-04，v0.32 起为 Outbox 延迟事件）、WebSocket（R-ORD-10） |
| API 风格 | GraphQL（spring-graphql，schema-first `*.graphqls`）为主；REST 仅用于认证回调/上传/推送通道 | PRD 全域以 query/mutation 表述；`POST /api/pay/notify`、`POST /api/v1/storage/files`、`/ws/notifications`、`/api/logs/user-actions/batch` 为 REST/WS |
| 数据库 | PostgreSQL 16；Liquibase 启动自动迁移 | PRD「PostgreSQL 会话时区」、按月 RANGE 分区（F-SYS-006）、`FOR UPDATE`/乐观锁 CAS 语义 |
| 消息 | **Transactional Outbox**：同事务写 `outbox_events`、提交后 `OutboxPoller` 轮询投递（`FOR UPDATE SKIP LOCKED`）；两服务共库，无外部 MQ | PRD v0.32 架构决策；R-ORD-04 延迟事件（`next_run_at` 到期认领）；F-NTF-003 `broadcast-events` 按订阅扇出到两服务 WS 会话 |
| 缓存 | **MapDB 本地持久缓存（默认）**，Redis 可选扩展：地区字典/地理编码/收藏计数等读多数据 | `spring.cache.type=mapdb`（单门店单服务器定位，PRD 01-overview）；access-token 缓存（F-NTF-001） |
| 实时通道 | WebSocket `/ws/notifications`（连接后首条消息 token 鉴权）+ 30s 轮询降级 | R-ORD-10 / R-NTF-03 |
| 支付 | 微信支付 V3 JSAPI（wechatpay-java `NotificationParser`）+ 开发环境假支付开关 | R-PAY-01、F-ORD-005 |
| 前端 | Taro 4（weapp）/ Expo RN / React+Vite+AntD；共享包 `@aquarush/common`（money/status/tracking） | 01-overview §产品定位；金额展示统一走 money 工具（R-MON-01） |
| 地图 | 管理后台与送水工端：MapLibre + 天地图底图（WGS-84）；用户端：微信内置地图（GCJ-02，边界转换） | F-ADR-012、F-ADR-022、R-ADR-02 坐标体系约定 |
| 测试 | 后端 Kotest 6 + MockK + Testcontainers PostgreSQL（无 Artemis/H2）；前端 jest | 覆盖率门槛见 06 分册 |

## 3. 总体架构

```
                        ┌────────────────────────── 单服务器 ──────────────────────────┐
 微信小程序 ──HTTPS──▶  │  aqua-client (Spring Boot)         aqua-admin (Spring Boot)  │
 送水工App ──HTTPS──▶  │   ├ REST: /api/auth/wechat/login    ├ REST: /api/auth/login   │
 管理后台  ──HTTPS──▶  │   ├      /api/pay/notify(微信回调)  ├ GraphQL /graphql (admin)│
                        │   ├      /api/v1/storage/files     ├ WS /ws/notifications   │
 微信支付/微信订阅消息 ◀─┼─  ├ GraphQL /graphql (client)      └ REST: /logs/**         │
                        │   └ WS /ws/notifications                                    │
                        │        │  Transactional Outbox: 同事务写 outbox_events,      │
                        │        │  提交后 OutboxPoller 轮询投递(SKIP LOCKED,两服务共库) │
                        │        ▼                                                     │
                        │   PostgreSQL 16 (业务库 + outbox_events + 日志月分区 + 冷归档) │
                        │   MapDB 本地持久缓存 (默认; Redis 可选扩展)                   │
                        │   本地磁盘 (配送照片等平台存储文件 + gzip CSV 日志归档)          │
                        └──────────────────────────────────────────────────────────┘
 静态资源: 管理后台由 Nginx 托管; 小程序/独立 App 走各自发布渠道
```

两个可部署服务的职责切分依据（非任意拆分）：

- **aqua-client**：面向公网小程序/送水工端，鉴权模型为 `isAuthenticated()` + 本人数据隔离（R-AUTH-02）；持有微信支付回调、订阅消息发送、code2session 等微信侧密钥；
- **aqua-admin**：面向后台与送水工端管理入口，鉴权模型为 40 权限码 × 3 角色静态矩阵（R-AUTH-04）；**支付超时兜底扫描、每日对账调度、统计快照等后台定时任务只部署在 admin 服务**（避免双实例重复执行；R-ORD-04「仅 admin 处理」）。

## 4. 后端模块划分

Gradle 多项目「共享内核 + 领域模块 + 2 服务」的模块化单体。模块划分直接对应 PRD 功能域：

| 模块 | 职任（对应 PRD 域） | 领域内容 |
| --- | --- | --- |
| `aqua-foundation` | 共享内核（跨域） | 领域模型与 Exposed 表映射集中（`common/domain/model/`、`common/domain/table/`——**除 aqua-ops 自带 6 张外，全仓 Exposed 表映射集中于此**）、跨模块 DTO（`api/dto`）与跨模块服务接口（`api/service`，唯一合法的跨模块数据通道）、GraphQL 公共配置、Spring Security、Transactional Outbox 消息封装（`outbox_events` 发布与轮询投递）、Snowflake ID、平台存储、日志基础设施、异常与错误码 |
| `aqua-user` | 用户与账户域（07）+ 用户侧地址（05） | 微信登录、用户资料、地址簿与地址服务、regions 级联/地理编码、后台账号/送水工管理、账号详情聚合查询 |
| `aqua-product` | 商品域（03） | 商品/规格字典/收藏、商品跨模块查询接口 |
| `aqua-trade` | 订单域（04）+ 收银对账跨模块接口（06） | 订单/明细/操作记录、支付退款、配送任务/片区/轨迹、派单抢单转单、完成配送、评价；号码保护（F-ORD-032，规划中）；周期订购为 **[已实现]**（2026-10-11 恢复，后台侧：调度自动下单 + 提醒送水工）、企业月结为 **[暂缓]**（2026-10-10 决策，保留设计不排期）、工资结算为规划中，落地归本模块 |
| `aqua-marketing` | 营销域（10）+ 水桶费（`barrel/`，原 aqua-deposit 并入） | 客户分组与分组价、优惠券、满减折扣、限时特价、买赠、新人优惠、水票、计价引擎、桶费与押金记录、营销 usage；会员等级与积分（规划中） |
| `aqua-analytics` | 统计与数据域（08）+ 收银对账（06） | 仪表盘与统计子页查询、营销日汇总快照、收款日报与每日对账、微信支付/退款/结算资金对账（`reconciliation/`，每日 02:00 起调度） |
| `aqua-platform` | 平台与消息域（09、13） | 通知与消息（`notification/`：消息网关、订阅消息、WS 会话注册表、提醒调度）、平台存储、公告配置 |
| `aqua-ops` | 运营配置域（12）+ 系统设置域（11） | 轮播图、公告、反馈处理、店铺信息、配送费规则、系统参数中心、数据维护；**自带 6 张表**（banners/notices/store_profiles/delivery_fee_rules/feedbacks/system_settings），其余表映射归 foundation |

> 说明：PRD 将「系统设置（仅超管）」与「运营配置（管理员级）」分域，两者共享 `system_settings` 存储与审计机制，实现上归入同一模块、以不同权限码（`SYSTEM_CONFIG_*` vs `OPS_CONFIG_*`）隔离。
> 原设计的 `aqua-address` / `aqua-notification` 两个独立模块不设：地址并入 `aqua-user`、通知并入 `aqua-platform`（裁决 §2.1，8 模块已由 ArchUnit 约束）。

### 模块依赖铁律

1. 领域模型与 Exposed 表映射**只存在于 `aqua-foundation`** 的 `common/domain/model/`、`common/domain/table/`（唯 aqua-ops 自带 6 张）；领域模块通过仓库访问。
2. 领域模块之间**禁止直接调用对方仓库**；跨模块读写一律走 foundation 的 `api/service/**` 接口（实现类由属主模块提供）。
3. Resolver 层输入 DTO 不内联：服务内输入放 `<admin|client>/<域>/dto/`，跨模块 DTO 放 foundation `api/dto/`。
4. 查询/变更服务命名 `*QueryService` / `*MutationService`；禁止新建 `*ApiService` 后缀。
5. 服务（aqua-admin / aqua-client）只做组装：引入所需领域模块，声明 GraphQL schema 执行器与 Web 安全配置，不含业务逻辑。

## 5. 全局横切约定（实现层红线）

| # | 约定 | 来源 |
| --- | --- | --- |
| 1 | 所有实体主键为 `Long` Snowflake ID（服务启动时 workerId 配置区分两服务，避免碰撞） | 工程约定 |
| 2 | **所有金额 `Long` 分**（`*_cents` 列），禁止浮点；仅展示层 ÷100 转元两位小数；服务端校验金额 = Σ 单价×数量 | R-MON-01 |
| 3 | 统计/账期时区**三处钉死 Asia/Shanghai**：应用入口 `TimeZone.setDefault`、JDBC URL `TimeZone=Asia/Shanghai`、systemd `-Duser.timezone` | 08 域口径 |
| 4 | **快照原则**：成交单价、营销优惠、运费、配送时段、地址（含楼层/坐标）在下单时快照落库；此后任何配置/地址簿变更不追溯已生成订单 | R-MKT-01/02、R-MON-02、F-ORD-024 |
| 5 | **幂等三模式**：唯一索引（下单 `clientRequestId`、日报 worker+date、水票票号、结算单 worker+period）、**状态谓词条件更新（`WHERE id=? AND status=?`，如订单状态机流转/取消认领——现状主实现）与乐观锁 CAS（支付超时 `payments.version` 认领）并列为合法手段**、原子条件更新（水票核销、库存扣减、余额扣减） | R-ORD-04/06、R-MKT-08/11（裁决 §2.9） |
| 6 | 订单状态每次流转**同事务**追加 `order_operations` 并写入 outbox（同事务写 `outbox_events`，提交后由 poller 投递） | R-ORD-01 |
| 7 | 禁止全表查询：所有列表/统计/导出/调度必须分页 + 过滤；导出与批处理用游标分批 | 工程红线（对应 PRD 调度扫描约束） |
| 8 | 敏感字段（手机号等）脱敏后才可进入日志/导出文件；密钥类（appsecret、支付证书、APNs/FCM 凭证）只走环境变量，不进在线配置 | R-SYS-02、NFR |
| 9 | 个人信息数据隔离：client 查询一律以登录主体 id 为过滤条件，前端隐藏不作为隔离手段 | R-AUTH-02 |
| 10 | 每个 `.kt` 文件带 AGPL 版权头（CI 强制） | 工程约定 |

## 6. 领域状态机总览

系统含 8 个显式状态机（唯一事实来源：02-business-flows + 各域文件），实现上均为数据库 `varchar` 枚举列 + 守卫转移（详见 01/03 分册）：

| 状态机 | 状态集 | 定义处 |
| --- | --- | --- |
| 订单（7 态） | 待支付 → 待派单 → 待配送 → 配送中 → 已完成 / 已取消 → 已退款 | 02 §订单状态机 |
| 退款 | 待审核 → 已通过/已拒绝 → 已完成/失败 | R-PAY-02 |
| 收款日报 | 草稿 → 已提交 → 已确认；（超容差）→ 有争议 →（驳回）草稿 | R-COL-01~02 |
| 对账 | 待对账 → 匹配/有差异 → 已复核 | R-COL-03 |
| 用户水票 | 未核销/已退回 →(核销)→ 已核销；→ 已过期 | R-MKT-08 |
| 用户优惠券 | 已领取 → 已核销/已过期/已退回 | R-MKT-03 |
| 押金记录 | 待收取 → 已收取 → 已退还 | R-MON-03 |
| 退押申请 | 待处理 → 已回收 → 已退还/已驳回 | R-PRO-04 |
| 套餐卡 | 未用/部分核销 → 已用完/已过期/已退款 | R-MKT-11 |
| 工资结算单 | 待生成 → 已生成 → 已确认 → 已发放 | R-COL-05 |
| 月结账单 | 待结清 → 部分结清/已结清（**[暂缓]** 随企业客户 F-ORD-027） | R-ORD-14 |
| 转单 | 待接收 → 已接收/已拒绝/已超时 | R-ORD-15 |
| 周期订购计划 | 生效中 → 已暂停/已终止（**[已实现]** 2026-10-11 恢复，后台侧） | R-ORD-13 |
| 公告 | 草稿 → 已发布 → 已停用 | F-OPS-002 |
| 账号 | ACTIVE/DISABLED（停用即拒登 + JWT 失效） | F-USR-020/021 |

## 7. PRD 域 → 模块 → 分册 追溯映射

| PRD 域 | 后端模块 | 主要表 | API 分册章节 | 流程分册章节 |
| --- | --- | --- | --- | --- |
| 03 商品 | aqua-product | products、product_options、product_favorites、deposit_* | §商品 | 库存与押金桶 |
| 04 订单 | aqua-trade | orders、order_items、payments、payment_refunds、subscriptions、enterprise_*（[暂缓]） | §订单 | 状态机/计价/支付/派单/转单/周期/企业（暂缓） |
| 05 地址配送 | aqua-user（地址）/ aqua-trade（片区·轨迹） | addresses、delivery_areas、delivery_trajectories | §地址配送 | 范围校验/轨迹 |
| 06 收银对账 | aqua-analytics | daily_collections、daily_reconciliations、微信对账三表 | §收银对账 | 日报/对账/微信资金对账 |
| 07 用户 | aqua-user | users、admins | §用户 | 三端认证 |
| 08 统计 | aqua-analytics | 聚合查询 + daily_marketing_* 快照 | §统计 | 营销快照任务 |
| 09 日志 | aqua-foundation（`logging/`） | api_logs、user_action_logs、business_logs、log_backup_records | §日志 | 分区归档 |
| 10 营销 | aqua-marketing | 20+ 表（见 01 分册 §营销）+ barrel 桶费 | §营销 | 计价引擎/水票/套餐卡 |
| 11 系统设置 | aqua-ops | system_settings | §系统设置 | 参数生效与审计 |
| 12 运营配置 | aqua-ops | banners、notices、store_profiles、delivery_fee_rules、feedbacks | §运营配置 | 配送费计费 |
| 13 通知 | aqua-platform | notifications、user_subscription_auths | §通知 | 消息网关/WS/提醒调度 |

---

下一册：[01-data-model.md](01-data-model.md) —— 数据库全量设计。
