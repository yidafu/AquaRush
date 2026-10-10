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

import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 满减/折扣活动仓储 Exposed 实现测试(Testcontainers 真实 PG):save/软删/分页/findActiveActivities。
 */
class PromotionActivityRepositoryImplTest :
  FunSpec({
    val repo = PromotionActivityRepositoryImpl(MarketingPgTestSupport.database)

    beforeTest { reset() }

    test("save 新增回填 id,findById/findAll/findAllById 可查") {
      val saved = repo.save(PromotionActivityModel(name = "满减A"))
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .name shouldBe "满减A"
      repo.findAll() shouldHaveSize 1
      repo.findAllById(listOf(saved.id!!, 999L)) shouldHaveSize 1
      repo.findAllById(emptyList()) shouldHaveSize 0
    }

    test("save 更新已有实体走 update 分支") {
      val id = MarketingPgTestSupport.promotionActivity(name = "旧", status = "ACTIVE")
      val loaded = repo.findById(id).orElse(null).shouldNotBeNull()
      loaded.name = "新"
      loaded.status = PromotionActivityStatus.DISABLED
      repo.save(loaded)

      val reloaded = repo.findById(id).orElse(null).shouldNotBeNull()
      reloaded.name shouldBe "新"
      reloaded.status shouldBe PromotionActivityStatus.DISABLED
    }

    test("软删活动不可见:各查询均过滤") {
      val keep = MarketingPgTestSupport.promotionActivity(name = "存活")
      MarketingPgTestSupport.promotionActivity(name = "已删", deleted = true)

      repo.findById(keep).isPresent shouldBe true
      repo.findAll() shouldHaveSize 1
      repo.findAllById(listOf(keep)) shouldHaveSize 1
    }

    test("searchActivities:关键词过滤 + 软删不进 + 分页 id 破平") {
      val a = MarketingPgTestSupport.promotionActivity(name = "满减一")
      val b = MarketingPgTestSupport.promotionActivity(name = "折扣二")
      MarketingPgTestSupport.promotionActivity(name = "满减隐藏", deleted = true)

      repo.searchActivities("满减", PageRequest.of(0, 10)).totalElements shouldBe 1L
      repo.searchActivities("二", PageRequest.of(0, 10)).totalElements shouldBe 1L
      repo.searchActivities("  ", PageRequest.of(0, 10)).totalElements shouldBe 2L

      val page0 = repo.searchActivities(null, PageRequest.of(0, 1))
      val page1 = repo.searchActivities(null, PageRequest.of(1, 1))
      page0.content.map { it.id } shouldContainExactly listOf(a)
      page1.content.map { it.id } shouldContainExactly listOf(b)
    }

    test("findActiveActivities:ACTIVE 且时间窗内,软删排除") {
      val now = LocalDateTime.of(2026, 5, 1, 0, 0)
      MarketingPgTestSupport.promotionActivity(
        name = "生效",
        validFrom = now.minusDays(1),
        validTo = now.plusDays(1),
        status = "ACTIVE",
      )
      MarketingPgTestSupport.promotionActivity(
        name = "未开始",
        validFrom = now.plusDays(1),
        validTo = now.plusDays(2),
        status = "ACTIVE",
      )
      MarketingPgTestSupport.promotionActivity(
        name = "停用",
        validFrom = now.minusDays(1),
        validTo = now.plusDays(1),
        status = "DISABLED",
      )
      MarketingPgTestSupport.promotionActivity(
        name = "软删",
        validFrom = now.minusDays(1),
        validTo = now.plusDays(1),
        status = "ACTIVE",
        deleted = true,
      )

      val active = repo.findActiveActivities(PromotionActivityStatus.ACTIVE, now)
      active shouldHaveSize 1
      active.single().name shouldBe "生效"
      repo.findActiveActivities(PromotionActivityStatus.DISABLED, now) shouldHaveSize 1
      repo.findActiveActivities(PromotionActivityStatus.ACTIVE, now.minusYears(1)) shouldHaveSize 0
    }
  })
