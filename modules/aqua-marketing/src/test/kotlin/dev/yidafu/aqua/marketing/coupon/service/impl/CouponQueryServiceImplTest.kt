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
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.marketing.coupon.domain.repository.CouponTemplateRepository
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 优惠券模板查询服务单元测试(F-MKT-003 列表/统计)。 */
class CouponQueryServiceImplTest {
  private lateinit var couponTemplateRepository: CouponTemplateRepository
  private lateinit var userCouponRepository: UserCouponRepository
  private lateinit var service: CouponQueryServiceImpl

  @BeforeEach
  fun setUp() {
    couponTemplateRepository = mockk()
    userCouponRepository = mockk()
    service = CouponQueryServiceImpl(couponTemplateRepository, userCouponRepository)
  }

  private fun template(id: Long): CouponTemplateModel =
    CouponTemplateModel(
      id = id,
      name = "券$id",
      status = CouponTemplateStatus.ACTIVE,
    )

  @Test
  @DisplayName("列表:带关键字与状态过滤")
  fun `listTemplates with keyword and status`() {
    val pageable: Pageable = PageRequest.of(0, 10)
    val page: Page<CouponTemplateModel> = PageImpl(listOf(template(1L)))
    every {
      couponTemplateRepository.findAll(any<Specification<CouponTemplateModel>>(), any<Pageable>())
    } returns page

    val result = service.listTemplates("券", CouponTemplateStatus.ACTIVE, pageable)

    assertEquals(1, result.totalElements)
    verify { couponTemplateRepository.findAll(any<Specification<CouponTemplateModel>>(), any<Pageable>()) }
  }

  @Test
  @DisplayName("列表:无关键字无状态(全量)")
  fun `listTemplates without filters`() {
    val pageable: Pageable = PageRequest.of(0, 10)
    every { couponTemplateRepository.findAll(any<Specification<CouponTemplateModel>>(), any<Pageable>()) } returns PageImpl(emptyList())

    val result = service.listTemplates(null, null, pageable)

    assertEquals(0, result.totalElements)
  }

  @Test
  @DisplayName("详情:存在返回,不存在返回 null")
  fun `getTemplate found and missing`() {
    every { couponTemplateRepository.findById(1L) } returns Optional.of(template(1L))
    every { couponTemplateRepository.findById(99L) } returns Optional.empty()

    assertEquals(1L, service.getTemplate(1L)?.id)
    assertNull(service.getTemplate(99L))
  }

  @Test
  @DisplayName("统计:已领未用数=RECEIVED+RETURNED,已核销数=USED")
  fun `receivedCount and usedCount`() {
    every { userCouponRepository.countByCouponTemplateIdAndStatus(1L, UserCouponStatus.RECEIVED) } returns 5
    every { userCouponRepository.countByCouponTemplateIdAndStatus(1L, UserCouponStatus.RETURNED) } returns 2
    every { userCouponRepository.countByCouponTemplateIdAndStatus(1L, UserCouponStatus.USED) } returns 3

    assertEquals(7, service.receivedCount(1L))
    assertEquals(3, service.usedCount(1L))
    verify { userCouponRepository.countByCouponTemplateIdAndStatus(1L, UserCouponStatus.RECEIVED) }
    verify { userCouponRepository.countByCouponTemplateIdAndStatus(1L, UserCouponStatus.RETURNED) }
    verify { userCouponRepository.countByCouponTemplateIdAndStatus(1L, UserCouponStatus.USED) }
  }
}
