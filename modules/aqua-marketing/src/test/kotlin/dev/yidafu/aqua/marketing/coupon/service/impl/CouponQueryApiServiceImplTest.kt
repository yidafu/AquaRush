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

package dev.yidafu.aqua.marketing.coupon.service.impl

import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.marketing.coupon.domain.repository.CouponTemplateRepository
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 优惠券用户侧查询服务单元测试(领券中心/我的券包/可用券)。 */
class CouponQueryApiServiceImplTest {
  private lateinit var couponTemplateRepository: CouponTemplateRepository
  private lateinit var userCouponRepository: UserCouponRepository
  private lateinit var service: CouponQueryApiServiceImpl

  @BeforeEach
  fun setUp() {
    couponTemplateRepository = mockk()
    userCouponRepository = mockk()
    service = CouponQueryApiServiceImpl(couponTemplateRepository, userCouponRepository)
  }

  private fun template(
    id: Long,
    issuanceType: CouponIssuanceType,
  ): CouponTemplateModel =
    CouponTemplateModel(
      id = id,
      name = "券$id",
      issuanceType = issuanceType,
      status = CouponTemplateStatus.ACTIVE,
    )

  private fun coupon(
    id: Long,
    expireTime: LocalDateTime,
    status: UserCouponStatus = UserCouponStatus.RECEIVED,
  ): UserCouponModel =
    UserCouponModel(
      id = id,
      userId = 1L,
      couponTemplateId = 1L,
      status = status,
      expireTime = expireTime,
    )

  @Test
  @DisplayName("领券中心:仅 ACTIVE+AUTO_CLAIM(AC-MKT-003-07)")
  fun `findClaimableTemplates filters`() {
    every { couponTemplateRepository.findByStatus(CouponTemplateStatus.ACTIVE) } returns
      listOf(template(1L, CouponIssuanceType.AUTO_CLAIM), template(2L, CouponIssuanceType.TARGETED))

    val result = service.findClaimableTemplates(1L)

    assertEquals(1, result.size)
    assertEquals(1L, result[0].id)
  }

  @Test
  @DisplayName("我的券包:无状态=全部,有状态=按状态过滤")
  fun `findUserCoupons by status`() {
    every { userCouponRepository.findByUserId(1L) } returns listOf(coupon(1L, LocalDateTime.now().plusDays(1)))
    every { userCouponRepository.findByUserIdAndStatus(1L, UserCouponStatus.USED) } returns
      listOf(coupon(2L, LocalDateTime.now().plusDays(1), UserCouponStatus.USED))

    assertEquals(1, service.findUserCoupons(1L, null).size)
    assertEquals(1, service.findUserCoupons(1L, UserCouponStatus.USED).size)
  }

  @Test
  @DisplayName("可用券:仅未过期 RECEIVED/RETURNED")
  fun `findUsableCoupons filters expired`() {
    val now = LocalDateTime.now()
    every {
      userCouponRepository.findByUserIdAndStatusIn(1L, listOf(UserCouponStatus.RECEIVED, UserCouponStatus.RETURNED))
    } returns
      listOf(
        coupon(1L, now.plusDays(1)),
        coupon(2L, now.minusDays(1)),
        coupon(3L, now.plusDays(1), UserCouponStatus.RETURNED),
      )

    val result = service.findUsableCoupons(1L)

    assertEquals(listOf(1L, 3L), result.map { it.id })
  }

  @Test
  @DisplayName("按用户查券:券归属非本人 → null")
  fun `findUserCouponByIdAndUser checks ownership`() {
    every { userCouponRepository.findById(1L) } returns
      Optional.of(UserCouponModel(id = 1L, userId = 9L, couponTemplateId = 1L, expireTime = LocalDateTime.now().plusDays(1)))
    every { userCouponRepository.findById(2L) } returns Optional.of(coupon(2L, LocalDateTime.now().plusDays(1)))
    every { userCouponRepository.findById(3L) } returns Optional.empty()

    assertNull(service.findUserCouponByIdAndUser(1L, 1L))
    assertEquals(2L, service.findUserCouponByIdAndUser(1L, 2L)?.id)
    assertNull(service.findUserCouponByIdAndUser(1L, 3L))
  }

  @Test
  @DisplayName("用户已领某模板数量")
  fun `countUserCouponsByTemplate delegates`() {
    every { userCouponRepository.countByUserIdAndCouponTemplateId(1L, 5L) } returns 2

    assertEquals(2, service.countUserCouponsByTemplate(1L, 5L))
  }
}
