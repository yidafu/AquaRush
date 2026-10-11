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

package dev.yidafu.aqua.subscription.service.impl

import dev.yidafu.aqua.api.dto.SubscriptionListQuery
import dev.yidafu.aqua.api.service.order.SubscriptionQueryService
import dev.yidafu.aqua.common.domain.model.SubscriptionModel
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.subscription.domain.repository.SubscriptionRepository
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 周期订购计划查询实现(后台列表,分页筛选,AGENTS 规则 10 禁止全表)。
 */
@Service
@Transactional(readOnly = true)
class SubscriptionQueryServiceImpl(
  private val subscriptionRepository: SubscriptionRepository,
) : SubscriptionQueryService {
  override fun search(query: SubscriptionListQuery): Page<SubscriptionModel> = subscriptionRepository.search(query)

  override fun getById(id: Long): SubscriptionModel = subscriptionRepository.findById(id) ?: throw NotFoundException("周期订购计划不存在: $id")
}
