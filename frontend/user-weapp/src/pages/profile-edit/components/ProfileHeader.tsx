import React from 'react'
import { View, Text } from '@tarojs/components'

import './ProfileHeader.scss'

interface ProfileHeaderProps {
  hasChanges: boolean
  onCancel: () => void
  onSave: () => void
}

/**
 * 页面头部:取消 / 标题 / 保存(未修改时保存禁用,保留 track* 埋点)
 */
const ProfileHeader: React.FC<ProfileHeaderProps> = ({ hasChanges, onCancel, onSave }) => {
  return (
    <View className='page-header'>
      <View className='header-left' onClick={onCancel}>
        <Text className='cancel-btn'>取消</Text>
      </View>
      <View className='header-center'>
        <Text className='page-title'>编辑资料</Text>
      </View>
      <View className='header-right'>
        <Text
          trackTarget="profile-save"
          trackAction="FORM_SUBMIT"
          trackElementType="button"
          trackElementText="保存"
          trackDisabled={!hasChanges}
          onClick={hasChanges ? onSave : undefined}
          className={`save-btn ${hasChanges ? 'active' : 'disabled'}`}
        >
          保存
        </Text>
      </View>
    </View>
  )
}

export default ProfileHeader
