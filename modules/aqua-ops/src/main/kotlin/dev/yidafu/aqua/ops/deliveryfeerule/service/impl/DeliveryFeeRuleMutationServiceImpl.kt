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

package dev.yidafu.aqua.ops.deliveryfeerule.service.impl

import dev.yidafu.aqua.api.service.ops.DeliveryFeeRuleMutationService
import dev.yidafu.aqua.common.domain.model.DeliveryDistanceTier
import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import dev.yidafu.aqua.common.domain.model.DeliveryFeeTier
import dev.yidafu.aqua.common.graphql.generated.SaveDeliveryFeeRuleInput
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.deliveryfeerule.domain.repository.DeliveryFeeRuleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 配送费规则变更服务实现(管理后台)
 * 单行配置(固定 id = 1),首次保存创建、再次保存覆盖;
 * 配置变更即时生效,已生成订单不受影响(运费按下单时快照 —— R-MON-02);
 * 每次保存留审计日志(R-OPS-02:操作人/时间/前后值)。
 */
@Service
class DeliveryFeeRuleMutationServiceImpl(
  private val deliveryFeeRuleRepository: DeliveryFeeRuleRepository,
  private val opsConfigAuditService: OpsConfigAuditService,
) : DeliveryFeeRuleMutationService {
  @Transactional
  override fun saveDeliveryFeeRule(
    adminId: Long,
    adminName: String,
    input: SaveDeliveryFeeRuleInput,
  ): DeliveryFeeRuleModel {
    validateFee(input)
    val existing = deliveryFeeRuleRepository.findById(DeliveryFeeRuleRepository.SINGLETON_ID).orElse(null)
    val before = existing?.let { ruleSnapshot(it) }

    val tiers =
      input.tiers.map { tier ->
        DeliveryFeeTier(fromFloor = tier.fromFloor, toFloor = tier.toFloor, feeCents = tier.feeCents)
      }
    val distanceTiers =
      input.distanceTiers
        .map { tier ->
          DeliveryDistanceTier(upToKm = tier.upToKm?.toDouble(), feeCents = tier.feeCents)
        }.ifEmpty { null }
    val storeLongitude = input.storeLongitude?.toDouble()
    val storeLatitude = input.storeLatitude?.toDouble()
    val rule =
      existing?.apply {
        freeFloorLimit = input.freeFloorLimit
        surchargeCents = input.surchargeCents
        this.tiers = tiers
        minimumOrderCents = input.minimumOrderCents
        this.distanceTiers = distanceTiers
        this.storeLongitude = storeLongitude
        this.storeLatitude = storeLatitude
      } ?: DeliveryFeeRuleModel(
        id = DeliveryFeeRuleRepository.SINGLETON_ID,
        freeFloorLimit = input.freeFloorLimit,
        surchargeCents = input.surchargeCents,
        tiers = tiers,
        minimumOrderCents = input.minimumOrderCents,
        distanceTiers = distanceTiers,
        storeLongitude = storeLongitude,
        storeLatitude = storeLatitude,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    val saved = deliveryFeeRuleRepository.save(rule)
    opsConfigAuditService.record(
      targetType = OpsConfigAuditService.TARGET_DELIVERY_FEE_RULE,
      targetId = saved.id,
      actionType = OpsConfigAuditService.ACTION_DELIVERY_FEE_RULE_SAVE,
      operatorId = adminId,
      operatorName = adminName,
      before = before,
      after = ruleSnapshot(saved),
    )
    return saved
  }

  /**
   * 运费校验(R-MON-01/02 + R-OPS-03):免费楼层 ≥ 0、兜底单档 0-999 元;
   * 楼层档位 tiers 非空、按楼层升序、区间不重叠、起始楼层 > 免费楼层、末档 toFloor 可空(不封顶);
   * 起送门槛 ≥ 0(0=不启用);距离档位升序、至多一档不封顶且必须最后、有档位时须配置门店坐标;
   * 门店坐标成对配置且在经纬度值域内。
   */
  private fun validateFee(input: SaveDeliveryFeeRuleInput) {
    if (input.freeFloorLimit < 0) {
      throw IllegalArgumentException("免费楼层不能小于0")
    }
    if (input.surchargeCents < 0 || input.surchargeCents > 99_900) {
      throw IllegalArgumentException("运费金额无效")
    }
    val tiers = input.tiers
    if (tiers.isEmpty()) {
      throw IllegalArgumentException("档位不能为空(至少一档)")
    }
    var prevFrom = 0
    var prevTo: Int? = null
    for (tier in tiers) {
      val toFloor = tier.toFloor
      if (toFloor != null && toFloor < tier.fromFloor) {
        throw IllegalArgumentException("档位结束楼层不能小于起始楼层")
      }
      if (tier.fromFloor <= prevFrom) {
        throw IllegalArgumentException("档位须按起始楼层严格升序")
      }
      prevTo?.let {
        if (tier.fromFloor <= it) {
          throw IllegalArgumentException("档位区间不可重叠")
        }
      }
      if (tier.fromFloor <= input.freeFloorLimit) {
        throw IllegalArgumentException("档位起始楼层须大于免费楼层")
      }
      prevFrom = tier.fromFloor
      prevTo = tier.toFloor
    }
    if (prevTo != null) {
      throw IllegalArgumentException("最后一档结束楼层须留空(不封顶)")
    }
    if (input.minimumOrderCents < 0) {
      throw IllegalArgumentException("起送门槛金额无效")
    }
    validateDistanceTiers(input)
  }

  /**
   * 距离阶梯校验(R-OPS-03):档位按距离上限严格升序、至多一档不封顶(null)且必须为最后一档、
   * 距离上限 > 0;配置了距离档位时门店坐标必须成对提供(否则距离计费静默失效)。
   */
  private fun validateDistanceTiers(input: SaveDeliveryFeeRuleInput) {
    val tiers = input.distanceTiers
    val hasStoreCoordinate = input.storeLongitude != null && input.storeLatitude != null
    if ((input.storeLongitude == null) != (input.storeLatitude == null)) {
      throw IllegalArgumentException("门店经度与纬度须同时配置")
    }
    if (hasStoreCoordinate) {
      val lng = input.storeLongitude!!.toDouble()
      val lat = input.storeLatitude!!.toDouble()
      if (lng < -180 || lng > 180 || lat < -90 || lat > 90) {
        throw IllegalArgumentException("门店经纬度超出取值范围")
      }
    }
    if (tiers.isEmpty()) return
    if (!hasStoreCoordinate) {
      throw IllegalArgumentException("配置距离阶梯前须先设置门店经纬度")
    }
    var prevUpTo: Double? = null
    tiers.forEachIndexed { index, tier ->
      val upTo = tier.upToKm?.toDouble()
      if (upTo != null) {
        if (upTo <= 0) {
          throw IllegalArgumentException("距离上限须大于0")
        }
        prevUpTo?.let {
          if (upTo <= it) {
            throw IllegalArgumentException("距离档位须按距离上限严格升序且区间不重叠")
          }
        }
        prevUpTo = upTo
      } else if (index != tiers.lastIndex) {
        throw IllegalArgumentException("不封顶档位(距离上限为空)必须为最后一档")
      }
    }
  }

  private fun ruleSnapshot(rule: DeliveryFeeRuleModel): Map<String, Any?> =
    linkedMapOf(
      "freeFloorLimit" to rule.freeFloorLimit,
      "surchargeCents" to rule.surchargeCents,
      "tiers" to rule.tiers,
      "minimumOrderCents" to rule.minimumOrderCents,
      "distanceTiers" to rule.distanceTiers,
      "storeLongitude" to rule.storeLongitude,
      "storeLatitude" to rule.storeLatitude,
    )
}
