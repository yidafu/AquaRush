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

package dev.yidafu.aqua.ops.storeprofile.service.impl

import dev.yidafu.aqua.api.dto.SaveStoreProfileRequest
import dev.yidafu.aqua.api.service.ops.StoreProfileMutationService
import dev.yidafu.aqua.common.domain.model.StoreProfileModel
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.storeprofile.domain.repository.StoreProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 店铺信息变更服务实现(管理后台)
 * 单行配置(固定 id = 1),首次保存创建、再次保存覆盖;配置即展示(R-OPS-01),每次保存留审计日志(操作人/时间/前后值)。
 */
@Service
class StoreProfileMutationServiceImpl(
  private val storeProfileRepository: StoreProfileRepository,
  private val opsConfigAuditService: OpsConfigAuditService,
) : StoreProfileMutationService {
  @Transactional
  override fun saveStoreProfile(
    adminId: Long,
    adminName: String,
    request: SaveStoreProfileRequest,
  ): StoreProfileModel {
    val existing = storeProfileRepository.findById(StoreProfileRepository.SINGLETON_ID).orElse(null)
    val before = existing?.let { storeProfileSnapshot(it) }

    val profile =
      existing?.apply {
        shopName = request.shopName
        logoUrl = request.logoUrl
        serviceHotline = request.serviceHotline
        businessHoursStart = request.businessHoursStart
        businessHoursEnd = request.businessHoursEnd
        shopIntro = request.shopIntro
        aboutUs = request.aboutUs
      } ?: StoreProfileModel(
        id = StoreProfileRepository.SINGLETON_ID,
        shopName = request.shopName,
        logoUrl = request.logoUrl,
        serviceHotline = request.serviceHotline,
        businessHoursStart = request.businessHoursStart,
        businessHoursEnd = request.businessHoursEnd,
        shopIntro = request.shopIntro,
        aboutUs = request.aboutUs,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    val saved = storeProfileRepository.save(profile)
    opsConfigAuditService.record(
      targetType = OpsConfigAuditService.TARGET_STORE_PROFILE,
      targetId = saved.id,
      actionType = OpsConfigAuditService.ACTION_STORE_PROFILE_SAVE,
      operatorId = adminId,
      operatorName = adminName,
      before = before,
      after = storeProfileSnapshot(saved),
    )
    return saved
  }

  private fun storeProfileSnapshot(profile: StoreProfileModel): Map<String, Any?> =
    linkedMapOf(
      "shopName" to profile.shopName,
      "logoUrl" to profile.logoUrl,
      "serviceHotline" to profile.serviceHotline,
      "businessHoursStart" to profile.businessHoursStart,
      "businessHoursEnd" to profile.businessHoursEnd,
      "shopIntro" to profile.shopIntro,
      "aboutUs" to profile.aboutUs,
    )
}
