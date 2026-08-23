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

package dev.yidafu.aqua.delivery.domain.repository

import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface DailyCollectionRecordRepository : JpaRepository<DailyCollectionRecordModel, Long> {
  fun findByDeliveryWorkerIdAndCollectionDate(
    deliveryWorkerId: Long,
    collectionDate: LocalDate,
  ): DailyCollectionRecordModel?

  fun findByDeliveryWorkerIdAndCollectionDateBetween(
    deliveryWorkerId: Long,
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyCollectionRecordModel>

  fun findByCollectionDateBetween(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyCollectionRecordModel>

  fun findByCollectionDate(collectionDate: LocalDate): List<DailyCollectionRecordModel>

  /** 按收款日期分页(后台收银对账列表,CLAUDE.md 规则 13 禁止全量查询) */
  fun findByCollectionDate(
    collectionDate: LocalDate,
    pageable: Pageable,
  ): Page<DailyCollectionRecordModel>

  /** 按送水工+收款日期分页(后台按配送员筛选) */
  fun findByDeliveryWorkerIdAndCollectionDate(
    deliveryWorkerId: Long,
    collectionDate: LocalDate,
    pageable: Pageable,
  ): Page<DailyCollectionRecordModel>

  /** 按送水工分页(后台按配送员筛选,不限日期) */
  fun findByDeliveryWorkerId(
    deliveryWorkerId: Long,
    pageable: Pageable,
  ): Page<DailyCollectionRecordModel>

  fun existsByDeliveryWorkerIdAndCollectionDate(
    deliveryWorkerId: Long,
    collectionDate: LocalDate,
  ): Boolean
}
