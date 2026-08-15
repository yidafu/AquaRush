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

package dev.yidafu.aqua.marketing.customer.service.impl

import dev.yidafu.aqua.api.dto.CustomerGroupCreateRequest
import dev.yidafu.aqua.api.dto.CustomerGroupUpdateRequest
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.customer.domain.exception.CustomerGroupNotFoundException
import dev.yidafu.aqua.marketing.customer.domain.exception.DuplicateGroupNameException
import dev.yidafu.aqua.marketing.customer.domain.repository.CustomerGroupRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 客户分组变更服务单元测试(F-MKT-001):名称唯一、成员同步、删除回退未分组。 */
class CustomerGroupMutationServiceImplTest {
  private lateinit var customerGroupRepository: CustomerGroupRepository
  private lateinit var userQueryApiService: UserQueryApiService
  private lateinit var userService: UserService
  private lateinit var bizLogger: BizLogger
  private lateinit var service: CustomerGroupMutationServiceImpl

  @BeforeEach
  fun setUp() {
    customerGroupRepository = mockk()
    userQueryApiService = mockk()
    userService = mockk(relaxed = true)
    bizLogger = mockk(relaxed = true)
    service = CustomerGroupMutationServiceImpl(customerGroupRepository, userQueryApiService, userService, bizLogger)
  }

  private fun group(
    id: Long = 1L,
    name: String = "熟客",
  ): CustomerGroupModel =
    CustomerGroupModel(
      id = id,
      name = name,
    )

  private fun user(
    id: Long,
    groupId: Long? = null,
  ): UserModel =
    UserModel(
      id = id,
      wechatOpenId = "wx$id",
      email = "u$id@x.com",
      customerGroupId = groupId,
    )

  @Test
  @DisplayName("建组:名称唯一,初始成员即时划入(AC-MKT-001-01)")
  fun `createGroup with initial members`() {
    every { customerGroupRepository.existsByName("熟客") } returns false
    every { customerGroupRepository.save(any()) } answers { (firstArg() as CustomerGroupModel).apply { id = 1L } }

    val saved = service.createGroup(1L, "admin", CustomerGroupCreateRequest(name = "熟客", memberIds = listOf(10L, 10L, 11L)))

    assertEquals(1L, saved.id)
    assertEquals("熟客", saved.name)
    verify { userService.setCustomerGroup(10L, 1L) }
    verify { userService.setCustomerGroup(11L, 1L) }
    verify(exactly = 2) { userService.setCustomerGroup(any(), any()) }
    verify {
      bizLogger.logMarketingOperation(
        targetId = 1L,
        operation = "CUSTOMER_GROUP_CREATED",
        result = "SUCCESS",
        userId = 1L,
        username = "admin",
        additionalData = any(),
      )
    }
  }

  @Test
  @DisplayName("建组:无初始成员")
  fun `createGroup without members`() {
    every { customerGroupRepository.existsByName("新组") } returns false
    every { customerGroupRepository.save(any()) } answers { (firstArg() as CustomerGroupModel).apply { id = 2L } }

    val saved = service.createGroup(1L, "admin", CustomerGroupCreateRequest(name = "新组"))

    assertEquals(2L, saved.id)
    verify(exactly = 0) { userService.setCustomerGroup(any(), any()) }
  }

  @Test
  @DisplayName("建组:名称重复 → DuplicateGroupNameException")
  fun `createGroup duplicate name throws`() {
    every { customerGroupRepository.existsByName("熟客") } returns true

    assertFailsWith<DuplicateGroupNameException> {
      service.createGroup(1L, "admin", CustomerGroupCreateRequest(name = "熟客"))
    }
  }

  @Test
  @DisplayName("改组:覆盖式成员同步(新增+移除)")
  fun `updateGroup syncs members`() {
    every { customerGroupRepository.findById(1L) } returns Optional.of(group())
    every { customerGroupRepository.findByName("新名") } returns null
    every { customerGroupRepository.save(any()) } answers { firstArg() }
    every { userQueryApiService.findByCustomerGroupId(1L) } returns listOf(user(10L, 1L), user(20L, 1L))

    val saved = service.updateGroup(1L, "admin", 1L, CustomerGroupUpdateRequest(name = "新名", memberIds = listOf(10L, 30L)))

    assertEquals("新名", saved.name)
    verify { userService.setCustomerGroup(30L, 1L) }
    verify { userService.setCustomerGroup(20L, null) }
    verify(exactly = 0) { userService.setCustomerGroup(10L, any()) }
  }

