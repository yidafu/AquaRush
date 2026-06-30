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

package dev.yidafu.aqua.common.domain.repository

import dev.yidafu.aqua.common.domain.model.BucketDepositModel
import dev.yidafu.aqua.common.domain.model.BucketDepositStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BucketDepositRepository : JpaRepository<BucketDepositModel, Long> {
  fun findByUserId(userId: Long): List<BucketDepositModel>

  fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<BucketDepositModel>

  fun findByUserIdAndStatus(
    userId: Long,
    status: BucketDepositStatus,
  ): List<BucketDepositModel>

  fun findByStatus(status: BucketDepositStatus): List<BucketDepositModel>

  fun findByStatusOrderByCreatedAtDesc(status: BucketDepositStatus): List<BucketDepositModel>

  fun findByUserIdAndStatusOrderByCreatedAtDesc(
    userId: Long,
    status: BucketDepositStatus,
  ): List<BucketDepositModel>

  fun countByUserId(userId: Long): Long

  fun countByUserIdAndStatus(
    userId: Long,
    status: BucketDepositStatus,
  ): Long

  // 分页查询
  fun findByUserId(
    userId: Long,
    pageable: Pageable,
  ): Page<BucketDepositModel>

  fun findByStatus(
    status: BucketDepositStatus,
    pageable: Pageable,
  ): Page<BucketDepositModel>

  fun findByUserIdAndStatus(
    userId: Long,
    status: BucketDepositStatus,
    pageable: Pageable,
  ): Page<BucketDepositModel>

  fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<BucketDepositModel>
}
