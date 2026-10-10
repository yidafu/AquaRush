# 02 · API 设计

> GraphQL 双端 Schema + 少量 REST/WS 端点。命名与场景均来自 PRD 功能清单；权限列标注方法级 `@PreAuthorize` 权限码（后台）或 `isAuthenticated()`（client）。

## 1. 总体形态

| 通道 | 端点 | 服务 | 说明 |
| --- | --- | --- | --- |
| 后台 GraphQL | `POST /graphql`（admin schema） | aqua-admin | 全部后台管理查询/变更，方法级权限码强制（R-AUTH-04）；**送水工履约操作（acceptDelivery/startDelivery/completeDelivery/reportTrajectory/submitDailyCollection 等）亦位于 admin schema**（送水工以账号密码调本端，见 §4.4） |
| 客户 GraphQL | `POST /graphql`（client schema） | aqua-client | 小程序业务操作；`isAuthenticated()` + 本人数据隔离（R-AUTH-02） |
| 认证 REST | `POST /api/auth/wechat/login`、`POST /api/auth/refresh`（仅 client）；`POST /api/auth/login`、`POST /api/auth/logout`（仅 admin） | client / admin | 见 §2。现状 refresh/logout 单侧对称缺失（client 无 logout、admin 无 refresh）——随结构 #17 端点/错误码统一时一并定稿，不在本批改动 |
| 支付回调 | `POST /api/pay/notify` | aqua-client | 微信支付 V3 回调（验签/金额校验/幂等，R-PAY-01） |
| 文件上传 | `POST /api/v1/storage/files` | 两服务 | 平台存储（配送照片/反馈图/商品图/导入文件），返回 fileUrl |
| 行为上报 | `POST /api/logs/user-actions/batch` | 两服务 | 三端 tracking SDK 批量上报（F-LOG-002） |
| 实时通道 | `WS /ws/notifications` | 两服务 | JWT 鉴权；消息推送 + 在线状态（R-ORD-10） |
| 日志 REST | `/api/logs/api`、`/api/logs/user-actions`、`/api/logs/business` | aqua-admin（user-actions 两服务均有） | 保留 REST 形态（F-LOG-001/002；业务日志另有 GraphQL 查询） |

**Schema 组织**：`schema-first`——`*.graphqls` 按「公共类型（foundation）+ 各域扩展」合并为两份可执行 schema；`pnpm codegen` 双向生成 Kotlin 校验类与前端 TS 类型（生成物不手改）。

## 2. 认证与登录态

### 2.1 用户端（微信静默登录，F-USR-001 / R-AUTH-01）

```
POST /api/auth/wechat/login  { code }        // Taro.login 取 code，服务端 code2session
→ { accessToken, refreshToken, userInfo, isNewUser }
```

- 免授权弹窗（不调 `wx.getUserProfile`）；openid 首次出现**自动创建用户**并回填 userInfo；
- 登录态过期：前端拦截器 401 → `POST /api/auth/refresh` 刷新（client 侧现状已实现：weapp `checkAndRefreshToken` 单飞 Promise 防并发刷新风暴）；刷新失败清态降级游客浏览（可浏览商品，需登录操作引导登录后回跳原页面）；
- 静默登录**单飞**（并发仅一个请求，Promise 去重）；401 重试幂等防刷新风暴（R-AUTH-03）。

### 2.2 后台/送水工端（账号密码，F-USR-010/020）

```
POST /api/auth/login     { username, password }   // admins 表，BCrypt 校验
→ { accessToken, adminInfo: { id, role, authorities[] } }
```

- 停用账号（account_status=DISABLED）登录拒绝：「账号已停用，请联系管理员」；停用后**旧 token 立即失效**（JWT 过滤器每请求校验账号状态）；
- 连续失败达 `security.login_fail_lock_count`（默认 5）锁定 `security.login_lock_minutes`（F-SYS-004）；
- 登录成功按角色分流（送水工端）：DELIVERY_WORKER → 任务首页；ADMIN/SUPER_ADMIN → 经营看板（兼送水入口）；
- 登出 `POST /api/auth/logout` 落 ADMIN_LOGOUT 业务日志。

### 2.3 JWT 与权限装配

- Claims：`sub`(账号/用户 id)、`type`(USER/ADMIN)、`role`、`permissions[]`（后台账号在签发时装配 40 码静态矩阵映射结果）；
- 用户端 GraphQL：`isAuthenticated()`；client 侧身份标记 `ROLE_USER`；
- 后台 resolver 每方法 `@PreAuthorize("hasAuthority('PERMISSION_<码>')")`；越权返回 FORBIDDEN、匿名 UNAUTHENTICATED；角色唯一来源 `admins.role` / `users.role`，禁止按手机号等不可信信息推断；角色变更重新登录生效。

## 3. GraphQL 通用规范

| 项 | 规范 |
| --- | --- |
| 分页 | 新式 `*VoPage { content:[T!]!, totalElements:Int!, totalPages:Int!, size:Int!, number:Int! }`（35 处）为统一目标；旧式 `{ list, pageInfo:{…} }`（日志/对账/系统设置等 12 处）随结构 #13 迁移改造。列表必带 `input { page, size, 过滤… }`，禁无分页列表（单页数据如配置项除外） |
| 金额 | 所有金额字段 `Long` 分（`*Cents` 命名）；前端经 `@aquarush/common/utils/money` 转元展示 |
| 时间 | `LocalDateTime` ISO 文本，时区 Asia/Shanghai；前端提交本地格式 `YYYY-MM-DDTHH:mm:ss`（G40 教训，禁 `.toISOString()`） |
| ID | 全部 `Long`（GraphQL ID 序列化为字符串传输） |
| 错误 | GraphQL `errors[].extensions.code` 业务错误码（BAD_USER_INPUT / UNAUTHENTICATED / FORBIDDEN / NOT_FOUND / BUSINESS_RULE / CONFLICT）；关键业务拒绝（库存不足/状态非法/容差超限等）以 BUSINESS_RULE + 中文 message 返回 |
| 单页单请求 | 一个页面渲染 = 一次 GraphQL 请求（含所需全字段）；多级数据用嵌套字段或别名一次性取回（工程规则 #7） |
| 输入 DTO | 服务内输入落 `<admin|client>/<域>/dto/`，跨模块 DTO 落 foundation `api/dto`；resolver 不内联输入对象 |

