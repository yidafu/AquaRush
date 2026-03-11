import React from 'react'
import { View } from '@tarojs/components'
import { AtCheckbox } from 'taro-ui'
import './TermsAgreement.scss'

interface TermsAgreementProps {
  agreeTerms: boolean[]
  onTermsChange: (value: boolean[]) => void
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