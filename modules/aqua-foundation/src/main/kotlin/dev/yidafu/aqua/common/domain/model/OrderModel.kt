/**
 * AquaRush
 *
 * Copyright (C) 2025 AquaRush Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.domain.model.enums.PriceSource
import dev.yidafu.aqua.common.util.MoneyUtils
import java.math.BigDecimal
import java.time.LocalDateTime

data class OrderModel(
  var id: Long? = null,
  val orderNo: String = "",
  val userId: Long = -1L,
  // 服务端下单幂等键(R-ORD-06):(user_id, client_request_id) 唯一,重复提交返回已存在订单;
  // 可空 —— 旧请求/未携带幂等键时维持原行为
  var clientRequestId: String? = null,
  // 配送时段快照(AC-ORD-001-05,下单时随单落库;null=未指定,默认 IMMEDIATE)
  var deliveryTimeSlot: DeliveryTimeSlot? = null,
  // 订单商品明细(F-ORD-004 多商品):权威数据在 order_items 表,经 OrderItemRepository 查询
  var amountCents: Long = 0,
  // 配送费(分,下单时按配送费规则快照,规则变更不影响已生成订单 —— R-MON-02)
  val deliveryFeeCents: Long = 0,
  // ========================================================================
  // 营销计价快照(R-MKT-01/02/07/08:下单时快照,营销配置变更不影响已生成订单)
  // ========================================================================
  // 成交单价(分,价格策略解析结果 —— R-MKT-02)
  var unitPriceCents: Long? = null,
  // 成交单价来源(CATALOG_PRICE/GROUP_PRICE/MANUAL —— R-MKT-01)
  var priceSource: PriceSource? = null,
  // 满减/折扣活动(叠加① —— R-MKT-07)
  var promotionActivityId: Long? = null,
  var promotionActivityName: String? = null,
  var promotionDiscountCents: Long = 0,
  // 优惠券(叠加③ —— R-MKT-07,一单一券)
  var couponId: Long? = null,
  var couponName: String? = null,
  var couponDiscountCents: Long = 0,
  // 水票核销桶数(一票抵一桶,被核销桶按单价 0 计入 —— R-MKT-08)
  var ticketRedeemCount: Int = 0,
  // 水票抵扣金额(分,= 商品小计 - 现金小计,随单快照 —— R-MKT-08)
  var ticketDiscountCents: Long = 0,
  // 首单立减(叠加①,与满减互斥取最大 —— R-MKT-10)
  var firstOrderActivityId: Long? = null,
  var firstOrderActivityName: String? = null,
  var firstOrderDiscountCents: Long = 0,
  val addressId: Long = 0,
  var status: OrderModelStatus = OrderModelStatus.PENDING_PAYMENT,
  var paymentMethod: PaymentMethod? = null,
  var paymentTransactionId: String? = null,
  var paymentTime: LocalDateTime? = null,
  var deliveryWorkerId: Long? = null,
  var deliveryPhotos: String? = null,
  // 配送照片核验(G23/F-ORD-021):核验状态/时间/核验人/备注留痕
  var deliveryPhotoReviewStatus: DeliveryPhotoReviewStatus? = null,
  var deliveryPhotoReviewedAt: LocalDateTime? = null,
  var deliveryPhotoReviewedBy: Long? = null,
  var deliveryPhotoReviewRemark: String? = null,
  // 桶押金(分,R-MON-03/G06:下单时按商品押金快照合计,随单一并收取;退还时退回)
  var depositCents: Long = 0,
  var isSelfCollect: Boolean = false,
  var paymentType: PaymentType? = null,
  var deliveryStartedAt: LocalDateTime? = null,
  var assignedAt: LocalDateTime? = null,
  var completedAt: LocalDateTime? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
  var remark: String? = null,
  var deliveryRemark: String? = null,
  var searchContent: String? = null,
  // ========================================================================
  // Hibernate Associations
  // ========================================================================
  // 送水员档案并入 admins 单表后,delivery_worker_id 引用账号 id(迁移 v1.2.1)
  val user: UserModel? = null,
  val address: AddressModel? = null,
  val deliveryWorker: AdminModel? = null,
) : SoftDeletable {
  // Compatibility property for existing code - returns amount in yuan as BigDecimal
  val amount: BigDecimal
    get() = MoneyUtils.fromCents(amountCents)
  val totalAmount: BigDecimal
    get() = MoneyUtils.fromCents(amountCents)

  // 应付合计(分)= 商品金额 + 配送费 + 桶押金(随单收取,R-MON-03/G06)
  val payAmountCents: Long
    get() = amountCents + deliveryFeeCents + depositCents
}
