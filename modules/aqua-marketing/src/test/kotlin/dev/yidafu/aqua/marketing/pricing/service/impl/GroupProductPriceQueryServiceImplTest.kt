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

package dev.yidafu.aqua.marketing.pricing.service.impl

import dev.yidafu.aqua.common.domain.model.GroupProductPriceModel
import dev.yidafu.aqua.marketing.pricing.domain.repository.GroupProductPriceRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 分组商品专属价查询服务单元测试(F-MKT-002 列表/详情)。 */
class GroupProductPriceQueryServiceImplTest {
  private lateinit var groupProductPriceRepository: GroupProductPriceRepository
  private lateinit var service: GroupProductPriceQueryServiceImpl

  @BeforeEach
  fun setUp() {
    groupProductPriceRepository = mockk()
    service = GroupProductPriceQueryServiceImpl(groupProductPriceRepository)
  }

  @Test
  @DisplayName("分组价格列表(分页)")
  fun `listByGroup`() {
    val pageable: Pageable = PageRequest.of(0, 10)
    val page: Page<GroupProductPriceModel> =
      PageImpl(listOf(GroupProductPriceModel(id = 1L, customerGroupId = 5L, productId = 1L, priceCents = 1500)))
    every { groupProductPriceRepository.findByCustomerGroupId(5L, pageable) } returns page

    val result = service.listByGroup(5L, pageable)

    assertEquals(1, result.totalElements)
    assertEquals(1500, result.content[0].priceCents)
  }

  @Test
  @DisplayName("分组×商品详情:存在/不存在")
  fun `getByGroupAndProduct`() {
    every { groupProductPriceRepository.findByCustomerGroupIdAndProductId(5L, 1L) } returns
      GroupProductPriceModel(id = 1L, customerGroupId = 5L, productId = 1L, priceCents = 1500)
    every { groupProductPriceRepository.findByCustomerGroupIdAndProductId(5L, 2L) } returns null

    assertEquals(1500, service.getByGroupAndProduct(5L, 1L)?.priceCents)
    assertNull(service.getByGroupAndProduct(5L, 2L))
  }
}
