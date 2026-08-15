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
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.delivery.domain.repository.DeliveryAreaRepository
import dev.yidafu.aqua.logging.util.BizLogger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 配送区域变更服务单元测试:创建/启停并记录业务日志。
 */
class DeliveryAreaMutationServiceImplTest {
  private lateinit var areaRepository: DeliveryAreaRepository
  private lateinit var bizLogger: BizLogger
  private lateinit var service: DeliveryAreaMutationServiceImpl

  @BeforeEach
  fun setUp() {
    areaRepository = mockk(relaxed = true)
    bizLogger = mockk(relaxed = true)
    every { bizLogger.resolveOperator() } returns (1L to "admin")
    service = DeliveryAreaMutationServiceImpl(areaRepository, bizLogger)
  }

  private fun area(
    id: Long? = 1L,
    enabled: Boolean = true,
  ) = DeliveryAreaModel(id = id, name = "西湖区", province = "浙江省", city = "杭州市", district = "西湖区", enabled = enabled)

  @Test
  @DisplayName("createDeliveryArea 落库并记录创建日志")
  fun `createDeliveryArea saves and logs`() {
    val input = area(id = null)
    every { areaRepository.save(any()) } answers { firstArg<DeliveryAreaModel>().also { it.id = 9L } }

    val saved = service.createDeliveryArea(input)

    assertEquals(9L, saved.id)
    verify(exactly = 1) {
      bizLogger.logDeliveryOperation(
        deliveryId = 9L,
        operation = "DELIVERY_AREA_CREATED",
        result = "SUCCESS",
        userId = 1L,
        username = "admin",
        additionalData = mapOf("province" to "浙江省", "city" to "杭州市", "district" to "西湖区"),
      )
    }
  }

  @Test
  @DisplayName("updateDeliveryArea 更新启用状态并记录日志")
  fun `updateDeliveryArea toggles enabled and logs`() {
    val existing = area(id = 1L, enabled = true)
    every { areaRepository.findById(1L) } returns Optional.of(existing)
    every { areaRepository.save(any()) } answers { firstArg() }

    val updated = service.updateDeliveryArea(1L, false)

    assertFalse(updated.enabled)
    verify(exactly = 1) {
      bizLogger.logDeliveryOperation(
        deliveryId = 1L,
        operation = "DELIVERY_AREA_UPDATED",
        result = "SUCCESS",
        userId = 1L,
        username = "admin",
        additionalData = mapOf("from" to true, "to" to false),
      )
    }
  }

  @Test
  @DisplayName("updateDeliveryArea 区域不存在抛 NotFoundException")
  fun `updateDeliveryArea throws when missing`() {
    every { areaRepository.findById(1L) } returns Optional.empty()

    assertFailsWith<NotFoundException> { service.updateDeliveryArea(1L, true) }
  }

  @Test
  @DisplayName("updateDeliveryArea 重新启用区域")
  fun `updateDeliveryArea re-enables area`() {
    val existing = area(id = 1L, enabled = false)
    every { areaRepository.findById(1L) } returns Optional.of(existing)
    every { areaRepository.save(any()) } answers { firstArg() }

    val updated = service.updateDeliveryArea(1L, true)

    assertTrue(updated.enabled)
  }
}
