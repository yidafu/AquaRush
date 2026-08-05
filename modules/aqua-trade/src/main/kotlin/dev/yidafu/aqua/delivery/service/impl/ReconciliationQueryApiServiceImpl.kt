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

import dev.yidafu.aqua.api.service.reconciliation.DailyCollectionQueryApiService
import dev.yidafu.aqua.api.service.reconciliation.DailyReconciliationQueryApiService
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.delivery.domain.repository.DailyCollectionRecordRepository
import dev.yidafu.aqua.delivery.domain.repository.DailyReconciliationRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 跨模块对账查询服务实现
 * 供其他模块使用
 */
@Service
class DailyCollectionQueryApiServiceImpl(
  private val dailyCollectionRecordRepository: DailyCollectionRecordRepository,
) : DailyCollectionQueryApiService {
  override fun findByDate(date: String): List<DailyCollectionRecordModel> {
    val localDate = LocalDate.parse(date)
    return dailyCollectionRecordRepository.findByCollectionDate(localDate)
  }

  override fun findByDateBetween(
    startDate: String,
    endDate: String,
  ): List<DailyCollectionRecordModel> {
    val start = LocalDate.parse(startDate)
    val end = LocalDate.parse(endDate)
    return dailyCollectionRecordRepository.findByCollectionDateBetween(start, end)
  }
}

@Service
class DailyReconciliationQueryApiServiceImpl(
  private val dailyReconciliationRepository: DailyReconciliationRepository,
) : DailyReconciliationQueryApiService {
  override fun findByDate(date: String): DailyReconciliationModel? {
    val localDate = LocalDate.parse(date)
    return dailyReconciliationRepository.findByReconciliationDate(localDate)
  }

  override fun findByDateBetween(
    startDate: String,
    endDate: String,
  ): List<DailyReconciliationModel> {
    val start = LocalDate.parse(startDate)
    val end = LocalDate.parse(endDate)
    return dailyReconciliationRepository.findByReconciliationDateBetween(start, end)
  }
}
