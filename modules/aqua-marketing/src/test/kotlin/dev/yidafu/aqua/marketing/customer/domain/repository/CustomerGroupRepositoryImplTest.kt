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

package dev.yidafu.aqua.marketing.customer.domain.repository

import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.common.domain.table.CustomerGroupsTable
import dev.yidafu.aqua.marketing.common.exposed.MarketingExposedH2TestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingExposedH2TestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 客户分组仓储 Exposed 实现测试(H2):save/软删与双删 no-op/派生查询/分页。
 */
class CustomerGroupRepositoryImplTest :
  FunSpec({
    val repo = CustomerGroupRepositoryImpl(MarketingExposedH2TestSupport.database)

    beforeTest { reset() }

    test("save 新增回填 id,findByName/existsByName/existsById 可查") {
      val saved = repo.save(CustomerGroupModel(name = "VIP组"))
      saved.id.shouldNotBeNull()
      repo.findByName("VIP组").shouldNotBeNull().id shouldBe saved.id
      repo.existsByName("VIP组") shouldBe true
      repo.existsByName("不存在") shouldBe false
      repo.existsById(saved.id!!) shouldBe true
      repo.existsById(999L) shouldBe false
    }

    test("save 更新已有实体走 update 分支") {
      val id = MarketingExposedH2TestSupport.customerGroup(name = "旧名")
      val loaded = repo.findById(id).orElse(null).shouldNotBeNull()
      loaded.name = "新名"
      loaded.description = "描述"
      repo.save(loaded)

      val reloaded = repo.findById(id).orElse(null).shouldNotBeNull()
      reloaded.name shouldBe "新名"
      reloaded.description shouldBe "描述"
      repo.findByName("旧名") shouldBe null
    }

    test("软删分组不可见:各查询均过滤,findAllById 按 id 升序") {
      val first = MarketingExposedH2TestSupport.customerGroup(name = "一")
      val second = MarketingExposedH2TestSupport.customerGroup(name = "二")
      MarketingExposedH2TestSupport.customerGroup(name = "删", deleted = true)

      repo.findById(first).isPresent shouldBe true
      repo.findById(second).isPresent shouldBe true
      repo.existsByName("删") shouldBe false
      repo.findAllById(listOf(first, second, 999L)).map { it.id } shouldContainExactly listOf(first, second)
      repo.findAllById(listOf(first, 999L)) shouldHaveSize 1
      repo.findAllById(emptyList()) shouldHaveSize 0
    }

    test("delete 软删三列写入,二次 delete no-op(deletedAt 哨兵不被重写)") {
      val id = MarketingExposedH2TestSupport.customerGroup(name = "待删")
      val entity = repo.findById(id).orElse(null).shouldNotBeNull()

      repo.delete(entity)
      repo.findById(id).isPresent shouldBe false
      val row =
        transaction(MarketingExposedH2TestSupport.database) {
          CustomerGroupsTable.selectAll().where { CustomerGroupsTable.id eq id }.single()
        }
      row[CustomerGroupsTable.isDeleted] shouldBe true
      row[CustomerGroupsTable.deletedAt].shouldNotBeNull()

      // 哨兵:二次 delete 不重写 deleted_at
      val sentinel = LocalDateTime.of(2020, 1, 1, 0, 0)
      transaction(MarketingExposedH2TestSupport.database) {
        CustomerGroupsTable.update({ CustomerGroupsTable.id eq id }) { it[CustomerGroupsTable.deletedAt] = sentinel }
      }
      repo.delete(entity)
      val after =
        transaction(MarketingExposedH2TestSupport.database) {
          CustomerGroupsTable.selectAll().where { CustomerGroupsTable.id eq id }.single()
        }
      after[CustomerGroupsTable.deletedAt] shouldBe sentinel
      after[CustomerGroupsTable.isDeleted] shouldBe true
    }

    test("searchGroups:关键词过滤,createdAt 降序,软删不进结果") {
      val older = LocalDateTime.of(2026, 1, 1, 10, 0)
      val newer = LocalDateTime.of(2026, 6, 1, 10, 0)
      MarketingExposedH2TestSupport.customerGroup(name = "熟客A", createdAt = older)
      MarketingExposedH2TestSupport.customerGroup(name = "熟客B", createdAt = newer)
      MarketingExposedH2TestSupport.customerGroup(name = "熟客C", createdAt = newer, deleted = true)
      MarketingExposedH2TestSupport.customerGroup(name = "新客", createdAt = newer)

      repo.searchGroups("熟客", PageRequest.of(0, 10)).totalElements shouldBe 2L
      // createdAt 降序 + id 破平(同 newer 时间戳的组内按 id 降序)
      repo
        .searchGroups(null, PageRequest.of(0, 10))
        .content
        .map { it.name } shouldContainExactly listOf("新客", "熟客B", "熟客A")
      repo.searchGroups("不存在", PageRequest.of(0, 10)).totalElements shouldBe 0L
      repo.searchGroups("  ", PageRequest.of(0, 10)).totalElements shouldBe 3L
    }

    test("searchGroups:同 createdAt 以 id 破平,跨页不重不漏") {
      val tie = LocalDateTime.of(2026, 5, 1, 8, 0)
      val a = MarketingExposedH2TestSupport.customerGroup(name = "A", createdAt = tie)
      val b = MarketingExposedH2TestSupport.customerGroup(name = "B", createdAt = tie)
      val c = MarketingExposedH2TestSupport.customerGroup(name = "C", createdAt = tie)

      val page0 = repo.searchGroups(null, PageRequest.of(0, 2))
      val page1 = repo.searchGroups(null, PageRequest.of(1, 2))
      page0.totalElements shouldBe 3L
      val desc = listOf(c, b, a)
      page0.content.map { it.id } shouldContainExactly desc.take(2)
      page1.content.map { it.id } shouldContainExactly desc.drop(2)
    }
  })
