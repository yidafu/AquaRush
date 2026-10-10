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
    <View className='section payment-section'>
      <View className='section-label-row'>
        <Text className='section-label'>支付方式</Text>
      </View>
      <View className='payment-method-wrapper'>
        {paymentMethods.map(method => {
          const disabled = !!method.disabled
          const selected = selectedMethod === method.id
          return (
            <View
              key={method.id}
              className={`payment-method${selected && !disabled ? ' payment-method-selected' : ''}${disabled ? ' payment-method-disabled' : ''}`}
              onClick={() => {
                if (!disabled) onMethodChange(method.id)
              }}
            >
              <View className='payment-info'>
                <View className='payment-name-row'>
                  <Text className='payment-name'>{method.name}</Text>
                  {!disabled && <Text className='payment-tag payment-tag-recommend'>推荐</Text>}
                  {disabled && <Text className='payment-tag payment-tag-soon'>即将上线</Text>}
                </View>
                <Text className='payment-desc'>{method.description}</Text>
              </View>
              <View className='payment-radio'>
                {selected && !disabled && <View className='payment-radio-dot' />}
              </View>
            </View>
          )
        })}
      </View>
    </View>
  )
}

export default PaymentMethodSelector
