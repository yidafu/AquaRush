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
import dev.yidafu.aqua.admin.testutil.setMockUser
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
import io.kotest.assertions.withClue
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
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
 * 用户与账号域管理 mutation 集成测试(spec/prd/07-user.md §F-USR-020/021/022,G11 落地):
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
@ApplyExtension(SpringExtension::class)
class AdminUserManagementGraphQLIntegrationTest(
  context: WebApplicationContext,
  adminRepository: AdminRepository,
  userRepository: UserRepository,
  passwordEncoder: PasswordEncoder,
) : FunSpec({
    var mockMvc: MockMvc = mockk()

    fun jsonEncode(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

    fun seedAdmin(
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

    fun seedUser(nickname: String): UserModel =
      userRepository.save(
        UserModel(
          wechatOpenId = "openid_${System.nanoTime()}",
          nickname = nickname,
          phone = "13900139009",
          createdAt = LocalDateTime.now(),
          updatedAt = LocalDateTime.now(),
        ),
      )

    fun perform(
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

    beforeTest {
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()
    }
    test("deleteAdmin removes admin") {
      setMockUser(authorities = listOf("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_ADMIN_USER_WRITE"))
      val target = seedAdmin("del_target_${System.nanoTime()}", "13900139001")

      perform("mutation { deleteAdmin(id: ${target.id}) }")
        .andExpect(jsonPath("$.data.deleteAdmin").value(true))

      withClue("删除后 findById 应为空(软删除过滤)") { (adminRepository.findAdminById(target.id!!) == null) shouldBe true }
    }
    test("deleteAdmin rejects last super admin deletion") {
      setMockUser(authorities = listOf("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_ADMIN_USER_WRITE"))
      // 场景:唯一的超级管理员(seed 前先清理 SUPER_ADMIN,避免测试库残留干扰)
      adminRepository.findByRole(AdminRoleModel.SUPER_ADMIN).forEach { adminRepository.deleteById(it.id!!) }
      val onlySuper = seedAdmin("super_only", "13900139002", AdminRoleModel.SUPER_ADMIN)

      // BadRequestException 经 GraphQL 包装为 errors(INTERNAL_ERROR),业务数据不应被删除
      perform("mutation { deleteAdmin(id: ${onlySuper.id}) }")
        .andExpect(jsonPath("$.errors").exists())
        .andExpect(jsonPath("$.data.deleteAdmin").doesNotExist())

      adminRepository.findAdminById(onlySuper.id!!).shouldNotBeNull()
    }
    test("resetAdminPassword resets to default password") {
      setMockUser(authorities = listOf("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_ADMIN_USER_WRITE"))
      val target = seedAdmin("reset_${System.nanoTime()}", "13900139003")
      val oldHash = adminRepository.findAdminById(target.id!!)!!.passwordHash

      perform("mutation { resetAdminPassword(id: ${target.id}, newPassword: null) }")
        .andExpect(jsonPath("$.data.resetAdminPassword").value(true))

      val newHash = adminRepository.findAdminById(target.id!!)!!.passwordHash
      (oldHash != newHash) shouldBe true
      withClue("缺省新密码应为 123456(与 PRD 一致)") { passwordEncoder.matches("123456", newHash) shouldBe true }
    }
    test("resetAdminPassword sets provided password") {
      setMockUser(authorities = listOf("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_ADMIN_USER_WRITE"))
      val target = seedAdmin("reset2_${System.nanoTime()}", "13900139004")

      perform(
        "mutation Reset(\$id: Long!, \$newPassword: String) { resetAdminPassword(id: \$id, newPassword: \$newPassword) }",
        """{"id": ${target.id}, "newPassword": "newpass123"}""",
      ).andExpect(jsonPath("$.data.resetAdminPassword").value(true))

      passwordEncoder.matches("newpass123", adminRepository.findAdminById(target.id!!)!!.passwordHash) shouldBe true
    }
    test("updateAdmin disables account") {
      setMockUser(authorities = listOf("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_ADMIN_USER_WRITE"))
      val target = seedAdmin("disable_${System.nanoTime()}", "13900139005")

      perform(
        "mutation Update(\$id: Long!, \$input: UpdateAdminInput!) { updateAdmin(id: \$id, input: \$input) { id accountStatus } }",
        """{"id": ${target.id}, "input": {"accountStatus": "DISABLED"}}""",
      ).andExpect(jsonPath("$.data.updateAdmin.accountStatus").value("DISABLED"))

      adminRepository.findAdminById(target.id!!)!!.accountStatus shouldBe AdminAccountStatus.DISABLED
    }
    test("deleteDeliveryWorker removes worker") {
      setMockUser(authorities = listOf("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_DELIVERY_WORKER_DELETE"))
      val worker =
        seedAdmin(
          "del_worker_${System.nanoTime()}",
          "13900139006",
          AdminRoleModel.DELIVERY_WORKER,
        )

      perform("mutation { deleteDeliveryWorker(id: ${worker.id}) }")
        .andExpect(jsonPath("$.data.deleteDeliveryWorker").value(true))

      (adminRepository.findAdminById(worker.id!!) == null) shouldBe true
    }
    test("resetDeliveryWorkerPassword resets worker") {
      setMockUser(authorities = listOf("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_DELIVERY_WORKER_RESET_PASSWORD"))
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

      passwordEncoder.matches("newpass456", adminRepository.findAdminById(worker.id!!)!!.passwordHash) shouldBe true
    }
    test("createUser creates user") {
      setMockUser(authorities = listOf("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_USER_WRITE"))
      perform(
        "mutation Create(\$input: CreateUserInput!) { createUser(input: \$input) { id nickname phone } }",
        """{"input": {"nickname": "后台用户", "phone": "13900139010"}}""",
      ).andExpect(jsonPath("$.data.createUser.nickname").value("后台用户"))

      val created = userRepository.findByPhone("13900139010")
      created.shouldNotBeNull()
      withClue("后台创建用户 openid 应为 manual_ 占位") { created!!.wechatOpenId.startsWith("manual_") shouldBe true }
    }
    test("updateUser updates status") {
      setMockUser(authorities = listOf("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_USER_WRITE"))
      val user = seedUser("待改用户")

      perform(
        "mutation Update(\$id: Long!, \$input: UpdateUserInput!) { updateUser(id: \$id, input: \$input) { id status } }",
        """{"id": ${user.id}, "input": {"status": "SUSPENDED"}}""",
      ).andExpect(jsonPath("$.data.updateUser.status").value("SUSPENDED"))

      userRepository.findById(user.id!!).get().status shouldBe dev.yidafu.aqua.common.graphql.generated.UserStatus.SUSPENDED
    }
    test("deleteUser removes user") {
      setMockUser(authorities = listOf("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_USER_DELETE"))
      val user = seedUser("待删用户")

      perform("mutation { deleteUser(id: ${user.id}) }")
        .andExpect(jsonPath("$.data.deleteUser").value(true))

      withClue("软删除后 findById 应为空") { userRepository.findById(user.id!!).isEmpty shouldBe true }
    }
    test("admin without write permission cannot delete admin") {
      setMockUser(authorities = listOf("ROLE_ADMIN", "PERMISSION_ADMIN_USER_READ"))
      val target = seedAdmin("deny_${System.nanoTime()}", "13900139011")

      perform("mutation { deleteAdmin(id: ${target.id}) }")
        .andExpect(jsonPath("$.errors").exists())
        .andExpect(jsonPath("$.data.deleteAdmin").doesNotExist())

      adminRepository.findAdminById(target.id!!).shouldNotBeNull()
    }
    test("delivery worker cannot delete user") {
      setMockUser(authorities = listOf("ROLE_DELIVERY_WORKER", "PERMISSION_DELIVERY_TASK"))
      val user = seedUser("越权用户")

      perform("mutation { deleteUser(id: ${user.id}) }")
        .andExpect(jsonPath("$.errors").exists())
        .andExpect(jsonPath("$.data.deleteUser").doesNotExist())

      userRepository.findById(user.id!!).isPresent shouldBe true
    }
  })
