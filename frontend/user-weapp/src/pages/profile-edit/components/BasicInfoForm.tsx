import React from 'react'
import { View, Text, Input } from '@tarojs/components'
import AvatarUpload from '../components/AvatarUpload'

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
 * 基本信息表单区块(profile-edit.html:weui-cells 分组 ——
 * 头像行(圆形缩略图)/ 昵称行(.weui-label + .weui-input)/ 手机号行;
 * 校验与提交逻辑由页面编排,此处仅渲染)
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
    <View className='weui-cells mt2 profile-cells'>
      {/* 头像行 */}
      <View className='weui-cell'>
        <View className='weui-cell__bd'>
          <Text>头像</Text>
        </View>
        <View className='weui-cell__ft profile-avatar-ft'>
          <AvatarUpload
            currentAvatar={avatarUrl}
            onAvatarChange={onAvatarChange}
            onError={onAvatarError}
            disabled={loading}
          />
        </View>
      </View>
      {errors.avatarUrl && (
        <View className='field-error'>
          <Text>{errors.avatarUrl}</Text>
        </View>
      )}

      {/* 昵称行 */}
      <View className='weui-cell'>
        <View className='weui-cell__hd'>
          <View className='weui-label'>昵称</View>
        </View>
        <View className='weui-cell__bd'>
          <Input
            className='weui-input'
            placeholderClass='ph'
            type='text'
            maxlength={20}
            placeholder='请输入昵称（2-20个字符）'
            value={nickname}
            onInput={(e) => onInputChange('nickname', e.detail.value)}
            disabled={loading}
          />
        </View>
      </View>
      {errors.nickname && (
        <View className='field-error'>
          <Text>{errors.nickname}</Text>
        </View>
      )}

      {/* 手机号行 */}
      <View className='weui-cell'>
        <View className='weui-cell__hd'>
          <View className='weui-label'>手机号</View>
        </View>
        <View className='weui-cell__bd'>
          <Input
            className='weui-input'
            placeholderClass='ph'
            type='number'
            maxlength={11}
            placeholder='请输入手机号'
            value={phone}
            onInput={(e) => onInputChange('phone', e.detail.value)}
            disabled={loading}
          />
        </View>
      </View>
      {errors.phone && (
        <View className='field-error'>
          <Text>{errors.phone}</Text>
        </View>
      )}
    </View>
  )
}

export default BasicInfoForm
