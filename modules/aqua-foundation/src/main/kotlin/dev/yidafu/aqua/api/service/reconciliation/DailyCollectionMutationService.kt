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

package dev.yidafu.aqua.api.service.reconciliation

import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel

/**
 * 跨模块收款日报写服务（新接口遵守 AGENTS 规则4：*MutationService 后缀，不用 *ApiService）
 */
interface DailyCollectionMutationService {
  fun save(record: DailyCollectionRecordModel): DailyCollectionRecordModel
}

/**
 * 跨模块每日对账记录写服务
 */
interface DailyReconciliationMutationService {
  fun save(model: DailyReconciliationModel): DailyReconciliationModel
}