---

## 4. client schema（小程序）查询与变更总表

> 送水工端的业务操作不在 client schema：**履约操作位于 admin schema**，见 §4.4。
> 用户储值（myBalance/recharge 等 7 个 API）**2026-10-09 决策不做**，不设对应接口。

### 4.1 商品与营销展示（用户端）

| 名称 | 权限 | 说明 |
| --- | --- | --- |
| `activeProducts(page,size,keyword)` | public（未登录回退目录价） | 在售且库存>0；每页 20，createdAt 倒序；返回 `effectivePriceCents/hasGroupPrice/listPriceCents/priceSource`（按登录用户解析分组价/特价/新人价，R-MKT-02 展示层） |
| `product(id)` | public | 详情：图集/规格/水源/库存/销量/详情富文本/证书；价格区同上解析（划线价口径） |
| `favoriteProducts(page,size)` | authenticated | 我的收藏（每页 20） |
| `toggleProductFavorites(productId)` | authenticated | 收藏/取消（幂等切换；配套查询 `isProductFavorited` / `favoritesCount`） |
| `flashSales(page,size)` / `newcomerPrices(productId)` | public | 特价/新人价展示（随商品查询嵌套下发亦可；两查询 schema 现状未落，列表补全列零散清单 A 类） |
| `couponCenter()` / `myCoupons(status)` / `claimCoupon(couponTemplateId)` | authenticated | 领券中心/券包（状态筛选）/领取（限领数校验）；三者现状均**无分页**（返回全量列表） |
| `ticketShop` / `myTickets(status)` / `purchaseTickets(input{ticketTypeId,packageCount})` | authenticated | 水票商城/票包/购票（packageCount 1~10，走支付；三者中 ticketShop/myTickets 现状无分页） |
| `memberCenter` | authenticated | 当前等级/成长进度（当前累计/下一级门槛）/权益列表（G80 排期，schema 现状未落） |
| `pointsCenter` / `pointsLedger(page,size)` / `exchangePoints(itemId)` | authenticated | 积分余额/明细/兑换（幂等；G81 排期，schema 现状未落） |
| `myPackages(page,size)` | authenticated | 套餐卡列表（剩余量/状态；G79 排期 schema 现状未落；用户储值不做——2026-10-09 决策，无余额/充值接口） |
| `barrelAccount` | authenticated | 本人桶账：按规格在租/欠桶/押金合计 + 押金记录分页（G75 随押金回退 B2 排期，schema 现状未落） |
| `submitDepositRefund(spec, count, addr, remark)` | authenticated | 退押申请（≤在租桶数校验；随押金回退 B2 排期） |
| `depositRefundApplications(page,size)` | authenticated | 退押申请记录（仅本人；随押金回退 B2 排期） |

### 4.2 下单与订单（用户端）

| 名称 | 权限 | 说明 |
| --- | --- | --- |
| `orderPreview(input)` | authenticated | 确认订单页**单请求**预览：按 R-MKT 链路服务端计价，返回逐行成交价/划线价、优惠明细（满减/立减/会员折扣/券）、可选券列表、可用水票、押金、配送费（楼层分档 + 距离阶梯加价【R-OPS-03 规划中,G96】）、赠品行、应付合计，并返回起送门槛信息（`minimumOrderCents`/`belowMinimum`，未达时前端禁提交+提示——R-OPS-03 规划中，服务端 createOrder 二次拦截为最终防线）（F-ORD-001） |
| `createOrder(input)` | authenticated | 提交下单。input：items[{productId,quantity}]×1~20、addressId、deliveryTime（时段枚举，缺省 IMMEDIATE）、remark、preferredCouponId?（0=不用券，缺省引擎自动选最优）、ticketRedeemCount?、clientRequestId（幂等）；服务端全量重算并校验金额（含**起送门槛拦截**——商品金额 < `minimum_order_cents` 抛 BUSINESS_RULE「未达起送金额」，自提单豁免，R-OPS-03 规划中；水票单次核销上限校验随 G97）、扣库存、核销券/票、收押金、落快照；创建 `PENDING_PAYMENT`（月结单另见 §4.3，**[暂缓]**） |
| `myOrders(statuses,page,size)` | authenticated | 订单列表：`statuses: [OrderStatus!]` 数组筛选（缺省=全部），前端 Tab 映射为状态数组——全部/待付款/待配送(PENDING_DISPATCH+PENDING_DELIVERY)/配送中/已完成（10/页）；「已取消/已退款」在全部以标签呈现 |
| `order(id)` | authenticated | 详情：步骤条/地址卡/商品卡（划线价）/费用明细/时间线（order_operations）/按状态操作集 |
| `myOrderStatistics` | authenticated | 我的页角标单请求：total/pendingPayment/pendingDispatch/pendingDelivery/delivering/completed/cancelled/refunded（待配送口径=两项之和） |
| `cancelOrder(orderId)` | authenticated | 待支付/待派单/待配送可取消；已支付自动生成退款申请（R-ORD-03） |
| `createOrderPayment(orderId)` | authenticated | 发起微信支付（原设计名 payOrder）：创建 payments（写 expired_at）→ 返回 `OrderPaymentParamsVo`（JSAPI 支付参数 + `requiresSimulation`/`autoCompleted` 假支付标记；`wechat.pay.mock-enabled` 发起即自动完成） |
| `urgeOrder(orderId)` | authenticated | 催单：仅本人配送中订单；服务端 5 分钟防抖；发 ORDER_URGING 事件双端推送（F-ORD-008） |
| `createReview(input{orderId,deliveryWorkerId,rating,comment,isAnonymous})` | authenticated | 一单一评（原设计名 reviewOrder）：1-5 星、comment ≤1000 字（R-EVL-01 的 500 字上限现状放宽至 1000）；提交前以 `orderReviewCheck(orderId)` 查可评状态（hasReviewed/canReview） |
| `reorder(orderId)` | authenticated | 再来一单：校验在售/库存、剔除失效并提示、地址快照回填或默认地址、价格当前重算——返回待下单数据（本地缓存），不直接建单（R-ORD-12；G76 排期，schema 现状未落） |
| `subscriptions(page,size)` / `createSubscription(input)` / `pauseSubscription(id)` / `resumeSubscription(id)` / `terminateSubscription(id)` | authenticated | 周期订购计划管理（R-ORD-13；G77 **[暂缓]** 2026-10-10 决策，schema 现状未落、不排期） |
| `deliverySlots` | authenticated | 配送时段配置下发（读 F-SYS-002：立即配送恒启用 + 上午/下午/晚上按启停与时间窗；未配置回退种子默认） |
| `checkDeliveryArea(addressId)` | authenticated | 范围校验（片区省市区匹配）；**异常降级放行**（R-ADR-02）。缺陷 #3：底层 `isAddressInDeliveryArea` 现状恒放行、查询未落 schema——按裁决改代码接线（B3，PRD G07） |
| `deliveryTracking(orderId)` | authenticated | 配送跟踪：订单状态门控（非配送中拦截「订单尚未开始配送」）；返回送水工姓名/电话/概略位置（latest location）、ETA；**不下发完整轨迹序列**（隐私，仅概略；F-ADR-010 目标形态，schema 现状未落） |
| `myFeedbacks(page,size)` / `submitFeedback(input)` | authenticated | 反馈记录（仅本人）/提交（类型必选、10-500 字、图≤3） |

