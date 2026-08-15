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

package dev.yidafu.aqua.delivery.service.impl

import dev.yidafu.aqua.common.domain.model.DeliveryAreaModel
import dev.yidafu.aqua.delivery.domain.repository.DeliveryAreaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 配送区域查询服务单元测试。
 */
class DeliveryAreaQueryServiceImplTest {
  private lateinit var areaRepository: DeliveryAreaRepository
  private lateinit var service: DeliveryAreaQueryServiceImpl

  private fun area(
    id: Long,
    enabled: Boolean = true,
  ) = DeliveryAreaModel(id = id, name = "区域$id", province = "浙江省", city = "杭州市", district = "西湖区", enabled = enabled)

  @BeforeEach
  fun setUp() {
    areaRepository = mockk(relaxed = true)
    service = DeliveryAreaQueryServiceImpl(areaRepository)
  }

  @Test
  @DisplayName("isAddressInDeliveryArea 当前恒为 true")
  fun `isAddressInDeliveryArea always true`() {
    assertTrue(service.isAddressInDeliveryArea("浙江省", "杭州市", "西湖区"))
  }

  @Test
  @DisplayName("validateDeliveryAddress 不抛异常")
  fun `validateDeliveryAddress does not throw`() {
    service.validateDeliveryAddress("浙江省", "杭州市", "西湖区")
  }

  @Test
  @DisplayName("getAllDeliveryAreas / getEnabledDeliveryAreas 透传")
  fun `list queries pass through`() {
    val all = listOf(area(1L), area(2L, enabled = false))
    val enabled = listOf(area(1L))
    every { areaRepository.findAll() } returns all
    every { areaRepository.findByEnabledTrue() } returns enabled

    assertEquals(all, service.getAllDeliveryAreas())
    assertEquals(enabled, service.getEnabledDeliveryAreas())
    verify(exactly = 1) { areaRepository.findAll() }
    verify(exactly = 1) { areaRepository.findByEnabledTrue() }
  }
}
