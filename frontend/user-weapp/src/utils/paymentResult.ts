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

/**
 * Taro.requestPayment 返回结果判定(F-ORD-005,官方《支付回调和查单实现指引》§6.1
 * 小程序回调类型):
 * - 'cancel':requestPayment:fail cancel —— 用户在收银台取消支付,订单保持待支付;
 * - 'fail':requestPayment:fail (detail message) —— 调用失败,仍可能已扣款,需查单确认;
 * 成功路径不抛异常(requestPayment:ok),不经过本函数。
 */
export type PaymentReturnKind = 'cancel' | 'fail'

export function classifyPaymentReturn(error: unknown): PaymentReturnKind {
  const errMsg = String((error as { errMsg?: string } | null)?.errMsg ?? '')
  return errMsg.includes('cancel') ? 'cancel' : 'fail'
}
