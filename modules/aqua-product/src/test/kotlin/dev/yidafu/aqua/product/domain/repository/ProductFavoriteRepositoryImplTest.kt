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
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 商品收藏仓储 Exposed 实现测试(Testcontainers 真实 PG):save 双分支/软删可见性/enable 过滤/分页排序/统计口径。
 */
class ProductFavoriteRepositoryImplTest :
  FunSpec({
    val repo = ProductFavoriteRepositoryImpl(ProductPgTestSupport.database)

    beforeTest { reset() }

    test("save 新增并回填雪花 id,findByUserIdAndProductId 可查") {
      val pid = ProductPgTestSupport.product()
      val saved = repo.save(ProductFavoriteModel(userId = 1L, productId = pid, enable = true))
      saved.id.shouldNotBeNull()
      val found = repo.findByUserIdAndProductId(1L, pid)
      found.shouldNotBeNull()
      found.id shouldBe saved.id
      found.enable shouldBe true
    }

    test("save 更新已有实体走 update 分支(enable 翻转)") {
      val pid = ProductPgTestSupport.product()
      val id = ProductPgTestSupport.favorite(userId = 1L, productId = pid, enable = true)
      val loaded = repo.findByUserIdAndProductId(1L, pid).shouldNotBeNull()
      repo.save(loaded.copy(enable = false))
      repo.findByUserIdAndProductId(1L, pid)!!.enable shouldBe false
      // 主键未变,行数仍为 1
      transaction(ProductPgTestSupport.database) {
        ProductFavoritesTable.selectAll().where { ProductFavoritesTable.id eq id }.count()
      } shouldBe 1L
    }

    test("软删收藏不可见:查询/count/exists 均过滤") {
      val pid = ProductPgTestSupport.product()
      ProductPgTestSupport.favorite(userId = 1L, productId = pid, deleted = true)

      repo.findByUserIdAndProductId(1L, pid) shouldBe null
      repo.count() shouldBe 0L
      repo.existsByUserIdAndProductId(1L, pid) shouldBe false
      repo.findFavoriteProductIdsByUserId(1L) shouldHaveSize 0
    }

    test("findByUserId 分页:仅该用户,软删过滤") {
      val p1 = ProductPgTestSupport.product()
      val p2 = ProductPgTestSupport.product()
      val p3 = ProductPgTestSupport.product()
      ProductPgTestSupport.favorite(userId = 1L, productId = p1)
      ProductPgTestSupport.favorite(userId = 1L, productId = p2)
      ProductPgTestSupport.favorite(userId = 2L, productId = p3)
      ProductPgTestSupport.favorite(userId = 1L, productId = ProductPgTestSupport.product(), deleted = true)

      val page = repo.findByUserId(1L, PageRequest.of(0, 10))
      page.totalElements shouldBe 2L
      page.content shouldHaveSize 2
      repo.findByUserId(1L, PageRequest.of(0, 1)).totalElements shouldBe 2L
      repo.findByUserId(1L, PageRequest.of(0, 1)).content shouldHaveSize 1
      repo.findByUserId(9L, PageRequest.of(0, 10)).totalElements shouldBe 0L
    }

    test("findByProductId:命中/无行/软删过滤") {
      val pid = ProductPgTestSupport.product()
      ProductPgTestSupport.favorite(userId = 1L, productId = pid)
      ProductPgTestSupport.favorite(userId = 2L, productId = pid, deleted = true)

      repo.findByProductId(pid) shouldHaveSize 1
      repo.findByProductId(999_999L) shouldHaveSize 0
    }

    test("enable 过滤:exists/countByUserId/findFavoriteProductIdsByUserId") {
      val p1 = ProductPgTestSupport.product()
      val p2 = ProductPgTestSupport.product()
      ProductPgTestSupport.favorite(userId = 1L, productId = p1, enable = true)
      ProductPgTestSupport.favorite(userId = 1L, productId = p2, enable = false)

      repo.existsByUserIdAndProductId(1L, p1) shouldBe true
      repo.existsByUserIdAndProductId(1L, p2) shouldBe false
      repo.countByUserId(1L) shouldBe 1L
      repo.findFavoriteProductIdsByUserId(1L) shouldContainExactly listOf(p1)
    }

    test("findFavoriteIdsByUserId:启用过滤+createdAt 倒序+分页+总数") {
      val base = LocalDateTime.of(2026, 3, 1, 12, 0)
      val p1 = ProductPgTestSupport.product()
      val p2 = ProductPgTestSupport.product()
      val p3 = ProductPgTestSupport.product()
      ProductPgTestSupport.favorite(userId = 7L, productId = p1, createdAt = base.plusHours(1))
      ProductPgTestSupport.favorite(userId = 7L, productId = p2, createdAt = base.plusHours(3))
      ProductPgTestSupport.favorite(userId = 7L, productId = p3, createdAt = base.plusHours(2))
      ProductPgTestSupport.favorite(
        userId = 7L,
        productId = ProductPgTestSupport.product(),
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
      val p1 = ProductPgTestSupport.product()
      val p2 = ProductPgTestSupport.product()
      val p3 = ProductPgTestSupport.product()
      val f1 = ProductPgTestSupport.favorite(userId = 7L, productId = p1, createdAt = at)
      val f2 = ProductPgTestSupport.favorite(userId = 7L, productId = p2, createdAt = at)
      val f3 = ProductPgTestSupport.favorite(userId = 7L, productId = p3, createdAt = at)

      // createdAt 全同 → 破平后按收藏行 id DESC 跨页切分,不重不漏
      val byFavoriteIdDesc =
        listOf(p1 to f1, p2 to f2, p3 to f3).sortedByDescending { it.second }.map { it.first }
      val page0 = repo.findFavoriteIdsByUserId(7L, PageRequest.of(0, 2))
      val page1 = repo.findFavoriteIdsByUserId(7L, PageRequest.of(1, 2))
      page0.content shouldContainExactly byFavoriteIdDesc.take(2)
      page1.content shouldContainExactly byFavoriteIdDesc.drop(2)
    }

    test("updateEnableStatus:仅更新活跃行") {
      val pid = ProductPgTestSupport.product()
      ProductPgTestSupport.favorite(userId = 1L, productId = pid, enable = true)

      repo.updateEnableStatus(1L, pid, false) shouldBe 1
      repo.findByUserIdAndProductId(1L, pid)!!.enable shouldBe false
      repo.updateEnableStatus(1L, 999_999L, true) shouldBe 0
    }

    test("统计口径:count/countDistinctUsersWithFavorites/countFavoritesSince") {
      val base = LocalDateTime.of(2026, 3, 1, 0, 0)
      val p1 = ProductPgTestSupport.product()
      val p2 = ProductPgTestSupport.product()
      ProductPgTestSupport.favorite(userId = 1L, productId = p1, createdAt = base)
      ProductPgTestSupport.favorite(userId = 2L, productId = p1, createdAt = base.plusDays(1))
      ProductPgTestSupport.favorite(userId = 2L, productId = p2, createdAt = base.plusDays(2))
      ProductPgTestSupport.favorite(userId = 3L, productId = p2, createdAt = base, deleted = true)

      repo.count() shouldBe 3L
      repo.countDistinctUsersWithFavorites() shouldBe 2L
      repo.countFavoritesSince(base.plusDays(1)) shouldBe 2L
      repo.countFavoritesSince(base.plusDays(10)) shouldBe 0L
    }

    test("findMostFavoritedProducts:SQL 侧分页+HAVING 过滤,软删不计,附最后收藏时间") {
      val base = LocalDateTime.of(2026, 3, 1, 0, 0)
      val p1 = ProductPgTestSupport.product()
      val p2 = ProductPgTestSupport.product()
      val p3 = ProductPgTestSupport.product()
      ProductPgTestSupport.favorite(userId = 1L, productId = p1, createdAt = base)
      ProductPgTestSupport.favorite(userId = 2L, productId = p1, createdAt = base.plusDays(1))
      ProductPgTestSupport.favorite(userId = 3L, productId = p1, createdAt = base.plusDays(2))
      ProductPgTestSupport.favorite(userId = 4L, productId = p2, createdAt = base)
      ProductPgTestSupport.favorite(userId = 5L, productId = p2, deleted = true)
      ProductPgTestSupport.favorite(userId = 6L, productId = p3, createdAt = base)

      // 第 1 页(每页 2 条):收藏数倒序,同数按商品 id 升序
      val page0 = repo.findMostFavoritedProducts(PageRequest.of(0, 2))
      page0.totalElements shouldBe 3L
      page0.content.map { it.productId } shouldContainExactly listOf(p1, p2)
      page0.content[0].favoriteCount shouldBe 3L
      page0.content[0].lastFavoritedAt shouldBe base.plusDays(2)

      // 第 2 页不重不漏
      val page1 = repo.findMostFavoritedProducts(PageRequest.of(1, 2))
      page1.content.map { it.productId } shouldContainExactly listOf(p3)

      // minFavorites 下推到 HAVING:只有 p1 满足 >= 2
      val filtered = repo.findMostFavoritedProducts(PageRequest.of(0, 10), minFavorites = 2L)
      filtered.totalElements shouldBe 1L
      filtered.content.single().productId shouldBe p1
    }

    test("findTopFavoritedProducts/countFavoritedProducts/countTotalFavorites:SQL 侧 limit 与聚合") {
      val p1 = ProductPgTestSupport.product()
      val p2 = ProductPgTestSupport.product()
      ProductPgTestSupport.favorite(userId = 1L, productId = p1)
      ProductPgTestSupport.favorite(userId = 2L, productId = p1)
      ProductPgTestSupport.favorite(userId = 3L, productId = p2)
      ProductPgTestSupport.favorite(userId = 4L, productId = p2, deleted = true)

      repo.findTopFavoritedProducts(1).map { it.productId } shouldContainExactly listOf(p1)
      repo.countFavoritedProducts() shouldBe 2L
      repo.countTotalFavorites() shouldBe 3L
    }

    test("findFavoritesForExport:过滤组合 + keyset 游标分批(软删不计)") {
      val base = LocalDateTime.of(2026, 3, 1, 0, 0)
      val pa = ProductPgTestSupport.product()
      val pb = ProductPgTestSupport.product()
      val f1 = ProductPgTestSupport.favorite(userId = 11L, productId = pa, createdAt = base)
      val f2 = ProductPgTestSupport.favorite(userId = 12L, productId = pb, createdAt = base.plusDays(5))
      val f3 = ProductPgTestSupport.favorite(userId = 11L, productId = pb, createdAt = base.plusDays(9))
      // 软删行:落在任一过滤维度内,均不应出现在导出结果
      ProductPgTestSupport.favorite(
        userId = 11L,
        productId = pa,
        createdAt = base.plusDays(3),
        deleted = true,
      )

      val all = repo.findFavoritesForExport(null, null, null, null, null, 100)
      all shouldHaveSize 3
      all.map { it.id } shouldContainExactly listOf(f1, f2, f3)
      repo.findFavoritesForExport(11L, null, null, null, null, 100) shouldHaveSize 2
      repo.findFavoritesForExport(null, listOf(pa), null, null, null, 100) shouldHaveSize 1
      repo.findFavoritesForExport(null, emptyList(), null, null, null, 100) shouldHaveSize 3
      repo.findFavoritesForExport(null, null, base.plusDays(1), null, null, 100) shouldHaveSize 2
      repo.findFavoritesForExport(null, null, null, base, null, 100) shouldHaveSize 1
      repo.findFavoritesForExport(null, null, base.plusDays(1), base.plusDays(8), null, 100) shouldHaveSize 1
      val scoped = repo.findFavoritesForExport(11L, listOf(pa, pb), base, base.plusDays(10), null, 100)
      scoped.map { it.id } shouldContainExactly listOf(f1, f3)

      // keyset:按 id 升序,afterId 之后取 limit 条 —— 导出分批不重不漏
      val firstBatch = repo.findFavoritesForExport(null, null, null, null, null, 2)
      firstBatch.map { it.id } shouldContainExactly listOf(f1, f2)
      val secondBatch = repo.findFavoritesForExport(null, null, null, null, firstBatch.last().id, 2)
      secondBatch.map { it.id } shouldContainExactly listOf(f3)
    }

    test("getAverageFavoritesPerUser:均值与无用户回退 0.0") {
      val p = ProductPgTestSupport.product()
      ProductPgTestSupport.favorite(userId = 1L, productId = p)
      ProductPgTestSupport.favorite(userId = 1L, productId = p)
      ProductPgTestSupport.favorite(userId = 2L, productId = p)

      repo.getAverageFavoritesPerUser() shouldBe 1.5
    }

    test("getAverageFavoritesPerUser:空表回退 0.0") {
      repo.getAverageFavoritesPerUser() shouldBe 0.0
      repo.count() shouldBe 0L
    }
  })
