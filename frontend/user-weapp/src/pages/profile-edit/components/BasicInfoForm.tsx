import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtInput, AtDivider } from 'taro-ui'
import AvatarUpload from '../components/AvatarUpload'

import "taro-ui/dist/style/components/input.scss"
import "taro-ui/dist/style/components/divider.scss"
import './BasicInfoForm.scss'

interface BasicInfoFormProps {
  avatarUrl: string
  nickname: string
  phone: string
  errors: Record<string, string>
  loading: boolean
  onAvatarChange: (avatarUrl: string) => void
  onAvatarError: (error: string) => void
  onInputChange: (field: 'nickname' | 'phone', value: string) => void
}

/**
 * 基本信息表单区块(头像上传 + 昵称 + 手机号,由页面外层 AtForm 包裹)
 */
const BasicInfoForm: React.FC<BasicInfoFormProps> = ({
  avatarUrl,
  nickname,
  phone,
  errors,
  loading,
  onAvatarChange,
  onAvatarError,
  onInputChange
}) => {
  return (
    <>
      {/* 头像上传 */}
      <View className='form-section avatar-section'>
        <Text className='section-title'>头像</Text>
        <View className='avatar-upload-wrapper'>
          <AvatarUpload
            currentAvatar={avatarUrl}
            onAvatarChange={onAvatarChange}
            onError={onAvatarError}
            disabled={loading}
            size='large'
          />
        </View>
        {errors.avatarUrl && (
          <Text className='error-text'>{errors.avatarUrl}</Text>
        )}
      </View>

      <AtDivider />

      {/* 基本信息 */}
      <View className='form-section'>
        <Text className='section-title'>基本信息</Text>

        <AtInput
          name='nickname'
          title='昵称'
          type='text'
          placeholder='请输入昵称（2-20个字符）'
          value={nickname}
          onChange={(value) => onInputChange('nickname', value as string)}
          error={!!errors.nickname}
          disabled={loading}
          maxLength={20}
          clear
        />
        {errors.nickname && (
          <View className='error-message'>
            <Text className='error-text'>{errors.nickname}</Text>
          </View>
        )}

        <AtInput
          name='phone'
          title='手机号'
          type='phone'
          placeholder='请输入手机号'
          value={phone}
          onChange={(value) => onInputChange('phone', value as string)}
          error={!!errors.phone}
          disabled={loading}
          maxLength={11}
          clear
        />
        {errors.phone && (
          <View className='error-message'>
            <Text className='error-text'>{errors.phone}</Text>
          </View>
        )}
      </View>
    </>
  )
}

export default BasicInfoForm
