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

package dev.yidafu.aqua.subscription.domain.repository

import dev.yidafu.aqua.api.dto.SubscriptionListQuery
import dev.yidafu.aqua.common.domain.model.SubscriptionItemModel
import dev.yidafu.aqua.common.domain.model.SubscriptionModel
import org.springframework.data.domain.Page
import java.time.LocalDate

/**
 * 周期订购计划仓储(G77/F-ORD-026):计划 + 明细两张表的读写入口。
 * 普通表(计划终止为状态而非删除,历史长期可查),无软删谓词。
 */
interface SubscriptionRepository {
  /** 插入或更新计划(按 id 判定) */
  fun save(entity: SubscriptionModel): SubscriptionModel

  fun findById(id: Long): SubscriptionModel?

  /** 后台列表分页筛选(用户昵称/手机号关键字、状态、下次配送时间区间;禁止全表) */
  fun search(query: SubscriptionListQuery): Page<SubscriptionModel>

  /**
   * 扫描到期计划:`status = ACTIVE AND next_run_date <= dueOnOrBefore AND id > afterId`,
   * 按 id 升序 keyset 分页(处理中谓词变化也不会漏行,禁止 offset 分页)。
   */
  fun findDue(
    afterId: Long,
    dueOnOrBefore: LocalDate,
    limit: Int,
  ): List<SubscriptionModel>

  fun saveItems(items: List<SubscriptionItemModel>)

  fun findItemsBySubscriptionId(subscriptionId: Long): List<SubscriptionItemModel>
}
