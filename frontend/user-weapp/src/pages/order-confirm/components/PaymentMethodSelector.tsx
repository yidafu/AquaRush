import React from 'react'
import { View, Text, Radio, RadioGroup } from '@tarojs/components'
import './PaymentMethodSelector.scss'

interface PaymentMethod {
  id: string
  name: string
  icon: string
  description: string
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
      icon: '/assets/wechat-pay.png',
      description: '推荐使用微信支付'
    },
    {
      id: 'balance',
      name: '余额支付',
      icon: '/assets/balance-pay.png',
      description: '使用账户余额支付'
    }
  ]

  const handleMethodChange = (e: any) => {
    onMethodChange(e.detail.value)
  }

  return (
    <View className='section payment-section'>
      <View className='section-header'>
        <Text className='section-title'>支付方式</Text>
      </View>
      <View className='payment-method-wrapper'>
        <RadioGroup onChange={handleMethodChange}>
          {paymentMethods.map(method => (
            <View key={method.id} className='payment-method'>
              <Radio
                value={method.id}
                checked={selectedMethod === method.id}
                color='var(--theme-primary)'
              />
              <View className='payment-info'>
                <Text className='payment-name'>{method.name}</Text>
                <Text className='payment-desc'>{method.description}</Text>
              </View>
            </View>
          ))}
        </RadioGroup>
      </View>
    </View>
  )
}

export default PaymentMethodSelector