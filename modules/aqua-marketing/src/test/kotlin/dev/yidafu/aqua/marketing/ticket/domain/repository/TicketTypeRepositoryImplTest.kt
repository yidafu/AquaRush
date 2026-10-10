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

package dev.yidafu.aqua.marketing.ticket.domain.repository

import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest

/**
 * 桶水票票种仓储 Exposed 实现测试(Testcontainers 真实 PG):save/软删/派生查询/分页。
 */
class TicketTypeRepositoryImplTest :
  FunSpec({
    val repo = TicketTypeRepositoryImpl(MarketingPgTestSupport.database)

    beforeTest { reset() }

    test("save 双分支 + findById/findByStatus") {
      val saved = repo.save(TicketTypeModel(name = "十次票", packageCount = 10, packagePriceCents = 1000L))
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .name shouldBe "十次票"
      repo.findByStatus(TicketTypeStatus.ACTIVE).map { it.id } shouldContainExactly listOf(saved.id)

      val loaded = repo.findById(saved.id!!).orElse(null).shouldNotBeNull()
      loaded.status = TicketTypeStatus.DISABLED
      loaded.packagePriceCents = 900L
      repo.save(loaded)
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .packagePriceCents shouldBe 900L
      repo.findByStatus(TicketTypeStatus.ACTIVE).size shouldBe 0
      repo.findByStatus(TicketTypeStatus.DISABLED).size shouldBe 1
    }

    test("软删票种不可见:findAll/findAllById/findByStatus/findById 均过滤") {
      val keep = MarketingPgTestSupport.ticketType(name = "存活")
      MarketingPgTestSupport.ticketType(name = "已删", deleted = true)

      repo.findById(keep).isPresent shouldBe true
      repo.findAll().map { it.id } shouldContainExactly repo.findAllById(listOf(keep)).map { it.id }
      repo.findAll().size shouldBe 1
      repo.findAllById(listOf(keep)).size shouldBe 1
      repo.findAllById(emptyList()).size shouldBe 0
      repo.findByStatus(TicketTypeStatus.ACTIVE).size shouldBe 1
    }

    test("searchTicketTypes:关键词+状态 + 软删过滤 + 分页 id 破平") {
      val a = MarketingPgTestSupport.ticketType(name = "十次票", status = "ACTIVE")
      val b = MarketingPgTestSupport.ticketType(name = "二十次票", status = "DISABLED")
      MarketingPgTestSupport.ticketType(name = "隐藏票", status = "ACTIVE", deleted = true)

      repo.searchTicketTypes("次票", null, PageRequest.of(0, 10)).totalElements shouldBe 2L
      repo.searchTicketTypes(null, TicketTypeStatus.ACTIVE, PageRequest.of(0, 10)).totalElements shouldBe 1L
      repo.searchTicketTypes("隐藏", null, PageRequest.of(0, 10)).totalElements shouldBe 0L
      repo.searchTicketTypes("  ", null, PageRequest.of(0, 10)).totalElements shouldBe 2L

      val page0 = repo.searchTicketTypes(null, null, PageRequest.of(0, 1))
      val page1 = repo.searchTicketTypes(null, null, PageRequest.of(1, 1))
      page0.content.map { it.id } shouldContainExactly listOf(a)
      page1.content.map { it.id } shouldContainExactly listOf(b)
    }
  })
