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

package dev.yidafu.aqua.marketing.common.exposed

import dev.yidafu.aqua.common.domain.table.BarrelPurchaseItemsTable
import dev.yidafu.aqua.common.domain.table.BarrelPurchasesTable
import dev.yidafu.aqua.common.domain.table.BarrelRecoveryRecordsTable
import dev.yidafu.aqua.common.domain.table.CouponTemplatesTable
import dev.yidafu.aqua.common.domain.table.CustomerGroupsTable
import dev.yidafu.aqua.common.domain.table.DepositPricesTable
import dev.yidafu.aqua.common.domain.table.DepositRecordsTable
import dev.yidafu.aqua.common.domain.table.DepositRefundApplicationsTable
import dev.yidafu.aqua.common.domain.table.FirstOrderActivitiesTable
import dev.yidafu.aqua.common.domain.table.FlashSaleActivitiesTable
import dev.yidafu.aqua.common.domain.table.GiftActivitiesTable
import dev.yidafu.aqua.common.domain.table.GroupProductPricesTable
import dev.yidafu.aqua.common.domain.table.OrderItemsTable
import dev.yidafu.aqua.common.domain.table.OrdersTable
import dev.yidafu.aqua.common.domain.table.PromotionActivitiesTable
import dev.yidafu.aqua.common.domain.table.PromotionActivityTiersTable
import dev.yidafu.aqua.common.domain.table.TicketNoSequencesTable
import dev.yidafu.aqua.common.domain.table.TicketPurchasesTable
import dev.yidafu.aqua.common.domain.table.TicketTypesTable
import dev.yidafu.aqua.common.domain.table.UserCouponsTable
import dev.yidafu.aqua.common.domain.table.UserTicketsTable
import dev.yidafu.aqua.common.domain.table.UsersTable
import dev.yidafu.aqua.common.testsupport.PostgresTestContainer
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime

/**
 * marketing 仓储实现测试的 Testcontainers 真实 PG 支撑(JPA→Exposed 迁移 P4 覆盖率门禁配套),
 * 模式与 product 的 ProductPgTestSupport 一致。
 * 建表覆盖 MarketingTables 14 张 + SourceTables 的 user_coupons/ticket_purchases/user_tickets;
 * 所有表均无跨表 FK reference,可直接 insert。种子行 NOT NULL 列全给值。
 */
object MarketingPgTestSupport {
  val database: Database = PostgresTestContainer.connect("aquamarketingrepo")

  private val allTables: Array<Table> =
    arrayOf(
      // MarketingTables
      CouponTemplatesTable,
      CustomerGroupsTable,
      GroupProductPricesTable,
      PromotionActivitiesTable,
      PromotionActivityTiersTable,
      FlashSaleActivitiesTable,
      GiftActivitiesTable,
      FirstOrderActivitiesTable,
      TicketTypesTable,
      TicketNoSequencesTable,
      DepositPricesTable,
      BarrelPurchasesTable,
      BarrelPurchaseItemsTable,
      DepositRecordsTable,
      DepositRefundApplicationsTable,
      BarrelRecoveryRecordsTable,
      // SourceTables(marketing 域部分 + 桶账聚合依赖 users/orders/order_items)
      UserCouponsTable,
      UsersTable,
      OrdersTable,
      OrderItemsTable,
      TicketPurchasesTable,
      UserTicketsTable,
    )

  @Volatile
  private var schemaCreated = false

  fun reset() {
    transaction(MarketingPgTestSupport.database) {
      if (schemaCreated) {
        allTables.forEach { it.deleteAll() }
      } else {
        SchemaUtils.create(*allTables)
        schemaCreated = true
      }
    }
  }

  private var nextId = 90000L

  fun id(): Long = nextId++

  private fun now(): LocalDateTime = LocalDateTime.now()

