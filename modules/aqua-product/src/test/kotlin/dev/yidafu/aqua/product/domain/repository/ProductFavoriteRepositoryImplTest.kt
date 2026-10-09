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

import dev.yidafu.aqua.common.domain.model.ProductFavoriteModel
import dev.yidafu.aqua.common.domain.table.ProductFavoritesTable
import dev.yidafu.aqua.product.common.exposed.ProductExposedH2TestSupport
import dev.yidafu.aqua.product.common.exposed.ProductExposedH2TestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 商品收藏仓储 Exposed 实现测试(H2):save 双分支/软删可见性/enable 过滤/分页排序/统计口径。
 */
class ProductFavoriteRepositoryImplTest :
  FunSpec({
    val repo = ProductFavoriteRepositoryImpl(ProductExposedH2TestSupport.database)

    beforeTest { reset() }

    test("save 新增并回填雪花 id,findByUserIdAndProductId 可查") {
      val pid = ProductExposedH2TestSupport.product()
      val saved = repo.save(ProductFavoriteModel(userId = 1L, productId = pid, enable = true))
      saved.id.shouldNotBeNull()
      val found = repo.findByUserIdAndProductId(1L, pid)
      found.shouldNotBeNull()
      found.id shouldBe saved.id
      found.enable shouldBe true
    }

    test("save 更新已有实体走 update 分支(enable 翻转)") {
      val pid = ProductExposedH2TestSupport.product()
      val id = ProductExposedH2TestSupport.favorite(userId = 1L, productId = pid, enable = true)
      val loaded = repo.findByUserIdAndProductId(1L, pid).shouldNotBeNull()
      repo.save(loaded.copy(enable = false))
      repo.findByUserIdAndProductId(1L, pid)!!.enable shouldBe false
      // 主键未变,行数仍为 1
      transaction(ProductExposedH2TestSupport.database) {
        ProductFavoritesTable.selectAll().where { ProductFavoritesTable.id eq id }.count()
      } shouldBe 1L
    }

    test("软删收藏不可见:查询/count/exists 均过滤") {
      val pid = ProductExposedH2TestSupport.product()
      ProductExposedH2TestSupport.favorite(userId = 1L, productId = pid, deleted = true)

      repo.findByUserIdAndProductId(1L, pid) shouldBe null
      repo.count() shouldBe 0L
      repo.existsByUserIdAndProductId(1L, pid) shouldBe false
      repo.findFavoriteProductIdsByUserId(1L) shouldHaveSize 0
    }

    test("findByUserId 分页:仅该用户,软删过滤") {
      val p1 = ProductExposedH2TestSupport.product()
      val p2 = ProductExposedH2TestSupport.product()
      val p3 = ProductExposedH2TestSupport.product()
      ProductExposedH2TestSupport.favorite(userId = 1L, productId = p1)
      ProductExposedH2TestSupport.favorite(userId = 1L, productId = p2)
      ProductExposedH2TestSupport.favorite(userId = 2L, productId = p3)
      ProductExposedH2TestSupport.favorite(userId = 1L, productId = ProductExposedH2TestSupport.product(), deleted = true)

      val page = repo.findByUserId(1L, PageRequest.of(0, 10))
      page.totalElements shouldBe 2L
      page.content shouldHaveSize 2
      repo.findByUserId(1L, PageRequest.of(0, 1)).totalElements shouldBe 2L
      repo.findByUserId(1L, PageRequest.of(0, 1)).content shouldHaveSize 1
      repo.findByUserId(9L, PageRequest.of(0, 10)).totalElements shouldBe 0L
    }

    test("findByProductId:命中/无行/软删过滤") {
      val pid = ProductExposedH2TestSupport.product()
      ProductExposedH2TestSupport.favorite(userId = 1L, productId = pid)
      ProductExposedH2TestSupport.favorite(userId = 2L, productId = pid, deleted = true)

      repo.findByProductId(pid) shouldHaveSize 1
      repo.findByProductId(999_999L) shouldHaveSize 0
    }

    test("enable 过滤:exists/countByUserId/findFavoriteProductIdsByUserId") {
      val p1 = ProductExposedH2TestSupport.product()
      val p2 = ProductExposedH2TestSupport.product()
      ProductExposedH2TestSupport.favorite(userId = 1L, productId = p1, enable = true)
      ProductExposedH2TestSupport.favorite(userId = 1L, productId = p2, enable = false)

      repo.existsByUserIdAndProductId(1L, p1) shouldBe true
      repo.existsByUserIdAndProductId(1L, p2) shouldBe false
      repo.countByUserId(1L) shouldBe 1L
      repo.findFavoriteProductIdsByUserId(1L) shouldContainExactly listOf(p1)
    }

    test("findFavoriteIdsByUserId:启用过滤+createdAt 倒序+分页+总数") {
      val base = LocalDateTime.of(2026, 3, 1, 12, 0)
      val p1 = ProductExposedH2TestSupport.product()
      val p2 = ProductExposedH2TestSupport.product()
      val p3 = ProductExposedH2TestSupport.product()
      ProductExposedH2TestSupport.favorite(userId = 7L, productId = p1, createdAt = base.plusHours(1))
      ProductExposedH2TestSupport.favorite(userId = 7L, productId = p2, createdAt = base.plusHours(3))
      ProductExposedH2TestSupport.favorite(userId = 7L, productId = p3, createdAt = base.plusHours(2))
      ProductExposedH2TestSupport.favorite(
        userId = 7L,
        productId = ProductExposedH2TestSupport.product(),
        enable = false,
        createdAt = base.plusHours(4),
      )

      val page = repo.findFavoriteIdsByUserId(7L, PageRequest.of(0, 10))
      page.content shouldContainExactly listOf(p2, p3, p1)
      page.totalElements shouldBe 3L

      val paged = repo.findFavoriteIdsByUserId(7L, PageRequest.of(1, 2))
      paged.content shouldContainExactly listOf(p1)
      paged.totalElements shouldBe 3L
    }

    test("findFavoriteIdsByUserId:同 createdAt 以 id 破平,跨页不重不漏") {
      val at = LocalDateTime.of(2026, 3, 1, 12, 0)
      val p1 = ProductExposedH2TestSupport.product()
      val p2 = ProductExposedH2TestSupport.product()
      val p3 = ProductExposedH2TestSupport.product()
      val f1 = ProductExposedH2TestSupport.favorite(userId = 7L, productId = p1, createdAt = at)
      val f2 = ProductExposedH2TestSupport.favorite(userId = 7L, productId = p2, createdAt = at)
      val f3 = ProductExposedH2TestSupport.favorite(userId = 7L, productId = p3, createdAt = at)

      // createdAt 全同 → 破平后按收藏行 id DESC 跨页切分,不重不漏
      val byFavoriteIdDesc =
        listOf(p1 to f1, p2 to f2, p3 to f3).sortedByDescending { it.second }.map { it.first }
      val page0 = repo.findFavoriteIdsByUserId(7L, PageRequest.of(0, 2))
      val page1 = repo.findFavoriteIdsByUserId(7L, PageRequest.of(1, 2))
      page0.content shouldContainExactly byFavoriteIdDesc.take(2)
      page1.content shouldContainExactly byFavoriteIdDesc.drop(2)
    }

    test("updateEnableStatus:仅更新活跃行") {
      val pid = ProductExposedH2TestSupport.product()
      ProductExposedH2TestSupport.favorite(userId = 1L, productId = pid, enable = true)

      repo.updateEnableStatus(1L, pid, false) shouldBe 1
      repo.findByUserIdAndProductId(1L, pid)!!.enable shouldBe false
      repo.updateEnableStatus(1L, 999_999L, true) shouldBe 0
    }

    test("统计口径:count/countDistinctUsersWithFavorites/countFavoritesSince") {
      val base = LocalDateTime.of(2026, 3, 1, 0, 0)
      val p1 = ProductExposedH2TestSupport.product()
      val p2 = ProductExposedH2TestSupport.product()
      ProductExposedH2TestSupport.favorite(userId = 1L, productId = p1, createdAt = base)
      ProductExposedH2TestSupport.favorite(userId = 2L, productId = p1, createdAt = base.plusDays(1))
      ProductExposedH2TestSupport.favorite(userId = 2L, productId = p2, createdAt = base.plusDays(2))
      ProductExposedH2TestSupport.favorite(userId = 3L, productId = p2, createdAt = base, deleted = true)

      repo.count() shouldBe 3L
      repo.countDistinctUsersWithFavorites() shouldBe 2L
      repo.countFavoritesSince(base.plusDays(1)) shouldBe 2L
      repo.countFavoritesSince(base.plusDays(10)) shouldBe 0L
    }

    test("findMostFavoritedProducts:按收藏数倒序,软删不计") {
      val p1 = ProductExposedH2TestSupport.product()
      val p2 = ProductExposedH2TestSupport.product()
      ProductExposedH2TestSupport.favorite(userId = 1L, productId = p1)
      ProductExposedH2TestSupport.favorite(userId = 2L, productId = p1)
      ProductExposedH2TestSupport.favorite(userId = 3L, productId = p1)
      ProductExposedH2TestSupport.favorite(userId = 4L, productId = p2)
      ProductExposedH2TestSupport.favorite(userId = 5L, productId = p2, deleted = true)

      val counts = repo.findMostFavoritedProducts()
      counts shouldHaveSize 2
      counts[0].productId shouldBe p1
      counts[0].favoriteCount shouldBe 3L
      counts[1].productId shouldBe p2
      counts[1].favoriteCount shouldBe 1L
    }

    test("findFavoritesForExport:过滤组合与空过滤回退") {
      val base = LocalDateTime.of(2026, 3, 1, 0, 0)
      val pa = ProductExposedH2TestSupport.product()
      val pb = ProductExposedH2TestSupport.product()
      val f1 = ProductExposedH2TestSupport.favorite(userId = 11L, productId = pa, createdAt = base)
      val f2 = ProductExposedH2TestSupport.favorite(userId = 12L, productId = pb, createdAt = base.plusDays(5))
      val f3 = ProductExposedH2TestSupport.favorite(userId = 11L, productId = pb, createdAt = base.plusDays(9))
      // 软删行:落在任一过滤维度内,均不应出现在导出结果
      ProductExposedH2TestSupport.favorite(
        userId = 11L,
        productId = pa,
        createdAt = base.plusDays(3),
        deleted = true,
      )

      val all = repo.findFavoritesForExport(null, null, null, null)
      all shouldHaveSize 3
      all.map { it.id } shouldContainExactly listOf(f1, f2, f3)
      repo.findFavoritesForExport(11L, null, null, null) shouldHaveSize 2
      repo.findFavoritesForExport(null, listOf(pa), null, null) shouldHaveSize 1
      repo.findFavoritesForExport(null, emptyList(), null, null) shouldHaveSize 3
      repo.findFavoritesForExport(null, null, base.plusDays(1), null) shouldHaveSize 2
      repo.findFavoritesForExport(null, null, null, base) shouldHaveSize 1
      repo.findFavoritesForExport(null, null, base.plusDays(1), base.plusDays(8)) shouldHaveSize 1
      val scoped = repo.findFavoritesForExport(11L, listOf(pa, pb), base, base.plusDays(10))
      scoped.map { it.id } shouldContainExactly listOf(f1, f3)
    }

    test("getAverageFavoritesPerUser:均值与无用户回退 0.0") {
      val p = ProductExposedH2TestSupport.product()
      ProductExposedH2TestSupport.favorite(userId = 1L, productId = p)
      ProductExposedH2TestSupport.favorite(userId = 1L, productId = p)
      ProductExposedH2TestSupport.favorite(userId = 2L, productId = p)

      repo.getAverageFavoritesPerUser() shouldBe 1.5
    }

    test("getAverageFavoritesPerUser:空表回退 0.0") {
      repo.getAverageFavoritesPerUser() shouldBe 0.0
      repo.count() shouldBe 0L
    }
  })
