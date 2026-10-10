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
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.marketing.flashsale.domain.repository.FlashSaleActivityRepository
import dev.yidafu.aqua.product.domain.repository.ProductRepository
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
 * 限时特价活动 Resolver GraphQL 集成测试(PRD 10-marketing.md §F-MKT-005,AC-MKT-005-01/06)
 * 权限:查询 PERMISSION_MKT_READ,变更 PERMISSION_MKT_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/marketing/"])
@ApplyExtension(SpringExtension::class)
class AdminFlashSaleResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  flashSaleActivityRepository: FlashSaleActivityRepository,
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

    fun productId(): Long = productRepository.findByName("限时特价商品")!!.id!!

    beforeTest {
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()

      // 幂等种子 1 商品(按名称去重,避免共享测试库遗留数据重复)
      if (productRepository.findByName("限时特价商品") == null) {
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
    test("createFlashSaleActivity creates flash sale with upcoming status") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"))
      val name = "特价活动${System.currentTimeMillis()}"
      val json =
        postGraphql(
          """mutation { createFlashSaleActivity(input: {name: "$name", productId: ${productId()}, salePriceCents: 8000, validFrom: "${futureFrom()}", validTo: "${futureTo()}"}) { id name productName catalogPriceCents salePriceCents currentStatus } }""",
        )
      json.contains("\"$name\"") &&
        json.contains("\"productName\" : \"限时特价商品\"") &&
        json.contains("\"catalogPriceCents\" : \"10000\"") &&
        json.contains("\"salePriceCents\" : \"8000\"") &&
        json.contains("\"currentStatus\" : \"UPCOMING\"") &&
        !json.contains("\"errors\"") shouldBe true
    }
    test("createFlashSaleActivity rejects sale price not below catalog") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"))
      val name = "特价非法${System.currentTimeMillis()}"
      val json =
        postGraphql(
          """mutation { createFlashSaleActivity(input: {name: "$name", productId: ${productId()}, salePriceCents: 10000, validFrom: "${futureFrom()}", validTo: "${futureTo()}"}) { id } }""",
        )
      json.contains("\"errors\"") shouldBe true
    }
    test("createFlashSaleActivity rejects overlapping time window") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"))
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
      json.contains("\"errors\"") shouldBe true
    }
    test("disableFlashSaleActivity sets currentStatus disabled") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"))
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
      json.contains("\"currentStatus\" : \"DISABLED\"") && !json.contains("\"errors\"") shouldBe true
      (flashSaleActivityRepository.findById(activity.id!!).get().status == PromotionActivityStatus.DISABLED) shouldBe true
    }
    test("flashSaleActivities lists activities") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"))
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
      json.contains("\"totalElements\"") && !json.contains("\"errors\"") shouldBe true
    }
  })
