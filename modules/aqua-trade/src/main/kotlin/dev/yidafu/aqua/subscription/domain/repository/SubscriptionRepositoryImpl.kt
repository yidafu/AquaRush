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

package dev.yidafu.aqua.subscription.domain.repository

import dev.yidafu.aqua.api.dto.SubscriptionListQuery
import dev.yidafu.aqua.common.domain.model.SubscriptionItemModel
import dev.yidafu.aqua.common.domain.model.SubscriptionModel
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionCycleType
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionStatus
import dev.yidafu.aqua.common.domain.table.SubscriptionItemsTable
import dev.yidafu.aqua.common.domain.table.SubscriptionsTable
import dev.yidafu.aqua.common.domain.table.UsersTable
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

/**
 * 周期订购计划仓储实现(Exposed DSL,口径与 OrderRepositoryImpl 一致:
 * 同一事务内 count + content,枚举列存 .name、读 fromValue,金额 Long 分)。
 */
@Repository
class SubscriptionRepositoryImpl(
  private val database: Database,
) : SubscriptionRepository {
  private fun toModel(row: ResultRow): SubscriptionModel =
    SubscriptionModel(
      id = row[SubscriptionsTable.id],
      userId = row[SubscriptionsTable.userId],
      addressId = row[SubscriptionsTable.addressId],
      workerId = row[SubscriptionsTable.workerId],
      cycleType = SubscriptionCycleType.fromValue(row[SubscriptionsTable.cycleType]),
      weekdays = row[SubscriptionsTable.weekdays],
      monthDay = row[SubscriptionsTable.monthDay],
      timeSlot = DeliveryTimeSlot.fromValue(row[SubscriptionsTable.timeSlot]),
      startDate = row[SubscriptionsTable.startDate],
      endDate = row[SubscriptionsTable.endDate],
      maxPeriods = row[SubscriptionsTable.maxPeriods],
      executedPeriods = row[SubscriptionsTable.executedPeriods],
      nextRunDate = row[SubscriptionsTable.nextRunDate],
      status = SubscriptionStatus.fromValue(row[SubscriptionsTable.status]),
      remark = row[SubscriptionsTable.remark],
      lastError = row[SubscriptionsTable.lastError],
      lastRunAt = row[SubscriptionsTable.lastRunAt],
      createdBy = row[SubscriptionsTable.createdBy],
      createdAt = row[SubscriptionsTable.createdAt],
      updatedAt = row[SubscriptionsTable.updatedAt],
    )

  private fun toItemModel(row: ResultRow): SubscriptionItemModel =
    SubscriptionItemModel(
      id = row[SubscriptionItemsTable.id],
      subscriptionId = row[SubscriptionItemsTable.subscriptionId],
      productId = row[SubscriptionItemsTable.productId],
      quantity = row[SubscriptionItemsTable.quantity],
      createdAt = row[SubscriptionItemsTable.createdAt],
      updatedAt = row[SubscriptionItemsTable.updatedAt],
    )

  override fun save(entity: SubscriptionModel): SubscriptionModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      if (entity.id != null && existsById(id)) {
        SubscriptionsTable
          .update({ SubscriptionsTable.id eq id }) {
            it[userId] = entity.userId
            it[addressId] = entity.addressId
            it[workerId] = entity.workerId
            it[cycleType] = entity.cycleType.name
            it[weekdays] = entity.weekdays
            it[monthDay] = entity.monthDay
            it[timeSlot] = entity.timeSlot.name
            it[startDate] = entity.startDate
            it[endDate] = entity.endDate
            it[maxPeriods] = entity.maxPeriods
            it[executedPeriods] = entity.executedPeriods
            it[nextRunDate] = entity.nextRunDate
            it[status] = entity.status.name
            it[remark] = entity.remark
            it[lastError] = entity.lastError
            it[lastRunAt] = entity.lastRunAt
            it[createdBy] = entity.createdBy
            it[updatedAt] = LocalDateTime.now()
          }
      } else {
        SubscriptionsTable
          .insert {
            it[this.id] = id
            it[userId] = entity.userId
            it[addressId] = entity.addressId
            it[workerId] = entity.workerId
            it[cycleType] = entity.cycleType.name
            it[weekdays] = entity.weekdays
            it[monthDay] = entity.monthDay
            it[timeSlot] = entity.timeSlot.name
            it[startDate] = entity.startDate
            it[endDate] = entity.endDate
            it[maxPeriods] = entity.maxPeriods
            it[executedPeriods] = entity.executedPeriods
            it[nextRunDate] = entity.nextRunDate
            it[status] = entity.status.name
            it[remark] = entity.remark
            it[lastError] = entity.lastError
            it[lastRunAt] = entity.lastRunAt
            it[createdBy] = entity.createdBy
            it[createdAt] = entity.createdAt
            it[updatedAt] = entity.updatedAt
          }
      }
      entity
    }

  /** 存在性判定(save 更新分支专用;避免先查后写两趟) */
  private fun existsById(id: Long): Boolean =
    SubscriptionsTable
      .selectAll()
      .andWhere { SubscriptionsTable.id eq id }
      .any()

  override fun findById(id: Long): SubscriptionModel? =
    transaction(database) {
      SubscriptionsTable
        .selectAll()
        .andWhere { SubscriptionsTable.id eq id }
        .firstOrNull()
    }?.let(::toModel)

  override fun search(query: SubscriptionListQuery): Page<SubscriptionModel> =
    transaction(database) {
      var after =
        SubscriptionsTable
          .selectAll()
          .andWhere { SubscriptionsTable.id neq 0L }

      query.keyword?.trim()?.takeIf { it.isNotEmpty() }?.let { kw ->
        // 关键字匹配用户昵称/手机号(用户子查询;UsersTable select 自动过滤软删)
        val userSub =
          UsersTable
            .select(UsersTable.id)
            .andWhere {
              (UsersTable.nickname like "%$kw%") or (UsersTable.phone like "%$kw%")
            }
        after = after.andWhere { SubscriptionsTable.userId inSubQuery userSub }
      }
      query.status?.trim()?.takeIf { it.isNotEmpty() }?.let { status ->
        SubscriptionStatus.fromValue(status) // 非法状态值快速失败
        after = after.andWhere { SubscriptionsTable.status eq status }
      }
      query.nextRunFrom?.let { from ->
        after = after.andWhere { SubscriptionsTable.nextRunDate greaterEq parseDate(from, "下次配送开始日期") }
      }
      query.nextRunTo?.let { to ->
        after = after.andWhere { SubscriptionsTable.nextRunDate lessEq parseDate(to, "下次配送结束日期") }
      }

      val total = after.count()
      val content =
        after
          .orderBy(SubscriptionsTable.createdAt to SortOrder.DESC, SubscriptionsTable.id to SortOrder.DESC)
          .limit(query.size.coerceIn(1, 100))
          .offset(query.page.coerceAtLeast(0).toLong() * query.size)
          .map(::toModel)
      PageImpl(content, PageRequest.of(query.page.coerceAtLeast(0), query.size), total)
    }

  override fun findDue(
    afterId: Long,
    dueOnOrBefore: LocalDate,
    limit: Int,
  ): List<SubscriptionModel> =
    transaction(database) {
      SubscriptionsTable
        .selectAll()
        .andWhere { SubscriptionsTable.status eq SubscriptionStatus.ACTIVE.name }
        .andWhere { SubscriptionsTable.nextRunDate lessEq dueOnOrBefore }
        .andWhere { SubscriptionsTable.id greater afterId }
        .orderBy(SubscriptionsTable.id to SortOrder.ASC)
        .limit(limit)
        .map(::toModel)
    }

  override fun saveItems(items: List<SubscriptionItemModel>) {
    if (items.isEmpty()) return
    transaction(database) {
      items.forEach { item ->
        val id = item.id ?: SnowflakeIdGenerator.getInstance().generate().also { item.id = it }
        SubscriptionItemsTable
          .insert {
            it[this.id] = id
            it[subscriptionId] = item.subscriptionId
            it[productId] = item.productId
            it[quantity] = item.quantity
            it[createdAt] = item.createdAt
            it[updatedAt] = item.updatedAt
          }
      }
    }
  }

  override fun findItemsBySubscriptionId(subscriptionId: Long): List<SubscriptionItemModel> =
    transaction(database) {
      SubscriptionItemsTable
        .selectAll()
        .andWhere { SubscriptionItemsTable.subscriptionId eq subscriptionId }
        .orderBy(SubscriptionItemsTable.id to SortOrder.ASC)
        .map(::toItemModel)
    }

  private fun parseDate(
    value: String,
    label: String,
  ): LocalDate =
    try {
      LocalDate.parse(value)
    } catch (e: DateTimeParseException) {
      throw BadRequestException("${label}格式应为yyyy-MM-dd: $value")
    }
}
