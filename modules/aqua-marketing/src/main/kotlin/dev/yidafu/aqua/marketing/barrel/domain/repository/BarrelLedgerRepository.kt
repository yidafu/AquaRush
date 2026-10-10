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

package dev.yidafu.aqua.marketing.barrel.domain.repository

import dev.yidafu.aqua.api.dto.BarrelLedgerRow
import org.springframework.data.domain.Page

/**
 * 桶账聚合读侧(R-PRO-04 用户水桶账户 / R-PRO-05 后台桶账台账,同一口径):
 * - 在租桶数 = Σ(HELD 押金记录 bucket_count),押金合计 = Σ deposit_amount_cents;
 * - 已交付桶数 = Σ(已完成订单 order_items.quantity),仅计配置了押金价目的规格
 *   (【开放决策】历史无押金桶归属——建议计入欠桶,以价目规格存在性为口径);
 * - 已回收桶数 = Σ(barrel_recovery_records.bucket_count),同样仅计押金价目规格;
 * - 欠桶数 = max(已交付 − 已回收, 0)(按用户;台账行再按客户展示)。
 * userIds 为必传限定集(用户账户传单 id,聚合查询禁全表);台账走分页 SQL。
 */
interface BarrelLedgerRepository {
  data class SpecBucketSum(
    val userId: Long,
    val specification: String,
    val bucketCount: Long,
    // 押金口径:HELD 行为押金合计(分);交付/回收口径该值为 0
    val amountCents: Long,
  )

  /** 在租押金按 用户×规格 聚合(status=HELD)。 */
  fun sumHeldByUserAndSpec(userIds: List<Long>): List<SpecBucketSum>

  /** 已交付桶数按 用户×规格(已完成订单 items,仅押金价目规格)。 */
  fun sumDeliveredByUserAndSpec(userIds: List<Long>): List<SpecBucketSum>

  /** 已回收空桶数按 用户×规格(回收登记挂订单,经 orders 取归属用户;仅押金价目规格)。 */
  fun sumRecoveredByUserAndSpec(userIds: List<Long>): List<SpecBucketSum>

  /** 桶账台账分页(R-PRO-05/F-PRO-012):关键词(昵称/手机号)+ 仅看有欠桶,欠桶降序。 */
  fun pageLedger(
    keyword: String?,
    onlyOwed: Boolean,
    page: Int,
    size: Int,
  ): Page<BarrelLedgerRow>
}
