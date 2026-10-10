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

package dev.yidafu.aqua.order.domain.repository

import dev.yidafu.aqua.api.dto.MarketingUsageItemRow
import dev.yidafu.aqua.api.dto.MarketingUsageKind
import dev.yidafu.aqua.api.dto.MarketingUsageOrderCriteria
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.PaymentMethod
import dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.domain.model.enums.PriceSource
import dev.yidafu.aqua.common.domain.table.OrderGiftsTable
import dev.yidafu.aqua.common.domain.table.OrderItemsTable
import dev.yidafu.aqua.common.domain.table.OrdersTable
import dev.yidafu.aqua.common.domain.table.UserCouponsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.javatime.JavaLocalDateTimeColumnType
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
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/**
 * 订单仓储实现(JPA→Exposed 迁移 P4:原 957 行 QueryDSL 实现整体重写为 Exposed DSL)。
 *
 * 口径约定(与原实现逐方法对照):
 * - 读侧入口(selectAll/select)一律自动过滤 is_deleted(等价原 JPA @SoftDelete 附加谓词);
 * - 枚举列存 .name、读 fromValue(显式 when 穷举);金额 Long 分;
 * - 分页 count 与 content 同一事务;业务排序 + id 破平(原 JPA 无序处已在 KDoc 注明);
 * - 「已送达」口径锚点 = COALESCE(delivery_confirmed_at, completed_at),状态含
 *   AWAITING_CONFIRMATION + COMPLETED(R-ORD-11 送水工业绩,历史 completed_at 回退);
 * - cancel/confirm 原子方法为单条条件 UPDATE,行数=1 成功 / 0 失败(乐观锁 CAS);
 * - 原 JPA EAGER 关联字段 user/address/deliveryWorker 不装载(null),列值经
 *   userId/addressId/deliveryWorkerId 提供,由服务层/Resolver 联查补全。
 */
