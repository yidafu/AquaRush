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

package dev.yidafu.aqua.marketing.promotion.domain.repository

import dev.yidafu.aqua.common.domain.model.PromotionActivityTierModel
import dev.yidafu.aqua.common.domain.table.PromotionActivityTiersTable
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime

/**
 * 活动档位仓储 Exposed 实现测试(Testcontainers 真实 PG):save/软删与双删 no-op/按 sort 排序。
 */
class PromotionActivityTierRepositoryImplTest :
  FunSpec({
    val repo = PromotionActivityTierRepositoryImpl(MarketingPgTestSupport.database)

    beforeTest { reset() }

    test("save 新增回填 id,findByActivityId 可查") {
      val saved = repo.save(PromotionActivityTierModel(activityId = 1L, thresholdCents = 100L, sort = 1))
      saved.id.shouldNotBeNull()
      repo.findByActivityId(1L) shouldHaveSize 1
      repo.findByActivityId(999L) shouldHaveSize 0
    }

    test("save 更新已有实体走 update 分支") {
      val id = MarketingPgTestSupport.promotionTier(activityId = 1L, thresholdCents = 100L, sort = 0)
      val loaded =
        repo
          .findByActivityId(1L)
          .single()
          .also {
            it.thresholdCents = 200L
            it.sort = 5
          }
      repo.save(loaded)
      val reloaded = repo.findByActivityIdOrderBySortAsc(1L).single()
      reloaded.thresholdCents shouldBe 200L
      reloaded.sort shouldBe 5
      reloaded.id shouldBe id
    }

    test("软删档位不可见,findByActivityIdOrderBySortAsc 按 sort 升序 + id 破平") {
      val activityId = MarketingPgTestSupport.promotionActivity(name = "活动")
      val t1 = MarketingPgTestSupport.promotionTier(activityId = activityId, thresholdCents = 100L, sort = 2)
      val t2 = MarketingPgTestSupport.promotionTier(activityId = activityId, thresholdCents = 200L, sort = 1)
      MarketingPgTestSupport.promotionTier(activityId = activityId, thresholdCents = 300L, sort = 0, deleted = true)

      repo
        .findByActivityIdOrderBySortAsc(activityId)
        .map { it.id } shouldContainExactly listOf(t2, t1)
      // 同 sort 以 id 破平
      val s1 = MarketingPgTestSupport.promotionTier(activityId = 9L, thresholdCents = 1L, sort = 7)
      val s2 = MarketingPgTestSupport.promotionTier(activityId = 9L, thresholdCents = 2L, sort = 7)
      repo
        .findByActivityIdOrderBySortAsc(9L)
        .map { it.id } shouldContainExactly listOf(s1, s2)
      repo.findByActivityId(9L) shouldHaveSize 2
    }

    test("delete 软删三列写入,二次 delete no-op(deletedAt 哨兵不被重写)") {
      val activityId = MarketingPgTestSupport.promotionActivity(name = "活动2")
      val id = MarketingPgTestSupport.promotionTier(activityId = activityId)
      val entity = repo.findByActivityId(activityId).single()

      repo.delete(entity)
      repo.findByActivityId(activityId) shouldHaveSize 0

      val sentinel = LocalDateTime.of(2020, 1, 1, 0, 0)
      transaction(MarketingPgTestSupport.database) {
        PromotionActivityTiersTable.update({
          PromotionActivityTiersTable.id eq id
        }) {
          it[PromotionActivityTiersTable.deletedAt] = sentinel
        }
      }
      repo.delete(entity)
      val row =
        transaction(MarketingPgTestSupport.database) {
          PromotionActivityTiersTable
            .selectAll()
            .where { PromotionActivityTiersTable.id eq id }
            .single()
        }
      row[PromotionActivityTiersTable.isDeleted] shouldBe true
      row[PromotionActivityTiersTable.deletedAt] shouldBe sentinel
    }
  })
