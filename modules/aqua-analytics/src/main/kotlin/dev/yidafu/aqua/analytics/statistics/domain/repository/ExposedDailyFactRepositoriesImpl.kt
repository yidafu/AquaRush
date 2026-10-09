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

package dev.yidafu.aqua.analytics.statistics.domain.repository

import dev.yidafu.aqua.analytics.common.exposed.ExposedDailyFactRepository
import dev.yidafu.aqua.common.domain.model.DailyMarketingFactModel
import dev.yidafu.aqua.common.domain.model.DailyMarketingUsageFactModel
import dev.yidafu.aqua.common.domain.model.DailyOrderFactModel
import dev.yidafu.aqua.common.domain.model.DailyProductFactModel
import dev.yidafu.aqua.common.domain.model.DailyRefundFactModel
import dev.yidafu.aqua.common.domain.model.DailyRevenueFactModel
import dev.yidafu.aqua.common.domain.model.DailyUserFactModel
import dev.yidafu.aqua.common.domain.table.DailyMarketingFactsTable
import dev.yidafu.aqua.common.domain.table.DailyMarketingUsageFactsTable
import dev.yidafu.aqua.common.domain.table.DailyOrderFactsTable
import dev.yidafu.aqua.common.domain.table.DailyProductFactsTable
import dev.yidafu.aqua.common.domain.table.DailyRefundFactsTable
import dev.yidafu.aqua.common.domain.table.DailyRevenueFactsTable
import dev.yidafu.aqua.common.domain.table.DailyUserFactsTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.stereotype.Repository
import java.time.LocalDate

/** 订单日事实 Exposed 实现。 */
@Repository
class ExposedDailyOrderFactRepositoryImpl(
  database: Database,
) : ExposedDailyFactRepository<DailyOrderFactModel>(
    database,
    DailyOrderFactsTable,
    DailyOrderFactsTable.id,
    DailyOrderFactsTable.statDate,
    DailyOrderFactsTable.createdAt,
    DailyOrderFactsTable.updatedAt,
    ::toOrderFactModel,
  ),
  DailyOrderFactRepository {
  override fun writeColumns(
    stmt: InsertStatement<Number>,
    entity: DailyOrderFactModel,
  ) {
    stmt[DailyOrderFactsTable.status] = entity.status
    stmt[DailyOrderFactsTable.deliveryWorkerId] = entity.deliveryWorkerId
    stmt[DailyOrderFactsTable.orderCount] = entity.orderCount
    stmt[DailyOrderFactsTable.amountCents] = entity.amountCents
    stmt[DailyOrderFactsTable.itemQuantity] = entity.itemQuantity
  }

  override fun statDateOf(entity: DailyOrderFactModel): LocalDate = entity.statDate

  companion object {
    fun toOrderFactModel(row: ResultRow): DailyOrderFactModel =
      DailyOrderFactModel(
        id = row[DailyOrderFactsTable.id],
        statDate = row[DailyOrderFactsTable.statDate],
        status = row[DailyOrderFactsTable.status],
        deliveryWorkerId = row[DailyOrderFactsTable.deliveryWorkerId],
        orderCount = row[DailyOrderFactsTable.orderCount],
        amountCents = row[DailyOrderFactsTable.amountCents],
        itemQuantity = row[DailyOrderFactsTable.itemQuantity],
        createdAt = row[DailyOrderFactsTable.createdAt],
        updatedAt = row[DailyOrderFactsTable.updatedAt],
      )
  }
}