  fun couponTemplate(
    id: Long = id(),
    name: String = "券模板$id",
    status: String = "ACTIVE",
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      CouponTemplatesTable.insert {
        it[CouponTemplatesTable.id] = id
        it[CouponTemplatesTable.name] = name
        it[CouponTemplatesTable.couponType] = "CASH"
        it[CouponTemplatesTable.thresholdCents] = 1000L
        it[CouponTemplatesTable.discountAmountCents] = 100L
        it[CouponTemplatesTable.discountRatePermille] = null
        it[CouponTemplatesTable.scope] = "ALL"
        it[CouponTemplatesTable.productId] = null
        it[CouponTemplatesTable.validityType] = "FIXED_RANGE"
        it[CouponTemplatesTable.validFrom] = createdAt
        it[CouponTemplatesTable.validTo] = createdAt.plusDays(30)
        it[CouponTemplatesTable.validDays] = null
        it[CouponTemplatesTable.issuanceType] = "AUTO_CLAIM"
        it[CouponTemplatesTable.perUserLimit] = 1
        it[CouponTemplatesTable.status] = status
        it[CouponTemplatesTable.createdAt] = createdAt
        it[CouponTemplatesTable.updatedAt] = createdAt
        it[CouponTemplatesTable.isDeleted] = deleted
      }
      id
    }

  fun userCoupon(
    id: Long = id(),
    userId: Long,
    couponTemplateId: Long,
    status: String = "RECEIVED",
    name: String = "券$id",
    expireTime: LocalDateTime = now().plusDays(7),
    claimTime: LocalDateTime = now(),
    usedOrderId: Long? = null,
    usedAt: LocalDateTime? = null,
    returnedAt: LocalDateTime? = null,
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      UserCouponsTable.insert {
        it[UserCouponsTable.id] = id
        it[UserCouponsTable.userId] = userId
        it[UserCouponsTable.couponTemplateId] = couponTemplateId
        it[UserCouponsTable.status] = status
        it[UserCouponsTable.name] = name
        it[UserCouponsTable.couponType] = "CASH"
        it[UserCouponsTable.thresholdCents] = 1000L
        it[UserCouponsTable.discountAmountCents] = 100L
        it[UserCouponsTable.discountRatePermille] = null
        it[UserCouponsTable.scope] = "ALL"
        it[UserCouponsTable.productId] = null
        it[UserCouponsTable.expireTime] = expireTime
        it[UserCouponsTable.claimTime] = claimTime
        it[UserCouponsTable.usedOrderId] = usedOrderId
        it[UserCouponsTable.usedAt] = usedAt
        it[UserCouponsTable.returnedAt] = returnedAt
        it[UserCouponsTable.createdAt] = createdAt
        it[UserCouponsTable.updatedAt] = createdAt
        it[UserCouponsTable.isDeleted] = deleted
      }
      id
    }

  fun customerGroup(
    id: Long = id(),
    name: String = "分组$id",
    description: String? = null,
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      CustomerGroupsTable.insert {
        it[CustomerGroupsTable.id] = id
        it[CustomerGroupsTable.name] = name
        it[CustomerGroupsTable.description] = description
        it[CustomerGroupsTable.createdAt] = createdAt
        it[CustomerGroupsTable.updatedAt] = createdAt
        it[CustomerGroupsTable.isDeleted] = deleted
      }
      id
    }

  fun groupProductPrice(
    id: Long = id(),
    groupId: Long,
    productId: Long,
    priceCents: Long = 900L,
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      GroupProductPricesTable.insert {
        it[GroupProductPricesTable.id] = id
        it[GroupProductPricesTable.customerGroupId] = groupId
        it[GroupProductPricesTable.productId] = productId
        it[GroupProductPricesTable.priceCents] = priceCents
        it[GroupProductPricesTable.createdAt] = createdAt
        it[GroupProductPricesTable.updatedAt] = createdAt
        it[GroupProductPricesTable.isDeleted] = deleted
      }
      id
    }