### 4.3 企业客户下单（用户端，F-ORD-027）

> G83 **[暂缓]**（2026-10-10 产品决策，勿重复立项）：以下两 API 与对应页面 schema 现状未落，**不进当期排期**；重启时建议先做「提醒型」与非月结批量单。

| 名称 | 权限 | 说明 |
| --- | --- | --- |
| `enterpriseOrderPreview(input)` | authenticated（企业标记客户） | 多地址多商品批量预览：按收货地址拆分子订单，逐单计价与校验，返回拆单结果 + 汇总应付；信用额度校验（已用+本单≤上限，超额拦截） |
| `createEnterpriseOrders(input)` | authenticated | 一次提交生成多张子订单（同事务拆单，各子单独立库存校验与状态机；月结单 payment_method=MONTHLY_SETTLEMENT，生成应收账款挂账） |

### 4.4 履约操作（送水工端 → **位于 admin schema**）

> **归口（裁决 §2.5）**：履约操作（`acceptDelivery` / `startDelivery` / `completeDelivery` / `reportTrajectory` / `submitDailyCollection` 等）注册于 **admin schema**（`services/aqua-admin` 的 `order/delivery-worker/reconciliation.graphqls`）。送水工用账号密码登录 admin `POST /api/auth/login`，持 Bearer token 调 admin `POST /graphql`；授权靠权限码——`RolePermissionMapping` 给 DELIVERY_WORKER 角色 8 个码（PRODUCT_READ/ORDER_READ/ORDER_CREATE/DELIVERY_TASK/ADDRESS_READ/RECONCILIATION_READ/RECONCILIATION_SUBMIT/STATISTICS_READ）。下表为送水工端常用操作清单（权限列=admin schema 权限码）。

| 名称 | 权限 | 说明 |
| --- | --- | --- |
| `workerTasks(tab,page,size)` | DELIVERY_TASK | 任务列表三栏：未接单（PENDING_DISPATCH，可抢）/已接单（PENDING_DELIVERY）/配送中（DELIVERING）；实时刷新（WS 推送 + 30s 轮询降级）。现状替代实现：`pendingDeliveryOrders` / `assignedOrders` / `deliveringOrders` 三查询（三栏分页补全列零散清单，A 类） |
| `acceptDelivery(orderId)` | DELIVERY_TASK | 抢单：`待派单→待配送`；并发先到先得（目标为状态条件更新，失败提示「该订单已被处理」;现状实现为读-改-存,并发双抢单缺口见 03 §6） |
| `startDelivery(orderId)` | DELIVERY_TASK | 开始配送：`待配送→配送中`，记 delivery_started_at，启动轨迹上报 |
| `completeDelivery(orderId,photos[],paymentType,remark)` | DELIVERY_TASK | 完成配送：仅本人配送中订单；**photos ≥1 服务端硬校验**；收款方式 `paymentType` 三选一（票核销单未选自动记 WATER_TICKET；自提豁免）；置 completed_at 即时计入营收（R-ORD-07/11）。现状无 `emptyBarrelsRetrieved` 入参——空桶回收登记随押金回退批次（B2 结构 #4）恢复 |
| `createDeliveryOrder(input{items,addressId,isSelfCollect,remark})` | ORDER_CREATE | 代客下单（原设计名 proxyOrder）：多商品 items + 地址 + `isSelfCollect`（自收/已收款，默认 false）；订单 userId=地址所属客户；进 `PENDING_DISPATCH` 免在线支付，默认 CASH；自提单 payment_time=下单时间（R-ORD-09，G51 归属口径） |
| `updateOrderAddress(orderId,input)` | DELIVERY_TASK | 订单地址快照修正（`UpdateOrderAddressInput`：receiver/phone/detail/floor/longitude/latitude 仅传字段生效，空值不覆盖；**不回写地址簿**；非终态订单；配送费不重算；留痕 ADDRESS_UPDATED）（F-ORD-024） |
| `transferOrder(orderId,{toWorkerId,reason})` | DELIVERY_TASK | 发起转单（仅本人待配送/配送中；目标在线可接单；次数上限校验）（R-ORD-15；G84 排期，schema 现状未落） |
| `respondTransfer(transferId,accept)` | DELIVERY_TASK | 目标送水工接收/拒绝；超时调度置 TIMEOUT 回退（随 G84 排期） |
| `reportTrajectory(input{orderId,longitude,latitude,accuracy,reportedAt})` | DELIVERY_TASK | 轨迹上报：服务端校验**本人 + DELIVERING**，否则拒绝（客户端停止该订单上报并丢弃缓存点）（R-ADR-03） |
| `submitDailyCollection(input)` | RECONCILIATION_SUBMIT | 提交日报（三栏 + 差异说明；超容差服务端置 DISPUTED；同人同日唯一）（R-COL-01/02） |
| `myDailyCollections(page,size)` / `myReconciliations(page,size)` | RECONCILIATION_READ | 本人日报/对账记录（10/页；聚合/查询现状以 `myTodayCollection`、`dailyCollections` 等替代，见 §5.4） |
| `dailyReconciliationOrderDetails(reconciliationDate,deliveryWorkerId,page,size)` | RECONCILIATION_READ | 对账下钻：当日已送达订单流水（服务端分页） |
| `workbenchData` | DELIVERY_TASK | 工作台单请求：今日四卡（总订单/已完成/未完成/今日营收）+ 近 7 天双折线 + 快捷入口九宫格（30s 轮询）。现状替代实现：`todayStatistics`+`weekStatistics` 单请求双字段（§5.7），聚合查询补全列零散清单（A 类） |
| `deliveryWorkerHistoryOrders(status,keyword,page,size)` | ORDER_READ | 历史订单（状态 Tab + 关键字，统计卡跳转；原设计名 workerHistoryOrders） |

