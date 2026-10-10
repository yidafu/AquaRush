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

package dev.yidafu.aqua.api.service.deposit

import dev.yidafu.aqua.api.dto.BarrelAccount
import dev.yidafu.aqua.api.dto.BarrelLedgerRow
import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import dev.yidafu.aqua.common.domain.model.DepositRefundApplicationModel
import dev.yidafu.aqua.common.domain.model.enums.DepositRefundApplicationStatus
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import org.springframework.data.domain.Page

/**
 * 押金查询服务(R-MON-03/R-PRO-04/05,实现位于 aqua-marketing;AGENTS.md 规则 3):
 * 用户水桶账户与退押记录(仅本人 R-AUTH-02)+ 后台押金记录/退押申请/桶账台账(全部强制分页)。
 */
interface DepositQueryService {
  /** 水桶账户(F-PRO-010):按规格在租/欠桶/押金合计 + 押金记录分页 */
  fun barrelAccount(
    userId: Long,
    page: Int,
    size: Int,
  ): BarrelAccount

  /** 用户退押申请列表(仅本人) */
  fun listUserApplications(
    userId: Long,
    status: DepositRefundApplicationStatus?,
    page: Int,
    size: Int,
  ): Page<DepositRefundApplicationModel>

  /** 后台退押申请分页(状态筛选) */
  fun listApplications(
    status: DepositRefundApplicationStatus?,
    page: Int,
    size: Int,
  ): Page<DepositRefundApplicationModel>

  /** 后台押金记录分页(用户/状态/规格筛选) */
  fun searchDepositRecords(
    userId: Long?,
    status: DepositStatus?,
    specification: String?,
    page: Int,
    size: Int,
  ): Page<DepositRecordModel>

  /** 桶账台账分页(R-PRO-05/F-PRO-012:关键词 + 仅看有欠桶) */
  fun barrelLedger(
    keyword: String?,
    onlyOwed: Boolean,
    page: Int,
    size: Int,
  ): Page<BarrelLedgerRow>
}
