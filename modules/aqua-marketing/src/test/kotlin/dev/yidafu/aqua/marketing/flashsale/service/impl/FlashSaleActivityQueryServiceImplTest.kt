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

/** 限时特价活动查询服务单元测试(F-MKT-005 列表/详情)。 */
class FlashSaleActivityQueryServiceImplTest {
  private lateinit var flashSaleActivityRepository: FlashSaleActivityRepository
  private lateinit var service: FlashSaleActivityQueryServiceImpl

  @BeforeEach
  fun setUp() {
    flashSaleActivityRepository = mockk()
    service = FlashSaleActivityQueryServiceImpl(flashSaleActivityRepository)
  }

  @Test
  @DisplayName("列表:带关键字")
  fun `listActivities with keyword`() {
    val pageable: Pageable = PageRequest.of(0, 10)
    val page: Page<FlashSaleActivityModel> = PageImpl(listOf(FlashSaleActivityModel(id = 1L, name = "特价")))
    every { flashSaleActivityRepository.findAll(any<Specification<FlashSaleActivityModel>>(), any<Pageable>()) } returns page

    val result = service.listActivities("特价", pageable)

    assertEquals(1, result.totalElements)
    verify { flashSaleActivityRepository.findAll(any<Specification<FlashSaleActivityModel>>(), any<Pageable>()) }
  }

  @Test
  @DisplayName("列表:无关键字")
  fun `listActivities without keyword`() {
    val pageable: Pageable = PageRequest.of(0, 10)
    every { flashSaleActivityRepository.findAll(any<Specification<FlashSaleActivityModel>>(), any<Pageable>()) } returns
      PageImpl(emptyList())

    val result = service.listActivities(null, pageable)

    assertEquals(0, result.totalElements)
  }

  @Test
  @DisplayName("详情:存在返回,不存在返回 null")
  fun `getActivity`() {
    every { flashSaleActivityRepository.findById(1L) } returns Optional.of(FlashSaleActivityModel(id = 1L, name = "特价"))
    every { flashSaleActivityRepository.findById(99L) } returns Optional.empty()

    assertEquals(1L, service.getActivity(1L)?.id)
    assertNull(service.getActivity(99L))
  }
}
