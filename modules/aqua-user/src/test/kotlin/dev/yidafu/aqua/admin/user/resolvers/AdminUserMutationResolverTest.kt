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

import dev.yidafu.aqua.api.service.AdminService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.AdminRole
import dev.yidafu.aqua.common.graphql.generated.CreateAdminInput
import dev.yidafu.aqua.common.graphql.generated.UpdateAdminInput
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdminUserMutationResolverTest {
  private lateinit var adminService: AdminService
  private lateinit var resolver: AdminUserMutationResolver

  private fun createSampleAdmin(): AdminModel =
    AdminModel(
      id = 1L,
      username = "admin",
      passwordHash = "hashed_password",
      phone = "13900139000",
      role = AdminRoleModel.ADMIN,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  @BeforeEach
  fun setUp() {
    adminService = mockk(relaxed = true)
    resolver = AdminUserMutationResolver(adminService)
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
    every { adminService.createAdmin(any(), any(), any(), any(), any()) } returns createSampleAdmin()

    // When
    val result = resolver.createAdmin(input)

    // Then
    assertNotNull(result)
    verify { adminService.createAdmin("newadmin", "password123", "新管理员", "13900139001", AdminRoleModel.ADMIN) }
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
    every { adminService.updateAdmin(any(), any(), any(), any()) } returns updatedAdmin

    // When
    val result = resolver.updateAdmin(1L, input)

    // Then
    assertNotNull(result)
    verify { adminService.updateAdmin(1L, "新名称", "13900139002", AdminRoleModel.ADMIN) }
  }

  @Test
  fun `deleteAdmin should delete admin successfully`() {
    // Given
    every { adminService.deleteAdmin(1L) } returns true

    // When
    val result = resolver.deleteAdmin(1L)

    // Then
    assertTrue(result)
    verify { adminService.deleteAdmin(1L) }
  }

  @Test
  fun `deleteAdmin should throw exception when service fails`() {
    // Given
    every { adminService.deleteAdmin(1L) } throws RuntimeException("Cannot delete")

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.deleteAdmin(1L) }
    assertTrue(exception.message!!.contains("删除管理员失败"))
  }
}
