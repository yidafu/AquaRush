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

package dev.yidafu.aqua.api.dto

/**
 * 退押申请输入(F-PRO-010/R-PRO-04,用户端):桶数 ≤ 在租押金桶数(服务端校验),
 * 应退金额由服务端按押金记录快照计算(R-MON-01 金额不来自前端)。
 */
data class SubmitDepositRefundRequest(
  val specification: String,
  val bucketCount: Int,
  // 回收地址快照(退押空桶回收用)
  val receiverName: String,
  val receiverPhone: String,
  val receiverDetail: String,
  val remark: String? = null,
)
