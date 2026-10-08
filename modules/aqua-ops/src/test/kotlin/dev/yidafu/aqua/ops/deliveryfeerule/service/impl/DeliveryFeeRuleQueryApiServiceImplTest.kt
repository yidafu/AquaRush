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

package dev.yidafu.aqua.ops.deliveryfeerule.service.impl

import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import dev.yidafu.aqua.ops.deliveryfeerule.domain.repository.DeliveryFeeRuleRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

/**
 * 配送费规则查询服务(用户端/订单域消费)单元测试:未配置返回默认规则(免费4楼/加价2元)
 */
class DeliveryFeeRuleQueryApiServiceImplTest :
  FunSpec({
    var deliveryFeeRuleRepository: DeliveryFeeRuleRepository = mockk()
    var service: DeliveryFeeRuleQueryApiServiceImpl = mockk()

    beforeTest {
      deliveryFeeRuleRepository = mockk(relaxed = true)
      service = DeliveryFeeRuleQueryApiServiceImpl(deliveryFeeRuleRepository)
    }
    test("unconfigured returns default rule") {
      every { deliveryFeeRuleRepository.findById(DeliveryFeeRuleRepository.SINGLETON_ID) } returns java.util.Optional.empty()

      val rule = service.getEffectiveDeliveryFeeRule()

      rule.freeFloorLimit shouldBe DeliveryFeeRuleModel.DEFAULT_FREE_FLOOR_LIMIT
      rule.surchargeCents shouldBe DeliveryFeeRuleModel.DEFAULT_SURCHARGE_CENTS
    }
    test("configured returns stored rule") {
      val stored = DeliveryFeeRuleModel(id = DeliveryFeeRuleRepository.SINGLETON_ID, freeFloorLimit = 6, surchargeCents = 500)
      every { deliveryFeeRuleRepository.findById(DeliveryFeeRuleRepository.SINGLETON_ID) } returns java.util.Optional.of(stored)

      val rule = service.getEffectiveDeliveryFeeRule()

      rule.freeFloorLimit shouldBe 6
      rule.surchargeCents shouldBe 500L
    }
  })
