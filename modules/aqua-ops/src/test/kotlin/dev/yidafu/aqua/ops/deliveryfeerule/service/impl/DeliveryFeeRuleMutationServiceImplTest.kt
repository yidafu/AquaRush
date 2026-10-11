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
import dev.yidafu.aqua.common.graphql.generated.DeliveryDistanceTierInput
import dev.yidafu.aqua.common.graphql.generated.DeliveryFeeTierInput
import dev.yidafu.aqua.common.graphql.generated.SaveDeliveryFeeRuleInput
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

    /** PRD R-MON-02 默认三档(免费4楼:5-6+2 / 7-9+5 / 10+不封顶+8) */
    fun defaultTiers() =
      listOf(
        DeliveryFeeTierInput(feeCents = 200, fromFloor = 5, toFloor = 6),
        DeliveryFeeTierInput(feeCents = 500, fromFloor = 7, toFloor = 9),
        DeliveryFeeTierInput(feeCents = 800, fromFloor = 10, toFloor = null),
      )

    fun input(
      free: Int = 4,
      surcharge: Long = 200,
      tiers: List<DeliveryFeeTierInput> = defaultTiers(),
      minimumOrderCents: Long = 0L,
      distanceTiers: List<DeliveryDistanceTierInput> = emptyList(),
      storeLongitude: Float? = null,
      storeLatitude: Float? = null,
    ) = SaveDeliveryFeeRuleInput(
      distanceTiers = distanceTiers,
      freeFloorLimit = free,
      minimumOrderCents = minimumOrderCents,
      storeLatitude = storeLatitude,
      storeLongitude = storeLongitude,
      surchargeCents = surcharge,
      tiers = tiers,
    )

    test("first save creates singleton row") {
      every { deliveryFeeRuleRepository.findById(DeliveryFeeRuleRepository.SINGLETON_ID) } returns java.util.Optional.empty()
      every { deliveryFeeRuleRepository.save(any()) } answers { firstArg() }

      val saved =
        service.saveDeliveryFeeRule(
          1L,
          "admin",
          input(
            free = 6,
            surcharge = 500,
            tiers =
              listOf(
                DeliveryFeeTierInput(feeCents = 500, fromFloor = 7, toFloor = 9),
                DeliveryFeeTierInput(feeCents = 800, fromFloor = 10, toFloor = null),
              ),
          ),
        )

      saved.id shouldBe DeliveryFeeRuleRepository.SINGLETON_ID
      saved.freeFloorLimit shouldBe 6
      saved.surchargeCents shouldBe 500L
    }
    test("subsequent save updates in place with audit") {
      val existing = DeliveryFeeRuleModel(id = DeliveryFeeRuleRepository.SINGLETON_ID, freeFloorLimit = 4, surchargeCents = 200)
      every { deliveryFeeRuleRepository.findById(DeliveryFeeRuleRepository.SINGLETON_ID) } returns java.util.Optional.of(existing)
      every { deliveryFeeRuleRepository.save(any()) } answers { firstArg() }

      val saved =
        service.saveDeliveryFeeRule(
          1L,
          "admin",
          input(
            free = 6,
            surcharge = 500,
            tiers =
              listOf(
                DeliveryFeeTierInput(feeCents = 500, fromFloor = 7, toFloor = 9),
                DeliveryFeeTierInput(feeCents = 800, fromFloor = 10, toFloor = null),
              ),
          ),
        )

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
        service.saveDeliveryFeeRule(1L, "admin", input(surcharge = 99_901))
      }
    }
    test("negative surcharge rejected") {
      shouldThrow<IllegalArgumentException> {
        service.saveDeliveryFeeRule(1L, "admin", input(surcharge = -1))
      }
    }
    test("empty tiers rejected") {
      shouldThrow<IllegalArgumentException> { service.saveDeliveryFeeRule(1L, "admin", input(tiers = emptyList())) }
    }
    test("tiers must ascend and not overlap") {
      val overlapping =
        listOf(
          DeliveryFeeTierInput(feeCents = 200, fromFloor = 5, toFloor = 9),
          DeliveryFeeTierInput(feeCents = 500, fromFloor = 7, toFloor = null),
        )
      shouldThrow<IllegalArgumentException> { service.saveDeliveryFeeRule(1L, "admin", input(tiers = overlapping)) }
    }
    test("tier start floor must exceed free floor") {
      val tooLow =
        listOf(
          DeliveryFeeTierInput(feeCents = 200, fromFloor = 3, toFloor = null),
        )
      shouldThrow<IllegalArgumentException> { service.saveDeliveryFeeRule(1L, "admin", input(free = 4, tiers = tooLow)) }
    }
    test("last tier must be unbounded") {
      val bounded =
        listOf(
          DeliveryFeeTierInput(feeCents = 200, fromFloor = 5, toFloor = null),
          DeliveryFeeTierInput(feeCents = 500, fromFloor = 7, toFloor = 20),
        )
      shouldThrow<IllegalArgumentException> { service.saveDeliveryFeeRule(1L, "admin", input(tiers = bounded)) }
    }
    test("negative free floor rejected") {
      shouldThrow<IllegalArgumentException> {
        service.saveDeliveryFeeRule(1L, "admin", input(free = -1, surcharge = 200))
      }
    }

    // ===== G96 R-OPS-03:起送门槛 + 距离阶梯配置 =====

    test("save persists minimum order, distance tiers and store coordinates") {
      every { deliveryFeeRuleRepository.findById(DeliveryFeeRuleRepository.SINGLETON_ID) } returns java.util.Optional.empty()
      every { deliveryFeeRuleRepository.save(any()) } answers { firstArg() }

      val saved =
        service.saveDeliveryFeeRule(
          1L,
          "admin",
          input(
            minimumOrderCents = 3000L,
            distanceTiers =
              listOf(
                DeliveryDistanceTierInput(feeCents = 0, upToKm = 3f),
                DeliveryDistanceTierInput(feeCents = 300, upToKm = 5f),
                DeliveryDistanceTierInput(feeCents = 600, upToKm = null),
              ),
            storeLongitude = 113.93f,
            storeLatitude = 22.53f,
          ),
        )

      saved.minimumOrderCents shouldBe 3000L
      saved.distanceTiers!!.map { it.upToKm } shouldBe listOf(3.0, 5.0, null)
      saved.distanceTiers!!.map { it.feeCents } shouldBe listOf(0L, 300L, 600L)
      saved.storeLongitude shouldBe 113.93f.toDouble()
      saved.storeLatitude shouldBe 22.53f.toDouble()
    }

    test("negative minimum order rejected") {
      shouldThrow<IllegalArgumentException> {
        service.saveDeliveryFeeRule(1L, "admin", input(minimumOrderCents = -1L))
      }
    }

    test("distance tiers must ascend and not overlap") {
      val overlapping =
        listOf(
          DeliveryDistanceTierInput(feeCents = 0, upToKm = 5f),
          DeliveryDistanceTierInput(feeCents = 300, upToKm = 5f),
        )
      shouldThrow<IllegalArgumentException> {
        service.saveDeliveryFeeRule(
          1L,
          "admin",
          input(distanceTiers = overlapping, storeLongitude = 113.93f, storeLatitude = 22.53f),
        )
      }
    }

    test("unbounded distance tier must be last") {
      val unboundedFirst =
        listOf(
          DeliveryDistanceTierInput(feeCents = 300, upToKm = null),
          DeliveryDistanceTierInput(feeCents = 600, upToKm = 5f),
        )
      shouldThrow<IllegalArgumentException> {
        service.saveDeliveryFeeRule(
          1L,
          "admin",
          input(distanceTiers = unboundedFirst, storeLongitude = 113.93f, storeLatitude = 22.53f),
        )
      }
    }

    test("distance tiers require store coordinates") {
      shouldThrow<IllegalArgumentException> {
        service.saveDeliveryFeeRule(
          1L,
          "admin",
          input(distanceTiers = listOf(DeliveryDistanceTierInput(feeCents = 0, upToKm = 3f))),
        )
      }
    }

    test("store coordinates must be paired") {
      shouldThrow<IllegalArgumentException> {
        service.saveDeliveryFeeRule(1L, "admin", input(storeLongitude = 113.93f, storeLatitude = null))
      }
    }

    test("store coordinate out of range rejected") {
      shouldThrow<IllegalArgumentException> {
        service.saveDeliveryFeeRule(1L, "admin", input(storeLongitude = 200f, storeLatitude = 22.53f))
      }
    }

    test("distance tiers without store coordinates cleared allowed when empty") {
      every { deliveryFeeRuleRepository.findById(DeliveryFeeRuleRepository.SINGLETON_ID) } returns java.util.Optional.empty()
      every { deliveryFeeRuleRepository.save(any()) } answers { firstArg() }

      val saved = service.saveDeliveryFeeRule(1L, "admin", input())
      saved.minimumOrderCents shouldBe 0L
      saved.distanceTiers shouldBe null
    }
  })
