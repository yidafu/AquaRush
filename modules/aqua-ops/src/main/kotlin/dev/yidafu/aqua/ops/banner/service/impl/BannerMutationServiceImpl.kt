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

package dev.yidafu.aqua.ops.banner.service.impl

import dev.yidafu.aqua.api.dto.BannerCreateRequest
import dev.yidafu.aqua.api.dto.BannerUpdateRequest
import dev.yidafu.aqua.api.dto.ReorderDirection
import dev.yidafu.aqua.api.service.ops.BannerMutationService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.common.domain.model.enums.BannerLinkType
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.banner.domain.exception.BannerNotFoundException
import dev.yidafu.aqua.ops.banner.domain.exception.DuplicateSortOrderException
import dev.yidafu.aqua.ops.banner.domain.exception.InvalidTimeWindowException
import dev.yidafu.aqua.ops.banner.domain.repository.BannerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 轮播图变更服务实现(管理后台)
 * 所有变更操作落审计日志(R-OPS-01:操作人/时间/前后值)。
 */
@Service
class BannerMutationServiceImpl(
  private val bannerRepository: BannerRepository,
  private val productQueryApiService: ProductQueryApiService,
  private val opsConfigAuditService: OpsConfigAuditService,
) : BannerMutationService {
  @Transactional
  override fun createBanner(
    adminId: Long,
    adminName: String,
    request: BannerCreateRequest,
  ): BannerModel {
    validateTimeWindow(request.startAt, request.endAt)
    validateLinkTarget(request.linkType, request.linkTarget)
    val sortOrder = request.sortOrder ?: (bannerRepository.findMaxSortOrder() ?: 0).plus(1)
    validateSortOrderUnique(sortOrder, excludeId = null)

    val banner =
      BannerModel(
        imageUrl = request.imageUrl,
        title = request.title,
        subtitle = request.subtitle,
        linkType = request.linkType,
        linkTarget = request.linkTarget,
        sortOrder = sortOrder,
        enabled = request.enabled,
        startAt = request.startAt,
        endAt = request.endAt,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    val saved = bannerRepository.save(banner)
    opsConfigAuditService.record(
      targetType = OpsConfigAuditService.TARGET_BANNER,
      targetId = saved.id,
      actionType = OpsConfigAuditService.ACTION_BANNER_CREATE,
      operatorId = adminId,
      operatorName = adminName,
      after = bannerSnapshot(saved),
    )
    return saved
  }

  @Transactional
  override fun updateBanner(
    adminId: Long,
    adminName: String,
    id: Long,
    request: BannerUpdateRequest,
  ): BannerModel {
    val existing = bannerRepository.findById(id).orElseThrow { BannerNotFoundException(id) }
    validateTimeWindow(request.startAt, request.endAt)
    validateLinkTarget(request.linkType, request.linkTarget)
    val sortOrder = request.sortOrder ?: existing.sortOrder
    validateSortOrderUnique(sortOrder, excludeId = id)

    val before = bannerSnapshot(existing)
    existing.apply {
      imageUrl = request.imageUrl
      title = request.title
      subtitle = request.subtitle
      linkType = request.linkType
      linkTarget = request.linkTarget
      this.sortOrder = sortOrder
      enabled = request.enabled
      startAt = request.startAt
      endAt = request.endAt
    }
    val saved = bannerRepository.save(existing)
    opsConfigAuditService.record(
      targetType = OpsConfigAuditService.TARGET_BANNER,
      targetId = saved.id,
      actionType = OpsConfigAuditService.ACTION_BANNER_UPDATE,
      operatorId = adminId,
      operatorName = adminName,
      before = before,
      after = bannerSnapshot(saved),
    )
    return saved
  }

  @Transactional
  override fun deleteBanner(
    adminId: Long,
    adminName: String,
    id: Long,
  ): Boolean {
    val existing = bannerRepository.findById(id).orElseThrow { BannerNotFoundException(id) }
    val before = bannerSnapshot(existing)
    bannerRepository.delete(existing)
    opsConfigAuditService.record(
      targetType = OpsConfigAuditService.TARGET_BANNER,
      targetId = id,
      actionType = OpsConfigAuditService.ACTION_BANNER_DELETE,
      operatorId = adminId,
      operatorName = adminName,
      before = before,
    )
    return true
  }

  @Transactional
  override fun reorderBanner(
    adminId: Long,
    adminName: String,
    id: Long,
    direction: ReorderDirection,
  ): List<BannerModel> {
    val banners = bannerRepository.findAllOrderedForUpdate()
    val index = banners.indexOfFirst { it.id == id }
    if (index == -1) {
      throw BannerNotFoundException(id)
    }
    val swapIndex =
      when (direction) {
        ReorderDirection.UP -> index - 1
        ReorderDirection.DOWN -> index + 1
      }
    if (swapIndex !in banners.indices) {
      // 已在边界,无需调整
      return banners
    }

    val before = banners.map { bannerSnapshot(it) }
    val current = banners[index]
    val neighbor = banners[swapIndex]
    val tmp = current.sortOrder
    current.sortOrder = neighbor.sortOrder
    neighbor.sortOrder = tmp
    bannerRepository.saveAll(listOf(current, neighbor))

    opsConfigAuditService.record(
      targetType = OpsConfigAuditService.TARGET_BANNER,
      targetId = id,
      actionType = OpsConfigAuditService.ACTION_BANNER_REORDER,
      operatorId = adminId,
      operatorName = adminName,
      before = before,
      after = banners.map { bannerSnapshot(it) },
      description = "direction=${direction.name}",
    )
    return banners
  }

  private fun validateTimeWindow(
    startAt: LocalDateTime?,
    endAt: LocalDateTime?,
  ) {
    if (startAt != null && endAt != null && !startAt.isBefore(endAt)) {
      throw InvalidTimeWindowException("时间窗不合法:开始时间必须早于结束时间")
    }
  }

  private fun validateLinkTarget(
    linkType: BannerLinkType,
    linkTarget: String,
  ) {
    if (linkType == BannerLinkType.PRODUCT) {
      val productId = linkTarget.toLongOrNull()
      if (productId == null || !productQueryApiService.existsById(productId)) {
        throw IllegalArgumentException("跳转目标商品不存在: $linkTarget")
      }
    }
  }

  /**
   * 排序号唯一性校验(R-OPS-01),软删行已被 @SoftDelete 过滤,可复用排序号
   */
  private fun validateSortOrderUnique(
    sortOrder: Int,
    excludeId: Long?,
  ) {
    val existing = bannerRepository.findBySortOrder(sortOrder)
    if (existing != null && existing.id != excludeId) {
      throw DuplicateSortOrderException(sortOrder)
    }
  }

  private fun bannerSnapshot(banner: BannerModel): Map<String, Any?> =
    linkedMapOf(
      "imageUrl" to banner.imageUrl,
      "title" to banner.title,
      "subtitle" to banner.subtitle,
      "linkType" to banner.linkType.name,
      "linkTarget" to banner.linkTarget,
      "sortOrder" to banner.sortOrder,
      "enabled" to banner.enabled,
      "startAt" to banner.startAt,
      "endAt" to banner.endAt,
    )
}
