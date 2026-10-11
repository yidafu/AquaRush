import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'

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
 *
 * 样式:StyleX(原 PaymentMethodSelector.scss 迁移;750 基准 rpx,weui-* 类保留;
 * 原后代选择器拍平到子元素)
 */
const styles = stylex.create({
  paymentSection: {
    marginTop: '8rpx',
  },
  paymentNameRow: {
    display: 'flex',
    alignItems: 'center',
    columnGap: '12rpx',
  },
  paymentDesc: {
    display: 'block',
    marginTop: '8rpx',
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-2)',
  },
  paymentMethodDisabled: {
    opacity: 0.4,
  },
})
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
    <View {...stylex.props(styles.paymentSection)}>
      <View className='weui-cells__title'>支付方式</View>
      <View className='weui-cells'>
        {paymentMethods.map(method => {
          const disabled = !!method.disabled
          const selected = selectedMethod === method.id
          return (
            <View
              key={method.id}
              {...props('weui-cell', disabled && styles.paymentMethodDisabled)}
              onClick={() => {
                if (!disabled) onMethodChange(method.id)
              }}
            >
              <View className='weui-cell__bd'>
                <View {...stylex.props(styles.paymentNameRow)}>
                  <Text>{method.name}</Text>
                  {!disabled && <Text className='weui-tag weui-tag--brand'>推荐</Text>}
                  {disabled && <Text className='weui-tag'>即将上线</Text>}
                </View>
                <Text {...stylex.props(styles.paymentDesc)}>{method.description}</Text>
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
