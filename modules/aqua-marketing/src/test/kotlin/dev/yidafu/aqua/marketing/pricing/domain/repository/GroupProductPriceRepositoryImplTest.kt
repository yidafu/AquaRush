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

package dev.yidafu.aqua.marketing.pricing.domain.repository

import dev.yidafu.aqua.common.domain.model.GroupProductPriceModel
import dev.yidafu.aqua.common.domain.table.GroupProductPricesTable
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
 * 分组商品专属价仓储 Exposed 实现测试(H2):save/软删与双删 no-op/派生查询/分页。
 */
class GroupProductPriceRepositoryImplTest :
  FunSpec({
    val repo = GroupProductPriceRepositoryImpl(MarketingExposedH2TestSupport.database)

    beforeTest { reset() }

    test("save 新增回填 id,按组+商品命中唯一行") {
      val saved = repo.save(GroupProductPriceModel(customerGroupId = 1L, productId = 10L, priceCents = 800L))
      saved.id.shouldNotBeNull()
      repo
        .findByCustomerGroupIdAndProductId(1L, 10L)
        .shouldNotBeNull()
        .priceCents shouldBe 800L
      repo.findByCustomerGroupIdAndProductId(1L, 999L) shouldBe null
    }

    test("save 更新已有实体走 update 分支") {
      val groupId = MarketingExposedH2TestSupport.customerGroup(name = "G1")
      val id = MarketingExposedH2TestSupport.groupProductPrice(groupId = groupId, productId = 10L, priceCents = 900L)
      val loaded =
        repo
          .findByCustomerGroupId(groupId)
          .single()
          .also { it.priceCents = 700L }
      loaded.id shouldBe id
      repo.save(loaded)
      repo
        .findByCustomerGroupIdAndProductId(groupId, 10L)
        .shouldNotBeNull()
        .priceCents shouldBe 700L
    }

    test("软删行不可见:列表/单查/分页均过滤") {
      val groupId = MarketingExposedH2TestSupport.customerGroup(name = "G2")
      MarketingExposedH2TestSupport.groupProductPrice(groupId = groupId, productId = 1L)
      MarketingExposedH2TestSupport.groupProductPrice(groupId = groupId, productId = 2L, deleted = true)
      MarketingExposedH2TestSupport.groupProductPrice(groupId = 777L, productId = 1L)

      repo.findByCustomerGroupId(groupId) shouldHaveSize 1
      repo.findByCustomerGroupIdAndProductId(groupId, 2L) shouldBe null
      repo.findByCustomerGroupId(groupId, PageRequest.of(0, 10)).totalElements shouldBe 1L
    }

    test("findByCustomerGroupId 分页:id 升序破平,跨页不重不漏") {
      val groupId = MarketingExposedH2TestSupport.customerGroup(name = "G3")
      val a = MarketingExposedH2TestSupport.groupProductPrice(groupId = groupId, productId = 1L)
      val b = MarketingExposedH2TestSupport.groupProductPrice(groupId = groupId, productId = 2L)
      val c = MarketingExposedH2TestSupport.groupProductPrice(groupId = groupId, productId = 3L)

      val page0 = repo.findByCustomerGroupId(groupId, PageRequest.of(0, 2))
      val page1 = repo.findByCustomerGroupId(groupId, PageRequest.of(1, 2))
      page0.totalElements shouldBe 3L
      page0.content.map { it.id } shouldContainExactly listOf(a, b)
      page1.content.map { it.id } shouldContainExactly listOf(c)
    }

    test("delete 软删三列写入,二次 delete no-op(deletedAt 哨兵不被重写)") {
      val groupId = MarketingExposedH2TestSupport.customerGroup(name = "G4")
      val id = MarketingExposedH2TestSupport.groupProductPrice(groupId = groupId, productId = 1L)
      val entity = repo.findByCustomerGroupId(groupId).single()

      repo.delete(entity)
      repo.findByCustomerGroupId(groupId) shouldHaveSize 0

      val sentinel = LocalDateTime.of(2020, 1, 1, 0, 0)
      transaction(MarketingExposedH2TestSupport.database) {
        GroupProductPricesTable.update({ GroupProductPricesTable.id eq id }) {
          it[GroupProductPricesTable.deletedAt] = sentinel
        }
      }
      repo.delete(entity)
      val row =
        transaction(MarketingExposedH2TestSupport.database) {
          GroupProductPricesTable.selectAll().where { GroupProductPricesTable.id eq id }.single()
        }
      row[GroupProductPricesTable.isDeleted] shouldBe true
      row[GroupProductPricesTable.deletedAt] shouldBe sentinel
    }
  })
