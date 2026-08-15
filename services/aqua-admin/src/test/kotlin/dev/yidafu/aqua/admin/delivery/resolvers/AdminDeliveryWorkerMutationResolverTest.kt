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

package dev.yidafu.aqua.admin.delivery.resolvers

import dev.yidafu.aqua.admin.delivery.resolvers.AdminDeliveryWorkerMutationResolver.Companion.CreateDeliveryWorkerInput
import dev.yidafu.aqua.admin.delivery.resolvers.AdminDeliveryWorkerMutationResolver.Companion.UpdateDeliveryWorkerInput
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.exception.BadRequestException
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

class AdminDeliveryWorkerMutationResolverTest {
  private lateinit var adminService: AdminService
  private lateinit var resolver: AdminDeliveryWorkerMutationResolver

  private fun createSampleWorker(): AdminModel =
    AdminModel(
      id = 2L,
      username = "delivery_2",
      passwordHash = "hashed_password",
      realName = "送水员甲",
      phone = "13900139000",
      role = AdminRoleModel.DELIVERY_WORKER,
      accountStatus = AdminAccountStatus.ACTIVE,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  private fun validCreateInput(): CreateDeliveryWorkerInput =
    CreateDeliveryWorkerInput(
      wechatOpenId = null,
      name = "送水员甲",
      phone = "13900139000",
      password = "password123",
      avatarUrl = null,
      coordinates = null,
      currentLocation = null,
      rating = 4.5,
      earningCents = 1000L,
      isAvailable = true,
      accountStatus = AdminAccountStatus.ACTIVE,
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
    resolver = AdminDeliveryWorkerMutationResolver(adminService)
  }

  @AfterEach
  fun tearDown() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `createDeliveryWorker should create worker account`() {
    // Given
    every { adminService.createAdmin(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
      createSampleWorker()

    // When
    val result = resolver.createDeliveryWorker(validCreateInput())

    // Then
    assertNotNull(result)
    verify {
      adminService.createAdmin(
        null,
        "password123",
        "送水员甲",
        "13900139000",
        AdminRoleModel.DELIVERY_WORKER,
        null,
        dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus.OFFLINE,
        true,
        null,
        null,
        4.5,
        1000L,
        AdminAccountStatus.ACTIVE,
      )
    }
  }

  @Test
  fun `createDeliveryWorker should reject blank name`() {
    // Given
    val input = validCreateInput().copy(name = "  ")

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.createDeliveryWorker(input) }
    assertTrue(exception.message!!.contains("姓名不能为空"))
  }

  @Test
  fun `createDeliveryWorker should reject blank phone`() {
    // Given
    val input = validCreateInput().copy(phone = " ")

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.createDeliveryWorker(input) }
    assertTrue(exception.message!!.contains("手机号码不能为空"))
  }

  @Test
  fun `createDeliveryWorker should reject invalid phone`() {
    // Given
    val input = validCreateInput().copy(phone = "12345")

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.createDeliveryWorker(input) }
    assertTrue(exception.message!!.contains("手机号码格式不正确"))
  }

