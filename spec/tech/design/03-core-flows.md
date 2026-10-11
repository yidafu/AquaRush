# 03 · 核心业务流程实现

> 每个流程给出：触发入口、步骤时序、并发/幂等控制、落库副作用。规则编号指向 PRD；公式与数字均引自 PRD 原文。

## 1. 订单状态机（R-ORD-01，7 态）

```
                    ┌──────────── 系统超时(30min) ────────────┐
创建(用户)→ PENDING_PAYMENT ──支付回调──▶ PENDING_DISPATCH ─派单/抢单─▶ PENDING_DELIVERY
创建(代客)─────────────────────────────────┘     │                    │
   │                                             │用户/管理员取消      │startDelivery
   │用户取消                                     ▼                    ▼
   └────────────────────────────▶ CANCELLED ◀────────────────── DELIVERING
                                        │                            │completeDelivery
                                        │退款审核通过+执行            ▼
                                        └──────────────▶ REFUNDED    COMPLETED(终态)
```

**实现机制**：

1. 每个转移封装为独立服务方法（`markPaid/accept/assign/start/complete/cancel…`），前置状态校验 + **条件更新**：`UPDATE orders SET status=? … WHERE id=? AND status=?`（订单表**无 version 列**——状态谓词即并发守卫，裁决 §2.9），受影响行数=0 即并发冲突/非法流转 → 抛 CONFLICT（非法流转一律拒绝，并发以服务端为最终裁决）。
2. 同事务追加 `order_operations` 并写入 outbox（同事务 INSERT `outbox_events`，提交后由 `OutboxPoller` 投递——Transactional Outbox，裁决 §2.8）驱动：订阅消息/WS 推送、统计 upsert、快照任务等下游。
3. 终态：COMPLETED / CANCELLED / REFUNDED——不可取消、不可修正地址、不可转单。
4. 转移表（实现唯一依据，与 02 §订单状态机一致）：

| 动作 | 前置 → 目标 | 触发方 | 附带副作用 |
| --- | --- | --- | --- |
| createOrder（用户/购物车/周期生成） | — → PENDING_PAYMENT | 用户/系统 | 扣库存；幂等键 clientRequestId；计价快照落库 |
| createDeliveryOrder（代客下单,原设计名 proxyOrder） | — → PENDING_DISPATCH | 送水工 | 免支付；`isSelfCollect` 自提单 payment_time=now（已收款） |
| 支付成功回调 | PENDING_PAYMENT → PENDING_DISPATCH | 微信回调 | 验签/金额校验/幂等（见 §4） |
| 支付超时 | PENDING_PAYMENT → CANCELLED | 延迟 outbox 事件(主)+扫描(兜底) | 恢复库存 + 退券/退票 + PAYMENT_TIMEOUT 事件 + 日志（见 §4） |
| 手动取消（未支付） | PENDING_PAYMENT → CANCELLED | 用户/管理员 | 恢复库存 |
| 手动取消（已支付） | PENDING_DISPATCH / PENDING_DELIVERY → CANCELLED | 用户/管理员 | 生成退款申请（PENDING）走 §5；**订单级 CAS**（cancelAtomically）与超时/回调并发仅一方成功 |
| 抢单 acceptDelivery | PENDING_DISPATCH → PENDING_DELIVERY | 送水工 | 记 assigned_at；先到先得 |
| 派单 assignDeliveryWorker / batchAssignOrders | PENDING_DISPATCH → PENDING_DELIVERY | 管理员 | worker 在线+可接单校验；同单仅一次（状态拦截） |
| startDelivery | PENDING_DELIVERY → DELIVERING | 送水工 | 记 delivery_started_at；启动轨迹上报 |
| completeDelivery | DELIVERING → COMPLETED | 送水工（仅本人） | 照片≥1 硬校验；收款方式必选；completed_at=now **即时计入营收**（R-ORD-11） |
| 转单（不改状态） | PENDING_DELIVERY / DELIVERING 不变 | 送水工发起+目标接收 | 仅改 delivery_worker_id，全程留痕（§7） |
| 退款完成 | CANCELLED → REFUNDED | 管理员（审核通过执行后） | ORDER_REFUND + 押金 DEPOSIT_REFUND（如有） |
| updateOrderStatus（后台） | 仅 PENDING_PAYMENT/PENDING_DISPATCH→CANCELLED；CANCELLED→REFUNDED | 管理员 | 按转移表校验 |

取消规则（R-ORD-03）：已取消/已退款/已完成/配送中不可取消；已取消不可恢复。

## 2. 营销计价引擎（R-MKT-01~10，纯服务端）

计价引擎为一个**无副作用纯函数** + 一个**落库执行器**：`Preview`（确认订单页/商品价展示）与 `Create`（下单）复用同一实现，Create 时对 Preview 结果不信任、全量重算（R-MON-01 防篡改：前端不传价格）。

### 2.1 第一阶段：成交单价解析（逐行 item）

```
resolveUnitPrice(item, userCtx, manualPrice?):
  ① manualPrice        —— 仅代客下单入参，>0 且 ≤2 位小数；命中则 priceSource=MANUAL
                          （命中①时默认禁用自动营销优惠：满减/会员折扣/首单立减，R-MKT-07）
                          【现状】入参**预留未接入**：CreateDeliveryOrderInput 无 manualPrice 字段，
                          引擎按 ②~⑤ 解析（PRD R-MKT-01「手动实收价①仍为开放项」）
  ② groupPrice         —— userCtx.groupId 有「商品×分组」价（空=未设置，跳过）→ GROUP_PRICE
  ③ flashSale          —— 商品处于特价活动期（now∈[start,end] 且活动启用）→ FLASH_SALE
  ④ newcomerPrice      —— userCtx.isNewcomer（无已完成订单，动态查询）且商品配置新人价 → NEWCOMER
  ⑤ catalogPrice       —— products.price_cents → CATALOG
```

