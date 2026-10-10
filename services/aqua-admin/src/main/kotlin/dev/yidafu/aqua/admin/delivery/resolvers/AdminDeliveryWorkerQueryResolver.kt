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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.admin.delivery.resolvers

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerStatisticsQueryApiService
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerStatisticsVo
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerVo
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerVoPage
import dev.yidafu.aqua.delivery.mapper.toDeliveryWorker
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.time.LocalDateTime
import dev.yidafu.aqua.common.annotation.AdminService as AdminServiceAnnotation

/**
 * 管理端配送员查询解析器
 * 提供配送员管理的查询功能，仅管理员可访问
 *
 * 送水员档案并入 admins 单表后(PRD/07-user.md §F-USR-021),配送员即
 * role=DELIVERY_WORKER/ADMIN 的账号行,DeliveryWorkerVo 为兼容投影类型。
 */
@AdminServiceAnnotation
@Controller
class AdminDeliveryWorkerQueryResolver(
  private val adminService: AdminService,
  private val deliveryWorkerStatisticsQueryApiService: DeliveryWorkerStatisticsQueryApiService,
) {
  /**
   * 分页查询配送员(管理员功能)。keyword 模糊匹配 username/realName/phone;
   * size 上限 100,防止绕过规则 10 一次拉全表。
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_WORKER_READ')")
  fun deliveryWorkers(
    @Argument keyword: String?,
    @Argument role: String?,
    @Argument page: Int = 0,
    @Argument size: Int = 20,
  ): DeliveryWorkerVoPage {
    val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, MAX_PAGE_SIZE))
    val result =
      adminService.findDeliveryWorkers(
        keyword = keyword,
        role = role?.let { runCatching { AdminRoleModel.fromValue(it) }.getOrNull() },
        pageable = pageable,
      )
    return DeliveryWorkerVoPage(
      content = result.content.map { it.toDeliveryWorker() },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }

  /**
   * 查询在线配送员（管理员功能）
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_WORKER_READ')")
  fun onlineDeliveryWorkers(): List<DeliveryWorkerVo> =
    adminService.findByOnlineStatus(DeliverWorkerModelStatus.ONLINE).map { it.toDeliveryWorker() }

  /**
   * 根据ID查询配送员（管理员功能）
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_WORKER_READ')")
  fun deliveryWorker(
    @Argument id: Long,
  ): DeliveryWorkerVo? =
    adminService.findById(id)?.let {
      if (it.role == AdminRoleModel.DELIVERY_WORKER || it.role == AdminRoleModel.ADMIN) {
        it.toDeliveryWorker()
      } else {
        null
      }
    }

  /**
   * 配送员评分分布/总评价数(账号详情页「评价记录」Tab,PERMISSION_DELIVERY_WORKER_READ):
   * 数据源 delivery_worker_statistics 表(deliveryWorkerId=账号 id);无统计记录返回全零分布。
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_WORKER_READ')")
  fun deliveryWorkerRatingStatistics(
    @Argument deliveryWorkerId: Long,
  ): DeliveryWorkerStatisticsVo {
    val stats = deliveryWorkerStatisticsQueryApiService.findByDeliveryWorkerId(deliveryWorkerId)
    val workerName = adminService.findById(deliveryWorkerId)?.realName
    if (stats == null) {
      return DeliveryWorkerStatisticsVo(
        deliveryWorkerId = deliveryWorkerId,
        workerName = workerName,
        averageRating = BigDecimal.ZERO,
        totalReviews = 0L,
        oneStarReviews = 0L,
        twoStarReviews = 0L,
        threeStarReviews = 0L,
        fourStarReviews = 0L,
        fiveStarReviews = 0L,
        ratingDistribution = emptyMap(),
        lastUpdated = LocalDateTime.now(),
      )
    }
    return DeliveryWorkerStatisticsVo(
      deliveryWorkerId = deliveryWorkerId,
      workerName = workerName,
      averageRating = stats.averageRating,
      totalReviews = stats.totalReviews.toLong(),
      oneStarReviews = stats.oneStarReviews.toLong(),
      twoStarReviews = stats.twoStarReviews.toLong(),
      threeStarReviews = stats.threeStarReviews.toLong(),
      fourStarReviews = stats.fourStarReviews.toLong(),
      fiveStarReviews = stats.fiveStarReviews.toLong(),
      ratingDistribution =
        mapOf(
          "1" to stats.oneStarReviews.toLong(),
          "2" to stats.twoStarReviews.toLong(),
          "3" to stats.threeStarReviews.toLong(),
          "4" to stats.fourStarReviews.toLong(),
          "5" to stats.fiveStarReviews.toLong(),
        ),
      lastUpdated = stats.lastUpdated,
    )
  }

  private companion object {
    /** 单页上限:防止调用方用超大 size 绕过规则 10 */
    const val MAX_PAGE_SIZE = 100
  }
}