> **号码保护（F-ORD-032/R-ORD-17，规划中 G95）**：`workerTasks` / `deliveryWorkerHistoryOrders` / 订单详情查询对 DELIVERY_WORKER 主体**服务端按订单状态裁剪收货人电话**——仅 PENDING_DELIVERY/DELIVERING 返回完整号码，其余状态一律返回脱敏串（138\*\*\*\*1234），完整号码不下发（前端不可绕过，AC-ORD-032-05）；ADMIN/SUPER_ADMIN 主体（兼送水）不受限。`tel:` 一键拨号仅在有完整号码阶段渲染。

### 4.5 消息中心（两端）

| 名称 | 权限 | 说明 |
| --- | --- | --- |
| `myNotifications(type,read,page,size)` | authenticated | 双行筛选：类型（用户：全部/订单/系统/提醒…；送水工：全部/任务/系统，`type: NotificationType`）× 已读状态（`read: Boolean`）；分页 + 未读数（`unreadNotificationCount`；送水工侧 `myWorkerNotifications`/`workerUnreadNotificationCount` 位于 admin schema） |
| `markNotificationRead(id)` / `markAllNotificationsRead` | authenticated | 已读标记幂等；全部已读角标清零（送水工侧 `markWorkerNotificationRead`/`markAllWorkerNotificationsRead`） |
| `unsubscribeSettings` / `updateNotificationSettings(settings)` | authenticated | 订阅管理（分类开关；查询/变更 API 现状未落 schema，补全列零散清单 A 类——开关数据本体在 `user_notification_settings`，见 01 §1.6） |

### 4.6 支付与回调（REST）

```
POST /api/pay/notify    微信支付 V3 通知（aqua-client）
```

处理序（R-PAY-01）：`NotificationParser` 验签解密 → 按业务单号（orders / barrel_purchases(BK 前缀) / ticket_purchases(TP 前缀) 的 order_no,即微信侧 out_trade_no;payments 表内无该列、无 target_type,见 01 §4.5）**链式探查匹配**支付单 → 校验回调金额=应付金额 → 幂等（仅 PENDING 流转；重复/已取消跳过）→ 业务入账（订单置待派单 / 购桶入账 / 购票入账）→ 返回成功应答。金额不符/验签失败返回失败并落 PAYMENT_FAILED 业务日志。
购票/购桶共用该回调（均幂等;**无 RECHARGE——用户储值 2026-10-09 决策不做**）。

---

## 5. admin schema 查询与变更总表

### 5.1 商品域

| 名称 | 权限 | 说明 |
| --- | --- | --- |
| `productsPaginated(input)` | PRODUCT_READ | 列表筛选（`ProductSearchInput`：keyword/status/价格/销量/库存区间/标签/排序；库存按阈值档着色数据） |
| `product(id)` | PRODUCT_READ | 详情 |
| `createProduct/updateProduct/deleteProduct` + `updateProductStatus`/`onlineProduct`/`offlineProduct` | PRODUCT_WRITE | 表单分组提交（金额元→分转换服务端兜底校验）；删除限无关联订单；状态三态切换（单状态切换/上下架独立 mutation） |
| `productOptions(type)` / `createProductOption/updateProductOption/deleteProductOption` | PRODUCT_READ / PRODUCT_WRITE | 字典维护（同类型唯一校验） |
| `exportProducts(filter)` | PRODUCT_READ + 导出 | 异步导出（export_tasks，按当前筛选；现状 disabled 占位，G20 P1 排期） |
| `importProducts(fileUrl)` | PRODUCT_WRITE | 批量导入：模板下载→上传→校验（必填/格式/价格精度/重复）→预览（行级原因、失败行可下载）→确认导入（服务端二次校验）（F-PRO-011；现状 disabled 占位，G20 P1 排期） |
| `depositPrices` / `upsertDepositPrice` / `barrelLedger(filter,page,size)` / `exportBarrelLedger(filter)` | PRODUCT_READ / PRODUCT_WRITE | 押金价目维护、桶账台账（实时聚合：在租/欠桶/押金合计/余额，筛选含「有欠桶」）、导出（脱敏）（F-PRO-012）。现状代码为 G06 一次性购桶费旧形态（`barrelFees`/`barrelPurchases`/`purchaseBarrel` 六 API）——押金回退批次（B2 结构 #4）后对齐本行 |
| `depositRefundApplications(status,page,size)` / `confirmBarrelRecovered(id)` / `approveDepositRefund(id)` / `rejectDepositRefund(id,reason)` | PRODUCT_WRITE | 退押处理流转 |

