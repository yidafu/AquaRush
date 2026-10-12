# 07 · 微信支付对账(R-PAY-04 / F-ORD-031)

> 本文档基于 `spec/prd/` v0.33(2026-10-10)编写,**只以 PRD 为需求来源推导实现方案**;业务规则与验收用例以 PRD 为准(以 `R-PAY-04` / `F-ORD-031` / `AC-ORD-031-*` / `G93` 编号引用,不复制全文),本文只管「怎么实现」。
> 本卷是 `03-core-flows.md §16`「微信资金对账」概要的**详细展开**(数据模型 / 账单下载解析 / 匹配算法 / 调度 / 权限 / 测试);与送水工收款日报对账(R-COL-01~04,`06-collection.md`)相互独立,复用其「重跑幂等、处置闭环」的产品语义但数据与权限互不相通。

## 0. 实现记录与偏差清单(2026-10 G93 收尾)

功能已实现(调度 + 差异处置闭环 + 「支付管理-支付对账」页 `/payments/reconciliation`,权限码 `PAYMENT_RECONCILIATION_READ/EXECUTE` 独立接线)。与本文正文的偏差(保守实现优先,均为已裁决记录):

| # | 正文设计 | 实际实现 | 决策理由 |
| --- | --- | --- | --- |
| 1 | §3 V350 三表(bill_date UK、download_url_hash、三方汇总列) | 复用既有 reconciliation_tasks/_discrepancies/_reports(task_id 任务号、total/matched/unmatched);V347 增列 trigger_type/executed_by/wechat_total_*/local_total_* + SKIPPED/IGNORED 状态值域 | 表已随 V100 建成并有数据形态;UK(bill_type,bill_date) 不加(存量可重复,幂等靠 hasRunningTask 守卫 + 手动重跑);download_url_hash 不适用(网关无哈希校验概念) |
| 2 | §4 wechatpay-java SDK 网关(aqua-trade `WechatBillGateway`,哈希校验) | **已落地(2026-10-12 对账接口核对整改)**:aqua-trade `SdkWechatBillGateway`(wechatpay-java `BillDownloadServiceExtension`:申请→下载→SHA1 校验→CSV 解析)+ analytics 侧直接消费网关 DTO;原 restTemplate 直连/手写签名(V2 形态)与 CSV 按下标解析全部删除 | 2026-10-12 核对确认原实现端点/签名/列解析均不符合官方 APIv3(详见 spec/changelog 审查报告);真实商户凭据下的账单格式联调验证仍为遗留事项 |
| 3 | §5 本地基准专用 Ledger API + 游标分页 | foundation `PaymentQueryService.getPaidByDateRange` 分页(paid_at 窗口 + SUCCESS/REFUNDING/REFUNDED 过滤),服务端逐页拉取 | 语义等价(规则 10 分页达标);挂在既有 PaymentQueryService 上,不新增接口面 |
| 4 | §6 匹配键 out_trade_no + 四类差异枚举 | 匹配键 transactionId(微信支付单号,回调落库);差异枚举 MISSING/MISMATCH/EXTRA × SourceSystem(INTERNAL/WECHAT) | PaymentModel 无 orderNo 冗余;四类语义完整覆盖(短款=MISSING+WECHAT、长款=MISSING+INTERNAL、金额=MISMATCH;状态不一致语义由退款账单短款承载) |
| 5 | §6-2 SKIPPED(未配置商户参数) | 已实现(`WechatBillGateway.isConfigured` = wechat.pay.* 四凭据非空,任务置 SKIPPED + 原因;原 aqua.reconciliation.weChat* V2 时代门槛配置已删除) | 对齐:门槛与真实凭据源(wechat.pay.*)统一 |
| 6 | §6/PRD 差异数>0 任务仍为成功 | 已实现(completeTask 恒 SUCCESS;FAILED 仅执行异常) | 修正原实现「有差异即 FAILED」与 PRD 冲突点 |
| 7 | §7 报表 30 天/已处置差异 180 天清理 | 已实现(03:30 调度,softDelete 分批;UNRESOLVED 永不清理) | 对齐 |
| 8 | §8.1 resolveDiscrepancy 带 action(RESOLVE/IGNORE) | 已实现(input.action + IGNORED 状态) | 对齐 |
| 9 | §10 不新增 outbox 事件;差异告警通知为后续增强(§13-7) | 维持:任务事件(RECONCILIATION_COMPLETED/FAILED/SKIPPED)已发;通知触达不做(receiverType 仅 USER/WORKER,管理员触达需扩通知接收人模型,非低成本) | 与 §13-7 建议默认值一致 |
| 10 | §7 调度(SETTLEMENT 预留) | 实现 10:50 SETTLEMENT 调度(走官方 APIv3 `GET /v3/bill/fundflowbill`,经 WechatBillGateway 过滤「交易类收入」行与支付比对) | 保留既有能力不回退;调度时间按官方约束改为 T+1 10 点后(原 02:xx 凌晨调度必然拿到 STATEMENT_CREATING);资金账单手续费/净额精确对账仍为后续增强 |

