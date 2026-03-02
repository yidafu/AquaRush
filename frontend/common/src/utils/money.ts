/**
 * Money utility functions for handling cent-based monetary values
 * All monetary values in the system are stored as cents (Long/number)
 * These utilities help convert between cents and yuan display format
 */

/**
 * Convert cents to yuan (divide by 100)
 * @param cents Amount in cents
 * @returns Amount in yuan
 */
export const centsToYuan = (cents: number): number => {
  return cents / 100;
};

/**
 * Convert yuan to cents (multiply by 100)
 * @param yuan Amount in yuan
 * @returns Amount in cents
 */
export const yuanToCents = (yuan: number): number => {
  return Math.round(yuan * 100);
};

/**
 * Format cents as Chinese Yuan display string
 * @param cents Amount in cents
 * @param options Formatting options
 * @returns Formatted currency string
 */
export const formatCentsToCurrency = (
  cents: number,
  options: {
    showSymbol?: boolean;
    decimalPlaces?: number;
    symbol?: string;
  } = {}
): string => {
  const { showSymbol = true, decimalPlaces = 2, symbol = '¥' } = options;
  const yuan = centsToYuan(cents);

  if (showSymbol) {
    return `${symbol}${yuan.toFixed(decimalPlaces)}`;
  }

  return yuan.toFixed(decimalPlaces);
};

/**
 * Format a number as Chinese Yuan display string
 * @param amount Amount in yuan
 * @param options Formatting options
 * @returns Formatted currency string
 */
export const formatCurrency = (
  amount: number,
  options: {
    showSymbol?: boolean;
    decimalPlaces?: number;
    symbol?: string;
  } = {}
): string => {
  const { showSymbol = true, decimalPlaces = 2, symbol = '¥' } = options;

  if (showSymbol) {
    return `${symbol}${amount.toFixed(decimalPlaces)}`;
  }

  return amount.toFixed(decimalPlaces);
};

/**
 * Parse currency string to cents
 * @param currencyString Currency string (e.g., "¥12.34" or "12.34")
 * @returns Amount in cents
 */
export const parseCurrencyToCents = (currencyString: string): number => {
  // Remove currency symbol and whitespace
  const cleanString = currencyString.replace(/[¥￥$,\s]/g, '');

  // Parse to number and convert to cents
  const yuan = parseFloat(cleanString);

  if (isNaN(yuan)) {
    throw new Error(`Invalid currency string: ${currencyString}`);
  }

  return yuanToCents(yuan);
};

/**
 * Calculate total amount from quantity and unit price in cents
 * @param quantity Quantity
 * @param unitPriceCents Unit price in cents
 * @returns Total amount in cents
 */
export const calculateTotalCents = (
  quantity: number,
  unitPriceCents: number
): number => {
  return Math.round(quantity * unitPriceCents);
};

/**
 * Calculate total amount from quantity and unit price in yuan
 * @param quantity Quantity
 * @param unitPriceYuan Unit price in yuan
 * @returns Total amount in cents
 */
export const calculateTotalFromYuan = (
  quantity: number,
  unitPriceYuan: number
): number => {
  const totalYuan = quantity * unitPriceYuan;
  return yuanToCents(totalYuan);
};

/**
 * Get a safe display value for potentially null/undefined monetary amounts
 * @param cents Amount in cents (can be null/undefined)
 * @param defaultValue Default value to return if amount is null/undefined
 * @returns Amount in cents or default value
 */
export const safeCents = (
  cents: number | null | undefined,
  defaultValue: number = 0
): number => {
  return cents ?? defaultValue;
};

/**
 * Format cents for display in a table or list with proper null handling
 * @param cents Amount in cents (can be null/undefined)
 * @param options Formatting options
 * @returns Formatted currency string or placeholder
 */
export const displayCents = (
  cents: number | null | undefined,
  options: {
    showSymbol?: boolean;
    decimalPlaces?: number;
    symbol?: string;
    placeholder?: string;
  } = {}
): string => {
  const { placeholder = '--' } = options;

  if (cents === null || cents === undefined) {
    return placeholder;
  }

  return formatCentsToCurrency(cents, options);
};

/**
 * Validate if a value represents a valid monetary amount in cents
 * @param cents Amount in cents
 * @returns True if valid
 */
export const isValidCentsAmount = (cents: number): boolean => {
  return Number.isInteger(cents) && cents >= 0;
};

/**
 * Round a yuan amount to the nearest cent
 * @param yuan Amount in yuan
 * @returns Rounded amount in yuan
 */
export const roundYuanToCent = (yuan: number): number => {
  return Math.round(yuan * 100) / 100;
};