水票先行：`ticketRedeemCount = min(用户可用票∩票种适用, 下单桶数)`；被覆盖桶数按**单价 0** 计入，不参与单价链与任何现金优惠；赠品（order_gifts）同为 0 价行（R-MKT-08/09）。

### 2.2 第二阶段：订单层优惠叠加（R-MKT-07 固定顺序）

```
subtotal(现金) = Σ 未被票/赠覆盖的 item：成交单价×数量          —— 券门槛/满减门槛判定基数（未含优惠）

① 满减/满折 或 首单立减（二选一，互斥取优惠最大者）：
   - 满减：候选=进行中且范围命中的活动；按活动商品小计判档（多档递增互斥，取命中档）；
     多活动同时命中 → 取优惠最大者不叠加；限时特价商品不参与
   - 首单立减：userCtx.isNewcomer 且活动有效 → 按配置（满X减Y 或 满X打N折）
   promotionDiscount / firstOrderDiscount 落各自快照列（命中哪个落哪个）
② 会员折扣：userCtx.memberLevel.discount_rate（1-99，9折=90）
   - 基数 = **未命中②③④单价让利的商品小计**（商品维度互斥，R-MKT-06）
   - memberDiscount = floor(基数 × (100 - rate)/100)
   【现状】未接入：member_levels 未建（PRD 缺口 G57,G80 排期），引擎在本级预留插槽
③ 优惠券：用户勾选一单一张
   - 资格：status=CLAIMED、未过期、范围命中、门槛 ≤ **现金部分商品小计（未含优惠）**（R-MKT-07 封顶行）
   - 优惠额 = 面额 或 floor(**满减后余额** × (10000−千分率)/10000)，**封顶不超余额**
     （基数 = cashSubtotal − 满减①后余额——PRD R-MKT-07「逐级应用/逐级扣减」口径，CouponRule 现实现;
      旧文「小计×折数」作废）
④（预留）积分抵扣：末位，≤ min(可用积分折算额, 应付×上限%)——现状未接入（G81 排期），仅占位

各级优惠金额一律以分向下取整；总优惠 ≤ 现金小计，优惠后应付 ≥ 0（封顶）。

应付合计 payAmount = 现金小计 + 配送费 + 随单押金
                    − (①+②+③ 各优惠) − 积分抵扣(预留)   [≥0；无余额抵扣——储值 2026-10-09 决策不做]
```

### 2.3 下单事务（Create 执行器，单 DB 事务）

1. 幂等检查：`uk_orders_user_client_request` 命中 → 直接返回已存在订单（不重复扣库存/核销）；
2. 逐 item `UPDATE products SET stock_quantity=stock_quantity-? WHERE id=? AND stock_quantity>=?`，任一失败整体回滚（提示具体缺货商品，R-ORD-02/05）；
3. 原子核销水票（§10）、核销优惠券（条件更新 CLAIMED→USED）、写入优惠/单价快照与 priceSource；
4. 押金判定（§9）+ 配送费快照（§11）+ 地址/时段快照；
5. 生成赠品行（gift_activity 命中，0 价）；订单落库 + order_operations(CREATED)；
6. 提交后：发 ORDER_CREATED 事件（订阅消息「下单成功」、送水工端广播「新订单可抢单」——仅 PENDING_DISPATCH 单）、业务日志。
7. 周期订购生成单 client_request_id=`sub-{id}-{period}`（同期次仅一单）；企业批量子单逐单独立执行（任一子单失败该子单回滚，其余成功——【开放决策】建议部分成功 + 结果回显）。**两者均已 [暂缓]**（2026-10-10 决策，§12/§13，不排期）。
8. 起送门槛拦截（R-OPS-03，规划中 G96）：非自提单商品金额（未扣优惠）< `minimum_order_cents` → 抛 BUSINESS_RULE「未达起送金额」整体拒绝（不进入扣库存步骤）。

## 3. 库存联动（R-PRO-01）

- 下单即扣（现状口径；「支付成功才扣」为 PRD 开放问题，如切换仅需移动扣减点到支付回调并加回调侧补偿）；恢复=取消/超时同事务反向条件更新；
- 售罄（stock_quantity=0）由用户端 `activeProducts` 过滤 + 详情「暂时缺货」；后台三态 ONLINE/OFFLINE/OUT_OF_STOCK 手动切换 + 下架/售罄不影响已生成订单（快照）；
- 并发：同一商品并发下单只能扣至 0（条件更新），无超卖。

## 4. 支付链路（R-PAY-01、R-ORD-04）

```
用户端 createOrderPayment(原设计名 payOrder):
  1. 校验订单 PENDING_PAYMENT → 创建 payments(PENDING, expired_at=now+超时分钟[默认30,可配],
     业务单号作微信 out_trade_no 匹配键, amount=payAmountCents, version=0)
  2. 微信支付 V3 JSAPI 预支付（prepay_id）→ 返回 OrderPaymentParamsVo 支付参数
     （requiresSimulation/autoCompleted 假支付标记;wechat.pay.mock-enabled=true 时发起即模拟
     回调完成——测试与生产共用同一完成路径）
  3. 发布延迟 outbox 事件：PAYMENT_EXPIRY_CHECK 写入 outbox_events
     （next_run_at=expired_at，destination=payment-expiry-events，aqua.outbox.destinations
     仅 admin 订阅）——延迟投递由 next_run_at 到期认领实现（R-ORD-04）

回调 POST /api/pay/notify:
  NotificationParser 验签解密 → 按业务单号(out_trade_no)链式探查匹配
  (订单 order_no → 购桶 order_no(BK 前缀) → 购票 order_no(TP 前缀);payments 表内无
   out_trade_no 列,见 01 §4.5;无 RECHARGE——储值 2026-10-09 决策不做)
  → 校验 回调金额 == payments.amount（不符拒绝+PAYMENT_FAILED 日志+告警）
  → 幂等: 仅 PENDING 流转（已 SUCCESS 重复回调直接应答成功；已取消保持不变）
  → 事务: payments→SUCCESS(paid_at, transaction_id) + 业务入账（订单 PENDING_PAYMENT→PENDING_DISPATCH、
    购票单入账逐张出票） + order_operations(PAYMENT_SUCCESS)
  → 提交后事件: 订阅消息「支付结果/下单成功」、送水工端广播

支付超时（双触发互斥）:
  主: outbox 延迟行到期 → OutboxPoller 投递 payment-expiry-events → PaymentExpiryCheckConsumer → handlePaymentTimeout
  兜底: admin 服务 PaymentExpiryScheduler 每 5 分钟分页游标扫描 (status=PENDING AND expired_at<now)
  两者都经 payments CAS 认领: UPDATE payments SET status='EXPIRED', version=version+1
        WHERE id=? AND status='PENDING' AND version=?   ← 只有一方成功
  认领成功 → 订单级 CAS 取消（cancelAtomically，与手动取消互斥，防重复恢复库存）
          → 同事务恢复库存 + 退券(未过期→RETURNED) + 退票(UNUSED/RETURNED 可退)
          → PAYMENT_TIMEOUT 事件（用户订阅消息「支付结果·超时取消」）+ 业务日志
```

