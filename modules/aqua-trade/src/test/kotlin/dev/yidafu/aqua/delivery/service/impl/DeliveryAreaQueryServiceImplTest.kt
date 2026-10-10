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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * 配送区域查询服务单元测试。
 */
class DeliveryAreaQueryServiceImplTest :
  FunSpec({
    var areaRepository: DeliveryAreaRepository = mockk()
    var service: DeliveryAreaQueryServiceImpl = mockk()

    fun area(
      id: Long,
      enabled: Boolean = true,
    ) = DeliveryAreaModel(id = id, name = "区域$id", province = "浙江省", city = "杭州市", district = "西湖区", enabled = enabled)

    beforeTest {
      areaRepository = mockk(relaxed = true)
      service = DeliveryAreaQueryServiceImpl(areaRepository)
    }
    test("isAddressInDeliveryArea always true") {
      service.isAddressInDeliveryArea("浙江省", "杭州市", "西湖区") shouldBe true
    }
    test("validateDeliveryAddress does not throw") {
      service.validateDeliveryAddress("浙江省", "杭州市", "西湖区")
    }
    test("list queries pass through") {
      val enabled = listOf(area(1L))
      every { areaRepository.findByEnabledTrue() } returns enabled

      service.getEnabledDeliveryAreas() shouldBe enabled
      verify(exactly = 1) { areaRepository.findByEnabledTrue() }
    }
  })
