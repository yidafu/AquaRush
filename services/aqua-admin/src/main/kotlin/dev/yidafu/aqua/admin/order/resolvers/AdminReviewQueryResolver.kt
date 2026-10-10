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

package dev.yidafu.aqua.admin.order.resolvers

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.review.ReviewQueryService
import dev.yidafu.aqua.common.domain.model.ReviewModel
import dev.yidafu.aqua.common.graphql.generated.ReviewVo
import dev.yidafu.aqua.common.graphql.generated.ReviewVoPage
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 用户评价查询 Resolver(管理后台,PERMISSION_USER_READ / PERMISSION_DELIVERY_WORKER_READ):
 * 供用户详情页「评价记录」Tab(F-ORD-007)与账号详情页「评价记录」Tab(F-USR-020/021)展示。
 * 配送员名经 AdminService.findById 联查(送水员档案并入 admins 单表后,deliveryWorkerId=账号 id)。
 */
@Controller
class AdminReviewQueryResolver(
  private val reviewQueryService: ReviewQueryService,
  private val adminService: AdminService,
) {
  /**
   * 用户评价记录(分页,创建时间倒序)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_USER_READ')")
  fun reviewsByUser(
    @Argument userId: Long,
    @Argument page: Int,
    @Argument size: Int,
  ): ReviewVoPage {
    val result = reviewQueryService.findByUserIdPaginated(userId, PageRequest.of(page, size))
    return ReviewVoPage(
      content = result.content.map { it.toGraphQL() },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }

  /**
   * 配送员收到的评价记录(账号详情页「评价记录」Tab,分页,创建时间倒序;
   * 送水员档案并入 admins 单表后 deliveryWorkerId=账号 id,管理员兼送水同口径)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_WORKER_READ')")
  fun reviewsByDeliveryWorker(
    @Argument deliveryWorkerId: Long,
    @Argument page: Int,
    @Argument size: Int,
  ): ReviewVoPage {
    val result = reviewQueryService.findByDeliveryWorkerIdPaginated(deliveryWorkerId, PageRequest.of(page, size))
    return ReviewVoPage(
      content = result.content.map { it.toGraphQL() },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }

  private fun ReviewModel.toGraphQL(): ReviewVo =
    ReviewVo(
      reviewId = id ?: 0L,
      orderId = orderId,
      userId = userId,
      deliveryWorkerId = deliveryWorkerId,
      deliveryWorkerName = adminService.findById(deliveryWorkerId)?.realName,
      rating = rating,
      comment = comment,
      isAnonymous = isAnonymous,
      createdAt = createdAt,
    )
}
