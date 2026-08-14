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
import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.common.domain.model.GroupProductPriceModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.marketing.customer.domain.repository.CustomerGroupRepository
import dev.yidafu.aqua.marketing.pricing.domain.repository.GroupProductPriceRepository
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
 * 分组商品专属价 Resolver GraphQL 集成测试(PRD 10-marketing.md §F-MKT-002,AC-MKT-002-01~03/05)
 * 权限:查询 PERMISSION_MKT_READ,变更 PERMISSION_MKT_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/marketing/"])
@DisplayName("AdminGroupProductPriceResolver GraphQL Integration Tests")
class AdminGroupProductPriceResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var customerGroupRepository: CustomerGroupRepository

  @Autowired
  private lateinit var groupProductPriceRepository: GroupProductPriceRepository

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

    // 幂等种子 1 分组 + 2 商品(按名称去重,避免共享 H2 遗留数据重复)
    if (customerGroupRepository.findByName("价组") == null) {
      customerGroupRepository.save(CustomerGroupModel(name = "价组", description = "专属价分组(种子)"))
    }
    if (productRepository.findAll().none { it.name == "山泉19L" }) {
      productRepository.save(
        ProductModel(
          name = "山泉19L",
          price = 2100,
          specification = "19L",
          stock = 100,
          status = ProductModelStatus.ONLINE,
          coverImageUrl = "http://localhost:9090/api/v1/storage/files/mkt-p1.png",
        ),
      )
    }
    if (productRepository.findAll().none { it.name == "桶装19L" }) {
      productRepository.save(
        ProductModel(
          name = "桶装19L",
          price = 1500,
          specification = "19L",
          stock = 100,
          status = ProductModelStatus.ONLINE,
          coverImageUrl = "http://localhost:9090/api/v1/storage/files/mkt-p2.png",
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

  private fun groupId(): Long = customerGroupRepository.findByName("价组")!!.id!!

  private fun product1(): ProductModel = productRepository.findAll().first { it.name == "山泉19L" }

  private fun product2(): ProductModel = productRepository.findAll().first { it.name == "桶装19L" }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("saveGroupProductPrices 保存分组价并回显目录价(AC-MKT-002-01)")
  fun `saveGroupProductPrices saves group price`() {
    val gid = groupId()
    val p1 = product1()
    val json =
      postGraphql(
        """mutation { saveGroupProductPrices(input: {customerGroupId: $gid, items: [{productId: ${p1.id}, priceCents: 1500}]}) }""",
      )
    assertTrue(json.contains("\"saveGroupProductPrices\" : true") && !json.contains("\"errors\""))
    assertTrue(groupProductPriceRepository.findByCustomerGroupIdAndProductId(gid, p1.id!!)?.priceCents == 1500L)

    // 查询接口:专属价 1500 + 划线目录价 2100
    val listJson =
      postGraphql(
        "{ groupProductPrices(input: {customerGroupId: $gid}) { content { productId productName priceCents catalogPriceCents } } }",
      )
    assertTrue(
      listJson.contains("\"priceCents\" : \"1500\"") &&
        listJson.contains("\"catalogPriceCents\" : \"2100\"") &&
        listJson.contains("\"山泉19L\"") &&
        !listJson.contains("\"errors\""),
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("saveGroupProductPrices clearUnlisted 清空批外已配置价格(AC-MKT-002-02)")
  fun `saveGroupProductPrices clears unlisted rows`() {
    val gid = groupId()
    val p1 = product1()
    val p2 = product2()
    postGraphql(
      """mutation { saveGroupProductPrices(input: {customerGroupId: $gid, items: [{productId: ${p1.id}, priceCents: 1500}, {productId: ${p2.id}, priceCents: 1200}]}) }""",
    )
    assertTrue(groupProductPriceRepository.findByCustomerGroupIdAndProductId(gid, p2.id!!)?.priceCents == 1200L)

    // 仅提交 p1 且 clearUnlisted=true → p2 行被清空
    postGraphql(
      """mutation { saveGroupProductPrices(input: {customerGroupId: $gid, items: [{productId: ${p1.id}, priceCents: 1800}], clearUnlisted: true}) }""",
    )
    assertTrue(groupProductPriceRepository.findByCustomerGroupIdAndProductId(gid, p2.id!!) == null)
    assertTrue(groupProductPriceRepository.findByCustomerGroupIdAndProductId(gid, p1.id!!)?.priceCents == 1800L)
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("saveGroupProductPrices 无效价格 0 被拦截(AC-MKT-002-03)")
  fun `saveGroupProductPrices rejects invalid price`() {
    val gid = groupId()
    val p1 = product1()
    val json =
      postGraphql(
        """mutation { saveGroupProductPrices(input: {customerGroupId: $gid, items: [{productId: ${p1.id}, priceCents: 0}]}) }""",
      )
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE", "PERMISSION_MKT_READ"])
  @DisplayName("saveGroupProductPrices priceCents 为 null 时清空单项回退目录价")
  fun `saveGroupProductPrices clears single item on null price`() {
    val gid = groupId()
    val p1 = product1()
    val p2 = product2()
    postGraphql(
      """mutation { saveGroupProductPrices(input: {customerGroupId: $gid, items: [{productId: ${p1.id}, priceCents: 1500}, {productId: ${p2.id}, priceCents: 1200}]}) }""",
    )
    postGraphql(
      """mutation { saveGroupProductPrices(input: {customerGroupId: $gid, items: [{productId: ${p1.id}, priceCents: null}]}) }""",
    )
    assertTrue(groupProductPriceRepository.findByCustomerGroupIdAndProductId(gid, p1.id!!) == null)
    assertTrue(groupProductPriceRepository.findByCustomerGroupIdAndProductId(gid, p2.id!!)?.priceCents == 1200L)
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_READ", "PERMISSION_MKT_WRITE"])
  @DisplayName("groupProductPrices 按分组过滤查询(AC-MKT-002-05)")
  fun `groupProductPrices query filters by group`() {
    val gid = groupId()
    val p1 = product1()
    val p2 = product2()
    // 先确定价组仅含 p1
    val resetJson =
      postGraphql(
        """mutation { saveGroupProductPrices(input: {customerGroupId: $gid, items: [{productId: ${p1.id}, priceCents: 1500}], clearUnlisted: true}) }""",
      )
    // 另一分组「价组B」直接落 p2 专属价
    val groupB =
      customerGroupRepository.findByName("价组B")
        ?: customerGroupRepository.save(CustomerGroupModel(name = "价组B", description = "专属价分组B(种子)"))
    if (groupProductPriceRepository.findByCustomerGroupIdAndProductId(groupB.id!!, p2.id!!) == null) {
      groupProductPriceRepository.save(
        GroupProductPriceModel(customerGroupId = groupB.id!!, productId = p2.id!!, priceCents = 1300),
      )
    }

    val json =
      postGraphql(
        "{ groupProductPrices(input: {customerGroupId: $gid}) { content { productId productName priceCents } } }",
      )
    assertTrue(json.contains("\"山泉19L\"") && !json.contains("\"桶装19L\"") && !json.contains("\"errors\""))
  }
}
