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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

/**
 * 配送区域变更服务单元测试:创建/启停并记录业务日志。
 */
class DeliveryAreaMutationServiceImplTest :
  FunSpec({
    var areaRepository: DeliveryAreaRepository = mockk()
    var bizLogger: BizLogger = mockk()
    var service: DeliveryAreaMutationServiceImpl = mockk()

    fun area(
      id: Long? = 1L,
      enabled: Boolean = true,
    ) = DeliveryAreaModel(id = id, name = "西湖区", province = "浙江省", city = "杭州市", district = "西湖区", enabled = enabled)

    beforeTest {
      areaRepository = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      every { bizLogger.resolveOperator() } returns (1L to "admin")
      service = DeliveryAreaMutationServiceImpl(areaRepository, bizLogger)
    }
    test("createDeliveryArea saves and logs") {
      val input = area(id = null)
      every { areaRepository.save(any()) } answers { firstArg<DeliveryAreaModel>().also { it.id = 9L } }

      val saved = service.createDeliveryArea(input)

      saved.id shouldBe 9L
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
    test("updateDeliveryArea toggles enabled and logs") {
      val existing = area(id = 1L, enabled = true)
      every { areaRepository.findById(1L) } returns Optional.of(existing)
      every { areaRepository.save(any()) } answers { firstArg() }

      val updated = service.updateDeliveryArea(1L, false)

      updated.enabled shouldBe false
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
    test("updateDeliveryArea throws when missing") {
      every { areaRepository.findById(1L) } returns Optional.empty()

      shouldThrow<NotFoundException> { service.updateDeliveryArea(1L, true) }
    }
    test("updateDeliveryArea re-enables area") {
      val existing = area(id = 1L, enabled = false)
      every { areaRepository.findById(1L) } returns Optional.of(existing)
      every { areaRepository.save(any()) } answers { firstArg() }

      val updated = service.updateDeliveryArea(1L, true)

      updated.enabled shouldBe true
    }
  })
