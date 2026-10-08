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

import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QUserCouponModel.Companion.userCouponModel
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 用户优惠券实例仓库 QueryDSL 实现。
 * 核销/退回用原子条件 UPDATE(防并发重复使用,核销幂等 R-MKT-03)。
 * 注意:@SoftDelete 由 Hibernate 在 SQL 层自动过滤已删行。
 */
@Repository
class UserCouponRepositoryImpl : UserCouponRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  @Transactional
  override fun redeemAtomically(
    id: Long,
    userId: Long,
    orderId: Long,
    from: UserCouponStatus,
    to: UserCouponStatus,
    now: LocalDateTime,
  ): Int =
    queryFactory
      .update(userCouponModel)
      .set(userCouponModel.status, to)
      .set(userCouponModel.usedOrderId, orderId)
      .set(userCouponModel.usedAt, now)
      .set(userCouponModel.updatedAt, now)
      .where(
        userCouponModel.id.eq(id),
        userCouponModel.userId.eq(userId),
        userCouponModel.status.eq(from),
        userCouponModel.expireTime.gt(now),
      ).execute()
      .toInt()

  @Transactional
  override fun returnByOrder(
    orderId: Long,
    now: LocalDateTime,
  ): Int =
    queryFactory
      .update(userCouponModel)
      .set(
        userCouponModel.status,
        Expressions
          .cases()
          .`when`(userCouponModel.expireTime.gt(now))
          .then(UserCouponStatus.RETURNED)
          .otherwise(UserCouponStatus.EXPIRED),
      ).setNull(userCouponModel.usedOrderId)
      .setNull(userCouponModel.usedAt)
      .set(userCouponModel.returnedAt, now)
      .set(userCouponModel.updatedAt, now)
      .where(
        userCouponModel.usedOrderId.eq(orderId),
        userCouponModel.status.eq(UserCouponStatus.USED),
      ).execute()
      .toInt()
}
