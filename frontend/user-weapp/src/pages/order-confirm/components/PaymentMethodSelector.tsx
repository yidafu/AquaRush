import React from 'react'
import { View, Text } from '@tarojs/components'
import './PaymentMethodSelector.scss'

interface PaymentMethod {
  id: string
  name: string
  description: string
  /** 预留:未接入的支付方式置灰不可选(如企业月结 F-ORD-027) */
  disabled?: boolean
}

interface PaymentMethodSelectorProps {
  selectedMethod: string
  onMethodChange: (method: string) => void
}

/**
 * 支付方式选择 —— WeUI .weui-cells 单选行(.weui-radio + 推荐 .weui-tag--brand,
 * 设计稿 02-order/order-confirm.html「支付方式」)
 */
const PaymentMethodSelector: React.FC<PaymentMethodSelectorProps> = ({
  selectedMethod,
  onMethodChange
}) => {
  // 仅微信支付:用户储值不做(2026-10-09 合规决策,原「余额支付·即将上线」占位移除)
  const paymentMethods: PaymentMethod[] = [
    {
      id: 'wechat',
      name: '微信支付',
      description: '使用微信支付，安全快捷'
    }
  ]

  return (
    <View className='payment-section'>
      <View className='weui-cells__title'>支付方式</View>
      <View className='weui-cells'>
        {paymentMethods.map(method => {
          const disabled = !!method.disabled
          const selected = selectedMethod === method.id
          return (
            <View
              key={method.id}
              className={`weui-cell${disabled ? ' payment-method-disabled' : ''}`}
              onClick={() => {
                if (!disabled) onMethodChange(method.id)
              }}
            >
              <View className='weui-cell__bd'>
                <View className='payment-name-row'>
                  <Text>{method.name}</Text>
                  {!disabled && <Text className='weui-tag weui-tag--brand'>推荐</Text>}
                  {disabled && <Text className='weui-tag'>即将上线</Text>}
                </View>
                <Text className='payment-desc'>{method.description}</Text>
              </View>
              <View className='weui-cell__ft'>
                <View className={`weui-radio${selected && !disabled ? ' on' : ''}`} />
              </View>
            </View>
          )
        })}
      </View>
    </View>
  )
}

export default PaymentMethodSelector