  fun promotionActivity(
    id: Long = id(),
    name: String = "满减$id",
    activityType: String = "FULL_REDUCE",
    scope: String = "ALL",
    productId: Long? = null,
    customerGroupId: Long? = null,
    validFrom: LocalDateTime = now().minusDays(1),
    validTo: LocalDateTime = now().plusDays(30),
    status: String = "ACTIVE",
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      PromotionActivitiesTable.insert {
        it[PromotionActivitiesTable.id] = id
        it[PromotionActivitiesTable.name] = name
        it[PromotionActivitiesTable.activityType] = activityType
        it[PromotionActivitiesTable.scope] = scope
        it[PromotionActivitiesTable.productId] = productId
        it[PromotionActivitiesTable.customerGroupId] = customerGroupId
        it[PromotionActivitiesTable.validFrom] = validFrom
        it[PromotionActivitiesTable.validTo] = validTo
        it[PromotionActivitiesTable.status] = status
        it[PromotionActivitiesTable.createdAt] = createdAt
        it[PromotionActivitiesTable.updatedAt] = createdAt
        it[PromotionActivitiesTable.isDeleted] = deleted
      }
      id
    }

  fun promotionTier(
    id: Long = id(),
    activityId: Long,
    thresholdCents: Long = 100L,
    discountAmountCents: Long? = 10L,
    discountRatePermille: Long? = null,
    sort: Int = 0,
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      PromotionActivityTiersTable.insert {
        it[PromotionActivityTiersTable.id] = id
        it[PromotionActivityTiersTable.activityId] = activityId
        it[PromotionActivityTiersTable.thresholdCents] = thresholdCents
        it[PromotionActivityTiersTable.discountAmountCents] = discountAmountCents
        it[PromotionActivityTiersTable.discountRatePermille] = discountRatePermille
        it[PromotionActivityTiersTable.sort] = sort
        it[PromotionActivityTiersTable.createdAt] = createdAt
        it[PromotionActivityTiersTable.updatedAt] = createdAt
        it[PromotionActivityTiersTable.isDeleted] = deleted
      }
      id
    }

  fun flashSale(
    id: Long = id(),
    name: String = "特价$id",
    productId: Long = 1L,
    salePriceCents: Long = 100L,
    validFrom: LocalDateTime = now().minusDays(1),
    validTo: LocalDateTime = now().plusDays(30),
    status: String = "ACTIVE",
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      FlashSaleActivitiesTable.insert {
        it[FlashSaleActivitiesTable.id] = id
        it[FlashSaleActivitiesTable.name] = name
        it[FlashSaleActivitiesTable.productId] = productId
        it[FlashSaleActivitiesTable.salePriceCents] = salePriceCents
        it[FlashSaleActivitiesTable.validFrom] = validFrom
        it[FlashSaleActivitiesTable.validTo] = validTo
        it[FlashSaleActivitiesTable.status] = status
        it[FlashSaleActivitiesTable.createdAt] = createdAt
        it[FlashSaleActivitiesTable.updatedAt] = createdAt
        it[FlashSaleActivitiesTable.isDeleted] = deleted
      }
      id
    }

  fun giftActivity(
    id: Long = id(),
    name: String = "满赠$id",
    giftType: String = "FULL_GIFT",
    thresholdCents: Long? = 2000L,
    thresholdQuantity: Int? = null,
    scope: String = "ALL",
    productId: Long? = null,
    customerGroupId: Long? = null,
    giftProductId: Long = 9L,
    giftQuantity: Int = 1,
    validFrom: LocalDateTime = now().minusDays(1),
    validTo: LocalDateTime = now().plusDays(30),
    status: String = "ACTIVE",
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      GiftActivitiesTable.insert {
        it[GiftActivitiesTable.id] = id
        it[GiftActivitiesTable.name] = name
        it[GiftActivitiesTable.giftType] = giftType
        it[GiftActivitiesTable.thresholdCents] = thresholdCents
        it[GiftActivitiesTable.thresholdQuantity] = thresholdQuantity
        it[GiftActivitiesTable.scope] = scope
        it[GiftActivitiesTable.productId] = productId
        it[GiftActivitiesTable.customerGroupId] = customerGroupId
        it[GiftActivitiesTable.giftProductId] = giftProductId
        it[GiftActivitiesTable.giftQuantity] = giftQuantity
        it[GiftActivitiesTable.validFrom] = validFrom
        it[GiftActivitiesTable.validTo] = validTo
        it[GiftActivitiesTable.status] = status
        it[GiftActivitiesTable.createdAt] = createdAt
        it[GiftActivitiesTable.updatedAt] = createdAt
        it[GiftActivitiesTable.isDeleted] = deleted
      }
      id
    }

