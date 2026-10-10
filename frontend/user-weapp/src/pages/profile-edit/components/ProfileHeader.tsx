import React from 'react'
import { View, Text } from '@tarojs/components'

import './ProfileHeader.scss'

interface ProfileHeaderProps {
  hasChanges: boolean
  onCancel: () => void
  onSave: () => void
}

/**
 * 页面头部:取消 / 保存(profile-edit.html:保存按钮 weui-btn_mini primary,
 * 未修改时置灰禁用;保留 track* 埋点)
 */
const ProfileHeader: React.FC<ProfileHeaderProps> = ({ hasChanges, onCancel, onSave }) => {
  return (
    <View className='page-header'>
      <View className='header-left' onClick={onCancel}>
        <Text className='cancel-btn'>取消</Text>
      </View>
      <View className='header-right'>
        <Text
          trackTarget="profile-save"
          trackAction="FORM_SUBMIT"
          trackElementType="button"
          trackElementText="保存"
          trackDisabled={!hasChanges}
          onClick={hasChanges ? onSave : undefined}
          className={`weui-btn weui-btn_mini ${hasChanges ? 'weui-btn_primary' : 'weui-btn_disabled'}`}
        >
          保存
        </Text>
      </View>
    </View>
  )
}

export default ProfileHeader
