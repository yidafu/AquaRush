/**
 * AquaRush
 *
 * Copyright (C) 2026 AquaRush Team
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
package dev.yidafu.aqua.common.domain.table

import dev.yidafu.aqua.common.testsupport.PostgresTestContainer
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * foundation 表定义冒烟测试的真实 PG 支撑(JPA→Exposed 迁移 P4),
 * 模式与 user/analytics/platform/ops 的 *PgTestSupport 一致。
 *
 * 覆盖 [common.domain.table] 包下全部 53 个具体 table object
 * (Trade/Marketing/Product/Source/Reconciliation/Fact/Platform/User),
 * 不含抽象基类 [SoftDeletableTable] —— 目的是在最便宜的时点暴露
 * 列名拼错、PG 保留字冲突、jsonString 类型映射问题。
 */
object FoundationPgTestSupport {
  val database: Database = PostgresTestContainer.connect("aquafoundationtables")

  val allTables: Array<Table> =
    arrayOf(
      // SourceTables
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
      // TradeTables
      PaymentsTable,
      ReviewsTable,
      DeliveryTrajectoriesTable,
      DeliveryWorkerStatisticsTable,
      DailyCollectionRecordsTable,
      DailyReconciliationsTable,
      DeliveryAreasTable,
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
      BarrelFeesTable,
      BarrelPurchasesTable,
      BarrelPurchaseItemsTable,
      DepositRecordsTable,
      // ProductTables
      ProductFavoritesTable,
      ProductOptionsTable,
      // ReconciliationTables
      ReconciliationTasksTable,
      ReconciliationReportsTable,
      ReconciliationDiscrepanciesTable,
      // FactTables
      DailyOrderFactsTable,
      DailyProductFactsTable,
      DailyUserFactsTable,
      DailyRefundFactsTable,
      DailyRevenueFactsTable,
      DailyMarketingFactsTable,
      DailyMarketingUsageFactsTable,
      // PlatformTables
      NotificationsTable,
      UserSubscriptionAuthsTable,
      UserNotificationSettingsTable,
      MessageHistoriesTable,
      FileMetadataTable,
      // UserTables
      AddressesTable,
      RegionsTable,
      NotificationSettingsTable,
    )

  @Volatile
  private var schemaCreated = false

  /** 首次调用建全部表,之后清空数据(测试间隔离)。 */
  fun reset() {
    transaction(database) {
      if (schemaCreated) {
        allTables.forEach { it.deleteAll() }
      } else {
        SchemaUtils.create(*allTables)
        schemaCreated = true
      }
    }
  }
}