## 文档导航

| 节 | 内容 |
| --- | --- |
| §1 | 目标、范围与非范围 |
| §2 | 总体架构与模块落点 |
| §3 | 数据模型(V350AddReconciliationTables) |
| §4 | 微信账单下载与解析(WechatBillGateway) |
| §5 | 本地账实基准数据(aqua-foundation 只读 API) |
| §6 | 对账匹配算法(四类差异 + 三方汇总校验) |
| §7 | 调度与并发(WeChatReconciliationScheduler) |
| §8 | admin GraphQL API 与前端页 |
| §9 | 权限与安全 |
| §10 | 业务日志与领域事件 |
| §11 | 测试策略与覆盖 |
| §12 | 实施批次 |
| §13 | 开放决策汇总 |

## 1. 目标、范围与非范围

### 1.1 解决的问题(G93)

支付/退款链路(R-PAY-01/02)目前**没有与微信侧的账实核对**:

- 回调丢失 / 本地处理异常 → 微信已扣款、本地无成功记录(**短款 / 漏单**),用户已付钱但订单未流转,只能靠客诉发现;
- 本地 `PaymentServiceImpl.completePaymentSuccess` 异常路径或假回调(验签已挡,防御纵深)→ 本地有、微信无(**长款**);
- 回调金额校验(R-PAY-01)挡住了「金额不符放行」,但**事后**无复盘凭据;
- 退款为同步等待(R-PAY-02),`payment_refunds` 卡在非 COMPLETED 无**兜底发现通道**。

### 1.2 范围

| 项 | 内容 |
| --- | --- |
| 账单基准 | 微信支付 V3 交易账单(`bill_type=ALL`,T-1)+ 退款账单(`bill_type=REFUND`,T-1) |
| 本地基准 | `payments`(`paid_at` 落账单日,status ∈ SUCCESS/REFUNDING/REFUNDED)与 `payment_refunds`(COMPLETED,`processed_at` 落账单日) |
| 差异四类 | WECHAT_MISSING(短款)/ LOCAL_MISSING(长款)/ AMOUNT_MISMATCH(金额不一致)/ STATUS_MISMATCH(状态不一致,含退款) |
| 处置闭环 | 差异人工处置(解决/忽略 + 备注必填)→ 留痕;同任务幂等重跑 |
| 入口 | 调度自动 + 后台「支付管理-支付对账」页手动触发/重跑 |

### 1.3 非范围(【开放决策】→ 见 §13)

