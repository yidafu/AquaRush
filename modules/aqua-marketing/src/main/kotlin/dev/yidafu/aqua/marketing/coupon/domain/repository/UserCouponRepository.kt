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
import java.time.LocalDateTime
import java.util.Optional

/**
 * 用户优惠券实例仓库(F-MKT-003):
 * 核销/退回用原子条件 UPDATE(防并发重复使用,核销幂等 R-MKT-03)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository/JpaSpecificationExecutor,仅保留实际被调用的方法;
 * 查询过滤 is_deleted;核销/退回为原子条件 UPDATE。
 */
interface UserCouponRepository {
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

  fun countByCouponTemplateIdAndStatus(
    templateId: Long,
    status: UserCouponStatus,
  ): Long

  fun findById(id: Long): Optional<UserCouponModel>

  /**
   * 分页查询指定状态且过期时间在 [from, to) 区间的优惠券(优惠券即将过期提醒调度用;
   * 分页游标,禁止全表查询 —— 规则 13);固定 id 升序破平。
   */
  fun findByStatusAndExpireTimeBetween(
    status: UserCouponStatus,
    from: LocalDateTime,
    to: LocalDateTime,
    pageable: Pageable,
  ): Page<UserCouponModel>

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: UserCouponModel): UserCouponModel

  /**
   * 原子核销:仅 RECEIVED 且未过期可核销;受影响行数=1 成功,=0 表示已被并发核销/过期(核销幂等)。
   */
  fun redeemAtomically(
    id: Long,
    userId: Long,
    orderId: Long,
    from: UserCouponStatus,
    to: UserCouponStatus,
    now: LocalDateTime,
  ): Int

  /**
   * 订单取消/退款退回(幂等):该订单已核销券,未过期→RETURNED 可复用,过期→EXPIRED。
   */
  fun returnByOrder(
    orderId: Long,
    now: LocalDateTime,
  ): Int
}
