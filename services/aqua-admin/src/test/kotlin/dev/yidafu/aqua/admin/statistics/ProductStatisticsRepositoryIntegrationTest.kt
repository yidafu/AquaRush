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
import dev.yidafu.aqua.analytics.statistics.domain.repository.ProductSalesStatisticsRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsUserRepository
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.OrderItemModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.order.domain.repository.OrderItemRepository
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 商品/用户统计仓储集成测试(Testcontainers 真实 PG)
 * 覆盖 PRD F-STA-006 与域级口径的数据正确性:
 * - P1-4 商品统计全量口径:总数/在售/下架/售罄按状态 SQL 分组,低库存预警含 OFFLINE/OUT_OF_STOCK 商品;
 * - P2-7 排行营收 = 已完成订单项金额合计(非 price × salesVolume 估算),非 COMPLETED 订单不计入;
 * - P2-9 每日新增用户按日 SQL GROUP BY(不整表拉取后内存分组)。
 * 种子数据使用 2031 年窗口 + nanoTime 唯一标识,避免与其它集成测试相互污染;
 * 商品计数类断言采用「前后差量」方式(共享内存库中其它测试类可能已种商品)。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/statistics/"])
@ApplyExtension(SpringExtension::class)
class ProductStatisticsRepositoryIntegrationTest(
  private val productRepository: ProductRepository,
  private val orderRepository: OrderRepository,
  private val orderItemRepository: OrderItemRepository,
  private val userRepository: UserRepository,
  private val addressRepository: AddressRepository,
  private val productSalesStatisticsRepository: ProductSalesStatisticsRepository,
  private val statisticsUserRepository: StatisticsUserRepository,
) : FunSpec({
    // orders.user_id/address_id 存在外键,先种一条用户与地址供订单引用
    var userId = 1L
    var addressId = 0L
    beforeTest {
      userId =
        userRepository
          .save(
            UserModel(
              wechatOpenId = "pstat-user-${System.nanoTime()}",
              email = "pstat@test.local",
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

    fun seedProduct(
      name: String,
      price: Long,
      stock: Int,
      salesVolume: Int,
      status: ProductModelStatus,
    ): Long =
      productRepository
        .save(
          ProductModel().apply {
            this.name = name
            this.price = price
            this.stock = stock
            this.salesVolume = salesVolume
            this.status = status
            this.specification = "500ml"
          },
        ).id!!

    fun seedOrder(
      amountCents: Long,
      status: OrderModelStatus,
      createdAt: LocalDateTime,
    ): Long =
      orderRepository
        .save(
          OrderModel(
            orderNo = "PSTAT-${System.nanoTime()}",
            userId = userId,
            addressId = addressId,
            amountCents = amountCents,
            status = status,
            createdAt = createdAt,
            completedAt = createdAt.takeIf { status == OrderModelStatus.COMPLETED },
          ),
        ).id!!

    fun seedItem(
      orderId: Long,
      productId: Long,
      quantity: Int,
      unitPriceCents: Long,
    ): Long =
      orderItemRepository
        .save(
          OrderItemModel.create(
            orderId = orderId,
            productId = productId,
            quantity = quantity,
            unitPriceCents = unitPriceCents,
            productSnapshot = """{"name":"测试桶装水"}""",
          ),
        ).id!!

    test("countProductsByStatus and aggregateProductValue cover all statuses (P1-4)") {
      val beforeCounts = productSalesStatisticsRepository.countProductsByStatus()
      val beforeAgg = productSalesStatisticsRepository.aggregateProductValue()

      val n = System.nanoTime()
      seedProduct(name = "p14-在线-$n", price = 200L, stock = 50, salesVolume = 100, status = ProductModelStatus.ONLINE)
      seedProduct(name = "p14-在线低库存-$n", price = 300L, stock = 3, salesVolume = 20, status = ProductModelStatus.ONLINE)
      seedProduct(name = "p14-下架-$n", price = 400L, stock = 10, salesVolume = 5, status = ProductModelStatus.OFFLINE)
      seedProduct(name = "p14-售罄-$n", price = 500L, stock = 0, salesVolume = 0, status = ProductModelStatus.OUT_OF_STOCK)

      val afterCounts = productSalesStatisticsRepository.countProductsByStatus()
      afterCounts[ProductModelStatus.ONLINE] shouldBe (beforeCounts[ProductModelStatus.ONLINE] ?: 0L) + 2
      afterCounts[ProductModelStatus.OFFLINE] shouldBe (beforeCounts[ProductModelStatus.OFFLINE] ?: 0L) + 1
      afterCounts[ProductModelStatus.OUT_OF_STOCK] shouldBe (beforeCounts[ProductModelStatus.OUT_OF_STOCK] ?: 0L) + 1
      // 商品总数 = 全量商品(在售 + 下架 + 售罄),不再等于在售数
      afterCounts.values.sum() shouldBe beforeCounts.values.sum() + 4

      val afterAgg = productSalesStatisticsRepository.aggregateProductValue()
      afterAgg.totalProducts shouldBe beforeAgg.totalProducts + 4
      afterAgg.totalValueCents shouldBe beforeAgg.totalValueCents + 1400L // 200+300+400+500
      afterAgg.totalSales shouldBe beforeAgg.totalSales + 125L // 100+20+5+0
    }

    test("countLowStockProducts and findLowStockProducts include OFFLINE/OUT_OF_STOCK (P1-4, R-PRO-02)") {
      val beforeCount = productSalesStatisticsRepository.countLowStockProducts(10L)

      val n = System.nanoTime()
      seedProduct(name = "ls-缺货售罄-$n", price = 100L, stock = 0, salesVolume = 1, status = ProductModelStatus.OUT_OF_STOCK)
      seedProduct(name = "ls-预警在线-$n", price = 100L, stock = 3, salesVolume = 1, status = ProductModelStatus.ONLINE)
      seedProduct(name = "ls-边界下架-$n", price = 100L, stock = 10, salesVolume = 1, status = ProductModelStatus.OFFLINE)
      seedProduct(name = "ls-充足-$n", price = 100L, stock = 50, salesVolume = 1, status = ProductModelStatus.ONLINE)

      // 低库存计数:下架/售罄商品也计入(此前 findByStatus(ONLINE) 子集会漏掉)
      productSalesStatisticsRepository.countLowStockProducts(10L) shouldBe beforeCount + 3

      val items = productSalesStatisticsRepository.findLowStockProducts(10L, limit = 500)
      val mine = items.filter { it.productName.startsWith("ls-") }
      mine.size shouldBe 3
      // 缺货(库存=0)置顶,其余按库存升序
      mine[0].productName shouldBe "ls-缺货售罄-$n"
      mine[0].stock shouldBe 0
      mine[1].productName shouldBe "ls-预警在线-$n"
      mine[1].stock shouldBe 3
      mine[2].productName shouldBe "ls-边界下架-$n"
      mine[2].stock shouldBe 10
      // LIMIT 有界(规则 13)
      items.size shouldBeLessThanOrEqual 500
    }

    test("getProductSalesRanking aggregates completed order items only (P2-7)") {
      val n = System.nanoTime()
      val productA = seedProduct(name = "rank-甲-$n", price = 1000L, stock = 99, salesVolume = 0, status = ProductModelStatus.ONLINE)
      val productB = seedProduct(name = "rank-乙-$n", price = 1000L, stock = 99, salesVolume = 0, status = ProductModelStatus.ONLINE)
      // 无销量的商品不进入排行
      seedProduct(name = "rank-无销量-$n", price = 1000L, stock = 99, salesVolume = 0, status = ProductModelStatus.ONLINE)

      // 种子月份 2031-05 与 StatisticsOrderRepositoryIntegrationTest 的 6~12 月窗口互不重叠(共享内存库)
      val day = LocalDate.of(2031, 5, 10)
      // 已完成订单:甲 5 桶 ×1000 = 5000,乙 3 桶 ×1000 = 3000
      val completedOrderId = seedOrder(amountCents = 8000L, status = OrderModelStatus.COMPLETED, createdAt = day.atTime(12, 0))
      seedItem(completedOrderId, productA, quantity = 5, unitPriceCents = 1000L)
      seedItem(completedOrderId, productB, quantity = 3, unitPriceCents = 1000L)
      // 非已完成订单:甲 99 桶 ×999 —— 不得计入排行营收/销量
      val pendingOrderId = seedOrder(amountCents = 999L, status = OrderModelStatus.PENDING_DISPATCH, createdAt = day.atTime(13, 0))
      seedItem(pendingOrderId, productA, quantity = 99, unitPriceCents = 999L)

      val ranking = productSalesStatisticsRepository.getProductSalesRanking(limit = 10)
      val mine = ranking.filter { it.productName.startsWith("rank-") }

      mine.size shouldBe 2
      // 按销量降序:甲(5)在前,乙(3)在后
      mine[0].productId shouldBe productA
      mine[0].salesVolume shouldBe 5L
      mine[0].revenueCents shouldBe 5000L
      mine[1].productId shouldBe productB
      mine[1].salesVolume shouldBe 3L
      mine[1].revenueCents shouldBe 3000L
    }

    test("countDailyNewUsers groups by day in SQL (P2-9)") {
      val day1 = LocalDate.of(2031, 9, 1)
      val day2 = LocalDate.of(2031, 9, 2)
      val n = System.nanoTime()
      userRepository.save(UserModel(wechatOpenId = "nu-1-$n", email = "nu@test.local", createdAt = day1.atTime(8, 0)))
      userRepository.save(UserModel(wechatOpenId = "nu-2-$n", email = "nu@test.local", createdAt = day1.atTime(20, 0)))
      userRepository.save(UserModel(wechatOpenId = "nu-3-$n", email = "nu@test.local", createdAt = day2.atTime(9, 30)))

      val counts =
        statisticsUserRepository.countDailyNewUsers(
          startDateTime = day1.atStartOfDay(),
          endDateTime = day2.plusDays(1).atStartOfDay(),
        )
      // 2031 窗口无其它测试污染(其余用例 createdAt 为当前时间),可绝对断言
      counts[day1] shouldBe 2L
      counts[day2] shouldBe 1L
    }
  })
