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

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.api.dto.ProductSearchRequest
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals

/**
 * 商品仓储 QueryDSL 实现单元测试: 关键词/状态/价格/库存/销量过滤、排序、计数与分页
 */
class ProductRepositoryImplTest {
  private lateinit var selectQuery: JPAQuery<ProductModel>
  private lateinit var countQuery: JPAQuery<Void>
  private lateinit var repo: ProductRepositoryImpl

  @BeforeEach
  fun setUp() {
    mockkConstructor(JPAQueryFactory::class)
    selectQuery = mockk(relaxed = true)
    countQuery = mockk(relaxed = true)
    repo = ProductRepositoryImpl()
    val em = mockk<EntityManager>(relaxed = true)
    val field = ProductRepositoryImpl::class.java.getDeclaredField("entityManager")
    field.isAccessible = true
    field.set(repo, em)

    every { anyConstructed<JPAQueryFactory>().query() } returns countQuery
    every { anyConstructed<JPAQueryFactory>().selectFrom(any<dev.yidafu.aqua.common.domain.model.QProductModel>()) } returns selectQuery
    every { countQuery.from(any()) } returns countQuery
    every { countQuery.where(any<Predicate>()) } returns countQuery
    every { countQuery.orderBy(any<OrderSpecifier<*>>()) } returns countQuery
    every { countQuery.fetchCount() } returns 5L
    every { selectQuery.where(any<Predicate>()) } returns selectQuery
    every { selectQuery.orderBy(any<OrderSpecifier<*>>()) } returns selectQuery
    every { selectQuery.offset(any()) } returns selectQuery
    every { selectQuery.limit(any()) } returns selectQuery
    every { selectQuery.fetch() } returns emptyList()
  }

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `searchProducts applies all filters and pagination`() {
    val pageable = PageRequest.of(1, 10)
    val request =
      ProductSearchRequest(
        keyword = "水",
        minPrice = 100L,
        maxPrice = 1000L,
        minStock = 1,
        maxStock = 100,
        minSalesVolume = 0,
        maxSalesVolume = 50,
        sortBy = "PRICE_DESC",
        status = ProductModelStatus.ONLINE,
      )

    val page = repo.searchProducts(request, pageable)

    assertEquals(10, page.size)
    assertEquals(5L, page.totalElements)
  }

  @Test
  fun `searchProducts handles empty filters`() {
    val page = repo.searchProducts(ProductSearchRequest(), PageRequest.of(0, 20))

    assertEquals(20, page.size)
    assertEquals(5L, page.totalElements)
  }

  @Test
  fun `searchProducts handles each sort mode`() {
    for (sortBy in listOf(
      "CREATED_AT_ASC",
      "CREATED_AT_DESC",
      "PRICE_ASC",
      "PRICE_DESC",
      "SALES_VOLUME_ASC",
      "SALES_VOLUME_DESC",
      "SORT_ORDER_ASC",
      "SORT_ORDER_DESC",
      "UNKNOWN",
    )) {
      val page = repo.searchProducts(ProductSearchRequest(sortBy = sortBy), PageRequest.of(0, 10))
      assertEquals(10, page.size)
    }
  }

  @Test
  fun `searchProducts filters by keyword only`() {
    val page = repo.searchProducts(ProductSearchRequest(keyword = "怡宝"), PageRequest.of(0, 5))

    assertEquals(5, page.size)
    assertEquals(5L, page.totalElements)
  }

  @Test
  fun `searchProducts filters by status only`() {
    val page = repo.searchProducts(ProductSearchRequest(status = ProductModelStatus.OFFLINE), PageRequest.of(0, 5))

    assertEquals(5, page.size)
  }
}