- **资金账单**(`fundflowbill`)精确对账(手续费/净额逐笔):不做,后续增强——手续费不影响「单边账/金额一致」的资损判定;既有 SETTLEMENT 任务保留但**改走官方 APIv3 `GET /v3/bill/fundflowbill`**、仅取「交易类收入」行与支付比对(差距表 #10,2026-10-12);
- **结算账单**(SETTLEMENT):任务类型**预留枚举不实现**(wechatpay-java 无对应服务,商户平台侧导出,形态不同);
- **差异自动修复**(自动补单/自动改支付状态):不做,一律人工处置留痕——自动修复的误判成本高于人工处置成本,资金操作不允许程序化兜底改状态;
- 退款结果**主动查询**通道(`queryRefund` 轮询):列为 R-PAY-04 后续增强,本期以退款账单对账为兜底。

## 2. 总体架构与模块落点

```
aqua-admin(调度宿主,§21 约定:调度全部部署于 aqua-admin)
  └─ WeChatReconciliationScheduler(aqua-analytics/reconciliation)
       │ 10:30 支付对账(T-1) / 10:40 退款对账(T-1) / 10:50 资金入账对账(T-2) / 03:30 报表清理
       ▼
  ReconciliationService(aqua-analytics/reconciliation)
       │ ① 取微信账单行                ② 取本地账实行              ③ 匹配 + 汇总校验
       ▼                              ▼                          ▼
  WechatBillQueryApi            PaymentLedgerQueryApi        reconciliation_tasks
  (aqua-foundation 接口)       (aqua-foundation 接口)       _reports / _discrepancies
       ▲                              ▲
  ┌────┴────────┐              ┌──────┴──────────┐
  │ aqua-trade  │              │ aqua-trade      │
  │ WechatBill- │              │ PaymentLedger-  │
  │ GatewayImpl │              │ QueryApiImpl    │
  │ (SDK 下载)  │              │ (payments/      │
  └─────────────┘              │  refunds 仓储)  │
                               └─────────────────┘
services/aqua-admin:GraphQL Resolver(PaymentReconciliationQueryResolver / MutationResolver)+ 页面数据出口
frontend/admin-client:「支付管理-支付对账」页(payment-reconciliation)
```

落点依据:

- **对账逻辑在 aqua-analytics/reconciliation**(`03-core-flows.md §16` 既定):对账是「读侧聚合 + 留痕」业务,不参与支付写链路;analytics 已承载统计快照类调度。
- **跨模块数据访问走 aqua-foundation `api/service/**`**(架构不变量 3):analytics 不直接 touch `payments`/`payment_refunds` 表,两个只读接口(§4/§5)定义于 foundation、实现于 aqua-trade(与 `PaymentService` → `PaymentServiceImpl` 既有模式一致)。
- **微信配置/SDK 网关留在 aqua-trade payment 包**:商户号/私钥/APIv3 密钥等配置与 `SdkWechatPayGateway` 同源复用,analytics 不接触微信凭据。
- 数据表迁移走 aqua-admin 的 Kotlin Liquibase(单库共享,`services/aqua-admin/src/main/kotlin/db/changelog/changes/`,当前最新 V341 → 新增 **V350AddReconciliationTables**)。

## 3. 数据模型(V350AddReconciliationTables)

三表与 `03-core-flows.md §16` 概要一致;全部 Snowflake `Long` 主键、金额一律 `Long` 分(R-MON-01)。

### 3.1 reconciliation_tasks(对账任务)

| 列 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | Snowflake |
| bill_type | varchar(20) | PAYMENT / REFUND / SETTLEMENT(预留,不实现) |
| bill_date | date | 账单日(T-1,Asia/Shanghai) |
| status | varchar(20) | PENDING / RUNNING / SUCCESS / FAILED / SKIPPED |
| wechat_bill_count / wechat_order_amount_cents / wechat_refund_amount_cents | int / bigint / bigint | 微信账单汇总行(三方校验之一) |
| parsed_count / parsed_amount_cents | int / bigint | 逐笔解析合计(三方校验之二) |
| local_count / local_amount_cents | int / bigint | 本地合计(三方校验之三) |
| discrepancy_count | int | 本轮差异条数 |
| download_url_hash | varchar(64) | 账单文件哈希值(哈希校验通过才解析;不落 download_url,含签名过期无留存价值) |
| error_message | text | FAILED 原因 |
| executed_by | bigint null | 手动触发的管理员 id;调度为 NULL |
| started_at / finished_at | timestamp | — |
| created_at / updated_at | timestamp | — |

约束:**UK `uk_recon_task_type_date (bill_type, bill_date)`** —— 同类型同日仅一条任务,重跑 = upsert 该行(幂等锚点);`status='RUNNING'` 行为并发守卫(§7)。

### 3.2 reconciliation_reports(报表快照)

| 列 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | Snowflake |
| task_id | bigint | FK → tasks |
| bill_type / bill_date | varchar(20) / date | 冗余便于按日查询与清理 |
| report_data | jsonb | 汇总卡数据 + 逐笔对账结果数组(微信行/本地行/匹配结果)。**隐私最小化:不落用户标识(OpenID),仅业务键** |
| created_at | timestamp | — |

> 逐笔明细落 JSONB 快照而非独立行表:对账明细是**任务产物**(不可变、整体读写、30 天过期),行表 + 索引的代价不划算;差异行才有独立处置生命周期,独立成表(§3.3)。

### 3.3 reconciliation_discrepancies(差异明细)

| 列 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint PK | Snowflake |
| task_id / bill_type / bill_date | bigint / varchar(20) / date | — |
| discrepancy_type | varchar(30) | WECHAT_MISSING / LOCAL_MISSING / AMOUNT_MISMATCH / STATUS_MISMATCH |
| out_trade_no | varchar(64) | 商户订单号(orders.order_no / 购票单 / 套餐单号;本地无时为账单侧值) |
| transaction_id | varchar(64) null | 微信支付单号 |
| refund_no / refund_id | varchar(64) null | 商户退款单号 / 微信退款单号(REFUND 账单用) |
| wechat_amount_cents / local_amount_cents | bigint null | 双方金额(分;缺失侧为 NULL) |
| status | varchar(20) | UNRESOLVED / RESOLVED / IGNORED |
| resolved_by / resolved_at / resolve_note | bigint / timestamp / varchar(500) | 处置留痕(备注必填) |
| created_at / updated_at | timestamp | — |

约束:**UK `uk_recon_disc_task_outtrade (task_id, discrepancy_type, out_trade_no, refund_no)`**;处置语义与 R-COL-03 日报对账「复核留痕」一致。

### 3.4 保留与清理

- reports:随任务滚动保留 **30 天**(03:30 清理,与 §16 概要一致;游标分批 DELETE,禁全表);
- discrepancies:**UNRESOLVED 永不清理**;已处置保留 180 天后随批次清理(【开放决策】§13-5);
- 重跑语义:tasks 行 upsert 覆盖汇总;reports delete-then-insert;discrepancies 先删 UNRESOLVED 再插本轮结果,**已处置(RESOLVED/IGNORED)按业务键 `(discrepancy_type, out_trade_no, refund_no)` 匹配保留**——与 R-COL-03「重跑重算并保留复核痕迹」语义对齐。

## 4. 微信账单下载与解析(WechatBillGateway)

### 4.1 网关接口(aqua-trade `payment/service/WechatBillGateway.kt`)

```
interface WechatBillGateway {
  /** 申请并下载交易账单(V3 `GET /v3/bill/tradebill`,billType=ALL/SUCCESS/REFUND):
   *  返回逐笔明细行 + 汇总行;SHA1 哈希校验失败、汇总与逐笔不一致均抛异常。 */
  fun downloadTradeBill(billDate: LocalDate, billType: WechatBillType = WechatBillType.ALL): WechatTradeBill
  /** 申请并下载资金账单(V3 `GET /v3/bill/fundflowbill`,基本账户,含收入与支出全部流水)。 */
  fun downloadFundFlowBill(billDate: LocalDate): WechatFundFlowBill
  /** 商户参数是否已配置(wechat.pay.* 四凭据非空,与 SdkWechatPayGateway 同源)。未配置时上层置 SKIPPED。 */
  val isConfigured: Boolean
}
```

真实实现 `SdkWechatBillGateway`(wechatpay-java 0.2.17 `BillDownloadServiceExtension`,凭据与 `SdkWechatPayGateway` 同源于 `wechat.pay.*`,独立惰性实例不牵连支付链路):

- `getTradeBill(GetTradeBillRequest().apply { billDate = "yyyy-MM-dd"; billType = ALL/REFUND })` → `DigestBillEntity`(SDK 内置申请 → 下载 URL 拉取 → 解压 → `DigestBillEntity(inputStream, hash)`);
- **先完整读取流(`DigestInputStream` 边读边累积摘要)→ `verifyHash()` 校验 SHA1 → 通过才解析**,失败即抛(不解析未校验账单);
- 不设 `tar_type`:单日账单体量小,明文流直接 UTF-8 读取,规避 GZIP 与哈希口径歧义(官方默认即不压缩);
- 错误语义(官方错误码):`NO_STATEMENT_EXIST` 当日无记录 → 空账单(合法状态);`STATEMENT_CREATING` → 抛异常(调度已排在次日 10 点后);其余(SIGN_ERROR/PARAM_ERROR/FREQUENCY_LIMITED/SYSTEM_ERROR)→ 抛异常置任务 FAILED,**不自动重试**(§7)。

### 4.2 CSV 解析规则

- 结构:可选说明行 + **列头行**(以首列「交易时间」/「记账时间」定位)+ 明细行 + 尾部**汇总表头行**(「总交易单数」/「资金流水总笔数」)+ 汇总值行;每字段前缀 1 个反引号(`),按官方规则反斜杠转义(`\,`、`\\`、`\n` 等),解析时剥离并反转义;
- **列定位按列头行名称映射,不按固定下标**(微信侧列增减/顺序变化容错;ALL/SUCCESS/REFUND 三种列集通用);
- 交易账单用到的列:`商户订单号`、`微信订单号`、`交易状态`、`应结订单金额`(对账比对基准,§6)、`订单金额`、`退款金额`、`手续费`、`交易时间`、`商品名称` 等;退款账单另用:`微信退款单号`、`商户退款单号`、`退款状态`、`退款成功时间`、`退款申请时间`;资金账单用:`记账时间`、`微信支付业务单号`、`资金流水单号`、`业务名称`、`业务类型`、`收支类型`、`收支金额(元)`、`业务凭证号`;
- **隐私最小化**:官方「用户标识」(OpenID)列不解析、不落库(§9);
- **金额为「元」字符串 → 分**:严格校验格式后字符串运算(拆整数/小数部分,小数补足 2 位),**禁止 Double 中转**(R-MON-01);手续费可为负(退款行);
- 汇总交叉校验:逐笔合计(笔数/应结订单总金额/订单总金额/退款总金额/手续费总金额;资金账单为收支笔数与金额)必须等于汇总行,不等 → 抛异常置任务 FAILED(账单完整性可疑,宁可失败重跑不可带病入账);时间格式非法、必填列缺失同样直接失败,不回退当前时间、不静默跳行;
- 解析产物 DTO(`WechatTradeBillRow`/`WechatFundFlowRow` 等)定义在 aqua-trade 网关层,不进 foundation;
- 消费方过滤(ReconciliationService 取数时):支付比对取 `交易状态=SUCCESS` 行(ALL 同时含退款单行,防 transactionId 碰撞;REVOKED/USERPAYING/PAYERROR 按 §6 不参与比对);资金入账比对取 `业务名称=交易 且 业务类型=交易 且 收入` 行(排除手续费支出/退款支出/提现)。

## 5. 本地账实基准数据(aqua-foundation 只读 API)

### 5.1 支付账实行(PaymentLedgerQueryApi)

foundation `api/service/payment/PaymentLedgerQueryApi.kt`(接口)+ DTO `PaymentLedgerRow`;实现 `PaymentLedgerQueryApiImpl` 于 aqua-trade(走既有 `PaymentRepository`,QueryDSL):

```
/** 指定账单日(Asia/Shanghai 自然日)成功支付流水,按 id 游标分页。禁全表。 */
fun pagePaidByBillDate(billDate: LocalDate, cursor: Long, size: Int): List<PaymentLedgerRow>

data class PaymentLedgerRow(
  val paymentId: Long,
  val outTradeNo: String,      // orders.order_no / 购票单号 / 套餐单号(按 payments.target_type 三分支 join)
  val transactionId: String?,  // payments.transaction_id
  val amountCents: Long,       // payments.amount_cents
  val status: PaymentStatus,   // SUCCESS / REFUNDING / REFUNDED
  val paidAt: LocalDateTime,
)
```

- 过滤条件:`paid_at ∈ [billDate 00:00, +1d)` 且 `status ∈ (SUCCESS, REFUNDING, REFUNDED)`(PENDING/FAILED/EXPIRED/CANCELLED 不在账单上);
- `out_trade_no` 不在 `payments` 表(现链路以单号即 `order.orderNo` 下单,`PaymentServiceImpl` 以 `orderNo` 作为 out_trade_no):按 `target_type` JOIN orders / 购票单 / 套餐购买单取号——与回调匹配路径(`findByOrderNo`)同源,保证口径一致;
- 时间锚点用 `paid_at` 而非 `created_at`:微信账单「交易时间」即支付成功时刻。

### 5.2 退款账实行(PaymentRefundLedgerQueryApi)

```
fun pageCompletedByBillDate(billDate: LocalDate, cursor: Long, size: Int): List<PaymentRefundLedgerRow>
// row: refundNo(商户退款单号 payment_refunds.refund_no)、refundId(微信退款单号)、
//      outTradeNo(经 paymentId → payments → 单号回溯)、refundAmountCents、processedAt
```

- 过滤:`processed_at ∈ [billDate 00:00, +1d)` 且 `status = COMPLETED`(REFUND_TYPE 不限:ORDER_REFUND/DEPOSIT_REFUND 均真实走微信退款,都须对上);
- `refund_no` 为发起退款时的 `out_refund_no`(幂等键),与账单「商户退款单号」直配;`refund_id` 为账单「微信退款单号」比对项。

## 6. 对账匹配算法

```
执行(ReconciliationService.executeReconciliation(billType, billDate, executedBy)):
  1) 守卫:UK 行存在且 status=RUNNING → 拒绝(hasRunningTask);billDate ∉ [T-1, T-30] → 拒绝
  2) isConfigured=false → 任务置 SKIPPED(error="未配置真实商户参数(假支付模式)"),返回
  3) 任务行 upsert(PENDING→RUNNING,version CAS 认领)→ 下载解析(§4)→ 本地流式分页(§5)
  4) 匹配(内存 Map,微信行按 out_trade_no 建索引;本地游标分页逐批比对,O(n)):
     支付账单(PAYMENT):
       账单行 out_trade_no 本地缺失                          → WECHAT_MISSING(短款,资损风险最高)
       本地行账单缺失(本地有 paid_at 当日 SUCCESS 账单无)     → LOCAL_MISSING(长款,查跨日/重复入账)
       应结订单金额 ≠ amount_cents                           → AMOUNT_MISMATCH
       账单交易状态 REFUND 而 status=SUCCESS(无退款记录)      → STATUS_MISMATCH
       账单交易状态 SUCCESS 而 status ∈ REFUNDING/REFUNDED    → STATUS_MISMATCH(方向二)
     退款账单(REFUND):
       账单退款行 refund_no 本地缺失                          → WECHAT_MISSING
       本地 COMPLETED 退款账单缺失                            → LOCAL_MISSING
       退款金额 ≠ refund_amount_cents                        → AMOUNT_MISMATCH
       账单退款状态=SUCCESS 而本地非 COMPLETED                 → STATUS_MISMATCH(退款单卡住,R-PAY-02 兜底通道)
  5) 三方汇总校验:微信汇总行 vs 逐笔解析合计 vs 本地合计 → 落 tasks 汇总列
     (账单汇总行 ≠ 逐笔合计 → 直接 FAILED,不落差异,保护基准正确性)
  6) 差异批量落 discrepancies(重跑语义 §3.4)→ reports 落快照 → 任务 SUCCESS
  7) 全程单事务边界:任务行 + 差异 + 报表同事务提交;下载/解析阶段无写库
