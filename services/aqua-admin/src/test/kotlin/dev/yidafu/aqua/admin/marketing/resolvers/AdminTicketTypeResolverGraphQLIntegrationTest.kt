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
import dev.yidafu.aqua.admin.testutil.setMockUserPrincipal
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import io.kotest.assertions.withClue
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.mockk
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
@ApplyExtension(SpringExtension::class)
class AdminTicketTypeResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  ticketTypeRepository: TicketTypeRepository,
  productRepository: ProductRepository,
) : FunSpec({
    var mockMvc: MockMvc = mockk()

    fun postGraphql(query: String): String {
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

    fun futureFrom(): String = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    fun futureTo(): String = LocalDateTime.now().plusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    fun createUniversalTicketType(name: String): Long {
      val json =
        postGraphql(
          """mutation { createTicketType(input: {name: "$name", ticketType: UNIVERSAL, specification: "19L", packageCount: 10, packagePriceCents: 10000, giftCount: 0, validityType: FIXED_RANGE, validFrom: "${futureFrom()}", validTo: "${futureTo()}", perUserLimit: 0}) { id name } }""",
        )
      withClue("createTicketType should succeed: $json") { !json.contains("\"errors\"") shouldBe true }
      return ticketTypeRepository.findByName(name)!!.id!!
    }

    /**
     * 经 GraphQL 创建通兑票种(10 张 ¥100,19L 规格,固定区间未来),返回票种 id。
     */

    beforeTest {
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()

      // 幂等种子 1 商品(按名称去重,避免共享测试库遗留数据重复)
      if (productRepository.findByName("桶装水19L") == null) {
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
    test("createTicketType creates universal ticket type") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"))
      val name = "通兑票${System.currentTimeMillis()}"
      val json =
        postGraphql(
          """mutation { createTicketType(input: {name: "$name", ticketType: UNIVERSAL, specification: "19L", packageCount: 10, packagePriceCents: 10000, giftCount: 0, validityType: FIXED_RANGE, validFrom: "${futureFrom()}", validTo: "${futureTo()}", perUserLimit: 0}) { id name ticketType specification packageCount packagePriceCents giftCount status } }""",
        )
      json.contains("\"$name\"") &&
        json.contains("\"ticketType\" : \"UNIVERSAL\"") &&
        json.contains("\"specification\" : \"19L\"") &&
        json.contains("\"packageCount\" : 10") &&
        json.contains("\"packagePriceCents\" : \"10000\"") &&
        json.contains("\"status\" : \"ACTIVE\"") &&
        !json.contains("\"errors\"") shouldBe true
      val ticketType = ticketTypeRepository.findByName(name)!!
      (ticketType.packageCount == 10 && ticketType.packagePriceCents == 10000L) shouldBe true
      (ticketType.specification == "19L" && ticketType.status == TicketTypeStatus.ACTIVE) shouldBe true
    }
    test("createTicketType rejects specific product without productId") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"))
      val name = "指定票缺商品${System.currentTimeMillis()}"
      val json =
        postGraphql(
          """mutation { createTicketType(input: {name: "$name", ticketType: SPECIFIC_PRODUCT, packageCount: 10, packagePriceCents: 10000, giftCount: 0, validityType: FIXED_RANGE, validFrom: "${futureFrom()}", validTo: "${futureTo()}", perUserLimit: 0}) { id } }""",
        )
      json.contains("\"errors\"") shouldBe true
    }
    test("createTicketType rejects universal without specification") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"))
      val name = "通兑缺规格${System.currentTimeMillis()}"
      val json =
        postGraphql(
          """mutation { createTicketType(input: {name: "$name", ticketType: UNIVERSAL, packageCount: 10, packagePriceCents: 10000, giftCount: 0, validityType: FIXED_RANGE, validFrom: "${futureFrom()}", validTo: "${futureTo()}", perUserLimit: 0}) { id } }""",
        )
      json.contains("\"errors\"") shouldBe true
    }
    test("ticketTypes query returns list and single") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_MKT_READ", "PERMISSION_MKT_WRITE"))
      val name = "查询票种${System.currentTimeMillis()}"
      val ticketTypeId = createUniversalTicketType(name)

      val listJson = postGraphql("{ ticketTypes(input: {keyword: \"$name\"}) { content { id name ticketType packageCount } } }")
      listJson.contains("\"$name\"") && listJson.contains("\"packageCount\" : 10") && !listJson.contains("\"errors\"") shouldBe true

      val singleJson = postGraphql("{ ticketType(id: $ticketTypeId) { id name packageCount packagePriceCents } }")
      singleJson.contains("\"$name\"") &&
        singleJson.contains("\"packageCount\" : 10") &&
        singleJson.contains("\"packagePriceCents\" : \"10000\"") &&
        !singleJson.contains("\"errors\"") shouldBe true
    }
    test("disableTicketType sets status disabled") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"))
      val name = "停用票种${System.currentTimeMillis()}"
      val ticketTypeId = createUniversalTicketType(name)

      val json = postGraphql("mutation { disableTicketType(id: $ticketTypeId) { id name status } }")
      json.contains("\"status\" : \"DISABLED\"") && !json.contains("\"errors\"") shouldBe true
      (ticketTypeRepository.findById(ticketTypeId).get().status == TicketTypeStatus.DISABLED) shouldBe true
    }
  })
