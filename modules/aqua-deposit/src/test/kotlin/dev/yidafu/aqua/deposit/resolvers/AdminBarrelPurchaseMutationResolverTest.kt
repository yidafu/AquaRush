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

package dev.yidafu.aqua.deposit.resolvers

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.barrel.BarrelPurchaseMutationService
import dev.yidafu.aqua.api.service.barrel.BarrelPurchaseQueryService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.logging.util.BizLogger
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.userdetails.User

/**
 * 管理端退还水桶费解析器单元测试(G06):
 * 线上支付购桶单执行退款并置「已退还」;无交易号仅落记录;非已支付状态拒绝。
 */
class AdminBarrelPurchaseMutationResolverTest :
  FunSpec({
    var barrelPurchaseQueryService: BarrelPurchaseQueryService = mockk()
    var barrelPurchaseMutationService: BarrelPurchaseMutationService = mockk()
    var paymentService: PaymentService = mockk()
    var adminService: AdminService = mockk()
    var bizLogger: BizLogger = mockk()
    var resolver: AdminBarrelPurchaseMutationResolver = mockk()

    fun paidPurchase(transactionId: String? = "WX-TXN-1") =
      BarrelPurchaseModel(
        id = 1L,
        userId = 100L,
        orderNo = "BK1001690000000000",
        totalAmountCents = 4000,
        paymentStatus = BarrelPaymentStatus.PAID,
        transactionId = transactionId,
      )

    fun admin() =
      AdminModel(
        id = 7L,
        username = "admin",
        passwordHash = "x",
        realName = "管理员",
        role = dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel.ADMIN,
      )

    fun userDetails() = User("admin", "x", emptyList())

    beforeTest {
      barrelPurchaseQueryService = mockk(relaxed = true)
      barrelPurchaseMutationService = mockk(relaxed = true)
      paymentService = mockk(relaxed = true)
      adminService = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      resolver =
        AdminBarrelPurchaseMutationResolver(
          barrelPurchaseQueryService,
          barrelPurchaseMutationService,
          paymentService,
          adminService,
          bizLogger,
        )
      every { adminService.findByUsername("admin") } returns admin()
      every { barrelPurchaseQueryService.findItemsByPurchaseId(1L) } returns emptyList()
    }

    test("refundBarrelPurchase executes refund and marks refunded") {
      val record = paidPurchase()
      every { barrelPurchaseQueryService.getById(1L) } returns record
      every { barrelPurchaseMutationService.markRefunded(1L, 7L, "空桶已退回") } returns
        paidPurchase().apply {
          paymentStatus = BarrelPaymentStatus.REFUNDED
          refundedBy = 7L
          refundRemark = "空桶已退回"
        }

      val result = resolver.refundBarrelPurchase(1L, "空桶已退回", userDetails())

      // 当前 PaymentService.refund(transactionId, refundAmountCents, totalAmountCents): Boolean 重载
      verify { paymentService.refund("WX-TXN-1", 4000, 4000) }
      verify { barrelPurchaseMutationService.markRefunded(1L, 7L, "空桶已退回") }
      result.paymentStatus.name shouldBe "REFUNDED"
    }

    test("refundBarrelPurchase without transactionId only marks record (线下处理)") {
      val record = paidPurchase(transactionId = null)
      every { barrelPurchaseQueryService.getById(1L) } returns record
      every { barrelPurchaseMutationService.markRefunded(1L, 7L, null) } returns
        paidPurchase().apply {
          paymentStatus = BarrelPaymentStatus.REFUNDED
          refundedBy = 7L
        }

      resolver.refundBarrelPurchase(1L, null, userDetails())

      verify(exactly = 0) { paymentService.refund(any(), any(), any()) }
      verify { barrelPurchaseMutationService.markRefunded(1L, 7L, null) }
    }

    test("refundBarrelPurchase rejects when not paid") {
      val record = paidPurchase().apply { paymentStatus = BarrelPaymentStatus.PENDING }
      every { barrelPurchaseQueryService.getById(1L) } returns record

      shouldThrow<BadRequestException> { resolver.refundBarrelPurchase(1L, null, userDetails()) }
    }

    test("refundBarrelPurchase rejects when purchase missing") {
      every { barrelPurchaseQueryService.getById(1L) } returns null

      shouldThrow<BadRequestException> { resolver.refundBarrelPurchase(1L, null, userDetails()) }
    }
  })
