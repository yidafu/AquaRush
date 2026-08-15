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

package dev.yidafu.aqua.delivery.service.impl

import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.delivery.domain.repository.DailyCollectionRecordRepository
import dev.yidafu.aqua.delivery.domain.repository.DailyReconciliationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * 日收款/日对账跨模块查询服务单元测试。
 */
class ReconciliationQueryApiServiceImplTest {
  private lateinit var dailyCollectionRecordRepository: DailyCollectionRecordRepository
  private lateinit var dailyReconciliationRepository: DailyReconciliationRepository
  private lateinit var collectionService: DailyCollectionQueryApiServiceImpl
  private lateinit var reconciliationService: DailyReconciliationQueryApiServiceImpl

  @BeforeEach
  fun setUp() {
    dailyCollectionRecordRepository = mockk(relaxed = true)
    dailyReconciliationRepository = mockk(relaxed = true)
    collectionService = DailyCollectionQueryApiServiceImpl(dailyCollectionRecordRepository)
    reconciliationService = DailyReconciliationQueryApiServiceImpl(dailyReconciliationRepository)
  }

  @Test
  @DisplayName("日收款:按日期与区间查询")
  fun `collection queries delegate`() {
    val date = LocalDate.of(2025, 1, 1)
    val records = listOf(DailyCollectionRecordModel(collectionDate = date))
    val all = listOf(DailyCollectionRecordModel(collectionDate = date), DailyCollectionRecordModel(collectionDate = date.plusDays(1)))
    every { dailyCollectionRecordRepository.findByCollectionDate(date) } returns records
    every { dailyCollectionRecordRepository.findByCollectionDateBetween(date, date.plusDays(1)) } returns all

    assertEquals(records, collectionService.findByDate("2025-01-01"))
    assertEquals(all, collectionService.findByDateBetween("2025-01-01", "2025-01-02"))
  }

  @Test
  @DisplayName("日对账:按日期与区间查询,未命中返回 null")
  fun `reconciliation queries delegate`() {
    val date = LocalDate.of(2025, 1, 1)
    val model = DailyReconciliationModel(reconciliationDate = date)
    every { dailyReconciliationRepository.findByReconciliationDate(date) } returns model
    every { dailyReconciliationRepository.findByReconciliationDateBetween(date, date.plusDays(1)) } returns listOf(model)

    assertEquals(model, reconciliationService.findByDate("2025-01-01"))
    assertEquals(listOf(model), reconciliationService.findByDateBetween("2025-01-01", "2025-01-02"))

    every { dailyReconciliationRepository.findByReconciliationDate(date.plusDays(2)) } returns null
    assertNull(reconciliationService.findByDate("2025-01-03"))
  }

  @Test
  @DisplayName("非法日期抛 DateTimeParseException")
  fun `invalid date throws`() {
    assertFailsWith<DateTimeParseException> { collectionService.findByDate("not-a-date") }
    assertFailsWith<DateTimeParseException> { reconciliationService.findByDate("not-a-date") }
    assertFailsWith<DateTimeParseException> { collectionService.findByDateBetween("bad", "2025-01-02") }
    verify(exactly = 0) { dailyCollectionRecordRepository.findByCollectionDate(any()) }
  }
}
