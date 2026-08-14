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
import dev.yidafu.aqua.marketing.gift.domain.repository.GiftActivityRepository
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
 * 买赠/满赠活动 Resolver GraphQL 集成测试(PRD 10-marketing.md §F-MKT-008,AC-MKT-008-01/05)
 * 权限:查询 PERMISSION_MKT_READ,变更 PERMISSION_MKT_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/marketing/"])
@DisplayName("AdminGiftActivityResolver GraphQL Integration Tests")
class AdminGiftActivityResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var giftActivityRepository: GiftActivityRepository

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

    // 幂等种子 2 商品(活动商品 + 赠品,按名称去重)
    if (productRepository.findAll().none { it.name == "满赠活动商品" }) {
      productRepository.save(
        ProductModel(
          name = "满赠活动商品",
          price = 2100,
          specification = "19L",
          stock = 100,
          status = ProductModelStatus.ONLINE,
          coverImageUrl = "http://localhost:9090/api/v1/storage/files/mkt-gift-main.png",
        ),
      )
    }
    if (productRepository.findAll().none { it.name == "满赠赠品商品" }) {
      productRepository.save(
        ProductModel(
          name = "满赠赠品商品",
          price = 1200,
          specification = "4L",
          stock = 100,
          status = ProductModelStatus.ONLINE,
          coverImageUrl = "http://localhost:9090/api/v1/storage/files/mkt-gift-free.png",
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

  private fun giftProductId(): Long = productRepository.findAll().first { it.name == "满赠赠品商品" }.id!!

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("createGiftActivity 新建满赠活动,展示态 UPCOMING(AC-MKT-008-01)")
  fun `createGiftActivity creates full gift with upcoming status`() {
    val name = "满赠活动${System.currentTimeMillis()}"
    val json =
      postGraphql(
        """mutation { createGiftActivity(input: {name: "$name", giftType: FULL_GIFT, thresholdCents: 10000, scope: ALL, giftProductId: ${giftProductId()}, giftQuantity: 1, validFrom: "${futureFrom()}", validTo: "${futureTo()}"}) { id name giftType giftProductName giftQuantity currentStatus } }""",
      )
    assertTrue(
      json.contains("\"$name\"") &&
        json.contains("\"giftType\" : \"FULL_GIFT\"") &&
        json.contains("\"giftProductName\" : \"满赠赠品商品\"") &&
        json.contains("\"giftQuantity\" : 1") &&
        json.contains("\"currentStatus\" : \"UPCOMING\"") &&
        !json.contains("\"errors\""),
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("createGiftActivity 满赠缺少门槛金额被拦截")
  fun `createGiftActivity rejects full gift without threshold`() {
    val name = "非法满赠${System.currentTimeMillis()}"
    val json =
      postGraphql(
        """mutation { createGiftActivity(input: {name: "$name", giftType: FULL_GIFT, scope: ALL, giftProductId: ${giftProductId()}, giftQuantity: 1, validFrom: "${futureFrom()}", validTo: "${futureTo()}"}) { id } }""",
      )
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("disableGiftActivity 停用后展示态 DISABLED(AC-MKT-008-05)")
  fun `disableGiftActivity sets currentStatus disabled`() {
    val activity =
      giftActivityRepository.save(
        dev.yidafu.aqua.common.domain.model.GiftActivityModel(
          name = "停用满赠${System.currentTimeMillis()}",
          giftType = dev.yidafu.aqua.common.domain.model.enums.GiftActivityType.FULL_GIFT,
          thresholdCents = 10000,
          scope = dev.yidafu.aqua.common.domain.model.enums.PromotionScope.ALL,
          giftProductId = giftProductId(),
          giftQuantity = 1,
          validFrom = LocalDateTime.now().plusDays(1),
          validTo = LocalDateTime.now().plusDays(30),
          status = PromotionActivityStatus.ACTIVE,
        ),
      )
    val json = postGraphql("mutation { disableGiftActivity(id: ${activity.id}) { id name status currentStatus } }")
    assertTrue(json.contains("\"currentStatus\" : \"DISABLED\"") && !json.contains("\"errors\""))
    assertTrue(giftActivityRepository.findById(activity.id!!).get().status == PromotionActivityStatus.DISABLED)
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("giftActivities 列表分页查询")
  fun `giftActivities lists activities`() {
    giftActivityRepository.save(
      dev.yidafu.aqua.common.domain.model.GiftActivityModel(
        name = "列表满赠${System.currentTimeMillis()}",
        giftType = dev.yidafu.aqua.common.domain.model.enums.GiftActivityType.BUY_GIFT,
        thresholdQuantity = 5,
        scope = dev.yidafu.aqua.common.domain.model.enums.PromotionScope.ALL,
        giftProductId = giftProductId(),
        giftQuantity = 1,
        validFrom = LocalDateTime.now().plusDays(1),
        validTo = LocalDateTime.now().plusDays(30),
        status = PromotionActivityStatus.ACTIVE,
      ),
    )
    val json = postGraphql("{ giftActivities(input: {page: 0, size: 20}) { totalElements content { id name } } }")
    assertTrue(json.contains("\"totalElements\"") && !json.contains("\"errors\""))
  }
}