  @Test
  fun `createDeliveryWorker should reject negative rating`() {
    // Given
    val input = validCreateInput().copy(rating = -1.0)

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.createDeliveryWorker(input) }
    assertTrue(exception.message!!.contains("评分不能为负数"))
  }

  @Test
  fun `createDeliveryWorker should reject negative earnings`() {
    // Given
    val input = validCreateInput().copy(earningCents = -10L)

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.createDeliveryWorker(input) }
    assertTrue(exception.message!!.contains("收入不能为负数"))
  }

  @Test
  fun `createDeliveryWorker should wrap service errors`() {
    // Given
    every { adminService.createAdmin(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws
      RuntimeException("db down")

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.createDeliveryWorker(validCreateInput()) }
    assertTrue(exception.message!!.contains("创建送水工失败"))
  }

  @Test
  fun `updateDeliveryWorker should update existing worker`() {
    // Given
    every { adminService.findById(2L) } returns createSampleWorker()
    every { adminService.updateAdmin(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
      createSampleWorker()
    val input =
      UpdateDeliveryWorkerInput(
        wechatOpenId = null,
        name = "送水员乙",
        phone = "13800138000",
        avatarUrl = null,
        coordinates = null,
        currentLocation = null,
        rating = 5.0,
        earningCents = 2000L,
        isAvailable = true,
        status = null,
        accountStatus = null,
      )

    // When
    val result = resolver.updateDeliveryWorker(2L, input)

    // Then
    assertNotNull(result)
    verify {
      adminService.updateAdmin(
        2L,
        "送水员乙",
        "13800138000",
        null,
        null,
        null,
        true,
        null,
        null,
        5.0,
        2000L,
        null,
      )
    }
  }

  @Test
  fun `updateDeliveryWorker should wrap not found error`() {
    // Given
    every { adminService.findById(999L) } returns null

    // When & Then
    val exception =
      assertFailsWith<BadRequestException> {
        resolver.updateDeliveryWorker(
          999L,
          UpdateDeliveryWorkerInput(
            wechatOpenId = null,
            name = "x",
            phone = null,
            avatarUrl = null,
            coordinates = null,
            currentLocation = null,
            rating = null,
            earningCents = null,
            isAvailable = null,
            status = null,
            accountStatus = null,
          ),
        )
      }
    assertTrue(exception.message!!.contains("更新送水工失败"))
  }

  @Test
  fun `updateDeliveryWorker should reject invalid phone`() {
    // Given
    every { adminService.findById(2L) } returns createSampleWorker()
    val input =
      UpdateDeliveryWorkerInput(
        wechatOpenId = null,
        name = null,
        phone = "123",
        avatarUrl = null,
        coordinates = null,
        currentLocation = null,
        rating = null,
        earningCents = null,
        isAvailable = null,
        status = null,
        accountStatus = null,
      )

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.updateDeliveryWorker(2L, input) }
    assertTrue(exception.message!!.contains("手机号码格式不正确"))
  }

  @Test
  fun `deleteDeliveryWorker should reject deleting self`() {
    // Given
    mockCurrentUser(2L)

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.deleteDeliveryWorker(2L) }
    assertTrue(exception.message!!.contains("不能删除当前登录账号"))
  }

  @Test
  fun `deleteDeliveryWorker should return false when worker not found`() {
    // Given
    mockCurrentUser(1L)
    every { adminService.findById(999L) } returns null

    // When
    val result = resolver.deleteDeliveryWorker(999L)

    // Then
    assertFalse(result)
  }

  @Test
  fun `deleteDeliveryWorker should reject non delivery worker accounts`() {
    // Given
    mockCurrentUser(1L)
    val admin = createSampleWorker().copy(id = 3L, role = AdminRoleModel.ADMIN)
    every { adminService.findById(3L) } returns admin

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.deleteDeliveryWorker(3L) }
    assertTrue(exception.message!!.contains("不是送水员"))
  }

  @Test
  fun `deleteDeliveryWorker should delete worker`() {
    // Given
    mockCurrentUser(1L)
    every { adminService.findById(2L) } returns createSampleWorker()
    every { adminService.deleteById(2L) } returns true

    // When
    val result = resolver.deleteDeliveryWorker(2L)

    // Then
    assertTrue(result)
    verify { adminService.deleteById(2L) }
  }

  @Test
  fun `resetDeliveryWorkerPassword should reject missing worker`() {
    // Given
    every { adminService.findById(999L) } returns null

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.resetDeliveryWorkerPassword(999L, "newpass") }
    assertTrue(exception.message!!.contains("送水员不存在"))
  }

  @Test
  fun `resetDeliveryWorkerPassword should reject non delivery worker`() {
    // Given
    every { adminService.findById(3L) } returns createSampleWorker().copy(id = 3L, role = AdminRoleModel.ADMIN)

    // When & Then
    val exception = assertFailsWith<BadRequestException> { resolver.resetDeliveryWorkerPassword(3L, "newpass") }
    assertTrue(exception.message!!.contains("无法重置密码"))
  }

  @Test
  fun `resetDeliveryWorkerPassword should reset password`() {
    // Given
    every { adminService.findById(2L) } returns createSampleWorker()
    every { adminService.resetPassword(2L, "newpass") } returns createSampleWorker()

    // When
    val result = resolver.resetDeliveryWorkerPassword(2L, "newpass")

    // Then
    assertTrue(result)
    verify { adminService.resetPassword(2L, "newpass") }
  }
}
