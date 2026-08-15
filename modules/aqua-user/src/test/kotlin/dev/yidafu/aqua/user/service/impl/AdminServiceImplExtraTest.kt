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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 补充 AdminServiceImplTest 未覆盖的增删改查/创建/更新分支。
 */
class AdminServiceImplExtraTest {
  private lateinit var adminRepository: AdminRepository
  private lateinit var userService: UserService
  private lateinit var snowflakeIdGenerator: SnowflakeIdGenerator
  private lateinit var bizLogger: BizLogger
  private lateinit var service: AdminServiceImpl

  @BeforeEach
  fun setUp() {
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

  private fun admin(
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

  @Test
  fun `findAll should delegate`() {
    every { adminRepository.findAll() } returns listOf(admin())
    assertEquals(1, service.findAll().size)
  }

  @Test
  fun `findById should delegate`() {
    every { adminRepository.findById(1L) } returns Optional.of(admin())
    assertNotNull(service.findById(1L))
    every { adminRepository.findById(9L) } returns Optional.empty()
    assertEquals(null, service.findById(9L))
  }

  @Test
  fun `findByUserId and findByUsername should delegate`() {
    every { adminRepository.findByUserId(1L) } returns admin()
    every { adminRepository.findByUsername("admin1") } returns admin()
    assertEquals(1L, service.findByUserId(1L)?.id)
    assertEquals("admin1", service.findByUsername("admin1")?.username)
  }

  @Test
  fun `save should delegate`() {
    val target = admin()
    every { adminRepository.save(target) } returns target
    assertEquals(target, service.save(target))
  }

  @Test
  fun `deleteById should return false when admin absent`() {
    every { adminRepository.existsById(9L) } returns false
    assertFalse(service.deleteById(9L))
  }

  @Test
  fun `deleteById should delete and log when present`() {
    every { adminRepository.existsById(1L) } returns true
    every { bizLogger.resolveOperator() } returns (null to null)
    assertTrue(service.deleteById(1L))
    verify { adminRepository.deleteById(1L) }
    verify { bizLogger.logUserOperation(1L, "ADMIN_DELETED", "SUCCESS", null, null, any()) }
  }

  @Test
  fun `existsByUsername and existsById and existsByPhone should delegate`() {
    every { adminRepository.existsByUsername("admin1") } returns true
    every { adminRepository.existsById(1L) } returns true
    every { adminRepository.existsByPhone("139") } returns true
    assertTrue(service.existsByUsername("admin1"))
    assertTrue(service.existsById(1L))
    assertTrue(service.existsByPhone("139"))
  }

  @Test
  fun `findByRole and countByRole should delegate`() {
    every { adminRepository.findByRole(AdminRoleModel.ADMIN) } returns listOf(admin())
    every { adminRepository.countByRole(AdminRoleModel.ADMIN) } returns 1L
    assertEquals(1, service.findByRole(AdminRoleModel.ADMIN).size)
    assertEquals(1L, service.countByRole(AdminRoleModel.ADMIN))
  }

  @Test
  fun `findAllDeliveryCapable should delegate with delivery capable roles`() {
    every { adminRepository.findAllByRoleIn(any()) } returns listOf(admin(role = AdminRoleModel.DELIVERY_WORKER))
    assertEquals(1, service.findAllDeliveryCapable().size)
  }

  @Test
  fun `findByOnlineStatus should filter to delivery capable roles`() {
    every { adminRepository.findByOnlineStatus(DeliverWorkerModelStatus.ONLINE) } returns
      listOf(admin(role = AdminRoleModel.DELIVERY_WORKER), admin(role = AdminRoleModel.SUPER_ADMIN))
    val result = service.findByOnlineStatus(DeliverWorkerModelStatus.ONLINE)
    assertEquals(1, result.size)
    assertEquals(AdminRoleModel.DELIVERY_WORKER, result[0].role)
  }

  @Test
  fun `createAdmin should throw when username blank for non-delivery role`() {
    assertThrows<BadRequestException> {
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

  @Test
  fun `createAdmin should throw when username exists`() {
    every { adminRepository.existsByUsername("admin1") } returns true
    assertThrows<BadRequestException> {
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

  @Test
  fun `createAdmin should throw when phone exists`() {
    every { adminRepository.existsByUsername("newadmin") } returns false
    every { adminRepository.existsByPhone("13900139000") } returns true
    assertThrows<BadRequestException> {
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

  @Test
  fun `createAdmin should create delivery worker with auto username`() {
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

    assertEquals("delivery_100", created.username)
    verify { adminRepository.save(created) }
  }

  @Test
  fun `createAdmin should create admin with provided username and default password`() {
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

    assertEquals("newadmin", created.username)
    assertTrue(created.passwordHash.startsWith("$2"))
  }

  @Test
  fun `updateAdmin should throw when admin not found`() {
    every { adminRepository.findAdminById(9L) } returns null
    assertThrows<BadRequestException> {
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

  @Test
  fun `updateAdmin should throw when phone used by other admin`() {
    val target = admin(1L)
    every { adminRepository.findAdminById(1L) } returns target
    every { adminRepository.findByPhone("138") } returns admin(2L)
    assertThrows<BadRequestException> {
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

  @Test
  fun `updateAdmin should update fields and log`() {
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

    assertEquals("新名字", updated.realName)
    assertEquals(AdminRoleModel.DELIVERY_WORKER, updated.role)
    assertEquals(AdminAccountStatus.DISABLED, updated.accountStatus)
    assertEquals(4.5, updated.rating)
    verify { bizLogger.logUserOperation(1L, "ADMIN_UPDATED", "SUCCESS", null, null, any()) }
  }

  @Test
  fun `getUserById should return linked user`() {
    every { adminRepository.findById(1L) } returns Optional.of(admin(1L, userId = 5L))
    every { userService.findById(5L) } returns UserModel(wechatOpenId = "openid", email = "e@e.com")
    val user = service.getUserById(1L)
    assertEquals("openid", user.wechatOpenId)
  }

  @Test
  fun `getUserById should throw when admin missing`() {
    every { adminRepository.findById(1L) } returns Optional.empty()
    assertThrows<UserNotFoundException> { service.getUserById(1L) }
  }

  @Test
  fun `getUserById should throw when admin has no user id`() {
    every { adminRepository.findById(1L) } returns Optional.of(admin(1L, userId = null))
    assertThrows<UserNotFoundException> { service.getUserById(1L) }
  }

  @Test
  fun `getUserById should throw when linked user missing`() {
    every { adminRepository.findById(1L) } returns Optional.of(admin(1L, userId = 5L))
    every { userService.findById(5L) } returns null
    assertThrows<UserNotFoundException> { service.getUserById(1L) }
  }
}
