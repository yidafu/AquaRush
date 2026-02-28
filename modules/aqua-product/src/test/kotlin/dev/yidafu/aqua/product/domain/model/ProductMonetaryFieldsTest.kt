/*
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

package dev.yidafu.aqua.product.domain.model

import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.common.utils.MoneyUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("Product Model Monetary Fields Tests")
class ProductMonetaryFieldsTest {

  // ========== ProductModel Tests ==========

  @Test
  @DisplayName("ProductModel should handle price fields correctly")
  fun `ProductModel handles price fields correctly`() {
    val testCases = listOf(
      Pair(0L, BigDecimal("0.00")),
      Pair(100L, BigDecimal("1.00")),
      Pair(150L, BigDecimal("1.50")),
      Pair(999L, BigDecimal("9.99")),
      Pair(123456L, BigDecimal("1234.56"))
    )

    for ((priceInCents, expectedYuan) in testCases) {
      val product = ProductModel(
        id = 1L,
        name = "Test Product",
        price = priceInCents,
        coverImageUrl = "https://example.com/image.jpg",
        status = ProductStatus.ONLINE
      )

      // Test direct cents field
      assertEquals(priceInCents, product.price, "Price in cents should match")

      // Test compatibility property
      assertEquals(expectedYuan, product.priceYuan, "Price in yuan should match")

      // Test round-trip conversion
      val convertedBack = MoneyUtils.toCents(expectedYuan)
      assertEquals(priceInCents, convertedBack, "Round-trip conversion should match")
    }
  }

  @Test
  @DisplayName("ProductModel should handle large price values correctly")
  fun `ProductModel handles large price values correctly`() {
    val largeCents = 999999999L // ¥9,999,999.99
    val expectedYuan = BigDecimal("9999999.99")

    val product = ProductModel(
      id = 1L,
      name = "Expensive Product",
      price = largeCents,
      coverImageUrl = "https://example.com/luxury.jpg",
      status = ProductStatus.ONLINE
    )

    assertEquals(largeCents, product.price)
    assertEquals(expectedYuan, product.priceYuan)
    assertEquals(expectedYuan, MoneyUtils.fromCents(largeCents))
  }

  @Test
  @DisplayName("ProductModel should handle pricing calculations correctly")
  fun `ProductModel handles pricing calculations correctly`() {
    val product = ProductModel(
      id = 1L,
      name = "Test Product",
      price = 2500L, // ¥25.00
      coverImageUrl = "https://example.com/image.jpg",
      status = ProductStatus.ONLINE
    )

    // Test bulk pricing calculations
    val quantities = listOf(1, 2, 5, 10)
    val expectedPrices = listOf(
      2500L,    // 1 × ¥25.00
      5000L,    // 2 × ¥25.00
      12500L,   // 5 × ¥25.00
      25000L    // 10 × ¥25.00
    )

    quantities.zip(expectedPrices).forEach { (quantity, expectedPrice) ->
      val calculatedPrice = product.price * quantity
      assertEquals(expectedPrice, calculatedPrice, "Bulk price calculation failed for quantity: $quantity")
      assertEquals(
        BigDecimal("25.00") * BigDecimal(quantity),
        MoneyUtils.fromCents(calculatedPrice),
        "Yuan calculation failed for quantity: $quantity"
      )
    }

    // Test discount calculations using MoneyUtils
    val discount10Percent = MoneyUtils.calculatePercentage(product.price, BigDecimal("10.0"))
    assertEquals(250L, discount10Percent) // ¥2.50
    assertEquals(BigDecimal("2.50"), MoneyUtils.fromCents(discount10Percent))

    val discountedPrice = MoneyUtils.subtractCents(product.price, discount10Percent)
    assertEquals(2250L, discountedPrice) // ¥22.50
    assertEquals(BigDecimal("22.50"), MoneyUtils.fromCents(discountedPrice))
  }

  @Test
  @DisplayName("ProductModel should work correctly with price formatting")
  fun `ProductModel works with price formatting correctly`() {
    val product = ProductModel(
      id = 1L,
      name = "Premium Product",
      price = 12345L, // ¥123.45
      coverImageUrl = "https://example.com/premium.jpg",
      status = ProductStatus.ONLINE
    )

    // Test formatting using MoneyUtils
    val formattedCents = MoneyUtils.formatCents(product.price)
    val formattedYuan = MoneyUtils.formatYuan(product.priceYuan)

    assertEquals("¥123.45", formattedCents)
    assertEquals("¥123.45", formattedYuan)
    assertEquals(formattedCents, formattedYuan)

    // Test boundary values
    val boundaryCases = listOf(
      ProductModel(2L, "Free Product", 0L, "https://example.com/free.jpg", ProductStatus.ONLINE),
      ProductModel(3L, "Cheap Product", 1L, "https://example.com/cheap.jpg", ProductStatus.ONLINE),
      ProductModel(4L, "Round Product", 100L, "https://example.com/round.jpg", ProductStatus.ONLINE)
    )

    boundaryCases.forEach { product ->
      val formattedPrice = MoneyUtils.formatCents(product.price)
      assertTrue(formattedPrice.startsWith("¥"))
      assertDoesNotThrow { MoneyUtils.fromCents(product.price) }
    }
  }

  @Test
  @DisplayName("ProductModel should maintain price consistency across operations")
  fun `ProductModel maintains price consistency across operations`() {
    val testPrices = listOf(100L, 150L, 999L, 12345L, 999999L)

    for (price in testPrices) {
      val product = ProductModel(
        id = 1L,
        name = "Test Product",
        price = price,
        coverImageUrl = "https://example.com/test.jpg",
        status = ProductStatus.ONLINE
      )

      // Verify internal consistency
      val yuanFromCents = MoneyUtils.fromCents(price)
      assertEquals(yuanFromCents, product.priceYuan)

      // Verify MoneyUtils compatibility
      assertTrue(MoneyUtils.validateCentsConversion(price, product.priceYuan))

      // Verify formatting consistency
      val formattedCents = MoneyUtils.formatCents(price)
      val formattedYuan = MoneyUtils.formatYuan(product.priceYuan)
      assertEquals(formattedCents, formattedYuan)
    }
  }

  @Test
  @DisplayName("ProductModel should work correctly in price-based filtering and sorting")
  fun `ProductModel works correctly in price-based operations`() {
    val products = listOf(
      ProductModel(1L, "Product A", 10000L, "https://example.com/a.jpg", ProductStatus.ONLINE),  // ¥100.00
      ProductModel(2L, "Product B", 15000L, "https://example.com/b.jpg", ProductStatus.ONLINE),  // ¥150.00
      ProductModel(3L, "Product C", 5000L, "https://example.com/c.jpg", ProductStatus.ONLINE),  // ¥50.00
      ProductModel(4L, "Product D", 20000L, "https://example.com/d.jpg", ProductStatus.OFFLINE), // ¥200.00
      ProductModel(5L, "Product E", 7500L, "https://example.com/e.jpg", ProductStatus.ONLINE)   // ¥75.00
    )

    // Test price filtering
    val expensiveProducts = products.filter { it.price > 10000L }
    assertEquals(2, expensiveProducts.size)
    assertTrue(expensiveProducts.all { it.price > 10000L })

    val cheapProducts = products.filter { it.price < 10000L }
    assertEquals(2, cheapProducts.size)
    assertTrue(cheapProducts.all { it.price < 10000L })

    // Test price sorting
    val sortedByPrice = products.sortedBy { it.price }
    assertEquals(5000L, sortedByPrice.first().price)
    assertEquals(20000L, sortedByPrice.last().price)

    // Test status and price combined filtering
    val onlineAndAffordable = products.filter {
      it.status == ProductStatus.ONLINE && it.price <= 15000L
    }
    assertEquals(4, onlineAndAffordable.size)

    // Test price aggregation for online products
    val onlineProducts = products.filter { it.status == ProductStatus.ONLINE }
    val totalInventoryValue = onlineProducts.sumOf { it.price }
    assertEquals(37500L, totalInventoryValue) // ¥375.00

    val averagePrice = totalInventoryValue / onlineProducts.size
    assertEquals(9375L, averagePrice) // ¥93.75
  }

  @Test
  @DisplayName("ProductModel should handle price arithmetic operations correctly")
  fun `ProductModel handles price arithmetic correctly`() {
    val product1 = ProductModel(
      id = 1L,
      name = "Product 1",
      price = 12000L, // ¥120.00
      coverImageUrl = "https://example.com/1.jpg",
      status = ProductStatus.ONLINE
    )

    val product2 = ProductModel(
      id = 2L,
      name = "Product 2",
      price = 8000L, // ¥80.00
      coverImageUrl = "https://example.com/2.jpg",
      status = ProductStatus.ONLINE
    )

    // Test price difference
    val priceDifference = MoneyUtils.subtractCents(product1.price, product2.price)
    assertEquals(4000L, priceDifference) // ¥40.00
    assertEquals(BigDecimal("40.00"), MoneyUtils.fromCents(priceDifference))

    // Test price sum
    val priceSum = MoneyUtils.addCents(product1.price, product2.price)
    assertEquals(20000L, priceSum) // ¥200.00
    assertEquals(BigDecimal("200.00"), MoneyUtils.fromCents(priceSum))

    // Test price increase percentage
    val increasePercentage = MoneyUtils.calculatePercentage(product2.price, BigDecimal("25.0"))
    assertEquals(2000L, increasePercentage) // ¥20.00 (25% of ¥80.00)
    assertEquals(BigDecimal("20.00"), MoneyUtils.fromCents(increasePercentage))

    val increasedPrice = MoneyUtils.addCents(product2.price, increasePercentage)
    assertEquals(10000L, increasedPrice) // ¥100.00
    assertEquals(BigDecimal("100.00"), MoneyUtils.fromCents(increasedPrice))
  }

  @Test
  @DisplayName("ProductModel should validate price boundary conditions")
  fun `ProductModel validates price boundary conditions`() {
    // Test with valid price amounts
    val validPrices = listOf(0L, 1L, 100L, 999L, 12345L, 999999L)
    validPrices.forEach { price ->
      assertDoesNotThrow {
        ProductModel(
          id = 1L,
          name = "Valid Product",
          price = price,
          coverImageUrl = "https://example.com/valid.jpg",
          status = ProductStatus.ONLINE
        )
      }
    }

    // Test special price values
    val freeProduct = ProductModel(
      id = 2L,
      name = "Free Product",
      price = 0L,
      coverImageUrl = "https://example.com/free.jpg",
      status = ProductStatus.ONLINE
    )
    assertEquals(BigDecimal("0.00"), freeProduct.priceYuan)
    assertEquals("¥0.00", MoneyUtils.formatCents(freeProduct.price))

    val minimumPriceProduct = ProductModel(
      id = 3L,
      name = "Minimum Price Product",
      price = 1L,
      coverImageUrl = "https://example.com/minimum.jpg",
      status = ProductStatus.ONLINE
    )
    assertEquals(BigDecimal("0.01"), minimumPriceProduct.priceYuan)
    assertEquals("¥0.01", MoneyUtils.formatCents(minimumPriceProduct.price))

    // Note: ProductModel itself doesn't validate negative prices since it's a data class,
    // but service layer should. This test documents the expected behavior.
    val negativePriceProduct = ProductModel(
      id = 4L,
      name = "Invalid Product",
      price = -100L,
      coverImageUrl = "https://example.com/invalid.jpg",
      status = ProductStatus.ONLINE
    )

    // The model allows negative prices, but MoneyUtils should handle validation
    assertThrows<IllegalArgumentException> {
      MoneyUtils.fromCents(negativePriceProduct.price)
    }
  }

  @Test
  @DisplayName("ProductModel should handle price calculations with inventory")
  fun `ProductModel handles price calculations with inventory`() {
    val product = ProductModel(
      id = 1L,
      name = "Inventory Product",
      price = 2500L, // ¥25.00
      coverImageUrl = "https://example.com/inventory.jpg",
      stock = 100,
      status = ProductStatus.ONLINE
    )

    // Test total inventory value
    val totalInventoryValue = product.price * product.stock
    assertEquals(250000L, totalInventoryValue) // ¥2,500.00
    assertEquals(BigDecimal("2500.00"), MoneyUtils.fromCents(totalInventoryValue))

    // Test inventory value after price changes
    val priceIncrease = MoneyUtils.calculatePercentage(product.price, BigDecimal("20.0"))
    val newPrice = MoneyUtils.addCents(product.price, priceIncrease)
    assertEquals(3000L, newPrice) // ¥30.00

    val newInventoryValue = newPrice * product.stock
    assertEquals(300000L, newInventoryValue) // ¥3,000.00
    assertEquals(BigDecimal("3000.00"), MoneyUtils.fromCents(newInventoryValue))

    // Test inventory value per remaining stock after some sales
    val remainingStock = 75
    val remainingInventoryValue = newPrice * remainingStock
    assertEquals(225000L, remainingInventoryValue) // ¥2,250.00
    assertEquals(BigDecimal("2250.00"), MoneyUtils.fromCents(remainingInventoryValue))

    // Test sold inventory value
    val soldStock = product.stock - remainingStock
    val soldInventoryValue = newPrice * soldStock
    assertEquals(75000L, soldInventoryValue) // ¥750.00
    assertEquals(BigDecimal("750.00"), MoneyUtils.fromCents(soldInventoryValue))
  }
}
