import React from 'react'
import { View, Text } from '@tarojs/components'
import './PaymentMethodSelector.scss'

interface PaymentMethod {
  id: string
  name: string
  description: string
  /** 未接入的支付方式置灰不可选(PRD F-ORD-001:余额支付未实现,仅展示) */
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
  const paymentMethods: PaymentMethod[] = [
    {
      id: 'wechat',
      name: '微信支付',
      description: '使用微信支付，安全快捷'
    },
    {
      id: 'balance',
      name: '余额支付',
      description: '使用账户余额支付',
      disabled: true
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
