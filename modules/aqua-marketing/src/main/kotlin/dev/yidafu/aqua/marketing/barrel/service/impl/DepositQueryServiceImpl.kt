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

package dev.yidafu.aqua.marketing.barrel.service.impl

import dev.yidafu.aqua.api.dto.BarrelAccount
import dev.yidafu.aqua.api.dto.BarrelAccountSpec
import dev.yidafu.aqua.api.dto.BarrelLedgerRow
import dev.yidafu.aqua.api.service.deposit.DepositQueryService
import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import dev.yidafu.aqua.common.domain.model.DepositRefundApplicationModel
import dev.yidafu.aqua.common.domain.model.enums.DepositRefundApplicationStatus
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import dev.yidafu.aqua.marketing.barrel.domain.repository.BarrelLedgerRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositRecordRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositRefundApplicationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * 押金查询服务实现(R-PRO-04 水桶账户 / R-PRO-05 桶账台账,同一聚合口径;
 * 用户侧经 userId 限定,后台列表全部强制分页)。
 */
@Service
class DepositQueryServiceImpl(
  private val depositRecordRepository: DepositRecordRepository,
  private val applicationRepository: DepositRefundApplicationRepository,
  private val ledgerRepository: BarrelLedgerRepository,
) : DepositQueryService {
  override fun barrelAccount(
    userId: Long,
    page: Int,
    size: Int,
  ): BarrelAccount {
    val held = ledgerRepository.sumHeldByUserAndSpec(listOf(userId)).associateBy { it.specification }
    val delivered = ledgerRepository.sumDeliveredByUserAndSpec(listOf(userId)).associateBy { it.specification }
    val recovered = ledgerRepository.sumRecoveredByUserAndSpec(listOf(userId)).associateBy { it.specification }

    val specs =
      (held.keys + delivered.keys + recovered.keys)
        .sorted()
        .map { spec ->
          val h = held[spec]
          val d = delivered[spec]?.bucketCount ?: 0L
          val r = recovered[spec]?.bucketCount ?: 0L
          BarrelAccountSpec(
            specification = spec,
            heldBucketCount = (h?.bucketCount ?: 0L).toInt(),
            owedBucketCount = (d - r).coerceAtLeast(0L).toInt(),
            depositHeldCents = h?.amountCents ?: 0L,
          )
        }.filter { it.heldBucketCount > 0 || it.owedBucketCount > 0 }

    return BarrelAccount(
      specs = specs,
      depositHeldCents = specs.sumOf { it.depositHeldCents },
      owedBucketCount = specs.sumOf { it.owedBucketCount },
      records = depositRecordRepository.pageByUserId(userId, null, PageRequest.of(page, size)),
    )
  }

  override fun listUserApplications(
    userId: Long,
    status: DepositRefundApplicationStatus?,
    page: Int,
    size: Int,
  ): Page<DepositRefundApplicationModel> = applicationRepository.search(userId, status, PageRequest.of(page, size))

  override fun listApplications(
    status: DepositRefundApplicationStatus?,
    page: Int,
    size: Int,
  ): Page<DepositRefundApplicationModel> = applicationRepository.search(null, status, PageRequest.of(page, size))

  override fun searchDepositRecords(
    userId: Long?,
    status: DepositStatus?,
    specification: String?,
    page: Int,
    size: Int,
  ): Page<DepositRecordModel> = depositRecordRepository.search(userId, status, specification, PageRequest.of(page, size))

  override fun barrelLedger(
    keyword: String?,
    onlyOwed: Boolean,
    page: Int,
    size: Int,
  ): Page<BarrelLedgerRow> = ledgerRepository.pageLedger(keyword, onlyOwed, page, size)
}