/** 商品日事实 Exposed 实现。 */
@Repository
class ExposedDailyProductFactRepositoryImpl(
  database: Database,
) : ExposedDailyFactRepository<DailyProductFactModel>(
    database,
    DailyProductFactsTable,
    DailyProductFactsTable.id,
    DailyProductFactsTable.statDate,
    DailyProductFactsTable.createdAt,
    DailyProductFactsTable.updatedAt,
    ::toProductFactModel,
  ),
  DailyProductFactRepository {
  override fun writeColumns(
    stmt: InsertStatement<Number>,
    entity: DailyProductFactModel,
  ) {
    stmt[DailyProductFactsTable.status] = entity.status
    stmt[DailyProductFactsTable.productId] = entity.productId
    stmt[DailyProductFactsTable.quantity] = entity.quantity
    stmt[DailyProductFactsTable.revenueCents] = entity.revenueCents
    stmt[DailyProductFactsTable.orderCount] = entity.orderCount
  }

  override fun statDateOf(entity: DailyProductFactModel): LocalDate = entity.statDate

  companion object {
    fun toProductFactModel(row: ResultRow): DailyProductFactModel =
      DailyProductFactModel(
        id = row[DailyProductFactsTable.id],
        statDate = row[DailyProductFactsTable.statDate],
        status = row[DailyProductFactsTable.status],
        productId = row[DailyProductFactsTable.productId],
        quantity = row[DailyProductFactsTable.quantity],
        revenueCents = row[DailyProductFactsTable.revenueCents],
        orderCount = row[DailyProductFactsTable.orderCount],
        createdAt = row[DailyProductFactsTable.createdAt],
        updatedAt = row[DailyProductFactsTable.updatedAt],
      )
  }
}

/** 用户日事实 Exposed 实现。 */
@Repository
class ExposedDailyUserFactRepositoryImpl(
  database: Database,
) : ExposedDailyFactRepository<DailyUserFactModel>(
    database,
    DailyUserFactsTable,
    DailyUserFactsTable.id,
    DailyUserFactsTable.statDate,
    DailyUserFactsTable.createdAt,
    DailyUserFactsTable.updatedAt,
    ::toUserFactModel,
  ),
  DailyUserFactRepository {
  override fun writeColumns(
    stmt: InsertStatement<Number>,
    entity: DailyUserFactModel,
  ) {
    stmt[DailyUserFactsTable.newUserCount] = entity.newUserCount
    stmt[DailyUserFactsTable.loginUserCount] = entity.loginUserCount
  }

  override fun statDateOf(entity: DailyUserFactModel): LocalDate = entity.statDate

  companion object {
    fun toUserFactModel(row: ResultRow): DailyUserFactModel =
      DailyUserFactModel(
        id = row[DailyUserFactsTable.id],
        statDate = row[DailyUserFactsTable.statDate],
        newUserCount = row[DailyUserFactsTable.newUserCount],
        loginUserCount = row[DailyUserFactsTable.loginUserCount],
        createdAt = row[DailyUserFactsTable.createdAt],
        updatedAt = row[DailyUserFactsTable.updatedAt],
      )
  }
}

/** 退款日事实 Exposed 实现。 */
@Repository
class ExposedDailyRefundFactRepositoryImpl(
  database: Database,
) : ExposedDailyFactRepository<DailyRefundFactModel>(
    database,
    DailyRefundFactsTable,
    DailyRefundFactsTable.id,
    DailyRefundFactsTable.statDate,
    DailyRefundFactsTable.createdAt,
    DailyRefundFactsTable.updatedAt,
    ::toRefundFactModel,
  ),
  DailyRefundFactRepository {
  override fun writeColumns(
    stmt: InsertStatement<Number>,
    entity: DailyRefundFactModel,
  ) {
    stmt[DailyRefundFactsTable.status] = entity.status
    stmt[DailyRefundFactsTable.refundType] = entity.refundType
    stmt[DailyRefundFactsTable.refundCount] = entity.refundCount
    stmt[DailyRefundFactsTable.refundAmountCents] = entity.refundAmountCents
  }

  override fun statDateOf(entity: DailyRefundFactModel): LocalDate = entity.statDate

  companion object {
    fun toRefundFactModel(row: ResultRow): DailyRefundFactModel =
      DailyRefundFactModel(
        id = row[DailyRefundFactsTable.id],
        statDate = row[DailyRefundFactsTable.statDate],
        status = row[DailyRefundFactsTable.status],
        refundType = row[DailyRefundFactsTable.refundType],
        refundCount = row[DailyRefundFactsTable.refundCount],
        refundAmountCents = row[DailyRefundFactsTable.refundAmountCents],
        createdAt = row[DailyRefundFactsTable.createdAt],
        updatedAt = row[DailyRefundFactsTable.updatedAt],
      )
  }
}

