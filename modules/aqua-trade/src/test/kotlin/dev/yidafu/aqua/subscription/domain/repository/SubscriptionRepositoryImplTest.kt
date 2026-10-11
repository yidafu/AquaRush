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
import dev.yidafu.aqua.common.domain.table.SubscriptionsTable
import dev.yidafu.aqua.common.domain.table.UsersTable
import dev.yidafu.aqua.trade.common.exposed.TradePgTestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDate

/**
 * 周期订购计划仓储 Testcontainers 真实 PG 测试(G77/F-ORD-026):
 * save 双分支 / findById / 分页筛选(状态、下次配送区间、用户关键字) / findDue keyset 扫描 / 明细读写。
 */
class SubscriptionRepositoryImplTest :
  FunSpec({
    val repo = SubscriptionRepositoryImpl(TradePgTestSupport.database)

    beforeTest { TradePgTestSupport.reset() }

    fun plan(
      id: Long? = null,
      userId: Long = 300L,
      nextRunDate: LocalDate = LocalDate.of(2026, 3, 2),
      status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
      weekdays: String? = "1,3,5",
      monthDay: Int? = null,
    ) = SubscriptionModel(
      id = id,
      userId = userId,
      addressId = 10L,
      cycleType = if (monthDay != null) SubscriptionCycleType.MONTHLY else SubscriptionCycleType.WEEKLY,
      weekdays = weekdays,
      monthDay = monthDay,
      timeSlot = DeliveryTimeSlot.MORNING,
      startDate = LocalDate.of(2026, 3, 1),
      nextRunDate = nextRunDate,
      status = status,
      createdBy = 7L,
    )

    test("save insert backfills snowflake id, update branch does not duplicate row") {
      val saved = repo.save(plan())
      saved.id.shouldNotBeNull()

      saved.status = SubscriptionStatus.PAUSED
      saved.executedPeriods = 3
      val updated = repo.save(saved)

      updated.id shouldBe saved.id
      updated.executedPeriods shouldBe 3
      repo.findById(saved.id!!).shouldNotBeNull().status shouldBe SubscriptionStatus.PAUSED

      transaction(TradePgTestSupport.database) {
        SubscriptionsTable.selectAll().count() shouldBe 1L
      }
    }

    test("search by status and next run range is paginated") {
      repo.save(plan(nextRunDate = LocalDate.of(2026, 3, 2)))
      repo.save(plan(nextRunDate = LocalDate.of(2026, 3, 9)))
      repo.save(plan(nextRunDate = LocalDate.of(2026, 3, 16), status = SubscriptionStatus.PAUSED))

      val activeInRange =
        repo.search(
          SubscriptionListQuery(
            status = "ACTIVE",
            nextRunFrom = "2026-03-01",
            nextRunTo = "2026-03-05",
            page = 0,
            size = 20,
          ),
        )
      activeInRange.totalElements shouldBe 1
      activeInRange.content.single().nextRunDate shouldBe LocalDate.of(2026, 3, 2)

      val page =
        repo.search(SubscriptionListQuery(page = 0, size = 2))
      page.totalElements shouldBe 3
      page.content.size shouldBe 2
    }

    test("search keyword matches user nickname via users subquery") {
      // 计划 1 归属用户 300,计划 2 归属 301;仅 300 有昵称匹配行
      transaction(TradePgTestSupport.database) {
        UsersTable
          .insert {
            it[id] = 300L
            it[wechatOpenid] = "openid-300"
            it[nickname] = "周期客户甲"
            it[email] = "a@test.dev"
            it[status] = "ACTIVE"
            it[role] = "USER"
            it[createdAt] = java.time.LocalDateTime.now()
            it[updatedAt] = java.time.LocalDateTime.now()
            it[lastLoginAt] = java.time.LocalDateTime.now()
          }
        UsersTable
          .insert {
            it[id] = 301L
            it[wechatOpenid] = "openid-301"
            it[nickname] = "路人乙"
            it[email] = "b@test.dev"
            it[status] = "ACTIVE"
            it[role] = "USER"
            it[createdAt] = java.time.LocalDateTime.now()
            it[updatedAt] = java.time.LocalDateTime.now()
            it[lastLoginAt] = java.time.LocalDateTime.now()
          }
      }
      repo.save(plan(userId = 300L))
      repo.save(plan(userId = 301L))

      val hit = repo.search(SubscriptionListQuery(keyword = "周期客户", page = 0, size = 10))
      hit.totalElements shouldBe 1
      hit.content.single().userId shouldBe 300L

      val miss = repo.search(SubscriptionListQuery(keyword = "不存在的人", page = 0, size = 10))
      miss.totalElements shouldBe 0
    }

    test("findDue scans ACTIVE plans due on or before date with keyset cursor") {
      val duePast = repo.save(plan(nextRunDate = LocalDate.of(2026, 2, 28)))
      val dueToday = repo.save(plan(nextRunDate = LocalDate.of(2026, 3, 2)))
      val future = repo.save(plan(nextRunDate = LocalDate.of(2026, 3, 9)))
      val pausedDue = repo.save(plan(nextRunDate = LocalDate.of(2026, 3, 1), status = SubscriptionStatus.PAUSED))

      val dueOn = LocalDate.of(2026, 3, 2)
      val firstBatch = repo.findDue(0L, dueOn, 10)
      firstBatch.map { it.id }.shouldContainExactly(listOf(duePast.id, dueToday.id))

      // keyset:越过第一批后不再返回已处理行
      val afterCursor = repo.findDue(dueToday.id!!, dueOn, 10)
      afterCursor shouldBe emptyList()

      // 未来期次与暂停计划不入列
      firstBatch.none { it.id == future.id } shouldBe true
      firstBatch.none { it.id == pausedDue.id } shouldBe true
    }

    test("items round trip by subscription id") {
      val saved = repo.save(plan())
      repo.saveItems(
        listOf(
          SubscriptionItemModel(subscriptionId = saved.id!!, productId = 5L, quantity = 2),
          SubscriptionItemModel(subscriptionId = saved.id!!, productId = 6L, quantity = 1),
        ),
      )

      val items = repo.findItemsBySubscriptionId(saved.id!!)
      items.map { it.productId } shouldContainExactly listOf(5L, 6L)
      items.map { it.quantity } shouldContainExactly listOf(2, 1)
      repo.findItemsBySubscriptionId(404L) shouldBe emptyList()
    }

    test("status column stores enum name") {
      val saved = repo.save(plan(status = SubscriptionStatus.TERMINATED))
      val row =
        transaction(TradePgTestSupport.database) {
          SubscriptionsTable
            .selectAll()
            .andWhere { SubscriptionsTable.id eq saved.id!! }
            .single()
        }
      row[SubscriptionsTable.status] shouldBe "TERMINATED"
    }
  })
