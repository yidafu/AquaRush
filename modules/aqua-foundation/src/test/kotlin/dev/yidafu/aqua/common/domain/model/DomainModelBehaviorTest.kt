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

import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DomainModelBehaviorTest {
  // ========== ProductModel ==========

  private fun product(
    price: Long = 1000L,
    stock: Int = 5,
    status: ProductModelStatus = ProductModelStatus.ONLINE,
  ): ProductModel =
    ProductModel(
      id = 1L,
      name = "水",
      price = price,
      originalPrice = 1200L,
      depositPrice = 50L,
      stock = stock,
      status = status,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  @Test
  fun `ProductModel priceYuan should convert cents`() {
    assertEquals(BigDecimal("10.00"), product().priceYuan)
    assertEquals(BigDecimal("12.00"), product().originalPriceYuan)
    assertEquals(BigDecimal("0.50"), product().depositPriceYuan)
  }

  @Test
  fun `ProductModel originalPriceYuan should be null when unset`() {
    val p = ProductModel(name = "水", price = 100L)
    assertNull(p.originalPriceYuan)
    assertNull(p.depositPriceYuan)
  }

  @Test
  fun `ProductModel isLowStock should check threshold`() {
    assertTrue(product(stock = 5).isLowStock())
    assertTrue(product(stock = 10).isLowStock())
    assertFalse(product(stock = 11).isLowStock())
    assertTrue(product(stock = 3).isLowStock(threshold = 5))
    assertFalse(product(stock = 6).isLowStock(threshold = 5))
  }

  @Test
  fun `ProductModel isAvailable should require online and stock`() {
    assertTrue(product(stock = 1).isAvailable())
    assertFalse(product(stock = 0).isAvailable())
    assertFalse(product(status = ProductModelStatus.OFFLINE, stock = 5).isAvailable())
    assertFalse(product(status = ProductModelStatus.OUT_OF_STOCK, stock = 5).isAvailable())
  }

  @Test
  fun `ProductModel tags list round trip`() {
    val p = product()
    assertTrue(p.getTagsAsList().isEmpty())
    p.setTagsFromList(listOf("hot", "new"))
    assertEquals(listOf("hot", "new"), p.getTagsAsList())
    p.setTagsFromList(emptyList())
    assertNull(p.tags)
  }

  @Test
  fun `ProductModel image gallery round trip`() {
    val p = product()
    assertTrue(p.getImageGalleryAsList().isEmpty())
    p.setImageGalleryFromList(listOf("http://a.png", "http://b.png"))
    assertEquals(listOf("http://a.png", "http://b.png"), p.getImageGalleryAsList())
    p.setImageGalleryFromList(emptyList())
    assertNull(p.imageGallery)
  }

  @Test
  fun `ProductModel certificate images round trip`() {
    val p = product()
    assertTrue(p.getCertificateImagesAsList().isEmpty())
    p.setCertificateImagesFromList(listOf("http://c.png"))
    assertEquals(listOf("http://c.png"), p.getCertificateImagesAsList())
  }

  @Test
  fun `ProductModel delivery settings round trip`() {
    val p = product()
    assertTrue(p.getDeliverySettingsAsMap().isEmpty())
    p.setDeliverySettingsFromMap(mapOf("freeFloor" to 5))
    assertEquals(mapOf("freeFloor" to 5), p.getDeliverySettingsAsMap())
    p.setDeliverySettingsFromMap(emptyMap())
    assertNull(p.deliverySettings)
  }

  @Test
  fun `ProductModel should ignore non-textual array entries`() {
    val mapper = jacksonObjectMapper()
    val p = product()
    val node = mapper.createArrayNode().add(123)
    p.tags = node
    assertTrue(p.getTagsAsList().isEmpty())
  }

  // ========== OrderModel ==========

  @Test
  fun `OrderModel amount and totalAmount should convert`() {
    val order = OrderModel(orderNo = "O1", userId = 1L, amountCents = 12345L, addressId = 1L)
    assertEquals(BigDecimal("123.45"), order.amount)
    assertEquals(BigDecimal("123.45"), order.totalAmount)
  }

  @Test
  fun `OrderModel payAmountCents should include delivery fee`() {
    val order = OrderModel(orderNo = "O1", userId = 1L, amountCents = 1000L, deliveryFeeCents = 300L, addressId = 1L)
    assertEquals(1300L, order.payAmountCents)
  }

  @Test
  fun `OrderModel status transitions should be mutable`() {
    val order = OrderModel(orderNo = "O1", userId = 1L, addressId = 1L)
    assertEquals(dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus.PENDING_PAYMENT, order.status)
    order.status = dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus.PENDING_DISPATCH
    assertEquals(dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus.PENDING_DISPATCH, order.status)
  }

  // ========== UserModel ==========

  @Test
  fun `UserModel balance and totalSpent should convert cents`() {
    val user =
      UserModel(
        wechatOpenId = "openid",
        email = "e@e.com",
        balanceCents = 500L,
        totalSpentCents = 12345L,
        status = UserStatus.ACTIVE,
        role = UserRole.USER,
      )
    assertEquals(BigDecimal("5.00"), user.balance)
    assertEquals(BigDecimal("123.45"), user.totalSpent)
  }

  @Test
  fun `UserModel defaults should apply`() {
    val user = UserModel(wechatOpenId = "openid", email = "e@e.com")
    assertEquals(0L, user.balanceCents)
    assertEquals(0L, user.totalSpentCents)
    assertEquals(UserStatus.INACTIVE, user.status)
    assertEquals(UserRole.NONE, user.role)
  }

  // ========== AdminModel ==========

  @Test
  fun `AdminModel earning should convert cents`() {
    val admin = AdminModel(username = "admin", passwordHash = "hash", earningCents = 9999L)
    assertEquals(BigDecimal("99.99"), admin.earning)
  }

  @Test
  fun `AdminModel earning should be null when unset`() {
    val admin = AdminModel(username = "admin", passwordHash = "hash")
    assertNull(admin.earning)
  }

  @Test
  fun `DeliverWorkerStatusConverter should convert to label`() {
    val converter = DeliverWorkerStatusConverter()
    assertEquals("ONLINE", converter.convertToDatabaseColumn(DeliverWorkerModelStatus.ONLINE))
    assertEquals("OFFLINE", converter.convertToDatabaseColumn(DeliverWorkerModelStatus.OFFLINE))
    assertNull(converter.convertToDatabaseColumn(null))
  }

  @Test
  fun `DeliverWorkerStatusConverter should parse labels`() {
    val converter = DeliverWorkerStatusConverter()
    assertEquals(DeliverWorkerModelStatus.ONLINE, converter.convertToEntityAttribute("ONLINE"))
    assertEquals(DeliverWorkerModelStatus.ONLINE, converter.convertToEntityAttribute("Online"))
    assertEquals(DeliverWorkerModelStatus.OFFLINE, converter.convertToEntityAttribute("OFFLINE"))
    assertEquals(DeliverWorkerModelStatus.OFFLINE, converter.convertToEntityAttribute("Offline"))
    assertEquals(DeliverWorkerModelStatus.OFFLINE, converter.convertToEntityAttribute("UNKNOWN"))
    assertEquals(DeliverWorkerModelStatus.OFFLINE, converter.convertToEntityAttribute(null))
  }

  // ========== DailyCollectionRecordModel ==========

  @Test
  fun `DailyCollectionRecordModel totals should sum fields`() {
    val record =
      DailyCollectionRecordModel(
        cashAmountCents = 1000L,
        qrCodeAmountCents = 2000L,
        calculatedCashAmountCents = 900L,
        calculatedQrCodeAmountCents = 2100L,
        orderAmountCents = 3000L,
      )
    assertEquals(3000L, record.getManualTotalCollectionAmountCents())
    assertEquals(3000L, record.getCalculatedTotalCollectionAmountCents())
    assertEquals(3000L, record.getOrderAmountCentsLegacy())
  }

  // ========== FeedbackModel ==========

  @Test
  fun `FeedbackModel images list round trip`() {
    val feedback = FeedbackModel(userId = 1L, content = "feedback")
    assertTrue(feedback.getImagesAsList().isEmpty())
    feedback.setImagesFromList(listOf("http://img1.png"))
    assertEquals(listOf("http://img1.png"), feedback.getImagesAsList())
    feedback.setImagesFromList(emptyList())
    assertNull(feedback.images)
  }

  // ========== PaymentModel ==========

  @Test
  fun `PaymentModel should expose status enum`() {
    val payment = PaymentModel(orderId = 1L, userId = 1L, amount = 100L)
    assertEquals(PaymentStatus.PENDING, payment.status)
    assertEquals(PaymentMethod.WECHAT_PAY, payment.paymentMethod)
    assertEquals("CNY", payment.currency)
  }

  // ========== enums ==========

  @Test
  fun `label enums should expose non-empty labels`() {
    assertTrue(ProductModelStatus.entries.all { it.label.isNotBlank() })
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.NoticeStatus.entries
        .all { it.label.isNotBlank() },
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.NoticeType.entries
        .all { it.label.isNotBlank() },
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus.entries
        .all { it.label.isNotBlank() },
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.FeedbackType.entries
        .all { it.label.isNotBlank() },
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup.entries
        .all { it.label.isNotBlank() },
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.SystemSettingType.entries
        .all { it.label.isNotBlank() },
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.BannerLinkType.entries
        .all { it.label.isNotBlank() },
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.AdminPermission.entries
        .all { it.description.isNotBlank() },
    )
    assertTrue(DeliverWorkerModelStatus.entries.all { it.label.isNotBlank() })
  }

  @Test
  fun `plain enums should have expected entries`() {
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.PaymentType.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.CouponType.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.GiftActivityType.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.CouponScope.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.PromotionScope.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.ValidityType.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.PriceSource.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.EventStatus.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus.entries
        .isNotEmpty(),
    )
    assertTrue(
      dev.yidafu.aqua.common.domain.model.enums.SourceSystem.entries
        .isNotEmpty(),
    )
    assertTrue(MessageStatus.entries.isNotEmpty())
  }
}
