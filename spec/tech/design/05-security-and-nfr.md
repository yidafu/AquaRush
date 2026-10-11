# 05 · 安全、非功能与运维

## 1. 权限模型落地（R-AUTH-04）

**代码级静态矩阵，不建数据库 RBAC 表**。单一来源两份代码：`AdminPermission`（40 码常量）+ `RolePermissionMapping`（角色→权限集合），与 PRD 01-overview §权限码目录/矩阵逐字对齐，变更需同步 PRD。

### 1.1 权限码 × 角色矩阵（40 码）

- **超级管理员（SUPER_ADMIN）**：全部 40 码；
- **管理员（ADMIN）**：35 码——缺 `ADMIN_USER_READ/WRITE`、`SYSTEM_CONFIG_READ/WRITE`、`NOTIFICATION_WRITE` 共 5 码；
- **送水工（DELIVERY_WORKER）**：8 码——`ORDER_READ / PRODUCT_READ / ADDRESS_READ / ORDER_CREATE(代客下单) / DELIVERY_TASK / RECONCILIATION_READ / RECONCILIATION_SUBMIT(日报) / STATISTICS_READ`；
- 超管额外携带 `ROLE_ADMIN`（兼容 hasRole 表达式）；`admins.role=DELIVERY_WORKER` ↔ `ROLE_DELIVERY_WORKER`。

全量 42 码：PRODUCT_READ/WRITE、ORDER_READ/WRITE/CREATE/ASSIGN/REVIEW、DELIVERY_TASK、DELIVERY_WORKER_READ/WRITE/DELETE/RESET_PASSWORD、PAYMENT_READ/REFUND、USER_READ/WRITE/DELETE/RESET_PASSWORD、ADDRESS_READ/WRITE、ADMIN_USER_READ/WRITE、RECONCILIATION_READ/SUBMIT/CONFIRM/REVIEW/EXECUTE、STATISTICS_READ/EXPORT(预留)、LOG_READ、SYSTEM_CONFIG_READ/WRITE、OPS_CONFIG_READ/WRITE、FEEDBACK_READ/REVIEW、MKT_READ/WRITE、NOTIFICATION_READ/WRITE、SUBSCRIPTION_READ/WRITE（周期订购计划查询/管理，F-ORD-026 后台侧，2026-10-11 新增）。规划新增码（未进代码，随对应功能落地接线）：`PAYMENT_RECONCILIATION_READ/EXECUTE`（微信支付对账，F-ORD-031/G93——落地后与 PRD 01-overview §权限码目录 44 码口径对齐，超管 44/管理员 39；与 `RECONCILIATION_*` 日报对账相互独立）、`ORDER_EXPORT`（订单导出，F-ORD-030）。

### 1.2 强制链路

- `@EnableMethodSecurity` 全局开启；admin `/graphql` 强制 authenticated；每个后台 resolver 方法 `@PreAuthorize("hasAuthority('PERMISSION_<码>')")`，**缺省拒绝**；
- 越权 → FORBIDDEN；匿名 → UNAUTHENTICATED；
- 角色唯一来源 `admins.role`/`users.role`；角色变更重新登录生效；
- 「管理员兼送水」（F-USR-023）：ADMIN/SUPER_ADMIN 持 DELIVERY_TASK/ORDER_CREATE/RECONCILIATION_SUBMIT，登录送水工端与送水工同权，业绩按账号归集。

## 2. 认证与会话

| 端 | 凭证 | 要点 |
| --- | --- | --- |
| 小程序 | JWT（access+refresh） | 静默登录 code2session；openid 首现自动建用户；401 拦截刷新（幂等）；单飞防并发 |
| 后台/送水工 | JWT | username+password（BCrypt）；停用账号拒登且**每请求校验 account_status**（旧 token 立即失效）；登录失败锁定（默认 5 次/锁 30 分钟，可配 F-SYS-004）；Token TTL 可配（重启生效） |
| WebSocket | JWT | **连接后首条消息 `{token, receiverType}` 鉴权**（无握手头；服务端校验 JWT 解析接收人并注册会话，失败关闭会话）；会话注册表管理在线状态 |