```

- 匹配键统一 `out_trade_no`(退款账单附 `refund_no`);REVOKED/USERPAYING/PAYERROR 行不参与比对(未成功交易,本地不应有 SUCCESS 记录),仅在 reports 快照中留档;
- 跨日边界(交易日 vs `paid_at` 日分界):两边统一 Asia/Shanghai 自然日(时区已全局钉死);残余跨日差异(如 23:59:59 边界)属可解释差异,处置走「忽略 + 备注」,不自动归并;
- 性能:单日账单量级(单门店)≤ 千行级,内存 Map + 游标分页即可;不为千行级数据引入批处理框架。

## 7. 调度与并发(WeChatReconciliationScheduler)

```
@Component @AdminService(部署于 aqua-admin;组件在 aqua-analytics/reconciliation)
@Scheduled(cron = "0 30 10 * * ?")  fun reconcilePayment()  = // T-1 支付对账
@Scheduled(cron = "0 40 10 * * ?")  fun reconcileRefund()   = // T-1 退款对账
@Scheduled(cron = "0 50 10 * * ?")  fun reconcileSettlement() = // T-2 资金入账对账
@Scheduled(cron = "0 30 3 * * ?")   fun cleanupExpired()    = service.cleanup(报表 30 天 / 已处置差异 180 天,游标分批)
```

> **调度时间约束(官方接口)**:账单 T+1 09:00 开始生成、**建议 10:00 后获取**;原 02:xx 凌晨调度必然拿到 `STATEMENT_CREATING`/`NO_STATEMENT_EXIST`,故三类对账排在 10:30~10:50 错峰(下载账单-常见问题)。

- **防并发三层**:UK(bill_type, bill_date) 兜底 → `status='RUNNING'` 行存在即跳过(hasRunningTask)→ 任务认领走 `version` CAS(与 `payments.version` 同款乐观锁惯例);10:30/10:40/10:50 错峰 + 手动触发与调度互斥同一守卫;
- 失败处理:任务置 FAILED + `error_message`,**不自动重试**(次日账单日推进自然重跑;当日可手动重跑);调度异常记 ERROR 业务日志(§10)不中断后续调度;
- 手动触发:`executeReconciliation` mutation 同入口,`billDate` 校验窗口 [T-1, T-30],`executed_by` 记操作人。

## 8. admin GraphQL API 与前端页

### 8.1 Schema(扩展 `services/aqua-admin/src/main/resources/graphql/modules/payment.graphqls`)

```graphql
# ── 微信支付对账(R-PAY-04/F-ORD-031,规划)──────────────────────────
extend type Query {
  # 对账任务分页(billType/billDate 范围/status 筛选;PERMISSION_PAYMENT_RECONCILIATION_READ)
  reconciliationTasks(billType: ReconciliationBillType, dateFrom: LocalDate, dateTo: LocalDate,
                      status: ReconciliationTaskStatus, page: Int = 0, size: Int = 20): ReconciliationTaskVoPage!
  # 任务详情(含报表汇总三方校验)
  reconciliationReport(taskId: String!): ReconciliationReportVo
  # 差异分页(taskId/type/status 筛选;PERMISSION_PAYMENT_RECONCILIATION_READ)
  reconciliationDiscrepancies(taskId: String, discrepancyType: ReconciliationDiscrepancyType,
                              status: ReconciliationDiscrepancyStatus, page: Int = 0, size: Int = 20): ReconciliationDiscrepancyVoPage!
}

