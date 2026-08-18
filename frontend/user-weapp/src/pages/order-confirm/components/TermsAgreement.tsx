import React from 'react'
import { View } from '@tarojs/components'
import { AtCheckbox } from 'taro-ui'
import './TermsAgreement.scss'

// AtCheckbox.selectedList 是「已选 option value 数组」(string[]),而非 boolean 数组:
// 勾选状态由 selectedList.includes(value) 判定,onChange 亦回传 string[]
interface TermsAgreementProps {
  agreeTerms: string[]
  onTermsChange: (value: string[]) => void
}

const TermsAgreement: React.FC<TermsAgreementProps> = ({
  agreeTerms,
  onTermsChange
}) => {
  return (
    <View className='terms-section'>
      <AtCheckbox
        options={[{
          value: 'agree',
          label: '我已阅读并同意《用户服务协议》和《隐私政策》'
        }]}
        selectedList={agreeTerms}
        onChange={onTermsChange}
      />
    </View>
  )
}

export default TermsAgreement