## 5. 退款链路（R-PAY-02）

```
取消已支付订单（用户/管理员）→ 订单 CANCELLED（CAS）
  → 事件 ORDER_CANCELLED → 生成 payment_refunds(PENDING, refund_type=ORDER_REFUND,
     amount=payAmountCents 含商品+配送费+押金)（幂等：存在 PENDING/APPROVED 不重复生成）
后台退款审核（PAYMENT_REFUND）:
  通过 APPROVE → 执行微信退款（V3 refund API）:
      ORDER_REFUND 单笔退款 + 押金随单退还（DEPOSIT_REFUND 独立账本，不计营收/退款统计）
      → 微信退款成功 → refunds COMPLETED(processed_at) → 订单 CANCELLED→REFUNDED
      → 失败 FAILED(failure_reason) 可重试
  驳回 REJECT → 订单保持 CANCELLED，退款终止
副作用: 退券（未过期可复用）/ 退水票（已核销未过期→RETURNED）/ 押金记录 HELD→REFUNDED /
  积分按比例扣回（RETURN）/ 企业月结单红冲（invoices REVERSED，随 G83 [暂缓]）
```

## 6. 派单 / 抢单 / 批量派单（R-ORD-08）

- 前置：订单 PENDING_DISPATCH；worker `account_status=ACTIVE` 且 `is_available=true` 且**在线**（`online_status=ONLINE` 字段判定——送水工端显式切换,非 WS 会话注册表;离线者在派单弹窗不可选，已派单不受影响）；
- 派单/抢单并发先到先得：派单/批量派单/抢单三写点均已落 `assigned_at`（V344 补列,缺陷 #9 完成）,目标态 `派单→PENDING_DELIVERY`（与抢单统一,送水工显式起送）已于 B2 落地;重复派单/抢单由状态前置校验拦截（「该订单已被处理」）；
  **【现状缺口】** `acceptDelivery` 抢单为**读状态→改→save**（`DeliveryTaskMutationServiceImpl.kt`,无条件更新）——并发双抢单窗口存在,未列入本批裁决,建议随 B3 补条件更新（裁决外发现,待确认）；
- 批量派单：逐单执行同语句（或单事务批量），失败单回显原因；
- 就近派单（F-ADR-013）：派单弹窗按订单地址所属片区（省市区匹配）**置顶推荐**片区绑定且在线的送水工，仍可人工改选；
- 派单超时提醒（`delivery.assign_timeout_remind_minutes`）：待派单滞留超阈值 → 提醒管理员（通知域）；
- 接单超时预警（F-NTF-008/R-NTF-07，已实现 2026-10 G94）：与配送超时（G72）对称覆盖「无人接单」侧——「待派单」超 `order_accept_timeout_minutes`（默认 30 分钟）→ ORDER_ACCEPT_TIMEOUT（阶段锚点=coalesce(支付时刻,建单时刻)）、「待配送」超 `delivery_start_timeout_minutes`（默认 60 分钟）→ DELIVERY_START_TIMEOUT（阶段锚点=派单时刻），落库**全体管理员站内预警**（接收人类型 ADMIN；幂等键=订单号+事件类型+阶段锚点、同阶段仅一次，预警前已接单/取消则不发）；WS 实时推送为目标形态本期不接入（偏差见 §18 实现记录）；调度接入见 §21；
- 号码保护（F-ORD-032/R-ORD-17，规划中 G95）：送水工端任务/订单查询由服务端按订单状态裁剪收货人电话——仅 PENDING_DELIVERY/DELIVERING 下发完整号码，其余状态下发脱敏串（前端不可绕过）；ADMIN/SUPER_ADMIN 兼送水不受限（02 §4.4）。

## 7. 转单（R-ORD-15，目标形态）

```
发起: 原送水工（仅本人 PENDING_DELIVERY/DELIVERING 单）选目标（在线+可接单，可按片区推荐）
      → 校验 orders.transfer_count < 上限（默认 3，可配）→ 建 order_transfers(PENDING_ACCEPT)
      → WS 推送目标送水工；订单加锁防并发重复转（目标形态;`transfer_count` 列随 G84 落库,现状订单无 version 列）
响应: 目标接收 → 同事务 delivery_worker_id 替换 + transfer_count+1 + operations(TRANSFER_COMPLETED)
             → 原 worker 任务移除（查询自然过滤）/目标任务出现
      拒绝/超时(调度扫描) → 回退原送水工（状态不变）
收款归属: 按实际完成人计入日报（R-COL-04 口径，【开放决策→建议采纳】）；转单不改金额
```

## 8. 完成配送与营收确认（R-ORD-07 / R-ORD-11）

