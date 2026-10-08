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

import dev.yidafu.aqua.api.service.ops.DeliveryFeeRuleQueryApiService
import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import dev.yidafu.aqua.ops.deliveryfeerule.domain.repository.DeliveryFeeRuleRepository
import org.springframework.stereotype.Service

/**
 * 配送费规则查询服务实现(用户端 + 订单域消费)
 * 订单域(R-MON-02)下单时读取本规则计算配送费;未配置时按默认规则(免费4楼/加价2元)。
 */
@Service
class DeliveryFeeRuleQueryApiServiceImpl(
  private val deliveryFeeRuleRepository: DeliveryFeeRuleRepository,
) : DeliveryFeeRuleQueryApiService {
  override fun getEffectiveDeliveryFeeRule(): DeliveryFeeRuleModel =
    deliveryFeeRuleRepository
      .findById(DeliveryFeeRuleRepository.SINGLETON_ID)
      .orElse(
        DeliveryFeeRuleModel(
          id = DeliveryFeeRuleRepository.SINGLETON_ID,
          freeFloorLimit = DeliveryFeeRuleModel.DEFAULT_FREE_FLOOR_LIMIT,
          surchargeCents = DeliveryFeeRuleModel.DEFAULT_SURCHARGE_CENTS,
        ),
      )
}
