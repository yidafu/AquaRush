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

import dev.yidafu.aqua.api.dto.ProductSearchRequest
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.domain.table.ProductsTable
import dev.yidafu.aqua.product.common.exposed.ProductExposedH2TestSupport
import dev.yidafu.aqua.product.common.exposed.ProductExposedH2TestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 商品仓储 Exposed 实现测试(H2):save 双分支/软删可见性/派生查询/分页/搜索/json 回环。
 */
class ProductRepositoryImplTest :
  FunSpec({
    val repo = ProductRepositoryImpl(ProductExposedH2TestSupport.database)

    beforeTest { reset() }

    test("save 新增并回填雪花 id,findById 可查") {
      val model =
        ProductModel(
          name = "山泉19L",
          price = 2000,
          coverImageUrl = "https://img.test/p.png",
          specification = "19L",
          stock = 5,
          status = ProductModelStatus.ONLINE,
        )
      val saved = repo.save(model)
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .name shouldBe "山泉19L"
    }

    test("save 更新已有实体走 update 分支") {
      val id = ProductExposedH2TestSupport.product(name = "旧名称", stock = 3)
      val loaded = repo.findById(id).orElse(null).shouldNotBeNull()
      loaded.name = "新名称"
      loaded.stock = 9
      loaded.status = ProductModelStatus.OFFLINE
      repo.save(loaded)
      val reloaded = repo.findById(id).orElse(null).shouldNotBeNull()
      reloaded.name shouldBe "新名称"
      reloaded.stock shouldBe 9
      reloaded.status shouldBe ProductModelStatus.OFFLINE
    }

    test("软删商品不可见:findById/existsById/findAll/findByStatus 均过滤") {
      val keep = ProductExposedH2TestSupport.product(name = "存活")
      val removed = ProductExposedH2TestSupport.product(name = "已删", deleted = true)

      repo.findById(keep).isPresent shouldBe true
      repo.findById(removed).isPresent shouldBe false
      repo.existsById(keep) shouldBe true
      repo.findAll() shouldHaveSize 1
      repo.findByStatus(ProductModelStatus.ONLINE) shouldHaveSize 1
    }

    test("findAllById:命中/缺失/空列表") {
      val a = ProductExposedH2TestSupport.product()
      val b = ProductExposedH2TestSupport.product()
      ProductExposedH2TestSupport.product(deleted = true)

      repo.findAllById(listOf(a, b)) shouldHaveSize 2
      repo.findAllById(listOf(a, 999_999L)) shouldHaveSize 1
      repo.findAllById(emptyList()) shouldHaveSize 0
    }

    test("deleteById 软删:查询不可见,行保留且 is_deleted=true") {
      val id = ProductExposedH2TestSupport.product()
      repo.deleteById(id)
      repo.findById(id).isPresent shouldBe false
      repo.existsById(id) shouldBe false
      val row =
        transaction(ProductExposedH2TestSupport.database) {
          ProductsTable.selectAll().where { ProductsTable.id eq id }.single()
        }
      row[ProductsTable.isDeleted] shouldBe true
      row[ProductsTable.deletedAt].shouldNotBeNull()
    }

    test("deleteById 对已软删行 no-op:deletedAt 不被重写,行仍不可见") {
      val id = ProductExposedH2TestSupport.product()
      repo.deleteById(id)
      // 哨兵值:若二次 delete 仍写库,deletedAt 会被 now 覆盖
      val sentinel = LocalDateTime.of(2020, 1, 1, 0, 0)
      transaction(ProductExposedH2TestSupport.database) {
        ProductsTable.update({ ProductsTable.id eq id }) { it[ProductsTable.deletedAt] = sentinel }
      }

      repo.deleteById(id)

      val row =
        transaction(ProductExposedH2TestSupport.database) {
          ProductsTable.selectAll().where { ProductsTable.id eq id }.single()
        }
      row[ProductsTable.deletedAt] shouldBe sentinel
      row[ProductsTable.isDeleted] shouldBe true
      repo.findById(id).isPresent shouldBe false
    }

    test("findByStatus 按状态过滤") {
      ProductExposedH2TestSupport.product(name = "上线", status = "ONLINE")
      ProductExposedH2TestSupport.product(name = "下架", status = "OFFLINE")

      repo.findByStatus(ProductModelStatus.ONLINE) shouldHaveSize 1
      repo.findByStatus(ProductModelStatus.OUT_OF_STOCK) shouldHaveSize 0
    }

    test("json 列回环:tags/detail_images 写入读回") {
      val model =
        ProductModel(
          name = "json商品",
          price = 100,
          coverImageUrl = "https://img.test/j.png",
          specification = "500ml",
          stock = 1,
        ).apply {
          setTagsFromList(listOf("a", "b"))
          setImageGalleryFromList(listOf("https://img.test/1.png"))
          detailContent = "详情正文"
        }
      val saved = repo.save(model)
      val savedId = saved.id!!
      val reloaded = repo.findById(savedId).orElse(null).shouldNotBeNull()
      reloaded.getTagsAsList() shouldContainExactly listOf("a", "b")
      reloaded.getImageGalleryAsList() shouldContainExactly listOf("https://img.test/1.png")
      reloaded.detailContent shouldBe "详情正文"
      // 原始列值逐字比对(写入读回)
      val rawTags =
        transaction(ProductExposedH2TestSupport.database) {
          ProductsTable
            .select(ProductsTable.tags)
            .where { ProductsTable.id eq savedId }
            .single()[ProductsTable.tags]
        }
      rawTags shouldBe """["a","b"]"""
    }

    test("searchProducts:关键词+状态+价格区间+分页") {
      ProductExposedH2TestSupport.product(name = "农夫山泉", priceCents = 2000L, status = "ONLINE")
      ProductExposedH2TestSupport.product(name = "怡宝纯净水", priceCents = 1500L, status = "OFFLINE")
      ProductExposedH2TestSupport.product(name = "农夫天然水", priceCents = 3000L, status = "ONLINE")

      val byKeyword =
        repo.searchProducts(ProductSearchRequest(keyword = "农夫"), PageRequest.of(0, 10))
      byKeyword.totalElements shouldBe 2L

      val byStatus =
        repo.searchProducts(ProductSearchRequest(status = ProductModelStatus.OFFLINE), PageRequest.of(0, 10))
      byStatus.totalElements shouldBe 1L

      // maxPrice 语义沿用原 QueryDSL:严格小于(不含边界)
      val byPrice =
        repo.searchProducts(
          ProductSearchRequest(minPrice = 1600L, maxPrice = 3000L),
          PageRequest.of(0, 10),
        )
      byPrice.totalElements shouldBe 1L
      byPrice.content.single().name shouldBe "农夫山泉"

      val page1 = repo.searchProducts(ProductSearchRequest(), PageRequest.of(0, 2))
      page1.totalElements shouldBe 3L
      page1.content shouldHaveSize 2
      val page2 = repo.searchProducts(ProductSearchRequest(), PageRequest.of(1, 2))
      page2.content shouldHaveSize 1
    }

    test("searchProducts:库存/销量过滤与空结果") {
      ProductExposedH2TestSupport.product(name = "低库存", stock = 2, salesVolume = 1)
      ProductExposedH2TestSupport.product(name = "高库存", stock = 99, salesVolume = 500)

      repo
        .searchProducts(
          ProductSearchRequest(minStock = 10, maxStock = 100, minSalesVolume = 100, maxSalesVolume = 1000),
          PageRequest.of(0, 10),
        ).totalElements shouldBe 1L
      repo
        .searchProducts(ProductSearchRequest(keyword = "不存在"), PageRequest.of(0, 10))
        .totalElements shouldBe 0L
      repo
        .searchProducts(ProductSearchRequest(keyword = "  "), PageRequest.of(0, 10))
        .totalElements shouldBe 2L
    }

    test("searchProducts:各排序模式与默认排序") {
      ProductExposedH2TestSupport.product(
        name = "A",
        priceCents = 100L,
        salesVolume = 5,
        sortOrder = 2,
        createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
      )
      ProductExposedH2TestSupport.product(
        name = "B",
        priceCents = 300L,
        salesVolume = 30,
        sortOrder = 1,
        createdAt = LocalDateTime.of(2026, 1, 3, 10, 0),
      )
      ProductExposedH2TestSupport.product(
        name = "C",
        priceCents = 200L,
        salesVolume = 20,
        sortOrder = 3,
        createdAt = LocalDateTime.of(2026, 1, 2, 10, 0),
      )

      fun names(sortBy: String?) = repo.searchProducts(ProductSearchRequest(sortBy = sortBy), PageRequest.of(0, 10)).content.map { it.name }

      names("PRICE_ASC") shouldContainExactly listOf("A", "C", "B")
      names("PRICE_DESC") shouldContainExactly listOf("B", "C", "A")
      names("SALES_VOLUME_DESC") shouldContainExactly listOf("B", "C", "A")
      names("SORT_ORDER_ASC") shouldContainExactly listOf("B", "A", "C")
      names("CREATED_AT_ASC") shouldContainExactly listOf("A", "C", "B")
      names("UNKNOWN") shouldContainExactly listOf("B", "C", "A")
      names(null) shouldContainExactly listOf("B", "C", "A")
    }

    test("searchProducts:同值排序键以 id 破平,跨页不重不漏") {
      val tie = LocalDateTime.of(2026, 5, 1, 8, 0)
      val a = ProductExposedH2TestSupport.product(name = "A", priceCents = 100L, createdAt = tie)
      val b = ProductExposedH2TestSupport.product(name = "B", priceCents = 100L, createdAt = tie)
      val c = ProductExposedH2TestSupport.product(name = "C", priceCents = 100L, createdAt = tie)

      // createdAt/price 全同 → 破平后按 id 切分跨页,不重不漏
      val desc = listOf(a, b, c).sortedDescending()
      val page0 = repo.searchProducts(ProductSearchRequest(), PageRequest.of(0, 2))
      val page1 = repo.searchProducts(ProductSearchRequest(), PageRequest.of(1, 2))
      page0.content.map { it.id } shouldContainExactly desc.take(2)
      page1.content.map { it.id } shouldContainExactly desc.drop(2)

      // 主键同向(ASC)破平
      repo
        .searchProducts(ProductSearchRequest(sortBy = "PRICE_ASC"), PageRequest.of(0, 3))
        .content
        .map { it.id } shouldContainExactly listOf(a, b, c).sorted()
    }

    test("searchProducts:软删商品不进搜索结果") {
      ProductExposedH2TestSupport.product(name = "可见商品")
      ProductExposedH2TestSupport.product(name = "可见商品二", deleted = true)

      val page = repo.searchProducts(ProductSearchRequest(keyword = "商品"), PageRequest.of(0, 10))
      page.totalElements shouldBe 1L
      page.content.single().name shouldBe "可见商品"
    }

    test("findAll 按 id 升序且不含软删") {
      val first = ProductExposedH2TestSupport.product(name = "一")
      val second = ProductExposedH2TestSupport.product(name = "二")
      ProductExposedH2TestSupport.product(name = "删", deleted = true)

      val all = repo.findAll()
      all shouldHaveSize 2
      all.map { it.id } shouldContainExactly listOf(first, second)
      all.map { it.name } shouldContainExactly listOf("一", "二")
    }
  })
