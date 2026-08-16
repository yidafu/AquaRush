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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class AdminCustomerGroupQueryResolverTest :
  FunSpec({
    var customerGroupQueryService: CustomerGroupQueryService = mockk()
    var resolver: AdminCustomerGroupQueryResolver = mockk()

    beforeTest {
      customerGroupQueryService = mockk(relaxed = true)
      resolver = AdminCustomerGroupQueryResolver(customerGroupQueryService)
    }
    test("customerGroups should list with member counts") {
      // Given
      every { customerGroupQueryService.listGroups("老", PageRequest.of(0, 20)) } returns
        PageImpl(listOf(CustomerGroupModel(id = 10L, name = "老客户")))
      every { customerGroupQueryService.memberCount(10L) } returns 5L

      // When
      val result = resolver.customerGroups(CustomerGroupListInput(keyword = "老"))

      // Then
      result.content.size shouldBe 1
      result.content[0].memberCount shouldBe 5
      verify { customerGroupQueryService.listGroups("老", PageRequest.of(0, 20)) }
    }
    test("customerGroup should throw when missing") {
      // Given
      every { customerGroupQueryService.getGroup(999L) } returns null

      // When & Then
      shouldThrow<IllegalArgumentException> { resolver.customerGroup(999L) }
    }
    test("customerGroup should map group with member count") {
      // Given
      every { customerGroupQueryService.getGroup(10L) } returns CustomerGroupModel(id = 10L, name = "老客户")
      every { customerGroupQueryService.memberCount(10L) } returns 3L

      // When
      val result = resolver.customerGroup(10L)

      // Then
      result!!.name shouldBe "老客户"
      result.memberCount shouldBe 3
    }
  })
