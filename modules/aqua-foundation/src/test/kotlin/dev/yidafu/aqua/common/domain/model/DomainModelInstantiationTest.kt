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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

/**
 * 批量实例化领域模型,覆盖构造器与默认值逻辑。
 */
class DomainModelInstantiationTest :
  FunSpec({

    test("address model should instantiate with defaults") {
      val address =
        AddressModel(userId = 1L, receiverName = "r", phone = "p", province = "省", city = "市", district = "区", detailAddress = "d")
      address.provinceCode shouldBe ""
      address.isDefault shouldBe false
      address.longitude.shouldBeNull()
      address.isDefault = true
      address.isDefault shouldBe true
    }
    test("admin model should instantiate with defaults") {
      val admin = AdminModel(username = "u", passwordHash = "h")
      admin.role shouldBe AdminRoleModel.ADMIN
      admin.accountStatus shouldBe AdminAccountStatus.ACTIVE
      admin.onlineStatus shouldBe DeliverWorkerModelStatus.OFFLINE
      admin.isAvailable shouldBe true
      admin.userId.shouldBeNull()
    }
    test("delivery address model should instantiate") {
      val address =
        DeliveryAddressModel(
          userId = 1L,
          receiverName = "r",
          receiverPhone = "p",
          province = "省",
          city = "市",
          district = "区",
          detailAddress = "d",
        )
      address.createdAt.shouldNotBeNull()
    }
    test("delivery area model should instantiate with defaults") {
      val area = DeliveryAreaModel(name = "区域", province = "省", city = "市", district = "区")
      area.enabled shouldBe true
    }
    test("message history model should instantiate") {
      val message =
        MessageHistoryModel(
          userId = 1L,
          messageType = "ORDER",
          templateId = "t",
          content = "c",
          status = MessageStatus.PENDING,
          sentAt = null,
          wxMessageId = null,
          errorMessage = null,
        )
      message.status shouldBe MessageStatus.PENDING
      message.sentAt.shouldBeNull()
    }
    test("order operation model should instantiate with defaults") {
      val operation = OrderOperationModel()
      operation.orderId shouldBe -1L
      operation.operationType shouldBe OrderOperationType.ORDER_CREATED
      operation.operatorType shouldBe OperatorType.SYSTEM
    }
    test("payment refund model should instantiate") {
      val refund = PaymentRefundModel(paymentId = 1L, orderId = 2L, userId = 3L, refundAmount = 100L)
      refund.status shouldBe RefundStatus.PENDING
    }
    test("product favorite model should instantiate with defaults") {
      val favorite = ProductFavoriteModel()
      favorite.userId shouldBe -1L
      favorite.productId shouldBe -1L
      favorite.enable shouldBe true
    }
    test("review model should instantiate") {
      val review = ReviewModel(orderId = 1L, userId = 1L, deliveryWorkerId = 1L, rating = 5)
      review.rating shouldBe 5
      review.isAnonymous shouldBe false
    }
    test("ticket purchase model should instantiate with defaults") {
      val purchase = TicketPurchaseModel()
      purchase.userId shouldBe 0L
      purchase.ticketTypeId shouldBe 0L
      purchase.totalCount shouldBe 0
    }
    test("delivery trajectory model should instantiate") {
      val trajectory = DeliveryTrajectoryModel(deliveryWorkerId = 1L, orderId = 2L, longitude = 114.0, latitude = 22.5)
      trajectory.longitude shouldBe 114.0
      trajectory.latitude shouldBe 22.5
      trajectory.accuracy.shouldBeNull()
    }
    test("notification settings models should instantiate") {
      val userSettings = UserNotificationSettingsModel(userId = 1L)
      userSettings.orderUpdates shouldBe true
      userSettings.paymentNotifications shouldBe true
      userSettings.deliveryNotifications shouldBe true
      !userSettings.promotionalNotifications shouldBe true
    }
    test("marketing models should instantiate with defaults") {
      CouponTemplateModel().shouldNotBeNull()
      UserCouponModel().shouldNotBeNull()
      UserTicketModel().shouldNotBeNull()
      TicketTypeModel().shouldNotBeNull()
      PromotionActivityModel().shouldNotBeNull()
      FlashSaleActivityModel().shouldNotBeNull()
      GiftActivityModel().shouldNotBeNull()
      FirstOrderActivityModel().shouldNotBeNull()
      CustomerGroupModel().shouldNotBeNull()
      GroupProductPriceModel().shouldNotBeNull()
      PromotionActivityTierModel().shouldNotBeNull()
      OrderGiftModel().shouldNotBeNull()
      TicketPurchaseModel().shouldNotBeNull()
    }
    test("ops models should instantiate with defaults") {
      NoticeModel().shouldNotBeNull()
      BannerModel().shouldNotBeNull()
      FeedbackModel().shouldNotBeNull()
      StoreProfileModel().shouldNotBeNull()
      SystemSettingsModel().shouldNotBeNull()
      DeliveryFeeRuleModel().shouldNotBeNull()
    }
    test("reconciliation models should instantiate") {
      ReconciliationTaskModel().shouldNotBeNull()
      ReconciliationDiscrepancyModel().shouldNotBeNull()
      ReconciliationReportModel().shouldNotBeNull()
      DailyCollectionRecordModel().shouldNotBeNull()
      DailyReconciliationModel().shouldNotBeNull()
    }
    test("log models should instantiate with defaults") {
      val apiLog = ApiLogModel()
      apiLog.requestType shouldBe RequestType.HTTP
      apiLog.uri.shouldBeNull()
      val businessLog = BusinessLogModel()
      businessLog.level shouldBe "INFO"
      businessLog.operation.shouldBeNull()
      val userActionLog = UserActionLogModel()
      userActionLog.actionType shouldBe "UNKNOWN"
      userActionLog.userId.shouldBeNull()
    }
    test("region model should instantiate") {
      val region = RegionModel(name = "省", code = "440000", level = 1)
      region.name shouldBe "省"
      region.code shouldBe "440000"
      region.parentCode.shouldBeNull()
    }
    test("delivery worker statistics should instantiate with defaults") {
      val stats = DeliveryWorkerStatisticsModel()
      stats.deliveryWorkerId shouldBe -1L
      stats.totalReviews shouldBe 0
    }
    test("soft deletable fields should default to null") {
      val banner = BannerModel()
      banner.deletedAt.shouldBeNull()
      banner.deletedBy.shouldBeNull()
      banner.deletedAt = LocalDateTime.now()
      banner.deletedBy = 1L
      banner.deletedAt.shouldNotBeNull()
      banner.deletedBy shouldBe 1L
    }
    test("order model should instantiate with defaults") {
      val order = OrderModel()
      order.orderNo shouldBe ""
      order.userId shouldBe -1L
      order.amountCents shouldBe 0L
      order.deliveryFeeCents shouldBe 0L
      order.addressId shouldBe 0L
      order.ticketRedeemCount shouldBe 0
    }
    test("product model should instantiate with defaults") {
      val product = ProductModel()
      product.name shouldBe ""
      product.price shouldBe 0L
      product.status shouldBe ProductModelStatus.OFFLINE
      product.sortOrder shouldBe 999
    }
    test("user model should instantiate with defaults") {
      val user = UserModel()
      user.wechatOpenId shouldBe ""
      user.email shouldBe ""
      user.nickname.shouldBeNull()
    }
  })
