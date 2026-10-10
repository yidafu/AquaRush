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
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.domain.table.UserCouponsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/**
 * 用户优惠券实例仓库实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;核销/退回为原子条件 UPDATE(防并发重复使用,核销幂等 R-MKT-03);
 * 原子条件 UPDATE 的 WHERE 同样带 activeOnly(软删行不可参与核销;JPA 侧 bulk update
 * 不过滤软删为已知差异,业务上不存在软删的用户券)。
 */
@Repository
class UserCouponRepositoryImpl(
  private val database: Database,
) : UserCouponRepository {
  private fun toModel(row: ResultRow): UserCouponModel =
    UserCouponModel(
      id = row[UserCouponsTable.id],
      userId = row[UserCouponsTable.userId],
      couponTemplateId = row[UserCouponsTable.couponTemplateId],
      status = UserCouponStatus.valueOf(row[UserCouponsTable.status]),
      name = row[UserCouponsTable.name],
      couponType = CouponType.valueOf(row[UserCouponsTable.couponType]),
      thresholdCents = row[UserCouponsTable.thresholdCents],
      discountAmountCents = row[UserCouponsTable.discountAmountCents],
      discountRatePermille = row[UserCouponsTable.discountRatePermille],
      scope = CouponScope.valueOf(row[UserCouponsTable.scope]),
      productId = row[UserCouponsTable.productId],
      expireTime = row[UserCouponsTable.expireTime],
      claimTime = row[UserCouponsTable.claimTime],
      usedOrderId = row[UserCouponsTable.usedOrderId],
      usedAt = row[UserCouponsTable.usedAt],
      returnedAt = row[UserCouponsTable.returnedAt],
      createdAt = row[UserCouponsTable.createdAt],
      updatedAt = row[UserCouponsTable.updatedAt],
      deletedAt = row[UserCouponsTable.deletedAt],
      deletedBy = row[UserCouponsTable.deletedBy],
    )

  override fun findByUserId(userId: Long): List<UserCouponModel> =
    transaction(database) {
      UserCouponsTable
        .selectAll()
        .andWhere { UserCouponsTable.userId eq userId }
        .orderBy(UserCouponsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByUserIdAndStatus(
    userId: Long,
    status: UserCouponStatus,
  ): List<UserCouponModel> =
    transaction(database) {
      UserCouponsTable
        .selectAll()
        .andWhere { (UserCouponsTable.userId eq userId) and (UserCouponsTable.status eq status.name) }
        .orderBy(
          UserCouponsTable.id to SortOrder.ASC,
        ).toList()
    }.map(::toModel)

  override fun findByUserIdAndStatusIn(
    userId: Long,
    statuses: List<UserCouponStatus>,
  ): List<UserCouponModel> {
    if (statuses.isEmpty()) return emptyList()
    return transaction(database) {
      UserCouponsTable
        .selectAll()
        .andWhere {
          (UserCouponsTable.userId eq userId) and
            (UserCouponsTable.status inList statuses.map { it.name })
        }.orderBy(UserCouponsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
  }

  override fun countByUserIdAndCouponTemplateId(
    userId: Long,
    templateId: Long,
  ): Long =
    transaction(database) {
      UserCouponsTable
        .selectAll()
        .andWhere { (UserCouponsTable.userId eq userId) and (UserCouponsTable.couponTemplateId eq templateId) }
        .count()
    }

  override fun countByCouponTemplateIdAndStatus(
    templateId: Long,
    status: UserCouponStatus,
  ): Long =
    transaction(database) {
      UserCouponsTable
        .selectAll()
        .andWhere { (UserCouponsTable.couponTemplateId eq templateId) and (UserCouponsTable.status eq status.name) }
        .count()
    }

  override fun findById(id: Long): Optional<UserCouponModel> =
    Optional.ofNullable(
      transaction(database) {
        UserCouponsTable
          .selectAll()
          .andWhere { UserCouponsTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun findByStatusAndExpireTimeBetween(
    status: UserCouponStatus,
    from: LocalDateTime,
    to: LocalDateTime,
    pageable: Pageable,
  ): Page<UserCouponModel> {
    // KDoc 口径 [from, to):expire_time >= from AND expire_time < to
    val condition: Op<Boolean> =
      (UserCouponsTable.status eq status.name) and
        (UserCouponsTable.expireTime greaterEq from) and
        (UserCouponsTable.expireTime less to)
    val (content, total) =
      transaction(database) {
        val content =
          UserCouponsTable
            .selectAll()
            .andWhere { condition }
            // 固定 id 升序破平(原 JPA 无序,取稳定插入序)
            .orderBy(UserCouponsTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to UserCouponsTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun save(entity: UserCouponModel): UserCouponModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        UserCouponsTable
          .selectAllIncludingDeleted()
          .where { UserCouponsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        UserCouponsTable.update({ UserCouponsTable.id eq id }) {
          it[status] = entity.status.name
          it[name] = entity.name
          it[couponType] = entity.couponType.name
          it[thresholdCents] = entity.thresholdCents
          it[discountAmountCents] = entity.discountAmountCents
          it[discountRatePermille] = entity.discountRatePermille
          it[scope] = entity.scope.name
          it[productId] = entity.productId
          it[expireTime] = entity.expireTime
          it[usedOrderId] = entity.usedOrderId
          it[usedAt] = entity.usedAt
          it[returnedAt] = entity.returnedAt
          it[updatedAt] = now
        }
      } else {
        UserCouponsTable.insert {
          it[UserCouponsTable.id] = id
          it[userId] = entity.userId
          it[couponTemplateId] = entity.couponTemplateId
          it[status] = entity.status.name
          it[name] = entity.name
          it[couponType] = entity.couponType.name
          it[thresholdCents] = entity.thresholdCents
          it[discountAmountCents] = entity.discountAmountCents
          it[discountRatePermille] = entity.discountRatePermille
          it[scope] = entity.scope.name
          it[productId] = entity.productId
          it[expireTime] = entity.expireTime
          it[claimTime] = entity.claimTime
          it[usedOrderId] = entity.usedOrderId
          it[usedAt] = entity.usedAt
          it[returnedAt] = entity.returnedAt
          it[UserCouponsTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun redeemAtomically(
    id: Long,
    userId: Long,
    orderId: Long,
    from: UserCouponStatus,
    to: UserCouponStatus,
    now: LocalDateTime,
  ): Int =
    transaction(database) {
      UserCouponsTable.update({
        UserCouponsTable.activeOnly {
          (UserCouponsTable.id eq id) and
            (UserCouponsTable.userId eq userId) and
            (UserCouponsTable.status eq from.name) and
            (UserCouponsTable.expireTime greater now)
        }
      }) {
        it[status] = to.name
        it[usedOrderId] = orderId
        it[usedAt] = now
        it[updatedAt] = now
      }
    }

  override fun returnByOrder(
    orderId: Long,
    now: LocalDateTime,
  ): Int =
    transaction(database) {
      // 与原 CASE WHEN 等价的两段式:两分支条件互补(未过期→RETURNED,剩余 USED→EXPIRED),
      // 同一事务内顺序执行,行集互斥不重叠
      val returned =
        UserCouponsTable.update({
          UserCouponsTable.activeOnly {
            (UserCouponsTable.usedOrderId eq orderId) and
              (UserCouponsTable.status eq UserCouponStatus.USED.name) and
              (UserCouponsTable.expireTime greater now)
          }
        }) {
          it[status] = UserCouponStatus.RETURNED.name
          it[usedOrderId] = null
          it[usedAt] = null
          it[returnedAt] = now
          it[updatedAt] = now
        }
      val expired =
        UserCouponsTable.update({
          UserCouponsTable.activeOnly {
            (UserCouponsTable.usedOrderId eq orderId) and
              (UserCouponsTable.status eq UserCouponStatus.USED.name)
          }
        }) {
          it[status] = UserCouponStatus.EXPIRED.name
          it[usedOrderId] = null
          it[usedAt] = null
          it[returnedAt] = now
          it[updatedAt] = now
        }
      returned + expired
    }
}