@Repository
class OrderRepositoryImpl(
  private val database: Database,
) : OrderRepository {
  private fun toModel(row: ResultRow): OrderModel =
    OrderModel(
      id = row[OrdersTable.id],
      orderNo = row[OrdersTable.orderNo],
      userId = row[OrdersTable.userId],
      clientRequestId = row[OrdersTable.clientRequestId],
      deliveryTimeSlot = row[OrdersTable.deliveryTimeSlot]?.let { DeliveryTimeSlot.fromValue(it) },
      amountCents = row[OrdersTable.totalAmountCents],
      deliveryFeeCents = row[OrdersTable.deliveryFeeCents],
      unitPriceCents = row[OrdersTable.unitPriceCents],
      priceSource = row[OrdersTable.priceSource]?.let { PriceSource.fromValue(it) },
      promotionActivityId = row[OrdersTable.promotionActivityId],
      promotionActivityName = row[OrdersTable.promotionActivityName],
      promotionDiscountCents = row[OrdersTable.promotionDiscountCents],
      couponId = row[OrdersTable.couponId],
      couponName = row[OrdersTable.couponName],
      couponDiscountCents = row[OrdersTable.couponDiscountCents],
      ticketRedeemCount = row[OrdersTable.ticketRedeemCount],
      ticketDiscountCents = row[OrdersTable.ticketDiscountCents],
      firstOrderActivityId = row[OrdersTable.firstOrderActivityId],
      firstOrderActivityName = row[OrdersTable.firstOrderActivityName],
      firstOrderDiscountCents = row[OrdersTable.firstOrderDiscountCents],
      addressId = row[OrdersTable.addressId],
      status = OrderModelStatus.fromValue(row[OrdersTable.status]),
      paymentMethod = row[OrdersTable.paymentMethod]?.let { PaymentMethod.fromValue(it) },
      paymentTransactionId = row[OrdersTable.paymentTransactionId],
      paymentTime = row[OrdersTable.paidAt],
      deliveryWorkerId = row[OrdersTable.deliveryWorkerId],
      deliveryPhotos = row[OrdersTable.deliveryPhotos],
      deliveryPhotoReviewStatus =
        row[OrdersTable.deliveryPhotoReviewStatus]?.let { DeliveryPhotoReviewStatus.fromValue(it) },
      deliveryPhotoReviewedAt = row[OrdersTable.deliveryPhotoReviewedAt],
      deliveryPhotoReviewedBy = row[OrdersTable.deliveryPhotoReviewedBy],
      deliveryPhotoReviewRemark = row[OrdersTable.deliveryPhotoReviewRemark],
      depositCents = row[OrdersTable.depositAmountCents],
      isSelfCollect = row[OrdersTable.isSelfCollect],
      paymentType = row[OrdersTable.paymentType]?.let { PaymentType.fromValue(it) },
      deliveryStartedAt = row[OrdersTable.deliveryStartedAt],
      deliveryConfirmedAt = row[OrdersTable.deliveryConfirmedAt],
      completedAt = row[OrdersTable.completedAt],
      createdAt = row[OrdersTable.createdAt],
      updatedAt = row[OrdersTable.updatedAt],
      deletedAt = row[OrdersTable.deletedAt],
      deletedBy = row[OrdersTable.deletedBy],
      remark = row[OrdersTable.remark],
      deliveryRemark = row[OrdersTable.deliveryRemark],
      searchContent = row[OrdersTable.searchContent],
    )

  /** 从聚合 ResultRow 取 SUM/COUNT 数值并转 Long(表达式类型为 Number 族投影,空值回退 0)。 */
  private fun ResultRow.longOf(expr: Expression<*>): Long = (this[expr] as? Number)?.toLong() ?: 0L

  /**
   * 「已送达」时间锚点 `COALESCE(delivery_confirmed_at, completed_at)`
   * —— 原 QueryDSL `coalesce(...)` 表达式的 Exposed 等价物(两参,历史数据回退 completedAt)。
   */
  private class DeliveredAtExpr : ExpressionWithColumnType<LocalDateTime>() {
    override val columnType: IColumnType<LocalDateTime> = JavaLocalDateTimeColumnType()

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
      queryBuilder.append("COALESCE(")
      OrdersTable.deliveryConfirmedAt.toQueryBuilder(queryBuilder)
      queryBuilder.append(", ")
      OrdersTable.completedAt.toQueryBuilder(queryBuilder)
      queryBuilder.append(")")
    }
  }

  /** 「已送达」状态集合(待确认收货 + 已完成,R-ORD-11 送水工业绩口径)。 */
  private fun deliveredStatusNames(): List<String> = listOf(OrderModelStatus.AWAITING_CONFIRMATION.name, OrderModelStatus.COMPLETED.name)

  /** Pageable 排序 → Exposed 排序;原 JPA 无序时默认 id 升序破平(未知属性忽略)。 */
  private fun pageableOrderings(pageable: Pageable): List<Pair<Expression<*>, SortOrder>> {
    if (pageable.sort.isUnsorted) return listOf(OrdersTable.id to SortOrder.ASC)
    val orderings =
      pageable.sort.mapNotNull { order ->
        val sortOrder = if (order.isAscending) SortOrder.ASC else SortOrder.DESC
        when (order.property) {
          "id" -> OrdersTable.id to sortOrder
          "createdAt" -> OrdersTable.createdAt to sortOrder
          "updatedAt" -> OrdersTable.updatedAt to sortOrder
          "amountCents", "totalAmountCents", "total_amount_cents" -> OrdersTable.totalAmountCents to sortOrder
          else -> null
        }
      }
    return orderings.ifEmpty { listOf(OrdersTable.id to SortOrder.ASC) }
  }

  // ========================================================================
  // OrderRepository(简单派生查询,入口 selectAll() 自动过滤软删)
  // ========================================================================

  override fun findByOrderNo(orderNo: String): OrderModel? =
    transaction(database) {
      OrdersTable
        .selectAll()
        .andWhere { OrdersTable.orderNo eq orderNo }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByUserIdAndClientRequestId(
    userId: Long,
    clientRequestId: String,
  ): OrderModel? =
    transaction(database) {
      OrdersTable
        .selectAll()
        .andWhere { (OrdersTable.userId eq userId) and (OrdersTable.clientRequestId eq clientRequestId) }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByUserId(userId: Long): List<OrderModel> =
    transaction(database) {
      OrdersTable
        .selectAll()
        .andWhere { OrdersTable.userId eq userId }
        // 原 JPA 无序,按 id 升序破平
        .orderBy(OrdersTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByUserIdAndStatus(
    userId: Long,
    status: OrderModelStatus,
  ): List<OrderModel> =
    transaction(database) {
      OrdersTable
        .selectAll()
        .andWhere { (OrdersTable.userId eq userId) and (OrdersTable.status eq status.name) }
        // 原 JPA 无序,按 id 升序破平
        .orderBy(OrdersTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun existsByUserIdAndStatus(
    userId: Long,
    status: OrderModelStatus,
  ): Boolean =
    transaction(database) {
      OrdersTable
        .selectAll()
        .andWhere { (OrdersTable.userId eq userId) and (OrdersTable.status eq status.name) }
        .limit(1)
        .any()
    }

  override fun findByStatus(status: OrderModelStatus): List<OrderModel> =
    transaction(database) {
      OrdersTable
        .selectAll()
        .andWhere { OrdersTable.status eq status.name }
        // 原 JPA 无序,按 id 升序破平
        .orderBy(OrdersTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByStatusOrderByCreatedAtAsc(status: OrderModelStatus): List<OrderModel> =
    transaction(database) {
      OrdersTable
        .selectAll()
        .andWhere { OrdersTable.status eq status.name }
        .orderBy(OrdersTable.createdAt to SortOrder.ASC, OrdersTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByStatusAndDeliveryStartedAtBefore(
    status: OrderModelStatus,
    before: LocalDateTime,
    pageable: Pageable,
  ): Page<OrderModel> =
    transaction(database) {
      val condition = (OrdersTable.status eq status.name) and (OrdersTable.deliveryStartedAt less before)
      val content =
        OrdersTable
          .selectAll()
          .andWhere { condition }
          .orderBy(*pageableOrderings(pageable).toTypedArray())
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
          .map(::toModel)
      content to OrdersTable.selectAll().andWhere { condition }.count()
    }.let { (content, total) -> PageImpl(content, pageable, total) }

  override fun findByStatusAndDeliveryConfirmedAtBefore(
    status: OrderModelStatus,
    before: LocalDateTime,
    pageable: Pageable,
  ): Page<OrderModel> =
    transaction(database) {
      val condition = (OrdersTable.status eq status.name) and (OrdersTable.deliveryConfirmedAt less before)
      val content =
        OrdersTable
          .selectAll()
          .andWhere { condition }
          .orderBy(*pageableOrderings(pageable).toTypedArray())
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
          .map(::toModel)
      content to OrdersTable.selectAll().andWhere { condition }.count()
    }.let { (content, total) -> PageImpl(content, pageable, total) }

  override fun findByDeliveryWorkerIdOrderByCreatedAtDesc(deliveryWorkerId: Long): List<OrderModel> =
    transaction(database) {
      OrdersTable
        .selectAll()
        .andWhere { OrdersTable.deliveryWorkerId eq deliveryWorkerId }
        .orderBy(OrdersTable.createdAt to SortOrder.DESC, OrdersTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByAddressId(addressId: Long): List<OrderModel> =
    transaction(database) {
      OrdersTable
        .selectAll()
        .andWhere { OrdersTable.addressId eq addressId }
        // 原 JPA 无序,按 id 升序破平
        .orderBy(OrdersTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun countByStatus(status: OrderModelStatus): Long =
    transaction(database) {
      OrdersTable
        .selectAll()
        .andWhere { OrdersTable.status eq status.name }
        .count()
    }

  override fun findById(id: Long): Optional<OrderModel> =
    Optional.ofNullable(
      transaction(database) {
        OrdersTable
          .selectAll()
          .andWhere { OrdersTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun findAllById(ids: Iterable<Long>): List<OrderModel> =
    transaction(database) {
      OrdersTable
        .selectAll()
        .andWhere { OrdersTable.id inList ids }
        .orderBy(OrdersTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun save(entity: OrderModel): OrderModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val exists =
        OrdersTable
          .selectAllIncludingDeleted()
          .where { OrdersTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        // 对齐原 JPA merge + @PreUpdate:业务列全量覆盖,createdAt 不可变,updatedAt=now
        OrdersTable.update({ OrdersTable.id eq id }) {
          it[orderNo] = entity.orderNo
          it[userId] = entity.userId
          it[clientRequestId] = entity.clientRequestId
          it[deliveryTimeSlot] = entity.deliveryTimeSlot?.name
          it[totalAmountCents] = entity.amountCents
          it[deliveryFeeCents] = entity.deliveryFeeCents
          it[unitPriceCents] = entity.unitPriceCents
          it[priceSource] = entity.priceSource?.name
          it[promotionActivityId] = entity.promotionActivityId
          it[promotionActivityName] = entity.promotionActivityName
          it[promotionDiscountCents] = entity.promotionDiscountCents
          it[couponId] = entity.couponId
          it[couponName] = entity.couponName
          it[couponDiscountCents] = entity.couponDiscountCents
          it[ticketRedeemCount] = entity.ticketRedeemCount
          it[ticketDiscountCents] = entity.ticketDiscountCents
          it[firstOrderActivityId] = entity.firstOrderActivityId
          it[firstOrderActivityName] = entity.firstOrderActivityName
          it[firstOrderDiscountCents] = entity.firstOrderDiscountCents
          it[addressId] = entity.addressId
          it[status] = entity.status.name
          it[paymentMethod] = entity.paymentMethod?.name
          it[paymentTransactionId] = entity.paymentTransactionId
          it[paidAt] = entity.paymentTime
          it[deliveryWorkerId] = entity.deliveryWorkerId
          it[deliveryPhotos] = entity.deliveryPhotos
          it[deliveryPhotoReviewStatus] = entity.deliveryPhotoReviewStatus?.name
          it[deliveryPhotoReviewedAt] = entity.deliveryPhotoReviewedAt
          it[deliveryPhotoReviewedBy] = entity.deliveryPhotoReviewedBy
          it[deliveryPhotoReviewRemark] = entity.deliveryPhotoReviewRemark
          it[depositAmountCents] = entity.depositCents
          it[isSelfCollect] = entity.isSelfCollect
          it[paymentType] = entity.paymentType?.name
          it[deliveryStartedAt] = entity.deliveryStartedAt
          it[deliveryConfirmedAt] = entity.deliveryConfirmedAt
          it[completedAt] = entity.completedAt
          it[remark] = entity.remark
          it[deliveryRemark] = entity.deliveryRemark
          it[searchContent] = entity.searchContent
          it[updatedAt] = LocalDateTime.now()
        }
      } else {
        OrdersTable.insert {
          it[OrdersTable.id] = id
          it[orderNo] = entity.orderNo
          it[userId] = entity.userId
          it[clientRequestId] = entity.clientRequestId
          it[deliveryTimeSlot] = entity.deliveryTimeSlot?.name
          it[totalAmountCents] = entity.amountCents
          it[deliveryFeeCents] = entity.deliveryFeeCents
          it[unitPriceCents] = entity.unitPriceCents
          it[priceSource] = entity.priceSource?.name
          it[promotionActivityId] = entity.promotionActivityId
          it[promotionActivityName] = entity.promotionActivityName
          it[promotionDiscountCents] = entity.promotionDiscountCents
          it[couponId] = entity.couponId
          it[couponName] = entity.couponName
          it[couponDiscountCents] = entity.couponDiscountCents
          it[ticketRedeemCount] = entity.ticketRedeemCount
          it[ticketDiscountCents] = entity.ticketDiscountCents
          it[firstOrderActivityId] = entity.firstOrderActivityId
          it[firstOrderActivityName] = entity.firstOrderActivityName
          it[firstOrderDiscountCents] = entity.firstOrderDiscountCents
          it[addressId] = entity.addressId
          it[status] = entity.status.name
          it[paymentMethod] = entity.paymentMethod?.name
          it[paymentTransactionId] = entity.paymentTransactionId
          it[paidAt] = entity.paymentTime
          it[deliveryWorkerId] = entity.deliveryWorkerId
          it[deliveryPhotos] = entity.deliveryPhotos
          it[deliveryPhotoReviewStatus] = entity.deliveryPhotoReviewStatus?.name
          it[deliveryPhotoReviewedAt] = entity.deliveryPhotoReviewedAt
          it[deliveryPhotoReviewedBy] = entity.deliveryPhotoReviewedBy
          it[deliveryPhotoReviewRemark] = entity.deliveryPhotoReviewRemark
          it[depositAmountCents] = entity.depositCents
          it[isSelfCollect] = entity.isSelfCollect
          it[paymentType] = entity.paymentType?.name
          it[deliveryStartedAt] = entity.deliveryStartedAt
          it[deliveryConfirmedAt] = entity.deliveryConfirmedAt
          it[completedAt] = entity.completedAt
          it[OrdersTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
          it[deletedAt] = entity.deletedAt
          it[deletedBy] = entity.deletedBy
          it[remark] = entity.remark
          it[deliveryRemark] = entity.deliveryRemark
          it[searchContent] = entity.searchContent
        }
      }
      entity
    }

  // ========================================================================
  // 分页查询(count 与 content 同一事务)
  // ========================================================================

  override fun findOrdersPaginated(
    keyword: String?,
    status: OrderModelStatus?,
    userId: Long?,
    deliveryWorkerId: Long?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    minAmount: Long?,
    maxAmount: Long?,
    page: Int,
    size: Int,
    sortField: String,
    sortDirection: String,
  ): Page<OrderModel> {
    var condition: Op<Boolean> = Op.TRUE
    keyword?.let { keywordVal ->
      // 订单号大小写不敏感包含(与原 containsIgnoreCase 一致)
      condition = condition and (OrdersTable.orderNo.lowerCase() like "%${keywordVal.lowercase()}%")
    }
    status?.let { condition = condition and (OrdersTable.status eq it.name) }
    userId?.let { condition = condition and (OrdersTable.userId eq it) }
    deliveryWorkerId?.let { condition = condition and (OrdersTable.deliveryWorkerId eq it) }
    if (startDate != null && endDate != null) {
      condition = condition and (OrdersTable.createdAt greaterEq startDate) and (OrdersTable.createdAt less endDate)
    }
    minAmount?.let { condition = condition and (OrdersTable.totalAmountCents greaterEq it) }
    maxAmount?.let { condition = condition and (OrdersTable.totalAmountCents lessEq it) }

    // 排序白名单(与原实现一致):totalAmount/updatedAt → 其余回退 createdAt;方向跟随参数;+ id 破平
    val ascending = sortDirection.equals("asc", ignoreCase = true)
    val sortProperty =
      when (sortField.lowercase()) {
        "totalamount", "total_amount" -> "amountCents"
        "updatedat", "updated_at" -> "updatedAt"
        else -> "createdAt"
      }
    val sortColumn =
      when (sortProperty) {
        "amountCents" -> OrdersTable.totalAmountCents
        "updatedAt" -> OrdersTable.updatedAt
        else -> OrdersTable.createdAt
      }
    val direction = if (ascending) SortOrder.ASC else SortOrder.DESC
    val springSort =
      Sort.by(
        Sort.Order(
          if (ascending) Sort.Direction.ASC else Sort.Direction.DESC,
          sortProperty,
        ),
      )
    val pageable = PageRequest.of(page, size, springSort)

    return transaction(database) {
      val content =
        OrdersTable
          .selectAll()
          .andWhere { condition }
          .orderBy(sortColumn to direction, OrdersTable.id to SortOrder.ASC)
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
          .map(::toModel)
      content to OrdersTable.selectAll().andWhere { condition }.count()
    }.let { (content, total) -> PageImpl(content, pageable, total) }
  }

  override fun findByDeliveryWorkerIdAndStatusIn(
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>,
    keyword: String?,
    page: Int,
    size: Int,
  ): Page<OrderModel> {
    // keyword 在原 QueryDSL 实现即为注释掉的死代码,保留参数不参与过滤(行为不变)
    var condition: Op<Boolean> = OrdersTable.status inList statuses.map { it.name }
    deliveryWorkerId?.let { condition = condition and (OrdersTable.deliveryWorkerId eq it) }
    val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

    return transaction(database) {
      val content =
        OrdersTable
          .selectAll()
          .andWhere { condition }
          .orderBy(OrdersTable.createdAt to SortOrder.DESC, OrdersTable.id to SortOrder.ASC)
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
          .map(::toModel)
      content to OrdersTable.selectAll().andWhere { condition }.count()
    }.let { (content, total) -> PageImpl(content, pageable, total) }
  }

  override fun findUserOrdersPaginated(
    userId: Long,
    statuses: List<OrderModelStatus>?,
    page: Int,
    size: Int,
  ): Page<OrderModel> {
    var condition: Op<Boolean> = OrdersTable.userId eq userId
    // statuses 为空 = 全部状态(与原 takeIf { isNotEmpty } 一致)
    statuses?.takeIf { it.isNotEmpty() }?.let {
      condition = condition and (OrdersTable.status inList it.map { s -> s.name })
    }
    val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

    return transaction(database) {
      val content =
        OrdersTable
          .selectAll()
          .andWhere { condition }
          .orderBy(OrdersTable.createdAt to SortOrder.DESC, OrdersTable.id to SortOrder.ASC)
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
          .map(::toModel)
      content to OrdersTable.selectAll().andWhere { condition }.count()
    }.let { (content, total) -> PageImpl(content, pageable, total) }
  }

  // ========================================================================
  // 日期窗口统计(两种窗口口径与原实现逐位一致)
  // - 半开 [start, end):goe/lt —— countOrdersByDateRange / sumAmountCentsByStatusAndDateRange
  // - 严格开 (start, end):gt/lt —— orderStatistics 各方法 / findBy*Between
  // ========================================================================

  /** 金额合计(分):SUM(total_amount_cents),空结果回退 0;条件构造与读行都在事务内。 */
  private fun sumAmountWhere(buildCondition: () -> Op<Boolean>): Long =
    transaction(database) {
      val sumExpr = OrdersTable.totalAmountCents.sum()
      OrdersTable
        .select(sumExpr)
        .andWhere { buildCondition() }
        .firstOrNull()
        ?.longOf(sumExpr)
        ?: 0L
    }

  /** 计数:COUNT(*),入口 selectAll() 自动过滤软删;条件构造在事务内。 */
  private fun countWhere(buildCondition: () -> Op<Boolean>): Long =
    transaction(database) {
      OrdersTable
        .selectAll()
        .andWhere { buildCondition() }
        .count()
    }

  override fun countOrdersByDateRange(
    startOfDay: LocalDateTime,
    endOfDay: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): Long {
    var condition: Op<Boolean> =
      (OrdersTable.createdAt greaterEq startOfDay) and (OrdersTable.createdAt less endOfDay)
    deliveryWorkerId?.let { condition = condition and (OrdersTable.deliveryWorkerId eq it) }
    statuses?.let { condition = condition and (OrdersTable.status inList it.map { s -> s.name }) }
    return countWhere { condition }
  }

  override fun sumAmountCentsByStatusAndDateRange(
    statuses: List<OrderModelStatus>,
    startOfDay: LocalDateTime,
    endOfDay: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long {
    var condition: Op<Boolean> =
      (OrdersTable.createdAt greaterEq startOfDay) and (OrdersTable.createdAt less endOfDay)
    deliveryWorkerId?.let { condition = condition and (OrdersTable.deliveryWorkerId eq it) }
    condition = condition and (OrdersTable.status inList statuses.map { it.name })
    return sumAmountWhere { condition }
  }

  // ========================================================================
  // 「已送达」口径聚合(R-ORD-11:COALESCE(delivery_confirmed_at, completed_at) 时间锚点)
  // ========================================================================

  private fun deliveredWindowCondition(
    start: LocalDateTime,
    end: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Op<Boolean> {
    val deliveredAt = DeliveredAtExpr()
    var condition: Op<Boolean> =
      (OrdersTable.status inList deliveredStatusNames()) and
        (deliveredAt greaterEq start) and (deliveredAt less end)
    deliveryWorkerId?.let { condition = condition and (OrdersTable.deliveryWorkerId eq it) }
    return condition
  }

  override fun countDeliveredByDateRange(
    startOfDay: LocalDateTime,
    endOfDay: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long = countWhere { deliveredWindowCondition(startOfDay, endOfDay, deliveryWorkerId) }

  override fun sumDeliveredAmountCentsByDateRange(
    startOfDay: LocalDateTime,
    endOfDay: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long = sumAmountWhere { deliveredWindowCondition(startOfDay, endOfDay, deliveryWorkerId) }

  override fun countAndSumDeliveredByPaymentType(
    deliveryWorkerId: Long,
    paymentType: PaymentType,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Array<Long> {
    val deliveredAt = DeliveredAtExpr()
    val condition: Op<Boolean> =
      (OrdersTable.deliveryWorkerId eq deliveryWorkerId) and
        (OrdersTable.status inList deliveredStatusNames()) and
        (OrdersTable.paymentType eq paymentType.name) and
        (deliveredAt greaterEq startDate) and (deliveredAt less endDate)
    val count = countWhere { condition }
    val sum = sumAmountWhere { condition }
    return arrayOf(count, sum)
  }

  override fun findDeliveredOrdersByDateRange(
    deliveryWorkerId: Long?,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    pageable: Pageable,
  ): Page<OrderModel> {
    val condition = deliveredWindowCondition(startDate, endDate, deliveryWorkerId)
    return transaction(database) {
      val total =
        OrdersTable
          .selectAll()
          .andWhere { condition }
          .count()
      val content =
        OrdersTable
          .selectAll()
          .andWhere { condition }
          // 与原实现一致:送达时间升序、id 降序破平
          .orderBy(DeliveredAtExpr() to SortOrder.ASC, OrdersTable.id to SortOrder.DESC)
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
          .map(::toModel)
      content to total
    }.let { (content, total) -> PageImpl(content, pageable, total) }
  }

  override fun countOrdersByUserIdGroupByStatus(userId: Long): Map<OrderModelStatus, Long> {
    val statusCount = OrdersTable.id.count()
    return transaction(database) {
      OrdersTable
        .select(OrdersTable.status, statusCount)
        .andWhere { OrdersTable.userId eq userId }
        .groupBy(OrdersTable.status)
        .toList()
        .associate { row -> OrderModelStatus.fromValue(row[OrdersTable.status]) to row[statusCount] }
    }
  }

  /**
   * 严格开区间窗口 + 可选配送员/状态过滤
   * (orderStatistics 各方法与原内存过滤 isAfter/isBefore 逐位等价;[window] 为按 created_at
   * 或 completed_at 构建的严格开区间谓词)。
   */
  private fun openWindowCondition(
    window: Op<Boolean>,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): Op<Boolean> {
    var condition = window
    deliveryWorkerId?.let { condition = condition and (OrdersTable.deliveryWorkerId eq it) }
    statuses?.let { condition = condition and (OrdersTable.status inList it.map { s -> s.name }) }
    return condition
  }

  override fun countByStatusAndCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): Long =
    countWhere {
      openWindowCondition(
        (OrdersTable.createdAt greater startDate) and (OrdersTable.createdAt less endDate),
        deliveryWorkerId,
        statuses,
      )
    }

  override fun sumAmountCentsByStatusAndCreatedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long =
    sumAmountWhere {
      openWindowCondition(
        (OrdersTable.createdAt greater startDate) and (OrdersTable.createdAt less endDate),
        deliveryWorkerId,
        statuses,
      )
    }

  override fun countByStatusAndCompletedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): Long =
    countWhere {
      openWindowCondition(
        (OrdersTable.completedAt greater startDate) and (OrdersTable.completedAt less endDate),
        deliveryWorkerId,
        statuses,
      )
    }

  override fun sumAmountCentsByStatusAndCompletedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long =
    sumAmountWhere {
      openWindowCondition(
        (OrdersTable.completedAt greater startDate) and (OrdersTable.completedAt less endDate),
        deliveryWorkerId,
        statuses,
      )
    }

  override fun findByCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderModel> =
    transaction(database) {
      OrdersTable
        .selectAll()
        .andWhere { (OrdersTable.createdAt greater startDate) and (OrdersTable.createdAt less endDate) }
        // 原 JPA 无序,按 id 升序破平
        .orderBy(OrdersTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByStatusAndCreatedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderModel> =
    transaction(database) {
      OrdersTable
        .selectAll()
        .andWhere {
          (OrdersTable.status inList statuses.map { it.name }) and
            (OrdersTable.createdAt greater startDate) and
            (OrdersTable.createdAt less endDate)
        }
        // 原 JPA 无序,按 id 升序破平
        .orderBy(OrdersTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  // ========================================================================
  // 营销使用情况(PRD 10-marketing.md:相关订单记录 + 营销使用汇总 + 商品聚合行)
  // ========================================================================

  /**
   * 营销使用订单匹配谓词(MarketingUsageKind 语义,与原实现逐分支一致):
   * 关联表匹配一律子查询(避免 join 造成的订单重复行);子查询同带各自软删过滤
   * (user_coupons/order_items 为 @SoftDelete 表,order_gifts 裸表不过滤)。
   */
  private fun marketingUsageCondition(criteria: MarketingUsageOrderCriteria): Op<Boolean> =
    when (criteria.kind) {
      MarketingUsageKind.PROMOTION_ACTIVITY -> OrdersTable.promotionActivityId eq criteria.refId

      MarketingUsageKind.FIRST_ORDER_REDUCE -> OrdersTable.firstOrderActivityId eq criteria.refId

      MarketingUsageKind.COUPON_TEMPLATE ->
        OrdersTable.couponId inSubQuery
          UserCouponsTable
            .select(UserCouponsTable.id)
            .andWhere { UserCouponsTable.couponTemplateId eq criteria.refId }

      MarketingUsageKind.GIFT_ACTIVITY ->
        OrdersTable.id inSubQuery
          OrderGiftsTable
            .select(OrderGiftsTable.orderId)
            .where { OrderGiftsTable.giftActivityId eq criteria.refId }

      MarketingUsageKind.FLASH_SALE_ITEM, MarketingUsageKind.NEWCOMER_PRICE_ITEM -> {
        val productId = requireNotNull(criteria.productId) { "productId is required for ${criteria.kind}" }
        val unitPriceCents = requireNotNull(criteria.unitPriceCents) { "unitPriceCents is required for ${criteria.kind}" }
        val validFrom = requireNotNull(criteria.validFrom) { "validFrom is required for ${criteria.kind}" }
        val validTo = requireNotNull(criteria.validTo) { "validTo is required for ${criteria.kind}" }
        // 活动窗口径与引擎一致:validFrom ≤ 下单时间 < validTo
        (OrdersTable.createdAt greaterEq validFrom) and
          (OrdersTable.createdAt less validTo) and
          (
            OrdersTable.id inSubQuery
              OrderItemsTable
                .select(OrderItemsTable.orderId)
                .andWhere {
                  (OrderItemsTable.productId eq productId) and
                    (OrderItemsTable.unitPriceCents eq unitPriceCents)
                }
          )
      }
    }

  override fun findOrdersByMarketingUsage(
    criteria: MarketingUsageOrderCriteria,
    page: Int,
    size: Int,
  ): Page<OrderModel> {
    val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
    return transaction(database) {
      // marketingUsageCondition 含子查询(需事务上下文),在事务内构造
      val condition = marketingUsageCondition(criteria)
      val content =
        OrdersTable
          .selectAll()
          .andWhere { condition }
          .orderBy(OrdersTable.createdAt to SortOrder.DESC, OrdersTable.id to SortOrder.ASC)
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
          .map(::toModel)
      content to OrdersTable.selectAll().andWhere { condition }.count()
    }.let { (content, total) -> PageImpl(content, pageable, total) }
  }

  override fun countOrdersByMarketingUsage(criteria: MarketingUsageOrderCriteria): Long = countWhere { marketingUsageCondition(criteria) }

  /** SUM(单个金额快照列) WHERE 条件(主动软删过滤),空结果回退 0;条件构造在事务内。 */
  private fun sumDiscountWhere(
    amountColumn: Column<Long>,
    buildCondition: () -> Op<Boolean>,
  ): Long =
    transaction(database) {
      val sumExpr = amountColumn.sum()
      OrdersTable
        .select(sumExpr)
        .andWhere { buildCondition() }
        .firstOrNull()
        ?.longOf(sumExpr)
        ?: 0L
    }

  override fun sumMarketingDiscountCents(criteria: MarketingUsageOrderCriteria): Long =
    when (criteria.kind) {
      MarketingUsageKind.PROMOTION_ACTIVITY ->
        sumDiscountWhere(OrdersTable.promotionDiscountCents) {
          OrdersTable.promotionActivityId eq criteria.refId
        }

      MarketingUsageKind.FIRST_ORDER_REDUCE ->
        sumDiscountWhere(OrdersTable.firstOrderDiscountCents) {
          OrdersTable.firstOrderActivityId eq criteria.refId
        }

      MarketingUsageKind.COUPON_TEMPLATE ->
        sumDiscountWhere(OrdersTable.couponDiscountCents) {
          marketingUsageCondition(criteria)
        }

      // 限时特价/新人专享价/买赠满赠无订单列优惠快照,让利金额由 aggregateMarketingUsageItems 计算
      else -> 0L
    }

  override fun aggregateMarketingUsageItems(criteria: MarketingUsageOrderCriteria): List<MarketingUsageItemRow> =
    when (criteria.kind) {
      MarketingUsageKind.GIFT_ACTIVITY -> {
        val quantitySum = OrderGiftsTable.quantity.sum()
        transaction(database) {
          OrderGiftsTable
            .select(OrderGiftsTable.productId, quantitySum)
            .where { OrderGiftsTable.giftActivityId eq criteria.refId }
            .groupBy(OrderGiftsTable.productId)
            .toList()
            .map { row ->
              MarketingUsageItemRow(
                productId = row[OrderGiftsTable.productId],
                unitPriceCents = null,
                quantitySum = row.longOf(quantitySum),
              )
            }
        }
      }

      MarketingUsageKind.FLASH_SALE_ITEM, MarketingUsageKind.NEWCOMER_PRICE_ITEM -> {
        // 时间窗匹配经 orders 子查询实现(与 marketingUsageCondition 同口径,避免 join 重复行)
        val productId = requireNotNull(criteria.productId) { "productId is required for ${criteria.kind}" }
        val unitPriceCents = requireNotNull(criteria.unitPriceCents) { "unitPriceCents is required for ${criteria.kind}" }
        val validFrom = requireNotNull(criteria.validFrom) { "validFrom is required for ${criteria.kind}" }
        val validTo = requireNotNull(criteria.validTo) { "validTo is required for ${criteria.kind}" }
        val quantitySum = OrderItemsTable.quantity.sum()
        transaction(database) {
          OrderItemsTable
            .select(OrderItemsTable.productId, OrderItemsTable.unitPriceCents, quantitySum)
            .andWhere {
              (OrderItemsTable.productId eq productId) and
                (OrderItemsTable.unitPriceCents eq unitPriceCents) and
                (
                  OrderItemsTable.orderId inSubQuery
                    OrdersTable
                      .select(OrdersTable.id)
                      .andWhere { (OrdersTable.createdAt greaterEq validFrom) and (OrdersTable.createdAt less validTo) }
                )
            }.groupBy(OrderItemsTable.productId, OrderItemsTable.unitPriceCents)
            .toList()
            .map { row ->
              MarketingUsageItemRow(
                productId = row[OrderItemsTable.productId],
                unitPriceCents = row[OrderItemsTable.unitPriceCents],
                quantitySum = row.longOf(quantitySum),
              )
            }
        }
      }

      else -> emptyList()
    }

  // ========================================================================
  // 原子条件 UPDATE(资金/状态路径:单条 UPDATE,WHERE 条件 + 行数即结果)
  // ========================================================================

  override fun cancelAtomically(
    id: Long,
    statuses: Collection<OrderModelStatus>,
    now: LocalDateTime,
  ): Int =
    transaction(database) {
      OrdersTable.update({
        OrdersTable.activeOnly {
          (OrdersTable.id eq id) and (OrdersTable.status inList statuses.map { it.name })
        }
      }) {
        it[status] = OrderModelStatus.CANCELLED.name
        it[updatedAt] = now
      }
    }

  override fun confirmReceiptAtomically(
    id: Long,
    completedAt: LocalDateTime,
  ): Int =
    transaction(database) {
      OrdersTable.update({
        OrdersTable.activeOnly {
          (OrdersTable.id eq id) and (OrdersTable.status eq OrderModelStatus.AWAITING_CONFIRMATION.name)
        }
      }) {
        it[status] = OrderModelStatus.COMPLETED.name
        it[OrdersTable.completedAt] = completedAt
        it[updatedAt] = completedAt
      }
    }
}
