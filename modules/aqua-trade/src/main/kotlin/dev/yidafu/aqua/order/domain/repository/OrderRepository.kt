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
import dev.yidafu.aqua.api.dto.MarketingUsageOrderCriteria
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.util.Optional

/**
 * 订单仓储(orders,trade 域)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository,仅保留实际被调用的方法
 * (含原 JPA 继承方法 findById/findAll/findAllById/save 的显式声明);
 * 查询过滤 is_deleted(软删可见性与原 @SoftDelete 一致);含复杂聚合/分页/原子更新。
 *
 * 注意:原 JPA EAGER 关联字段(user/address/deliveryWorker)由 Exposed 实现装载为 null
 * (列值经 userId/addressId/deliveryWorkerId 提供,由服务层/Resolver 联查补全)。
 */
interface OrderRepository {
  fun findByOrderNo(orderNo: String): OrderModel?

  /**
   * 服务端下单幂等(R-ORD-06):同一用户同一 clientRequestId 已下单则直接返回已存在订单,
   * 由 (user_id, client_request_id) 唯一索引兜底并发重复提交。
   */
  fun findByUserIdAndClientRequestId(
    userId: Long,
    clientRequestId: String,
  ): OrderModel?

  fun findByUserId(userId: Long): List<OrderModel>

  fun findByUserIdAndStatus(
    userId: Long,
    status: OrderModelStatus,
  ): List<OrderModel>

  fun existsByUserIdAndStatus(
    userId: Long,
    status: OrderModelStatus,
  ): Boolean

  fun findByStatus(status: OrderModelStatus): List<OrderModel>

  fun findByStatusOrderByCreatedAtAsc(status: OrderModelStatus): List<OrderModel>

  /**
   * 分页查询指定状态且开始配送时间早于截止时刻的订单(配送超时扫描用,分页游标,规则 13)。
   * 排序取 pageable.sort(支持 id/createdAt/updatedAt/amountCents,未知属性忽略);
   * 无排序时按 id 升序破平 —— 原 JPA derived 无序,取稳定游标。
   */
  fun findByStatusAndDeliveryStartedAtBefore(
    status: OrderModelStatus,
    before: LocalDateTime,
    pageable: Pageable,
  ): Page<OrderModel>

  fun findByDeliveryWorkerIdOrderByCreatedAtDesc(deliveryWorkerId: Long): List<OrderModel>

  fun findByAddressId(addressId: Long): List<OrderModel>

  fun countByStatus(status: OrderModelStatus): Long

  fun findById(id: Long): Optional<OrderModel>

  fun findAllById(ids: Iterable<Long>): List<OrderModel>

  /**
   * upsert 语义:无 id 或 id 不存在时插入(雪花 id,与原 @SnowflakeIdGenerator 预分配一致),
   * 否则按 id 更新业务列(updatedAt=now,等价原 @PreUpdate);createdAt 不可变(updatable=false)。
   */
  fun save(entity: OrderModel): OrderModel

  /**
   * 分页查询订单(后台管理列表):keyword 对 orderNo 大小写不敏感包含;
   * 排序字段白名单 totalAmount/updatedAt/createdAt(未知字段回退 createdAt),+ id 破平。
   */
  fun findOrdersPaginated(
    keyword: String? = null,
    status: OrderModelStatus? = null,
    userId: Long? = null,
    deliveryWorkerId: Long? = null,
    startDate: LocalDateTime? = null,
    endDate: LocalDateTime? = null,
    minAmount: Long? = null,
    maxAmount: Long? = null,
    page: Int = 0,
    size: Int = 20,
    sortField: String = "createdAt",
    sortDirection: String = "desc",
  ): Page<OrderModel>

  /**
   * 分页查询配送员历史订单(按多个状态)。
   * 原实现的关键字过滤在 QueryDSL 版即为注释掉的死代码,此处保留参数但不参与过滤(行为不变);
   * createdAt 降序 + id 破平;statuses 为空列表=不匹配任何行。
   */
  fun findByDeliveryWorkerIdAndStatusIn(
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>,
    keyword: String? = null,
    page: Int,
    size: Int,
  ): Page<OrderModel>