  @Test
  @DisplayName("改组:memberIds=null 不调整成员")
  fun `updateGroup without memberIds keeps members`() {
    every { customerGroupRepository.findById(1L) } returns Optional.of(group())
    every { customerGroupRepository.findByName("新名") } returns null
    every { customerGroupRepository.save(any()) } answers { firstArg() }

    service.updateGroup(1L, "admin", 1L, CustomerGroupUpdateRequest(name = "新名"))

    verify(exactly = 0) { userQueryApiService.findByCustomerGroupId(any()) }
    verify(exactly = 0) { userService.setCustomerGroup(any(), any()) }
  }

  @Test
  @DisplayName("改组:memberIds 空数组清空成员")
  fun `updateGroup empty memberIds clears members`() {
    every { customerGroupRepository.findById(1L) } returns Optional.of(group())
    every { customerGroupRepository.findByName("新名") } returns null
    every { customerGroupRepository.save(any()) } answers { firstArg() }
    every { userQueryApiService.findByCustomerGroupId(1L) } returns listOf(user(10L, 1L))

    service.updateGroup(1L, "admin", 1L, CustomerGroupUpdateRequest(name = "新名", memberIds = emptyList()))

    verify { userService.setCustomerGroup(10L, null) }
  }

  @Test
  @DisplayName("改组:他组同名 → DuplicateGroupNameException")
  fun `updateGroup duplicate name throws`() {
    every { customerGroupRepository.findById(1L) } returns Optional.of(group())
    every { customerGroupRepository.findByName("熟客") } returns group(id = 2L, name = "熟客")

    assertFailsWith<DuplicateGroupNameException> {
      service.updateGroup(1L, "admin", 1L, CustomerGroupUpdateRequest(name = "熟客"))
    }
  }

  @Test
  @DisplayName("改组:自身同名不冲突")
  fun `updateGroup same name on self ok`() {
    every { customerGroupRepository.findById(1L) } returns Optional.of(group())
    every { customerGroupRepository.findByName("熟客") } returns group(id = 1L, name = "熟客")
    every { customerGroupRepository.save(any()) } answers { firstArg() }

    val saved = service.updateGroup(1L, "admin", 1L, CustomerGroupUpdateRequest(name = "熟客"))

    assertEquals("熟客", saved.name)
  }

  @Test
  @DisplayName("改组:分组不存在 → CustomerGroupNotFoundException")
  fun `updateGroup not found throws`() {
    every { customerGroupRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<CustomerGroupNotFoundException> {
      service.updateGroup(1L, "admin", 99L, CustomerGroupUpdateRequest(name = "x"))
    }
  }

  @Test
  @DisplayName("删组:成员回未分组并软删除(AC-MKT-001-03)")
  fun `deleteGroup resets members`() {
    every { customerGroupRepository.findById(1L) } returns Optional.of(group())
    every { userQueryApiService.findByCustomerGroupId(1L) } returns listOf(user(10L, 1L), user(20L, 1L))
    every { customerGroupRepository.delete(any<CustomerGroupModel>()) } returns Unit

    val deleted = service.deleteGroup(1L, "admin", 1L)

    assertTrue(deleted)
    verify { userService.setCustomerGroup(10L, null) }
    verify { userService.setCustomerGroup(20L, null) }
    verify { customerGroupRepository.delete(any<CustomerGroupModel>()) }
  }

  @Test
  @DisplayName("删组:分组不存在 → CustomerGroupNotFoundException")
  fun `deleteGroup not found throws`() {
    every { customerGroupRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<CustomerGroupNotFoundException> {
      service.deleteGroup(1L, "admin", 99L)
    }
  }

  @Test
  @DisplayName("设组:设置用户分组并返回分组")
  fun `setUserGroup sets group`() {
    every { customerGroupRepository.existsById(1L) } returns true
    every { customerGroupRepository.findById(1L) } returns Optional.of(group())
    every { userService.setCustomerGroup(10L, 1L) } returns user(10L, 1L)

    val result = service.setUserGroup(1L, "admin", 10L, 1L)

    assertEquals(1L, result?.id)
    verify { userService.setCustomerGroup(10L, 1L) }
  }

  @Test
  @DisplayName("设组:移除分组(customerGroupId=null)返回 null")
  fun `setUserGroup unsets group`() {
    every { userService.setCustomerGroup(10L, null) } returns user(10L)

    val result = service.setUserGroup(1L, "admin", 10L, null)

    assertNull(result)
    verify { userService.setCustomerGroup(10L, null) }
  }

  @Test
  @DisplayName("设组:分组不存在 → CustomerGroupNotFoundException")
  fun `setUserGroup missing group throws`() {
    every { customerGroupRepository.existsById(99L) } returns false

    assertFailsWith<CustomerGroupNotFoundException> {
      service.setUserGroup(1L, "admin", 10L, 99L)
    }
  }
}
