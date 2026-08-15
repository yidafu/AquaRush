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

import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BusinessLogFormatterTest {
  private val formatter = BusinessLogFormatter.instance
  private val mapper = ObjectMapper()

  @Suppress("UNCHECKED_CAST")
  private fun parse(json: String): Map<String, Any> = mapper.readValue(json, Map::class.java) as Map<String, Any>

  @Test
  fun `formatBusinessOperation success should produce INFO json`() {
    val json = formatter.formatBusinessOperation("CREATE", "ORDER", "SUCCESS", businessId = "1", description = "创建订单")
    val map = parse(json)
    assertEquals("INFO", map["level"])
    assertEquals("business", map["logger"])
    assertEquals("CREATE", map["businessOperation"])
    assertEquals("ORDER", map["businessModule"])
    assertEquals("ORDER_CREATION", map["businessCategory"])
    assertEquals("1", map["businessId"])
    assertEquals("ORDER_MANAGEMENT", map["businessDomain"])
  }

  @Test
  fun `formatBusinessOperation failure should produce ERROR level`() {
    val map = parse(formatter.formatBusinessOperation("CREATE", "PAYMENT", "FAILURE"))
    assertEquals("ERROR", map["level"])
    assertEquals("PAYMENT_PROCESSING", map["businessCategory"])
  }

  @Test
  fun `formatBusinessOperation warning should produce WARN level`() {
    val map = parse(formatter.formatBusinessOperation("CREATE", "DELIVERY", "PARTIAL"))
    assertEquals("WARN", map["level"])
    assertEquals("DELIVERY_MANAGEMENT", map["businessCategory"])
  }

  @Test
  fun `formatBusinessOperation default level should be INFO`() {
    val map = parse(formatter.formatBusinessOperation("CREATE", "UNKNOWN_MODULE", "OTHER"))
    assertEquals("INFO", map["level"])
    assertEquals("GENERAL_BUSINESS", map["businessCategory"])
  }

  @Test
  fun `formatBusinessOperation with metadata should include workflow stage`() {
    val map = parse(formatter.formatBusinessOperation("UPDATE", "ORDER", "SUCCESS", metadata = mapOf("workflowStage" to "CONFIRMED")))
    assertEquals("CONFIRMED", map["workflowStage"])
  }

  @Test
  fun `formatBusinessOperation with metadata should include transaction type`() {
    val map = parse(formatter.formatBusinessOperation("REFUND", "PAYMENT", "SUCCESS", metadata = mapOf("transactionType" to "REFUND")))
    assertEquals("REFUND", map["transactionType"])
  }

  @Test
  fun `formatBusinessOperation with delivery metadata should default delivery type`() {
    val map = parse(formatter.formatBusinessOperation("ASSIGN", "DELIVERY", "SUCCESS"))
    assertEquals("STANDARD", map["deliveryType"])
  }

  @Test
  fun `formatBusinessOperation user metadata should default user type`() {
    val map = parse(formatter.formatBusinessOperation("CREATE", "USER", "SUCCESS"))
    assertEquals("CUSTOMER", map["userType"])
  }

  @Test
  fun `formatBusinessOperation product metadata should default product type`() {
    val map = parse(formatter.formatBusinessOperation("CREATE", "PRODUCT", "SUCCESS"))
    assertEquals("WATER", map["productType"])
  }

  @Test
  fun `formatOrderLog completed should produce INFO`() {
    val map = parse(formatter.formatOrderLog("PAY", "100", status = "COMPLETED"))
    assertEquals("INFO", map["level"])
    assertEquals("ORDER_MANAGEMENT", map["businessCategory"])
    assertEquals("100", map["orderId"])
  }

  @Test
  fun `formatOrderLog cancelled should produce ERROR`() {
    val map = parse(formatter.formatOrderLog("CANCEL", "100", status = "CANCELLED", userId = "7"))
    assertEquals("ERROR", map["level"])
    assertEquals("7", map["customerId"])
  }

  @Test
  fun `formatOrderLog partial should produce WARN`() {
    val map = parse(formatter.formatOrderLog("UPDATE", "100", status = "PARTIAL"))
    assertEquals("WARN", map["level"])
  }

  @Test
  fun `formatOrderLog with details should include order details`() {
    val map = parse(formatter.formatOrderLog("CREATE", "100", status = "PENDING", details = mapOf("items" to 3)))
    assertEquals(mapOf("items" to 3), map["orderDetails"])
  }

  @Test
  fun `formatPaymentLog success should produce INFO with currency`() {
    val map = parse(formatter.formatPaymentLog("PAY", "p1", "o1", 100.0, "SUCCESS", paymentMethod = "WECHAT"))
    assertEquals("INFO", map["level"])
    assertEquals("CNY", map["currency"])
    assertEquals("WECHAT", map["paymentMethod"])
  }

  @Test
  fun `formatPaymentLog declined should produce ERROR`() {
    val map = parse(formatter.formatPaymentLog("PAY", "p1", "o1", 100.0, "DECLINED"))
    assertEquals("ERROR", map["level"])
  }

  @Test
  fun `formatPaymentLog refunded should produce INFO`() {
    val map = parse(formatter.formatPaymentLog("REFUND", "p1", "o1", 50.0, "REFUNDED"))
    assertEquals("INFO", map["level"])
  }

  @Test
  fun `formatPaymentLog with details should include payment details`() {
    val map = parse(formatter.formatPaymentLog("PAY", "p1", "o1", 100.0, "SUCCESS", details = mapOf("fee" to 1)))
    assertEquals(mapOf("fee" to 1), map["paymentDetails"])
  }

  @Test
  fun `formatDeliveryLog delivered should produce INFO`() {
    val map = parse(formatter.formatDeliveryLog("COMPLETE", "d1", "o1", "DELIVERED", workerId = "w1"))
    assertEquals("INFO", map["level"])
    assertEquals("w1", map["workerId"])
  }

  @Test
  fun `formatDeliveryLog failed should produce ERROR`() {
    val map = parse(formatter.formatDeliveryLog("COMPLETE", "d1", "o1", "FAILED"))
    assertEquals("ERROR", map["level"])
  }

  @Test
  fun `formatDeliveryLog cancelled should produce WARN`() {
    val map = parse(formatter.formatDeliveryLog("CANCEL", "d1", "o1", "CANCELLED"))
    assertEquals("WARN", map["level"])
  }

  @Test
  fun `formatUserLog success should produce INFO`() {
    val map = parse(formatter.formatUserLog("CREATE", "u1", "SUCCESS", username = "alice"))
    assertEquals("INFO", map["level"])
    assertEquals("alice", map["targetUsername"])
  }

  @Test
  fun `formatUserLog error should produce ERROR`() {
    val map = parse(formatter.formatUserLog("CREATE", "u1", "ERROR"))
    assertEquals("ERROR", map["level"])
  }

  @Test
  fun `formatProductLog created should produce INFO`() {
    val map = parse(formatter.formatProductLog("CREATE", "p1", "CREATED", productName = "水"))
    assertEquals("INFO", map["level"])
    assertEquals("水", map["productName"])
  }

  @Test
  fun `formatProductLog error should produce ERROR`() {
    val map = parse(formatter.formatProductLog("CREATE", "p1", "ERROR"))
    assertEquals("ERROR", map["level"])
  }

  @Test
  fun `formatBusinessOperation with null optional fields should omit them`() {
    val map = parse(formatter.formatBusinessOperation("CREATE", "ORDER", "SUCCESS"))
    assertTrue(!map.containsKey("businessId"))
    assertTrue(!map.containsKey("businessDescription"))
    assertTrue(!map.containsKey("businessMetadata"))
  }

  @Test
  fun `formatOrderLog with null user should omit customer id`() {
    val map = parse(formatter.formatOrderLog("PAY", "100", status = "COMPLETED"))
    assertTrue(!map.containsKey("customerId"))
  }
}
