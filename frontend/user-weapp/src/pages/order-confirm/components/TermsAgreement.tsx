import React from 'react'
import { View, Text } from '@tarojs/components'
import './TermsAgreement.scss'

// 勾选状态沿用父级 string[] 契约(原 AtCheckbox.selectedList):'agree' 在数组中即勾选
interface TermsAgreementProps {
  agreeTerms: string[]
  onTermsChange: (value: string[]) => void
}

/**
 * 服务条款 —— WeUI .weui-cells + .weui-checkbox
 * (设计稿 02-order/order-confirm.html「服务条款」:勾选框 + 说明文字)
 */
const TermsAgreement: React.FC<TermsAgreementProps> = ({
  agreeTerms,
  onTermsChange
}) => {
  const checked = agreeTerms.includes('agree')

  return (
    <View className='terms-section'>
      <View className='weui-cells terms-cells'>
        <View
          className='weui-cell'
          onClick={() => onTermsChange(checked ? [] : ['agree'])}
        >
          <View className='weui-cell__hd'>
            <View className={`weui-checkbox${checked ? ' on' : ''}`} />
          </View>
          <View className='weui-cell__bd'>
            <Text className='terms-text'>我已阅读并同意《用户服务协议》和《隐私政策》</Text>
          </View>
        </View>
      </View>
    </View>
  )
}

export default TermsAgreement
