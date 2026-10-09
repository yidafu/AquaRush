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

package dev.yidafu.aqua.analytics.statistics.domain.repository

import dev.yidafu.aqua.analytics.common.exposed.AnalyticsExposedH2TestSupport
import dev.yidafu.aqua.common.domain.model.DailyMarketingFactModel
import dev.yidafu.aqua.common.domain.model.DailyMarketingUsageFactModel
import dev.yidafu.aqua.common.domain.model.DailyOrderFactModel
import dev.yidafu.aqua.common.domain.model.DailyProductFactModel
import dev.yidafu.aqua.common.domain.model.DailyRefundFactModel
import dev.yidafu.aqua.common.domain.model.DailyRevenueFactModel
import dev.yidafu.aqua.common.domain.model.DailyUserFactModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDate

/**
 * 7 个统计日事实仓储实现的 H2 测试:saveAll→findBy→findFirst→deleteBy 生命周期 + 雪花 id 回填。
 * 覆盖 ExposedDailyFactRepository 基类与各 Impl 的列映射。
 */
class ExposedDailyFactRepositoriesImplTest :
  FunSpec({
    val database = AnalyticsExposedH2TestSupport.database
    val orderRepo = ExposedDailyOrderFactRepositoryImpl(database)
    val productRepo = ExposedDailyProductFactRepositoryImpl(database)
    val userRepo = ExposedDailyUserFactRepositoryImpl(database)
    val refundRepo = ExposedDailyRefundFactRepositoryImpl(database)
    val revenueRepo = ExposedDailyRevenueFactRepositoryImpl(database)
    val marketingRepo = ExposedDailyMarketingFactRepositoryImpl(database)
    val usageRepo = ExposedDailyMarketingUsageFactRepositoryImpl(database)
    val date = LocalDate.of(2026, 10, 1)

    beforeTest { AnalyticsExposedH2TestSupport.reset() }

    test("order fact 生命周期:saveAll→findByStatDateBetween→findFirst→deleteByStatDateIn") {
      orderRepo.saveAll(
        listOf(
          DailyOrderFactModel(
            statDate = date,
            status = "COMPLETED",
            deliveryWorkerId = 7L,
            orderCount = 3,
            amountCents = 900L,
            itemQuantity = 5,
          ),
        ),
      )
      val rows = orderRepo.findByStatDateBetween(date, date)
      rows shouldHaveSize 1
      rows.first().id.shouldNotBeNullId()
      rows.first().orderCount shouldBe 3L

      orderRepo.findFirstByOrderByStatDateDesc().shouldNotBeNullOrderFact()
      orderRepo.findFirstByOrderByStatDateAsc().shouldNotBeNullOrderFact()
      orderRepo.count() shouldBe 1L

      orderRepo.deleteByStatDateIn(listOf(date)) shouldBe 1L
      orderRepo.findByStatDateBetween(date, date) shouldHaveSize 0
    }

    test("product/user/refund/revenue/marketing/usage fact save 与查询") {
      productRepo.saveAll(
        listOf(
          DailyProductFactModel(statDate = date, status = "COMPLETED", productId = 1L, quantity = 2, revenueCents = 300L, orderCount = 1),
        ),
      )
      productRepo.findByStatDateBetween(date, date).first().quantity shouldBe 2L

      userRepo.saveAll(listOf(DailyUserFactModel(statDate = date, newUserCount = 4, loginUserCount = 6)))
      userRepo.findByStatDateBetween(date, date).first().newUserCount shouldBe 4L

      refundRepo.saveAll(
        listOf(DailyRefundFactModel(statDate = date, status = "SUCCESS", refundType = "FULL", refundCount = 1, refundAmountCents = 50L)),
      )
      refundRepo.findByStatDateBetween(date, date).first().refundAmountCents shouldBe 50L

      revenueRepo.saveAll(listOf(DailyRevenueFactModel(statDate = date, orderCount = 2, amountCents = 800L)))
      revenueRepo.findByStatDateBetween(date, date).first().amountCents shouldBe 800L

      marketingRepo.save(
        DailyMarketingFactModel(
          statDate = date,
          marketingOrderCount = 2,
          couponBenefitCents = 100L,
          promotionBenefitCents = 0L,
          firstOrderBenefitCents = 0L,
          couponRedeemedCount = 1,
          ticketSalesCount = 0L,
          ticketSalesRevenueCents = 0L,
          ticketRedeemedCount = 0L,
          giftQuantity = 0L,
        ),
      )
      marketingRepo.findFirstByOrderByStatDateDesc()!!.couponBenefitCents shouldBe 100L

      usageRepo.saveAll(
        listOf(
          DailyMarketingUsageFactModel(
            statDate = date,
            kind = "COUPON_TEMPLATE",
            refId = 9L,
            orderCount = 2,
            benefitCents = 100L,
            usageCount = 1,
            ticketSalesCount = 0L,
            ticketSalesRevenueCents = 0L,
            ticketRedeemedCount = 0L,
          ),
        ),
      )
      usageRepo.findByStatDateBetween(date, date).first().refId shouldBe 9L

      // 空日期查询
      usageRepo.findByStatDateBetween(date.plusDays(9), date.plusDays(9)) shouldHaveSize 0
      userRepo.findFirstByOrderByStatDateAsc().shouldNotBeNullUserFact()
      // 无 asc 实现的仓储也应返回最近一条
      productRepo.findFirstByOrderByStatDateDesc()!!.statDate shouldBe date
      revenueRepo.findFirstByOrderByStatDateDesc()!!.statDate shouldBe date
      refundRepo.findFirstByOrderByStatDateDesc()!!.statDate shouldBe date
      marketingRepo.findByStatDateBetween(date.plusDays(9), date.plusDays(9)) shouldHaveSize 0
    }
  })

private fun Long?.shouldNotBeNullId(): Long = this ?: throw AssertionError("expected snowflake id backfilled")

private fun DailyOrderFactModel?.shouldNotBeNullOrderFact(): DailyOrderFactModel = this ?: throw AssertionError("expected non-null")

private fun DailyUserFactModel?.shouldNotBeNullUserFact(): DailyUserFactModel = this ?: throw AssertionError("expected non-null")
