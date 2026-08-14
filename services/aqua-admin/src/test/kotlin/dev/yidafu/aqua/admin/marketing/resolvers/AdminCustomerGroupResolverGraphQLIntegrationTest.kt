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
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.marketing.customer.domain.repository.CustomerGroupRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
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
 * 客户分组 Resolver GraphQL 集成测试(PRD 10-marketing.md §F-MKT-001,AC-MKT-001-01~05)
 * 权限:查询 PERMISSION_MKT_READ,变更 PERMISSION_MKT_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/marketing/"])
@DisplayName("AdminCustomerGroupResolver GraphQL Integration Tests")
class AdminCustomerGroupResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var customerGroupRepository: CustomerGroupRepository

  @Autowired
  private lateinit var userRepository: UserRepository

  private lateinit var mockMvc: MockMvc

  @BeforeEach
  fun setUp() {
    mockMvc =
      MockMvcBuilders
        .webAppContextSetup(context)
        .apply<DefaultMockMvcBuilder>(springSecurity())
        .build()

    // 幂等种子 2 用户 + 2 分组(按 openid/名称去重,避免共享 H2 遗留数据重复)
    if (userRepository.findByWechatOpenId("mkt-group-seed-a") == null) {
      userRepository.save(
        UserModel(
          wechatOpenId = "mkt-group-seed-a",
          nickname = "测试用户A",
          email = "",
          status = UserStatus.ACTIVE,
          role = UserRole.USER,
        ),
      )
    }
    if (userRepository.findByWechatOpenId("mkt-group-seed-b") == null) {
      userRepository.save(
        UserModel(
          wechatOpenId = "mkt-group-seed-b",
          nickname = "测试用户B",
          email = "",
          status = UserStatus.ACTIVE,
          role = UserRole.USER,
        ),
      )
    }
    if (customerGroupRepository.findByName("熟客组") == null) {
      val group =
        customerGroupRepository.save(
          CustomerGroupModel(
            name = "熟客组",
            description = "熟客分组(种子)",
          ),
        )
      // 种子成员:A 归入「熟客组」
      userRepository.findByWechatOpenId("mkt-group-seed-a")!!.also {
        it.customerGroupId = group.id
        userRepository.save(it)
      }
    }
    if (customerGroupRepository.findByName("企业客户组") == null) {
      customerGroupRepository.save(CustomerGroupModel(name = "企业客户组", description = "企业客户(种子)"))
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

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE"])
  @DisplayName("createCustomerGroup 新建分组成功(AC-MKT-001-01)")
  fun `createCustomerGroup creates group`() {
    val name = "新分组${System.currentTimeMillis()}"
    val json =
      postGraphql(
        """mutation { createCustomerGroup(input: {name: "$name", description: "测试新建"}) { id name description } }""",
      )
    assertTrue(json.contains("\"$name\"") && !json.contains("\"errors\""))
    assertTrue(customerGroupRepository.findByName(name) != null)
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE"])
  @DisplayName("createCustomerGroup 同名分组被拦截(AC-MKT-001-02)")
  fun `createCustomerGroup rejects duplicate name`() {
    val json = postGraphql("""mutation { createCustomerGroup(input: {name: "熟客组"}) { id name } }""")
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE"])
  @DisplayName("createCustomerGroup 携带 memberIds 创建并划入成员")
  fun `createCustomerGroup with memberIds assigns members`() {
    val userB = userRepository.findByWechatOpenId("mkt-group-seed-b")!!
    val name = "成员组${System.currentTimeMillis()}"
    val json =
      postGraphql(
        """mutation { createCustomerGroup(input: {name: "$name", memberIds: [${userB.id}]}) { id name memberCount } }""",
      )
    assertTrue(json.contains("\"memberCount\" : 1") && !json.contains("\"errors\""))
    val group = customerGroupRepository.findByName(name)!!
    assertTrue(userRepository.findById(userB.id!!).get().customerGroupId == group.id)

    // 清理:还原未分组,避免影响 memberCount 用例
    userRepository.findById(userB.id!!).get().also {
      it.customerGroupId = null
      userRepository.save(it)
    }
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE"])
  @DisplayName("updateCustomerGroup 覆盖式同步成员:缺省不调整、非空仅保留该集合")
  fun `updateCustomerGroup with memberIds replaces members`() {
    val userA = userRepository.findByWechatOpenId("mkt-group-seed-a")!!
    val userB = userRepository.findByWechatOpenId("mkt-group-seed-b")!!
    val name = "改成员组${System.currentTimeMillis()}"
    // 创建:成员 B
    postGraphql("""mutation { createCustomerGroup(input: {name: "$name", memberIds: [${userB.id}]}) { id } }""")
    val group = customerGroupRepository.findByName(name)!!
    assertTrue(userRepository.findById(userB.id!!).get().customerGroupId == group.id)

    // 更新不带 memberIds:成员不调整
    postGraphql("""mutation { updateCustomerGroup(id: ${group.id}, input: {name: "$name"}) { id } }""")
    assertTrue(userRepository.findById(userB.id!!).get().customerGroupId == group.id)

    // 更新成员为 [A]:B 移出回未分组,A 划入
    val json =
      postGraphql(
        """mutation { updateCustomerGroup(id: ${group.id}, input: {name: "$name", memberIds: [${userA.id}]}) { memberCount } }""",
      )
    assertTrue(json.contains("\"memberCount\" : 1") && !json.contains("\"errors\""))
    assertTrue(userRepository.findById(userB.id!!).get().customerGroupId == null)
    assertTrue(userRepository.findById(userA.id!!).get().customerGroupId == group.id)

    // 清理:还原种子状态(A 回「熟客组」,B 未分组)
    val seedGroup = customerGroupRepository.findByName("熟客组")!!
    userRepository.findById(userA.id!!).get().also {
      it.customerGroupId = seedGroup.id
      userRepository.save(it)
    }
    userRepository.findById(userB.id!!).get().also {
      it.customerGroupId = null
      userRepository.save(it)
    }
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE"])
  @DisplayName("setUserCustomerGroup 将用户加入分组(AC-MKT-001-03)")
  fun `setUserCustomerGroup assigns user to group`() {
    val userB = userRepository.findByWechatOpenId("mkt-group-seed-b")!!
    val groupId = customerGroupRepository.findByName("熟客组")!!.id!!
    val json = postGraphql("mutation { setUserCustomerGroup(userId: ${userB.id}, customerGroupId: $groupId) }")
    assertTrue(json.contains("\"setUserCustomerGroup\" : true") && !json.contains("\"errors\""))
    assertTrue(userRepository.findById(userB.id!!).get().customerGroupId == groupId)

    // 清理:还原未分组,避免影响 memberCount 用例
    postGraphql("mutation { setUserCustomerGroup(userId: ${userB.id}, customerGroupId: null) }")
    assertTrue(userRepository.findById(userB.id!!).get().customerGroupId == null)
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_WRITE"])
  @DisplayName("deleteCustomerGroup 删除分组且成员回退未分组(AC-MKT-001-04)")
  fun `deleteCustomerGroup resets members to ungrouped`() {
    val group =
      customerGroupRepository.findByName("待删组")
        ?: customerGroupRepository.save(CustomerGroupModel(name = "待删组", description = "删除用分组"))
    val userB = userRepository.findByWechatOpenId("mkt-group-seed-b")!!
    postGraphql("mutation { setUserCustomerGroup(userId: ${userB.id}, customerGroupId: ${group.id}) }")
    assertTrue(userRepository.findById(userB.id!!).get().customerGroupId == group.id)

    val json = postGraphql("mutation { deleteCustomerGroup(id: ${group.id}) }")
    assertTrue(json.contains("\"deleteCustomerGroup\" : true") && !json.contains("\"errors\""))
    assertTrue(customerGroupRepository.findById(group.id!!).isEmpty)
    assertTrue(userRepository.findById(userB.id!!).get().customerGroupId == null)
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_READ"])
  @DisplayName("customerGroups 关键字过滤仅返回匹配分组(AC-MKT-001-05)")
  fun `customerGroups query filters by keyword`() {
    val json = postGraphql("{ customerGroups(input: {keyword: \"熟客\"}) { content { id name memberCount } totalElements } }")
    assertTrue(json.contains("\"熟客组\"") && !json.contains("\"企业客户组\"") && !json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_MKT_READ"])
  @DisplayName("customerGroups 返回 memberCount 成员数")
  fun `customerGroups query returns memberCount`() {
    val json = postGraphql("{ customerGroups(input: {keyword: \"熟客组\"}) { content { name memberCount } } }")
    assertTrue(json.contains("\"memberCount\" : 1") && !json.contains("\"errors\""))
  }

  @Test
  @DisplayName("匿名访问被拒绝(401)")
  fun `anonymous access is denied`() {
    mockMvc
      .perform(
        post("/graphql")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"query": "{ customerGroups { content { id } } }"}"""),
      ).andExpect(status().isUnauthorized)
  }
}