extend type Mutation {
  # 手动触发/重跑(billDate ∈ [T-1,T-30];幂等;PERMISSION_PAYMENT_RECONCILIATION_EXECUTE)
  executeReconciliation(input: ExecuteReconciliationInput!): ReconciliationTaskVo!
  # 差异处置(RESOLVE/IGNORE,备注必填;PERMISSION_PAYMENT_RECONCILIATION_EXECUTE)
  resolveDiscrepancy(input: ResolveDiscrepancyInput!): ReconciliationDiscrepancyVo!
}
```

- 类型:`ReconciliationTaskVo` / `ReconciliationReportVo` / `ReconciliationDiscrepancyVo` + 各自 Page;枚举带 `Reconciliation` 前缀防与既有枚举撞名(`ReconciliationBillType`: PAYMENT/REFUND;`ReconciliationTaskStatus`: PENDING/RUNNING/SUCCESS/FAILED/SKIPPED;`ReconciliationDiscrepancyType`: WECHAT_MISSING/LOCAL_MISSING/AMOUNT_MISMATCH/STATUS_MISMATCH;`ReconciliationDiscrepancyStatus`: UNRESOLVED/RESOLVED/IGNORED);
- 输入 `ExecuteReconciliationInput { billType, billDate }`、`ResolveDiscrepancyInput { id, action(RESOLVE/IGNORE), note }` 按规则放本模块 schema(生成的 input 类由 codegen 产出,不手写);
- schema 改后跑 `pnpm codegen` 再实现 resolver(生成 `schema.kt` 不手改)。

### 8.2 前端(admin-client)

- 新页 `src/pages/payment/payment-reconciliation/`(路由 `/payment-reconciliation`,菜单挂「支付管理」分组,与退款管理同级);权限点接 `PAYMENT_RECONCILIATION_READ/EXECUTE`;
- 布局按 PRD F-ORD-031:筛选行 + 任务 Table + 详情 Modal(三方汇总 StatisticCard + 差异 Table + 处置弹窗);交互范式对齐既有「收银对账」页;
- 单页一次 GraphQL 请求取「任务列表」,详情弹窗一次取「报表 + 差异首页」(架构不变量 7:合并请求,不链式串行)。

## 9. 权限与安全

- 新权限码(规划,落地时同步 `AdminPermission` + `RolePermissionMapping`,PRD 01-overview §权限码目录):
  - `PAYMENT_RECONCILIATION_READ`("支付对账查询")→ 任务/报表/差异查询;超管 + 管理员;
  - `PAYMENT_RECONCILIATION_EXECUTE`("支付对账执行")→ 手动触发/重跑/差异处置;超管 + 管理员;送水工无;
- resolver 每方法 `@PreAuthorize("hasAuthority('PERMISSION_PAYMENT_RECONCILIATION_READ')")` / `…_EXECUTE')`(resolvers.md 约定);
- **隐私最小化**:账单含「用户标识」(OpenID)列——解析时**丢弃该列不落库**(reports/discrepancies 仅业务键);报表 JSONB 不含个人信息,30 天过期即整体清理;
- 差异处置为资金相关写操作:备注必填 + 操作人留痕 + 处置不可逆(RESOLVED/IGNORED 不可再改,只能查看),审计走业务日志(§10)。

