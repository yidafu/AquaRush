import React from 'react'
import { View, Text } from '@tarojs/components'

import './SubmitSection.scss'

interface SubmitSectionProps {
  isDefault: boolean
  loading: boolean
  onDefaultChange: (checked: boolean) => void
  onSubmit: () => void
}

/**
 * 底部提交区(address-edit.html:.weui-cell_switch 默认地址开关 +
 * .weui-btn_area 保存按钮;保留 track* 埋点)
 */
const SubmitSection: React.FC<SubmitSectionProps> = ({ isDefault, loading, onDefaultChange, onSubmit }) => {
  return (
    <>
      {/* 设为默认地址(.weui-switch on 态品牌色) */}
      <View className='weui-cells mt3'>
        <View
          className='weui-cell weui-cell_switch'
          onClick={() => onDefaultChange(!isDefault)}
        >
          <View className='weui-cell__bd'>设为默认地址</View>
          <View className='weui-cell__ft'>
            <View className={`weui-switch ${isDefault ? 'on' : ''}`} />
          </View>
        </View>
      </View>

      {/* 保存按钮(.weui-btn_area) */}
      <View className='weui-btn_area submit-section'>
        <View
          trackTarget="address-save"
          trackAction="FORM_SUBMIT"
          trackElementType="button"
          trackElementText="保存地址"
          trackDisabled={loading}
          onClick={onSubmit}
          className={`weui-btn weui-btn_primary flex1 ${loading ? 'weui-btn_disabled' : ''}`}
        >
          <Text>{loading ? '保存中...' : '保存地址'}</Text>
        </View>
      </View>
    </>
  )
}

export default SubmitSection