### 5.2 订单域

| 名称 | 权限 | 说明 |
| --- | --- | --- |
| `orders(input{status,keyword,dateRange,workerId,enterpriseId,page,size})` | ORDER_READ | 列表 + 多选（批量派单/导出范围） |
| `order(id)` / `orderByNo(orderNo)` / `orderOperations(orderId)` | ORDER_READ | 详情：全字段 + 时间线（orderByNo/orderOperations 为 B 类补录：按单号直查与操作时间线独立查询） |
| `updateOrderStatus(orderId,status)` | ORDER_WRITE | 仅允许 待支付/待派单→已取消、已取消→已退款（按流转表校验） |
| `assignDeliveryWorker(orderId,workerId,isSelfCollect)` / `batchAssignOrders(orderIds,workerId)` | ORDER_ASSIGN | 派单/批量派单：worker 须在线且可接单；前置=待派单；目标态=**待配送**（与抢单统一）；同一订单仅一次（状态拦截）；单笔派单必填 `isSelfCollect`（自收标记，批量接口现状无此参数）；派单弹窗按**片区绑定送水工优先推荐**（F-ADR-013） |
| `onlineDeliveryWorkers` | ORDER_ASSIGN | 在线送水工列表（`online_status=ONLINE` 过滤 + SQL 侧上限；原设计名 onlineWorkers） |
| `reviewDeliveryPhoto(orderId,action,remark)` | ORDER_REVIEW | 照片核验：`action=APPROVED/REJECTED` 通过/驳回（驳回 remark 必填，可重复核验纠错，留痕）（F-ORD-021） |
| `exportOrders(filter)` | ORDER_READ + ORDER_EXPORT【规划码】 | 异步导出（条数上限 5 万建议、手机号脱敏）（F-ORD-030） |
| `refundRequests(status,dateFrom,dateTo,page,size)` / `refundRequest(id)` | PAYMENT_READ | 退款申请分页/详情（原设计名 payments/paymentRefunds；退款单模型见 01 §4.6） |
| `processRefund(input)` | PAYMENT_REFUND | 退款审核（原设计名 reviewRefund，input 化）：input{refundRequestId, originalTransactionId, orderId, userId, amountCents, reason, action(APPROVE/REJECT), adminNote?}；APPROVE→执行微信退款（ORDER_REFUND + 押金 DEPOSIT_REFUND 如有）→订单转已退款；REJECT→订单保持已取消；失败可重试（R-PAY-02，拍板 5.1 方案 A） |
| `reconciliationTasks(input{taskType,status,dateFrom,dateTo,page,size})` / `reconciliationTask(taskId)` / `reconciliationReports(taskId,reportType)` / `reconciliationDiscrepancies(input{taskId,status,page,size})` | RECONCILIATION_READ | 微信资金对账任务/报表/差异查询（B 类补录,现状签名;流程见 03 §16） |
| `executeReconciliation(input{taskType,reconciliationDate})` / `resolveDiscrepancy(input{discrepancyId,resolutionNotes})` | RECONCILIATION_EXECUTE / RECONCILIATION_REVIEW | 发起支付/退款/结算对账（T-1/T-1/T-2 错峰,hasRunningTask 防并发）与差异处置留痕。**目标演进**（R-PAY-04,G93——billType/billDate 入参与 `PAYMENT_RECONCILIATION_*` 权限码为规划形态）见 [07-payment-reconciliation.md](07-payment-reconciliation.md) §8 |
| `transfers(input)` | ORDER_READ | 转单记录查看（留痕；随 G84 排期，schema 现状未落） |
| `subscriptions(input)` | ORDER_READ | 周期订购计划查看/管理（G77 **[暂缓]** 2026-10-10 决策，schema 现状未落、不排期） |

### 5.3 用户与账号域

| 名称 | 权限 | 说明 |
| --- | --- | --- |
| `users(input{search,customerGroupId,status,page,size})` | USER_READ | 用户列表（搜索=昵称/手机号；分组筛选） |
| `user(id)` | USER_READ | 详情聚合：基本信息 + 统计卡（全量口径）+ 7 Tab（收货地址/订单/优惠券/水票/反馈/评价/收藏） |
| `createUser/updateUser/deleteUser` | USER_WRITE / USER_DELETE | 建用户（manual_ openid 占位）/改（昵称/手机/头像/状态）/软删 |
| `resetUserPassword(id)` | USER_RESET_PASSWORD | schema 现状无此字段（死码，零散清单：补 mutation 或删码，A 类） |
| `admins(keyword,role,page,size)` / `admin(id)` | ADMIN_USER_READ | 账号列表（原设计名 adminAccounts；`role` 过滤=用户类型下拉） |
| `createAdmin/updateAdmin/deleteAdmin/resetAdminPassword` | ADMIN_USER_WRITE | 删除保护：不能删自己/最后一个超管；重置密码留空=123456 |
| `deliveryWorkers(keyword,role,page,size)` / `deliveryWorker(id)` | DELIVERY_WORKER_READ | 送水工列表（含在线/任务数；原设计名 deliveryWorkers(input)） |
| `createDeliveryWorker/updateDeliveryWorker` / `resetDeliveryWorkerPassword` / `deleteDeliveryWorker` | DELIVERY_WORKER_WRITE / _RESET_PASSWORD / _DELETE | 新建即 admins 行（role=DELIVERY_WORKER，username=`delivery_{id}`，默认离线 is_available=true）；停用不可派单/接单 |
| `accountDetail(id)` | ADMIN_USER_READ 或 DELIVERY_WORKER_READ | 统一账号详情：管理员 8 Tab / 送水工 6 Tab（基本信息/权限/安全/配送能力/配送记录/轨迹/每日收款/评价/操作） |
| `reviewsByUser(userId,…)` / `reviewsByDeliveryWorker(workerId,…)` / `deliveryWorkerRatingStatistics(workerId)` | USER_READ / DELIVERY_WORKER_READ | 评价与评分分布（1~5 星，无记录全零） |