```
completeDelivery（仅本人 DELIVERING 单，单事务）:
  校验: photos 非空（服务端硬校验）∧ paymentType ∈ {CASH, WATER_TICKET, QR_CODE}
        （入参名 paymentType——原设计名 collectionMethod;QR_CODE 即扫码,原设计名 SCAN;
         电子票核销单未选 → 自动记 WATER_TICKET；自提单默认已收款豁免）
  执行: 订单 → COMPLETED，completed_at=now（状态谓词条件更新）
        delivery_remark / delivery_photos / payment_type 落库
        空桶回收登记（emptyBarrels:[{specification,count}] 非强制,B2 已实现:
        orders.empty_barrels_retrieved 落总数快照,明细按规格落 barrel_recovery_records）
        photos 置待核验（delivery_photo_review_status=PENDING,随缺陷 #10 已接线）
        sales_volume 累计、送水工统计 upsert（完成单量/收入）
        （押金实收确认不在本步——PENDING→HELD 在支付成功/代客下单确认时完成,见 §9）
  提交后: 营收即时计入（统计按 completed_at）+ 订阅消息「送达确认」+ 用户端可评价
照片核验（管理员, ORDER_REVIEW）: reviewDeliveryPhoto(action=APPROVED/REJECTED, remark) 通过/驳回
        （驳回必填 remark,可重复核验纠错）
        → orders.delivery_photo_review_status 更新 + operations 留痕（订单详情「交付照片」卡展示）
```

口径红线：**营收（已完成订单金额）、送水工业绩、收款日报、对账四者统一以 completed_at 为时间锚点**（R-ORD-11；取消「待确认收货」中间态后无确认并发问题）。

## 9. 押金桶生命周期（R-MON-03 / R-PRO-04；**B2 已实施**）

```
收取: 下单含规格 S 商品 ∧ 用户对 S 无 HELD/PENDING 押金记录 ∧ 价目启用
      → deposit_cents 随单计入 payAmountCents；建 deposit_records(PENDING, bucket_count=本单该规格桶数)
      （支付成功/代客下单确认后置 HELD；取消/超时软删 PENDING,微信已付走退款审核联动 REFUNDED）
      预览(orderPreview)与下单同源试算,押金行进费用明细(R-ORD 确认订单页)
持有: 在租桶数=Σ(HELD bucket_count)；空桶由送水工完成配送时按规格登记回收
      （barrel_recovery_records，非强制;orders.empty_barrels_retrieved 总数快照）
退押: 用户申请（≤在租桶数,同规格无处理中申请）→ PENDING（金额按 HELD 记录单桶押金快照定格）
      → 门店/送水工回收确认（RECOVERED，欠桶数同步减少）
      → 管理员确认退还: 微信退款 DEPOSIT_REFUND（独立账本，不计营收/退款统计;现金/无交易号仅落账本）
        + deposit_records HELD→REFUNDED（条件更新幂等，防并发重复退还;部分退还=原记录缩量+
          已退切片另落 REFUNDED 行）→ REFUNDED
      / 驳回（REJECTED，留原因）
台账: 桶账为实时聚合视图（在租/欠桶/押金合计/已退）,用户端 barrelAccount 与后台 barrelLedger 同源
      （仅计押金价目规格;后台筛选含「有欠桶」）;导出脱敏只读快照为规划（export_tasks,G20,R-PRO-05）
【开放决策】损坏/缺失扣减规则（建议：驳回转人工客服处理）
```

## 10. 水票生命周期（R-MKT-08）

```
购票: purchaseTickets(1~10 张) → ticket_purchases(PENDING) → 微信支付(payments target=TICKET_PURCHASE)
      → 回调入账: 逐张生成 user_tickets（含赠送票同池同口径）
      票号: ticket_no_sequences 按月原子取号 → "SP"+YYYYMM+8 位零填充序号（全局唯一索引）
核销（现状点位: 下单侧）: createOrder 事务内原子条件更新
      UPDATE user_tickets SET status='USED', used_at, used_order_id
      WHERE id IN (选中票) AND status IN ('UNUSED','RETURNED') AND expire_at > now
      受影响行数 ≠ 选中数 → 整单回滚（核销不足）；
      被核销桶单价 0 计入、收款方式计 WATER_TICKET 进日报（R-COL-04）
退回: 订单取消/退款 → 已核销未过期票 RETURNED（回票夹可复用）；过期作废 EXPIRED
退款: 未核销且未过期按张退（TICKET_REFUND 计入退款统计）；已核销/已过期不可退
目标增强【G91】: 送达侧扫码核销（或下单侧预扣+送达确认），使核销与服务交付对齐
运营策略【G97，已实现 2026-10】（票种级开关，默认不启用，R-MKT-08 修订）:
  引流票: is_lead_generation 标记 → 购票门禁:仅无已完成订单新用户可购(口径同 R-MKT-10)
            (purchaseTickets 服务层 hasCompletedOrders 校验,违规拒绝;核销语义不变)
  复购激活: activation_order_count=N(0/空=关) → 购票单落基线 completed_orders_at_purchase
            (启用票种购票时刻的已完成订单数;未启用/启用前存量单为 NULL 不参与判定)
            核销前置:当前已完成订单数 − 基线 ≥ N → 自动可用;
            不满足 → 「待激活」派生态(查询侧 findPendingActivationIds 判定,不入库)
            候选池剔除 + 显式请求超已激活池且存在待激活 → 拒「水票待激活:需完成复购后方可核销」
  单次核销上限: max_redeem_per_order=N(0/空=不限) → 计法=单笔订单 × 票种 ≤ N
            TicketRule 按票种截断(组内先过期先用,组合后重排 FIFO),
            请求超截断池且上限确为瓶颈 → 拒「超出单次核销上限」;普通存量不足保持静默截断
            preview.availableTicketCount = 截断后池大小(确认页勾选上限随之收敛)
```

**G97 实现记录与偏差清单（2026-10）**:
1. **激活判定口径(开放问题裁决)**:基线=启用票种**购票单创建时刻**的已完成订单数（`ticket_purchases.completed_orders_at_purchase`,V348）,
   激活条件=当前完成单数−基线≥N;**条件满足自动可用,无手动激活动作**;「待激活」为查询侧派生展示态
   （`UserTicketVo.pendingActivation`,服务端 `TicketQueryApiService.findPendingActivationIds` 一次判定）,
   **不新增持久化状态**——user_tickets 仍四态,`redeemAtomically` 原子条件与防重核销不变。
