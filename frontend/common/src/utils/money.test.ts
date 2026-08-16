/**
 * money 工具单测
 * 金额一律以分(Long/number)存储与计算(R-MON-01),展示层才转元。
 */
import { describe, expect, it } from '@jest/globals'
import {
  calculateTotalCents,
  calculateTotalFromYuan,
  centsToYuan,
  displayCents,
  formatCentsToCurrency,
  isValidCentsAmount,
  normalizeCents,
  parseCurrencyToCents,
  roundYuanToCent,
  safeCents,
  yuanToCents,
} from './money'

describe('centsToYuan / yuanToCents', () => {
  it('分转元: 除以 100', () => {
    expect(centsToYuan(299)).toBe(2.99)
    expect(centsToYuan(0)).toBe(0)
    expect(centsToYuan(-150)).toBe(-1.5)
  })

  it('元转分: 乘以 100 并四舍五入', () => {
    expect(yuanToCents(2.99)).toBe(299)
    expect(yuanToCents(0)).toBe(0)
    expect(yuanToCents(2.995)).toBe(300)
    expect(yuanToCents(-1.5)).toBe(-150)
  })
})

describe('formatCentsToCurrency', () => {
  it('默认带 ¥ 符号与两位小数', () => {
    expect(formatCentsToCurrency(299)).toBe('¥2.99')
    expect(formatCentsToCurrency(0)).toBe('¥0.00')
  })

  it('可通过选项关闭符号、调整小数位与符号', () => {
    expect(formatCentsToCurrency(299, { showSymbol: false })).toBe('2.99')
    expect(formatCentsToCurrency(299, { decimalPlaces: 0 })).toBe('¥3')
    expect(formatCentsToCurrency(299, { symbol: '￥' })).toBe('￥2.99')
    expect(formatCentsToCurrency(123456, { showSymbol: false, decimalPlaces: 1 })).toBe('1234.6')
  })

  it('负数也能格式化', () => {
    expect(formatCentsToCurrency(-100)).toBe('¥-1.00')
  })
})

describe('normalizeCents', () => {
  it('后端 Long 字符串与数字均可归一化', () => {
    expect(normalizeCents(299)).toBe(299)
    expect(normalizeCents('299')).toBe(299)
    expect(normalizeCents('0')).toBe(0)
  })

  it('null / undefined / 空串 / 非法值返回 null', () => {
    expect(normalizeCents(null)).toBeNull()
    expect(normalizeCents(undefined)).toBeNull()
    expect(normalizeCents('')).toBeNull()
    expect(normalizeCents('abc')).toBeNull()
  })
})

describe('parseCurrencyToCents', () => {
  it('解析带符号/逗号/空白的金额字符串', () => {
    expect(parseCurrencyToCents('12.34')).toBe(1234)
    expect(parseCurrencyToCents('¥12.34')).toBe(1234)
    expect(parseCurrencyToCents('￥12.34')).toBe(1234)
    expect(parseCurrencyToCents('$1,234.50')).toBe(123450)
    expect(parseCurrencyToCents('  3.00  ')).toBe(300)
  })

  it('非法字符串抛出错误', () => {
    expect(() => parseCurrencyToCents('abc')).toThrow('Invalid currency string')
    expect(() => parseCurrencyToCents('')).toThrow('Invalid currency string')
  })
})

describe('calculateTotalCents / calculateTotalFromYuan', () => {
  it('分单价 × 数量 = 总分', () => {
    expect(calculateTotalCents(3, 200)).toBe(600)
    expect(calculateTotalCents(0, 200)).toBe(0)
  })

  it('元单价 × 数量 = 分(四舍五入)', () => {
    expect(calculateTotalFromYuan(3, 2.5)).toBe(750)
    expect(calculateTotalFromYuan(1, 0.1)).toBe(10)
  })
})

describe('safeCents / displayCents', () => {
  it('safeCents 空值回退默认值', () => {
    expect(safeCents(100)).toBe(100)
    expect(safeCents(null)).toBe(0)
    expect(safeCents(undefined, 5)).toBe(5)
  })

  it('displayCents 空值显示占位符', () => {
    expect(displayCents(null)).toBe('--')
    expect(displayCents(undefined)).toBe('--')
    expect(displayCents(null, { placeholder: '免费' })).toBe('免费')
  })

  it('displayCents 非空值格式化', () => {
    expect(displayCents(299)).toBe('¥2.99')
    expect(displayCents(299, { showSymbol: false })).toBe('2.99')
  })
})

describe('isValidCentsAmount / roundYuanToCent', () => {
  it('校验整数且非负', () => {
    expect(isValidCentsAmount(100)).toBe(true)
    expect(isValidCentsAmount(0)).toBe(true)
    expect(isValidCentsAmount(-1)).toBe(false)
    expect(isValidCentsAmount(1.5)).toBe(false)
  })

  it('元四舍五入到分', () => {
    expect(roundYuanToCent(2.995)).toBe(3)
    expect(roundYuanToCent(2.994)).toBe(2.99)
    expect(roundYuanToCent(0.1 + 0.2)).toBe(0.3)
  })
})
