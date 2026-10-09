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

package dev.yidafu.aqua.product.domain.model

import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.util.MoneyUtils
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class ProductMonetaryFieldsTest :
  FunSpec({
    // ========== ProductModel Tests ==========

    test("ProductModel handles price fields correctly") {
      val testCases =
        listOf(
          Pair(0L, BigDecimal("0.00")),
          Pair(100L, BigDecimal("1.00")),
          Pair(150L, BigDecimal("1.50")),
          Pair(999L, BigDecimal("9.99")),
          Pair(123456L, BigDecimal("1234.56")),
        )

      for ((priceInCents, expectedYuan) in testCases) {
        val product =
          ProductModel(
            id = 1L,
            name = "Test Product",
            price = priceInCents,
            coverImageUrl = "https://example.com/image.jpg",
            status = ProductModelStatus.ONLINE,
          )

        // Test direct cents field
        withClue("Price in cents should match") { product.price shouldBe priceInCents }

        // Test compatibility property
        withClue("Price in yuan should match") { product.priceYuan shouldBe expectedYuan }

        // Test round-trip conversion
        val convertedBack = MoneyUtils.toCents(expectedYuan)
        withClue("Round-trip conversion should match") { convertedBack shouldBe priceInCents }
      }
    }
    test("ProductModel handles large price values correctly") {
      val largeCents = 999999999L // ¥9,999,999.99
      val expectedYuan = BigDecimal("9999999.99")

      val product =
        ProductModel(
          id = 1L,
          name = "Expensive Product",
          price = largeCents,
          coverImageUrl = "https://example.com/luxury.jpg",
          status = ProductModelStatus.ONLINE,
        )

      product.price shouldBe largeCents
      product.priceYuan shouldBe expectedYuan
      MoneyUtils.fromCents(largeCents) shouldBe expectedYuan
    }
    test("ProductModel handles pricing calculations correctly") {
      val product =
        ProductModel(
          id = 1L,
          name = "Test Product",
          price = 2500L, // ¥25.00
          coverImageUrl = "https://example.com/image.jpg",
          status = ProductModelStatus.ONLINE,
        )

      // Test bulk pricing calculations
      val quantities = listOf(1, 2, 5, 10)
      val expectedPrices =
        listOf(
          2500L, // 1 × ¥25.00
          5000L, // 2 × ¥25.00
          12500L, // 5 × ¥25.00
          25000L, // 10 × ¥25.00
        )

      quantities.zip(expectedPrices).forEach { (quantity, expectedPrice) ->
        val calculatedPrice = product.price * quantity
        withClue("Bulk price calculation failed for quantity: $quantity") { calculatedPrice shouldBe expectedPrice }
        withClue("Yuan calculation failed for quantity: $quantity") {
          MoneyUtils.fromCents(calculatedPrice) shouldBe
            BigDecimal("25.00") * BigDecimal(quantity)
        }
      }

      // Test discount calculations using MoneyUtils
      val discount10Percent = MoneyUtils.calculatePercentage(product.price, BigDecimal("10.0"))
      discount10Percent shouldBe 250L // ¥2.50
      MoneyUtils.fromCents(discount10Percent) shouldBe BigDecimal("2.50")

      val discountedPrice = MoneyUtils.subtractCents(product.price, discount10Percent)
      discountedPrice shouldBe 2250L // ¥22.50
      MoneyUtils.fromCents(discountedPrice) shouldBe BigDecimal("22.50")
    }
    test("ProductModel works with price formatting correctly") {
      val product =
        ProductModel(
          id = 1L,
          name = "Premium Product",
          price = 12345L, // ¥123.45
          coverImageUrl = "https://example.com/premium.jpg",
          status = ProductModelStatus.ONLINE,
        )

      // Test formatting using MoneyUtils
      val formattedCents = MoneyUtils.formatCents(product.price)
      val formattedYuan = MoneyUtils.formatYuan(product.priceYuan)

      formattedCents shouldBe "¥123.45"
      formattedYuan shouldBe "¥123.45"
      formattedYuan shouldBe formattedCents

      // Test boundary values
      val boundaryCases =
        listOf(
          ProductModel(
            id = 2L,
            name = "Free Product",
            price = 0L,
            coverImageUrl = "https://example.com/free.jpg",
            status = ProductModelStatus.ONLINE,
          ),
          ProductModel(
            id = 3L,
            name = "Cheap Product",
            price = 1L,
            coverImageUrl = "https://example.com/cheap.jpg",
            status = ProductModelStatus.ONLINE,
          ),
          ProductModel(
            id = 4L,
            name = "Round Product",
            price = 100L,
            coverImageUrl = "https://example.com/round.jpg",
            status = ProductModelStatus.ONLINE,
          ),
        )

      boundaryCases.forEach { product ->
        val formattedPrice = MoneyUtils.formatCents(product.price)
        formattedPrice.startsWith("¥") shouldBe true
        shouldNotThrowAny { MoneyUtils.fromCents(product.price) }
      }
    }
    test("ProductModel maintains price consistency across operations") {
      val testPrices = listOf(100L, 150L, 999L, 12345L, 999999L)

      for (price in testPrices) {
        val product =
          ProductModel(
            id = 1L,
            name = "Test Product",
            price = price,
            coverImageUrl = "https://example.com/test.jpg",
            status = ProductModelStatus.ONLINE,
          )

        // Verify internal consistency
        val yuanFromCents = MoneyUtils.fromCents(price)
        product.priceYuan shouldBe yuanFromCents

        // Verify MoneyUtils compatibility
        MoneyUtils.validateCentsConversion(price, product.priceYuan) shouldBe true

        // Verify formatting consistency
        val formattedCents = MoneyUtils.formatCents(price)
        val formattedYuan = MoneyUtils.formatYuan(product.priceYuan)
        formattedYuan shouldBe formattedCents
      }
    }
    test("ProductModel works correctly in price-based operations") {
      val products =
        listOf(
          ProductModel(
            id = 1L,
            name = "Product A",
            price = 10000L,
            coverImageUrl = "https://example.com/a.jpg",
            status = ProductModelStatus.ONLINE,
          ), // ¥100.00
          ProductModel(
            id = 2L,
            name = "Product B",
            price = 15000L,
            coverImageUrl = "https://example.com/b.jpg",
            status = ProductModelStatus.ONLINE,
          ), // ¥150.00
          ProductModel(
            id = 3L,
            name = "Product C",
            price = 5000L,
            coverImageUrl = "https://example.com/c.jpg",
            status = ProductModelStatus.ONLINE,
          ), // ¥50.00
          ProductModel(
            id = 4L,
            name = "Product D",
            price = 20000L,
            coverImageUrl = "https://example.com/d.jpg",
            status = ProductModelStatus.OFFLINE,
          ), // ¥200.00
          ProductModel(
            id = 5L,
            name = "Product E",
            price = 7500L,
            coverImageUrl = "https://example.com/e.jpg",
            status = ProductModelStatus.ONLINE,
          ), // ¥75.00
        )

      // Test price filtering
      val expensiveProducts = products.filter { it.price > 10000L }
      expensiveProducts.size shouldBe 2
      expensiveProducts.all { it.price > 10000L } shouldBe true

      val cheapProducts = products.filter { it.price < 10000L }
      cheapProducts.size shouldBe 2
      cheapProducts.all { it.price < 10000L } shouldBe true

      // Test price sorting
      val sortedByPrice = products.sortedBy { it.price }
      sortedByPrice.first().price shouldBe 5000L
      sortedByPrice.last().price shouldBe 20000L

      // Test status and price combined filtering
      val onlineAndAffordable =
        products.filter {
          it.status == ProductModelStatus.ONLINE && it.price <= 15000L
        }
      onlineAndAffordable.size shouldBe 4

      // Test price aggregation for online products
      val onlineProducts = products.filter { it.status == ProductModelStatus.ONLINE }
      val totalInventoryValue = onlineProducts.sumOf { it.price }
      totalInventoryValue shouldBe 37500L // ¥375.00

      val averagePrice = totalInventoryValue / onlineProducts.size
      averagePrice shouldBe 9375L // ¥93.75
    }
    test("ProductModel handles price arithmetic correctly") {
      val product1 =
        ProductModel(
          id = 1L,
          name = "Product 1",
          price = 12000L, // ¥120.00
          coverImageUrl = "https://example.com/1.jpg",
          status = ProductModelStatus.ONLINE,
        )

      val product2 =
        ProductModel(
          id = 2L,
          name = "Product 2",
          price = 8000L, // ¥80.00
          coverImageUrl = "https://example.com/2.jpg",
          status = ProductModelStatus.ONLINE,
        )

      // Test price difference
      val priceDifference = MoneyUtils.subtractCents(product1.price, product2.price)
      priceDifference shouldBe 4000L // ¥40.00
      MoneyUtils.fromCents(priceDifference) shouldBe BigDecimal("40.00")

      // Test price sum
      val priceSum = MoneyUtils.addCents(product1.price, product2.price)
      priceSum shouldBe 20000L // ¥200.00
      MoneyUtils.fromCents(priceSum) shouldBe BigDecimal("200.00")

      // Test price increase percentage
      val increasePercentage = MoneyUtils.calculatePercentage(product2.price, BigDecimal("25.0"))
      increasePercentage shouldBe 2000L // ¥20.00 (25% of ¥80.00)
      MoneyUtils.fromCents(increasePercentage) shouldBe BigDecimal("20.00")

      val increasedPrice = MoneyUtils.addCents(product2.price, increasePercentage)
      increasedPrice shouldBe 10000L // ¥100.00
      MoneyUtils.fromCents(increasedPrice) shouldBe BigDecimal("100.00")
    }
    test("ProductModel validates price boundary conditions") {
      // Test with valid price amounts
      val validPrices = listOf(0L, 1L, 100L, 999L, 12345L, 999999L)
      validPrices.forEach { price ->
        shouldNotThrowAny {
          ProductModel(
            id = 1L,
            name = "Valid Product",
            price = price,
            coverImageUrl = "https://example.com/valid.jpg",
            status = ProductModelStatus.ONLINE,
          )
        }
      }

      // Test special price values
      val freeProduct =
        ProductModel(
          id = 2L,
          name = "Free Product",
          price = 0L,
          coverImageUrl = "https://example.com/free.jpg",
          status = ProductModelStatus.ONLINE,
        )
      freeProduct.priceYuan shouldBe BigDecimal("0.00")
      MoneyUtils.formatCents(freeProduct.price) shouldBe "¥0.00"

      val minimumPriceProduct =
        ProductModel(
          id = 3L,
          name = "Minimum Price Product",
          price = 1L,
          coverImageUrl = "https://example.com/minimum.jpg",
          status = ProductModelStatus.ONLINE,
        )
      minimumPriceProduct.priceYuan shouldBe BigDecimal("0.01")
      MoneyUtils.formatCents(minimumPriceProduct.price) shouldBe "¥0.01"

      // Note: ProductModel itself doesn't validate negative prices since it's a data class,
      // but service layer should. This test documents the expected behavior.
      val negativePriceProduct =
        ProductModel(
          id = 4L,
          name = "Invalid Product",
          price = -100L,
          coverImageUrl = "https://example.com/invalid.jpg",
          status = ProductModelStatus.ONLINE,
        )

      // The model allows negative prices, but MoneyUtils should handle validation
      shouldThrow<IllegalArgumentException> {
        MoneyUtils.fromCents(negativePriceProduct.price)
      }
    }
    test("ProductModel handles price calculations with inventory") {
      val product =
        ProductModel(
          id = 1L,
          name = "Inventory Product",
          price = 2500L, // ¥25.00
          coverImageUrl = "https://example.com/inventory.jpg",
          stock = 100,
          status = ProductModelStatus.ONLINE,
        )

      // Test total inventory value
      val totalInventoryValue = product.price * product.stock
      totalInventoryValue shouldBe 250000L // ¥2,500.00
      MoneyUtils.fromCents(totalInventoryValue) shouldBe BigDecimal("2500.00")

      // Test inventory value after price changes
      val priceIncrease = MoneyUtils.calculatePercentage(product.price, BigDecimal("20.0"))
      val newPrice = MoneyUtils.addCents(product.price, priceIncrease)
      newPrice shouldBe 3000L // ¥30.00

      val newInventoryValue = newPrice * product.stock
      newInventoryValue shouldBe 300000L // ¥3,000.00
      MoneyUtils.fromCents(newInventoryValue) shouldBe BigDecimal("3000.00")

      // Test inventory value per remaining stock after some sales
      val remainingStock = 75
      val remainingInventoryValue = newPrice * remainingStock
      remainingInventoryValue shouldBe 225000L // ¥2,250.00
      MoneyUtils.fromCents(remainingInventoryValue) shouldBe BigDecimal("2250.00")

      // Test sold inventory value
      val soldStock = product.stock - remainingStock
      val soldInventoryValue = newPrice * soldStock
      soldInventoryValue shouldBe 75000L // ¥750.00
      MoneyUtils.fromCents(soldInventoryValue) shouldBe BigDecimal("750.00")
    }
  })
