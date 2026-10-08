/**
 * Money - cross-platform monetary display component
 *
 * All monetary values in the system are stored/transported as cents (分).
 * This component formats cents as yuan display string (e.g. 299 -> "¥2.99"),
 * so pages no longer need to divide by 100 or format manually.
 *
 * The component renders no host element (returns a fragment-wrapped string),
 * a valid ReactNode in both React DOM (admin) and Taro (mini programs).
 * Place it inside a <Text> (Taro) / <span> (admin) when styling is needed.
 */

import { formatCentsToCurrency, normalizeCents } from '../utils/money'

export interface MoneyProps {
  /** Amount in cents (分). Backend Long fields arrive as string on the wire - both are accepted */
  cents?: number | string | null
  /** Currency symbol, default '¥' */
  symbol?: string
  /** Decimal places, default 2 */
  decimalPlaces?: number
  /** Whether to show the symbol, default true */
  showSymbol?: boolean
  /** Placeholder when cents is null/undefined/empty, default '--' */
  placeholder?: string
}

// 注意:不要显式标注返回类型(ReactElement/ReactNode 在该项目 @types/react 组合下
// 不可赋值给 JSX 组件签名,报 TS2786);让 TS 从 JSX 推断为 Element。
export const Money = ({
  cents,
  symbol = '¥',
  decimalPlaces = 2,
  showSymbol = true,
  placeholder = '--',
}: MoneyProps) => {
  const n = normalizeCents(cents)
  if (n === null) return <>{placeholder}</>
  return <>{formatCentsToCurrency(n, { symbol, decimalPlaces, showSymbol })}</>
}

export default Money