  /**
   * 用户订单分页查询(F-ORD-002 用户端订单列表,G02):
   * statuses 为空=全部状态;按下单时间倒序 + id 破平;userId 由调用方强制为登录用户(防越权)。
   */
  fun findUserOrdersPaginated(
    userId: Long,
    statuses: List<OrderModelStatus>?,
    page: Int,
    size: Int,
  ): Page<OrderModel>

  /**
   * 统计指定日期范围内的订单数量(可按配送员筛选,可按状态筛选):
   * createdAt ∈ [startOfDay, endOfDay)(goe/lt 半开区间,与原实现一致)。
   */
  fun countOrdersByDateRange(
    startOfDay: LocalDateTime,
    endOfDay: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>? = null,
  ): Long

  /**
   * 统计指定日期范围内指定状态订单的金额总和(可按配送员筛选):
   * createdAt ∈ [startOfDay, endOfDay)(goe/lt 半开区间,与原实现一致)。
   */
  fun sumAmountCentsByStatusAndDateRange(
    statuses: List<OrderModelStatus>,
    startOfDay: LocalDateTime,
    endOfDay: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long

  // ========================================================================
  // 「已送达」口径聚合(R-ORD-11 2026-10 简化):送水工业绩/收款日报/对账专用,
  // 状态 = COMPLETED、时间锚点 = completed_at(= 送达时间,送达即完成即时计入)——
  // 与营收口径(仅 COMPLETED 按 completed_at)同源同锚,见 08-statistics.md。
  // ========================================================================

  /**
   * 统计日期窗口内「已送达」订单数(送水工业绩口径 = 已完成):
   * status=COMPLETED 且 completed_at ∈ [startOfDay, endOfDay)。
   */
  fun countDeliveredByDateRange(
    startOfDay: LocalDateTime,
    endOfDay: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long

  /**
   * 统计日期窗口内「已送达」订单金额合计(分,送水工业绩口径):窗口口径同 [countDeliveredByDateRange]。
   */
  fun sumDeliveredAmountCentsByDateRange(
    startOfDay: LocalDateTime,
    endOfDay: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long

  /**
   * 按支付类型统计日期窗口内「已送达」订单数量与金额(收款日报/对账口径 = 已完成):
   * 返回 [订单数, 金额合计(分)];窗口口径同 [countDeliveredByDateRange]。
   */
  fun countAndSumDeliveredByPaymentType(
    deliveryWorkerId: Long,
    paymentType: PaymentType,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Array<Long>

  /**
   * 分页查询日期窗口内「已送达」订单明细(对账逐单明细 = 已完成):
   * 按 completed_at(送达时间)过滤,可指定送水工;
   * 排序 = 送达时间升序、id 降序(与原实现一致);count 与 content 同一事务。
   * 供 G29 对账详情逐单差异明细使用,禁止全量拉取(AGENTS.md 规则 10)。
   */
  fun findDeliveredOrdersByDateRange(
    deliveryWorkerId: Long?,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    pageable: Pageable,
  ): Page<OrderModel>

  /**
   * 按订单状态分组统计指定用户的订单数(F-USR-002,G31 我的页订单角标):
   * 仅本人订单(软删除自动过滤);缺失状态不计入(调用方补 0)。
   */
  fun countOrdersByUserIdGroupByStatus(userId: Long): Map<OrderModelStatus, Long>

  /**
   * 统计日期窗口内订单数(后台首页 orderStatistics 口径):
   * createdAt 严格开区间 (startDate, endDate),与原内存过滤 isAfter/isBefore 逐位等价;
   * statuses 为 null=全部状态;deliveryWorkerId 为 null=不限配送员;软删除自动过滤。
   */
  fun countByStatusAndCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): Long

  /**
   * 统计日期窗口内已完成订单数(orderStatistics 已完成口径,按完成时间归属):
   * completedAt 严格开区间 (startDate, endDate);statuses 为 null=全部状态;软删除自动过滤。
   * 与 countByStatusAndCreatedAtBetween 的差异:窗口按 completed_at 而非 created_at ——
   * 跨日完成订单(下单日 ≠ 完成日)计入完成日,与营收口径一致(PRD 01-overview §日完成订单数)。
   */
  fun countByStatusAndCompletedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): Long

  /**
   * 统计日期窗口内指定状态订单金额合计(分,amountCents 快照列,后台首页 orderStatistics 口径):
   * createdAt 严格开区间 (startDate, endDate),与原内存过滤 isAfter/isBefore 逐位等价;
   * deliveryWorkerId 为 null=不限配送员;软删除自动过滤。
   */
  fun sumAmountCentsByStatusAndCreatedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long

  /**
   * 统计日期窗口内已完成订单金额合计(orderStatistics 营收口径,按完成时间归属):
   * completedAt 严格开区间 (startDate, endDate);软删除自动过滤。
   * 窗口按 completed_at 而非 created_at —— 跨日完成订单营收计入完成日,不随下单日丢失,
   * 与 dailyStatistics/weeklyStatistics/monthlyStatistics 营收口径统一(PRD F-STA-002)。
   */
  fun sumAmountCentsByStatusAndCompletedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long

  /**
   * 查询日期窗口内订单(createdAt 严格开区间,与原内存过滤 isAfter/isBefore 逐位等价;软删除自动过滤;
   * 原 JPA 无序,按 id 升序破平)
   */
  fun findByCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderModel>

  /**
   * 查询日期窗口内指定状态订单(createdAt 严格开区间,与原内存过滤 isAfter/isBefore 逐位等价;
   * 软删除自动过滤;原 JPA 无序,按 id 升序破平)
   */
  fun findByStatusAndCreatedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderModel>

  // ========================================================================
  // 营销使用情况(PRD 10-marketing.md 营销方式详情:相关订单记录 + 营销使用汇总)
  // ========================================================================

  /**
   * 按营销追溯条件分页查询相关订单(按下单时间倒序 + id 破平),匹配语义见 MarketingUsageKind;
   * count 与 content 同一事务。
   */
  fun findOrdersByMarketingUsage(
    criteria: MarketingUsageOrderCriteria,
    page: Int,
    size: Int,
  ): Page<OrderModel>

  /**
   * 营销使用订单数(与 findOrdersByMarketingUsage 同口径)。
   */
  fun countOrdersByMarketingUsage(criteria: MarketingUsageOrderCriteria): Long

  /**
   * 营销优惠金额合计(分):券/满减/首单立减按订单快照列求和,其余类型返回 0。
   */
  fun sumMarketingDiscountCents(criteria: MarketingUsageOrderCriteria): Long

  /**
   * 营销使用商品聚合行:限时特价/新人价按 order_items (product_id, unit_price_cents) 分组;
   * 买赠/满赠按 order_gifts product_id 分组。
   */
  fun aggregateMarketingUsageItems(criteria: MarketingUsageOrderCriteria): List<MarketingUsageItemRow>

  /**
   * 原子认领取消(订单级乐观锁 CAS,R-ORD-04 互斥):
   * 单条条件 UPDATE:仅 [statuses] 内状态可置 CANCELLED,返回受影响行数
   * (1=认领成功,0=已被并发取消/支付超时/状态已流转);WHERE 同带 activeOnly
   * (JPA 侧 bulk update 不过滤软删为已知差异,业务上不存在软删订单)。
   */
  fun cancelAtomically(
    id: Long,
    statuses: Collection<OrderModelStatus>,
    now: LocalDateTime,
  ): Int

  /**
   * 状态谓词条件更新(裁决结构 #9 ①:updateOrderStatus 原为读-改-存无守卫,存在并发丢更新):
   * 单条条件 UPDATE:仅当前状态为 [from] 时置 [to](同步 updatedAt),返回受影响行数
   * (1=流转成功,0=当前状态已非 [from],并发流转/状态不符);WHERE 同带 activeOnly(口径同 [cancelAtomically])。
   */
  fun updateStatusAtomically(
    id: Long,
    from: OrderModelStatus,
    to: OrderModelStatus,
    now: LocalDateTime,
  ): Int
}
