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

package dev.yidafu.aqua.logging.formatter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import tools.jackson.databind.ObjectMapper

class BusinessLogFormatterTest :
  FunSpec({
    val formatter = BusinessLogFormatter.instance
    val mapper = ObjectMapper()

    fun parse(json: String): Map<String, Any> = mapper.readValue(json, Map::class.java) as Map<String, Any>

    @Suppress("UNCHECKED_CAST")

    test("formatBusinessOperation success should produce INFO json") {
      val json = formatter.formatBusinessOperation("CREATE", "ORDER", "SUCCESS", businessId = "1", description = "创建订单")
      val map = parse(json)
      map["level"] shouldBe "INFO"
      map["logger"] shouldBe "business"
      map["businessOperation"] shouldBe "CREATE"
      map["businessModule"] shouldBe "ORDER"
      map["businessCategory"] shouldBe "ORDER_CREATION"
      map["businessId"] shouldBe "1"
      map["businessDomain"] shouldBe "ORDER_MANAGEMENT"
    }
    test("formatBusinessOperation failure should produce ERROR level") {
      val map = parse(formatter.formatBusinessOperation("CREATE", "PAYMENT", "FAILURE"))
      map["level"] shouldBe "ERROR"
      map["businessCategory"] shouldBe "PAYMENT_PROCESSING"
    }
    test("formatBusinessOperation warning should produce WARN level") {
      val map = parse(formatter.formatBusinessOperation("CREATE", "DELIVERY", "PARTIAL"))
      map["level"] shouldBe "WARN"
      map["businessCategory"] shouldBe "DELIVERY_MANAGEMENT"
    }
    test("formatBusinessOperation default level should be INFO") {
      val map = parse(formatter.formatBusinessOperation("CREATE", "UNKNOWN_MODULE", "OTHER"))
      map["level"] shouldBe "INFO"
      map["businessCategory"] shouldBe "GENERAL_BUSINESS"
    }
    test("formatBusinessOperation with metadata should include workflow stage") {
      val map = parse(formatter.formatBusinessOperation("UPDATE", "ORDER", "SUCCESS", metadata = mapOf("workflowStage" to "CONFIRMED")))
      map["workflowStage"] shouldBe "CONFIRMED"
    }
    test("formatBusinessOperation with metadata should include transaction type") {
      val map = parse(formatter.formatBusinessOperation("REFUND", "PAYMENT", "SUCCESS", metadata = mapOf("transactionType" to "REFUND")))
      map["transactionType"] shouldBe "REFUND"
    }
    test("formatBusinessOperation with delivery metadata should default delivery type") {
      val map = parse(formatter.formatBusinessOperation("ASSIGN", "DELIVERY", "SUCCESS"))
      map["deliveryType"] shouldBe "STANDARD"
    }
    test("formatBusinessOperation user metadata should default user type") {
      val map = parse(formatter.formatBusinessOperation("CREATE", "USER", "SUCCESS"))
      map["userType"] shouldBe "CUSTOMER"
    }
    test("formatBusinessOperation product metadata should default product type") {
      val map = parse(formatter.formatBusinessOperation("CREATE", "PRODUCT", "SUCCESS"))
      map["productType"] shouldBe "WATER"
    }
    test("formatOrderLog completed should produce INFO") {
      val map = parse(formatter.formatOrderLog("PAY", "100", status = "COMPLETED"))
      map["level"] shouldBe "INFO"
      map["businessCategory"] shouldBe "ORDER_MANAGEMENT"
      map["orderId"] shouldBe "100"
    }
    test("formatOrderLog cancelled should produce ERROR") {
      val map = parse(formatter.formatOrderLog("CANCEL", "100", status = "CANCELLED", userId = "7"))
      map["level"] shouldBe "ERROR"
      map["customerId"] shouldBe "7"
    }
    test("formatOrderLog partial should produce WARN") {
      val map = parse(formatter.formatOrderLog("UPDATE", "100", status = "PARTIAL"))
      map["level"] shouldBe "WARN"
    }
    test("formatOrderLog with details should include order details") {
      val map = parse(formatter.formatOrderLog("CREATE", "100", status = "PENDING", details = mapOf("items" to 3)))
      map["orderDetails"] shouldBe mapOf("items" to 3)
    }
    test("formatPaymentLog success should produce INFO with currency") {
      val map = parse(formatter.formatPaymentLog("PAY", "p1", "o1", 100.0, "SUCCESS", paymentMethod = "WECHAT"))
      map["level"] shouldBe "INFO"
      map["currency"] shouldBe "CNY"
      map["paymentMethod"] shouldBe "WECHAT"
    }
    test("formatPaymentLog declined should produce ERROR") {
      val map = parse(formatter.formatPaymentLog("PAY", "p1", "o1", 100.0, "DECLINED"))
      map["level"] shouldBe "ERROR"
    }
    test("formatPaymentLog refunded should produce INFO") {
      val map = parse(formatter.formatPaymentLog("REFUND", "p1", "o1", 50.0, "REFUNDED"))
      map["level"] shouldBe "INFO"
    }
    test("formatPaymentLog with details should include payment details") {
      val map = parse(formatter.formatPaymentLog("PAY", "p1", "o1", 100.0, "SUCCESS", details = mapOf("fee" to 1)))
      map["paymentDetails"] shouldBe mapOf("fee" to 1)
    }
    test("formatDeliveryLog delivered should produce INFO") {
      val map = parse(formatter.formatDeliveryLog("COMPLETE", "d1", "o1", "DELIVERED", workerId = "w1"))
      map["level"] shouldBe "INFO"
      map["workerId"] shouldBe "w1"
    }
    test("formatDeliveryLog failed should produce ERROR") {
      val map = parse(formatter.formatDeliveryLog("COMPLETE", "d1", "o1", "FAILED"))
      map["level"] shouldBe "ERROR"
    }
    test("formatDeliveryLog cancelled should produce WARN") {
      val map = parse(formatter.formatDeliveryLog("CANCEL", "d1", "o1", "CANCELLED"))
      map["level"] shouldBe "WARN"
    }
    test("formatUserLog success should produce INFO") {
      val map = parse(formatter.formatUserLog("CREATE", "u1", "SUCCESS", username = "alice"))
      map["level"] shouldBe "INFO"
      map["targetUsername"] shouldBe "alice"
    }
    test("formatUserLog error should produce ERROR") {
      val map = parse(formatter.formatUserLog("CREATE", "u1", "ERROR"))
      map["level"] shouldBe "ERROR"
    }
    test("formatProductLog created should produce INFO") {
      val map = parse(formatter.formatProductLog("CREATE", "p1", "CREATED", productName = "水"))
      map["level"] shouldBe "INFO"
      map["productName"] shouldBe "水"
    }
    test("formatProductLog error should produce ERROR") {
      val map = parse(formatter.formatProductLog("CREATE", "p1", "ERROR"))
      map["level"] shouldBe "ERROR"
    }
    test("formatBusinessOperation with null optional fields should omit them") {
      val map = parse(formatter.formatBusinessOperation("CREATE", "ORDER", "SUCCESS"))
      !map.containsKey("businessId") shouldBe true
      !map.containsKey("businessDescription") shouldBe true
      !map.containsKey("businessMetadata") shouldBe true
    }
    test("formatOrderLog with null user should omit customer id") {
      val map = parse(formatter.formatOrderLog("PAY", "100", status = "COMPLETED"))
      !map.containsKey("customerId") shouldBe true
    }
  })
