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

package dev.yidafu.aqua.admin.user.resolvers

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDateTime

/**
 * 用户与账号域管理 mutation 集成测试(PRD/07-user.md §F-USR-020/021/022,G11 落地):
 * - deleteAdmin/resetAdminPassword:删除保护(最后一个超管)、密码重置落库;
 * - deleteDeliveryWorker/resetDeliveryWorkerPassword;
 * - createUser/updateUser/deleteUser:微信用户后台增改删;
 * - 越权拒绝:@PreAuthorize 方法级强制。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/user-management/"])
@DisplayName("Admin User & Account Management GraphQL Integration Tests")
class AdminUserManagementGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var adminRepository: AdminRepository

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var passwordEncoder: PasswordEncoder

  private lateinit var mockMvc: MockMvc

  @BeforeEach
  fun setUp() {
    mockMvc =
      MockMvcBuilders
        .webAppContextSetup(context)
        .apply<DefaultMockMvcBuilder>(springSecurity())
        .build()
  }

  private fun perform(
    query: String,
    variables: String = "{}",
  ): ResultActions =
    mockMvc
      .perform(
        post("/graphql")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"query": ${jsonEncode(query)}, "variables": $variables}"""),
      ).andExpect(status().isOk)

  /** 简单 JSON 字符串编码(查询中可能含双引号,如内联字符串参数) */
  private fun jsonEncode(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

  private fun seedAdmin(
    username: String,
    phone: String,
    role: AdminRoleModel = AdminRoleModel.ADMIN,
    accountStatus: AdminAccountStatus = AdminAccountStatus.ACTIVE,
  ): AdminModel =
    adminRepository.save(
      AdminModel(
        username = username,
        passwordHash = requireNotNull(passwordEncoder.encode("oldpass123")),
        realName = "测试管理员",
        phone = phone,
        role = role,
        accountStatus = accountStatus,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      ),
    )

  private fun seedUser(nickname: String): UserModel =
    userRepository.save(
      UserModel(
        wechatOpenId = "openid_${System.nanoTime()}",
        nickname = nickname,
        phone = "13900139009",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      ),
    )

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_ADMIN_USER_WRITE"])
  @DisplayName("deleteAdmin deletes an existing admin (soft delete)")
  fun `deleteAdmin removes admin`() {
    val target = seedAdmin("del_target_${System.nanoTime()}", "13900139001")

    perform("mutation { deleteAdmin(id: ${target.id}) }")
      .andExpect(jsonPath("$.data.deleteAdmin").value(true))

    assertTrue(adminRepository.findAdminById(target.id!!) == null, "删除后 findById 应为空(软删除过滤)")
  }

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_ADMIN_USER_WRITE"])
  @DisplayName("deleteAdmin rejects deleting the last super admin")
  fun `deleteAdmin rejects last super admin deletion`() {
    // 场景:唯一的超级管理员(seed 前先清理 SUPER_ADMIN,避免测试库残留干扰)
    adminRepository.findByRole(AdminRoleModel.SUPER_ADMIN).forEach { adminRepository.deleteById(it.id!!) }
    val onlySuper = seedAdmin("super_only", "13900139002", AdminRoleModel.SUPER_ADMIN)

    // BadRequestException 经 GraphQL 包装为 errors(INTERNAL_ERROR),业务数据不应被删除
    perform("mutation { deleteAdmin(id: ${onlySuper.id}) }")
      .andExpect(jsonPath("$.errors").exists())
      .andExpect(jsonPath("$.data.deleteAdmin").doesNotExist())

    assertNotNull(adminRepository.findAdminById(onlySuper.id!!))
  }

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_ADMIN_USER_WRITE"])
  @DisplayName("resetAdminPassword resets to default password 123456 when newPassword is null")
  fun `resetAdminPassword resets to default password`() {
    val target = seedAdmin("reset_${System.nanoTime()}", "13900139003")
    val oldHash = adminRepository.findAdminById(target.id!!)!!.passwordHash

    perform("mutation { resetAdminPassword(id: ${target.id}, newPassword: null) }")
      .andExpect(jsonPath("$.data.resetAdminPassword").value(true))

    val newHash = adminRepository.findAdminById(target.id!!)!!.passwordHash
    assertTrue(oldHash != newHash)
    assertTrue(passwordEncoder.matches("123456", newHash), "缺省新密码应为 123456(与 PRD 一致)")
  }

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_ADMIN_USER_WRITE"])
  @DisplayName("resetAdminPassword sets the provided new password")
  fun `resetAdminPassword sets provided password`() {
    val target = seedAdmin("reset2_${System.nanoTime()}", "13900139004")

    perform(
      "mutation Reset(\$id: Long!, \$newPassword: String) { resetAdminPassword(id: \$id, newPassword: \$newPassword) }",
      """{"id": ${target.id}, "newPassword": "newpass123"}""",
    ).andExpect(jsonPath("$.data.resetAdminPassword").value(true))

    assertTrue(passwordEncoder.matches("newpass123", adminRepository.findAdminById(target.id!!)!!.passwordHash))
  }

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_ADMIN_USER_WRITE"])
  @DisplayName("updateAdmin with accountStatus DISABLED disables the account")
  fun `updateAdmin disables account`() {
    val target = seedAdmin("disable_${System.nanoTime()}", "13900139005")

    perform(
      "mutation Update(\$id: Long!, \$input: UpdateAdminInput!) { updateAdmin(id: \$id, input: \$input) { id accountStatus } }",
      """{"id": ${target.id}, "input": {"accountStatus": "DISABLED"}}""",
    ).andExpect(jsonPath("$.data.updateAdmin.accountStatus").value("DISABLED"))

    assertEquals(AdminAccountStatus.DISABLED, adminRepository.findAdminById(target.id!!)!!.accountStatus)
  }

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_DELIVERY_WORKER_DELETE"])
  @DisplayName("deleteDeliveryWorker removes a delivery worker account")
  fun `deleteDeliveryWorker removes worker`() {
    val worker =
      seedAdmin(
        "del_worker_${System.nanoTime()}",
        "13900139006",
        AdminRoleModel.DELIVERY_WORKER,
      )

    perform("mutation { deleteDeliveryWorker(id: ${worker.id}) }")
      .andExpect(jsonPath("$.data.deleteDeliveryWorker").value(true))

    assertTrue(adminRepository.findAdminById(worker.id!!) == null)
  }

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_DELIVERY_WORKER_RESET_PASSWORD"])
  @DisplayName("resetDeliveryWorkerPassword resets worker password")
  fun `resetDeliveryWorkerPassword resets worker`() {
    val worker =
      seedAdmin(
        "del_worker2_${System.nanoTime()}",
        "13900139007",
        AdminRoleModel.DELIVERY_WORKER,
      )

    perform(
      "mutation Reset(\$id: Long!, \$newPassword: String) { resetDeliveryWorkerPassword(id: \$id, newPassword: \$newPassword) }",
      """{"id": ${worker.id}, "newPassword": "newpass456"}""",
    ).andExpect(jsonPath("$.data.resetDeliveryWorkerPassword").value(true))

    assertTrue(passwordEncoder.matches("newpass456", adminRepository.findAdminById(worker.id!!)!!.passwordHash))
  }

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_USER_WRITE"])
  @DisplayName("createUser creates a user via admin")
  fun `createUser creates user`() {
    perform(
      "mutation Create(\$input: CreateUserInput!) { createUser(input: \$input) { id nickname phone } }",
      """{"input": {"nickname": "后台用户", "phone": "13900139010"}}""",
    ).andExpect(jsonPath("$.data.createUser.nickname").value("后台用户"))

    val created = userRepository.findByPhone("13900139010")
    assertNotNull(created)
    assertTrue(created!!.wechatOpenId.startsWith("manual_"), "后台创建用户 openid 应为 manual_ 占位")
  }

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_USER_WRITE"])
  @DisplayName("updateUser updates user status via admin")
  fun `updateUser updates status`() {
    val user = seedUser("待改用户")

    perform(
      "mutation Update(\$id: Long!, \$input: UpdateUserInput!) { updateUser(id: \$id, input: \$input) { id status } }",
      """{"id": ${user.id}, "input": {"status": "SUSPENDED"}}""",
    ).andExpect(jsonPath("$.data.updateUser.status").value("SUSPENDED"))

    assertEquals(
      dev.yidafu.aqua.common.graphql.generated.UserStatus.SUSPENDED,
      userRepository.findById(user.id!!).get().status,
    )
  }

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_USER_DELETE"])
  @DisplayName("deleteUser soft deletes a user")
  fun `deleteUser removes user`() {
    val user = seedUser("待删用户")

    perform("mutation { deleteUser(id: ${user.id}) }")
      .andExpect(jsonPath("$.data.deleteUser").value(true))

    assertTrue(userRepository.findById(user.id!!).isEmpty, "软删除后 findById 应为空")
  }

  @Test
  @WithMockUser(authorities = ["ROLE_ADMIN", "PERMISSION_ADMIN_USER_READ"])
  @DisplayName("ADMIN without ADMIN_USER_WRITE is denied deleteAdmin (method security)")
  fun `admin without write permission cannot delete admin`() {
    val target = seedAdmin("deny_${System.nanoTime()}", "13900139011")

    perform("mutation { deleteAdmin(id: ${target.id}) }")
      .andExpect(jsonPath("$.errors").exists())
      .andExpect(jsonPath("$.data.deleteAdmin").doesNotExist())

    assertNotNull(adminRepository.findAdminById(target.id!!))
  }

  @Test
  @WithMockUser(authorities = ["ROLE_DELIVERY_WORKER", "PERMISSION_DELIVERY_TASK"])
  @DisplayName("DELIVERY_WORKER is denied deleteUser (no USER_DELETE)")
  fun `delivery worker cannot delete user`() {
    val user = seedUser("越权用户")

    perform("mutation { deleteUser(id: ${user.id}) }")
      .andExpect(jsonPath("$.errors").exists())
      .andExpect(jsonPath("$.data.deleteUser").doesNotExist())

    assertTrue(userRepository.findById(user.id!!).isPresent)
  }
}
