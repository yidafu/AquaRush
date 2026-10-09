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

package dev.yidafu.aqua.marketing.flashsale.service.impl

import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import dev.yidafu.aqua.marketing.flashsale.domain.repository.FlashSaleActivityRepository
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
import java.util.Optional

/** 限时特价活动查询服务单元测试(F-MKT-005 列表/详情)。 */
class FlashSaleActivityQueryServiceImplTest :
  FunSpec({
    var flashSaleActivityRepository: FlashSaleActivityRepository = mockk()
    var service: FlashSaleActivityQueryServiceImpl = mockk()

    beforeTest {
      flashSaleActivityRepository = mockk()
      service = FlashSaleActivityQueryServiceImpl(flashSaleActivityRepository)
    }
    test("listActivities with keyword") {
      val pageable: Pageable = PageRequest.of(0, 10)
      val page: Page<FlashSaleActivityModel> = PageImpl(listOf(FlashSaleActivityModel(id = 1L, name = "特价")))
      every { flashSaleActivityRepository.searchActivities(any(), any()) } returns page

      val result = service.listActivities("特价", pageable)

      result.totalElements shouldBe 1
      verify { flashSaleActivityRepository.searchActivities(any(), any()) }
    }
    test("listActivities without keyword") {
      val pageable: Pageable = PageRequest.of(0, 10)
      every { flashSaleActivityRepository.searchActivities(any(), any()) } returns
        PageImpl(emptyList())

      val result = service.listActivities(null, pageable)

      result.totalElements shouldBe 0
    }
    test("getActivity") {
      every { flashSaleActivityRepository.findById(1L) } returns Optional.of(FlashSaleActivityModel(id = 1L, name = "特价"))
      every { flashSaleActivityRepository.findById(99L) } returns Optional.empty()

      service.getActivity(1L)?.id shouldBe 1L
      service.getActivity(99L).shouldBeNull()
    }
  })
