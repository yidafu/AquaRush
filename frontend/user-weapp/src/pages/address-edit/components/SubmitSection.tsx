import React from 'react'
import { View } from '@tarojs/components'
import { AtSwitch, AtButton } from 'taro-ui'

import "taro-ui/dist/style/components/switch.scss"
import "taro-ui/dist/style/components/button.scss"
import './SubmitSection.scss'

interface SubmitSectionProps {
  isDefault: boolean
  loading: boolean
  onDefaultChange: (checked: boolean) => void
  onSubmit: () => void
}

/**
 * 底部提交区:设为默认地址开关 + 保存按钮(保留 track* 埋点)
 */
const SubmitSection: React.FC<SubmitSectionProps> = ({ isDefault, loading, onDefaultChange, onSubmit }) => {
  return (
    <>
      {/* 设为默认地址 */}
      <AtSwitch
        title='设为默认地址'
        checked={isDefault}
        onChange={(value) => onDefaultChange(value)}
      />

      {/* 提交按钮 */}
      <View className='submit-section'>
        <AtButton
          trackTarget="address-save"
          trackAction="FORM_SUBMIT"
          trackElementType="button"
          trackElementText="保存地址"
          trackDisabled={loading}
          onClick={onSubmit}
          type='primary'
          size='normal'
          loading={loading}
          disabled={loading}
          className='submit-button'
        >
          {loading ? '保存中...' : '保存地址'}
        </AtButton>
      </View>
    </>
  )
}

export default SubmitSection
