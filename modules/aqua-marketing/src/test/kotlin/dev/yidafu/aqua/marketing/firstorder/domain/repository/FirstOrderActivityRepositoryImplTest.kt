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

package dev.yidafu.aqua.marketing.firstorder.domain.repository

import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.marketing.common.exposed.MarketingExposedH2TestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingExposedH2TestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 首单活动仓储 Exposed 实现测试(H2):save/软删/分页/生效活动/新人专享价/重叠校验。
 */
class FirstOrderActivityRepositoryImplTest :
  FunSpec({
    val repo = FirstOrderActivityRepositoryImpl(MarketingExposedH2TestSupport.database)

    beforeTest { reset() }

    test("save 双分支 + findById/findAllById 软删过滤") {
      val saved = repo.save(FirstOrderActivityModel(name = "首单A"))
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .name shouldBe "首单A"

      val loaded = repo.findById(saved.id!!).orElse(null).shouldNotBeNull()
      loaded.name = "首单B"
      repo.save(loaded)
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .name shouldBe "首单B"

      val gone = MarketingExposedH2TestSupport.firstOrderActivity(name = "已删", deleted = true)
      repo.findById(gone).isPresent shouldBe false
      repo.findAllById(listOf(saved.id!!, gone)).map { it.id } shouldContainExactly listOf(saved.id)
      repo.findAllById(emptyList()).size shouldBe 0
    }

    test("searchActivities:关键词 + 分页 id 破平 + 软删过滤") {
      val a = MarketingExposedH2TestSupport.firstOrderActivity(name = "首单一")
      val b = MarketingExposedH2TestSupport.firstOrderActivity(name = "新人二")
      MarketingExposedH2TestSupport.firstOrderActivity(name = "隐藏首单", deleted = true)

      repo.searchActivities("首单", PageRequest.of(0, 10)).totalElements shouldBe 1L
      repo.searchActivities("  ", PageRequest.of(0, 10)).totalElements shouldBe 2L

      val page0 = repo.searchActivities(null, PageRequest.of(0, 1))
      val page1 = repo.searchActivities(null, PageRequest.of(1, 1))
      page0.content.map { it.id } shouldContainExactly listOf(a)
      page1.content.map { it.id } shouldContainExactly listOf(b)
    }

    test("findActiveActivities:ACTIVE 且时间窗内") {
      val now = LocalDateTime.of(2026, 5, 1, 0, 0)
      MarketingExposedH2TestSupport.firstOrderActivity(name = "生效", validFrom = now.minusDays(1), validTo = now.plusDays(1))
      MarketingExposedH2TestSupport.firstOrderActivity(name = "未开始", validFrom = now.plusDays(1), validTo = now.plusDays(2))
      MarketingExposedH2TestSupport.firstOrderActivity(name = "软删", validFrom = now.minusDays(1), validTo = now.plusDays(1), deleted = true)

      repo.findActiveActivities(PromotionActivityStatus.ACTIVE, now).map { it.name } shouldContainExactly listOf("生效")
      repo.findActiveActivities(PromotionActivityStatus.ACTIVE, now.plusYears(1)).size shouldBe 0
    }

    test("findActiveNewcomerPriceByProductId:类型+商品过滤,validFrom 最新优先") {
      val now = LocalDateTime.of(2026, 5, 1, 0, 0)
      MarketingExposedH2TestSupport.firstOrderActivity(
        name = "早",
        activityType = "NEWCOMER_PRICE",
        productId = 1L,
        newcomerPriceCents = 900L,
        validFrom = now.minusDays(10),
        validTo = now.plusDays(10),
      )
      MarketingExposedH2TestSupport.firstOrderActivity(
        name = "晚",
        activityType = "NEWCOMER_PRICE",
        productId = 1L,
        newcomerPriceCents = 800L,
        validFrom = now.minusDays(2),
        validTo = now.plusDays(10),
      )
      MarketingExposedH2TestSupport.firstOrderActivity(
        name = "别型",
        activityType = "FIRST_ORDER_REDUCE",
        productId = 1L,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(10),
      )

      val hit =
        repo.findActiveNewcomerPriceByProductId(
          1L,
          FirstOrderActivityType.NEWCOMER_PRICE,
          PromotionActivityStatus.ACTIVE,
          now,
        )
      hit.shouldNotBeNull().name shouldBe "晚"
      hit.newcomerPriceCents shouldBe 800L
      repo.findActiveNewcomerPriceByProductId(2L, FirstOrderActivityType.NEWCOMER_PRICE, PromotionActivityStatus.ACTIVE, now) shouldBe null
      repo.findActiveNewcomerPriceByProductId(
        1L,
        FirstOrderActivityType.NEWCOMER_PRICE,
        PromotionActivityStatus.ACTIVE,
        now.plusYears(1),
      ) shouldBe
        null
    }

    test("existsOverlap:时间窗重叠 + 类型过滤 + excludeId + 软删不冲突") {
      val base = LocalDateTime.of(2026, 5, 1, 0, 0)
      val overlapId =
        MarketingExposedH2TestSupport.firstOrderActivity(
          name = "重叠",
          activityType = "FIRST_ORDER_REDUCE",
          productId = 1L,
          validFrom = base,
          validTo = base.plusDays(10),
        )
      MarketingExposedH2TestSupport.firstOrderActivity(
        name = "软删",
        activityType = "FIRST_ORDER_REDUCE",
        productId = 1L,
        validFrom = base,
        validTo = base.plusDays(10),
        deleted = true,
      )

      repo
        .existsOverlap(
          1L,
          FirstOrderActivityType.FIRST_ORDER_REDUCE,
          PromotionActivityStatus.ACTIVE,
          base.plusDays(5),
          base.plusDays(15),
          null,
        ) shouldBe true
      repo
        .existsOverlap(
          1L,
          FirstOrderActivityType.NEWCOMER_PRICE,
          PromotionActivityStatus.ACTIVE,
          base.plusDays(5),
          base.plusDays(15),
          null,
        ) shouldBe false
      repo
        .existsOverlap(
          1L,
          FirstOrderActivityType.FIRST_ORDER_REDUCE,
          PromotionActivityStatus.ACTIVE,
          base.plusDays(5),
          base.plusDays(15),
          overlapId,
        ) shouldBe false
      repo
        .existsOverlap(
          1L,
          FirstOrderActivityType.FIRST_ORDER_REDUCE,
          PromotionActivityStatus.ACTIVE,
          base.minusDays(5),
          base.minusDays(1),
          null,
        ) shouldBe false
    }
  })
