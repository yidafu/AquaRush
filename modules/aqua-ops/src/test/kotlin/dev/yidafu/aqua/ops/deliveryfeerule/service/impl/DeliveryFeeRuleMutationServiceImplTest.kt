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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * 配送费规则变更服务单元测试(F-OPS-005:金额校验、单行配置 upsert、审计留痕)
 */
class DeliveryFeeRuleMutationServiceImplTest :
  FunSpec({
    var deliveryFeeRuleRepository: DeliveryFeeRuleRepository = mockk()
    var opsConfigAuditService: OpsConfigAuditService = mockk()
    var service: DeliveryFeeRuleMutationServiceImpl = mockk()

    beforeTest {
      deliveryFeeRuleRepository = mockk(relaxed = true)
      opsConfigAuditService = mockk(relaxed = true)
      service = DeliveryFeeRuleMutationServiceImpl(deliveryFeeRuleRepository, opsConfigAuditService)
    }
    test("first save creates singleton row") {
      every { deliveryFeeRuleRepository.findById(DeliveryFeeRuleRepository.SINGLETON_ID) } returns java.util.Optional.empty()
      every { deliveryFeeRuleRepository.save(any()) } answers { firstArg() }

      val saved = service.saveDeliveryFeeRule(1L, "admin", SaveDeliveryFeeRuleRequest(freeFloorLimit = 6, surchargeCents = 500))

      saved.id shouldBe DeliveryFeeRuleRepository.SINGLETON_ID
      saved.freeFloorLimit shouldBe 6
      saved.surchargeCents shouldBe 500L
    }
    test("subsequent save updates in place with audit") {
      val existing = DeliveryFeeRuleModel(id = DeliveryFeeRuleRepository.SINGLETON_ID, freeFloorLimit = 4, surchargeCents = 200)
      every { deliveryFeeRuleRepository.findById(DeliveryFeeRuleRepository.SINGLETON_ID) } returns java.util.Optional.of(existing)
      every { deliveryFeeRuleRepository.save(any()) } answers { firstArg() }

      val saved = service.saveDeliveryFeeRule(1L, "admin", SaveDeliveryFeeRuleRequest(freeFloorLimit = 6, surchargeCents = 500))

      saved.freeFloorLimit shouldBe 6
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
    test("surcharge over limit rejected") {
      shouldThrow<IllegalArgumentException> {
        service.saveDeliveryFeeRule(1L, "admin", SaveDeliveryFeeRuleRequest(freeFloorLimit = 4, surchargeCents = 99_901))
      }
    }
    test("negative surcharge rejected") {
      shouldThrow<IllegalArgumentException> {
        service.saveDeliveryFeeRule(1L, "admin", SaveDeliveryFeeRuleRequest(freeFloorLimit = 4, surchargeCents = -1))
      }
    }
    test("negative free floor rejected") {
      shouldThrow<IllegalArgumentException> {
        service.saveDeliveryFeeRule(1L, "admin", SaveDeliveryFeeRuleRequest(freeFloorLimit = -1, surchargeCents = 200))
      }
    }
  })
