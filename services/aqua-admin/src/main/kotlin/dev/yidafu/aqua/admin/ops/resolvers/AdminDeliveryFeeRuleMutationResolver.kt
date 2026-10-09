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

package dev.yidafu.aqua.admin.ops.resolvers

import dev.yidafu.aqua.api.service.ops.DeliveryFeeRuleMutationService
import dev.yidafu.aqua.common.graphql.generated.DeliveryFeeRule
import dev.yidafu.aqua.common.graphql.generated.SaveDeliveryFeeRuleInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.ops.deliveryfeerule.mapper.toGraphQL
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 配送费规则变更 Resolver(管理后台,PERMISSION_OPS_CONFIG_WRITE)
 * 配置变更即时生效,已生成订单不受影响(运费按下单时快照 —— R-MON-02);每次保存留审计日志。
 */
@Controller
class AdminDeliveryFeeRuleMutationResolver(
  private val deliveryFeeRuleMutationService: DeliveryFeeRuleMutationService,
) {
  private val logger = LoggerFactory.getLogger(AdminDeliveryFeeRuleMutationResolver::class.java)

  /**
   * 保存配送费规则(单行配置,整体覆盖)
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_WRITE')")
  fun saveDeliveryFeeRule(
    @Argument @Valid input: SaveDeliveryFeeRuleInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): DeliveryFeeRule {
    logger.info("Admin {} saving delivery fee rule", principal.username)
    val rule =
      deliveryFeeRuleMutationService.saveDeliveryFeeRule(
        adminId = principal.id,
        adminName = principal.username,
        input = input,
      )
    return rule.toGraphQL()
  }
}