### 5.4 收银对账域

| 名称 | 权限 | 说明 |
| --- | --- | --- |
| `dailyCollections(input{workerId,date,status,page,size})` / `dailyCollectionSummary(collectionDate)` / `myTodayCollection` | RECONCILIATION_READ | 日报列表（含系统计算值、差异、差异说明、驳回信息；汇总卡）+ 当日汇总 + 送水工当日自查（后两者为 B 类补录） |
| `confirmDailyCollection(input{recordId,notes})` / `rejectDailyCollection(input{recordId,reason})` | RECONCILIATION_CONFIRM | 确认 / 驳回（必填原因，退回草稿留痕）——input 化 |
| `dailyReconciliations(input{page,size})` | RECONCILIATION_READ | 对账记录列表（原设计名 reconciliations；input 现状仅分页、无日期/状态筛选——筛选补全列零散清单） |
| `executeDailyReconciliation(reconciliationDate)` | RECONCILIATION_EXECUTE | 手动执行对账（重算更新已有数据，清除复核痕迹；调度器每日 03:00 自动对昨日） |
| `reviewReconciliation(input{reconciliationId,reviewNotes})` | RECONCILIATION_REVIEW | 复核完结（已复核不可再复核）——input 化 |
| `dailyReconciliationOrderDetails(reconciliationDate,deliveryWorkerId,page,size)` | RECONCILIATION_READ | 逐单差异明细（服务端分页；送水工侧同名查询见 §4.4） |
| `payrollRules` / `upsertPayrollRule` | RECONCILIATION_READ / CONFIRM | 工资规则配置（MKT/无——归本域；建议权限=RECONCILIATION_CONFIRM，【开放决策】） |
| `generatePayrollStatements(input{period,workerIds})` | RECONCILIATION_CONFIRM | 生成结算单（同周期同人唯一；与日报/订单对账校验，差异挂起告警） |
| `confirmPayrollStatement(id)` / `recordPayrollPaid(id,{method,voucher})` | RECONCILIATION_CONFIRM | 确认 / 登记发放（幂等，已发放不可重复） |
| `payrollStatements(input)` / `exportPayroll(filter)` | RECONCILIATION_READ | 查询 / 台账导出 |

### 5.5 营销域

| 名称 | 权限 | 说明 |
| --- | --- | --- |
| `customerGroups(input)` / `customerGroup(id)` / `createCustomerGroup/updateCustomerGroup/deleteCustomerGroup` / `setUserCustomerGroup(userId,customerGroupId)` | MKT_READ / MKT_WRITE | 分组管理（重名拦截；成员编辑**单用户设置分组**——原设计名 updateGroupMembers(groupId,userIds) 为批量口径，与实现不符已按现状修订；删除回未分组） |
| `groupProductPrices(input)` / `saveGroupProductPrices(input)` | MKT_READ / MKT_WRITE | 分组价：列表查询 + **批量覆盖式保存**（原设计名 groupPrices/upsertGroupPrice/clearGroupPrice 三点式已按现状修订；空价=不设置回退目录价） |
| `couponTemplates(input)` / `couponTemplate(id)` / `createCouponTemplate/updateCouponTemplate/disableCouponTemplate` / `issueCoupons(input):Int` | MKT_READ / MKT_WRITE | 券管理 + 定向发放（原设计名 createCoupon/grantCoupons 系列；发放返回成功张数）；详情页含使用情况（使用订单数/优惠金额/已核销张数）与相关订单分页 |
| `promotionActivities(input)` / `promotionActivity(id)` / `createPromotionActivity/updatePromotionActivity/disablePromotionActivity` | MKT_READ / MKT_WRITE | 满减/折扣（多档位；原设计名 promotions 系列按实现改名） |
| `flashSaleActivities(input)` / `flashSaleActivity(id)` / `createFlashSaleActivity/updateFlashSaleActivity/disableFlashSaleActivity` | MKT_READ / MKT_WRITE | 限时特价（同商品时间窗重叠拦截；原设计名 flashSales 系列） |
| `memberLevels` / `upsertMemberLevel` | MKT_READ / MKT_WRITE | 会员等级与权益（门槛递增唯一；schema 现状未落，G80 排期 A 类） |
| `ticketTypes(input)` / `ticketType(id)` / `createTicketType/updateTicketType/disableTicketType` / `ticketTypeStats(ticketTypeId,input)` | MKT_READ / MKT_WRITE | 票种管理 + 票种统计（销售/核销/退回/过期/当前可用；原设计名 ticketTypeStatistics）；票种表单预留**运营策略**字段（`isLeadGeneration`/`activationOrderCount`/`maxRedeemPerOrder`，R-MKT-08 修订、G97 排期待定，schema 现状未落） |
| `giftActivities(input)` / `giftActivity(id)` / `createGiftActivity/updateGiftActivity/disableGiftActivity` | MKT_READ / MKT_WRITE | 买赠/满赠 |
| `firstOrderActivities(input)` / `firstOrderActivity(id)` / `createFirstOrderActivity/updateFirstOrderActivity/disableFirstOrderActivity` | MKT_READ / MKT_WRITE | 首单立减/新人价（原设计名 newcomerActivities/upsertNewcomerActivity，按实现改名） |
| `userCoupons(userId,status)` / `userTickets(userId,status)` | MKT_READ | 按用户查券包/票包（B 类补录） |
| `couponTemplateUsage` / `promotionActivityUsage` / `flashSaleActivityUsage` / `giftActivityUsage` / `firstOrderActivityUsage`（均 `…(xxxId,input)`） | MKT_READ | 各营销活动**使用情况**查询（usage_count/让利额/订单分页——B 类补录，03 §22 统计口径同源） |
| `packageProducts` / `upsertPackageProduct` | MKT_READ / MKT_WRITE | 套餐票种（储值档位 rechargePlans 不设——储值 2026-10-09 决策不做；schema 现状未落，G79 排期 A 类） |
| `pointsRules`（system settings 透传）/ `pointsExchangeItems` / `upsertExchangeItem` | MKT_READ / MKT_WRITE | 积分配置与兑换品（schema 现状未落，G81 排期 A 类） |
| `marketingStatistics(input)` | STATISTICS_READ | 营销统计单查询（六汇总卡 + 四图 + 明细表，读快照 + 今日兜底） |

