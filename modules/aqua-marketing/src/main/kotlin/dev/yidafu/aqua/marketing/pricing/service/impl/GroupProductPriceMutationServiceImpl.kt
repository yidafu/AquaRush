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

package dev.yidafu.aqua.marketing.pricing.service.impl

import dev.yidafu.aqua.api.dto.SaveGroupProductPricesRequest
import dev.yidafu.aqua.api.service.marketing.GroupProductPriceMutationService
import dev.yidafu.aqua.common.domain.model.GroupProductPriceModel
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.pricing.domain.repository.GroupProductPriceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 分组商品专属价变更服务实现(管理后台,F-MKT-002):
 * 批量 upsert;priceCents=null 清空该项(回退目录价);clearUnlisted 清空本批外的已配置价格。
 * 保存后订单计价即时生效,已生成订单成交价不变(R-MKT-01 快照)。
 */
@Service
class GroupProductPriceMutationServiceImpl(
  private val groupProductPriceRepository: GroupProductPriceRepository,
  private val bizLogger: BizLogger,
) : GroupProductPriceMutationService {
  private val logger = LoggerFactory.getLogger(GroupProductPriceMutationServiceImpl::class.java)

  @Transactional
  override fun savePrices(
    adminId: Long,
    adminName: String,
    request: SaveGroupProductPricesRequest,
  ): Boolean {
    val now = LocalDateTime.now()
    val submittedProductIds = request.items.map { it.productId }.toSet()

    // 清空本批未包含的已配置价格(整组清空/批量覆盖语义)
    if (request.clearUnlisted) {
      groupProductPriceRepository
        .findByCustomerGroupId(request.customerGroupId)
        .filter { it.productId !in submittedProductIds }
        .forEach { groupProductPriceRepository.delete(it) }
    }

    request.items.forEach { item ->
      val priceCents = item.priceCents
      // 服务端兜底校验(嵌套输入校验不级联,金额 0.01-99999 元 R-MON-01)
      require(priceCents == null || (priceCents in 1..9999900)) { "分组价格无效(0.01-99999元)" }
      val existing = groupProductPriceRepository.findByCustomerGroupIdAndProductId(request.customerGroupId, item.productId)
      when {
        priceCents == null -> {
          // 清空该项 → 回退目录价
          existing?.let { groupProductPriceRepository.delete(it) }
        }
        existing != null -> {
          existing.priceCents = priceCents
          existing.updatedAt = now
          groupProductPriceRepository.save(existing)
        }
        else -> {
          groupProductPriceRepository.save(
            GroupProductPriceModel(
              customerGroupId = request.customerGroupId,
              productId = item.productId,
              priceCents = priceCents,
              createdAt = now,
              updatedAt = now,
            ),
          )
        }
      }
    }
    logger.info("Admin {} saved {} group prices for group {}", adminId, request.items.size, request.customerGroupId)

    // 业务日志(F-LOG-003 MARKETING 域):分组定价保存
    bizLogger.logMarketingOperation(
      targetId = request.customerGroupId,
      operation = "GROUP_PRICE_SAVED",
      result = "SUCCESS",
      userId = adminId,
      username = adminName,
      additionalData = mapOf("itemCount" to request.items.size, "clearUnlisted" to request.clearUnlisted),
    )
    return true
  }
}