2. **上限计法(开放问题裁决)**:「单张票单笔订单最多核销 N 张」定为**单笔订单 × 票种**维度（非单票维度）;
   实现=构池按票种截断 + 超限拒绝;`availableTicketCount` 同步按截断后池下发。
3. **门禁优先级**:待激活 > 单次上限 > 普通存量不足(截断)——三者均以 `min(请求张数, 下单桶数)` 为有效请求比较;
   正常 UI 流(勾选数 ≤ preview 可用数)不会触发拒绝,拒绝仅兜底直连 API 与并发边界。
4. **引流票「拉新首购」判定(裁决)**:= R-MKT-10 新用户口径（无已完成订单）,**作用于购票环节**(非核销环节),
   核销语义与普通票一致;与 R-MKT-007 首单链路正交。
5. **配置变更口径**:策略仅对**启用后新购**的购票单生效(基线仅在启用时落值),遵循 R-MKT-08「票种/套餐配置变更不影响已购票」;
   票种侧停用激活(N→NULL)即时生效于已落基线的存量票(策略开关按票种当前值,基线按购票时快照)。
6. **效果区未实现残留(裁决)**:拉新首购转化/激活率/上限触发次数三指标——转化缺漏斗口径(无曝光/访问埋点)、
   触发缺事件埋点,成本不可控,**本期不实现**,记残留待口径齐备后补（_spec/10-marketing.md 页面 11）。

## 11. 配送费（R-MON-02 / F-OPS-005）

```
computeDeliveryFee(floor, selfPickup, rule, distance?):
  selfPickup → 0（且不受起送门槛约束，R-OPS-03）
  楼层分档（已落地,V345/结构 #6）:
    floor 为空 → 0（平房/无楼层）
    floor ≤ rule.freeFloor(默认 4) → 0
    否则 → 命中档位 tiers [fromFloor..toFloor|null=∞] 的 feeCents
            (5-6楼+200 / 7-9楼+500 / 10楼+800 默认,可配 —— V345 已落列并回填,结构 #6 完成);
            tiers 为空(存量兼容)回退兜底单档 surchargeCents
  距离阶梯【R-OPS-03,规划中 G96 排期待定】:
    distance = Haversine(地址经纬度, 门店经纬度)（直线距离【开放决策→建议默认直线】;
              地址 WGS-84 对门店 WGS-84 同系直接计算）
    命中 distanceTiers [fromKm..toKm|null=∞] 的 feeCents（3km 内 0 / 3-5km+300 / 5km+ 600 默认示例,可配;
              distanceTiers 为空或全 0 → 不加收）
  起送门槛【R-OPS-03,规划中 G96】: 非自提单商品金额 < minimum_order_cents →
    确认页 orderPreview 返回 belowMinimum 前置提示 + createOrder 服务端拦截（双保险,§2.3 步骤 8）
  delivery_fee_cents = 楼层分档 + 距离阶梯 一次快照合计；后续规则/楼层/距离档位变更不追溯；
  确认订单页读规则+地址楼层展示运费/「免配送费」；会员免配送费权益【开放决策→建议权益优先】(G80)
```

## 12. 周期订购（R-ORD-13，**[暂缓]** 2026-10-10 决策——保留设计不排期，勿重复立项）

```
计划: subscriptions(ACTIVE) + items + next_run_date
调度（admin 服务, 每日）: 分页扫描 status=ACTIVE AND next_run_date<=today（禁全表）
  对每个计划（期次 n=executed_periods+1）:
    AUTO_ORDER: 校验商品在售/库存、地址有效、范围可达
      → 全通过: 生成订单 client_request_id="sub-{id}-{n}"（幂等，同期次仅一单）
                → PENDING_PAYMENT + 推送「自动下单成功·待支付」（走 R-ORD-04 超时链路，不阻塞下一期）
      → 失败(缺货/地址不可达/欠信用): 不静默——转提醒「下单失败，请手动处理」（R-NTF-06）
    REMIND_ONLY: 推送「即将配送」提醒
    executed_periods=n，next_run_date=下期日期；达 max_periods/end_date → TERMINATED
用户操作: 暂停（PAUSED 期间不生成）/恢复/终止（已生成订单保留）
```

## 13. 企业客户月结（R-ORD-14，资金类，**[暂缓]** 2026-10-10 决策——保留设计不排期，勿重复立项）

```
建档: 后台标记企业客户（账期天数、信用额度、开票抬头/税号；绑定小程序账号+多地址）
批量下单: enterpriseOrderPreview 按收货地址拆子单预览 → createEnterpriseOrders
      → 逐子单独立库存校验+状态机（§1）；月结子单 payment_method=MONTHLY_SETTLEMENT
        不即时收款 → 生成应收账款（挂 enterprise_customer_id）
      → 信用校验: used + 本单 ≤ credit_limit（超额拦截「已超出信用额度…」；并发以条件更新防超）
账期: 调度按账期到期日聚合客户「已完成」订单生成 monthly_bills(OPEN)
      → 收款核销 bill_receipts（部分/结清）→ 释放信用额度
开票: 按账单（或订单集合）开 invoices（金额 ≤ 已结金额）；退款触发红冲（REVERSED）
风控: 账期到期未结清可限制下单【开放决策】；资金类需对账与告警（对齐 §14 套餐卡资金类口径）
```

## 14. 套餐卡与储值决策（R-MKT-11；储值 2026-10-09 决策不做）

```
用户储值: 不做（2026-10-09 合规决策，无预付卡资质）——recharge/payments target=RECHARGE/
  stored_value 流水与渠道对账/余额抵扣与组合支付全部不实现；确认订单支付方式仅微信（AC-MKT-010-05）。
套餐卡（G79 收敛项，保留）:
  购买: package_products → 支付回调入账生成 user_package_cards → 持有
  核销: 下单侧原子条件更新 + usages uk(card_id, order_id) 幂等（口径对齐水票，R-MKT-11）
  →过期/用完不可核销；未核销未过期可退（按未核销张数原路退款，AC-MKT-010-03）
资金类通用（套餐卡/工资；企业月结 G83 [暂缓]）: 幂等键 + 流水账本 + 对账任务 + 告警（见 §16 微信资金对账）
```

