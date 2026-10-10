/**
 * AquaRush
 *
 * Copyright (C) 2026 AquaRush Team
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

package dev.yidafu.aqua.common.domain.model.enums

import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelFeeStatus
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus
import dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus
import dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType
import dev.yidafu.aqua.common.domain.model.enums.FileType
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.GiftActivityType
import dev.yidafu.aqua.common.domain.model.enums.NotificationChannelStatus
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.domain.model.enums.PriceSource
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType
import dev.yidafu.aqua.common.domain.model.enums.SourceSystem
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * 领域枚举 DB/查询/事件字符串解析入口 `fromValue` 契约:
 * - 每个枚举值 roundtrip(`fromValue(name)` 必须还原自身) —— 覆盖全部 when 分支;
 * - 未知值 fail-fast 抛 [IllegalArgumentException](与原 valueOf 行为一致,不静默兜底)。
 */
class EnumFromValueTest :
  FunSpec({
    test("fromValue roundtrip: 每个枚举值可无损解析") {
      AdminAccountStatus.entries.forEach { AdminAccountStatus.fromValue(it.name) shouldBe it }
      AdminRoleModel.entries.forEach { AdminRoleModel.fromValue(it.name) shouldBe it }
      BarrelFeeStatus.entries.forEach { BarrelFeeStatus.fromValue(it.name) shouldBe it }
      BarrelPaymentStatus.entries.forEach { BarrelPaymentStatus.fromValue(it.name) shouldBe it }
      CouponIssuanceType.entries.forEach { CouponIssuanceType.fromValue(it.name) shouldBe it }
      CouponScope.entries.forEach { CouponScope.fromValue(it.name) shouldBe it }
      CouponTemplateStatus.entries.forEach { CouponTemplateStatus.fromValue(it.name) shouldBe it }
      CouponType.entries.forEach { CouponType.fromValue(it.name) shouldBe it }
      DailyCollectionStatus.entries.forEach { DailyCollectionStatus.fromValue(it.name) shouldBe it }
      DailyReconciliationStatus.entries.forEach { DailyReconciliationStatus.fromValue(it.name) shouldBe it }
      DeliveryPhotoReviewStatus.entries.forEach { DeliveryPhotoReviewStatus.fromValue(it.name) shouldBe it }
      DeliveryTimeSlot.entries.forEach { DeliveryTimeSlot.fromValue(it.name) shouldBe it }
      DepositStatus.entries.forEach { DepositStatus.fromValue(it.name) shouldBe it }
      DiscrepancyStatus.entries.forEach { DiscrepancyStatus.fromValue(it.name) shouldBe it }
      DiscrepancyType.entries.forEach { DiscrepancyType.fromValue(it.name) shouldBe it }
      FileType.entries.forEach { FileType.fromValue(it.name) shouldBe it }
      FirstOrderActivityType.entries.forEach { FirstOrderActivityType.fromValue(it.name) shouldBe it }
      GiftActivityType.entries.forEach { GiftActivityType.fromValue(it.name) shouldBe it }
      NotificationChannelStatus.entries.forEach { NotificationChannelStatus.fromValue(it.name) shouldBe it }
      NotificationMessageType.entries.forEach { NotificationMessageType.fromValue(it.name) shouldBe it }
      NotificationReceiverType.entries.forEach { NotificationReceiverType.fromValue(it.name) shouldBe it }
      OperatorType.entries.forEach { OperatorType.fromValue(it.name) shouldBe it }
      OrderModelStatus.entries.forEach { OrderModelStatus.fromValue(it.name) shouldBe it }
      OrderOperationType.entries.forEach { OrderOperationType.fromValue(it.name) shouldBe it }
      PaymentType.entries.forEach { PaymentType.fromValue(it.name) shouldBe it }
      PriceSource.entries.forEach { PriceSource.fromValue(it.name) shouldBe it }
      ProductModelStatus.entries.forEach { ProductModelStatus.fromValue(it.name) shouldBe it }
      ProductOptionType.entries.forEach { ProductOptionType.fromValue(it.name) shouldBe it }
      PromotionActivityStatus.entries.forEach { PromotionActivityStatus.fromValue(it.name) shouldBe it }
      PromotionActivityType.entries.forEach { PromotionActivityType.fromValue(it.name) shouldBe it }
      PromotionScope.entries.forEach { PromotionScope.fromValue(it.name) shouldBe it }
      ReconciliationTaskStatus.entries.forEach { ReconciliationTaskStatus.fromValue(it.name) shouldBe it }
      ReconciliationTaskType.entries.forEach { ReconciliationTaskType.fromValue(it.name) shouldBe it }
      SourceSystem.entries.forEach { SourceSystem.fromValue(it.name) shouldBe it }
      TicketPurchaseStatus.entries.forEach { TicketPurchaseStatus.fromValue(it.name) shouldBe it }
      TicketTypeKind.entries.forEach { TicketTypeKind.fromValue(it.name) shouldBe it }
      TicketTypeStatus.entries.forEach { TicketTypeStatus.fromValue(it.name) shouldBe it }
      UserCouponStatus.entries.forEach { UserCouponStatus.fromValue(it.name) shouldBe it }
      UserTicketStatus.entries.forEach { UserTicketStatus.fromValue(it.name) shouldBe it }
      ValidityType.entries.forEach { ValidityType.fromValue(it.name) shouldBe it }
    }

    test("fromValue 未知值抛 IllegalArgumentException(fail-fast)") {
      shouldThrow<IllegalArgumentException> { AdminAccountStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { AdminRoleModel.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { BarrelFeeStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { BarrelPaymentStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { CouponIssuanceType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { CouponScope.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { CouponTemplateStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { CouponType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { DailyCollectionStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { DailyReconciliationStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { DeliveryPhotoReviewStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { DeliveryTimeSlot.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { DepositStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { DiscrepancyStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { DiscrepancyType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { FileType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { FirstOrderActivityType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { GiftActivityType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { NotificationChannelStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { NotificationMessageType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { NotificationReceiverType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { OperatorType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { OrderModelStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { OrderOperationType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { PaymentType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { PriceSource.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { ProductModelStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { ProductOptionType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { PromotionActivityStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { PromotionActivityType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { PromotionScope.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { ReconciliationTaskStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { ReconciliationTaskType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { SourceSystem.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { TicketPurchaseStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { TicketTypeKind.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { TicketTypeStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { UserCouponStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { UserTicketStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { ValidityType.fromValue("__NO_SUCH_VALUE__") }
    }
  })
