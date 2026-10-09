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
import dev.yidafu.aqua.admin.testutil.StatisticsTestSeeds
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsSnapshotAggregationRepository
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.OrderItemModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentRefundModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import dev.yidafu.aqua.common.domain.model.UserActionLogModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.graphql.generated.RefundType
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import dev.yidafu.aqua.order.domain.repository.OrderItemRepository
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import dev.yidafu.aqua.payment.domain.repository.PaymentRefundRepository
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import dev.yidafu.aqua.user.domain.repository.AddressRepository
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

/**
 * 统计快照按日聚合仓储集成测试(H2 PostgreSQL 模式)
 * 覆盖第 3 层中间表写入侧的数据正确性:
 * - 订单事实:多商品订单(F-ORD-004)金额按订单只计一次(防 P1-3),件数按明细汇总,状态维度落库;
 * - 商品事实:revenue = Σ order_items.total_price_cents,quantity = Σ 件数;
 * - 用户事实:新增用户数 + USER_LOGIN 登录用户数(按 userId 去重);
 * - 退款事实:按 processed_at 归属日,status/refund_type 维度落库(查询侧组合口径)。
 * 种子数据使用 2040 年窗口 + nanoTime 标识,避免与其它集成测试互相污染。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/statistics/"])
@ApplyExtension(SpringExtension::class)
class StatisticsSnapshotAggregationIntegrationTest(
  private val orderRepository: OrderRepository,
  private val orderItemRepository: OrderItemRepository,
  private val addressRepository: AddressRepository,
  private val userRepository: UserRepository,
  private val paymentRepository: PaymentRepository,
  private val paymentRefundRepository: PaymentRefundRepository,
  private val userActionLogRepository: UserActionLogRepository,
  private val snapshotAggregationRepository: StatisticsSnapshotAggregationRepository,
  private val productRepository: ProductRepository,
) : FunSpec({
    var userId = 1L
    var addressId = 0L
    beforeTest {
      // order_items.product_id 有外键 fk_order_items_product,而用例以固定商品 id 1/2/3
      // 断言聚合口径 → 按 id 显式种子(Exposed save 带 id 为幂等 upsert)。
      StatisticsTestSeeds.seedProducts(productRepository)
      userId =
        userRepository
          .save(
            UserModel(
              wechatOpenId = "snap-user-${System.nanoTime()}",
              email = "snap@test.local",
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
      deliveryWorkerId: Long? = null,
    ): Long =
      orderRepository
        .save(
          OrderModel(
            orderNo = "SNAP-${System.nanoTime()}",
            userId = userId,
            addressId = addressId,
            amountCents = amountCents,
            status = status,
            deliveryWorkerId = deliveryWorkerId,
            createdAt = createdAt,
            // OrderModel.completedAt 默认 null;生产在完成时写入,种子数据按下单即完成模拟
            completedAt = createdAt.takeIf { status == OrderModelStatus.COMPLETED },
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
            transactionId = "WX-SNAP-${System.nanoTime()}",
            amount = 10000L,
            status = PaymentStatus.SUCCESS,
          ),
        ).id!!

    fun seedRefund(
      paymentId: Long,
      orderId: Long,
      refundAmount: Long,
      status: RefundStatus,
      refundType: RefundType = RefundType.ORDER_REFUND,
      refundedAt: LocalDateTime,
    ): Long =
      paymentRefundRepository
        .save(
          PaymentRefundModel(
            paymentId = paymentId,
            orderId = orderId,
            userId = userId,
            refundId = "REF-SNAP-${System.nanoTime()}",
            outRefundNo = "R-SNAP-${System.nanoTime()}",
            refundAmount = refundAmount,
            refundReason = "测试退款",
            status = status,
            refundType = refundType,
            refundedAt = refundedAt,
          ),
        ).id!!

    test("aggregateOrderFacts counts amount once and quantity per item for multi-product orders") {
      val day = LocalDate.of(2040, 6, 10)
      // 多商品订单:1 单 2 条明细(件数 2+3),订单金额 10000 分 —— 金额只能计一次,件数计 5
      val multiOrderId = seedOrder(amountCents = 10000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(12, 0))
      seedItem(multiOrderId, productId = 1L, quantity = 2)
      seedItem(multiOrderId, productId = 2L, quantity = 3)
      // 同日的未完成订单:状态维度独立落库
      seedOrder(amountCents = 9999L, status = OrderModelStatus.PENDING_DISPATCH, createdAt = day.atTime(14, 0))

      val facts = snapshotAggregationRepository.aggregateOrderFacts(day)

      val completed = facts.first { it.status == OrderModelStatus.COMPLETED.name }
      completed.orderCount shouldBe 1L
      completed.amountCents shouldBe 10000L // 金额按订单只计一次(P1-3)
      completed.itemQuantity shouldBe 5L // 2 + 3
      val pending = facts.first { it.status == OrderModelStatus.PENDING_DISPATCH.name }
      pending.orderCount shouldBe 1L
      pending.amountCents shouldBe 9999L
      pending.itemQuantity shouldBe 0L // 无明细的订单件数为 0
    }

    test("aggregateProductFacts groups by product with revenue and quantity") {
      val day = LocalDate.of(2040, 6, 11)
      val orderId = seedOrder(amountCents = 10000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(10, 0))
      seedItem(orderId, productId = 1L, quantity = 2)
      seedItem(orderId, productId = 2L, quantity = 3)
      // 另一单也含商品 1
      val orderId2 = seedOrder(amountCents = 5000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(11, 0))
      seedItem(orderId2, productId = 1L, quantity = 1)

      val facts = snapshotAggregationRepository.aggregateProductFacts(day)

      facts.first { it.productId == 1L }.quantity shouldBe 3L // 2 + 1
      facts.first { it.productId == 1L }.revenueCents shouldBe 3000L // 1000*2 + 1000*1
      facts.first { it.productId == 1L }.orderCount shouldBe 2L // 去重订单数
      facts.first { it.productId == 2L }.quantity shouldBe 3L
      facts.first { it.productId == 2L }.revenueCents shouldBe 3000L
      facts.first { it.productId == 2L }.orderCount shouldBe 1L
    }

    test("aggregateUserFact counts new users and distinct login users per day") {
      val day = LocalDate.of(2040, 6, 12)
      // 当日新增 1 用户(显式 createdAt 落在测试日;beforeTest 的种子用户 createdAt=now 不计入)
      userRepository.save(
        UserModel(
          wechatOpenId = "snap-new-${System.nanoTime()}",
          email = "snap-new@test.local",
          createdAt = day.atTime(8, 0),
        ),
      )
      // 补一个登录日志
      userActionLogRepository.save(
        UserActionLogModel(
          userId = userId.toString(),
          username = "snap-user",
          actionType = "USER_LOGIN",
          createdAt = day.atTime(9, 0),
        ),
      )
      // 同用户同日重复登录 → 登录用户数仍为 1(按 userId 去重)
      userActionLogRepository.save(
        UserActionLogModel(
          userId = userId.toString(),
          username = "snap-user",
          actionType = "USER_LOGIN",
          createdAt = day.atTime(10, 0),
        ),
      )

      val fact = snapshotAggregationRepository.aggregateUserFact(day)

      fact.newUserCount shouldBe 1L
      fact.loginUserCount shouldBe 1L
    }

    test("aggregateRefundFacts attributes by processed_at and keeps refund_type dimension") {
      val day = LocalDate.of(2040, 6, 13)
      val orderId = seedOrder(amountCents = 10000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(10, 0))
      val paymentId = seedPayment(orderId)
      seedRefund(paymentId, orderId, refundAmount = 400L, status = RefundStatus.COMPLETED, refundedAt = day.atTime(15, 0))
      // 同日的押金退还(DEPOSIT_REFUND)与 PENDING 退款也落库,查询侧组合时排除
      seedRefund(
        paymentId,
        orderId,
        refundAmount = 300L,
        status = RefundStatus.COMPLETED,
        refundType = RefundType.DEPOSIT_REFUND,
        refundedAt = day.atTime(16, 0),
      )
      seedRefund(paymentId, orderId, refundAmount = 200L, status = RefundStatus.PENDING, refundedAt = day.atTime(17, 0))

      val facts = snapshotAggregationRepository.aggregateRefundFacts(day)

      facts.size shouldBe 3
      facts.first { it.refundType == RefundType.ORDER_REFUND.name && it.status == RefundStatus.COMPLETED.name }.refundAmountCents shouldBe
        400L
      facts.first { it.refundType == RefundType.DEPOSIT_REFUND.name && it.status == RefundStatus.COMPLETED.name }.refundAmountCents shouldBe
        300L
      facts.first { it.status == RefundStatus.PENDING.name }.refundAmountCents shouldBe 200L
    }

    test("aggregateRevenueFacts attributes completed revenue by completion day (营收按完成日归属)") {
      // 1/10 下单、1/12 完成:1/10 的营收事实为空(当时未完成),1/12 落 1 单 6600 分
      val createdDay = LocalDate.of(2040, 6, 15)
      val completedDay = LocalDate.of(2040, 6, 17)
      seedOrder(
        amountCents = 6600L,
        status = OrderModelStatus.COMPLETED,
        createdAt = createdDay.atTime(23, 30),
      ).also {
        // 显式把 completed_at 改为完成日(seedOrder 默认 completed_at=created_at)
        val order = orderRepository.findById(it).get()
        order.completedAt = completedDay.atTime(9, 0)
        orderRepository.save(order)
      }
      // 同日完成的普通订单(6/17 也下单也完成)与未完成订单:未完成不计入营收事实
      seedOrder(amountCents = 2000L, status = OrderModelStatus.COMPLETED, createdAt = completedDay.atTime(10, 0))
      seedOrder(amountCents = 9999L, status = OrderModelStatus.PENDING_DISPATCH, createdAt = completedDay.atTime(11, 0))

      val createdFacts = snapshotAggregationRepository.aggregateRevenueFacts(createdDay)
      val completedFacts = snapshotAggregationRepository.aggregateRevenueFacts(completedDay)

      // 下单日无营收(当时未完成);完成日营收 = 6600 + 2000
      createdFacts shouldBe emptyList()
      completedFacts.size shouldBe 1
      completedFacts[0].orderCount shouldBe 2L
      completedFacts[0].amountCents shouldBe 8600L
    }

    test("aggregateUserFact returns zero row for empty day (coverage continuity)") {
      val day = LocalDate.of(2040, 6, 14)
      val fact = snapshotAggregationRepository.aggregateUserFact(day)
      fact.newUserCount shouldBe 0L
      fact.loginUserCount shouldBe 0L
    }
  })
