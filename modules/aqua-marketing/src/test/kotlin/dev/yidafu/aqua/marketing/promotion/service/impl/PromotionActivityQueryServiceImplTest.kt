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

package dev.yidafu.aqua.marketing.promotion.service.impl

import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.PromotionActivityTierModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityRepository
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityTierRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.util.Optional

/** 满减/折扣活动查询服务单元测试(F-MKT-004 列表/详情/档位/生效活动)。 */
class PromotionActivityQueryServiceImplTest :
  FunSpec({
    var promotionActivityRepository: PromotionActivityRepository = mockk()
    var promotionActivityTierRepository: PromotionActivityTierRepository = mockk()
    var service: PromotionActivityQueryServiceImpl = mockk()

    beforeTest {
      promotionActivityRepository = mockk()
      promotionActivityTierRepository = mockk()
      service = PromotionActivityQueryServiceImpl(promotionActivityRepository, promotionActivityTierRepository)
    }
    test("listActivities with keyword") {
      val pageable: Pageable = PageRequest.of(0, 10)
      val page: Page<PromotionActivityModel> = PageImpl(listOf(PromotionActivityModel(id = 1L, name = "满减")))
      every { promotionActivityRepository.searchActivities(any(), any()) } returns page

      val result = service.listActivities("满减", pageable)

      result.totalElements shouldBe 1
      verify { promotionActivityRepository.searchActivities(any(), any()) }
    }
    test("listActivities without keyword") {
      val pageable: Pageable = PageRequest.of(0, 10)
      every { promotionActivityRepository.searchActivities(any(), any()) } returns
        PageImpl(emptyList())

      val result = service.listActivities(null, pageable)

      result.totalElements shouldBe 0
    }
    test("getActivity") {
      every { promotionActivityRepository.findById(1L) } returns Optional.of(PromotionActivityModel(id = 1L, name = "满减"))
      every { promotionActivityRepository.findById(99L) } returns Optional.empty()

      service.getActivity(1L)?.id shouldBe 1L
      service.getActivity(99L).shouldBeNull()
    }
    test("listTiers") {
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns
        listOf(PromotionActivityTierModel(id = 1L, activityId = 1L, thresholdCents = 10000, sort = 0))

      service.listTiers(1L).size shouldBe 1
    }
    test("findActiveActivities") {
      val now = LocalDateTime.of(2026, 8, 14, 10, 0)
      every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
        listOf(PromotionActivityModel(id = 1L, name = "进行中"))

      service.findActiveActivities(now).size shouldBe 1
    }
  })
