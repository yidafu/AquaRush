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

package dev.yidafu.aqua.marketing.barrel.domain.repository

import dev.yidafu.aqua.common.domain.model.BarrelFeeModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelFeeStatus
import dev.yidafu.aqua.common.domain.table.BarrelFeesTable
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 水桶费价目仓储 Exposed 实现测试(Testcontainers 真实 PG):save 双分支/软删可见性/派生查询/分页。
 */
class BarrelFeeRepositoryImplTest :
  FunSpec({
    val repo = BarrelFeeRepositoryImpl(MarketingPgTestSupport.database)

    beforeTest { reset() }

    test("save 新增并回填雪花 id,findById 可查") {
      val saved = repo.save(BarrelFeeModel(specification = "19L/桶", priceCents = 3000L))
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .specification shouldBe "19L/桶"
    }

    test("save 更新已有实体走 update 分支") {
      val id = MarketingPgTestSupport.barrelFee(specification = "12L/桶", priceCents = 2000L)
      val loaded = repo.findById(id).orElse(null).shouldNotBeNull()
      loaded.priceCents = 2500L
      loaded.status = BarrelFeeStatus.DISABLED
      repo.save(loaded)
      val reloaded = repo.findById(id).orElse(null).shouldNotBeNull()
      reloaded.priceCents shouldBe 2500L
      reloaded.status shouldBe BarrelFeeStatus.DISABLED
    }

    test("软删价目不可见:findById/findBySpecification/查询均过滤") {
      val keep = MarketingPgTestSupport.barrelFee(specification = "存活")
      val removed = MarketingPgTestSupport.barrelFee(specification = "已删", deleted = true)

      repo.findById(keep).isPresent shouldBe true
      repo.findById(removed).isPresent shouldBe false
      repo.findBySpecification("已删") shouldBe null
      repo.findBySpecification("存活").shouldNotBeNull()
      repo.findAllByStatusOrderBySortOrderAscIdAsc(BarrelFeeStatus.ENABLED) shouldHaveSize 1
    }

    test("findBySpecification 命中唯一行") {
      MarketingPgTestSupport.barrelFee(specification = "19L/桶")
      repo.findBySpecification("19L/桶").shouldNotBeNull().specification shouldBe "19L/桶"
      repo.findBySpecification("不存在") shouldBe null
    }

    test("findAllByStatusOrderBySortOrderAscIdAsc:状态过滤 + sortOrder 升序 + id 破平") {
      MarketingPgTestSupport.barrelFee(specification = "A", status = "ENABLED", sortOrder = 2)
      MarketingPgTestSupport.barrelFee(specification = "B", status = "ENABLED", sortOrder = 1)
      MarketingPgTestSupport.barrelFee(specification = "C", status = "DISABLED", sortOrder = 0)

      repo
        .findAllByStatusOrderBySortOrderAscIdAsc(BarrelFeeStatus.ENABLED)
        .map { it.specification } shouldContainExactly listOf("B", "A")
      repo.findAllByStatusOrderBySortOrderAscIdAsc(BarrelFeeStatus.DISABLED) shouldHaveSize 1
    }

    test("searchFees:关键词+状态过滤,软删不进结果,空关键词不过滤") {
      MarketingPgTestSupport.barrelFee(specification = "19L/桶", status = "ENABLED")
      MarketingPgTestSupport.barrelFee(specification = "12L/桶", status = "DISABLED")
      MarketingPgTestSupport.barrelFee(specification = "已删19L", status = "ENABLED", deleted = true)

      repo
        .searchFees(null, "19L", PageRequest.of(0, 10))
        .totalElements shouldBe 1L
      repo
        .searchFees(BarrelFeeStatus.DISABLED, null, PageRequest.of(0, 10))
        .totalElements shouldBe 1L
      repo
        .searchFees(null, "  ", PageRequest.of(0, 10))
        .totalElements shouldBe 2L
      repo
        .searchFees(null, "不存在", PageRequest.of(0, 10))
        .totalElements shouldBe 0L
    }

    test("searchFees:固定 sortOrder 升序 + 同值 id 破平,跨页不重不漏") {
      val tieSort = 5
      val a = MarketingPgTestSupport.barrelFee(specification = "A", sortOrder = tieSort)
      val b = MarketingPgTestSupport.barrelFee(specification = "B", sortOrder = tieSort)
      val c = MarketingPgTestSupport.barrelFee(specification = "C", sortOrder = tieSort)

      val page0 = repo.searchFees(null, null, PageRequest.of(0, 2))
      val page1 = repo.searchFees(null, null, PageRequest.of(1, 2))
      page0.totalElements shouldBe 3L
      page0.content.map { it.id } shouldContainExactly listOf(a, b)
      page1.content.map { it.id } shouldContainExactly listOf(c)
    }

    test("save 更新不重写 createdAt,行内 updated_at 刷新") {
      val created = LocalDateTime.of(2026, 1, 1, 10, 0)
      val id = MarketingPgTestSupport.barrelFee(specification = "时间", createdAt = created)
      val loaded = repo.findById(id).orElse(null).shouldNotBeNull()
      loaded.priceCents = 1L
      repo.save(loaded)

      val row =
        transaction(MarketingPgTestSupport.database) {
          BarrelFeesTable.selectAll().andWhere { BarrelFeesTable.id eq id }.single()
        }
      row[BarrelFeesTable.createdAt] shouldBe created
      (row[BarrelFeesTable.updatedAt] > created) shouldBe true
    }

    test("save 对已软删行仍可更新(存在判定用 selectAllIncludingDeleted)") {
      val id = MarketingPgTestSupport.barrelFee(specification = "已删", deleted = true)
      val loaded = repo.findById(id).orElse(null)
      // findById 过滤软删 → 不可见;直接构造实体按 id 更新
      loaded shouldBe null
      val entity = BarrelFeeModel(id = id, specification = "已删", priceCents = 1L)
      repo.save(entity)
      // 行仍软删,更新分支命中(不会插入重复行)
      val row =
        transaction(MarketingPgTestSupport.database) {
          BarrelFeesTable.selectAllIncludingDeleted().where { BarrelFeesTable.id eq id }.single()
        }
      row[BarrelFeesTable.isDeleted] shouldBe true
      row[BarrelFeesTable.priceCents] shouldBe 1L
    }
  })
