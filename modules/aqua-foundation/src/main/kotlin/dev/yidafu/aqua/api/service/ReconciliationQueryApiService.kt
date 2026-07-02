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

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel

/**
 * 跨模块对账查询服务接口
 * 由 aqua-delivery 模块实现，供其他模块使用
 */
interface DailyCollectionQueryApiService {
  fun findByDate(date: String): List<DailyCollectionRecordModel>

  fun findByDateBetween(
    startDate: String,
    endDate: String,
  ): List<DailyCollectionRecordModel>
}

interface DailyReconciliationQueryApiService {
  fun findByDate(date: String): DailyReconciliationModel?

  fun findByDateBetween(
    startDate: String,
    endDate: String,
  ): List<DailyReconciliationModel>
}
