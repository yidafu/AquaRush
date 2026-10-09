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
import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
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

    val rule =
      existing?.apply {
        freeFloorLimit = input.freeFloorLimit
        surchargeCents = input.surchargeCents
      } ?: DeliveryFeeRuleModel(
        id = DeliveryFeeRuleRepository.SINGLETON_ID,
        freeFloorLimit = input.freeFloorLimit,
        surchargeCents = input.surchargeCents,
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
   * 运费金额校验(R-MON-01 以分存储计算):免费楼层 ≥ 0、加价 0-999 元(≤2 位小数,前端以分提交)
   */
  private fun validateFee(input: SaveDeliveryFeeRuleInput) {
    if (input.freeFloorLimit < 0) {
      throw IllegalArgumentException("免费楼层不能小于0")
    }
    if (input.surchargeCents < 0 || input.surchargeCents > 99_900) {
      throw IllegalArgumentException("运费金额无效")
    }
  }

  private fun ruleSnapshot(rule: DeliveryFeeRuleModel): Map<String, Any?> =
    linkedMapOf(
      "freeFloorLimit" to rule.freeFloorLimit,
      "surchargeCents" to rule.surchargeCents,
    )
}
