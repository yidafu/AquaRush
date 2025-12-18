/**
 * GraphQL Money scalar utility functions for the AquaRush WeChat Mini Program
 *
 * This utility provides helper functions for handling GraphQL Money scalar values,
 * converting between different formats (cents, yuan, display format) for proper
 * financial calculations and display in the frontend.
 *
 * @author AquaRush Team
 */

import { Scalars } from '@aquarush/common';

/**
 * GraphQL Money type from the common package
 */
export type Money = Scalars['Money']['output'];

/**
 * Converts GraphQL Money string to number (cents)
 *
 * GraphQL Money is stored as a string representing cents to avoid floating-point precision issues.
 * This function converts it to a number for calculations.
 *
 * @param moneyString - The GraphQL Money string (e.g., "299" for ¥2.99)
 * @returns The amount in cents as a number
 *
 * @example
 * ```typescript
 * const cents = parseMoneyString("299"); // Returns: 299
 * const displayCents = parseMoneyString("1500"); // Returns: 1500
 * ```
 */
export function parseMoneyString(moneyString: Money): number {
  if (moneyString === null || moneyString === undefined) {
    return 0;
  }

  // Convert string to number, defaulting to 0 if invalid
  const parsed = parseInt(moneyString, 10);
  return isNaN(parsed) ? 0 : parsed;
}

/**
 * Converts GraphQL Money string to yuan for display
 *
 * This function converts the stored cents value to yuan format,
 * which is the standard currency display format for Chinese Yuan.
 *
 * @param moneyString - The GraphQL Money string (e.g., "299" for ¥2.99)
 * @param includeSymbol - Whether to include the ¥ symbol (default: true)
 * @returns The formatted yuan amount as string
 *
 * @example
 * ```typescript
 * const yuan = moneyToYuan("299"); // Returns: "¥2.99"
 * const yuanNoSymbol = moneyToYuan("1500", false); // Returns: "15.00"
 * ```
 */
export function moneyToYuan(moneyString: Money, includeSymbol: boolean = true): string {
  const cents = parseMoneyString(moneyString);
  const yuan = (cents / 100).toFixed(2);

  return includeSymbol ? `¥${yuan}` : yuan;
}

/**
 * Transforms a Product object with Money fields to display format
 *
 * This function processes a Product object and converts all Money fields
 * to display-friendly yuan format, keeping the original values for API calls.
 *
 * @param product - The product object with Money fields
 * @returns A new product object with additional display fields
 *
 * @example
 * ```typescript
 * const product = {
 *   id: "123",
 *   name: "Bottled Water",
 *   price: "299",
 *   originalPrice: "399"
 * };
 *
 * const transformed = transformProductMoney(product);
 * // Returns:
 * // {
 * //   id: "123",
 * //   name: "Bottled Water",
 * //   price: "299",
 * //   originalPrice: "399",
 * //   priceDisplay: "¥2.99",
 * //   originalPriceDisplay: "¥3.99"
 * // }
 * ```
 */
export function transformProductMoney<T extends Record<string, any>>(
  product: T
): T & {
  priceDisplay: string;
  originalPriceDisplay?: string;
  totalPriceDisplay?: string;
} {
  const transformed = { ...product } as any;

  // Handle standard price field
  if ('price' in product && product.price) {
    transformed.priceDisplay = moneyToYuan(product.price);
  }

  // Handle originalPrice field (for discounted products)
  if ('originalPrice' in product && product.originalPrice) {
    transformed.originalPriceDisplay = moneyToYuan(product.originalPrice);
  }

  // Handle totalPrice field (for order items with quantity)
  if ('totalPrice' in product && product.totalPrice) {
    transformed.totalPriceDisplay = moneyToYuan(product.totalPrice);
  }

  return transformed;
}

/**
 * Formats Money string for input fields
 *
 * This function converts cents to a decimal format suitable for input fields,
 * typically used in admin interfaces or product management.
 *
 * @param moneyString - The GraphQL Money string
 * @returns The decimal value as string (e.g., "2.99" for "299")
 *
 * @example
 * ```typescript
 * const inputValue = formatMoneyForInput("299"); // Returns: "2.99"
 * ```
 */
export function formatMoneyForInput(moneyString: Money): string {
  const cents = parseMoneyString(moneyString);
  return (cents / 100).toFixed(2);
}

/**
 * Converts decimal input to GraphQL Money format
 *
 * This function is the inverse of formatMoneyForInput, converting user input
 * back to the GraphQL Money format (cents as string).
 *
 * @param decimalValue - The decimal value (e.g., "2.99")
 * @returns The GraphQL Money string (e.g., "299")
 *
 * @example
 * ```typescript
 * const moneyValue = convertDecimalToMoney("2.99"); // Returns: "299"
 * ```
 */
export function convertDecimalToMoney(decimalValue: string | number): Money {
  const decimal = typeof decimalValue === 'string' ? parseFloat(decimalValue) : decimalValue;

  if (isNaN(decimal)) {
    return "0";
  }

  // Convert to cents and round to nearest integer
  const cents = Math.round(decimal * 100);
  return cents.toString();
}

/**
 * Validates if a value is a valid GraphQL Money string
 *
 * @param value - The value to validate
 * @returns True if valid Money string, false otherwise
 */
export function isValidMoneyString(value: any): value is Money {
  if (typeof value !== 'string') {
    return false;
  }

  // Check if it's a non-negative integer
  return /^\d+$/.test(value) && parseInt(value, 10) >= 0;
}

/**
 * Calculates total price from price and quantity
 *
 * @param price - The unit price as GraphQL Money string
 * @param quantity - The quantity
 * @returns The total price as GraphQL Money string
 */
export function calculateTotalPrice(price: Money, quantity: number): Money {
  const priceInCents = parseMoneyString(price);
  const totalInCents = priceInCents * quantity;
  return totalInCents.toString();
}

/**
 * Compares two Money values
 *
 * @param money1 - First Money value
 * @param money2 - Second Money value
 * @returns -1 if money1 < money2, 0 if equal, 1 if money1 > money2
 */
export function compareMoney(money1: Money, money2: Money): number {
  const cents1 = parseMoneyString(money1);
  const cents2 = parseMoneyString(money2);

  if (cents1 < cents2) return -1;
  if (cents1 > cents2) return 1;
  return 0;
}