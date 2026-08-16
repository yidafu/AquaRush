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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * 日收款/日对账跨模块查询服务单元测试。
 */
class ReconciliationQueryApiServiceImplTest :
  FunSpec({
    var dailyCollectionRecordRepository: DailyCollectionRecordRepository = mockk()
    var dailyReconciliationRepository: DailyReconciliationRepository = mockk()
    var collectionService: DailyCollectionQueryApiServiceImpl = mockk()
    var reconciliationService: DailyReconciliationQueryApiServiceImpl = mockk()

    beforeTest {
      dailyCollectionRecordRepository = mockk(relaxed = true)
      dailyReconciliationRepository = mockk(relaxed = true)
      collectionService = DailyCollectionQueryApiServiceImpl(dailyCollectionRecordRepository)
      reconciliationService = DailyReconciliationQueryApiServiceImpl(dailyReconciliationRepository)
    }
    test("collection queries delegate") {
      val date = LocalDate.of(2025, 1, 1)
      val records = listOf(DailyCollectionRecordModel(collectionDate = date))
      val all = listOf(DailyCollectionRecordModel(collectionDate = date), DailyCollectionRecordModel(collectionDate = date.plusDays(1)))
      every { dailyCollectionRecordRepository.findByCollectionDate(date) } returns records
      every { dailyCollectionRecordRepository.findByCollectionDateBetween(date, date.plusDays(1)) } returns all

      collectionService.findByDate("2025-01-01") shouldBe records
      collectionService.findByDateBetween("2025-01-01", "2025-01-02") shouldBe all
    }
    test("reconciliation queries delegate") {
      val date = LocalDate.of(2025, 1, 1)
      val model = DailyReconciliationModel(reconciliationDate = date)
      every { dailyReconciliationRepository.findByReconciliationDate(date) } returns model
      every { dailyReconciliationRepository.findByReconciliationDateBetween(date, date.plusDays(1)) } returns listOf(model)

      reconciliationService.findByDate("2025-01-01") shouldBe model
      reconciliationService.findByDateBetween("2025-01-01", "2025-01-02") shouldBe listOf(model)

      every { dailyReconciliationRepository.findByReconciliationDate(date.plusDays(2)) } returns null
      reconciliationService.findByDate("2025-01-03").shouldBeNull()
    }
    test("invalid date throws") {
      shouldThrow<DateTimeParseException> { collectionService.findByDate("not-a-date") }
      shouldThrow<DateTimeParseException> { reconciliationService.findByDate("not-a-date") }
      shouldThrow<DateTimeParseException> { collectionService.findByDateBetween("bad", "2025-01-02") }
      verify(exactly = 0) { dailyCollectionRecordRepository.findByCollectionDate(any()) }
    }
  })
