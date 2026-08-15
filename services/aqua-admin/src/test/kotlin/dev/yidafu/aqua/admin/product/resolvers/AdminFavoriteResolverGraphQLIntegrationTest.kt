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

package dev.yidafu.aqua.admin.product.resolvers

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.admin.ops.resolvers.WithMockUserPrincipal
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.product.domain.repository.ProductFavoriteRepository
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/**
 * 用户收藏商品 Resolver GraphQL 集成测试(用户详情页「收藏商品」Tab,PRD 07-user.md §F-USR-022)
 * 权限:PERMISSION_USER_READ。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/product/"])
@DisplayName("AdminFavoriteResolver GraphQL Integration Tests")
class AdminFavoriteResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var productRepository: ProductRepository

  @Autowired
  private lateinit var productFavoriteRepository: ProductFavoriteRepository

  private lateinit var mockMvc: MockMvc

  @BeforeEach
  fun setUp() {
    mockMvc =
      MockMvcBuilders
        .webAppContextSetup(context)
        .apply<DefaultMockMvcBuilder>(springSecurity())
        .build()
  }

  private fun postGraphql(query: String): String {
    val escaped = query.replace("\"", "\\\"")
    val result =
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "$escaped"}"""),
        ).andExpect(status().isOk)
        .andReturn()
    return result.response.contentAsString
  }

  /** 种子商品(名称唯一),返回商品 id */
  private fun seedProduct(name: String): Long =
    productRepository
      .save(
        ProductModel(
          name = name,
          price = 2000,
          coverImageUrl = "https://example.com/$name.jpg",
          specification = "19L",
          stock = 100,
          salesVolume = 10,
          status = ProductModelStatus.ONLINE,
        ),
      ).id!!

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_USER_READ"])
  @DisplayName("favoriteProducts 返回该用户收藏商品(含 addedAt/价格/销量)")
  fun `favoriteProducts returns user favorites`() {
    val productId = seedProduct("收藏商品${System.currentTimeMillis()}")
    productFavoriteRepository.save(
      dev.yidafu.aqua.common.domain.model.ProductFavoriteModel(
        userId = 1L,
        productId = productId,
        enable = true,
      ),
    )
    val json =
      postGraphql(
        "{ favoriteProducts(userId: 1, page: 0, size: 20) { list { id name priceCents salesVolume status addedAt } pageInfo { total pageSize pageNum } } }",
      )
    assertTrue(
      json.contains("\"id\" : \"$productId\"") &&
        json.contains("\"priceCents\" : \"2000\"") &&
        json.contains("\"salesVolume\" : 10") &&
        json.contains("\"status\" : \"ONLINE\"") &&
        json.contains("\"addedAt\"") &&
        json.contains("\"total\" : 1") &&
        !json.contains("\"errors\""),
      "favoriteProducts should return seeded favorite: $json",
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_USER_READ"])
  @DisplayName("favoriteProducts 分页生效(size=1 返回 1 条,total 为 2)")
  fun `favoriteProducts paginates`() {
    val p1 = seedProduct("收藏分页商品A${System.currentTimeMillis()}")
    val p2 = seedProduct("收藏分页商品B${System.currentTimeMillis()}")
    val favorite =
      dev.yidafu.aqua.common.domain.model
        .ProductFavoriteModel(userId = 2L, productId = p1, enable = true)
    productFavoriteRepository.save(favorite)
    productFavoriteRepository.save(
      dev.yidafu.aqua.common.domain.model
        .ProductFavoriteModel(userId = 2L, productId = p2, enable = true),
    )
    val json = postGraphql("{ favoriteProducts(userId: 2, page: 0, size: 1) { list { id } pageInfo { total pageSize hasNext } } }")
    assertTrue(
      json.contains("\"total\" : 2") &&
        json.contains("\"pageSize\" : 1") &&
        json.contains("\"hasNext\" : true") &&
        !json.contains("\"errors\""),
      "favoriteProducts should paginate: $json",
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_USER_READ"])
  @DisplayName("favoriteProducts 无收藏返回空列表")
  fun `favoriteProducts returns empty for user without favorites`() {
    val json = postGraphql("{ favoriteProducts(userId: 999999, page: 0, size: 20) { list { id } pageInfo { total } } }")
    assertTrue(
      json.contains("\"list\" : [ ]") && json.contains("\"total\" : 0") && !json.contains("\"errors\""),
      "favoriteProducts should be empty: $json",
    )
  }
}
