package dev.yidafu.aqua.common.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

/**
 * Utility class for monetary value conversions and formatting.
 *
 * This class provides conversion functions between BigDecimal (yuan) and Long (cents),
 * which is the standard representation for monetary values in the AquaRush system.
 * All monetary values are stored as cents (Long type) to avoid floating-point precision issues
 * with financial calculations.
 */
object MoneyUtils {

    /**
     * Conversion factor from yuan to cents (1 yuan = 100 cents)
     */
    private val YUAN_TO_CENTS = BigDecimal(100)

    /**
     * Number of decimal places for monetary calculations
     */
    private const val MONETARY_SCALE = 2

    /**
     * Locale used for currency formatting (Chinese Yuan)
     */
    private val CHINESE_LOCALE = Locale.CHINA

    /**
     * Currency formatter for Chinese Yuan
     */
    private val CURRENCY_FORMATTER: NumberFormat = NumberFormat.getCurrencyInstance(CHINESE_LOCALE).apply {
        // Ensure minimum 2 decimal places
        minimumFractionDigits = MONETARY_SCALE
        maximumFractionDigits = MONETARY_SCALE
    }

    /**
     * Converts a BigDecimal amount in yuan to cents (Long).
     *
     * @param yuan The amount in yuan (BigDecimal)
     * @return The equivalent amount in cents (Long)
     * @throws IllegalArgumentException if the amount is negative or null
     * @throws ArithmeticException if the conversion would lose precision
     */
    fun toCents(yuan: BigDecimal): Long {
        requireNotNull(yuan) { "Yuan amount cannot be null" }
        require(yuan.scale() <= MONETARY_SCALE) {
            "Yuan amount cannot have more than $MONETARY_SCALE decimal places. Current scale: ${yuan.scale()}"
        }
        require(yuan >= BigDecimal.ZERO) { "Yuan amount cannot be negative: $yuan" }

        return try {
            yuan.multiply(YUAN_TO_CENTS)
                .setScale(0, RoundingMode.HALF_EVEN)
                .longValueExact()
        } catch (e: ArithmeticException) {
            throw ArithmeticException("Conversion from yuan to cents would lose precision for amount: $yuan")
        }
    }

    /**
     * Converts cents (Long) to a BigDecimal amount in yuan.
     *
     * @param cents The amount in cents (Long)
     * @return The equivalent amount in yuan (BigDecimal) with 2 decimal places
     * @throws IllegalArgumentException if the amount is negative or null
     */
    fun fromCents(cents: Long): BigDecimal {
        require(cents >= 0) { "Cents amount cannot be negative: $cents" }

        return BigDecimal(cents)
            .divide(YUAN_TO_CENTS, MONETARY_SCALE, RoundingMode.HALF_EVEN)
    }

    /**
     * Formats cents as a currency string in Chinese Yuan format (¥).
     *
     * @param cents The amount in cents
     * @return The formatted currency string (e.g., "¥123.45")
     * @throws IllegalArgumentException if the amount is negative or null
     */
    fun formatCents(cents: Long): String {
        require(cents >= 0) { "Cents amount cannot be negative: $cents" }

        val yuan = fromCents(cents)
        return CURRENCY_FORMATTER.format(yuan)
    }

    /**
     * Formats a BigDecimal amount in yuan as a currency string in Chinese Yuan format (¥).
     *
     * @param yuan The amount in yuan
     * @return The formatted currency string (e.g., "¥123.45")
     * @throws IllegalArgumentException if the amount is negative or null
     */
    fun formatYuan(yuan: BigDecimal): String {
        requireNotNull(yuan) { "Yuan amount cannot be null" }
        require(yuan >= BigDecimal.ZERO) { "Yuan amount cannot be negative: $yuan" }

        return CURRENCY_FORMATTER.format(yuan)
    }