/** 已完成营收日事实 Exposed 实现。 */
@Repository
class ExposedDailyRevenueFactRepositoryImpl(
  database: Database,
) : ExposedDailyFactRepository<DailyRevenueFactModel>(
    database,
    DailyRevenueFactsTable,
    DailyRevenueFactsTable.id,
    DailyRevenueFactsTable.statDate,
    DailyRevenueFactsTable.createdAt,
    DailyRevenueFactsTable.updatedAt,
    ::toRevenueFactModel,
  ),
  DailyRevenueFactRepository {
  override fun writeColumns(
    stmt: InsertStatement<Number>,
    entity: DailyRevenueFactModel,
  ) {
    stmt[DailyRevenueFactsTable.orderCount] = entity.orderCount
    stmt[DailyRevenueFactsTable.amountCents] = entity.amountCents
  }

  override fun statDateOf(entity: DailyRevenueFactModel): LocalDate = entity.statDate

  companion object {
    fun toRevenueFactModel(row: ResultRow): DailyRevenueFactModel =
      DailyRevenueFactModel(
        id = row[DailyRevenueFactsTable.id],
        statDate = row[DailyRevenueFactsTable.statDate],
        orderCount = row[DailyRevenueFactsTable.orderCount],
        amountCents = row[DailyRevenueFactsTable.amountCents],
        createdAt = row[DailyRevenueFactsTable.createdAt],
        updatedAt = row[DailyRevenueFactsTable.updatedAt],
      )
  }
}

/** 营销日事实 Exposed 实现。 */
@Repository
class ExposedDailyMarketingFactRepositoryImpl(
  database: Database,
) : ExposedDailyFactRepository<DailyMarketingFactModel>(
    database,
    DailyMarketingFactsTable,
    DailyMarketingFactsTable.id,
    DailyMarketingFactsTable.statDate,
    DailyMarketingFactsTable.createdAt,
    DailyMarketingFactsTable.updatedAt,
    ::toMarketingFactModel,
  ),
  DailyMarketingFactRepository {
  override fun writeColumns(
    stmt: InsertStatement<Number>,
    entity: DailyMarketingFactModel,
  ) {
    stmt[DailyMarketingFactsTable.marketingOrderCount] = entity.marketingOrderCount
    stmt[DailyMarketingFactsTable.couponBenefitCents] = entity.couponBenefitCents
    stmt[DailyMarketingFactsTable.promotionBenefitCents] = entity.promotionBenefitCents
    stmt[DailyMarketingFactsTable.firstOrderBenefitCents] = entity.firstOrderBenefitCents
    stmt[DailyMarketingFactsTable.couponRedeemedCount] = entity.couponRedeemedCount
    stmt[DailyMarketingFactsTable.ticketSalesCount] = entity.ticketSalesCount
    stmt[DailyMarketingFactsTable.ticketSalesRevenueCents] = entity.ticketSalesRevenueCents
    stmt[DailyMarketingFactsTable.ticketRedeemedCount] = entity.ticketRedeemedCount
    stmt[DailyMarketingFactsTable.giftQuantity] = entity.giftQuantity
  }

  override fun statDateOf(entity: DailyMarketingFactModel): LocalDate = entity.statDate

  companion object {
    fun toMarketingFactModel(row: ResultRow): DailyMarketingFactModel =
      DailyMarketingFactModel(
        id = row[DailyMarketingFactsTable.id],
        statDate = row[DailyMarketingFactsTable.statDate],
        marketingOrderCount = row[DailyMarketingFactsTable.marketingOrderCount],
        couponBenefitCents = row[DailyMarketingFactsTable.couponBenefitCents],
        promotionBenefitCents = row[DailyMarketingFactsTable.promotionBenefitCents],
        firstOrderBenefitCents = row[DailyMarketingFactsTable.firstOrderBenefitCents],
        couponRedeemedCount = row[DailyMarketingFactsTable.couponRedeemedCount],
        ticketSalesCount = row[DailyMarketingFactsTable.ticketSalesCount],
        ticketSalesRevenueCents = row[DailyMarketingFactsTable.ticketSalesRevenueCents],
        ticketRedeemedCount = row[DailyMarketingFactsTable.ticketRedeemedCount],
        giftQuantity = row[DailyMarketingFactsTable.giftQuantity],
        createdAt = row[DailyMarketingFactsTable.createdAt],
        updatedAt = row[DailyMarketingFactsTable.updatedAt],
      )
  }
}

