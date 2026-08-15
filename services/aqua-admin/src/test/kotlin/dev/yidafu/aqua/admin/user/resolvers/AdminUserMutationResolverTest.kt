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

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.AdminRole
import dev.yidafu.aqua.common.graphql.generated.CreateAdminInput
import dev.yidafu.aqua.common.graphql.generated.CreateUserInput
import dev.yidafu.aqua.common.graphql.generated.UpdateAdminInput
import dev.yidafu.aqua.common.graphql.generated.UpdateUserInput
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdminUserMutationResolverTest {
  private lateinit var adminService: AdminService
  private lateinit var userService: UserService
  private lateinit var resolver: AdminUserMutationResolver

  private fun createSampleAdmin(): AdminModel =
    AdminModel(
      id = 1L,
      username = "admin",
      passwordHash = "hashed_password",
      phone = "13900139000",
      role = AdminRoleModel.ADMIN,
      accountStatus = AdminAccountStatus.ACTIVE,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  private fun createSampleUser(): UserModel =
    UserModel(
      id = 1L,
      wechatOpenId = "openid_1",
      nickname = "用户甲",
      phone = "13900139000",
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  private fun mockCurrentUser(id: Long) {
    SecurityContextHolder.getContext().authentication =
      UsernamePasswordAuthenticationToken(
        UserPrincipal(id, "admin", "ADMIN", emptyList()),
        null,
        emptyList(),
      )
  }

  @BeforeEach
  fun setUp() {
    adminService = mockk(relaxed = true)
    userService = mockk(relaxed = true)
    resolver = AdminUserMutationResolver(adminService, userService)
  }

  @AfterEach
  fun tearDown() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `createAdmin should create admin successfully`() {
    // Given
    val input =
      CreateAdminInput(
        username = "newadmin",
        password = "password123",
        realName = "新管理员",
        phone = "13900139001",
        role = AdminRole.ADMIN,
      )
    every { adminService.createAdmin(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
      createSampleAdmin()

    // When
    val result = resolver.createAdmin(input)

    // Then
    assertNotNull(result)
    verify {
      adminService.createAdmin(
        "newadmin",
        "password123",
        "新管理员",
        "13900139001",
        AdminRoleModel.ADMIN,
        null,
        DeliverWorkerModelStatus.OFFLINE,
        true,
        null,
        null,
        null,
        null,
        AdminAccountStatus.ACTIVE,
      )
    }
  }

  @Test
  fun `createAdmin with DELIVERY_WORKER role and null username should delegate auto username generation`() {
    // Given 送水员档案并入 admins 单表(F-USR-021):送水员角色可不传用户名,后端自动生成 delivery_{账号ID}
    val input =
      CreateAdminInput(
        username = null,
        password = "password123",
        realName = "送水员甲",
        phone = "13900139001",
        role = AdminRole.DELIVERY_WORKER,
        avatarUrl = "https://example.com/a.png",
        isAvailable = true,
      )
    every { adminService.createAdmin(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
      createSampleAdmin()

    // When
    val result = resolver.createAdmin(input)

    // Then
    assertNotNull(result)
    verify {
      adminService.createAdmin(
        null,
        "password123",
        "送水员甲",
        "13900139001",
        AdminRoleModel.DELIVERY_WORKER,
        "https://example.com/a.png",
        DeliverWorkerModelStatus.OFFLINE,
        true,
        null,
        null,
        null,
        null,
        AdminAccountStatus.ACTIVE,
      )
    }
  }

  @Test
  fun `createAdmin should throw exception when username is blank`() {
    // Given
    val input =
      CreateAdminInput(
        username = "",
        password = "password123",
        realName = "新管理员",
        phone = "13900139001",
        role = AdminRole.ADMIN,
      )

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.createAdmin(input) }
    assertTrue(exception.message!!.contains("用户名不能为空"))
  }

  @Test
  fun `createAdmin should throw exception when username is too short`() {
    // Given
    val input =
      CreateAdminInput(
        username = "ab",
        password = "password123",
        realName = "新管理员",
        phone = "13900139001",
        role = AdminRole.ADMIN,
      )

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.createAdmin(input) }
    assertTrue(exception.message!!.contains("用户名长度"))
  }

  @Test
  fun `createAdmin should throw exception when username contains invalid characters`() {
    // Given
    val input =
      CreateAdminInput(
        username = "admin@123",
        password = "password123",
        realName = "新管理员",
        phone = "13900139001",
        role = AdminRole.ADMIN,
      )

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.createAdmin(input) }
    assertTrue(exception.message!!.contains("只能包含字母"))
  }

  @Test
  fun `updateAdmin should update admin successfully`() {
    // Given
    val input = UpdateAdminInput(realName = "新名称", phone = "13900139002", role = AdminRole.ADMIN)
    val updatedAdmin = createSampleAdmin()
    every { adminService.updateAdmin(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
      updatedAdmin

    // When
    val result = resolver.updateAdmin(1L, input)

    // Then
    assertNotNull(result)
    verify {
      adminService.updateAdmin(
        1L,
        "新名称",
        "13900139002",
        AdminRoleModel.ADMIN,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
      )
    }
  }

  @Test
  fun `deleteAdmin should delete admin successfully`() {
    // Given 当前登录用户为另一账号(非目标)
    mockCurrentUser(100L)
    val target = createSampleAdmin().copy(id = 2L)
    every { adminService.findById(2L) } returns target
    every { adminService.deleteById(2L) } returns true

    // When
    val result = resolver.deleteAdmin(2L)

    // Then
    assertTrue(result)
    verify { adminService.deleteById(2L) }
  }

  @Test
  fun `deleteAdmin should reject deleting self`() {
    // Given 当前登录用户即目标账号
    mockCurrentUser(1L)
    every { adminService.findById(1L) } returns createSampleAdmin()

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.deleteAdmin(1L) }
    assertTrue(exception.message!!.contains("不能删除当前登录账号"))
  }

  @Test
  fun `deleteAdmin should reject deleting the last super admin`() {
    // Given
    mockCurrentUser(100L)
    val superAdmin = createSampleAdmin().copy(id = 2L, role = AdminRoleModel.SUPER_ADMIN)
    every { adminService.findById(2L) } returns superAdmin
    every { adminService.countByRole(AdminRoleModel.SUPER_ADMIN) } returns 1L

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.deleteAdmin(2L) }
    assertTrue(exception.message!!.contains("至少保留一个超级管理员"))
  }

  @Test
  fun `deleteAdmin should return false when admin not found`() {
    // Given
    mockCurrentUser(100L)
    every { adminService.findById(999L) } returns null

    // When
    val result = resolver.deleteAdmin(999L)

    // Then
    assertFalse(result)
  }

  @Test
  fun `resetAdminPassword should reset password successfully`() {
    // Given
    every { adminService.resetPassword(1L, "newpass123") } returns createSampleAdmin()

    // When
    val result = resolver.resetAdminPassword(1L, "newpass123")

    // Then
    assertTrue(result)
    verify { adminService.resetPassword(1L, "newpass123") }
  }

  @Test
  fun `resetAdminPassword with null newPassword should delegate default reset`() {
    // Given
    every { adminService.resetPassword(1L, null) } returns createSampleAdmin()

    // When
    val result = resolver.resetAdminPassword(1L, null)

    // Then
    assertTrue(result)
    verify { adminService.resetPassword(1L, null) }
  }

  @Test
  fun `createUser should create user via admin service`() {
    // Given
    val input = CreateUserInput(nickname = "新用户", phone = "13900139002")
    every { userService.createUserByAdmin("新用户", "13900139002", null) } returns createSampleUser()

    // When
    val result = resolver.createUser(input)

    // Then
    assertNotNull(result)
    verify { userService.createUserByAdmin("新用户", "13900139002", null) }
  }

  @Test
  fun `updateUser should update user via admin service`() {
    // Given
    val input = UpdateUserInput(nickname = "改名", status = UserStatus.SUSPENDED)
    every { userService.updateUserByAdmin(1L, "改名", null, null, UserStatus.SUSPENDED) } returns createSampleUser()

    // When
    val result = resolver.updateUser(1L, input)

    // Then
    assertNotNull(result)
    verify { userService.updateUserByAdmin(1L, "改名", null, null, UserStatus.SUSPENDED) }
  }

  @Test
  fun `deleteUser should delete user via admin service`() {
    // Given
    every { userService.deleteUserById(1L) } returns true

    // When
    val result = resolver.deleteUser(1L)

    // Then
    assertTrue(result)
    verify { userService.deleteUserById(1L) }
  }
}
