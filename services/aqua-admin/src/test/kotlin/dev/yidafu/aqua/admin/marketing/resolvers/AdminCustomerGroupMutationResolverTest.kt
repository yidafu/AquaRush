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

package dev.yidafu.aqua.admin.marketing.resolvers

import dev.yidafu.aqua.api.service.marketing.CustomerGroupMutationService
import dev.yidafu.aqua.api.service.marketing.CustomerGroupQueryService
import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.common.graphql.generated.CreateCustomerGroupInput
import dev.yidafu.aqua.common.graphql.generated.UpdateCustomerGroupInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminCustomerGroupMutationResolverTest {
  private lateinit var customerGroupMutationService: CustomerGroupMutationService
  private lateinit var customerGroupQueryService: CustomerGroupQueryService
  private lateinit var resolver: AdminCustomerGroupMutationResolver

  private fun customerGroup(): CustomerGroupModel = CustomerGroupModel(id = 10L, name = "老客户")

  private fun principal(): UserPrincipal =
    UserPrincipal(
      id = 1L,
      _username = "admin",
      userType = "ADMIN",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
    )

  @BeforeEach
  fun setUp() {
    customerGroupMutationService = mockk(relaxed = true)
    customerGroupQueryService = mockk(relaxed = true)
    resolver = AdminCustomerGroupMutationResolver(customerGroupMutationService, customerGroupQueryService)
  }

  @Test
  fun `createCustomerGroup should delegate with member count`() {
    // Given
    every { customerGroupMutationService.createGroup(1L, "admin", any()) } returns customerGroup()
    every { customerGroupQueryService.memberCount(10L) } returns 5L

    // When
    val result = resolver.createCustomerGroup(CreateCustomerGroupInput(name = "老客户"), principal())

    // Then
    assertEquals("老客户", result.name)
    assertEquals(5, result.memberCount)
    verify { customerGroupMutationService.createGroup(1L, "admin", any()) }
  }

  @Test
  fun `updateCustomerGroup should delegate`() {
    // Given
    every { customerGroupMutationService.updateGroup(1L, "admin", 10L, any()) } returns customerGroup()
    every { customerGroupQueryService.memberCount(10L) } returns 0L

    // When
    val result = resolver.updateCustomerGroup(10L, UpdateCustomerGroupInput(name = "老客户"), principal())

    // Then
    assertEquals("老客户", result.name)
    verify { customerGroupMutationService.updateGroup(1L, "admin", 10L, any()) }
  }

  @Test
  fun `deleteCustomerGroup should delegate`() {
    // Given
    every { customerGroupMutationService.deleteGroup(1L, "admin", 10L) } returns true

    // When
    val result = resolver.deleteCustomerGroup(10L, principal())

    // Then
    assertTrue(result)
    verify { customerGroupMutationService.deleteGroup(1L, "admin", 10L) }
  }

  @Test
  fun `setUserCustomerGroup should delegate and return true`() {
    // Given
    every { customerGroupMutationService.setUserGroup(1L, "admin", 5L, 10L) } returns null

    // When
    val result = resolver.setUserCustomerGroup(5L, 10L, principal())

    // Then
    assertTrue(result)
    verify { customerGroupMutationService.setUserGroup(1L, "admin", 5L, 10L) }
  }
}
