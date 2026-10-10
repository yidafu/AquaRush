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

package dev.yidafu.aqua.marketing.barrel.domain.repository

import dev.yidafu.aqua.common.domain.model.DepositRefundApplicationModel
import dev.yidafu.aqua.common.domain.model.enums.DepositRefundApplicationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 退押申请仓储(R-PRO-04/F-PRO-010):
 * 状态流转 transition 走状态谓词条件更新(fromStatuses 谓词),受影响行数=0 即并发冲突,
 * 防止回收确认/审核退还的重复流转。
 */
interface DepositRefundApplicationRepository {
  fun findById(id: Long): DepositRefundApplicationModel?

  /** 本人可见查询(R-AUTH-02 本人数据隔离)。 */
  fun findByIdAndUserId(
    id: Long,
    userId: Long,
  ): DepositRefundApplicationModel?

  /** 同用户同规格处理中申请(PENDING/RECOVERED;重复提交拦截,R-PRO-04)。 */
  fun findActiveByUserAndSpec(
    userId: Long,
    specification: String,
  ): DepositRefundApplicationModel?

  /** 申请列表分页(userId/status 可选:用户端传本人,后台按状态筛)。 */
  fun search(
    userId: Long?,
    status: DepositRefundApplicationStatus?,
    pageable: Pageable,
  ): Page<DepositRefundApplicationModel>

  fun save(entity: DepositRefundApplicationModel): DepositRefundApplicationModel

  /**
   * 状态谓词流转(受影响行数=0 即失败):
   * RECOVERED 落 recovered_at、REFUNDED 落 refunded_at、REJECTED 落 rejected_at+原因。
   */
  fun transition(
    id: Long,
    fromStatuses: List<DepositRefundApplicationStatus>,
    to: DepositRefundApplicationStatus,
    handlerAdminId: Long?,
    rejectReason: String?,
  ): Boolean
}
