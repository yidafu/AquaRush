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
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * 配送费规则查询服务(用户端/订单域消费)单元测试:未配置返回默认规则(免费4楼/加价2元)
 */
class DeliveryFeeRuleQueryApiServiceImplTest {
  private lateinit var deliveryFeeRuleRepository: DeliveryFeeRuleRepository
  private lateinit var service: DeliveryFeeRuleQueryApiServiceImpl

  @BeforeEach
  fun setUp() {
    deliveryFeeRuleRepository = mockk(relaxed = true)
    service = DeliveryFeeRuleQueryApiServiceImpl(deliveryFeeRuleRepository)
  }

  @Test
  @DisplayName("未配置返回默认规则 免费4楼/加价2元")
  fun `unconfigured returns default rule`() {
    every { deliveryFeeRuleRepository.findById(DeliveryFeeRuleRepository.SINGLETON_ID) } returns java.util.Optional.empty()

    val rule = service.getEffectiveDeliveryFeeRule()

    assertEquals(DeliveryFeeRuleModel.DEFAULT_FREE_FLOOR_LIMIT, rule.freeFloorLimit)
    assertEquals(DeliveryFeeRuleModel.DEFAULT_SURCHARGE_CENTS, rule.surchargeCents)
  }

  @Test
  @DisplayName("已配置返回配置值(永不返回 null)")
  fun `configured returns stored rule`() {
    val stored = DeliveryFeeRuleModel(id = DeliveryFeeRuleRepository.SINGLETON_ID, freeFloorLimit = 6, surchargeCents = 500)
    every { deliveryFeeRuleRepository.findById(DeliveryFeeRuleRepository.SINGLETON_ID) } returns java.util.Optional.of(stored)

    val rule = service.getEffectiveDeliveryFeeRule()

    assertEquals(6, rule.freeFloorLimit)
    assertEquals(500L, rule.surchargeCents)
  }
}