    /**
     * Validates that the given BigDecimal amount can be accurately converted to cents.
     *
     * @param yuan The amount in yuan to validate
     * @return true if the amount can be converted without precision loss
     */
    fun canConvertToCents(yuan: BigDecimal): Boolean {
        if (yuan == null || yuan < BigDecimal.ZERO) {
            return false
        }

        if (yuan.scale() > MONETARY_SCALE) {
            return false
        }

        return try {
            val cents = yuan.multiply(YUAN_TO_CENTS)
                .setScale(0, RoundingMode.HALF_EVEN)
            // Verify round-trip conversion
            fromCents(cents.longValueExact()).compareTo(yuan) == 0
        } catch (e: ArithmeticException) {
            false
        }
    }

    /**
     * Validates that the given cents value can be accurately converted back to yuan and match the original.
     *
     * @param cents The amount in cents to validate
     * @param expectedYuan The expected yuan amount to match against
     * @return true if the conversion is accurate and matches the expected amount
     */
    fun validateCentsConversion(cents: Long, expectedYuan: BigDecimal): Boolean {
        if (cents < 0 || expectedYuan == null || expectedYuan < BigDecimal.ZERO) {
            return false
        }

        val actualYuan = fromCents(cents)
        return actualYuan.compareTo(expectedYuan) == 0
    }

    /**
     * Rounds a BigDecimal amount to 2 decimal places using standard monetary rounding rules.
     *
     * @param amount The amount to round
     * @return The rounded amount with exactly 2 decimal places
     */
    fun roundToMonetaryScale(amount: BigDecimal): BigDecimal {
        return amount.setScale(MONETARY_SCALE, RoundingMode.HALF_EVEN)
    }

    /**
     * Adds two amounts in cents and returns the result as cents.
     *
     * @param cents1 The first amount in cents
     * @param cents2 The second amount in cents
     * @return The sum as cents
     * @throws ArithmeticException if the addition would overflow
     */
    fun addCents(cents1: Long, cents2: Long): Long {
        return try {
            Math.addExact(cents1, cents2)
        } catch (e: ArithmeticException) {
            throw ArithmeticException("Overflow when adding cents: $cents1 + $cents2")
        }
    }

    /**
     * Subtracts cents2 from cents1 and returns the result as cents.
     *
     * @param cents1 The minuend in cents
     * @param cents2 The subtrahend in cents
     * @return The difference as cents
     * @throws IllegalArgumentException if the result would be negative
     * @throws ArithmeticException if the subtraction would overflow
     */
    fun subtractCents(cents1: Long, cents2: Long): Long {
        require(cents1 >= cents2) { "Result would be negative: $cents1 - $cents2 = ${cents1 - cents2}" }

        return try {
            Math.subtractExact(cents1, cents2)
        } catch (e: ArithmeticException) {
            throw ArithmeticException("Overflow when subtracting cents: $cents1 - $cents2")
        }
    }

    /**
     * Multiplies an amount in cents by a multiplier and returns the result as cents.
     * Uses rounding to nearest cent for fractional results.
     *
     * @param cents The amount in cents
     * @param multiplier The multiplier
     * @return The product as cents
     * @throws IllegalArgumentException if the multiplier is negative or null
     */
    fun multiplyCents(cents: Long, multiplier: BigDecimal): Long {
        requireNotNull(multiplier) { "Multiplier cannot be null" }
        require(multiplier >= BigDecimal.ZERO) { "Multiplier cannot be negative: $multiplier" }

        val result = BigDecimal(cents).multiply(multiplier)
        return result.setScale(0, RoundingMode.HALF_EVEN).longValueExact()
    }

    /**
     * Calculates percentage of an amount in cents.
     *
     * @param cents The base amount in cents
     * @param percentage The percentage (e.g., 15.0 for 15%)
     * @return The percentage amount as cents
     */
    fun calculatePercentage(cents: Long, percentage: BigDecimal): Long {
        requireNotNull(percentage) { "Percentage cannot be null" }
        require(percentage >= BigDecimal.ZERO) { "Percentage cannot be negative: $percentage" }

        val multiplier = percentage.divide(BigDecimal(100))
        return multiplyCents(cents, multiplier)
    }
}