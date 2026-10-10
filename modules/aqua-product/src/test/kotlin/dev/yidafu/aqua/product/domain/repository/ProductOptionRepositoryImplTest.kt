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

package dev.yidafu.aqua.product.domain.repository

import dev.yidafu.aqua.common.domain.model.ProductOptionModel
import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType
import dev.yidafu.aqua.common.domain.table.ProductOptionsTable
import dev.yidafu.aqua.product.common.exposed.ProductPgTestSupport
import dev.yidafu.aqua.product.common.exposed.ProductPgTestSupport.reset
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
 * 商品属性选项字典仓储 Exposed 实现测试(Testcontainers 真实 PG):save 双分支/软删/排序派生查询/唯一性校验/最大排序号。
 */
class ProductOptionRepositoryImplTest :
  FunSpec({
    val repo = ProductOptionRepositoryImpl(ProductPgTestSupport.database)

    beforeTest { reset() }

    test("save 新增并回填雪花 id,findById 可查") {
      val option =
        ProductOptionModel(
          optionType = ProductOptionType.SPECIFICATION,
          value = "19L",
          sortOrder = 1,
        )
      val saved = repo.save(option)
      saved.id.shouldNotBeNull()
      val found = repo.findById(saved.id!!).orElse(null).shouldNotBeNull()
      found.optionType shouldBe ProductOptionType.SPECIFICATION
      found.value shouldBe "19L"
      found.sortOrder shouldBe 1
      found.enabled shouldBe true
    }

    test("save 更新已有实体走 update 分支") {
      val id = ProductPgTestSupport.option(value = "旧值", sortOrder = 5, enabled = true)
      val loaded = repo.findById(id).orElse(null).shouldNotBeNull()
      loaded.value = "新值"
      loaded.sortOrder = 7
      loaded.enabled = false
      repo.save(loaded)
      val reloaded = repo.findById(id).orElse(null).shouldNotBeNull()
      reloaded.value shouldBe "新值"
      reloaded.sortOrder shouldBe 7
      reloaded.enabled shouldBe false
      transaction(ProductPgTestSupport.database) {
        ProductOptionsTable.selectAll().where { ProductOptionsTable.id eq id }.count()
      } shouldBe 1L
    }

    test("delete 软删:查询不可见,行保留且 is_deleted=true") {
      val id = ProductPgTestSupport.option()
      val entity = repo.findById(id).orElse(null).shouldNotBeNull()
      repo.delete(entity)
      repo.findById(id).isPresent shouldBe false
      val row =
        transaction(ProductPgTestSupport.database) {
          ProductOptionsTable.selectAll().where { ProductOptionsTable.id eq id }.single()
        }
      row[ProductOptionsTable.isDeleted] shouldBe true
      row[ProductOptionsTable.deletedAt].shouldNotBeNull()
    }

    test("delete 对已软删行 no-op:deletedAt 不被重写,行仍不可见") {
      val id = ProductPgTestSupport.option()
      val entity = repo.findById(id).orElse(null).shouldNotBeNull()
      repo.delete(entity)
      // 哨兵值:若二次 delete 仍写库,deletedAt 会被 now 覆盖
      val sentinel = LocalDateTime.of(2020, 1, 1, 0, 0)
      transaction(ProductPgTestSupport.database) {
        ProductOptionsTable.update({ ProductOptionsTable.id eq id }) { it[ProductOptionsTable.deletedAt] = sentinel }
      }

      repo.delete(entity)

      val row =
        transaction(ProductPgTestSupport.database) {
          ProductOptionsTable.selectAll().where { ProductOptionsTable.id eq id }.single()
        }
      row[ProductOptionsTable.deletedAt] shouldBe sentinel
      row[ProductOptionsTable.isDeleted] shouldBe true
      repo.findById(id).isPresent shouldBe false
    }

    test("findByOptionTypeOrderBySortOrderAscCreatedAtAsc:排序断言+类型过滤+软删过滤") {
      val base = LocalDateTime.of(2026, 4, 1, 8, 0)
      val s2 =
        ProductPgTestSupport.option(
          optionType = "SPECIFICATION",
          value = "s2",
          sortOrder = 2,
          createdAt = base.plusMinutes(1),
        )
      val s1a =
        ProductPgTestSupport.option(
          optionType = "SPECIFICATION",
          value = "s1a",
          sortOrder = 1,
          createdAt = base.plusMinutes(2),
        )
      val s1b =
        ProductPgTestSupport.option(
          optionType = "SPECIFICATION",
          value = "s1b",
          sortOrder = 1,
          createdAt = base.plusMinutes(3),
        )
      ProductPgTestSupport.option(optionType = "SPECIFICATION", value = "删", sortOrder = 0, createdAt = base, deleted = true)
      ProductPgTestSupport.option(optionType = "WATER_SOURCE", value = "长白山", sortOrder = 1, createdAt = base)

      val list = repo.findByOptionTypeOrderBySortOrderAscCreatedAtAsc(ProductOptionType.SPECIFICATION)
      list.map { it.id } shouldContainExactly listOf(s1a, s1b, s2)
      repo.findByOptionTypeOrderBySortOrderAscCreatedAtAsc(ProductOptionType.WATER_SOURCE) shouldHaveSize 1
    }

    test("findByOptionTypeAndEnabledTrueOrderBySortOrderAscCreatedAtAsc:禁用项不返回") {
      ProductPgTestSupport.option(value = "启用", sortOrder = 1, enabled = true)
      ProductPgTestSupport.option(value = "禁用", sortOrder = 0, enabled = false)

      val list = repo.findByOptionTypeAndEnabledTrueOrderBySortOrderAscCreatedAtAsc(ProductOptionType.SPECIFICATION)
      list shouldHaveSize 1
      list.single().value shouldBe "启用"
      repo.findByOptionTypeAndEnabledTrueOrderBySortOrderAscCreatedAtAsc(ProductOptionType.WATER_SOURCE) shouldHaveSize 0
    }

    test("findByOptionTypeAndValue:唯一性校验命中/类型不符/软删行不可见") {
      val id = ProductPgTestSupport.option(value = "昆仑山")
      ProductPgTestSupport.option(optionType = "WATER_SOURCE", value = "长白山")
      ProductPgTestSupport.option(value = "已删值", deleted = true)

      repo.findByOptionTypeAndValue(ProductOptionType.SPECIFICATION, "昆仑山").shouldNotBeNull().id shouldBe id
      repo.findByOptionTypeAndValue(ProductOptionType.WATER_SOURCE, "昆仑山") shouldBe null
      repo.findByOptionTypeAndValue(ProductOptionType.SPECIFICATION, "已删值") shouldBe null
      repo.findByOptionTypeAndValue(ProductOptionType.SPECIFICATION, "不存在") shouldBe null
    }

    test("findMaxSortOrder:同类型最大值/类型隔离/软删不计/无行回退 null") {
      ProductPgTestSupport.option(value = "a", sortOrder = 4)
      ProductPgTestSupport.option(value = "b", sortOrder = 9)
      ProductPgTestSupport.option(value = "删", sortOrder = 99, deleted = true)
      ProductPgTestSupport.option(optionType = "WATER_SOURCE", value = "w", sortOrder = 50)

      repo.findMaxSortOrder(ProductOptionType.SPECIFICATION) shouldBe 9
      repo.findMaxSortOrder(ProductOptionType.WATER_SOURCE) shouldBe 50
    }

    test("findMaxSortOrder:空类型回退 null") {
      repo.findMaxSortOrder(ProductOptionType.WATER_SOURCE) shouldBe null
    }
  })
