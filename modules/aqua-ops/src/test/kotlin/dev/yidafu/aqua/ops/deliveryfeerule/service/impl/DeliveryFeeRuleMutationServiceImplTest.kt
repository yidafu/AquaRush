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

import dev.yidafu.aqua.api.dto.SaveDeliveryFeeRuleRequest
import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.deliveryfeerule.domain.repository.DeliveryFeeRuleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 配送费规则变更服务单元测试(F-OPS-005:金额校验、单行配置 upsert、审计留痕)
 */
class DeliveryFeeRuleMutationServiceImplTest {
  private lateinit var deliveryFeeRuleRepository: DeliveryFeeRuleRepository
  private lateinit var opsConfigAuditService: OpsConfigAuditService
  private lateinit var service: DeliveryFeeRuleMutationServiceImpl

  @BeforeEach
  fun setUp() {
    deliveryFeeRuleRepository = mockk(relaxed = true)
    opsConfigAuditService = mockk(relaxed = true)
    service = DeliveryFeeRuleMutationServiceImpl(deliveryFeeRuleRepository, opsConfigAuditService)
  }

  @Test
  @DisplayName("首次保存创建单行配置(id=1)")
  fun `first save creates singleton row`() {
    every { deliveryFeeRuleRepository.findById(DeliveryFeeRuleRepository.SINGLETON_ID) } returns java.util.Optional.empty()
    every { deliveryFeeRuleRepository.save(any()) } answers { firstArg() }

    val saved = service.saveDeliveryFeeRule(1L, "admin", SaveDeliveryFeeRuleRequest(freeFloorLimit = 6, surchargeCents = 500))

    assertEquals(DeliveryFeeRuleRepository.SINGLETON_ID, saved.id)
    assertEquals(6, saved.freeFloorLimit)
    assertEquals(500L, saved.surchargeCents)
  }

  @Test
  @DisplayName("再次保存覆盖并落审计(含前后值)")
  fun `subsequent save updates in place with audit`() {
    val existing = DeliveryFeeRuleModel(id = DeliveryFeeRuleRepository.SINGLETON_ID, freeFloorLimit = 4, surchargeCents = 200)
    every { deliveryFeeRuleRepository.findById(DeliveryFeeRuleRepository.SINGLETON_ID) } returns java.util.Optional.of(existing)
    every { deliveryFeeRuleRepository.save(any()) } answers { firstArg() }

    val saved = service.saveDeliveryFeeRule(1L, "admin", SaveDeliveryFeeRuleRequest(freeFloorLimit = 6, surchargeCents = 500))

    assertEquals(6, saved.freeFloorLimit)
    verify {
      opsConfigAuditService.record(
        OpsConfigAuditService.TARGET_DELIVERY_FEE_RULE,
        DeliveryFeeRuleRepository.SINGLETON_ID,
        OpsConfigAuditService.ACTION_DELIVERY_FEE_RULE_SAVE,
        1L,
        "admin",
        before = any(),
        after = any(),
        description = any(),
      )
    }
  }

  @Test
  @DisplayName("加价超上限(999 元)抛 IllegalArgumentException")
  fun `surcharge over limit rejected`() {
    assertFailsWith<IllegalArgumentException> {
      service.saveDeliveryFeeRule(1L, "admin", SaveDeliveryFeeRuleRequest(freeFloorLimit = 4, surchargeCents = 99_901))
    }
  }

  @Test
  @DisplayName("加价为负抛 IllegalArgumentException")
  fun `negative surcharge rejected`() {
    assertFailsWith<IllegalArgumentException> {
      service.saveDeliveryFeeRule(1L, "admin", SaveDeliveryFeeRuleRequest(freeFloorLimit = 4, surchargeCents = -1))
    }
  }

  @Test
  @DisplayName("免费楼层为负抛 IllegalArgumentException")
  fun `negative free floor rejected`() {
    assertFailsWith<IllegalArgumentException> {
      service.saveDeliveryFeeRule(1L, "admin", SaveDeliveryFeeRuleRequest(freeFloorLimit = -1, surchargeCents = 200))
    }
  }
}
