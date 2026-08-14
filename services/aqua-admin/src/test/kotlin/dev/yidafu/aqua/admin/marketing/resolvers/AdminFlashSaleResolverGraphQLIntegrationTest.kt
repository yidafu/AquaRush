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

package dev.yidafu.aqua.admin.marketing.resolvers

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.admin.ops.resolvers.WithMockUserPrincipal
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.marketing.flashsale.domain.repository.FlashSaleActivityRepository
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 限时特价活动 Resolver GraphQL 集成测试(PRD 10-marketing.md §F-MKT-005,AC-MKT-005-01/06)
 * 权限:查询 PERMISSION_MKT_READ,变更 PERMISSION_MKT_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/marketing/"])
@DisplayName("AdminFlashSaleResolver GraphQL Integration Tests")
class AdminFlashSaleResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var flashSaleActivityRepository: FlashSaleActivityRepository

  @Autowired
  private lateinit var productRepository: ProductRepository

  private lateinit var mockMvc: MockMvc

  @BeforeEach
  fun setUp() {
    mockMvc =
      MockMvcBuilders
        .webAppContextSetup(context)
        .apply<DefaultMockMvcBuilder>(springSecurity())
        .build()

    // 幂等种子 1 商品(按名称去重,避免共享 H2 遗留数据重复)
    if (productRepository.findAll().none { it.name == "限时特价商品" }) {
      productRepository.save(
        ProductModel(
          name = "限时特价商品",
          price = 10000,
          specification = "19L",
          stock = 100,
          status = ProductModelStatus.ONLINE,
          coverImageUrl = "http://localhost:9090/api/v1/storage/files/mkt-flash.png",
        ),
      )
    }
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

  private fun futureFrom(): String = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

  private fun futureTo(): String = LocalDateTime.now().plusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

  private fun productId(): Long = productRepository.findAll().first { it.name == "限时特价商品" }.id!!

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("createFlashSaleActivity 新建特价(低于目录价),展示态 UPCOMING(AC-MKT-005-01)")
  fun `createFlashSaleActivity creates flash sale with upcoming status`() {
    val name = "特价活动${System.currentTimeMillis()}"
    val json =
      postGraphql(
        """mutation { createFlashSaleActivity(input: {name: "$name", productId: ${productId()}, salePriceCents: 8000, validFrom: "${futureFrom()}", validTo: "${futureTo()}"}) { id name productName catalogPriceCents salePriceCents currentStatus } }""",
      )
    assertTrue(
      json.contains("\"$name\"") &&
        json.contains("\"productName\" : \"限时特价商品\"") &&
        json.contains("\"catalogPriceCents\" : \"10000\"") &&
        json.contains("\"salePriceCents\" : \"8000\"") &&
        json.contains("\"currentStatus\" : \"UPCOMING\"") &&
        !json.contains("\"errors\""),
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("createFlashSaleActivity 特价不低于目录价被拦截")
  fun `createFlashSaleActivity rejects sale price not below catalog`() {
    val name = "特价非法${System.currentTimeMillis()}"
    val json =
      postGraphql(
        """mutation { createFlashSaleActivity(input: {name: "$name", productId: ${productId()}, salePriceCents: 10000, validFrom: "${futureFrom()}", validTo: "${futureTo()}"}) { id } }""",
      )
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("createFlashSaleActivity 同商品时间窗重叠被拦截")
  fun `createFlashSaleActivity rejects overlapping time window`() {
    val name = "重叠特价${System.currentTimeMillis()}"
    // 已存在的活动(上一条用例创建),再建重叠时间窗活动
    flashSaleActivityRepository.save(
      dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel(
        name = "已有特价${System.currentTimeMillis()}",
        productId = productId(),
        salePriceCents = 8000,
        validFrom = LocalDateTime.now().plusDays(1),
        validTo = LocalDateTime.now().plusDays(30),
        status = PromotionActivityStatus.ACTIVE,
      ),
    )
    val json =
      postGraphql(
        """mutation { createFlashSaleActivity(input: {name: "$name", productId: ${productId()}, salePriceCents: 7000, validFrom: "${futureFrom()}", validTo: "${futureTo()}"}) { id } }""",
      )
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("disableFlashSaleActivity 停用后展示态 DISABLED(AC-MKT-005-06)")
  fun `disableFlashSaleActivity sets currentStatus disabled`() {
    val activity =
      flashSaleActivityRepository.save(
        dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel(
          name = "停用特价${System.currentTimeMillis()}",
          productId = productId(),
          salePriceCents = 8000,
          validFrom = LocalDateTime.now().plusDays(1),
          validTo = LocalDateTime.now().plusDays(30),
          status = PromotionActivityStatus.ACTIVE,
        ),
      )
    val json = postGraphql("mutation { disableFlashSaleActivity(id: ${activity.id}) { id name status currentStatus } }")
    assertTrue(json.contains("\"currentStatus\" : \"DISABLED\"") && !json.contains("\"errors\""))
    assertTrue(flashSaleActivityRepository.findById(activity.id!!).get().status == PromotionActivityStatus.DISABLED)
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("flashSaleActivities 列表分页查询")
  fun `flashSaleActivities lists activities`() {
    flashSaleActivityRepository.save(
      dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel(
        name = "列表特价${System.currentTimeMillis()}",
        productId = productId(),
        salePriceCents = 8000,
        validFrom = LocalDateTime.now().plusDays(1),
        validTo = LocalDateTime.now().plusDays(30),
        status = PromotionActivityStatus.ACTIVE,
      ),
    )
    val json = postGraphql("{ flashSaleActivities(input: {page: 0, size: 20}) { totalElements content { id name } } }")
    assertTrue(json.contains("\"totalElements\"") && !json.contains("\"errors\""))
  }
}
