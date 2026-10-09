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

import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.marketing.customer.domain.repository.CustomerGroupRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.util.Optional

/** 客户分组查询服务单元测试(F-MKT-001 列表/详情/成员数)。 */
class CustomerGroupQueryServiceImplTest :
  FunSpec({
    var customerGroupRepository: CustomerGroupRepository = mockk()
    var userQueryApiService: UserQueryApiService = mockk()
    var service: CustomerGroupQueryServiceImpl = mockk()

    beforeTest {
      customerGroupRepository = mockk()
      userQueryApiService = mockk()
      service = CustomerGroupQueryServiceImpl(customerGroupRepository, userQueryApiService)
    }
    test("listGroups without keyword") {
      val pageable: Pageable = PageRequest.of(0, 10)
      every { customerGroupRepository.searchGroups(null, pageable) } returns PageImpl(listOf(CustomerGroupModel(id = 1L, name = "熟客")))

      val result = service.listGroups(null, pageable)

      result.totalElements shouldBe 1
      verify { customerGroupRepository.searchGroups(null, pageable) }
    }
    test("listGroups with keyword") {
      val pageable: Pageable = PageRequest.of(0, 10)
      every { customerGroupRepository.searchGroups("客", pageable) } returns PageImpl(emptyList())

      val result = service.listGroups("客", pageable)

      result.totalElements shouldBe 0
    }
    test("getGroup found and missing") {
      every { customerGroupRepository.findById(1L) } returns Optional.of(CustomerGroupModel(id = 1L, name = "熟客"))
      every { customerGroupRepository.findById(99L) } returns Optional.empty()

      service.getGroup(1L)?.id shouldBe 1L
      service.getGroup(99L).shouldBeNull()
    }
    test("findByName delegates") {
      every { customerGroupRepository.findByName("熟客") } returns CustomerGroupModel(id = 1L, name = "熟客")

      service.findByName("熟客")?.name shouldBe "熟客"
    }
    test("memberCount delegates") {
      every { userQueryApiService.countUsersByCustomerGroupId(1L) } returns 8

      service.memberCount(1L) shouldBe 8
    }
  })
