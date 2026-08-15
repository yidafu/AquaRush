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

package dev.yidafu.aqua.marketing.gift.service.impl

import dev.yidafu.aqua.common.domain.model.GiftActivityModel
import dev.yidafu.aqua.common.domain.model.enums.GiftActivityType
import dev.yidafu.aqua.marketing.gift.domain.repository.GiftActivityRepository
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

/** 买赠/满赠活动查询服务单元测试(F-MKT-008 列表/详情)。 */
class GiftActivityQueryServiceImplTest {
  private lateinit var giftActivityRepository: GiftActivityRepository
  private lateinit var service: GiftActivityQueryServiceImpl

  @BeforeEach
  fun setUp() {
    giftActivityRepository = mockk()
    service = GiftActivityQueryServiceImpl(giftActivityRepository)
  }

  @Test
  @DisplayName("列表:带关键字")
  fun `listActivities with keyword`() {
    val pageable: Pageable = PageRequest.of(0, 10)
    val page: Page<GiftActivityModel> = PageImpl(listOf(GiftActivityModel(id = 1L, name = "满赠", giftType = GiftActivityType.FULL_GIFT)))
    every { giftActivityRepository.findAll(any<Specification<GiftActivityModel>>(), any<Pageable>()) } returns page

    val result = service.listActivities("满赠", pageable)

    assertEquals(1, result.totalElements)
    verify { giftActivityRepository.findAll(any<Specification<GiftActivityModel>>(), any<Pageable>()) }
  }

  @Test
  @DisplayName("列表:无关键字")
  fun `listActivities without keyword`() {
    val pageable: Pageable = PageRequest.of(0, 10)
    every { giftActivityRepository.findAll(any<Specification<GiftActivityModel>>(), any<Pageable>()) } returns PageImpl(emptyList())

    val result = service.listActivities(null, pageable)

    assertEquals(0, result.totalElements)
  }

  @Test
  @DisplayName("详情:存在返回,不存在返回 null")
  fun `getActivity`() {
    every { giftActivityRepository.findById(1L) } returns
      Optional.of(GiftActivityModel(id = 1L, name = "满赠", giftType = GiftActivityType.FULL_GIFT))
    every { giftActivityRepository.findById(99L) } returns Optional.empty()

    assertEquals(1L, service.getActivity(1L)?.id)
    assertNull(service.getActivity(99L))
  }
}
