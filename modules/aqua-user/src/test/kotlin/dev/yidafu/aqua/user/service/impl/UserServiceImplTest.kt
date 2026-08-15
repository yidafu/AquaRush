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

package dev.yidafu.aqua.user.service.impl

import dev.yidafu.aqua.common.domain.model.NotificationSettingsModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.domain.repository.NotificationSettingsRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserServiceImplTest {
  private lateinit var userRepository: UserRepository
  private lateinit var notificationSettingsRepository: NotificationSettingsRepository
  private lateinit var bizLogger: BizLogger
  private lateinit var service: UserServiceImpl

  private fun sampleUser(
    id: Long = 1L,
    openId: String = "openid_1",
  ): UserModel =
    UserModel(
      id = id,
      wechatOpenId = openId,
      nickname = "alice",
      phone = "13800138000",
      avatarUrl = "http://avatar",
      email = "e@e.com",
      status = UserStatus.ACTIVE,
      role = UserRole.USER,
      customerGroupId = null,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
      lastLoginAt = LocalDateTime.now(),
    )

  @BeforeEach
  fun setUp() {
    userRepository = mockk(relaxed = true)
    notificationSettingsRepository = mockk(relaxed = true)
    bizLogger = mockk(relaxed = true)
    // relaxed mock 对泛型返回(save 返回 S)会生成 Object 导致 ClassCastException,需显式 stub
    every { userRepository.save(any()) } answers { firstArg() }
    every { notificationSettingsRepository.save(any()) } answers { firstArg() }
    service = UserServiceImpl(userRepository, notificationSettingsRepository, bizLogger)
  }

  @Test
  fun `findById should return user when present`() {
    every { userRepository.findById(1L) } returns Optional.of(sampleUser())
    val user = service.findById(1L)
    assertNotNull(user)
    assertEquals(1L, user?.id)
  }

  @Test
  fun `findById should return null when absent`() {
    every { userRepository.findById(999L) } returns Optional.empty()
    assertEquals(null, service.findById(999L))
  }

  @Test
  fun `findByWechatOpenId should delegate`() {
    every { userRepository.findByWechatOpenId("openid_1") } returns sampleUser()
    assertEquals(1L, service.findByWechatOpenId("openid_1")?.id)
  }

  @Test
  fun `createUser should save user and notification settings`() {
    every { userRepository.save(any()) } answers { firstArg<UserModel>().also { it.id = 10L } }
    every { bizLogger.resolveOperator() } returns (1L to "admin")

    val created = service.createUser("openid_new", "nick", "http://avatar")

    assertEquals("openid_new", created.wechatOpenId)
    assertEquals(10L, created.id)
    verify { notificationSettingsRepository.save(any<NotificationSettingsModel>()) }
    verify { bizLogger.logUserOperation(10L, "USER_CREATED", "SUCCESS", 1L, "admin", any()) }
  }

  @Test
  fun `updateUserInfo should update fields and save`() {
    val user = sampleUser()
    every { userRepository.findById(1L) } returns Optional.of(user)
    every { userRepository.save(any()) } answers { firstArg() }
    every { bizLogger.resolveOperator() } returns (null to null)

    val updated = service.updateUserInfo(1L, nickname = "bob", phone = "139", avatarUrl = "http://new")

    assertEquals("bob", updated.nickname)
    assertEquals("139", updated.phone)
    assertEquals("http://new", updated.avatarUrl)
    verify { bizLogger.logUserOperation(1L, "USER_UPDATED", "SUCCESS", null, null, any()) }
  }

  @Test
  fun `updateUserInfo should throw when user not found`() {
    every { userRepository.findById(999L) } returns Optional.empty()
    assertThrows<IllegalArgumentException> { service.updateUserInfo(999L, nickname = "x", phone = null, avatarUrl = null) }
  }

  @Test
  fun `updateUserInfo with null fields should keep existing values`() {
    val user = sampleUser()
    every { userRepository.findById(1L) } returns Optional.of(user)
    every { userRepository.save(any()) } answers { firstArg() }
    every { bizLogger.resolveOperator() } returns (null to null)
    val updated = service.updateUserInfo(1L, nickname = null, phone = null, avatarUrl = null)
    assertEquals("alice", updated.nickname)
    assertEquals("13800138000", updated.phone)
  }

  @Test
  fun `existsByWechatOpenId should delegate`() {
    every { userRepository.existsByWechatOpenId("openid_1") } returns true
    assertTrue(service.existsByWechatOpenId("openid_1"))
  }

  @Test
  fun `createUserByAdmin should use manual placeholder openid`() {
    every { userRepository.save(any()) } answers { firstArg<UserModel>().also { it.id = 20L } }
    every { bizLogger.resolveOperator() } returns (null to null)

    val created = service.createUserByAdmin(nickname = "admin-created", phone = "13900139000", avatarUrl = null)

    assertTrue(created.wechatOpenId.startsWith("manual_"))
    assertEquals("admin-created", created.nickname)
    assertEquals(UserStatus.ACTIVE, created.status)
    verify { notificationSettingsRepository.save(any<NotificationSettingsModel>()) }
  }

  @Test
  fun `updateUserByAdmin should update status`() {
    val user = sampleUser()
    every { userRepository.findById(1L) } returns Optional.of(user)
    every { userRepository.save(any()) } answers { firstArg() }
    every { bizLogger.resolveOperator() } returns (null to null)

    val updated = service.updateUserByAdmin(1L, nickname = null, phone = null, avatarUrl = null, status = UserStatus.SUSPENDED)

    assertEquals(UserStatus.SUSPENDED, updated.status)
  }

  @Test
  fun `updateUserByAdmin should throw when user not found`() {
    every { userRepository.findById(999L) } returns Optional.empty()
    assertThrows<IllegalArgumentException> {
      service.updateUserByAdmin(
        999L,
        nickname = null,
        phone = null,
        avatarUrl = null,
        status = null,
      )
    }
  }

  @Test
  fun `deleteUserById should return false when user absent`() {
    every { userRepository.existsById(999L) } returns false
    assertFalse(service.deleteUserById(999L))
    verify(exactly = 0) { userRepository.deleteById(999L) }
  }

  @Test
  fun `deleteUserById should delete and log when user present`() {
    every { userRepository.existsById(1L) } returns true
    every { bizLogger.resolveOperator() } returns (null to null)
    assertTrue(service.deleteUserById(1L))
    verify { userRepository.deleteById(1L) }
    verify { bizLogger.logUserOperation(1L, "USER_DELETED", "SUCCESS", null, null, any()) }
  }

  @Test
  fun `findAllUsers should delegate to repository`() {
    val pageable = PageRequest.of(0, 10)
    val page: Page<UserModel> = PageImpl(listOf(sampleUser()), pageable, 1)
    every { userRepository.findAll(pageable) } returns page
    assertEquals(1, service.findAllUsers(pageable).totalElements)
  }

  @Test
  fun `findUsersByKeyword should delegate`() {
    val pageable = PageRequest.of(0, 10)
    val page: Page<UserModel> = PageImpl(listOf(sampleUser()), pageable, 1)
    every { userRepository.findByNicknameContainingIgnoreCaseOrPhoneContainingIgnoreCase("kw", "kw", pageable) } returns page
    assertEquals(1, service.findUsersByKeyword("kw", pageable).totalElements)
  }

  @Test
  fun `findUsersByStatus should delegate`() {
    val pageable = PageRequest.of(0, 10)
    val page: Page<UserModel> = PageImpl(emptyList(), pageable, 0)
    every { userRepository.findByStatus(UserStatus.ACTIVE, pageable) } returns page
    assertEquals(0, service.findUsersByStatus(UserStatus.ACTIVE, pageable).totalElements)
  }

  @Test
  fun `findUsersByKeywordAndStatus should delegate`() {
    val pageable = PageRequest.of(0, 10)
    val page: Page<UserModel> = PageImpl(listOf(sampleUser()), pageable, 1)
    every {
      userRepository.findByNicknameContainingIgnoreCaseAndStatusOrPhoneContainingIgnoreCaseAndStatus("kw", UserStatus.ACTIVE, pageable)
    } returns page
    assertEquals(1, service.findUsersByKeywordAndStatus("kw", UserStatus.ACTIVE, pageable).totalElements)
  }

  @Test
  fun `findUsersByCustomerGroupId should delegate`() {
    val pageable = PageRequest.of(0, 10)
    val page: Page<UserModel> = PageImpl(listOf(sampleUser()), pageable, 1)
    every { userRepository.findByCustomerGroupId(5L, pageable) } returns page
    assertEquals(1, service.findUsersByCustomerGroupId(5L, pageable).totalElements)
  }

  @Test
  fun `setCustomerGroup should update group and log`() {
    val user = sampleUser().also { it.customerGroupId = 1L }
    every { userRepository.findById(1L) } returns Optional.of(user)
    every { userRepository.save(any()) } answers { firstArg() }
    every { bizLogger.resolveOperator() } returns (null to null)

    val updated = service.setCustomerGroup(1L, customerGroupId = 9L)

    assertEquals(9L, updated.customerGroupId)
    verify { bizLogger.logMarketingOperation(1L, "CUSTOMER_GROUP_USER_SET", "SUCCESS", null, null, any()) }
  }

  @Test
  fun `setCustomerGroup should throw when user not found`() {
    every { userRepository.findById(999L) } returns Optional.empty()
    assertThrows<IllegalArgumentException> { service.setCustomerGroup(999L, customerGroupId = 1L) }
  }
}
