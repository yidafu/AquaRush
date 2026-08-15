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

import dev.yidafu.aqua.api.service.marketing.CustomerGroupQueryService
import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.common.graphql.generated.CustomerGroupListInput
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdminCustomerGroupQueryResolverTest {
  private lateinit var customerGroupQueryService: CustomerGroupQueryService
  private lateinit var resolver: AdminCustomerGroupQueryResolver

  @BeforeEach
  fun setUp() {
    customerGroupQueryService = mockk(relaxed = true)
    resolver = AdminCustomerGroupQueryResolver(customerGroupQueryService)
  }

  @Test
  fun `customerGroups should list with member counts`() {
    // Given
    every { customerGroupQueryService.listGroups("老", PageRequest.of(0, 20)) } returns
      PageImpl(listOf(CustomerGroupModel(id = 10L, name = "老客户")))
    every { customerGroupQueryService.memberCount(10L) } returns 5L

    // When
    val result = resolver.customerGroups(CustomerGroupListInput(keyword = "老"))

    // Then
    assertEquals(1, result.content.size)
    assertEquals(5, result.content[0].memberCount)
    verify { customerGroupQueryService.listGroups("老", PageRequest.of(0, 20)) }
  }

  @Test
  fun `customerGroup should throw when missing`() {
    // Given
    every { customerGroupQueryService.getGroup(999L) } returns null

    // When & Then
    assertFailsWith<IllegalArgumentException> { resolver.customerGroup(999L) }
  }

  @Test
  fun `customerGroup should map group with member count`() {
    // Given
    every { customerGroupQueryService.getGroup(10L) } returns CustomerGroupModel(id = 10L, name = "老客户")
    every { customerGroupQueryService.memberCount(10L) } returns 3L

    // When
    val result = resolver.customerGroup(10L)

    // Then
    assertEquals("老客户", result!!.name)
    assertEquals(3, result.memberCount)
  }
}
