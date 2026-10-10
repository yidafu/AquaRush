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

package dev.yidafu.aqua.analytics.common.exposed

import dev.yidafu.aqua.common.domain.table.AdminsTable
import dev.yidafu.aqua.common.domain.table.DailyMarketingFactsTable
import dev.yidafu.aqua.common.domain.table.DailyMarketingUsageFactsTable
import dev.yidafu.aqua.common.domain.table.DailyOrderFactsTable
import dev.yidafu.aqua.common.domain.table.DailyProductFactsTable
import dev.yidafu.aqua.common.domain.table.DailyRefundFactsTable
import dev.yidafu.aqua.common.domain.table.DailyRevenueFactsTable
import dev.yidafu.aqua.common.domain.table.DailyUserFactsTable
import dev.yidafu.aqua.common.domain.table.OrderGiftsTable
import dev.yidafu.aqua.common.domain.table.OrderItemsTable
import dev.yidafu.aqua.common.domain.table.OrderOperationsTable
import dev.yidafu.aqua.common.domain.table.OrdersTable
import dev.yidafu.aqua.common.domain.table.PaymentRefundsTable
import dev.yidafu.aqua.common.domain.table.ProductsTable
import dev.yidafu.aqua.common.domain.table.ReconciliationDiscrepanciesTable
import dev.yidafu.aqua.common.domain.table.ReconciliationReportsTable
import dev.yidafu.aqua.common.domain.table.ReconciliationTasksTable
import dev.yidafu.aqua.common.domain.table.TicketPurchasesTable
import dev.yidafu.aqua.common.domain.table.UserActionLogsTable
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
 * analytics 仓储实现测试的 Testcontainers 真实 PG 支撑(JPA→Exposed 迁移 P3 覆盖率门禁配套),
 * 模式与 ops 的 OpsPgTestSupport 一致。
 */
object AnalyticsPgTestSupport {
  val database: Database = PostgresTestContainer.connect("aquaanalyticsrepo")

  private val allTables: Array<Table> =
    arrayOf(
      DailyOrderFactsTable,
      DailyProductFactsTable,
      DailyUserFactsTable,
      DailyRefundFactsTable,
      DailyRevenueFactsTable,
      DailyMarketingFactsTable,
      DailyMarketingUsageFactsTable,
      ReconciliationTasksTable,
      ReconciliationReportsTable,
      ReconciliationDiscrepanciesTable,
      OrdersTable,
      OrderItemsTable,
      UsersTable,
      UserActionLogsTable,
      PaymentRefundsTable,
      UserCouponsTable,
      TicketPurchasesTable,
      UserTicketsTable,
      OrderGiftsTable,
      ProductsTable,
      AdminsTable,
      OrderOperationsTable,
    )

  @Volatile
  private var schemaCreated = false

  fun reset() {
    transaction(AnalyticsPgTestSupport.database) {
      if (schemaCreated) {
        allTables.forEach { it.deleteAll() }
      } else {
        SchemaUtils.create(*allTables)
        schemaCreated = true
      }
    }
  }
}

/** 测试种子数据工厂:直接以 Exposed INSERT 写源表(均包 transaction)。 */
object Seed {
  private var nextId = 9000L

  fun id(): Long = nextId++

  fun order(
    id: Long = id(),
    userId: Long = 1L,
    status: String = "COMPLETED",
    amountCents: Long = 10000L,
    deliveryWorkerId: Long? = 77L,
    createdAt: LocalDateTime = LocalDateTime.now(),
    completedAt: LocalDateTime? = createdAt,
    couponId: Long? = null,
    couponDiscountCents: Long = 0L,
    promotionDiscountCents: Long = 0L,
    firstOrderDiscountCents: Long = 0L,
    promotionActivityId: Long? = null,
    firstOrderActivityId: Long? = null,
    deleted: Boolean = false,
  ): Long =
    transaction(AnalyticsPgTestSupport.database) {
      val oid =
        if (id != 0L) id else id()
      OrdersTable.insert {
        it[OrdersTable.id] = oid
        it[orderNo] = "T$oid"
        it[OrdersTable.userId] = userId
        it[totalAmountCents] = amountCents
        it[OrdersTable.addressId] = 1L
        it[OrdersTable.status] = status
        it[isSelfCollect] = false
        it[deliveryFeeCents] = 0L
        it[OrdersTable.promotionDiscountCents] = promotionDiscountCents
        it[OrdersTable.couponDiscountCents] = couponDiscountCents
        it[OrdersTable.firstOrderDiscountCents] = firstOrderDiscountCents
        it[ticketRedeemCount] = 0
        it[ticketDiscountCents] = 0L
        it[depositAmountCents] = 0L
        it[OrdersTable.couponId] = couponId
        it[OrdersTable.promotionActivityId] = promotionActivityId
        it[OrdersTable.firstOrderActivityId] = firstOrderActivityId
        it[OrdersTable.deliveryWorkerId] = deliveryWorkerId
        it[OrdersTable.createdAt] = createdAt
        it[updatedAt] = createdAt
        it[OrdersTable.completedAt] = completedAt
        it[isDeleted] = deleted
      }
      oid
    }

