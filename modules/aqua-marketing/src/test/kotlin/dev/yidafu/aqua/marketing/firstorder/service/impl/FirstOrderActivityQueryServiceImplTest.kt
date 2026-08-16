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

package dev.yidafu.aqua.marketing.firstorder.service.impl

import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.marketing.firstorder.domain.repository.FirstOrderActivityRepository
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
import org.springframework.data.jpa.domain.Specification
import java.util.Optional

/** 首单立减/新人专享价活动查询服务单元测试(F-MKT-009 列表/详情)。 */
class FirstOrderActivityQueryServiceImplTest :
  FunSpec({
    var firstOrderActivityRepository: FirstOrderActivityRepository = mockk()
    var service: FirstOrderActivityQueryServiceImpl = mockk()

    beforeTest {
      firstOrderActivityRepository = mockk()
      service = FirstOrderActivityQueryServiceImpl(firstOrderActivityRepository)
    }
    test("listActivities with keyword") {
      val pageable: Pageable = PageRequest.of(0, 10)
      val page: Page<FirstOrderActivityModel> = PageImpl(listOf(FirstOrderActivityModel(id = 1L, name = "首单")))
      every { firstOrderActivityRepository.findAll(any<Specification<FirstOrderActivityModel>>(), any<Pageable>()) } returns page

      val result = service.listActivities("首单", pageable)

      result.totalElements shouldBe 1
      verify { firstOrderActivityRepository.findAll(any<Specification<FirstOrderActivityModel>>(), any<Pageable>()) }
    }
    test("listActivities without keyword") {
      val pageable: Pageable = PageRequest.of(0, 10)
      every { firstOrderActivityRepository.findAll(any<Specification<FirstOrderActivityModel>>(), any<Pageable>()) } returns
        PageImpl(emptyList())

      val result = service.listActivities(null, pageable)

      result.totalElements shouldBe 0
    }
    test("getActivity") {
      every { firstOrderActivityRepository.findById(1L) } returns
        Optional.of(FirstOrderActivityModel(id = 1L, name = "首单", activityType = FirstOrderActivityType.FIRST_ORDER_REDUCE))
      every { firstOrderActivityRepository.findById(99L) } returns Optional.empty()

      service.getActivity(1L)?.id shouldBe 1L
      service.getActivity(99L).shouldBeNull()
    }
  })
