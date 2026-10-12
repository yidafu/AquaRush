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

import { classifyPaymentReturn } from './paymentResult'

describe('classifyPaymentReturn', () => {
  test('用户取消(requestPayment:fail cancel)判定为 cancel', () => {
    expect(classifyPaymentReturn({ errMsg: 'requestPayment:fail cancel' })).toBe('cancel')
  })

  test('调用失败(含后台失败原因)判定为 fail', () => {
    expect(classifyPaymentReturn({ errMsg: 'requestPayment:fail 签名错误' })).toBe('fail')
  })

  test('非对象/缺失 errMsg 兜底为 fail(需查单确认)', () => {
    expect(classifyPaymentReturn(null)).toBe('fail')
    expect(classifyPaymentReturn(undefined)).toBe('fail')
    expect(classifyPaymentReturn(new Error('network error'))).toBe('fail')
  })
})