  fun firstOrderActivity(
    id: Long = id(),
    name: String = "首单$id",
    activityType: String = "FIRST_ORDER_REDUCE",
    thresholdCents: Long? = null,
    discountAmountCents: Long? = 500L,
    discountRatePermille: Long? = null,
    scope: String = "ALL",
    productId: Long? = null,
    newcomerPriceCents: Long? = null,
    validFrom: LocalDateTime = now().minusDays(1),
    validTo: LocalDateTime = now().plusDays(30),
    status: String = "ACTIVE",
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      FirstOrderActivitiesTable.insert {
        it[FirstOrderActivitiesTable.id] = id
        it[FirstOrderActivitiesTable.name] = name
        it[FirstOrderActivitiesTable.activityType] = activityType
        it[FirstOrderActivitiesTable.thresholdCents] = thresholdCents
        it[FirstOrderActivitiesTable.discountAmountCents] = discountAmountCents
        it[FirstOrderActivitiesTable.discountRatePermille] = discountRatePermille
        it[FirstOrderActivitiesTable.scope] = scope
        it[FirstOrderActivitiesTable.productId] = productId
        it[FirstOrderActivitiesTable.newcomerPriceCents] = newcomerPriceCents
        it[FirstOrderActivitiesTable.validFrom] = validFrom
        it[FirstOrderActivitiesTable.validTo] = validTo
        it[FirstOrderActivitiesTable.status] = status
        it[FirstOrderActivitiesTable.createdAt] = createdAt
        it[FirstOrderActivitiesTable.updatedAt] = createdAt
        it[FirstOrderActivitiesTable.isDeleted] = deleted
      }
      id
    }

  fun ticketType(
    id: Long = id(),
    name: String = "票种$id",
    ticketType: String = "UNIVERSAL",
    productId: Long? = null,
    specification: String? = null,
    packageCount: Int = 10,
    packagePriceCents: Long = 1000L,
    giftCount: Int = 0,
    validityType: String = "FIXED_RANGE",
    validFrom: LocalDateTime? = now(),
    validTo: LocalDateTime? = now().plusDays(365),
    validDays: Int? = null,
    perUserLimit: Int = 0,
    isLeadGeneration: Boolean = false,
    activationOrderCount: Int? = null,
    maxRedeemPerOrder: Int? = null,
    status: String = "ACTIVE",
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      TicketTypesTable.insert {
        it[TicketTypesTable.id] = id
        it[TicketTypesTable.name] = name
        it[TicketTypesTable.ticketType] = ticketType
        it[TicketTypesTable.productId] = productId
        it[TicketTypesTable.specification] = specification
        it[TicketTypesTable.packageCount] = packageCount
        it[TicketTypesTable.packagePriceCents] = packagePriceCents
        it[TicketTypesTable.giftCount] = giftCount
        it[TicketTypesTable.validityType] = validityType
        it[TicketTypesTable.validFrom] = validFrom
        it[TicketTypesTable.validTo] = validTo
        it[TicketTypesTable.validDays] = validDays
        it[TicketTypesTable.perUserLimit] = perUserLimit
        it[TicketTypesTable.isLeadGeneration] = isLeadGeneration
        it[TicketTypesTable.activationOrderCount] = activationOrderCount
        it[TicketTypesTable.maxRedeemPerOrder] = maxRedeemPerOrder
        it[TicketTypesTable.status] = status
        it[TicketTypesTable.createdAt] = createdAt
        it[TicketTypesTable.updatedAt] = createdAt
        it[TicketTypesTable.isDeleted] = deleted
      }
      id
    }

