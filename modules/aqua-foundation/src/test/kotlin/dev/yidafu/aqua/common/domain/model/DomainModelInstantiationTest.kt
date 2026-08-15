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
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 批量实例化领域模型,覆盖构造器与默认值逻辑。
 */
class DomainModelInstantiationTest {
  @Test
  fun `address model should instantiate with defaults`() {
    val address =
      AddressModel(userId = 1L, receiverName = "r", phone = "p", province = "省", city = "市", district = "区", detailAddress = "d")
    assertEquals("", address.provinceCode)
    assertEquals(false, address.isDefault)
    assertNull(address.longitude)
    address.isDefault = true
    assertTrue(address.isDefault)
  }

  @Test
  fun `admin model should instantiate with defaults`() {
    val admin = AdminModel(username = "u", passwordHash = "h")
    assertEquals(AdminRoleModel.ADMIN, admin.role)
    assertEquals(AdminAccountStatus.ACTIVE, admin.accountStatus)
    assertEquals(DeliverWorkerModelStatus.OFFLINE, admin.onlineStatus)
    assertTrue(admin.isAvailable)
    assertNull(admin.userId)
  }

  @Test
  fun `delivery address model should instantiate`() {
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
    assertNotNull(address.createdAt)
  }

  @Test
  fun `delivery area model should instantiate with defaults`() {
    val area = DeliveryAreaModel(name = "区域", province = "省", city = "市", district = "区")
    assertTrue(area.enabled)
  }

  @Test
  fun `message history model should instantiate`() {
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
    assertEquals(MessageStatus.PENDING, message.status)
    assertNull(message.sentAt)
  }

  @Test
  fun `order operation model should instantiate with defaults`() {
    val operation = OrderOperationModel()
    assertEquals(-1L, operation.orderId)
    assertEquals(OrderOperationType.ORDER_CREATED, operation.operationType)
    assertEquals(OperatorType.SYSTEM, operation.operatorType)
  }

  @Test
  fun `payment refund model should instantiate`() {
    val refund = PaymentRefundModel(paymentId = 1L, refundAmount = 100L)
    assertEquals(RefundStatus.PENDING, refund.status)
  }

  @Test
  fun `product favorite model should instantiate with defaults`() {
    val favorite = ProductFavoriteModel()
    assertEquals(-1L, favorite.userId)
    assertEquals(-1L, favorite.productId)
    assertTrue(favorite.enable)
  }

  @Test
  fun `review model should instantiate`() {
    val review = ReviewModel(orderId = 1L, userId = 1L, deliveryWorkerId = 1L, rating = 5)
    assertEquals(5, review.rating)
    assertEquals(false, review.isAnonymous)
  }

  @Test
  fun `ticket purchase model should instantiate with defaults`() {
    val purchase = TicketPurchaseModel()
    assertEquals(0L, purchase.userId)
    assertEquals(0L, purchase.ticketTypeId)
    assertEquals(0, purchase.totalCount)
  }

  @Test
  fun `delivery trajectory model should instantiate`() {
    val trajectory = DeliveryTrajectoryModel(deliveryWorkerId = 1L, orderId = 2L, longitude = 114.0, latitude = 22.5)
    assertEquals(114.0, trajectory.longitude)
    assertEquals(22.5, trajectory.latitude)
    assertNull(trajectory.accuracy)
  }

  @Test
  fun `notification settings models should instantiate`() {
    val settings = NotificationSettingsModel(userId = 1L)
    assertTrue(settings.orderUpdates)
    assertTrue(settings.paymentNotifications)
    assertTrue(settings.deliveryNotifications)
    assertTrue(!settings.promotionalNotifications)
    val userSettings = UserNotificationSettingsModel(userId = 1L)
    assertTrue(userSettings.orderUpdates)
  }

  @Test
  fun `marketing models should instantiate with defaults`() {
    assertNotNull(CouponTemplateModel())
    assertNotNull(UserCouponModel())
    assertNotNull(UserTicketModel())
    assertNotNull(TicketTypeModel())
    assertNotNull(PromotionActivityModel())
    assertNotNull(FlashSaleActivityModel())
    assertNotNull(GiftActivityModel())
    assertNotNull(FirstOrderActivityModel())
    assertNotNull(CustomerGroupModel())
    assertNotNull(GroupProductPriceModel())
    assertNotNull(PromotionActivityTierModel())
    assertNotNull(OrderGiftModel())
    assertNotNull(TicketPurchaseModel())
  }

  @Test
  fun `ops models should instantiate with defaults`() {
    assertNotNull(NoticeModel())
    assertNotNull(BannerModel())
    assertNotNull(FeedbackModel())
    assertNotNull(StoreProfileModel())
    assertNotNull(SystemSettingsModel())
    assertNotNull(DeliveryFeeRuleModel())
  }

  @Test
  fun `reconciliation models should instantiate`() {
    assertNotNull(ReconciliationTaskModel())
    assertNotNull(ReconciliationDiscrepancyModel())
    assertNotNull(ReconciliationReportModel())
    assertNotNull(DailyCollectionRecordModel())
    assertNotNull(DailyReconciliationModel())
  }

  @Test
  fun `log models should instantiate with defaults`() {
    val apiLog = ApiLogModel()
    assertEquals(RequestType.HTTP, apiLog.requestType)
    assertNull(apiLog.uri)
    val businessLog = BusinessLogModel()
    assertEquals("INFO", businessLog.level)
    assertNull(businessLog.operation)
    val userActionLog = UserActionLogModel()
    assertEquals("UNKNOWN", userActionLog.actionType)
    assertNull(userActionLog.userId)
  }

  @Test
  fun `region model should instantiate`() {
    val region = RegionModel(name = "省", code = "440000", level = 1)
    assertEquals("省", region.name)
    assertEquals("440000", region.code)
    assertNull(region.parentCode)
  }

  @Test
  fun `delivery worker statistics should instantiate with defaults`() {
    val stats = DeliveryWorkerStatisticsModel()
    assertEquals(-1L, stats.deliveryWorkerId)
    assertEquals(0, stats.totalReviews)
  }

  @Test
  fun `soft deletable fields should default to null`() {
    val banner = BannerModel()
    assertNull(banner.deletedAt)
    assertNull(banner.deletedBy)
    banner.deletedAt = LocalDateTime.now()
    banner.deletedBy = 1L
    assertNotNull(banner.deletedAt)
    assertEquals(1L, banner.deletedBy)
  }

  @Test
  fun `order model should instantiate with defaults`() {
    val order = OrderModel()
    assertEquals("", order.orderNo)
    assertEquals(-1L, order.userId)
    assertEquals(0L, order.amountCents)
    assertEquals(0L, order.deliveryFeeCents)
    assertEquals(0L, order.addressId)
    assertEquals(0, order.ticketRedeemCount)
  }

  @Test
  fun `product model should instantiate with defaults`() {
    val product = ProductModel()
    assertEquals("", product.name)
    assertEquals(0L, product.price)
    assertEquals(ProductModelStatus.OFFLINE, product.status)
    assertEquals(999, product.sortOrder)
  }

  @Test
  fun `user model should instantiate with defaults`() {
    val user = UserModel()
    assertEquals("", user.wechatOpenId)
    assertEquals("", user.email)
    assertNull(user.nickname)
  }
}
