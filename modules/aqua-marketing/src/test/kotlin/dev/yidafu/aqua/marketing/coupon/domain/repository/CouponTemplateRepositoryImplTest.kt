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

import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.marketing.common.exposed.MarketingExposedH2TestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingExposedH2TestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest

/**
 * 优惠券模板仓储 Exposed 实现测试(H2):save/软删/派生查询/分页。
 */
class CouponTemplateRepositoryImplTest :
  FunSpec({
    val repo = CouponTemplateRepositoryImpl(MarketingExposedH2TestSupport.database)

    beforeTest { reset() }

    test("save 新增回填 id,findByName/findById 可查") {
      val saved = repo.save(CouponTemplateModel(name = "新人券"))
      saved.id.shouldNotBeNull()
      repo.findByName("新人券").shouldNotBeNull().id shouldBe saved.id
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .name shouldBe "新人券"
    }

    test("save 更新已有实体走 update 分支") {
      val id = MarketingExposedH2TestSupport.couponTemplate(name = "旧名", status = "ACTIVE")
      val loaded = repo.findById(id).orElse(null).shouldNotBeNull()
      loaded.name = "新名"
      loaded.status = CouponTemplateStatus.DISABLED
      repo.save(loaded)

      val reloaded = repo.findById(id).orElse(null).shouldNotBeNull()
      reloaded.name shouldBe "新名"
      reloaded.status shouldBe CouponTemplateStatus.DISABLED
      repo.findByName("旧名") shouldBe null
      repo.findByName("新名").shouldNotBeNull()
    }

    test("软删模板不可见:findByName/findByStatus/findById/findAllById 均过滤") {
      val keep = MarketingExposedH2TestSupport.couponTemplate(name = "存活", status = "ACTIVE")
      MarketingExposedH2TestSupport.couponTemplate(name = "已删", status = "ACTIVE", deleted = true)
      val other = MarketingExposedH2TestSupport.couponTemplate(name = "停用", status = "DISABLED")

      repo.findByName("已删") shouldBe null
      repo.findByStatus(CouponTemplateStatus.ACTIVE) shouldHaveSize 1
      repo.findById(other).isPresent shouldBe true
      repo.findAllById(listOf(keep, other, 999L)) shouldHaveSize 2
      repo.findAllById(emptyList()) shouldHaveSize 0
    }

    test("searchTemplates:关键词+状态,软删不进结果") {
      MarketingExposedH2TestSupport.couponTemplate(name = "春日券", status = "ACTIVE")
      MarketingExposedH2TestSupport.couponTemplate(name = "夏日券", status = "DISABLED")
      MarketingExposedH2TestSupport.couponTemplate(name = "春日隐藏券", status = "ACTIVE", deleted = true)

      repo.searchTemplates("券", null, PageRequest.of(0, 10)).totalElements shouldBe 2L
      repo.searchTemplates("春日", null, PageRequest.of(0, 10)).totalElements shouldBe 1L
      repo.searchTemplates(null, CouponTemplateStatus.ACTIVE, PageRequest.of(0, 10)).totalElements shouldBe 1L
      repo.searchTemplates("  ", null, PageRequest.of(0, 10)).totalElements shouldBe 2L
      repo.searchTemplates("不存在", null, PageRequest.of(0, 10)).totalElements shouldBe 0L
    }

    test("searchTemplates:固定 id 升序破平,跨页不重不漏") {
      val a = MarketingExposedH2TestSupport.couponTemplate(name = "A")
      val b = MarketingExposedH2TestSupport.couponTemplate(name = "B")
      val c = MarketingExposedH2TestSupport.couponTemplate(name = "C")

      val page0 = repo.searchTemplates(null, null, PageRequest.of(0, 2))
      val page1 = repo.searchTemplates(null, null, PageRequest.of(1, 2))
      page0.totalElements shouldBe 3L
      page0.content.map { it.id } shouldContainExactly listOf(a, b)
      page1.content.map { it.id } shouldContainExactly listOf(c)
    }
  })
