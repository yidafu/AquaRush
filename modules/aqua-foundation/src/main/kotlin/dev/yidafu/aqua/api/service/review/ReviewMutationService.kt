/*
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

package dev.yidafu.aqua.api.service.review

import dev.yidafu.aqua.api.dto.CreateReviewRequest
import dev.yidafu.aqua.common.domain.model.ReviewModel

/**
 * 订单评价写入服务接口(F-ORD-007,PRD 04-order.md §R-EVL-01):
 * 一单一评、仅已完成订单可评、评价不可修改、评分实时计入送水工统计。
 * 由 aqua-trade 模块实现,供 aqua-client 等调用。
 */
interface ReviewMutationService {
  /**
   * 创建评价:userId 由调用方强制为登录用户(防越权);
   * 校验订单已完成且属于该用户、存在配送员、未评价过;评价后更新送水工统计。
   */
  fun createReview(
    userId: Long,
    input: CreateReviewRequest,
  ): ReviewModel
}
