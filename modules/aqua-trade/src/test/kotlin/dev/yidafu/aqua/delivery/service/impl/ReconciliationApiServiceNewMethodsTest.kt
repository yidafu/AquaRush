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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.util.Optional

class ReconciliationApiServiceNewMethodsTest :
  FunSpec({
    val date = LocalDate.of(2025, 6, 1)
    val pageable = PageRequest.of(0, 10)

    test("DailyCollectionQueryApiServiceImpl 新增查询全部委托仓库") {
      val repo = mockk<DailyCollectionRecordRepository>()
      val impl = DailyCollectionQueryApiServiceImpl(repo)
      val record = mockk<DailyCollectionRecordModel>()
      val page = PageImpl(listOf(record))
      every { repo.findByDeliveryWorkerIdAndCollectionDate(1L, date) } returns record
      every { repo.findByDeliveryWorkerIdAndCollectionDate(1L, date, pageable) } returns page
      every { repo.findByCollectionDate(date) } returns listOf(record)
      every { repo.findByCollectionDate(date, pageable) } returns page
      every { repo.findByDeliveryWorkerId(1L, pageable) } returns page
      every { repo.findByCollectionDateBetween(date, date) } returns listOf(record)
      every { repo.findById(9L) } returns Optional.of(record)
      every { repo.findById(10L) } returns Optional.empty()
      every { repo.findAll(pageable) } returns page

      impl.findByDeliveryWorkerIdAndCollectionDate(1L, date) shouldBe record
      impl.findByDeliveryWorkerIdAndCollectionDate(1L, date, pageable) shouldBe page
      impl.findByCollectionDate(date) shouldBe listOf(record)
      impl.findByCollectionDate(date, pageable) shouldBe page
      impl.findByDeliveryWorkerId(1L, pageable) shouldBe page
      impl.findByCollectionDateBetween(date, date) shouldBe listOf(record)
      impl.findById(9L) shouldBe record
      impl.findById(10L).shouldBeNull()
      impl.findAll(pageable) shouldBe page
    }

    test("DailyReconciliationQueryApiServiceImpl 新增查询全部委托仓库") {
      val repo = mockk<DailyReconciliationRepository>()
      val impl = DailyReconciliationQueryApiServiceImpl(repo)
      val model = mockk<DailyReconciliationModel>()
      val page = PageImpl(listOf(model))
      every { repo.findByReconciliationDate(date) } returns model
      every { repo.findById(9L) } returns Optional.of(model)
      every { repo.findById(10L) } returns Optional.empty()
      every { repo.findAll(pageable) } returns page

      impl.findByReconciliationDate(date) shouldBe model
      impl.findById(9L) shouldBe model
      impl.findById(10L).shouldBeNull()
      impl.findAll(pageable) shouldBe page
    }

    test("写服务实现委托仓库 save") {
      val recordRepo = mockk<DailyCollectionRecordRepository>()
      val reconRepo = mockk<DailyReconciliationRepository>()
      val record = mockk<DailyCollectionRecordModel>()
      val model = mockk<DailyReconciliationModel>()
      every { recordRepo.save(record) } returns record
      every { reconRepo.save(model) } returns model

      DailyCollectionMutationServiceImpl(recordRepo).save(record) shouldBe record
      DailyReconciliationMutationServiceImpl(reconRepo).save(model) shouldBe model
    }
  })