## 15. 会员等级与积分（R-MKT-12/13，目标形态）

```
会员: 判定=累计消费（Σ已完成实付，不含押金/退款）或完成单数 → 命中最高门槛等级
      订单完成/退款事件即时刷新 users.member_level_id 缓存（升降即时生效，快照不追溯）
      权益: 折扣率（叠加链②）/ 免配送费 / 优先派单 / 生日礼包
积分: 获取=订单完成 按实付×N 分（1 元=N 分）；退款按比例扣回（RETURN），赠出积分作废
      消耗=兑换（幂等）/下单抵扣（折算比例+单笔上限，末位）
      有效期=批次制【建议】: 每批带 expire_at，到期调度作废（EXPIRE）+ 提前提醒（F-NTF-007）
      原子性: 账户 version 条件更新防超扣/负积分；余额=Σ流水恒等
```

## 16. 收款日报与对账（R-COL-01~04 + 微信资金对账）

```
提交（送水工, RECONCILIATION_SUBMIT）:
  uk(worker_id, collection_date) 唯一（「今日已提交」拦截；前端按钮防重仅辅助）
  系统计算值=当日本人 COMPLETED 订单按 payment_type 汇总（锚点 completed_at）
  差异 = 填报(现金+扫码) − 系统(现金+扫码)（水票金额不入基数，张数单独精确比对）
  |现金/扫码差异| ≤ 100分 且 水票差异张数=0 → SUBMITTED（正常）
  否则 → 差异说明必填(≤200) → 服务端置 DISPUTED
后台（RECONCILIATION_*）:
  确认: SUBMITTED/DISPUTED → CONFIRMED；驳回: → DRAFT（rejected_* 留痕，可改可重提）

收款对账（现实形态，裁决 §2.7——按日聚合成单条，非逐送水工多行）:
  调度: DailyCollectionScheduler 每日 03:00 对昨日（admin；手动 executeDailyReconciliation 同口径，RECONCILIATION_EXECUTE）
  聚合: 汇总当日全部日报 → **单条 daily_reconciliations**（reconciliation_date 唯一约束）
        总额五项 + discrepancy_amount_cents + 状态: |差异| ≤ 容差 且 水票张数一致 → MATCHED，否则 → DISCREPANCY
        逐送水工明细入 report_data（JSON，键 workerReports[]：人/现金/扫码/水票/系统计算值/差异/status）
  已存在则同事务更新（重算并清除复核痕迹 reviewed_* 置空；非 delete-then-insert）
  复核: DISCREPANCY → REVIEWED（填备注；已复核不可再复核）
  下钻: dailyReconciliationOrderDetails 当日已送达订单流水（三端同源）
工作台统计（F-COL-003）: 今日四卡+7 天双折线经 `todayStatistics`+`weekStatistics` **单请求双字段**取回,30s 轮询（原设计聚合查询 workbenchData 未落 schema,列零散补全 A 类,02 §4.4/§5.7）

微信资金对账（R-PAY-04/F-ORD-031，G93 立项，**详细设计见 07-payment-reconciliation.md**；aqua-analytics/reconciliation）:
  三表: reconciliation_tasks（PAYMENT/REFUND 任务 + SETTLEMENT 预留: PENDING→RUNNING→SUCCESS/FAILED/SKIPPED,
        UK(bill_type,bill_date) 幂等锚点）
        + reconciliation_reports（报表快照,30 天保留）+ reconciliation_discrepancies（差异明细 → UNRESOLVED/RESOLVED/IGNORED）
  调度（admin）: 02:00 支付对账(T-1) / 02:05 退款对账(T-1)（错峰）；03:30 旧数据清理(reports 30 天/已处置差异 180 天)
  比对: 微信 V3 交易账单(ALL)+退款账单(REFUND)（BillDownloadServiceExtension 下载+解密+哈希校验）vs
        payments(paid_at 落账单日, SUCCESS/REFUNDING/REFUNDED) / payment_refunds(COMPLETED, processed_at 落账单日)
        → 四类差异（短款/长款/金额不一致/状态不一致）人工处置（resolveDiscrepancy→RESOLVED/IGNORED + 业务日志）
        防并发三层: UK + RUNNING 行守卫(hasRunningTask) + version CAS 认领
  管理端: executeReconciliation + 任务/报表/差异查询与处置（PERMISSION_PAYMENT_RECONCILIATION_READ/EXECUTE,规划码）
```

## 17. 工资结算（R-COL-05，目标形态，资金类）

```
规则: payroll_rules（底薪/计件单价/补贴/扣款，全局默认+按人覆盖；计件口径=订单数或桶数可配）
生成: 选周期+送水工范围 → 逐人生成结算单
      计件数=周期内本人 COMPLETED 订单（completed_at 归属，与统计/日报/对账同源）
      应发 = 底薪 + 计件数×单价 + 补贴 − 扣款（全分计）
      uk(worker,period) 唯一（「该周期结算单已存在」拦截）
      对账校验: 与日报/已完成订单比对，差异 → 挂起 + 告警（不静默生成）
流转: GENERATED →（复核可下钻）CONFIRMED → PAID（登记方式/凭证/时间/操作人；已发放幂等不可重复）
导出: 台账导出（export_tasks，脱敏）
```

## 18. 通知网关与实时推送（R-NTF-01~05 / R-ORD-10）

