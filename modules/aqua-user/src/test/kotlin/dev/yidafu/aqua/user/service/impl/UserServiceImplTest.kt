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

import dev.yidafu.aqua.api.service.notification.NotificationMutationApiService
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.domain.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.Optional

class UserServiceImplTest :
  FunSpec({
    var userRepository: UserRepository = mockk()
    var notificationMutationApiService: NotificationMutationApiService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: UserServiceImpl = mockk()

    fun sampleUser(
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

    beforeTest {
      userRepository = mockk(relaxed = true)
      notificationMutationApiService = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      // relaxed mock 对泛型返回(save 返回 S)会生成 Object 导致 ClassCastException,需显式 stub
      every { userRepository.save(any()) } answers { firstArg() }
      service = UserServiceImpl(userRepository, notificationMutationApiService, bizLogger)
    }
    test("findById should return user when present") {
      every { userRepository.findById(1L) } returns Optional.of(sampleUser())
      val user = service.findById(1L)
      user.shouldNotBeNull()
      user?.id shouldBe 1L
    }
    test("findById should return null when absent") {
      every { userRepository.findById(999L) } returns Optional.empty()
      service.findById(999L) shouldBe null
    }
    test("findByWechatOpenId should delegate") {
      every { userRepository.findByWechatOpenId("openid_1") } returns sampleUser()
      service.findByWechatOpenId("openid_1")?.id shouldBe 1L
    }
    test("createUser should save user and default notification settings") {
      every { userRepository.save(any()) } answers { firstArg<UserModel>().also { it.id = 10L } }
      every { bizLogger.resolveOperator() } returns (1L to "admin")

      val created = service.createUser("openid_new", "nick", "http://avatar")

      created.wechatOpenId shouldBe "openid_new"
      created.id shouldBe 10L
      verify { notificationMutationApiService.createDefaultUserSettings(10L) }
      verify { bizLogger.logUserOperation(10L, "USER_CREATED", "SUCCESS", 1L, "admin", any()) }
    }
    test("updateUserInfo should update fields and save") {
      val user = sampleUser()
      every { userRepository.findById(1L) } returns Optional.of(user)
      every { userRepository.save(any()) } answers { firstArg() }
      every { bizLogger.resolveOperator() } returns (null to null)

      val updated = service.updateUserInfo(1L, nickname = "bob", phone = "139", avatarUrl = "http://new")

      updated.nickname shouldBe "bob"
      updated.phone shouldBe "139"
      updated.avatarUrl shouldBe "http://new"
      verify { bizLogger.logUserOperation(1L, "USER_UPDATED", "SUCCESS", null, null, any()) }
    }
    test("updateUserInfo should throw when user not found") {
      every { userRepository.findById(999L) } returns Optional.empty()
      shouldThrow<IllegalArgumentException> { service.updateUserInfo(999L, nickname = "x", phone = null, avatarUrl = null) }
    }
    test("updateUserInfo with null fields should keep existing values") {
      val user = sampleUser()
      every { userRepository.findById(1L) } returns Optional.of(user)
      every { userRepository.save(any()) } answers { firstArg() }
      every { bizLogger.resolveOperator() } returns (null to null)
      val updated = service.updateUserInfo(1L, nickname = null, phone = null, avatarUrl = null)
      updated.nickname shouldBe "alice"
      updated.phone shouldBe "13800138000"
    }
    test("existsByWechatOpenId should delegate") {
      every { userRepository.existsByWechatOpenId("openid_1") } returns true
      service.existsByWechatOpenId("openid_1") shouldBe true
    }
    test("createUserByAdmin should use manual placeholder openid") {
      every { userRepository.save(any()) } answers { firstArg<UserModel>().also { it.id = 20L } }
      every { bizLogger.resolveOperator() } returns (null to null)

      val created = service.createUserByAdmin(nickname = "admin-created", phone = "13900139000", avatarUrl = null)

      created.wechatOpenId.startsWith("manual_") shouldBe true
      created.nickname shouldBe "admin-created"
      created.status shouldBe UserStatus.ACTIVE
      verify { notificationMutationApiService.createDefaultUserSettings(20L) }
    }
    test("updateUserByAdmin should update status") {
      val user = sampleUser()
      every { userRepository.findById(1L) } returns Optional.of(user)
      every { userRepository.save(any()) } answers { firstArg() }
      every { bizLogger.resolveOperator() } returns (null to null)

      val updated = service.updateUserByAdmin(1L, nickname = null, phone = null, avatarUrl = null, status = UserStatus.SUSPENDED)

      updated.status shouldBe UserStatus.SUSPENDED
    }
    test("updateUserByAdmin should throw when user not found") {
      every { userRepository.findById(999L) } returns Optional.empty()
      shouldThrow<IllegalArgumentException> {
        service.updateUserByAdmin(
          999L,
          nickname = null,
          phone = null,
          avatarUrl = null,
          status = null,
        )
      }
    }
    test("deleteUserById should return false when user absent") {
      every { userRepository.existsById(999L) } returns false
      service.deleteUserById(999L) shouldBe false
      verify(exactly = 0) { userRepository.deleteById(999L) }
    }
    test("deleteUserById should delete and log when user present") {
      every { userRepository.existsById(1L) } returns true
      every { bizLogger.resolveOperator() } returns (null to null)
      service.deleteUserById(1L) shouldBe true
      verify { userRepository.deleteById(1L) }
      verify { bizLogger.logUserOperation(1L, "USER_DELETED", "SUCCESS", null, null, any()) }
    }
    test("findAllUsers should delegate to repository") {
      val pageable = PageRequest.of(0, 10)
      val page: Page<UserModel> = PageImpl(listOf(sampleUser()), pageable, 1)
      every { userRepository.findAll(pageable) } returns page
      service.findAllUsers(pageable).totalElements shouldBe 1
    }
    test("findUsersByKeyword should delegate") {
      val pageable = PageRequest.of(0, 10)
      val page: Page<UserModel> = PageImpl(listOf(sampleUser()), pageable, 1)
      every { userRepository.findByNicknameContainingIgnoreCaseOrPhoneContainingIgnoreCase("kw", "kw", pageable) } returns page
      service.findUsersByKeyword("kw", pageable).totalElements shouldBe 1
    }
    test("findUsersByStatus should delegate") {
      val pageable = PageRequest.of(0, 10)
      val page: Page<UserModel> = PageImpl(emptyList(), pageable, 0)
      every { userRepository.findByStatus(UserStatus.ACTIVE, pageable) } returns page
      service.findUsersByStatus(UserStatus.ACTIVE, pageable).totalElements shouldBe 0
    }
    test("findUsersByKeywordAndStatus should delegate") {
      val pageable = PageRequest.of(0, 10)
      val page: Page<UserModel> = PageImpl(listOf(sampleUser()), pageable, 1)
      every {
        userRepository.findByNicknameContainingIgnoreCaseAndStatusOrPhoneContainingIgnoreCaseAndStatus("kw", UserStatus.ACTIVE, pageable)
      } returns page
      service.findUsersByKeywordAndStatus("kw", UserStatus.ACTIVE, pageable).totalElements shouldBe 1
    }
    test("findUsersByCustomerGroupId should delegate") {
      val pageable = PageRequest.of(0, 10)
      val page: Page<UserModel> = PageImpl(listOf(sampleUser()), pageable, 1)
      every { userRepository.findByCustomerGroupId(5L, pageable) } returns page
      service.findUsersByCustomerGroupId(5L, pageable).totalElements shouldBe 1
    }
    test("setCustomerGroup should update group and log") {
      val user = sampleUser().also { it.customerGroupId = 1L }
      every { userRepository.findById(1L) } returns Optional.of(user)
      every { userRepository.save(any()) } answers { firstArg() }
      every { bizLogger.resolveOperator() } returns (null to null)

      val updated = service.setCustomerGroup(1L, customerGroupId = 9L)

      updated.customerGroupId shouldBe 9L
      verify { bizLogger.logMarketingOperation(1L, "CUSTOMER_GROUP_USER_SET", "SUCCESS", null, null, any()) }
    }
    test("setCustomerGroup should throw when user not found") {
      every { userRepository.findById(999L) } returns Optional.empty()
      shouldThrow<IllegalArgumentException> { service.setCustomerGroup(999L, customerGroupId = 1L) }
    }
  })