/** 营销使用日事实 Exposed 实现。 */
@Repository
class ExposedDailyMarketingUsageFactRepositoryImpl(
  database: Database,
) : ExposedDailyFactRepository<DailyMarketingUsageFactModel>(
    database,
    DailyMarketingUsageFactsTable,
    DailyMarketingUsageFactsTable.id,
    DailyMarketingUsageFactsTable.statDate,
    DailyMarketingUsageFactsTable.createdAt,
    DailyMarketingUsageFactsTable.updatedAt,
    ::toMarketingUsageFactModel,
  ),
  DailyMarketingUsageFactRepository {
  override fun writeColumns(
    stmt: InsertStatement<Number>,
    entity: DailyMarketingUsageFactModel,
  ) {
    stmt[DailyMarketingUsageFactsTable.kind] = entity.kind
    stmt[DailyMarketingUsageFactsTable.refId] = entity.refId
    stmt[DailyMarketingUsageFactsTable.orderCount] = entity.orderCount
    stmt[DailyMarketingUsageFactsTable.benefitCents] = entity.benefitCents
    stmt[DailyMarketingUsageFactsTable.usageCount] = entity.usageCount
    stmt[DailyMarketingUsageFactsTable.ticketSalesCount] = entity.ticketSalesCount
    stmt[DailyMarketingUsageFactsTable.ticketSalesRevenueCents] = entity.ticketSalesRevenueCents
    stmt[DailyMarketingUsageFactsTable.ticketRedeemedCount] = entity.ticketRedeemedCount
  }

  override fun statDateOf(entity: DailyMarketingUsageFactModel): LocalDate = entity.statDate

  companion object {
    fun toMarketingUsageFactModel(row: ResultRow): DailyMarketingUsageFactModel =
      DailyMarketingUsageFactModel(
        id = row[DailyMarketingUsageFactsTable.id],
        statDate = row[DailyMarketingUsageFactsTable.statDate],
        kind = row[DailyMarketingUsageFactsTable.kind],
        refId = row[DailyMarketingUsageFactsTable.refId],
        orderCount = row[DailyMarketingUsageFactsTable.orderCount],
        benefitCents = row[DailyMarketingUsageFactsTable.benefitCents],
        usageCount = row[DailyMarketingUsageFactsTable.usageCount],
        ticketSalesCount = row[DailyMarketingUsageFactsTable.ticketSalesCount],
        ticketSalesRevenueCents = row[DailyMarketingUsageFactsTable.ticketSalesRevenueCents],
        ticketRedeemedCount = row[DailyMarketingUsageFactsTable.ticketRedeemedCount],
        createdAt = row[DailyMarketingUsageFactsTable.createdAt],
        updatedAt = row[DailyMarketingUsageFactsTable.updatedAt],
      )
  }
}
