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

import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDateTime
import java.util.Optional

/**
 * 补充 AdminServiceImplTest 未覆盖的增删改查/创建/更新分支。
 */
class AdminServiceImplExtraTest :
  FunSpec({
    var adminRepository: AdminRepository = mockk()
    var userService: UserService = mockk()
    var snowflakeIdGenerator: SnowflakeIdGenerator = mockk()
    var bizLogger: BizLogger = mockk()
    var service: AdminServiceImpl = mockk()

    fun admin(
      id: Long = 1L,
      role: AdminRoleModel = AdminRoleModel.ADMIN,
      userId: Long? = null,
    ): AdminModel =
      AdminModel(
        id = id,
        username = "admin$id",
        passwordHash = "hash",
        realName = "管理员",
        phone = "13900139000",
        role = role,
        userId = userId,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    beforeTest {
      adminRepository = mockk(relaxed = true)
      userService = mockk(relaxed = true)
      snowflakeIdGenerator = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      service =
        AdminServiceImpl(
          adminRepository,
          userService,
          BCryptPasswordEncoder(),
          snowflakeIdGenerator,
          bizLogger,
        )
    }
    test("findAll should delegate") {
      every { adminRepository.findAll() } returns listOf(admin())
      service.findAll().size shouldBe 1
    }
    test("findById should delegate") {
      every { adminRepository.findById(1L) } returns Optional.of(admin())
      service.findById(1L).shouldNotBeNull()
      every { adminRepository.findById(9L) } returns Optional.empty()
      service.findById(9L) shouldBe null
    }
    test("findByUserId and findByUsername should delegate") {
      every { adminRepository.findByUserId(1L) } returns admin()
      every { adminRepository.findByUsername("admin1") } returns admin()
      service.findByUserId(1L)?.id shouldBe 1L
      service.findByUsername("admin1")?.username shouldBe "admin1"
    }
    test("save should delegate") {
      val target = admin()
      every { adminRepository.save(target) } returns target
      service.save(target) shouldBe target
    }
    test("deleteById should return false when admin absent") {
      every { adminRepository.existsById(9L) } returns false
      service.deleteById(9L) shouldBe false
    }
    test("deleteById should delete and log when present") {
      every { adminRepository.existsById(1L) } returns true
      every { bizLogger.resolveOperator() } returns (null to null)
      service.deleteById(1L) shouldBe true
      verify { adminRepository.deleteById(1L) }
      verify { bizLogger.logUserOperation(1L, "ADMIN_DELETED", "SUCCESS", null, null, any()) }
    }
    test("existsByUsername and existsById and existsByPhone should delegate") {
      every { adminRepository.existsByUsername("admin1") } returns true
      every { adminRepository.existsById(1L) } returns true
      every { adminRepository.existsByPhone("139") } returns true
      service.existsByUsername("admin1") shouldBe true
      service.existsById(1L) shouldBe true
      service.existsByPhone("139") shouldBe true
    }
    test("findByRole and countByRole should delegate") {
      every { adminRepository.findByRole(AdminRoleModel.ADMIN) } returns listOf(admin())
      every { adminRepository.countByRole(AdminRoleModel.ADMIN) } returns 1L
      service.findByRole(AdminRoleModel.ADMIN).size shouldBe 1
      service.countByRole(AdminRoleModel.ADMIN) shouldBe 1L
    }
    test("findAllDeliveryCapable should delegate with delivery capable roles") {
      every { adminRepository.findAllByRoleIn(any()) } returns listOf(admin(role = AdminRoleModel.DELIVERY_WORKER))
      service.findAllDeliveryCapable().size shouldBe 1
    }
    test("findByOnlineStatus should filter to delivery capable roles") {
      every { adminRepository.findByOnlineStatus(DeliverWorkerModelStatus.ONLINE) } returns
        listOf(admin(role = AdminRoleModel.DELIVERY_WORKER), admin(role = AdminRoleModel.SUPER_ADMIN))
      val result = service.findByOnlineStatus(DeliverWorkerModelStatus.ONLINE)
      result.size shouldBe 1
      result[0].role shouldBe AdminRoleModel.DELIVERY_WORKER
    }
    test("createAdmin should throw when username blank for non-delivery role") {
      shouldThrow<BadRequestException> {
        service.createAdmin(
          username = null,
          password = "123456",
          realName = null,
          phone = null,
          role = AdminRoleModel.ADMIN,
          avatarUrl = null,
          onlineStatus = DeliverWorkerModelStatus.OFFLINE,
          isAvailable = true,
          coordinates = null,
          currentLocation = null,
          rating = null,
          earningCents = null,
          accountStatus = AdminAccountStatus.ACTIVE,
        )
      }
    }
    test("createAdmin should throw when username exists") {
      every { adminRepository.existsByUsername("admin1") } returns true
      shouldThrow<BadRequestException> {
        service.createAdmin(
          username = "admin1",
          password = "123456",
          realName = null,
          phone = null,
          role = AdminRoleModel.ADMIN,
          avatarUrl = null,
          onlineStatus = DeliverWorkerModelStatus.OFFLINE,
          isAvailable = true,
          coordinates = null,
          currentLocation = null,
          rating = null,
          earningCents = null,
          accountStatus = AdminAccountStatus.ACTIVE,
        )
      }
    }
    test("createAdmin should throw when phone exists") {
      every { adminRepository.existsByUsername("newadmin") } returns false
      every { adminRepository.existsByPhone("13900139000") } returns true
      shouldThrow<BadRequestException> {
        service.createAdmin(
          username = "newadmin",
          password = "123456",
          realName = null,
          phone = "13900139000",
          role = AdminRoleModel.ADMIN,
          avatarUrl = null,
          onlineStatus = DeliverWorkerModelStatus.OFFLINE,
          isAvailable = true,
          coordinates = null,
          currentLocation = null,
          rating = null,
          earningCents = null,
          accountStatus = AdminAccountStatus.ACTIVE,
        )
      }
    }
    test("createAdmin should create delivery worker with auto username") {
      every { snowflakeIdGenerator.nextId() } returns 42L
      every { adminRepository.existsByUsername("delivery_42") } returns false
      every { adminRepository.existsByUsername("delivery_100") } returns false
      every { adminRepository.save(any()) } answers { firstArg<AdminModel>().also { it.id = 100L } }
      every { bizLogger.resolveOperator() } returns (null to null)

      val created =
        service.createAdmin(
          username = null,
          password = "123456",
          realName = "送水工",
          phone = "13700137000",
          role = AdminRoleModel.DELIVERY_WORKER,
          avatarUrl = null,
          onlineStatus = DeliverWorkerModelStatus.OFFLINE,
          isAvailable = true,
          coordinates = null,
          currentLocation = null,
          rating = null,
          earningCents = null,
          accountStatus = AdminAccountStatus.ACTIVE,
        )

      created.username shouldBe "delivery_100"
      verify { adminRepository.save(created) }
    }
    test("createAdmin should create admin with provided username and default password") {
      every { adminRepository.existsByUsername("newadmin") } returns false
      every { adminRepository.save(any()) } answers { firstArg<AdminModel>().also { it.id = 200L } }
      every { bizLogger.resolveOperator() } returns (null to null)

      val created =
        service.createAdmin(
          username = "newadmin",
          password = null,
          realName = null,
          phone = null,
          role = AdminRoleModel.ADMIN,
          avatarUrl = null,
          onlineStatus = DeliverWorkerModelStatus.OFFLINE,
          isAvailable = true,
          coordinates = null,
          currentLocation = null,
          rating = null,
          earningCents = null,
          accountStatus = AdminAccountStatus.ACTIVE,
        )

      created.username shouldBe "newadmin"
      created.passwordHash.startsWith("$2") shouldBe true
    }
    test("updateAdmin should throw when admin not found") {
      every { adminRepository.findAdminById(9L) } returns null
      shouldThrow<BadRequestException> {
        service.updateAdmin(
          id = 9L,
          realName = null,
          phone = null,
          role = null,
          avatarUrl = null,
          onlineStatus = null,
          isAvailable = null,
          coordinates = null,
          currentLocation = null,
          rating = null,
          earningCents = null,
          accountStatus = null,
        )
      }
    }
    test("updateAdmin should throw when phone used by other admin") {
      val target = admin(1L)
      every { adminRepository.findAdminById(1L) } returns target
      every { adminRepository.findByPhone("138") } returns admin(2L)
      shouldThrow<BadRequestException> {
        service.updateAdmin(
          id = 1L,
          realName = null,
          phone = "138",
          role = null,
          avatarUrl = null,
          onlineStatus = null,
          isAvailable = null,
          coordinates = null,
          currentLocation = null,
          rating = null,
          earningCents = null,
          accountStatus = null,
        )
      }
    }
    test("updateAdmin should update fields and log") {
      val target = admin(1L)
      every { adminRepository.findAdminById(1L) } returns target
      every { adminRepository.findByPhone("13800138001") } returns null
      every { adminRepository.save(any()) } answers { firstArg() }
      every { bizLogger.resolveOperator() } returns (null to null)

      val updated =
        service.updateAdmin(
          id = 1L,
          realName = "新名字",
          phone = "13800138001",
          role = AdminRoleModel.DELIVERY_WORKER,
          avatarUrl = "http://a",
          onlineStatus = DeliverWorkerModelStatus.ONLINE,
          isAvailable = false,
          coordinates = "1,2",
          currentLocation = "深圳",
          rating = 4.5,
          earningCents = 100L,
          accountStatus = AdminAccountStatus.DISABLED,
        )

      updated.realName shouldBe "新名字"
      updated.role shouldBe AdminRoleModel.DELIVERY_WORKER
      updated.accountStatus shouldBe AdminAccountStatus.DISABLED
      updated.rating shouldBe 4.5
      verify { bizLogger.logUserOperation(1L, "ADMIN_UPDATED", "SUCCESS", null, null, any()) }
    }
    test("getUserById should return linked user") {
      every { adminRepository.findById(1L) } returns Optional.of(admin(1L, userId = 5L))
      every { userService.findById(5L) } returns UserModel(wechatOpenId = "openid", email = "e@e.com")
      val user = service.getUserById(1L)
      user.wechatOpenId shouldBe "openid"
    }
    test("getUserById should throw when admin missing") {
      every { adminRepository.findById(1L) } returns Optional.empty()
      shouldThrow<UserNotFoundException> { service.getUserById(1L) }
    }
    test("getUserById should throw when admin has no user id") {
      every { adminRepository.findById(1L) } returns Optional.of(admin(1L, userId = null))
      shouldThrow<UserNotFoundException> { service.getUserById(1L) }
    }
    test("getUserById should throw when linked user missing") {
      every { adminRepository.findById(1L) } returns Optional.of(admin(1L, userId = 5L))
      every { userService.findById(5L) } returns null
      shouldThrow<UserNotFoundException> { service.getUserById(1L) }
    }
  })