```
事件源: 各域事务**同事务写入 outbox_events**，提交后 OutboxPoller 投递 notifications-events → 通知网关消费:
  1. 组装消息（场景→模板/标题/摘要/关联单号；幂等键=关联单号+事件类型+事件时间）
  2. 统一落库 notifications（幂等键 UK 去重——推送与轮询的渲染去重基准）
  3. 分发:
     用户端 → 尝试微信订阅消息（门控四条件，见 02 §7；结果回写 channel_status）
            + WS（若在线）
     送水工端 → broadcast-events 目的地按订阅扇出（subscriber=admin/client 各一行）→
            两服务各自 WS 会话注册表定向/广播推送
  4. RN 端经 expo-notifications 呈现系统通知（前台横幅/通知栏/点击跳订单详情）；web 端静默
降级: 断线 → 指数退避重连 + 30s 轮询 + 重连补发（幂等键去重）
清理: 每日按 notification_retention_days(默认90) 游标分批删除
提醒类（F-NTF-007，调度分页扫描禁全表）:
  补水提醒: 用户近 N 次订单平均间隔（默认 30 天）判定 + 频率上限（同人 7 天 1 条）
            + 幂等键=用户+场景+期次；已购周期订购者降频【开放决策】
  周期提醒 / 券到期(默认 3 天窗口) / 水票余量 / 积分到期 / 配送超时(默认120分钟)
接单超时预警（F-NTF-008/R-NTF-07，已实现 2026-10 G94）:
  OrderAcceptTimeoutScanner 调度（aqua-trade，5 分钟周期）分页扫描「待派单/待配送」:
    待派单 超 order_accept_timeout_minutes(默认30) → ORDER_ACCEPT_TIMEOUT
      （阶段锚点 = coalesce(paid_at, created_at):在线支付以支付时刻、代客下单以落库时刻起算）
    待配送 超 delivery_start_timeout_minutes(默认60) → DELIVERY_START_TIMEOUT
      （阶段锚点 = assigned_at 派单时刻）
  → 事件经 outbox 投递 → 通知网关落库全体管理员站内预警（接收人类型 ADMIN，
    摘要含订单号/金额/超时时长，关联单号跳订单详情，后台消息列表 F-NTF-006 可查）；
    幂等键=订单号+事件类型+阶段锚点（同订单同状态阶段仅预警一次，R-NTF-07）；
    预警前订单被接单/开始配送/取消 → 不预警（扫描状态谓词 + 消费端状态复核，AC-NTF-008-04）
    【偏差】WS 推送全体管理员为目标形态本期不接入（见 §18 G94 实现记录）
```

**G94 实现记录与偏差清单（2026-10）**:
1. 无新 API（沿用设计）——新增 `OrderAcceptTimeoutScanner`（aqua-trade，5 分钟周期）、
   `OrderAcceptTimeoutNotificationHandler` / `DeliveryStartTimeoutNotificationHandler`（aqua-platform）、
   仓储分页查询 `findByStatusAndDispatchAnchorBefore` / `findByStatusAndAssignedAtBefore`（规则 13 分页游标，禁全表）。
2. **接收人模型偏差**:新增 `NotificationReceiverType.ADMIN`（GraphQL 枚举同步）,预警落库为**全体管理员共享单条站内消息**
   （不逐管理员复制;后台消息列表 F-NTF-006 为读取面;已读态共享,列表无逐人已读交互）。
3. **WS 实时推送不接入（偏差）**:admin-client 无 WebSocket 客户端与未读角标,现有 `/ws/notifications` 会话仅覆盖 USER/WORKER;
   网关对 ADMIN 仅落库不推送,WS 链路接入留目标形态（触达面=后台消息列表站内消息）。
4. **幂等锚点**:事件时间取阶段稳定锚点（待派单=支付/建单时刻、待配送=派单时刻）而非扫描时刻,
   使幂等键 `订单号:事件类型:锚点` 跨次扫描/双服务并发不变（R-NTF-07 = 订单号+事件类型+阶段）。
5. **开放问题裁决**:「超时升级（多次预警通知超管）」本期不做——每阶段仅一次预警,超时未处理由消息常驻后台消息列表人工跟进。
6. **阈值默认值口径**:`delivery_start_timeout_minutes` 默认 60 按 PRD R-NTF-07 与本技术设计定稿
   （`_spec/11-system-settings.md` 与 order-fulfillment.html 原稿「默认 30」已同步修正）。

## 19. 轨迹上报（R-ADR-03）

```
客户端（RN dev build）: 开始配送启动原生后台定位（原生 GPS+SQLite 持久化，锁屏/后台不丢点）
  5s 节流上报 reportTrajectory；弱网失败 AsyncStorage 缓存恢复补传；
  App 重启/重登录 resumeActive 校验订单仍 DELIVERING 后恢复
服务端: 校验 本人 ∧ DELIVERING → 落 delivery_trajectories；拒绝则停止该订单上报并丢弃缓存点
查看: 后台 trajectories 页（在线送水工+时间范围）实时位置+折线+回放（DELIVERY_WORKER_READ）
用户端跟踪（F-ADR-010 目标）: deliveryTracking 消费 latest location + ETA；
  状态门控（非配送中拦截）；已完成展示「已送达」停止刷新；仅概略位置（隐私）
```

## 20. 配送范围校验与片区（R-ADR-02/04）

```
下单校验（createOrder 前置 + 确认页 checkDeliveryArea）:
  地址(省,市,区) ∈ 任一启用片区 → 可达；否则拦截「该地址暂不支持配送」
  校验服务异常 → 降级放行（避免误拦）；片区停用即时失效；已生成订单不受影响（快照语义）
【现状·缺陷 #3】底层 DeliveryAreaQueryServiceImpl.isAddressInDeliveryArea 恒返回 true、
  checkDeliveryArea 查询未落 schema——范围校验实际未生效,按裁决改代码接线（B3,PRD G07 已登记）
片区维护: 表 delivery_areas 行内省市区单组（多行=多区域,is_enabled 启停）;
  名称唯一/省市区多选子表/绑定送水工（就近派单推荐集）为目标形态（G07/G78,01 §2.2）
周期订购（[暂缓] G77）: 生成订单前同校验，失败转提醒
```