  fun ticketPurchase(
    id: Long = id(),
    userId: Long,
    ticketTypeId: Long,
    orderNo: String = "TK$id",
    packageCount: Int = 1,
    giftCount: Int = 0,
    totalCount: Int = 10,
    packagePriceCents: Long = 1000L,
    paymentStatus: String = "PAID",
    transactionId: String? = null,
    paidAt: LocalDateTime? = now(),
    completedOrdersAtPurchase: Int? = null,
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      TicketPurchasesTable.insert {
        it[TicketPurchasesTable.id] = id
        it[TicketPurchasesTable.userId] = userId
        it[TicketPurchasesTable.ticketTypeId] = ticketTypeId
        it[TicketPurchasesTable.orderNo] = orderNo
        it[TicketPurchasesTable.packageCount] = packageCount
        it[TicketPurchasesTable.giftCount] = giftCount
        it[TicketPurchasesTable.totalCount] = totalCount
        it[TicketPurchasesTable.packagePriceCents] = packagePriceCents
        it[TicketPurchasesTable.paymentStatus] = paymentStatus
        it[TicketPurchasesTable.transactionId] = transactionId
        it[TicketPurchasesTable.paidAt] = paidAt
        it[TicketPurchasesTable.completedOrdersAtPurchase] = completedOrdersAtPurchase
        it[TicketPurchasesTable.createdAt] = createdAt
        it[TicketPurchasesTable.updatedAt] = createdAt
        it[TicketPurchasesTable.isDeleted] = deleted
      }
      id
    }

  fun userTicket(
    id: Long = id(),
    userId: Long,
    ticketTypeId: Long,
    purchaseId: Long,
    ticketNo: String = "NO$id",
    status: String = "UNUSED",
    expireTime: LocalDateTime = now().plusDays(30),
    isGift: Boolean = false,
    usedOrderId: Long? = null,
    usedAt: LocalDateTime? = null,
    returnedAt: LocalDateTime? = null,
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      UserTicketsTable.insert {
        it[UserTicketsTable.id] = id
        it[UserTicketsTable.userId] = userId
        it[UserTicketsTable.ticketTypeId] = ticketTypeId
        it[UserTicketsTable.purchaseId] = purchaseId
        it[UserTicketsTable.ticketNo] = ticketNo
        it[UserTicketsTable.status] = status
        it[UserTicketsTable.expireTime] = expireTime
        it[UserTicketsTable.isGift] = isGift
        it[UserTicketsTable.usedOrderId] = usedOrderId
        it[UserTicketsTable.usedAt] = usedAt
        it[UserTicketsTable.returnedAt] = returnedAt
        it[UserTicketsTable.createdAt] = createdAt
        it[UserTicketsTable.updatedAt] = createdAt
        it[UserTicketsTable.isDeleted] = deleted
      }
      id
    }

  fun barrelFee(
    id: Long = id(),
    specification: String = "19L/桶$id",
    priceCents: Long = 3000L,
    status: String = "ENABLED",
    sortOrder: Int = 999,
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      DepositPricesTable.insert {
        it[DepositPricesTable.id] = id
        it[DepositPricesTable.specification] = specification
        it[DepositPricesTable.priceCents] = priceCents
        it[DepositPricesTable.status] = status
        it[DepositPricesTable.sortOrder] = sortOrder
        it[DepositPricesTable.createdAt] = createdAt
        it[DepositPricesTable.updatedAt] = createdAt
        it[DepositPricesTable.isDeleted] = deleted
      }
      id
    }

