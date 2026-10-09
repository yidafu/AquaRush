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

package dev.yidafu.aqua.marketing.gift.domain.repository

import dev.yidafu.aqua.common.domain.model.GiftActivityModel
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
 * 买赠活动仓储 Exposed 实现测试(H2):save/软删/分页/findActiveActivities。
 */
class GiftActivityRepositoryImplTest :
  FunSpec({
    val repo = GiftActivityRepositoryImpl(MarketingExposedH2TestSupport.database)

    beforeTest { reset() }

    test("save 双分支 + findById/findAllById 软删过滤") {
      val saved = repo.save(GiftActivityModel(name = "满赠A", giftProductId = 8L, giftQuantity = 1))
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .giftProductId shouldBe 8L

      val loaded = repo.findById(saved.id!!).orElse(null).shouldNotBeNull()
      loaded.giftQuantity = 2
      repo.save(loaded)
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .giftQuantity shouldBe 2

      val gone = MarketingExposedH2TestSupport.giftActivity(name = "已删", deleted = true)
      repo.findById(gone).isPresent shouldBe false
      repo.findAllById(listOf(saved.id!!, gone)).map { it.id } shouldContainExactly listOf(saved.id)
      repo.findAllById(emptyList()).size shouldBe 0
    }

    test("searchActivities:关键词 + 分页 id 破平 + 软删过滤") {
      val a = MarketingExposedH2TestSupport.giftActivity(name = "满赠一")
      val b = MarketingExposedH2TestSupport.giftActivity(name = "买赠二")
      MarketingExposedH2TestSupport.giftActivity(name = "隐藏满赠", deleted = true)

      repo.searchActivities("赠", PageRequest.of(0, 10)).totalElements shouldBe 2L
      repo.searchActivities("隐藏", PageRequest.of(0, 10)).totalElements shouldBe 0L

      val page0 = repo.searchActivities(null, PageRequest.of(0, 1))
      val page1 = repo.searchActivities(null, PageRequest.of(1, 1))
      page0.content.map { it.id } shouldContainExactly listOf(a)
      page1.content.map { it.id } shouldContainExactly listOf(b)
    }

    test("findActiveActivities:ACTIVE 且时间窗内,软删排除") {
      val now = LocalDateTime.of(2026, 5, 1, 0, 0)
      MarketingExposedH2TestSupport.giftActivity(name = "生效", validFrom = now.minusDays(1), validTo = now.plusDays(1))
      MarketingExposedH2TestSupport.giftActivity(name = "过期", validFrom = now.minusDays(10), validTo = now.minusDays(5))
      MarketingExposedH2TestSupport.giftActivity(
        name = "停用",
        validFrom = now.minusDays(1),
        validTo = now.plusDays(1),
        status = "DISABLED",
      )
      MarketingExposedH2TestSupport.giftActivity(
        name = "软删",
        validFrom = now.minusDays(1),
        validTo = now.plusDays(1),
        deleted = true,
      )

      val active = repo.findActiveActivities(PromotionActivityStatus.ACTIVE, now)
      active.size shouldBe 1
      active.single().name shouldBe "生效"
    }
  })
