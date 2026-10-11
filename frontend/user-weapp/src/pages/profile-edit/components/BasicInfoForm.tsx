import React from 'react'
import { View, Text, Input } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import AvatarUpload from '../components/AvatarUpload'

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
 *
 * 样式:StyleX(原 BasicInfoForm.scss 迁移,750 基准 rpx;weui- / mt2 保留)
 */
const styles = stylex.create({
  profileCells: {
    marginLeft: '16rpx',
    marginRight: '16rpx',
    borderRadius: 'var(--weui-radius)',
  },
  profileAvatarFt: {
    marginLeft: 0,
  },
  fieldError: {
    paddingTop: '12rpx',
    paddingRight: '32rpx',
    paddingBottom: 0,
    paddingLeft: '32rpx',
    marginBottom: '12rpx',
    fontSize: '24rpx',
    lineHeight: 1.5,
    color: 'var(--weui-RED)',
    backgroundColor: 'var(--weui-BG-2)',
  },
})

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
    <View {...props('weui-cells', 'mt2', styles.profileCells)}>
      {/* 头像行 */}
      <View className='weui-cell'>
        <View className='weui-cell__bd'>
          <Text>头像</Text>
        </View>
        <View {...props('weui-cell__ft', styles.profileAvatarFt)}>
          <AvatarUpload
            currentAvatar={avatarUrl}
            onAvatarChange={onAvatarChange}
            onError={onAvatarError}
            disabled={loading}
          />
        </View>
      </View>
      {errors.avatarUrl && (
        <View {...stylex.props(styles.fieldError)}>
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
        <View {...stylex.props(styles.fieldError)}>
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
        <View {...stylex.props(styles.fieldError)}>
          <Text>{errors.phone}</Text>
        </View>
      )}
    </View>
  )
}

export default BasicInfoForm