  fun barrelPurchase(
    id: Long = id(),
    userId: Long,
    orderNo: String = "BK$id",
    totalAmountCents: Long = 3000L,
    paymentStatus: String = "PENDING",
    transactionId: String? = null,
    paidAt: LocalDateTime? = null,
    remark: String? = null,
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      BarrelPurchasesTable.insert {
        it[BarrelPurchasesTable.id] = id
        it[BarrelPurchasesTable.userId] = userId
        it[BarrelPurchasesTable.orderNo] = orderNo
        it[BarrelPurchasesTable.totalAmountCents] = totalAmountCents
        it[BarrelPurchasesTable.paymentStatus] = paymentStatus
        it[BarrelPurchasesTable.transactionId] = transactionId
        it[BarrelPurchasesTable.paidAt] = paidAt
        it[BarrelPurchasesTable.refundedAt] = null
        it[BarrelPurchasesTable.refundedBy] = null
        it[BarrelPurchasesTable.refundRemark] = null
        it[BarrelPurchasesTable.remark] = remark
        it[BarrelPurchasesTable.createdAt] = createdAt
        it[BarrelPurchasesTable.updatedAt] = createdAt
        it[BarrelPurchasesTable.isDeleted] = deleted
      }
      id
    }

  fun barrelItem(
    id: Long = id(),
    purchaseId: Long,
    barrelFeeId: Long,
    specification: String = "19L/桶",
    unitPriceCents: Long = 3000L,
    quantity: Int = 1,
    amountCents: Long = 3000L,
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      BarrelPurchaseItemsTable.insert {
        it[BarrelPurchaseItemsTable.id] = id
        it[BarrelPurchaseItemsTable.purchaseId] = purchaseId
        it[BarrelPurchaseItemsTable.barrelFeeId] = barrelFeeId
        it[BarrelPurchaseItemsTable.specification] = specification
        it[BarrelPurchaseItemsTable.unitPriceCents] = unitPriceCents
        it[BarrelPurchaseItemsTable.quantity] = quantity
        it[BarrelPurchaseItemsTable.amountCents] = amountCents
        it[BarrelPurchaseItemsTable.createdAt] = createdAt
        it[BarrelPurchaseItemsTable.updatedAt] = createdAt
        it[BarrelPurchaseItemsTable.isDeleted] = deleted
      }
      id
    }

  fun depositRecord(
    id: Long = id(),
    orderId: Long,
    userId: Long,
    depositAmountCents: Long = 500L,
    specification: String? = "19L",
    unitDepositCents: Long? = 500L,
    bucketCount: Int? = 1,
    status: String = "HELD",
    collectedAt: LocalDateTime? = now(),
    refundedAt: LocalDateTime? = null,
    refundedBy: Long? = null,
    refundRemark: String? = null,
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      DepositRecordsTable.insert {
        it[DepositRecordsTable.id] = id
        it[DepositRecordsTable.orderId] = orderId
        it[DepositRecordsTable.userId] = userId
        it[DepositRecordsTable.depositAmountCents] = depositAmountCents
        it[DepositRecordsTable.specification] = specification
        it[DepositRecordsTable.unitDepositCents] = unitDepositCents
        it[DepositRecordsTable.bucketCount] = bucketCount
        it[DepositRecordsTable.status] = status
        it[DepositRecordsTable.collectedAt] = collectedAt
        it[DepositRecordsTable.refundedAt] = refundedAt
        it[DepositRecordsTable.refundedBy] = refundedBy
        it[DepositRecordsTable.refundRemark] = refundRemark
        it[DepositRecordsTable.createdAt] = createdAt
        it[DepositRecordsTable.updatedAt] = createdAt
        it[DepositRecordsTable.isDeleted] = deleted
      }
      id
    }

