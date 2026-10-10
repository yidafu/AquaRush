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

package dev.yidafu.aqua.marketing.barrel.service.impl

import dev.yidafu.aqua.api.dto.PurchaseBarrelItemRequest
import dev.yidafu.aqua.api.dto.PurchaseBarrelRequest
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseItemModel
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel
import dev.yidafu.aqua.common.domain.model.DepositPriceModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import dev.yidafu.aqua.common.domain.model.enums.DepositPriceStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.barrel.domain.repository.BarrelPurchaseItemRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.BarrelPurchaseRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositPriceRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Optional

/**
 * 水桶购买单变更服务单元测试(G06:一次性塑料水桶费,独立支付,每用户总限购一次):
 * 购桶(限购/价目校验/明细快照)、支付入账(原子/幂等)、管理员退还(状态守卫/幂等)。
 */
class BarrelPurchaseMutationServiceImplTest :
  FunSpec({
    var barrelFeeRepository: DepositPriceRepository = mockk()
    var barrelPurchaseRepository: BarrelPurchaseRepository = mockk()
    var barrelPurchaseItemRepository: BarrelPurchaseItemRepository = mockk()
    var bizLogger: BizLogger = mockk()
    var service: BarrelPurchaseMutationServiceImpl = mockk()

    fun fee(
      id: Long = 1L,
      specification: String = "19L/桶",
      priceCents: Long = 4000,
      status: DepositPriceStatus = DepositPriceStatus.ENABLED,
    ): DepositPriceModel =
      DepositPriceModel(
        id = id,
        specification = specification,
        priceCents = priceCents,
        status = status,
        sortOrder = 1,
      )

    fun purchase(
      id: Long = 1L,
      userId: Long = 100L,
      status: BarrelPaymentStatus = BarrelPaymentStatus.PENDING,
    ) = BarrelPurchaseModel(
      id = id,
      userId = userId,
      orderNo = "BK1001690000000000",
      totalAmountCents = 4000,
      paymentStatus = status,
    )

    beforeTest {
      barrelFeeRepository = mockk()
      barrelPurchaseRepository = mockk()
      barrelPurchaseItemRepository = mockk()
      bizLogger = mockk(relaxed = true)
      service =
        BarrelPurchaseMutationServiceImpl(
          barrelFeeRepository,
          barrelPurchaseRepository,
          barrelPurchaseItemRepository,
          bizLogger,
        )
    }

    test("purchaseBarrel creates PENDING purchase with item snapshots") {
      every { barrelPurchaseRepository.existsByUserIdAndPaymentStatus(100L, BarrelPaymentStatus.PAID) } returns false
      every { barrelFeeRepository.findById(1L) } returns Optional.of(fee())
      every { barrelPurchaseRepository.cancelPendingByUser(any(), any()) } returns 0
      val purchaseSlot = slot<BarrelPurchaseModel>()
      every { barrelPurchaseRepository.save(capture(purchaseSlot)) } answers {
        firstArg<BarrelPurchaseModel>().apply { id = 99L }
      }
      every { barrelPurchaseItemRepository.saveAll(any<List<BarrelPurchaseItemModel>>()) } answers
        { firstArg<Iterable<BarrelPurchaseItemModel>>().toList() }

      val saved = service.purchaseBarrel(100L, PurchaseBarrelRequest(listOf(PurchaseBarrelItemRequest(1L, 2))))

      saved.paymentStatus shouldBe BarrelPaymentStatus.PENDING
      saved.totalAmountCents shouldBe 8000
      saved.orderNo.startsWith("BK") shouldBe true
      verify { barrelPurchaseRepository.cancelPendingByUser(100L, any()) }
    }

    test("purchaseBarrel rejects when user already has PAID purchase (每用户总限购一次)") {
      every { barrelPurchaseRepository.existsByUserIdAndPaymentStatus(100L, BarrelPaymentStatus.PAID) } returns true

      shouldThrow<BadRequestException> {
        service.purchaseBarrel(100L, PurchaseBarrelRequest(listOf(PurchaseBarrelItemRequest(1L, 1))))
      }
      verify(exactly = 0) { barrelPurchaseRepository.save(any()) }
    }

    test("purchaseBarrel rejects when fee disabled") {
      every { barrelPurchaseRepository.existsByUserIdAndPaymentStatus(100L, BarrelPaymentStatus.PAID) } returns false
      every { barrelFeeRepository.findById(1L) } returns Optional.of(fee(status = DepositPriceStatus.DISABLED))

      shouldThrow<BadRequestException> {
        service.purchaseBarrel(100L, PurchaseBarrelRequest(listOf(PurchaseBarrelItemRequest(1L, 1))))
      }
    }

    test("purchaseBarrel rejects when fee missing") {
      every { barrelPurchaseRepository.existsByUserIdAndPaymentStatus(100L, BarrelPaymentStatus.PAID) } returns false
      every { barrelFeeRepository.findById(1L) } returns Optional.empty()

      shouldThrow<BadRequestException> {
        service.purchaseBarrel(100L, PurchaseBarrelRequest(listOf(PurchaseBarrelItemRequest(1L, 1))))
      }
    }

    test("handleBarrelPurchasePaid marks PAID atomically") {
      val p = purchase()
      every { barrelPurchaseRepository.findById(1L) } returns Optional.of(p)
      every { barrelPurchaseRepository.markPaidAtomically(1L, "WX-TXN-1", any()) } returns 1

      val saved = service.handleBarrelPurchasePaid(1L, "WX-TXN-1")

      verify { barrelPurchaseRepository.markPaidAtomically(1L, "WX-TXN-1", any()) }
      saved.id shouldBe 1L
    }

    test("handleBarrelPurchasePaid is idempotent when already PAID") {
      val p = purchase(status = BarrelPaymentStatus.PAID)
      every { barrelPurchaseRepository.findById(1L) } returns Optional.of(p)

      val saved = service.handleBarrelPurchasePaid(1L, "WX-TXN-1")

      verify(exactly = 0) { barrelPurchaseRepository.markPaidAtomically(any(), any(), any()) }
      saved.paymentStatus shouldBe BarrelPaymentStatus.PAID
    }

    test("markRefunded marks REFUNDED with operator info") {
      val p = purchase(status = BarrelPaymentStatus.PAID)
      every { barrelPurchaseRepository.findById(1L) } returns Optional.of(p)
      every { barrelPurchaseRepository.save(any()) } answers { firstArg() }

      val saved = service.markRefunded(1L, refundedBy = 7L, remark = "空桶已退回")

      saved.paymentStatus shouldBe BarrelPaymentStatus.REFUNDED
      saved.refundedBy shouldBe 7L
      saved.refundRemark shouldBe "空桶已退回"
    }

    test("markRefunded throws when not PAID") {
      val p = purchase(status = BarrelPaymentStatus.PENDING)
      every { barrelPurchaseRepository.findById(1L) } returns Optional.of(p)

      shouldThrow<BadRequestException> { service.markRefunded(1L, refundedBy = 7L, remark = null) }
    }

    test("markRefunded is idempotent when already REFUNDED") {
      val p = purchase(status = BarrelPaymentStatus.REFUNDED)
      every { barrelPurchaseRepository.findById(1L) } returns Optional.of(p)

      val saved = service.markRefunded(1L, refundedBy = 7L, remark = "重复退还")

      saved.paymentStatus shouldBe BarrelPaymentStatus.REFUNDED
      verify(exactly = 0) { barrelPurchaseRepository.save(any()) }
    }
  })