## 10. 业务日志与领域事件

- BizLogger(PAYMENT 域,`business_logs.operation` 结构化落库,与 09-platform-logs 口径一致):
  - `RECONCILIATION_EXECUTED`(任务成功:类型/账单日/差异计数/触发方 调度|管理员)
  - `RECONCILIATION_FAILED`(任务失败:类型/账单日/原因摘要)
  - `DISCREPANCY_RESOLVED`(差异处置:类型/out_trade_no/action/操作人)
- **不新增 outbox 领域事件**(§22 清单不扩):对账无跨模块下游消费者;差异告警的触达(如站内消息/订阅消息通知管理员)列为后续增强,届时优先复用 NOTIFICATION_SEND 链路再补事件行。

## 11. 测试策略与覆盖

| 层 | 用例(Kotest FunSpec + MockK;JUnit 平台,禁 @Test 风格) |
| --- | --- |
| 单元:CSV/汇总解析 | 正常账单 / 含引号逗号商品名 / 汇总行三方一致校验 / 「元→分」字符串转换(拒绝 Double)/ 列头顺序变化容错 / 哈希校验失败拒绝解析 |
| 单元:匹配算法 | 四类差异各至少 2 例 / 跨日长款短款 / REVOKED 行不比对 / 重跑幂等(已处置差异保留、未处置重算)/ 同 UK 并发守卫 |
| 单元:调度 | SKIPPED(mock 未配置)路径 / FAILED 不重试不阻塞次日 / 窗口 [T-1,T-30] 边界拒绝 |
| 集成(Testcontainers PG) | V350 迁移 → FakeWechatBillGateway(注入样例账单)→ executeReconciliation 落三表 → GraphQL 查询/处置 → 业务日志断言;本地流式分页正确性(>1 页数据) |

