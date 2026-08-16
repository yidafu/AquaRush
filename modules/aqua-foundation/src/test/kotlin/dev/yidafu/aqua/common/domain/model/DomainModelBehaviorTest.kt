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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.LocalDateTime

class DomainModelBehaviorTest :
  FunSpec({
    fun product(
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
    // ========== ProductModel ==========

    // ========== OrderModel ==========

    // ========== UserModel ==========

    // ========== AdminModel ==========

    // ========== DailyCollectionRecordModel ==========

    // ========== FeedbackModel ==========

    // ========== PaymentModel ==========

    // ========== enums ==========

    test("ProductModel priceYuan should convert cents") {
      product().priceYuan shouldBe BigDecimal("10.00")
      product().originalPriceYuan shouldBe BigDecimal("12.00")
      product().depositPriceYuan shouldBe BigDecimal("0.50")
    }
    test("ProductModel originalPriceYuan should be null when unset") {
      val p = ProductModel(name = "水", price = 100L)
      p.originalPriceYuan.shouldBeNull()
      p.depositPriceYuan.shouldBeNull()
    }
    test("ProductModel isLowStock should check threshold") {
      product(stock = 5).isLowStock() shouldBe true
      product(stock = 10).isLowStock() shouldBe true
      product(stock = 11).isLowStock() shouldBe false
      product(stock = 3).isLowStock(threshold = 5) shouldBe true
      product(stock = 6).isLowStock(threshold = 5) shouldBe false
    }
    test("ProductModel isAvailable should require online and stock") {
      product(stock = 1).isAvailable() shouldBe true
      product(stock = 0).isAvailable() shouldBe false
      product(status = ProductModelStatus.OFFLINE, stock = 5).isAvailable() shouldBe false
      product(status = ProductModelStatus.OUT_OF_STOCK, stock = 5).isAvailable() shouldBe false
    }
    test("ProductModel tags list round trip") {
      val p = product()
      p.getTagsAsList().isEmpty() shouldBe true
      p.setTagsFromList(listOf("hot", "new"))
      p.getTagsAsList() shouldBe listOf("hot", "new")
      p.setTagsFromList(emptyList())
      p.tags.shouldBeNull()
    }
    test("ProductModel image gallery round trip") {
      val p = product()
      p.getImageGalleryAsList().isEmpty() shouldBe true
      p.setImageGalleryFromList(listOf("http://a.png", "http://b.png"))
      p.getImageGalleryAsList() shouldBe listOf("http://a.png", "http://b.png")
      p.setImageGalleryFromList(emptyList())
      p.imageGallery.shouldBeNull()
    }
    test("ProductModel certificate images round trip") {
      val p = product()
      p.getCertificateImagesAsList().isEmpty() shouldBe true
      p.setCertificateImagesFromList(listOf("http://c.png"))
      p.getCertificateImagesAsList() shouldBe listOf("http://c.png")
    }
    test("ProductModel should ignore non-textual array entries") {
      val mapper = jacksonObjectMapper()
      val p = product()
      val node = mapper.createArrayNode().add(123)
      p.tags = node
      p.getTagsAsList().isEmpty() shouldBe true
    }
    test("OrderModel amount and totalAmount should convert") {
      val order = OrderModel(orderNo = "O1", userId = 1L, amountCents = 12345L, addressId = 1L)
      order.amount shouldBe BigDecimal("123.45")
      order.totalAmount shouldBe BigDecimal("123.45")
    }
    test("OrderModel payAmountCents should include delivery fee") {
      val order = OrderModel(orderNo = "O1", userId = 1L, amountCents = 1000L, deliveryFeeCents = 300L, addressId = 1L)
      order.payAmountCents shouldBe 1300L
    }
    test("OrderModel status transitions should be mutable") {
      val order = OrderModel(orderNo = "O1", userId = 1L, addressId = 1L)
      order.status shouldBe dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus.PENDING_PAYMENT
      order.status = dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus.PENDING_DISPATCH
      order.status shouldBe dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus.PENDING_DISPATCH
    }
    test("UserModel balance and totalSpent should convert cents") {
      val user =
        UserModel(
          wechatOpenId = "openid",
          email = "e@e.com",
          balanceCents = 500L,
          totalSpentCents = 12345L,
          status = UserStatus.ACTIVE,
          role = UserRole.USER,
        )
      user.balance shouldBe BigDecimal("5.00")
      user.totalSpent shouldBe BigDecimal("123.45")
    }
    test("UserModel defaults should apply") {
      val user = UserModel(wechatOpenId = "openid", email = "e@e.com")
      user.balanceCents shouldBe 0L
      user.totalSpentCents shouldBe 0L
      user.status shouldBe UserStatus.INACTIVE
      user.role shouldBe UserRole.NONE
    }
    test("AdminModel earning should convert cents") {
      val admin = AdminModel(username = "admin", passwordHash = "hash", earningCents = 9999L)
      admin.earning shouldBe BigDecimal("99.99")
    }
    test("AdminModel earning should be null when unset") {
      val admin = AdminModel(username = "admin", passwordHash = "hash")
      admin.earning.shouldBeNull()
    }
    test("DeliverWorkerStatusConverter should convert to label") {
      val converter = DeliverWorkerStatusConverter()
      converter.convertToDatabaseColumn(DeliverWorkerModelStatus.ONLINE) shouldBe "ONLINE"
      converter.convertToDatabaseColumn(DeliverWorkerModelStatus.OFFLINE) shouldBe "OFFLINE"
      converter.convertToDatabaseColumn(null).shouldBeNull()
    }
    test("DeliverWorkerStatusConverter should parse labels") {
      val converter = DeliverWorkerStatusConverter()
      converter.convertToEntityAttribute("ONLINE") shouldBe DeliverWorkerModelStatus.ONLINE
      converter.convertToEntityAttribute("Online") shouldBe DeliverWorkerModelStatus.ONLINE
      converter.convertToEntityAttribute("OFFLINE") shouldBe DeliverWorkerModelStatus.OFFLINE
      converter.convertToEntityAttribute("Offline") shouldBe DeliverWorkerModelStatus.OFFLINE
      converter.convertToEntityAttribute("UNKNOWN") shouldBe DeliverWorkerModelStatus.OFFLINE
      converter.convertToEntityAttribute(null) shouldBe DeliverWorkerModelStatus.OFFLINE
    }
    test("DailyCollectionRecordModel totals should sum fields") {
      val record =
        DailyCollectionRecordModel(
          cashAmountCents = 1000L,
          qrCodeAmountCents = 2000L,
          calculatedCashAmountCents = 900L,
          calculatedQrCodeAmountCents = 2100L,
          orderAmountCents = 3000L,
        )
      record.getManualTotalCollectionAmountCents() shouldBe 3000L
      record.getCalculatedTotalCollectionAmountCents() shouldBe 3000L
      record.getOrderAmountCentsLegacy() shouldBe 3000L
    }
    test("FeedbackModel images list round trip") {
      val feedback = FeedbackModel(userId = 1L, content = "feedback")
      feedback.getImagesAsList().isEmpty() shouldBe true
      feedback.setImagesFromList(listOf("http://img1.png"))
      feedback.getImagesAsList() shouldBe listOf("http://img1.png")
      feedback.setImagesFromList(emptyList())
      feedback.images.shouldBeNull()
    }
    test("PaymentModel should expose status enum") {
      val payment = PaymentModel(orderId = 1L, userId = 1L, amount = 100L)
      payment.status shouldBe PaymentStatus.PENDING
      payment.paymentMethod shouldBe PaymentMethod.WECHAT_PAY
      payment.currency shouldBe "CNY"
    }
    test("label enums should expose non-empty labels") {
      ProductModelStatus.entries.all { it.label.isNotBlank() } shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.NoticeStatus.entries
        .all { it.label.isNotBlank() } shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.NoticeType.entries
        .all { it.label.isNotBlank() } shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus.entries
        .all { it.label.isNotBlank() } shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.FeedbackType.entries
        .all { it.label.isNotBlank() } shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup.entries
        .all { it.label.isNotBlank() } shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.SystemSettingType.entries
        .all { it.label.isNotBlank() } shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.BannerLinkType.entries
        .all { it.label.isNotBlank() } shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.AdminPermission.entries
        .all { it.description.isNotBlank() } shouldBe true
      DeliverWorkerModelStatus.entries.all { it.label.isNotBlank() } shouldBe true
    }
    test("plain enums should have expected entries") {
      dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.PaymentType.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.CouponType.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.GiftActivityType.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.CouponScope.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.PromotionScope.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.ValidityType.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.PriceSource.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.EventStatus.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus.entries
        .isNotEmpty() shouldBe true
      dev.yidafu.aqua.common.domain.model.enums.SourceSystem.entries
        .isNotEmpty() shouldBe true
      MessageStatus.entries.isNotEmpty() shouldBe true
    }
  })
