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

package dev.yidafu.aqua.subscription.mapper

import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot as DomainDeliveryTimeSlot
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionCycleType as DomainSubscriptionCycleType
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionStatus as DomainSubscriptionStatus
import dev.yidafu.aqua.common.graphql.generated.DeliveryTimeSlot as DeliveryTimeSlotG
import dev.yidafu.aqua.common.graphql.generated.SubscriptionCycleType as SubscriptionCycleTypeG
import dev.yidafu.aqua.common.graphql.generated.SubscriptionStatus as SubscriptionStatusG

/**
 * 周期订购枚举 领域 ↔ GraphQL 互转(G77/F-ORD-026):
 * `when` 穷举,任一侧增删取值即编译失败(枚举转换约定 R1,禁 valueOf 字符串匹配)。
 */

fun DomainSubscriptionCycleType.toGraphQL(): SubscriptionCycleTypeG =
  when (this) {
    DomainSubscriptionCycleType.WEEKLY -> SubscriptionCycleTypeG.WEEKLY
    DomainSubscriptionCycleType.BIWEEKLY -> SubscriptionCycleTypeG.BIWEEKLY
    DomainSubscriptionCycleType.MONTHLY -> SubscriptionCycleTypeG.MONTHLY
  }

fun SubscriptionCycleTypeG.toDomain(): DomainSubscriptionCycleType =
  when (this) {
    SubscriptionCycleTypeG.WEEKLY -> DomainSubscriptionCycleType.WEEKLY
    SubscriptionCycleTypeG.BIWEEKLY -> DomainSubscriptionCycleType.BIWEEKLY
    SubscriptionCycleTypeG.MONTHLY -> DomainSubscriptionCycleType.MONTHLY
  }

fun DomainSubscriptionStatus.toGraphQL(): SubscriptionStatusG =
  when (this) {
    DomainSubscriptionStatus.ACTIVE -> SubscriptionStatusG.ACTIVE
    DomainSubscriptionStatus.PAUSED -> SubscriptionStatusG.PAUSED
    DomainSubscriptionStatus.TERMINATED -> SubscriptionStatusG.TERMINATED
  }

fun SubscriptionStatusG.toDomain(): DomainSubscriptionStatus =
  when (this) {
    SubscriptionStatusG.ACTIVE -> DomainSubscriptionStatus.ACTIVE
    SubscriptionStatusG.PAUSED -> DomainSubscriptionStatus.PAUSED
    SubscriptionStatusG.TERMINATED -> DomainSubscriptionStatus.TERMINATED
  }

fun DomainDeliveryTimeSlot.toGraphQL(): DeliveryTimeSlotG =
  when (this) {
    DomainDeliveryTimeSlot.IMMEDIATE -> DeliveryTimeSlotG.IMMEDIATE
    DomainDeliveryTimeSlot.MORNING -> DeliveryTimeSlotG.MORNING
    DomainDeliveryTimeSlot.AFTERNOON -> DeliveryTimeSlotG.AFTERNOON
    DomainDeliveryTimeSlot.EVENING -> DeliveryTimeSlotG.EVENING
  }

fun DeliveryTimeSlotG.toDomain(): DomainDeliveryTimeSlot =
  when (this) {
    DeliveryTimeSlotG.IMMEDIATE -> DomainDeliveryTimeSlot.IMMEDIATE
    DeliveryTimeSlotG.MORNING -> DomainDeliveryTimeSlot.MORNING
    DeliveryTimeSlotG.AFTERNOON -> DomainDeliveryTimeSlot.AFTERNOON
    DeliveryTimeSlotG.EVENING -> DomainDeliveryTimeSlot.EVENING
  }
