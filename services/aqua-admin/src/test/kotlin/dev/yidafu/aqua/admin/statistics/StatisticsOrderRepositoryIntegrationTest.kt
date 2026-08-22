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

package dev.yidafu.aqua.admin.statistics

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsOrderRepositoryCustom
import dev.yidafu.aqua.analytics.statistics.dto.OrderValueBucketDTO
import dev.yidafu.aqua.analytics.statistics.service.StatisticsService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.OrderItemModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentRefundModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.order.domain.repository.OrderItemRepository
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import dev.yidafu.aqua.payment.domain.repository.PaymentRefundRepository
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 订单统计仓储集成测试(H2 PostgreSQL 模式)
 * 覆盖 PRD F-STA-002/005 营收口径的数据正确性:
 * - 多商品订单(F-ORD-004)营收按订单只计一次,不得按明细行数重复求和(P1-3);
 * - 营收口径 = 已完成订单金额合计(COMPLETED 过滤在服务层传入 statuses);
 * - 商品件数仍按 order_items 明细汇总。
 * 种子数据使用 2031 年窗口 + nanoTime 订单号,避免与其它集成测试/本类各用例相互污染。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/statistics/"])
@ApplyExtension(SpringExtension::class)
class StatisticsOrderRepositoryIntegrationTest(
  private val orderRepository: OrderRepository,
  private val orderItemRepository: OrderItemRepository,
  private val addressRepository: AddressRepository,
  private val userRepository: UserRepository,
  private val statisticsOrderRepository: StatisticsOrderRepositoryCustom,
  private val statisticsService: StatisticsService,
  private val paymentRepository: PaymentRepository,
  private val paymentRefundRepository: PaymentRefundRepository,
  private val adminRepository: AdminRepository,
) : FunSpec({
    // orders.user_id/address_id 存在外键,先种一条用户与地址供订单引用
    var userId = 1L
    var addressId = 0L
    beforeTest {
      userId =
        userRepository
          .save(
            UserModel(
              wechatOpenId = "rev-user-${System.nanoTime()}",
              email = "rev@test.local",
            ),
          ).id!!
      addressId =
        addressRepository
          .save(
            AddressModel(
              userId = userId,
              receiverName = "测试收货人",
              phone = "13800000000",
              province = "广东省",
              city = "深圳市",
              district = "南山区",
              detailAddress = "测试路1号",
            ),
          ).id!!
    }

    fun seedOrder(
      amountCents: Long,
      status: OrderModelStatus,
      createdAt: LocalDateTime,
    ): Long =
      orderRepository
        .save(
          OrderModel(
            orderNo = "REV-${System.nanoTime()}",
            userId = userId,
            addressId = addressId,
            amountCents = amountCents,
            status = status,
            createdAt = createdAt,
          ),
        ).id!!

    fun seedItem(
      orderId: Long,
      productId: Long,
      quantity: Int,
    ): Long =
      orderItemRepository
        .save(
          OrderItemModel.create(
            orderId = orderId,
            productId = productId,
            quantity = quantity,
            unitPriceCents = 1000L,
            productSnapshot = """{"name":"测试桶装水"}""",
          ),
        ).id!!

    fun seedPayment(orderId: Long): Long =
      paymentRepository
        .save(
          PaymentModel(
            orderId = orderId,
            userId = userId,
            transactionId = "WX-REFUND-${System.nanoTime()}",
            amount = 10000L,
            status = PaymentStatus.SUCCESS,
          ),
        ).id!!

    fun seedRefund(
      paymentId: Long,
      orderId: Long,
      refundAmount: Long,
      status: RefundStatus,
      refundedAt: LocalDateTime,
    ): Long =
      paymentRefundRepository
        .save(
          PaymentRefundModel(
            paymentId = paymentId,
            orderId = orderId,
            userId = userId,
            refundId = "REF-${System.nanoTime()}",
            outRefundNo = "R-${System.nanoTime()}",
            refundAmount = refundAmount,
            refundReason = "订单取消退款",
            status = status,
            refundedAt = refundedAt,
          ),
        ).id!!

    test("getDailyOrderStatistics does not multiply revenue by item count for multi-product orders") {
      val day = LocalDate.of(2031, 6, 10)
      // 多商品订单:1 单 2 条明细(件数 2+3),订单金额 10000 分 —— 金额只能计一次
      val multiOrderId = seedOrder(amountCents = 10000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(12, 0))
      seedItem(multiOrderId, productId = 1L, quantity = 2)
      seedItem(multiOrderId, productId = 2L, quantity = 3)
      // 单商品订单:1 单 1 条明细,金额 5000 分
      val singleOrderId = seedOrder(amountCents = 5000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(13, 0))
      seedItem(singleOrderId, productId = 3L, quantity = 1)
      // 同日的未完成订单:计入订单数,不计入已完成营收口径
      seedOrder(amountCents = 9999L, status = OrderModelStatus.PENDING_DISPATCH, createdAt = day.atTime(14, 0))

      val dayStart = day.atStartOfDay()
      val dayEnd = day.atTime(LocalTime.MAX)

      val all =
        statisticsOrderRepository.getDailyOrderStatistics(
          startDateTime = dayStart,
          endDateTime = dayEnd,
          deliveryWorkerId = null,
          statuses = null,
        )
      all.size shouldBe 1
      all[0].orderCount shouldBe 3L
      all[0].orderProductCount shouldBe 6L // 2 + 3 + 1
      all[0].totalAmountCents shouldBe 24999L // 10000 + 5000 + 9999,金额按订单只计一次

      val completed =
        statisticsOrderRepository.getDailyOrderStatistics(
          startDateTime = dayStart,
          endDateTime = dayEnd,
          deliveryWorkerId = null,
          statuses = listOf(OrderModelStatus.COMPLETED),
        )
      completed.size shouldBe 1
      completed[0].orderCount shouldBe 2L
      completed[0].orderProductCount shouldBe 6L
      completed[0].totalAmountCents shouldBe 15000L // 已完成口径:10000 + 5000
    }

    test("getWeeklyOrderStatistics aggregates per week without revenue overcount") {
      // 种子数据在独立周内(7 月),避免与日/月用例互相污染;不断言桶键具体日期 ——
      // DATE_TRUNC('week') 在 H2 与 Postgres 的周起始日不同(周日/周一),属既有方言差异,与本用例口径无关
      val orderId =
        seedOrder(
          amountCents = 12000L,
          status = OrderModelStatus.COMPLETED,
          createdAt = LocalDateTime.of(2031, 7, 11, 10, 0),
        )
      seedItem(orderId, productId = 1L, quantity = 2)

      val result =
        statisticsOrderRepository.getWeeklyOrderStatistics(
          startDateTime = LocalDate.of(2031, 7, 7).atStartOfDay(),
          endDateTime = LocalDate.of(2031, 7, 13).atTime(LocalTime.MAX),
          deliveryWorkerId = null,
          statuses = listOf(OrderModelStatus.COMPLETED),
        )
      result.size shouldBe 1
      result[0].orderCount shouldBe 1L
      result[0].orderProductCount shouldBe 2L
      result[0].totalAmountCents shouldBe 12000L
    }

    test("getMonthlyOrderStatistics aggregates per month without revenue overcount") {
      // 种子数据在独立月内(8 月),避免与日/周用例互相污染
      val orderId =
        seedOrder(
          amountCents = 8000L,
          status = OrderModelStatus.COMPLETED,
          createdAt = LocalDateTime.of(2031, 8, 12, 9, 0),
        )
      seedItem(orderId, productId = 1L, quantity = 4)

      val result =
        statisticsOrderRepository.getMonthlyOrderStatistics(
          startDateTime = LocalDate.of(2031, 8, 1).atStartOfDay(),
          endDateTime = LocalDate.of(2031, 8, 31).atTime(LocalTime.MAX),
          deliveryWorkerId = null,
          statuses = listOf(OrderModelStatus.COMPLETED),
        )
      result.size shouldBe 1
      result[0].orderCount shouldBe 1L
      result[0].orderProductCount shouldBe 4L
      result[0].totalAmountCents shouldBe 8000L
    }

    test("countOrdersGroupByStatus aggregates order count by status (G37)") {
      // 种子数据在独立月内(9 月),避免与其它用例互相污染
      val day = LocalDate.of(2031, 9, 10)
      seedOrder(amountCents = 1000L, status = OrderModelStatus.PENDING_PAYMENT, createdAt = day.atTime(9, 0))
      seedOrder(amountCents = 2000L, status = OrderModelStatus.PENDING_DISPATCH, createdAt = day.atTime(10, 0))
      seedOrder(amountCents = 3000L, status = OrderModelStatus.PENDING_DELIVERY, createdAt = day.atTime(11, 0))
      seedOrder(amountCents = 4000L, status = OrderModelStatus.DELIVERING, createdAt = day.atTime(12, 0))
      seedOrder(amountCents = 5000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(13, 0))
      seedOrder(amountCents = 6000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(14, 0))
      seedOrder(amountCents = 7000L, status = OrderModelStatus.CANCELLED, createdAt = day.atTime(15, 0))
      seedOrder(amountCents = 8000L, status = OrderModelStatus.REFUNDED, createdAt = day.atTime(16, 0))

      val result =
        statisticsOrderRepository.countOrdersGroupByStatus(
          startDateTime = LocalDate.of(2031, 9, 1).atStartOfDay(),
          endDateTime = LocalDate.of(2031, 9, 30).atTime(LocalTime.MAX),
        )

      result[OrderModelStatus.PENDING_PAYMENT] shouldBe 1L
      result[OrderModelStatus.PENDING_DISPATCH] shouldBe 1L
      result[OrderModelStatus.PENDING_DELIVERY] shouldBe 1L
      result[OrderModelStatus.DELIVERING] shouldBe 1L
      result[OrderModelStatus.COMPLETED] shouldBe 2L
      result[OrderModelStatus.CANCELLED] shouldBe 1L
      result[OrderModelStatus.REFUNDED] shouldBe 1L
      result.values.sum() shouldBe 8L
    }

    test("countOrdersGroupByHour aggregates order count by creation hour (G37)") {
      // 种子数据在独立月内(10 月),避免与其它用例互相污染
      val day = LocalDate.of(2031, 10, 10)
      seedOrder(amountCents = 1000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(9, 30))
      seedOrder(amountCents = 2000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(9, 50))
      seedOrder(amountCents = 3000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(18, 10))
      seedOrder(amountCents = 4000L, status = OrderModelStatus.PENDING_DISPATCH, createdAt = day.atTime(0, 5))

      val result =
        statisticsOrderRepository.countOrdersGroupByHour(
          startDateTime = LocalDate.of(2031, 10, 1).atStartOfDay(),
          endDateTime = LocalDate.of(2031, 10, 31).atTime(LocalTime.MAX),
        )

      result[9] shouldBe 2L
      result[18] shouldBe 1L
      result[0] shouldBe 1L
      result.values.sum() shouldBe 4L
    }

    test("countOrdersByAmountBucket buckets by order amount counting multi-product order once (G37)") {
      // 种子数据在独立月内(11 月),避免与其它用例互相污染
      val day = LocalDate.of(2031, 11, 10)
      // 多商品订单:1 单 2 条明细,金额 1800 分 → 落在「0-20元」区间,只能计一次
      val multiOrderId = seedOrder(amountCents = 1800L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(10, 0))
      seedItem(multiOrderId, productId = 1L, quantity = 2)
      seedItem(multiOrderId, productId = 2L, quantity = 3)
      seedOrder(amountCents = 3000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(11, 0)) // 20-50元
      seedOrder(amountCents = 8000L, status = OrderModelStatus.PENDING_DISPATCH, createdAt = day.atTime(12, 0)) // 50-100元
      seedOrder(amountCents = 15000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(13, 0)) // 100-200元
      seedOrder(amountCents = 30000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(14, 0)) // 200元以上

      val buckets =
        listOf(
          OrderValueBucketDTO(fromCents = 0L, toCents = 2000L, label = "0-20元"),
          OrderValueBucketDTO(fromCents = 2000L, toCents = 5000L, label = "20-50元"),
          OrderValueBucketDTO(fromCents = 5000L, toCents = 10000L, label = "50-100元"),
          OrderValueBucketDTO(fromCents = 10000L, toCents = 20000L, label = "100-200元"),
          OrderValueBucketDTO(fromCents = 20000L, toCents = null, label = "200元以上"),
        )

      val result =
        statisticsOrderRepository.countOrdersByAmountBucket(
          startDateTime = LocalDate.of(2031, 11, 1).atStartOfDay(),
          endDateTime = LocalDate.of(2031, 11, 30).atTime(LocalTime.MAX),
          buckets = buckets,
        )

      result["0-20元"] shouldBe 1L // 多商品订单只计一次
      result["20-50元"] shouldBe 1L
      result["50-100元"] shouldBe 1L
      result["100-200元"] shouldBe 1L
      result["200元以上"] shouldBe 1L
      result.values.sum() shouldBe 5L
    }

    test("getOrderStatistics aggregates refund and net revenue (F-STA-002)") {
      // 种子数据在独立月内(12 月),避免与其它用例互相污染
      val day = LocalDate.of(2031, 12, 10)
      val orderId = seedOrder(amountCents = 10000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(10, 0))
      seedOrder(amountCents = 9999L, status = OrderModelStatus.PENDING_DISPATCH, createdAt = day.atTime(11, 0))
      val paymentId = seedPayment(orderId)
      seedRefund(paymentId, orderId, refundAmount = 400L, status = RefundStatus.COMPLETED, refundedAt = day.atTime(15, 0))
      // 非 COMPLETED 退款记录不计入退款统计
      seedRefund(paymentId, orderId, refundAmount = 300L, status = RefundStatus.PENDING, refundedAt = day.atTime(16, 0))

      val result = statisticsService.getOrderStatistics(LocalDate.of(2031, 12, 1), LocalDate.of(2031, 12, 31))

      result.completedOrders shouldBe 1L
      result.completedAmountCents shouldBe 10000L
      result.refundedAmountCents shouldBe 400L
      result.netRevenueCents shouldBe 9600L
    }

    test("order statistics window is strict open interval with filters (第1层 SQL 聚合)") {
      // 与旧内存过滤 isAfter/isBefore 等价:createdAt 严格 > start 且 < end
      // 种子数据在独立月内(2033-03),避免与其它用例互相污染
      val start = LocalDateTime.of(2033, 3, 1, 0, 0)
      val end = LocalDateTime.of(2033, 3, 2, 0, 0)
      seedOrder(amountCents = 1000L, status = OrderModelStatus.COMPLETED, createdAt = start) // 恰在窗口起点 → 排除
      seedOrder(amountCents = 2000L, status = OrderModelStatus.COMPLETED, createdAt = start.plusSeconds(1))
      seedOrder(amountCents = 4000L, status = OrderModelStatus.PENDING_DISPATCH, createdAt = end.minusSeconds(1))
      seedOrder(amountCents = 8000L, status = OrderModelStatus.COMPLETED, createdAt = end) // 恰在窗口终点 → 排除
      // orders.delivery_worker_id 外键引用 admins.id,须先种一条真实账号行
      val workerId =
        adminRepository
          .save(
            AdminModel(
              username = "rev-worker-${System.nanoTime()}",
              passwordHash = "hash",
              role = AdminRoleModel.DELIVERY_WORKER,
            ),
          ).id!!
      orderRepository.save(
        OrderModel(
          orderNo = "REV-${System.nanoTime()}",
          userId = userId,
          addressId = addressId,
          amountCents = 16000L,
          status = OrderModelStatus.COMPLETED,
          deliveryWorkerId = workerId,
          createdAt = start.plusSeconds(2),
        ),
      )

      // 窗口内共 3 单:2000(COMPLETED) + 4000(PENDING_DISPATCH) + 16000(COMPLETED,workerId)
      orderRepository.countByStatusAndCreatedAtBetween(start, end, null, null) shouldBe 3L
      orderRepository.countByStatusAndCreatedAtBetween(start, end, workerId, null) shouldBe 1L
      orderRepository.countByStatusAndCreatedAtBetween(start, end, null, listOf(OrderModelStatus.COMPLETED)) shouldBe 2L
      orderRepository.sumAmountCentsByStatusAndCreatedAtBetween(
        listOf(OrderModelStatus.COMPLETED),
        start,
        end,
        null,
      ) shouldBe 18000L
      orderRepository.sumAmountCentsByStatusAndCreatedAtBetween(
        listOf(OrderModelStatus.COMPLETED),
        start,
        end,
        workerId,
      ) shouldBe 16000L

      // 端到端:statisticsService 走同一聚合,口径与直接调用一致
      val stats = statisticsService.getOrderStatistics(LocalDate.of(2033, 3, 1), LocalDate.of(2033, 3, 1))
      stats.totalOrders shouldBe 3L
      stats.totalAmountCents shouldBe 22000L
      stats.completedOrders shouldBe 2L
      stats.completedAmountCents shouldBe 18000L
    }

    test("getDailyStatistics merges refund amounts by processed day (F-STA-002)") {
      // 种子数据在独立月内(2032-01),避免与其它用例互相污染
      val day = LocalDate.of(2032, 1, 10)
      val orderId = seedOrder(amountCents = 8000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(10, 0))
      val paymentId = seedPayment(orderId)
      seedRefund(paymentId, orderId, refundAmount = 250L, status = RefundStatus.COMPLETED, refundedAt = day.atTime(15, 0))

      val result = statisticsService.getDailyStatistics(LocalDate.of(2032, 1, 1), LocalDate.of(2032, 1, 31))

      val dayStat = result.first { it.date == day }
      dayStat.totalAmountCents shouldBe 8000L
      dayStat.refundedAmountCents shouldBe 250L
    }
  })
