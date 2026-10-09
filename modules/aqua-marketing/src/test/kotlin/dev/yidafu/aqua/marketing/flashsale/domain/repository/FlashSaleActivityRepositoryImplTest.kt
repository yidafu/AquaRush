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

package dev.yidafu.aqua.marketing.flashsale.domain.repository

import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
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
 * 限时特价活动仓储 Exposed 实现测试(H2):save/软删/分页/findActiveByProductId/existsOverlap。
 */
class FlashSaleActivityRepositoryImplTest :
  FunSpec({
    val repo = FlashSaleActivityRepositoryImpl(MarketingExposedH2TestSupport.database)

    beforeTest { reset() }

    test("save 双分支 + 软删不可见") {
      val saved = repo.save(FlashSaleActivityModel(name = "特价A", productId = 10L, salePriceCents = 100L))
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .salePriceCents shouldBe 100L

      val loaded = repo.findById(saved.id!!).orElse(null).shouldNotBeNull()
      loaded.salePriceCents = 80L
      repo.save(loaded)
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .salePriceCents shouldBe 80L

      val gone = MarketingExposedH2TestSupport.flashSale(name = "已删", deleted = true)
      repo.findById(gone).isPresent shouldBe false
      repo.searchActivities(null, PageRequest.of(0, 10)).totalElements shouldBe 1L
    }

    test("searchActivities:关键词 + 分页 id 破平 + 软删过滤") {
      val a = MarketingExposedH2TestSupport.flashSale(name = "限时一")
      val b = MarketingExposedH2TestSupport.flashSale(name = "限时二")
      MarketingExposedH2TestSupport.flashSale(name = "隐藏", deleted = true)

      repo.searchActivities("限时", PageRequest.of(0, 10)).totalElements shouldBe 2L
      repo.searchActivities("三", PageRequest.of(0, 10)).totalElements shouldBe 0L

      val page0 = repo.searchActivities(null, PageRequest.of(0, 2))
      val page1 = repo.searchActivities(null, PageRequest.of(1, 2))
      page0.content.map { it.id } shouldContainExactly listOf(a, b)
      page1.content.size shouldBe 0
    }

    test("findActiveByProductId:时间窗内最新 validFrom 优先") {
      val now = LocalDateTime.of(2026, 5, 1, 0, 0)
      MarketingExposedH2TestSupport.flashSale(
        name = "早窗",
        productId = 1L,
        validFrom = now.minusDays(10),
        validTo = now.plusDays(10),
      )
      MarketingExposedH2TestSupport.flashSale(
        name = "晚窗",
        productId = 1L,
        validFrom = now.minusDays(2),
        validTo = now.plusDays(10),
      )
      MarketingExposedH2TestSupport.flashSale(
        name = "他品",
        productId = 2L,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(10),
      )
      MarketingExposedH2TestSupport.flashSale(
        name = "停用",
        productId = 1L,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(10),
        status = "DISABLED",
      )
      MarketingExposedH2TestSupport.flashSale(
        name = "软删",
        productId = 1L,
        validFrom = now,
        validTo = now.plusDays(10),
        deleted = true,
      )

      val active = repo.findActiveByProductId(1L, PromotionActivityStatus.ACTIVE, now)
      active.shouldNotBeNull().name shouldBe "晚窗"
      repo.findActiveByProductId(3L, PromotionActivityStatus.ACTIVE, now) shouldBe null
      repo.findActiveByProductId(1L, PromotionActivityStatus.ACTIVE, now.plusDays(100)) shouldBe null
    }

    test("existsOverlap:时间窗重叠校验 + excludeId + 软删不冲突") {
      val base = LocalDateTime.of(2026, 5, 1, 0, 0)
      val overlapId =
        MarketingExposedH2TestSupport.flashSale(
          name = "重叠",
          productId = 1L,
          validFrom = base,
          validTo = base.plusDays(10),
        )
      MarketingExposedH2TestSupport.flashSale(
        name = "软删",
        productId = 1L,
        validFrom = base,
        validTo = base.plusDays(10),
        deleted = true,
      )

      repo.existsOverlap(1L, PromotionActivityStatus.ACTIVE, base.plusDays(5), base.plusDays(15), null) shouldBe true
      repo.existsOverlap(1L, PromotionActivityStatus.ACTIVE, base.minusDays(5), base.minusDays(1), null) shouldBe false
      repo
        .existsOverlap(1L, PromotionActivityStatus.ACTIVE, base.plusDays(5), base.plusDays(15), overlapId) shouldBe false
      repo.existsOverlap(2L, PromotionActivityStatus.ACTIVE, base.plusDays(5), base.plusDays(15), null) shouldBe false
      // 恰好端点相接(新 from == 旧 to)不算重叠(旧口径:validFrom < newValidTo && validTo > newValidFrom)
      repo.existsOverlap(1L, PromotionActivityStatus.ACTIVE, base.plusDays(10), base.plusDays(20), null) shouldBe false
    }
  })
