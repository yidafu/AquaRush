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
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
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
 * 桶水票票种 Resolver GraphQL 集成测试(PRD 10-marketing.md §F-MKT-007,AC-MKT-007-01)
 * 权限:查询 PERMISSION_MKT_READ,变更 PERMISSION_MKT_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/marketing/"])
@DisplayName("AdminTicketTypeResolver GraphQL Integration Tests")
class AdminTicketTypeResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var ticketTypeRepository: TicketTypeRepository

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
    if (productRepository.findAll().none { it.name == "桶装水19L" }) {
      productRepository.save(
        ProductModel(
          name = "桶装水19L",
          price = 2100,
          specification = "19L",
          stock = 100,
          status = ProductModelStatus.ONLINE,
          coverImageUrl = "http://localhost:9090/api/v1/storage/files/mkt-ticket.png",
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

  /**
   * 经 GraphQL 创建通兑票种(10 张 ¥100,19L 规格,固定区间未来),返回票种 id。
   */
  private fun createUniversalTicketType(name: String): Long {
    val json =
      postGraphql(
        """mutation { createTicketType(input: {name: "$name", ticketType: UNIVERSAL, specification: "19L", packageCount: 10, packagePriceCents: 10000, giftCount: 0, validityType: FIXED_RANGE, validFrom: "${futureFrom()}", validTo: "${futureTo()}", perUserLimit: 0}) { id name } }""",
      )
    assertTrue(!json.contains("\"errors\""), "createTicketType should succeed: $json")
    return ticketTypeRepository.findAll().first { it.name == name }.id!!
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("createTicketType 新建通兑票种 10 张 ¥100(AC-MKT-007-01)")
  fun `createTicketType creates universal ticket type`() {
    val name = "通兑票${System.currentTimeMillis()}"
    val json =
      postGraphql(
        """mutation { createTicketType(input: {name: "$name", ticketType: UNIVERSAL, specification: "19L", packageCount: 10, packagePriceCents: 10000, giftCount: 0, validityType: FIXED_RANGE, validFrom: "${futureFrom()}", validTo: "${futureTo()}", perUserLimit: 0}) { id name ticketType specification packageCount packagePriceCents giftCount status } }""",
      )
    assertTrue(
      json.contains("\"$name\"") &&
        json.contains("\"ticketType\" : \"UNIVERSAL\"") &&
        json.contains("\"specification\" : \"19L\"") &&
        json.contains("\"packageCount\" : 10") &&
        json.contains("\"packagePriceCents\" : \"10000\"") &&
        json.contains("\"status\" : \"ACTIVE\"") &&
        !json.contains("\"errors\""),
    )
    val ticketType = ticketTypeRepository.findAll().first { it.name == name }
    assertTrue(ticketType.packageCount == 10 && ticketType.packagePriceCents == 10000L)
    assertTrue(ticketType.specification == "19L" && ticketType.status == TicketTypeStatus.ACTIVE)
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("createTicketType 指定商品票缺少商品被拦截")
  fun `createTicketType rejects specific product without productId`() {
    val name = "指定票缺商品${System.currentTimeMillis()}"
    val json =
      postGraphql(
        """mutation { createTicketType(input: {name: "$name", ticketType: SPECIFIC_PRODUCT, packageCount: 10, packagePriceCents: 10000, giftCount: 0, validityType: FIXED_RANGE, validFrom: "${futureFrom()}", validTo: "${futureTo()}", perUserLimit: 0}) { id } }""",
      )
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("createTicketType 通兑票缺少规格被拦截")
  fun `createTicketType rejects universal without specification`() {
    val name = "通兑缺规格${System.currentTimeMillis()}"
    val json =
      postGraphql(
        """mutation { createTicketType(input: {name: "$name", ticketType: UNIVERSAL, packageCount: 10, packagePriceCents: 10000, giftCount: 0, validityType: FIXED_RANGE, validFrom: "${futureFrom()}", validTo: "${futureTo()}", perUserLimit: 0}) { id } }""",
      )
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_READ", "PERMISSION_MKT_WRITE"])
  @DisplayName("ticketTypes 列表与 ticketType 单查")
  fun `ticketTypes query returns list and single`() {
    val name = "查询票种${System.currentTimeMillis()}"
    val ticketTypeId = createUniversalTicketType(name)

    val listJson = postGraphql("{ ticketTypes(input: {keyword: \"$name\"}) { content { id name ticketType packageCount } } }")
    assertTrue(listJson.contains("\"$name\"") && listJson.contains("\"packageCount\" : 10") && !listJson.contains("\"errors\""))

    val singleJson = postGraphql("{ ticketType(id: $ticketTypeId) { id name packageCount packagePriceCents } }")
    assertTrue(
      singleJson.contains("\"$name\"") &&
        singleJson.contains("\"packageCount\" : 10") &&
        singleJson.contains("\"packagePriceCents\" : \"10000\"") &&
        !singleJson.contains("\"errors\""),
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("disableTicketType 停用票种状态置 DISABLED")
  fun `disableTicketType sets status disabled`() {
    val name = "停用票种${System.currentTimeMillis()}"
    val ticketTypeId = createUniversalTicketType(name)

    val json = postGraphql("mutation { disableTicketType(id: $ticketTypeId) { id name status } }")
    assertTrue(json.contains("\"status\" : \"DISABLED\"") && !json.contains("\"errors\""))
    assertTrue(ticketTypeRepository.findById(ticketTypeId).get().status == TicketTypeStatus.DISABLED)
  }
}
