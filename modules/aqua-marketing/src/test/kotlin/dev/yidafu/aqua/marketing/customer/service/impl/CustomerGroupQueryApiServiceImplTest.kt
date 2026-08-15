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

import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.marketing.customer.domain.repository.CustomerGroupRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 客户分组跨模块查询服务单元测试(用户列表/定向发放/营销引擎数据源)。 */
class CustomerGroupQueryApiServiceImplTest {
  private lateinit var customerGroupRepository: CustomerGroupRepository
  private lateinit var service: CustomerGroupQueryApiServiceImpl

  @BeforeEach
  fun setUp() {
    customerGroupRepository = mockk()
    service = CustomerGroupQueryApiServiceImpl(customerGroupRepository)
  }

  @Test
  @DisplayName("按 id 查询:存在/不存在")
  fun `findById`() {
    every { customerGroupRepository.findById(1L) } returns Optional.of(CustomerGroupModel(id = 1L, name = "熟客"))
    every { customerGroupRepository.findById(99L) } returns Optional.empty()

    assertEquals(1L, service.findById(1L)?.id)
    assertNull(service.findById(99L))
  }

  @Test
  @DisplayName("按 id 列表查询")
  fun `findByIds`() {
    every { customerGroupRepository.findAllById(listOf(1L, 2L)) } returns listOf(CustomerGroupModel(id = 1L, name = "A"))

    val result = service.findByIds(listOf(1L, 2L))

    assertEquals(1, result.size)
  }

  @Test
  @DisplayName("全量查询")
  fun `findAll`() {
    every { customerGroupRepository.findAll() } returns
      listOf(CustomerGroupModel(id = 1L, name = "A"), CustomerGroupModel(id = 2L, name = "B"))

    assertEquals(2, service.findAll().size)
  }
}
