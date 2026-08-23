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

package dev.yidafu.aqua.marketing.coupon.domain.repository

import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 用户优惠券实例仓库(F-MKT-003):
 * 核销/退回用原子条件 UPDATE(防并发重复使用,核销幂等 R-MKT-03)。
 */
@Repository
interface UserCouponRepository :
  JpaRepository<UserCouponModel, Long>,
  JpaSpecificationExecutor<UserCouponModel> {
  fun findByUserId(userId: Long): List<UserCouponModel>

  fun findByUserIdAndStatus(
    userId: Long,
    status: UserCouponStatus,
  ): List<UserCouponModel>

  fun findByUserIdAndStatusIn(
    userId: Long,
    statuses: List<UserCouponStatus>,
  ): List<UserCouponModel>

  fun countByUserIdAndCouponTemplateId(
    userId: Long,
    templateId: Long,
  ): Long

  fun findByUsedOrderId(orderId: Long): List<UserCouponModel>

  fun countByCouponTemplateId(templateId: Long): Long

  fun countByCouponTemplateIdAndStatus(
    templateId: Long,
    status: UserCouponStatus,
  ): Long

  fun findByCouponTemplateId(templateId: Long): List<UserCouponModel>

  /**
   * 分页查询指定状态且过期时间在 [from, to) 区间的优惠券(优惠券即将过期提醒调度用;
   * 分页游标,禁止全表查询 —— 规则 13)。
   */
  fun findByStatusAndExpireTimeBetween(
    status: UserCouponStatus,
    from: LocalDateTime,
    to: LocalDateTime,
    pageable: Pageable,
  ): Page<UserCouponModel>

  /**
   * 原子核销:仅 RECEIVED 且未过期可核销;受影响行数=1 成功,=0 表示已被并发核销/过期(核销幂等)。
   */
  @Modifying
  @Query(
    """
    update UserCouponModel c
    set c.status = :to, c.usedOrderId = :orderId, c.usedAt = :now, c.updatedAt = :now
    where c.id = :id and c.userId = :userId and c.status = :from and c.expireTime > :now
    """,
  )
  fun redeemAtomically(
    @Param("id") id: Long,
    @Param("userId") userId: Long,
    @Param("orderId") orderId: Long,
    @Param("from") from: UserCouponStatus,
    @Param("to") to: UserCouponStatus,
    @Param("now") now: LocalDateTime,
  ): Int

  /**
   * 订单取消/退款退回(幂等):该订单已核销券,未过期→RETURNED 可复用,过期→EXPIRED。
   */
  @Modifying
  @Query(
    """
    update UserCouponModel c
    set c.status = case when c.expireTime > :now
        then dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus.RETURNED
        else dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus.EXPIRED end,
        c.usedOrderId = null, c.usedAt = null, c.returnedAt = :now, c.updatedAt = :now
    where c.usedOrderId = :orderId and c.status = dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus.USED
    """,
  )
  fun returnByOrder(
    @Param("orderId") orderId: Long,
    @Param("now") now: LocalDateTime,
  ): Int
}
