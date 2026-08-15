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

import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.domain.model.enums.PriceSource
import dev.yidafu.aqua.common.utils.MoneyUtils
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SoftDelete
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "orders")
data class OrderModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "order_no", unique = true, nullable = false)
  val orderNo: String = "",
  @Column(name = "user_id", nullable = false)
  val userId: Long = -1L,
  // 订单商品明细(F-ORD-004 多商品):权威数据在 order_items 表,经 OrderItemRepository 查询
  @Column(name = "total_amount_cents", nullable = false)
  var amountCents: Long = 0,
  // 配送费(分,下单时按配送费规则快照,规则变更不影响已生成订单 —— R-MON-02)
  @Column(name = "delivery_fee_cents", nullable = false)
  val deliveryFeeCents: Long = 0,
  // ========================================================================
  // 营销计价快照(R-MKT-01/02/07/08:下单时快照,营销配置变更不影响已生成订单)
  // ========================================================================
  // 成交单价(分,价格策略解析结果 —— R-MKT-02)
  @Column(name = "unit_price_cents")
  var unitPriceCents: Long? = null,
  // 成交单价来源(CATALOG_PRICE/GROUP_PRICE/MANUAL —— R-MKT-01)
  @Column(name = "price_source")
  @Enumerated(EnumType.STRING)
  var priceSource: PriceSource? = null,
  // 满减/折扣活动(叠加① —— R-MKT-07)
  @Column(name = "promotion_activity_id")
  var promotionActivityId: Long? = null,
  @Column(name = "promotion_activity_name")
  var promotionActivityName: String? = null,
  @Column(name = "promotion_discount_cents", nullable = false)
  var promotionDiscountCents: Long = 0,
  // 优惠券(叠加③ —— R-MKT-07,一单一券)
  @Column(name = "coupon_id")
  var couponId: Long? = null,
  @Column(name = "coupon_name")
  var couponName: String? = null,
  @Column(name = "coupon_discount_cents", nullable = false)
  var couponDiscountCents: Long = 0,
  // 水票核销桶数(一票抵一桶,被核销桶按单价 0 计入 —— R-MKT-08)
  @Column(name = "ticket_redeem_count", nullable = false)
  var ticketRedeemCount: Int = 0,
  // 首单立减(叠加①,与满减互斥取最大 —— R-MKT-10)
  @Column(name = "first_order_activity_id")
  var firstOrderActivityId: Long? = null,
  @Column(name = "first_order_activity_name")
  var firstOrderActivityName: String? = null,
  @Column(name = "first_order_discount_cents", nullable = false)
  var firstOrderDiscountCents: Long = 0,
  @Column(name = "address_id", nullable = false)
  val addressId: Long = 0,
  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  var status: OrderModelStatus = OrderModelStatus.PENDING_PAYMENT,
  @Column(name = "payment_method")
  @Enumerated(EnumType.STRING)
  var paymentMethod: PaymentMethod? = null,
  @Column(name = "payment_transaction_id")
  var paymentTransactionId: String? = null,
  @Column(name = "paid_at")
  var paymentTime: LocalDateTime? = null,
  @Column(name = "delivery_worker_id")
  var deliveryWorkerId: Long? = null,
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "delivery_photos", columnDefinition = "json")
  var deliveryPhotos: String? = null,
  @Column(name = "is_self_collect", nullable = false)
  var isSelfCollect: Boolean = false,
  @Column(name = "payment_type")
  @Enumerated(EnumType.STRING)
  var paymentType: PaymentType? = null,
  @Column(name = "delivery_started_at")
  var deliveryStartedAt: LocalDateTime? = null,
  @Column(name = "delivery_confirmed_at")
  var deliveryConfirmedAt: LocalDateTime? = null,
  @Column(name = "completed_at")
  var completedAt: LocalDateTime? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,
  @Column(name = "deleted_by")
  override var deletedBy: Long? = null,
  @Column(name = "remark", length = 500)
  var remark: String? = null,
  @Column(name = "delivery_remark", length = 500)
  var deliveryRemark: String? = null,
  @Column(name = "search_content", columnDefinition = "text")
  var searchContent: String? = null,
  // ========================================================================
  // Hibernate Associations
  // ========================================================================
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  val user: UserModel? = null,
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "address_id", insertable = false, updatable = false)
  val address: AddressModel? = null,
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "delivery_worker_id", insertable = false, updatable = false)
  val deliveryWorker: DeliveryWorkerModel? = null,
) : SoftDeletable {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }

  // Compatibility property for existing code - returns amount in yuan as BigDecimal
  val amount: BigDecimal
    get() = MoneyUtils.fromCents(amountCents)
  val totalAmount: BigDecimal
    get() = MoneyUtils.fromCents(amountCents)

  // 应付合计(分)= 商品金额 + 配送费(纯展示口径,支付流暂不包含配送费 —— G17 待接)
  val payAmountCents: Long
    get() = amountCents + deliveryFeeCents
}
