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
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityRepository
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityTierRepository
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
 * 满减/折扣活动 Resolver GraphQL 集成测试(PRD 10-marketing.md §F-MKT-004,AC-MKT-004-01/05)
 * 权限:查询 PERMISSION_MKT_READ,变更 PERMISSION_MKT_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/marketing/"])
@DisplayName("AdminPromotionActivityResolver GraphQL Integration Tests")
class AdminPromotionActivityResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var promotionActivityRepository: PromotionActivityRepository

  @Autowired
  private lateinit var promotionActivityTierRepository: PromotionActivityTierRepository

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
    if (productRepository.findAll().none { it.name == "满减商品" }) {
      productRepository.save(
        ProductModel(
          name = "满减商品",
          price = 10000,
          specification = "19L",
          stock = 100,
          status = ProductModelStatus.ONLINE,
          coverImageUrl = "http://localhost:9090/api/v1/storage/files/mkt-promo.png",
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
   * 经 GraphQL 创建满减活动(全场,两档 10000→1000 / 20000→2500),返回活动 id。
   */
  private fun createFullReduceActivity(name: String): Long {
    val json =
      postGraphql(
        """mutation { createPromotionActivity(input: {name: "$name", activityType: FULL_REDUCE, scope: ALL, validFrom: "${futureFrom()}", validTo: "${futureTo()}", tiers: [{thresholdCents: 10000, discountAmountCents: 1000}, {thresholdCents: 20000, discountAmountCents: 2500}]}) { id name } }""",
      )
    assertTrue(!json.contains("\"errors\""), "createPromotionActivity should succeed: $json")
    return promotionActivityRepository.findAll().first { it.name == name }.id!!
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("createPromotionActivity 新建两档满减活动,展示态 UPCOMING(AC-MKT-004-01)")
  fun `createPromotionActivity creates full reduce with tiers`() {
    val name = "满减活动${System.currentTimeMillis()}"
    val json =
      postGraphql(
        """mutation { createPromotionActivity(input: {name: "$name", activityType: FULL_REDUCE, scope: ALL, validFrom: "${futureFrom()}", validTo: "${futureTo()}", tiers: [{thresholdCents: 10000, discountAmountCents: 1000}, {thresholdCents: 20000, discountAmountCents: 2500}]}) { id name currentStatus tiers { thresholdCents discountAmountCents } } }""",
      )
    assertTrue(
      json.contains("\"$name\"") &&
        json.contains("\"currentStatus\" : \"UPCOMING\"") &&
        json.contains("\"thresholdCents\" : \"10000\"") &&
        json.contains("\"thresholdCents\" : \"20000\"") &&
        json.contains("\"discountAmountCents\" : \"1000\"") &&
        json.contains("\"discountAmountCents\" : \"2500\"") &&
        !json.contains("\"errors\""),
    )

    // 单查 promotionActivity:档位 + 展示态一致
    val activityId = promotionActivityRepository.findAll().first { it.name == name }.id!!
    val detail =
      postGraphql(
        "{ promotionActivity(id: $activityId) { id name currentStatus tiers { thresholdCents discountAmountCents } } }",
      )
    assertTrue(
      detail.contains("\"currentStatus\" : \"UPCOMING\"") &&
        detail.contains("\"discountAmountCents\" : \"2500\"") &&
        !detail.contains("\"errors\""),
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("createPromotionActivity 档位金额非递增被拦截")
  fun `createPromotionActivity rejects non-increasing tiers`() {
    val name = "非法档位${System.currentTimeMillis()}"
    val json =
      postGraphql(
        """mutation { createPromotionActivity(input: {name: "$name", activityType: FULL_REDUCE, scope: ALL, validFrom: "${futureFrom()}", validTo: "${futureTo()}", tiers: [{thresholdCents: 10000, discountAmountCents: 1000}, {thresholdCents: 5000, discountAmountCents: 500}]}) { id } }""",
      )
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("createPromotionActivity 满折活动缺少折扣率被拦截")
  fun `createPromotionActivity rejects full discount without rate`() {
    val name = "缺折扣率${System.currentTimeMillis()}"
    val json =
      postGraphql(
        """mutation { createPromotionActivity(input: {name: "$name", activityType: FULL_DISCOUNT, scope: ALL, validFrom: "${futureFrom()}", validTo: "${futureTo()}", tiers: [{thresholdCents: 10000, discountAmountCents: 1000}]}) { id } }""",
      )
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("updatePromotionActivity 全量替换档位")
  fun `updatePromotionActivity replaces tiers`() {
    val name = "更新活动${System.currentTimeMillis()}"
    val activityId = createFullReduceActivity(name)

    val json =
      postGraphql(
        """mutation { updatePromotionActivity(id: $activityId, input: {name: "$name", activityType: FULL_REDUCE, scope: ALL, validFrom: "${futureFrom()}", validTo: "${futureTo()}", tiers: [{thresholdCents: 30000, discountAmountCents: 3000}]}) { id name tiers { thresholdCents discountAmountCents } } }""",
      )
    assertTrue(
      json.contains("\"thresholdCents\" : \"30000\"") &&
        !json.contains("\"thresholdCents\" : \"10000\"") &&
        !json.contains("\"errors\""),
    )
    val tiers = promotionActivityTierRepository.findByActivityId(activityId)
    assertTrue(tiers.size == 1 && tiers[0].thresholdCents == 30000L && tiers[0].discountAmountCents == 3000L)
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("disablePromotionActivity 停用后展示态 DISABLED(AC-MKT-004-05)")
  fun `disablePromotionActivity sets currentStatus disabled`() {
    val name = "停用活动${System.currentTimeMillis()}"
    val activityId = createFullReduceActivity(name)

    val json = postGraphql("mutation { disablePromotionActivity(id: $activityId) { id name status currentStatus } }")
    assertTrue(json.contains("\"currentStatus\" : \"DISABLED\"") && !json.contains("\"errors\""))
    assertTrue(promotionActivityRepository.findById(activityId).get().status == PromotionActivityStatus.DISABLED)
  }
}