## 3. 数据隔离矩阵（R-AUTH-02）

| 数据 | 用户 | 送水工 | 管理员 |
| --- | --- | --- | --- |
| 订单 | 仅本人（user_id 过滤） | 仅名下任务（delivery_worker_id 过滤）；未接单池可见（待派单） | 全部（ORDER_READ） |
| 地址 | 仅本人 | 配送任务地址快照；后台地址 ADDRESS_READ | 全部 |
| 轨迹 | 概略位置（跟踪页） | 本人上报 | 仅 DELIVERY_WORKER_READ |
| 消息/反馈/桶账/券/票/积分 | 仅本人 | 本人任务消息 | 按对应权限码 |
| 统计 | — | 本人工作台（STATISTICS_READ 8 码之一） | STATISTICS_READ 全域 |

前端隐藏不作为隔离手段；服务端一律以登录主体 id 为过滤条件。

## 4. 敏感信息与密钥

- **脱敏**：手机号（导出台账/日志/PRD 文档场景）先脱敏再输出（如 138\*\*\*\*5678）；properties JSON 剔除敏感键；不留存完整手机号于日志；**号码保护**（F-ORD-032/R-ORD-17，规划中 G95）：送水工端联系方式可见性按订单状态服务端裁剪（仅配送阶段下发完整号码，其余阶段连完整号码都不出服务端，非仅前端遮罩）——与导出脱敏同属个人信息最小化口径；
- **密钥**（只走环境变量/配置文件，禁在线配置，R-SYS-02）：微信 appid/appsecret、微信支付 V3 商户号/证书/APIv3Key、天地图 Key、APNs/FCM 凭证（目标）、数据库口令、JWT 签名密钥；`services/aqua-client/.env` 由 bootRun 自动加载（开发），生产经 systemd EnvironmentFile；
- 在线可配仅非敏感参数（模板 ID、开关、阈值、时段等，R-NTF-05）。

## 5. 异常处理 → 统一错误码（14-misc §异常矩阵的 API 落地）

GraphQL `errors[].extensions.code`：`UNAUTHENTICATED / FORBIDDEN / NOT_FOUND / BAD_USER_INPUT / CONFLICT（并发/幂等冲突）/ BUSINESS_RULE（业务规则拒绝）`；REST 同构 ProblemDetail。关键映射（message 为 PRD 原文案）：

| 场景 | code | message |
| --- | --- | --- |
| 库存不足 | BUSINESS_RULE | 「库存不足」+ 具体商品 |
| 重复下单（clientRequestId 命中） | —（返回已存在订单） | 幂等成功路径 |
| 非法状态流转/重复接单/重复派单/重复提交日报/重复生成结算单 | CONFLICT | 「该订单已被处理」/「今日已提交」/「该周期结算单已存在」 |
| 支付回调金额不符/验签失败 | —（HTTP 失败应答） | PAYMENT_FAILED 业务日志 + 告警 |
| 地址不可达 | BUSINESS_RULE | 「该地址暂不支持配送」（校验服务自身异常则**降级放行**） |
| 超信用额度 | BUSINESS_RULE | 「已超出信用额度，请先结清或联系管理员」 |
| 超容差日报提交 | —（成功 + DISPUTED 状态） | 差异说明必填校验 |
| 退还桶数超在租 | BUSINESS_RULE | 「退还桶数不能超过在租押金桶数」 |
| 催单防抖 | BUSINESS_RULE | 「催单过于频繁」 |
| 停用账号登录 | BUSINESS_RULE | 「账号已停用，请联系管理员」 |
| 删除保护 | BUSINESS_RULE | 「不能删除当前登录账号」/「至少保留一个超级管理员」 |
| 停用送水工派单 | BUSINESS_RULE | 「配送员账号已停用，无法分配任务」 |
| 定位权限被拒（端侧） | — | 提示开启定位；上报停止，配送主流程不受影响 |
| 推送断线 | — | 30s 轮询降级 + 重连补发（幂等键去重） |
| 资损类 | 告警（非用户可见） | 退款超额/对账差异（outbox DEAD 消费亦触发健康指标 DOWN） |
| 转单目标不可用 | BUSINESS_RULE | 「目标送水工不在线或不可接单」 |