  fun orderItem(
    orderId: Long,
    productId: Long = 1L,
    quantity: Int = 2,
    totalPriceCents: Long = 5000L,
    createdAt: LocalDateTime = LocalDateTime.now(),
  ): Long =
    transaction(AnalyticsPgTestSupport.database) {
      val itemId = id()
      OrderItemsTable.insert {
        it[OrderItemsTable.id] = itemId
        it[OrderItemsTable.orderId] = orderId
        it[OrderItemsTable.productId] = productId
        it[OrderItemsTable.quantity] = quantity
        it[unitPriceCents] = totalPriceCents / quantity
        it[OrderItemsTable.totalPriceCents] = totalPriceCents
        it[productSnapshot] = "{}"
        it[OrderItemsTable.createdAt] = createdAt
        it[OrderItemsTable.updatedAt] = createdAt
        it[OrderItemsTable.isDeleted] = false
      }
      itemId
    }

  fun user(
    id: Long = id(),
    createdAt: LocalDateTime = LocalDateTime.now(),
    deleted: Boolean = false,
  ): Long =
    transaction(AnalyticsPgTestSupport.database) {
      val uid = if (id != 0L) id else id()
      UsersTable.insert {
        it[UsersTable.id] = uid
        it[wechatOpenid] = "openid-$uid"
        it[email] = "u$uid@t.cn"
        it[UsersTable.status] = "ACTIVE"
        it[UsersTable.role] = "USER"
        it[totalSpentCents] = 0L
        it[UsersTable.createdAt] = createdAt
        it[updatedAt] = createdAt
        it[lastLoginAt] = createdAt
        it[isDeleted] = deleted
      }
      uid
    }

  fun userActionLog(
    userId: String = "1",
    actionType: String = "USER_LOGIN",
    createdAt: LocalDateTime = LocalDateTime.now(),
  ): Long =
    transaction(AnalyticsPgTestSupport.database) {
      val logId = id()
      UserActionLogsTable.insert {
        it[UserActionLogsTable.id] = logId
        it[UserActionLogsTable.userId] = userId
        it[UserActionLogsTable.actionType] = actionType
        it[UserActionLogsTable.createdAt] = createdAt
      }
      logId
    }

  fun product(
    id: Long = id(),
    name: String = "农夫山泉",
    status: String = "ONLINE",
    stock: Int = 100,
    sales: Int = 10,
    priceCents: Long = 1200L,
  ): Long =
    transaction(AnalyticsPgTestSupport.database) {
      val pid = if (id != 0L) id else id()
      ProductsTable.insert {
        it[ProductsTable.id] = pid
        it[ProductsTable.name] = name
        it[ProductsTable.priceCents] = priceCents
        it[ProductsTable.thumbnailUrl] = "http://x/y.png"
        it[ProductsTable.stockQuantity] = stock
        it[ProductsTable.status] = status
        it[ProductsTable.specification] = "12L"
        it[ProductsTable.salesVolume] = sales
        it[ProductsTable.sortOrder] = 0
        it[ProductsTable.createdAt] = LocalDateTime.now()
        it[ProductsTable.updatedAt] = LocalDateTime.now()
        it[ProductsTable.depositPriceCents] = 0L
        it[ProductsTable.isDeleted] = false
      }
      pid
    }

  fun admin(
    id: Long = id(),
    role: String = "DELIVERY_WORKER",
    realName: String? = "师傅$id",
  ): Long =
    transaction(AnalyticsPgTestSupport.database) {
      val aid = if (id != 0L) id else id()
      AdminsTable.insert {
        it[AdminsTable.id] = aid
        it[username] = "w$aid"
        it[passwordHash] = "x"
        it[AdminsTable.role] = role
        it[onlineStatus] = "ONLINE"
        it[totalOrders] = 0
        it[completedOrders] = 0
        it[isAvailable] = true
        it[accountStatus] = "ACTIVE"
        it[AdminsTable.createdAt] = LocalDateTime.now()
        it[updatedAt] = LocalDateTime.now()
        it[isDeleted] = false
        it[AdminsTable.realName] = realName
      }
      aid
    }
}