### 5.6 地址配送域

| 名称 | 权限 | 说明 |
| --- | --- | --- |
| `searchAllAddresses(keyword,page,size)` / `createAdminAddress` / `updateAdminAddress` / `deleteAdminAddress` / `batchImportAddresses(input:[AddressInput!]!)` | ADDRESS_READ / ADDRESS_WRITE | 地址维护（原设计名 adminAddresses；编辑走独立 update；分页筛选；删除二次确认；导入**直接传地址对象数组**（无文件/模板五步流程），返回 success/failure/total + 行级原因 + isDuplicateAddress 去重） |
| `userAddresses(userId)` | ADDRESS_READ | 按用户查地址（B 类补录，后台用户详情 Tab 数据源） |
| `deliveryZones` / `upsertDeliveryZone` / `enableDeliveryZone` / `zoneRegions` / `zoneWorkers` | OPS_CONFIG_READ / OPS_CONFIG_WRITE【建议，开放决策】 | 片区维护（名称唯一；省市区多选；绑定送水工；schema 现状未落——片区表 `delivery_areas` 已建、CRUD 与范围校验接线随 G07/缺陷 #3，A 类） |
| `deliveryTrajectories(workerId,orderId,startTime,endTime)` / `deliveryWorkerLatestLocation(workerId)` | DELIVERY_WORKER_READ | 轨迹查询（时间范围过滤、现状**不分页**返回序列；实时位置 + 折线回放数据源；仅管理员） |
| `addressHeatmapStats(timeRange)` | ADDRESS_READ | 热力图聚合：按地址订单热度 + 片区维度（订单量/客户数）+ Top 地址 + 汇总（缺坐标地址计数提示；时间范围默认 30 天上限 1 年；schema 现状未落，G86 排期 A 类） |

### 5.7 统计域（全部 STATISTICS_READ）

`dailyStatistics(input)`（仪表盘四卡+双图）、`pendingOrderBreakdown()`（待支付/待派单/待配送三态细分）、`orderStatistics(input)`（营收/退款/净营收/状态分布/客单价分布/高峰时段；现状无 granularity 入参）、`weeklyStatistics`/`monthlyStatistics`（趋势）、`userStatistics()`（六卡+三图+下单分布；现状无 range 入参）、`deliveryWorkerStatistics()`（六卡+排行——原设计名 deliveryWorkerOverview(date)）、`deliveryWorkerRanking(limit)`、`productStatistics(dateRange)`+`productDailySales(input)`（五卡+趋势+双排行+库存预警明细，位于商品模块）、`todayStatistics`/`weekStatistics`（送水工工作台今日卡与周折线，B 类补录，与 §4.4 workbenchData 现状替代实现同源）。
口径约束：营收=已完成按 completed_at；退款=COMPLETED 按 processed_at；环比上期 0 且本期>0 显示「—」。`STATISTICS_EXPORT` 为预留码（暂无导出功能）。

### 5.8 日志域（LOG_READ）

`businessLogs(input{domain,operationType,level,userId,timeRange,view})`——现状**仅 businessLogs 为 GraphQL 查询**；`apiLogs`/`userActionLogs` 仅 REST（GraphQL 化列零散补全 A 类）。表格 20/页、时间线 10/页恒 createdAt 倒序；`domain: BizLogDomain` 现状**五值** ORDER/PAYMENT/DELIVERY/USER/AUTH（映射 loggerName；PRODUCT/MARKETING/OPS/RECONCILIATION 扩值随日志域补全）、operationType 精确匹配 operation 列；correlationId 关联订单时间线。

### 5.9 运营配置与系统设置域