前端：错误态必须给用户提示（Alert/toast + 重试），禁仅 console（G74 目标）。

## 6. 非功能需求落地

| 类别 | 实现 |
| --- | --- |
| 金额精度 | `Long` 分端到端；服务端校验金额=Σ单价×数量（R-MON-01） |
| 时区 | `TimeZone.setDefault("Asia/Shanghai")`（main）+ JDBC URL `TimeZone=Asia/Shanghai` + systemd `-Duser.timezone=Asia/Shanghai` 三处一致；统计日界按此时区 |
| 幂等 | 三模式全覆盖（唯一索引/状态谓词条件更新与 CAS/原子条件更新），见 01 §12 |
| 性能 | 无 PRD 量化指标（登记待补充）；工程红线：列表分页（建议默认 10-20/页）、调度/导出游标分批、统计走快照表 + 今日实时兜底、活动商品/配置读缓存；单页单 GraphQL 请求 |
| 可用性 | 单服务器轻量部署；Transactional Outbox（同库 `outbox_events`，无外部 MQ 依赖）；MapDB 本地持久缓存默认、Redis 可选扩展；微信侧失败重试 maxRetries + access-token 自动刷新 |
| 可观测 | 三类日志（API/用户操作/业务）+ correlationId 贯穿（业务日志 ↔ 订单时间线对齐）；资损/对账差异告警（触达方式【待补充】建议接企业微信/邮件） |
| 日志留存 | 三表按月 RANGE 分区 + DEFAULT；冷表 `log_archive.{table}_{yyyy}` + gzip CSV + SHA-256；每日 03:00 预建分区、03:30 自动备份（可关） |
| 数据保留 | 站内消息默认 90 天；积分流水只增不改；轨迹保留时长【开放 G65】 |
| 合规 | AGPL 版权头（CI 强制）；个人信息脱敏（用户储值 2026-10-09 决策不做，无预付卡合规项） |

## 7. 部署与运维（单服务器）

```
Nginx (443, 静态 admin-client + 反代 /api,/graphql,/ws)
 ├─ systemd: aqua-admin.service   (Gradle bootRun 产物 jar; -Duser.timezone=Asia/Shanghai)
 ├─ systemd: aqua-client.service  (同上; EnvironmentFile=/etc/aquarush/client.env)
 ├─ PostgreSQL 16 (业务库 + outbox_events; 每日 pg_dump — scripts/backup-db.sh)
 ├─ Transactional Outbox (两服务进程内 OutboxPoller, 无独立组件)
 └─ MapDB 本地持久缓存 (默认; Redis 可选)
文件存储: 本地磁盘目录 (配送照片/反馈图/导入导出文件), 按日期分目录, Nginx 只读托管静态文件路径
```

- 迁移：Liquibase 随服务启动执行（无手动 update 任务）；changelog 只追加；
- 环境变量清单（client）：微信 appid/appsecret、微信支付商户证书/APIv3Key/回调地址、`WECHAT_PAY_MOCK_ENABLED`（开发假支付）、天地图 Key、JWT 密钥、DB；（admin）：DB、JWT 密钥；
- 时区：systemd 单元统一 `-Duser.timezone=Asia/Shanghai`；
- 集成测试堆：Spring + **Testcontainers PostgreSQL**（真实 PG 容器 `postgres:17-alpine`，需 Docker/podman；无 H2/Artemis）；
- 升级顺序：先 DB 迁移随新版本启动（向后兼容 changelog），再滚动重启两服务。

---

下一册：[06-testing-and-roadmap.md](06-testing-and-roadmap.md) —— 测试与路线图。