## 21. 调度任务总表（全部部署于 aqua-admin，分页/游标，禁全表）

| 任务 | 频率/触发 | 参数（默认） | 作用 |
| --- | --- | --- | --- |
| PaymentExpiryScheduler | 每 5 分钟 | — | 支付超时兜底扫描（主=outbox 延迟事件 PAYMENT_EXPIRY_CHECK） |
| DailyCollectionScheduler | 每日 03:00 | — | 对昨日自动执行收款对账（聚合单条 daily_reconciliations，见 §16） |
| WeChatReconciliation | 每日 02:00 / 02:05 | — | 微信支付/退款账单对账（T-1 错峰；02:10 结算对账为预留不实现；03:30 报表清理保留 30 天；详见 07-payment-reconciliation.md） |
| StatisticsSnapshot | 每日（夜间） | — | 七张日汇总快照 delete-then-insert（营销统计覆盖判定;表清单见 01 §10——含 daily_revenue_facts/daily_refund_facts 拆分） |
| LogPartitionMaintainer | 每日 03:00 + 启动 | — | 预建当前月/+1/+2 分区（幂等） |
| LogAutoBackup | 每日 03:30 + 启动 | log.cleanup_enabled / retention_days(90) | 超期分区自动备份（advisory lock 防重，单分区失败继续） |
| NotificationCleanup | 每日 | retention_days(90) | 过期消息游标清理 |
| SubscriptionScheduler | 每日 | — | 周期订购到期生成订单/提醒（**[暂缓]** G77，不实现） |
| EnterpriseBillScheduler | 每日 | — | 账期到期生成月结账单（**[暂缓]** G83，不实现） |
| DeliveryTimeoutScanner | 周期扫描 | delivery.timeout_minutes(120) | 配送超时事件+定向推送（幂等键=订单号+场景+开始配送时间） |
| OrderAcceptTimeoutScanner | 每 5 分钟 | order_accept_timeout_minutes(30) / delivery_start_timeout_minutes(60) | 接单超时预警（F-NTF-008/G94 已实现 2026-10）：待派单/待配送超阈值 → ORDER_ACCEPT_TIMEOUT / DELIVERY_START_TIMEOUT 落库全体管理员站内预警（幂等键=订单号+事件类型+阶段锚点；WS 推送目标形态不接入，见 §18 实现记录） |
| CouponExpiringScanner | 每日 | coupon_expiring_notify_days(3) | 券到期提醒（幂等键=券id+场景+过期时间） |
| ReplenishReminderScanner | 每日 | 平均周期 30 天 / 频率 7 天 1 条 | 补水/余量/余额提醒（幂等键=用户+场景+期次） |
| PointsExpiryScanner | 每日 | 批次有效期 | 积分到期作废+提前提醒 |
| TransferTimeoutScanner | 周期 | — | 转单超时回退 |
| 存量订单查单补偿 | 周期 | — | payments PENDING 超时主动查单（防漏单） |

## 22. 领域事件清单（Transactional Outbox：同事务写入，提交后轮询投递）

以代码 `DomainEventType.kt`（`modules/aqua-foundation/.../messaging/event/`）现状为准；目的地路由见 `EventDestinationRouting.kt`（裁决 §3.1④）。

| 事件（DomainEventType） | destination | 消费者/用途 |
| --- | --- | --- |
| ORDER_CREATED / ORDER_PAID / ORDER_CANCELLED / ORDER_COMPLETED / ORDER_DELIVERED / ORDER_URGING | order-events | `DomainEventOutboxHandler` 分发 EventProcessor：通知网关（订阅消息+WS 推送：下单/支付/取消/送达/催单）、OrderCreated/OrderPaid/OrderCancelledHandler（取消副作用与退款链路触发，退款口径见 §5）、订单搜索内容更新 |
| PAYMENT_TIMEOUT / PAYMENT_REFUNDED | payment-events | PaymentTimeoutHandler（超时取消链路）、通知网关「支付结果」；PAYMENT_REFUNDED 为预留（暂无专属处理器） |
| PAYMENT_EXPIRY_CHECK | payment-expiry-events（**仅 admin 订阅**） | `PaymentExpiryCheckConsumer` → handlePaymentTimeout（R-ORD-04 主触发，`next_run_at` 到期投递） |
| DELIVERY_ASSIGNED / DELIVERY_STARTED / DELIVERY_COMPLETED / DELIVERY_TIMEOUT | delivery-events | 通知网关（被派单/开始配送/送达确认/超时定向推送）、DeliveryAssignmentHandler、DeliveryTimeoutHandler |
| ORDER_ACCEPT_TIMEOUT / DELIVERY_START_TIMEOUT | delivery-events | **已实现（2026-10）**（F-NTF-008/R-NTF-07，G94）：接单超时预警——路由随 DELIVERY_TIMEOUT 惯例，通知网关落库全体管理员站内预警（接收人类型 ADMIN，幂等键=订单号+事件类型+阶段锚点，见 §18） |
| USER_REGISTERED / USER_UPDATED / COUPON_EXPIRING | user-events | COUPON_EXPIRING → 通知网关（券到期提醒）；USER_* 现状预留（消费端仅记 warn，按需挂处理器） |
| ADDRESS_UPDATED / PRODUCT_UPDATED | delivery-events（缺省路由分支） | UpdateOrderSearchContentHandler / UpdateOrderSearchContentForProductHandler（订单搜索内容刷新） |
| NOTIFICATION_SEND | notifications-events | `NotificationMessageConsumer` → 消息网关组装落库 notifications 并分发（03 §18） |
| （推送扇出）notifications 摘要 | broadcast-events（按 subscriber 扇出行） | `NotificationWsPushConsumer` → 两服务 WS 会话注册表 → 客户端定向/广播推送 |

消费端约定：至少一次投递，处理器必须幂等；失败指数退避重试、耗尽置 DEAD（健康指标 DOWN）。UNKNOWN 类型仅记日志不投递失败。

---

下一册：[04-frontend-design.md](04-frontend-design.md) —— 三端前端实现。
