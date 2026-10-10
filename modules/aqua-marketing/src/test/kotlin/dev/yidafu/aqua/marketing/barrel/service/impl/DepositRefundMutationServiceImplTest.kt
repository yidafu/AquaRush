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

import dev.yidafu.aqua.api.dto.SubmitDepositRefundRequest
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import dev.yidafu.aqua.common.domain.model.DepositRefundApplicationModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.DepositRefundApplicationStatus
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.RefundType
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositPriceRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositRecordRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositRefundApplicationRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

/**
 * 退押申请服务单元测试(R-PRO-04 状态机 / R-MON-03 账本幂等):
 * 提交校验(在租上限/重复申请)、回收确认、审核退还(微信/线下、整额/部分)、驳回。
 */
class DepositRefundMutationServiceImplTest :
  FunSpec({
    var applicationRepository: DepositRefundApplicationRepository = mockk()
    var depositRecordRepository: DepositRecordRepository = mockk()
    var depositPriceRepository: DepositPriceRepository = mockk()
    var orderQueryService: OrderQueryService = mockk()
    var paymentService: PaymentService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: DepositRefundMutationServiceImpl = mockk()

    fun heldRecord(
      id: Long,
      bucketCount: Int = 1,
      unit: Long = 4000,
      spec: String = "19L",
    ) = DepositRecordModel(
      id = id,
      orderId = 500L,
      userId = 100L,
      depositAmountCents = unit * bucketCount,
      specification = spec,
      unitDepositCents = unit,
      bucketCount = bucketCount,
      status = DepositStatus.HELD,
    )

    fun app(
      status: DepositRefundApplicationStatus = DepositRefundApplicationStatus.RECOVERED,
      bucketCount: Int = 1,
      spec: String = "19L",
    ) = DepositRefundApplicationModel(
      id = 900L,
      userId = 100L,
      specification = spec,
      bucketCount = bucketCount,
      depositCents = 4000L * bucketCount,
      status = status,
    )

    fun order(txnId: String?) =
      OrderModel(
        id = 500L,
        orderNo = "SO500",
        userId = 100L,
        amountCents = 4000L,
        deliveryFeeCents = 0L,
        depositCents = 4000L,
        status = OrderModelStatus.COMPLETED,
        paymentTransactionId = txnId,
        addressId = 1L,
      )

    val submitRequest =
      SubmitDepositRefundRequest(
        specification = "19L",
        bucketCount = 1,
        receiverName = "张三",
        receiverPhone = "13800000000",
        receiverDetail = "某小区1栋101",
        remark = "桶已备好",
      )

    beforeTest {
      applicationRepository = mockk(relaxed = true)
      depositRecordRepository = mockk(relaxed = true)
      depositPriceRepository = mockk(relaxed = true)
      orderQueryService = mockk(relaxed = true)
      paymentService = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      service =
        DepositRefundMutationServiceImpl(
          applicationRepository,
          depositRecordRepository,
          depositPriceRepository,
          orderQueryService,
          paymentService,
          bizLogger,
        )
    }

    // ---------- submit ----------

    test("submit 成功:按 HELD 快照计算应退金额") {
      every { applicationRepository.findActiveByUserAndSpec(100L, "19L") } returns null
      every { depositRecordRepository.findByUserIdAndSpecification(100L, "19L") } returns listOf(heldRecord(id = 1L))
      val saved = slot<DepositRefundApplicationModel>()
      every { applicationRepository.save(capture(saved)) } answers { firstArg() }

      val result = service.submit(100L, submitRequest)

      result.depositCents shouldBe 4000L
      result.bucketCount shouldBe 1
      result.status shouldBe DepositRefundApplicationStatus.PENDING
      saved.captured.receiverName shouldBe "张三"
    }

    test("submit 成功:多记录顺序覆盖合计金额") {
      every { applicationRepository.findActiveByUserAndSpec(100L, "19L") } returns null
      every { depositRecordRepository.findByUserIdAndSpecification(100L, "19L") } returns
        listOf(heldRecord(id = 1L, bucketCount = 2), heldRecord(id = 2L, bucketCount = 1))
      every { applicationRepository.save(any()) } answers { firstArg() }

      service.submit(100L, submitRequest.copy(bucketCount = 3)).depositCents shouldBe 12000L
    }

    test("submit 重复处理中申请拦截") {
      every { applicationRepository.findActiveByUserAndSpec(100L, "19L") } returns app(status = DepositRefundApplicationStatus.PENDING)

      shouldThrow<BadRequestException> { service.submit(100L, submitRequest) }
      verify(exactly = 0) { applicationRepository.save(any()) }
    }

    test("submit 退还桶数超过在租拦截") {
      every { applicationRepository.findActiveByUserAndSpec(100L, "19L") } returns null
      every { depositRecordRepository.findByUserIdAndSpecification(100L, "19L") } returns listOf(heldRecord(id = 1L, bucketCount = 1))

      shouldThrow<BadRequestException> { service.submit(100L, submitRequest.copy(bucketCount = 2)) }
    }

    test("submit 无在租记录拦截") {
      every { applicationRepository.findActiveByUserAndSpec(100L, "19L") } returns null
      every { depositRecordRepository.findByUserIdAndSpecification(100L, "19L") } returns emptyList()

      shouldThrow<BadRequestException> { service.submit(100L, submitRequest) }
    }

    test("submit 桶数非法拦截") {
      shouldThrow<BadRequestException> { service.submit(100L, submitRequest.copy(bucketCount = 0)) }
    }

    // ---------- confirmRecovered / reject ----------

    test("confirmRecovered PENDING→RECOVERED 成功") {
      every { applicationRepository.findById(900L) } returns app(status = DepositRefundApplicationStatus.PENDING)
      every {
        applicationRepository.transition(
          900L,
          listOf(DepositRefundApplicationStatus.PENDING),
          DepositRefundApplicationStatus.RECOVERED,
          7L,
          null,
        )
      } returns true
      every { applicationRepository.findById(900L) } returns app(status = DepositRefundApplicationStatus.RECOVERED)

      service.confirmRecovered(900L, 7L).status shouldBe DepositRefundApplicationStatus.RECOVERED
    }

    test("confirmRecovered 状态不符抛错") {
      every { applicationRepository.findById(900L) } returns app(status = DepositRefundApplicationStatus.PENDING)
      every {
        applicationRepository.transition(
          900L,
          listOf(DepositRefundApplicationStatus.PENDING),
          DepositRefundApplicationStatus.RECOVERED,
          7L,
          null,
        )
      } returns false

      shouldThrow<BadRequestException> { service.confirmRecovered(900L, 7L) }
    }

    test("reject 空原因拦截 + 非法状态拦截") {
      every { applicationRepository.findById(900L) } returns app(status = DepositRefundApplicationStatus.PENDING)

      shouldThrow<BadRequestException> { service.reject(900L, 7L, "  ") }

      every {
        applicationRepository.transition(900L, any(), DepositRefundApplicationStatus.REJECTED, 7L, "空桶破损")
      } returns false
      shouldThrow<BadRequestException> { service.reject(900L, 7L, "空桶破损") }
    }

    // ---------- approve ----------

    test("approve 状态前置校验:非 RECOVERED 拒绝") {
      every { applicationRepository.findById(900L) } returns app(status = DepositRefundApplicationStatus.PENDING)

      shouldThrow<BadRequestException> { service.approve(900L, 7L) }
      verify(exactly = 0) { paymentService.refund(any(), any(), any(), any(), any(), any()) }
    }

    test("approve 微信单:退款 DEPOSIT_REFUND + 记录翻转 + 申请置 REFUNDED") {
      every { applicationRepository.findById(900L) } returnsMany
        listOf(app(status = DepositRefundApplicationStatus.RECOVERED), app(status = DepositRefundApplicationStatus.REFUNDED))
      every { depositRecordRepository.findByUserIdAndSpecification(100L, "19L") } returns listOf(heldRecord(id = 1L))
      every { orderQueryService.getOrderById(500L) } returns order(txnId = "TXN123")
      every { depositRecordRepository.markRefundedIfHeld(any(), any(), any()) } returns 1
      every {
        applicationRepository.transition(
          900L,
          listOf(DepositRefundApplicationStatus.RECOVERED),
          DepositRefundApplicationStatus.REFUNDED,
          7L,
          null,
        )
      } returns true

      val result = service.approve(900L, 7L)

      result.status shouldBe DepositRefundApplicationStatus.REFUNDED
      verify(exactly = 1) {
        paymentService.refund(
          transactionId = "TXN123",
          refundAmountCents = 4000L,
          totalAmountCents = any(),
          reason = any(),
          refundType = RefundType.DEPOSIT_REFUND,
          refundRequestId = null,
        )
      }
      verify(exactly = 1) { depositRecordRepository.markRefundedIfHeld(listOf(1L), 7L, any()) }
    }

    test("approve 现金/无交易号单:不调微信退款,仅落账本") {
      every { applicationRepository.findById(900L) } returnsMany
        listOf(app(status = DepositRefundApplicationStatus.RECOVERED), app(status = DepositRefundApplicationStatus.REFUNDED))
      every { depositRecordRepository.findByUserIdAndSpecification(100L, "19L") } returns listOf(heldRecord(id = 1L))
      every { orderQueryService.getOrderById(500L) } returns order(txnId = null)
      every { depositRecordRepository.markRefundedIfHeld(any(), any(), any()) } returns 1
      every {
        applicationRepository.transition(
          900L,
          listOf(DepositRefundApplicationStatus.RECOVERED),
          DepositRefundApplicationStatus.REFUNDED,
          7L,
          null,
        )
      } returns true

      service.approve(900L, 7L).status shouldBe DepositRefundApplicationStatus.REFUNDED

      verify(exactly = 0) { paymentService.refund(any(), any(), any(), any(), any(), any()) }
      verify(exactly = 1) { depositRecordRepository.markRefundedIfHeld(listOf(1L), 7L, any()) }
    }

    test("approve 部分退还:原记录缩量保 HELD,切片落 REFUNDED") {
      every { applicationRepository.findById(900L) } returnsMany
        listOf(
          app(status = DepositRefundApplicationStatus.RECOVERED, bucketCount = 1),
          app(status = DepositRefundApplicationStatus.REFUNDED, bucketCount = 1),
        )
      every { depositRecordRepository.findByUserIdAndSpecification(100L, "19L") } returns
        listOf(heldRecord(id = 1L, bucketCount = 3))
      every { orderQueryService.getOrderById(500L) } returns order(txnId = null)
      every { depositRecordRepository.markRefundedIfHeld(any(), any(), any()) } returns 0
      every {
        applicationRepository.transition(
          900L,
          listOf(DepositRefundApplicationStatus.RECOVERED),
          DepositRefundApplicationStatus.REFUNDED,
          7L,
          null,
        )
      } returns true

      service.approve(900L, 7L)

      // 整额翻转不涉及该记录(部分覆盖)
      verify(exactly = 0) { depositRecordRepository.markRefundedIfHeld(listOf(1L), any(), any()) }
      // 原记录缩量(3-1=2 桶,4000*2)+ 新切片 REFUNDED 行(两次 saveAll 全量收集)
      val savedCalls = mutableListOf<List<DepositRecordModel>>()
      verify(atLeast = 1) { depositRecordRepository.saveAll(capture(savedCalls)) }
      val saved = savedCalls.flatten()
      saved.any { it.bucketCount == 2 && it.status == DepositStatus.HELD && it.depositAmountCents == 8000L } shouldBe true
      saved.any { it.bucketCount == 1 && it.status == DepositStatus.REFUNDED && it.depositAmountCents == 4000L } shouldBe true
    }

    test("approve 在租不足抛错") {
      every { applicationRepository.findById(900L) } returns app(status = DepositRefundApplicationStatus.RECOVERED, bucketCount = 5)
      every { depositRecordRepository.findByUserIdAndSpecification(100L, "19L") } returns listOf(heldRecord(id = 1L, bucketCount = 1))

      shouldThrow<BadRequestException> { service.approve(900L, 7L) }
      verify(exactly = 0) { paymentService.refund(any(), any(), any(), any(), any(), any()) }
    }
  })