- 覆盖门槛:aqua-analytics 不在 80% 高线模块列表,适用全局 ≥ 60%;新增业务代码配齐上表单测保证 koverVerify 绿;
- 集成测试不依赖真实微信:网关按接口注入 Fake(`WechatBillGateway` 为接口的原因之一);假支付模式(`wechat.pay.mock-enabled=true`)下调度路径走 SKIPPED 分支,有独立用例。

## 12. 实施批次(对齐 14-misc §路线图 G93 / 06-testing-and-roadmap 批次)

| 批次 | 内容 | 验收 |
| --- | --- | --- |
| 批次 1(纯后端) | V350 迁移 + WechatBillGateway(SDK)+ 本地账实 API(foundation/trade)+ ReconciliationService 匹配 + 调度 + BizLogger | `./gradlew :modules:aqua-analytics:test` + 集成测试绿;AC-ORD-031-01~05/07 服务端可验 |
| 批次 2(管理端) | payment.graphqls 扩展 + codegen + resolver(@PreAuthorize)+ admin-client 对账页 | AC-ORD-031-01~08 全量;`./gradlew check` + `pnpm test:frontend` 全绿 |
| 批次 3(后续增强,另立项) | 资金账单(手续费/净额)/ SETTLEMENT / 退款主动查询 / 差异告警通知 | 开放决策 §13 定稿后启动 |

## 13. 开放决策汇总(【开放决策】,PRD 侧定稿后回填)

| # | 决策点 | 建议默认值 |
| --- | --- | --- |
| 1 | 资金账单(fundflowbill,手续费/净额)是否纳入 | 后续增强,本期不做(资损判定不依赖手续费) |
| 2 | 结算账单(SETTLEMENT) | 枚举预留不实现(商户平台导出形态,SDK 无服务) |
| 3 | 账单回溯窗口 | T-1 ~ T-30(微信支持更长,取保守值防误触发大范围重跑) |
| 4 | 差异自动修复 / 退款自动主动查询 | 均不做;仅标记 + 人工处置留痕(资金操作不程序化改状态) |
| 5 | 数据保留 | reports 30 天;差异已处置 180 天、未处置永不清理 |
| 6 | 重跑语义 | 未处置差异重算;已处置按业务键匹配保留(R-COL-03 对齐) |
| 7 | 差异告警触达 | 本期业务日志;后续复用通知链路(§10) |
