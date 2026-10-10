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

package dev.yidafu.aqua.marketing.coupon.domain.repository

import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 用户优惠券实例仓储 Exposed 实现测试(Testcontainers 真实 PG):save/软删/派生查询/分页/核销与退回原子更新。
 */
class UserCouponRepositoryImplTest :
  FunSpec({
    val repo = UserCouponRepositoryImpl(MarketingPgTestSupport.database)

    beforeTest { reset() }

    test("save 新增回填 id,分页窗口与 findById 可查") {
      val saved =
        repo.save(
          UserCouponModel(userId = 100L, couponTemplateId = 1L, name = "新人券"),
        )
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .name shouldBe "新人券"
    }

    test("save 更新已有实体走 update 分支") {
      val id =
        MarketingPgTestSupport.userCoupon(userId = 100L, couponTemplateId = 1L, status = "RECEIVED")
      val loaded = repo.findById(id).orElse(null).shouldNotBeNull()
      loaded.status = UserCouponStatus.USED
      loaded.usedOrderId = 555L
      loaded.usedAt = LocalDateTime.now()
      repo.save(loaded)

      val reloaded = repo.findById(id).orElse(null).shouldNotBeNull()
      reloaded.status shouldBe UserCouponStatus.USED
      reloaded.usedOrderId shouldBe 555L
    }

    test("软删券不可见:各派生查询均过滤") {
      MarketingPgTestSupport.userCoupon(userId = 100L, couponTemplateId = 1L, status = "RECEIVED")
      MarketingPgTestSupport.userCoupon(userId = 100L, couponTemplateId = 1L, status = "RECEIVED", deleted = true)
      MarketingPgTestSupport.userCoupon(userId = 101L, couponTemplateId = 1L, status = "USED", usedOrderId = 9L)

      repo.findByUserId(100L) shouldHaveSize 1
      repo.findByUserIdAndStatus(100L, UserCouponStatus.RECEIVED) shouldHaveSize 1
      repo.findByUserIdAndStatusIn(100L, listOf(UserCouponStatus.RECEIVED, UserCouponStatus.USED)) shouldHaveSize 1
      repo.countByUserIdAndCouponTemplateId(100L, 1L) shouldBe 1L
      repo.countByCouponTemplateIdAndStatus(1L, UserCouponStatus.RECEIVED) shouldBe 1L
      // 软删 USED 行不计入
      repo.findByUserIdAndStatusIn(101L, listOf(UserCouponStatus.USED)) shouldHaveSize 1
    }

    test("findByUserIdAndStatusIn 空列表返回空") {
      repo.findByUserIdAndStatusIn(100L, emptyList()) shouldHaveSize 0
    }

    test("findByStatusAndExpireTimeBetween:[from,to) 分页 + id 破平") {
      val base = LocalDateTime.of(2026, 4, 10, 0, 0)
      val in1 = MarketingPgTestSupport.userCoupon(userId = 1L, couponTemplateId = 1L, status = "RECEIVED", expireTime = base)
      val in2 = MarketingPgTestSupport.userCoupon(userId = 2L, couponTemplateId = 1L, status = "RECEIVED", expireTime = base)
      // 边界:等于 to 不含;等于 from 含
      MarketingPgTestSupport.userCoupon(
        userId = 3L,
        couponTemplateId = 1L,
        status = "RECEIVED",
        expireTime = LocalDateTime.of(2026, 4, 20, 0, 0),
      )
      val bFrom =
        MarketingPgTestSupport.userCoupon(
          userId = 4L,
          couponTemplateId = 1L,
          status = "RECEIVED",
          expireTime = LocalDateTime.of(2026, 4, 1, 0, 0),
        )
      // 其他状态不进
      MarketingPgTestSupport.userCoupon(userId = 5L, couponTemplateId = 1L, status = "USED", expireTime = base, usedOrderId = 1L)

      val from = LocalDateTime.of(2026, 4, 1, 0, 0)
      val to = LocalDateTime.of(2026, 4, 20, 0, 0)
      val page0 = repo.findByStatusAndExpireTimeBetween(UserCouponStatus.RECEIVED, from, to, PageRequest.of(0, 2))
      page0.totalElements shouldBe 3L
      page0.content shouldHaveSize 2
      val page1 = repo.findByStatusAndExpireTimeBetween(UserCouponStatus.RECEIVED, from, to, PageRequest.of(1, 2))
      page1.content shouldHaveSize 1
      // 跨页合集 = 全部三张(id 升序,不重不漏)
      (page0.content + page1.content).map { it.id } shouldContainExactly listOf(in1, in2, bFrom).sorted()
      // 空窗口
      repo
        .findByStatusAndExpireTimeBetween(
          UserCouponStatus.RECEIVED,
          LocalDateTime.of(2027, 1, 1, 0, 0),
          LocalDateTime.of(2027, 2, 1, 0, 0),
          PageRequest.of(0, 10),
        ).totalElements shouldBe 0L
    }

    test("redeemAtomically:命中条件才核销,二次核销/条件不符返回 0") {
      val id = MarketingPgTestSupport.userCoupon(userId = 100L, couponTemplateId = 1L, status = "RECEIVED")
      val now = LocalDateTime.now()

      repo.redeemAtomically(id, 100L, 777L, UserCouponStatus.RECEIVED, UserCouponStatus.USED, now) shouldBe 1
      val used = repo.findById(id).orElse(null).shouldNotBeNull()
      used.status shouldBe UserCouponStatus.USED
      used.usedOrderId shouldBe 777L
      used.usedAt shouldBe now

      // 已是 USED → from 不匹配
      repo.redeemAtomically(id, 100L, 778L, UserCouponStatus.RECEIVED, UserCouponStatus.USED, now) shouldBe 0
      // 用户不匹配
      val id2 = MarketingPgTestSupport.userCoupon(userId = 100L, couponTemplateId = 1L)
      repo.redeemAtomically(id2, 999L, 779L, UserCouponStatus.RECEIVED, UserCouponStatus.USED, now) shouldBe 0
    }

    test("redeemAtomically:已过期券不可核销") {
      val id =
        MarketingPgTestSupport.userCoupon(
          userId = 100L,
          couponTemplateId = 1L,
          expireTime = LocalDateTime.now().minusMinutes(1),
        )
      repo.redeemAtomically(id, 100L, 1L, UserCouponStatus.RECEIVED, UserCouponStatus.USED, LocalDateTime.now()) shouldBe 0
    }

    test("redeemAtomically:软删券不可核销") {
      val id = MarketingPgTestSupport.userCoupon(userId = 100L, couponTemplateId = 1L, deleted = true)
      repo.redeemAtomically(id, 100L, 1L, UserCouponStatus.RECEIVED, UserCouponStatus.USED, LocalDateTime.now()) shouldBe 0
    }

    test("returnByOrder:未过期→RETURNED,已过期→EXPIRED,清 used 字段") {
      val now = LocalDateTime.now()
      val unexpired =
        MarketingPgTestSupport.userCoupon(
          userId = 1L,
          couponTemplateId = 1L,
          status = "USED",
          usedOrderId = 42L,
          usedAt = now.minusDays(1),
          expireTime = now.plusDays(3),
        )
      val expired =
        MarketingPgTestSupport.userCoupon(
          userId = 2L,
          couponTemplateId = 1L,
          status = "USED",
          usedOrderId = 42L,
          usedAt = now.minusDays(10),
          expireTime = now.minusDays(1),
        )
      // 非本订单不动
      val other =
        MarketingPgTestSupport.userCoupon(
          userId = 3L,
          couponTemplateId = 1L,
          status = "USED",
          usedOrderId = 43L,
          expireTime = now.plusDays(3),
        )

      repo.returnByOrder(42L, now) shouldBe 2

      val r = repo.findById(unexpired).orElse(null).shouldNotBeNull()
      r.status shouldBe UserCouponStatus.RETURNED
      r.usedOrderId shouldBe null
      r.usedAt shouldBe null
      r.returnedAt shouldBe now

      val e = repo.findById(expired).orElse(null).shouldNotBeNull()
      e.status shouldBe UserCouponStatus.EXPIRED
      e.usedOrderId shouldBe null
      e.returnedAt shouldBe now

      repo
        .findById(other)
        .orElse(null)
        .shouldNotBeNull()
        .status shouldBe UserCouponStatus.USED

      // 幂等:二次调用无匹配行
      repo.returnByOrder(42L, now) shouldBe 0
    }

    test("countByCouponTemplateIdAndStatus 对软删行不计数") {
      MarketingPgTestSupport.userCoupon(userId = 1L, couponTemplateId = 1L, status = "RECEIVED")
      MarketingPgTestSupport.userCoupon(userId = 2L, couponTemplateId = 1L, status = "RECEIVED", deleted = true)
      repo.countByCouponTemplateIdAndStatus(1L, UserCouponStatus.RECEIVED) shouldBe 1L
    }

    test("UserCouponModel 构造默认字段经 save 回环") {
      val model =
        UserCouponModel(
          userId = 7L,
          couponTemplateId = 3L,
          name = "回环",
          couponType = CouponType.DISCOUNT,
          scope = CouponScope.PRODUCT,
          productId = 12L,
          thresholdCents = null,
          discountAmountCents = null,
          discountRatePermille = 800L,
        )
      val saved = repo.save(model)
      val back = repo.findById(saved.id!!).orElse(null).shouldNotBeNull()
      back.couponType shouldBe CouponType.DISCOUNT
      back.scope shouldBe CouponScope.PRODUCT
      back.productId shouldBe 12L
      back.discountRatePermille shouldBe 800L
      back.thresholdCents shouldBe null
    }
  })
