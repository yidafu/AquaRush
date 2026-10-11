import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'

// 勾选状态沿用父级 string[] 契约(原 AtCheckbox.selectedList):'agree' 在数组中即勾选
interface TermsAgreementProps {
  agreeTerms: string[]
  onTermsChange: (value: string[]) => void
}

/**
 * 服务条款 —— WeUI .weui-cells + .weui-checkbox
 * (设计稿 02-order/order-confirm.html「服务条款」:勾选框 + 说明文字)
 *
 * 样式:StyleX(原 TermsAgreement.scss 迁移;750 基准 rpx,weui-* 类保留;
 * 原后代选择器拍平到子元素)
 */
const styles = stylex.create({
  termsSection: {
    marginTop: '24rpx',
    paddingBottom: '24rpx',
  },
  termsText: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
    lineHeight: 1.5,
  },
})
const TermsAgreement: React.FC<TermsAgreementProps> = ({
  agreeTerms,
  onTermsChange
}) => {
  const checked = agreeTerms.includes('agree')

  return (
    <View {...stylex.props(styles.termsSection)}>
      <View className='weui-cells'>
        <View
          className='weui-cell'
          onClick={() => onTermsChange(checked ? [] : ['agree'])}
        >
          <View className='weui-cell__hd'>
            <View className={`weui-checkbox${checked ? ' on' : ''}`} />
          </View>
          <View className='weui-cell__bd'>
            <Text {...stylex.props(styles.termsText)}>我已阅读并同意《用户服务协议》和《隐私政策》</Text>
          </View>
        </View>
      </View>
    </View>
  )
}

export default TermsAgreement