| 名称 | 权限 | 说明 |
| --- | --- | --- |
| `activeBanners` / `banners(input)` / `banner(id)` / `createBanner/updateBanner/deleteBanner` / `reorderBanner(id,direction)` | public / OPS_CONFIG_READ / OPS_CONFIG_WRITE | 用户端只读 activeBanners；后台维护（排序=reorderBanner 上下移，原设计名 moveBanner；时间窗、启停） |
| `activeNotices` / `notices(input)` / `notice(id)` / `createNotice/updateNotice/publishNotice/stopNotice/setNoticePinned/deleteNotice` | public / OPS_CONFIG_* | 公告（发布/置顶/停用闭环；原设计名 disableNotice 现状为 stopNotice，另有独立置顶/删除） |
| `feedbacks(input{type,status,contact,userId,dateRange,page,size})` / `reviewFeedback(id,input{status,handleResult})` | FEEDBACK_READ / FEEDBACK_REVIEW | 反馈处理（待处理→处理中→已处理仅允许前进；处理人/时间留痕；原设计名 processFeedback({status,reply})，字段现状为 handleResult） |
| `storeProfile` / `saveStoreProfile(input)` | OPS_CONFIG_READ / OPS_CONFIG_WRITE | 店铺信息（原设计名 updateStoreProfile）；表单预留**门店经纬度**（longitude/latitude，R-OPS-03 距离计算输入、G96 排期待定，列现状未落） |
| `deliveryFeeRule` / `saveDeliveryFeeRule(input{freeFloorLimit,surchargeCents,tiers})` | OPS_CONFIG_READ / OPS_CONFIG_WRITE | 配送费规则。**R-MON-02 分档已落地（V345,结构 #6 完成）**：input 必填 `tiers: [{fromFloor,toFloor,feeCents}]`（升序/不重叠/起始楼层>免费楼层/末档不封顶,服务层校验）;`surchargeCents` 为 tiers 为空时的兜底单档。**规划扩展**（R-OPS-03,G96 排期待定）：input 增 `minimumOrderCents`（起送门槛,0=不启用）与 `distanceTiers: [{fromKm,toKm,feeCents}]`（距离阶梯,升序/不重叠/末档不封顶）,与楼层分档叠加计价、下单快照 |
| `systemConfigs(group)` / `saveSystemConfigs(input)` / `restoreSystemConfig(settingKey)` / `systemConfigChanges(key,page,size)` | SYSTEM_CONFIG_READ / SYSTEM_CONFIG_WRITE（仅超管） | 参数中心（键类型校验/审计闭环/恢复默认——原设计名 resetSystemConfig 现状为 restoreSystemConfig；审计查询 API 现状未落 schema，列零散补全 A 类） |
| `dataMaintenanceOverview(input)` | SYSTEM_CONFIG_READ | 数据维护单查询：保留策略 + 三类日志分区状态（行数/大小/可备份性）+ 备份记录 |
| `backupLogPartition(input)` | SYSTEM_CONFIG_WRITE | 手动离线备份（不受自动开关影响） |
| `notifications(input{messageType,receiverType,keyword,read,startAt,endAt,page,size})` / `notification(id)` | NOTIFICATION_READ | 后台消息列表（默认近 30 天）+ 详情（原设计名 type/readStatus/dateRange，现状为 messageType/read/startAt/endAt） |
| `resendNotification(id)` | NOTIFICATION_WRITE（仅超管） | 失败订阅消息重发（复用全部门控；站内消息不重推） |

> **接单超时预警（F-NTF-008/R-NTF-07，规划中 G94）**：无新 API——`OrderAcceptTimeoutScanner` 调度（admin）扫描「待派单/待配送」超阈值订单 → `ORDER_ACCEPT_TIMEOUT` / `DELIVERY_START_TIMEOUT` 事件（outbox）→ 通知网关落库 + WS 推送**全体管理员**（幂等键=订单号+事件类型，R-NTF-07）；阈值参数 `order_accept_timeout_minutes`（默认 30）/`delivery_start_timeout_minutes`（默认 60）入 F-SYS-002。

---

## 6. WebSocket 协议（/ws/notifications）

**连接**：客户端连接 `/ws/notifications` 后**首条消息发送 `{ "token": "<JWT>", "receiverType": "USER|WORKER" }` 完成鉴权**（无握手头鉴权，裁决 §2.11 现行唯一通道）；服务端校验 JWT、解析接收人身份并注册会话（receiver_type + id），鉴权失败关闭会话。
**下行消息**（JSON）：

```json
{ "type": "NOTIFICATION", "idempotencyKey": "…", "category": "ORDER|SYSTEM|REMINDER",
  "title": "…", "summary": "…", "relatedType": "ORDER", "relatedNo": "…", "occurredAt": "…" }
```

- 客户端按 `idempotencyKey` 去重（推送与轮询并发同事件仅渲染一次，R-NTF-02）；
- 推送场景（送水工端 6 个，开关 `notification.push.{scene}.enabled`）：新订单可抢单（**广播**全部在线送水工）、被派单、订单取消、用户催单、配送超时、支付超时——后四者为**定向推送**（配送该订单的送水工）；
- 同一用户多端在线以最后连接为准；断线指数退避重连；重连后拉取离线消息（notifications 未读 + 保留期内补发）；
- 在线状态：判定与派单可用性以 `admins.online_status` 字段为准（ONLINE/OFFLINE，送水工端显式切换 + 后台可改；WS 连接事件不改写该字段）；WS 会话注册表仅承担定向/广播推送路由；
- 兜底：客户端 30s 轮询 `myNotifications(未读)`。

## 7. 订阅消息（微信侧，F-NTF-001）

- 授权：确认订单页提交前 `Taro.requestSubscribeMessage`（一次性/长期模板）；已拒绝或达上限不再弹；授权落 `user_subscription_auths`；
- 发送门控（四条件全过才发）：模板启用（配置）+ 用户分类开关 + 授权剩余次数>0 + 真实 openid；
- 发送：access-token 内存缓存（过期自动刷新，失败按 maxRetries 重试）→ `POST /cgi-bin/message/subscribe/send`；结果回写 notifications.channel_status / wechat_msg_id；失败可后台手动重发（NOTIFICATION_WRITE）；
- 场景与模板（6 个，配置键 `notification.template.{scene}.*`）：下单成功 / 配送员接单 / 开始配送 / 送达确认 / 催单 / 支付结果（超时取消复用支付结果模板）；
- 未授权降级站内查看（订单详情/消息中心），不影响业务流程（R-NTF-01）。

## 8. 文件上传（平台存储）

`POST /api/v1/storage/files`（multipart，JWT）：校验类型（jpg/png/webp/xlsx 等）与大小（图片 ≤5MB）；落本地磁盘（单服务器）按日期分目录；返回 `{ fileUrl }`。业务表只存 URL。上传失败抛错不静默降级（G13 教训）。

---

下一册：[03-core-flows.md](03-core-flows.md) —— 核心流程实现。
