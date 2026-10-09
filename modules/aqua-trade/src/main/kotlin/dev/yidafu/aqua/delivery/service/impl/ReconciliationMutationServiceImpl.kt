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

import dev.yidafu.aqua.api.service.reconciliation.DailyCollectionMutationService
import dev.yidafu.aqua.api.service.reconciliation.DailyReconciliationMutationService
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.delivery.domain.repository.DailyCollectionRecordRepository
import dev.yidafu.aqua.delivery.domain.repository.DailyReconciliationRepository
import org.springframework.stereotype.Service

/**
 * 跨模块对账写服务实现（aqua-trade 持有仓储，供 aqua-analytics 经 api/service 调用）
 */
@Service
class DailyCollectionMutationServiceImpl(
  private val dailyCollectionRecordRepository: DailyCollectionRecordRepository,
) : DailyCollectionMutationService {
  override fun save(record: DailyCollectionRecordModel): DailyCollectionRecordModel = dailyCollectionRecordRepository.save(record)
}

@Service
class DailyReconciliationMutationServiceImpl(
  private val dailyReconciliationRepository: DailyReconciliationRepository,
) : DailyReconciliationMutationService {
  override fun save(model: DailyReconciliationModel): DailyReconciliationModel = dailyReconciliationRepository.save(model)
}
