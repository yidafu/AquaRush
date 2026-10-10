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
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

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

  /**
   * 送水员指定日收款日报（跨模块数据访问统一走 api/service —— ArchUnit P1）
   */
  fun findByDeliveryWorkerIdAndCollectionDate(
    deliveryWorkerId: Long,
    collectionDate: LocalDate,
  ): DailyCollectionRecordModel?

  fun findByDeliveryWorkerIdAndCollectionDate(
    deliveryWorkerId: Long,
    collectionDate: LocalDate,
    pageable: Pageable,
  ): Page<DailyCollectionRecordModel>

  fun findByCollectionDate(collectionDate: LocalDate): List<DailyCollectionRecordModel>

  fun findByCollectionDate(
    collectionDate: LocalDate,
    pageable: Pageable,
  ): Page<DailyCollectionRecordModel>

  fun findByDeliveryWorkerId(
    deliveryWorkerId: Long,
    pageable: Pageable,
  ): Page<DailyCollectionRecordModel>

  fun findByCollectionDateBetween(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyCollectionRecordModel>

  fun findById(id: Long): DailyCollectionRecordModel?

  fun findAll(pageable: Pageable): Page<DailyCollectionRecordModel>
}

interface DailyReconciliationQueryApiService {
  fun findByDate(date: String): DailyReconciliationModel?

  fun findByDateBetween(
    startDate: String,
    endDate: String,
  ): List<DailyReconciliationModel>

  fun findByReconciliationDate(reconciliationDate: LocalDate): DailyReconciliationModel?

  fun findById(id: Long): DailyReconciliationModel?

  fun findAll(pageable: Pageable): Page<DailyReconciliationModel>
}