  fun depositRefundApplication(
    id: Long = id(),
    userId: Long,
    specification: String = "19L",
    bucketCount: Int = 1,
    depositCents: Long = 500L,
    status: String = "PENDING",
    handlerAdminId: Long? = null,
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      DepositRefundApplicationsTable.insert {
        it[DepositRefundApplicationsTable.id] = id
        it[DepositRefundApplicationsTable.userId] = userId
        it[DepositRefundApplicationsTable.specification] = specification
        it[DepositRefundApplicationsTable.bucketCount] = bucketCount
        it[DepositRefundApplicationsTable.depositCents] = depositCents
        it[DepositRefundApplicationsTable.status] = status
        it[DepositRefundApplicationsTable.handlerAdminId] = handlerAdminId
        it[DepositRefundApplicationsTable.createdAt] = createdAt
        it[DepositRefundApplicationsTable.updatedAt] = createdAt
        it[DepositRefundApplicationsTable.isDeleted] = deleted
      }
      id
    }

  fun barrelRecoveryRecord(
    id: Long = id(),
    workerId: Long,
    orderId: Long,
    specification: String = "19L",
    bucketCount: Int = 1,
    createdAt: LocalDateTime = now(),
    deleted: Boolean = false,
  ): Long =
    transaction(database) {
      BarrelRecoveryRecordsTable.insert {
        it[BarrelRecoveryRecordsTable.id] = id
        it[BarrelRecoveryRecordsTable.workerId] = workerId
        it[BarrelRecoveryRecordsTable.orderId] = orderId
        it[BarrelRecoveryRecordsTable.specification] = specification
        it[BarrelRecoveryRecordsTable.bucketCount] = bucketCount
        it[BarrelRecoveryRecordsTable.createdAt] = createdAt
        it[BarrelRecoveryRecordsTable.updatedAt] = createdAt
        it[BarrelRecoveryRecordsTable.isDeleted] = deleted
      }
      id
    }

  fun depositUser(
    id: Long = id(),
    nickname: String = "桶账用户$id",
    phone: String? = "13800000000",
  ): Long =
    transaction(database) {
      UsersTable.insert {
        it[UsersTable.id] = id
        it[UsersTable.wechatOpenid] = "openid-$id"
        it[UsersTable.nickname] = nickname
        it[UsersTable.phone] = phone
        it[UsersTable.email] = "user-$id@test.local"
        it[UsersTable.status] = "ACTIVE"
        it[UsersTable.role] = "USER"
        it[UsersTable.createdAt] = now()
        it[UsersTable.updatedAt] = now()
        it[UsersTable.lastLoginAt] = now()
      }
      id
    }

  fun completedOrder(
    id: Long = id(),
    userId: Long,
    orderNo: String = "SO$id",
    depositAmountCents: Long = 0L,
  ): Long =
    transaction(database) {
      OrdersTable.insert {
        it[OrdersTable.id] = id
        it[OrdersTable.orderNo] = orderNo
        it[OrdersTable.userId] = userId
        it[OrdersTable.totalAmountCents] = 4000L
        it[OrdersTable.deliveryFeeCents] = 0L
        it[OrdersTable.addressId] = 1L
        it[OrdersTable.status] = "COMPLETED"
        it[OrdersTable.depositAmountCents] = depositAmountCents
        it[OrdersTable.createdAt] = now()
        it[OrdersTable.updatedAt] = now()
      }
      id
    }

  fun orderItem(
    id: Long = id(),
    orderId: Long,
    productId: Long = id(),
    quantity: Int = 1,
    specification: String = "19L",
  ): Long =
    transaction(database) {
      OrderItemsTable.insert {
        it[OrderItemsTable.id] = id
        it[OrderItemsTable.orderId] = orderId
        it[OrderItemsTable.productId] = productId
        it[OrderItemsTable.quantity] = quantity
        it[OrderItemsTable.unitPriceCents] = 4000L
        it[OrderItemsTable.totalPriceCents] = 4000L * quantity
        it[OrderItemsTable.productSnapshot] = """{"specification":"$specification","name":"水"}"""
        it[OrderItemsTable.createdAt] = now()
        it[OrderItemsTable.updatedAt] = now()
      }
      id
    }
}
