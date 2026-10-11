import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, Image } from '@tarojs/components'
import Taro from '@tarojs/taro'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { fileUploadService } from '../../../services/FileUploadService'
import { UploadError } from '../../../types/storage'

interface AvatarUploadProps {
  currentAvatar?: string
  onAvatarChange: (avatarUrl: string) => void
  onError: (error: string) => void
  disabled?: boolean
  size?: 'large' | 'normal' | 'small'
}

/**
 * 头像上传(profile-edit.html:weui-cell 头像行右侧 48px 圆形缩略图;
 * 选择/上传/错误处理逻辑与原实现一致)
 *
 * 样式:StyleX(原 AvatarUpload.scss 迁移,750 基准 rpx)
 */
const styles = stylex.create({
  avatarUpload: {
    position: 'relative',
    width: '96rpx',
    height: '96rpx',
  },
  avatarUploadThumb: {
    width: '96rpx',
    height: '96rpx',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '22rpx',
    color: 'var(--weui-FG-1)',
  },
  avatarUploadPlaceholder: {
    backgroundColor: 'var(--weui-BG-1)',
  },
  avatarUploadOverlay: {
    position: 'absolute',
    left: 0,
    top: 0,
    right: 0,
    bottom: 0,
    borderRadius: '50%',
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarUploadOverlayText: {
    fontSize: '20rpx',
    color: tokens.onBrand,
  },
  avatarUploadDisabled: {
    opacity: 0.6,
  },
})
const AvatarUpload: React.FC<AvatarUploadProps> = ({
  currentAvatar,
  onAvatarChange,
  onError,
  disabled = false
}) => {
  const [uploading, setUploading] = useState<boolean>(false)
  const [previewUrl, setPreviewUrl] = useState<string>(currentAvatar || '')

  // 更新预览URL
  useEffect(() => {
    setPreviewUrl(currentAvatar || '')
  }, [currentAvatar])

  // 上传头像
  const uploadAvatar = useCallback(async (filePath: string): Promise<void> => {
    try {
      setUploading(true)

      const avatarUrl = await fileUploadService.uploadAvatar(filePath)
      setPreviewUrl(avatarUrl)

      // 通知父组件
      onAvatarChange(avatarUrl)

      Taro.showToast({ title: '头像上传成功', icon: 'success' })

    } catch (error) {
      console.error('Avatar upload failed:', error)

      // 恢复原始头像
      setPreviewUrl(currentAvatar || '')

      let errorMsg = '上传失败'
      if (error instanceof Error) {
        errorMsg = error.message
      } else if ((error as UploadError).code) {
        const uploadError = error as UploadError
        errorMsg = uploadError.message
      }

      Taro.showToast({ title: errorMsg, icon: 'none' })
      onError?.(errorMsg)

      throw error
    } finally {
      setUploading(false)
    }
  }, [currentAvatar, onAvatarChange, onError])

  // 处理头像选择
  const handleAvatarSelect = useCallback(async () => {
    if (disabled || uploading) {
      return
    }

    try {
      // 选择图片
      const chooseResult = await Taro.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera']
      })

      if (chooseResult.errMsg !== 'chooseImage:ok') {
        throw new Error(`选择图片失败: ${chooseResult.errMsg}`)
      }

      const tempFilePath = chooseResult.tempFilePaths[0]

      // 显示预览
      setPreviewUrl(tempFilePath)

      // 上传文件
      await uploadAvatar(tempFilePath)

    } catch (error) {
      console.error('Avatar selection failed:', error)

      // 恢复原始头像
      setPreviewUrl(currentAvatar || '')

      let errorMsg = '选择图片失败'
      if (error instanceof Error) {
        errorMsg = error.message
      } else if ((error as UploadError).code) {
        const uploadError = error as UploadError
        errorMsg = uploadError.message
      }

      Taro.showToast({ title: errorMsg, icon: 'none' })
      onError?.(errorMsg)
    }
  }, [disabled, uploading, currentAvatar, uploadAvatar, onError])

  return (
    <View
      {...props(styles.avatarUpload, disabled && styles.avatarUploadDisabled)}
      trackTarget='profile-avatar-upload'
      trackElementType='button'
      trackElementText='上传头像'
      onClick={handleAvatarSelect}
    >
      {previewUrl ? (
        <Image
          src={previewUrl}
          {...stylex.props(styles.avatarUploadThumb)}
          mode='aspectFill'
        />
      ) : (
        <View {...stylex.props(styles.avatarUploadThumb, styles.avatarUploadPlaceholder)}>
          <Text>头像</Text>
        </View>
      )}

      {uploading && (
        <View {...stylex.props(styles.avatarUploadOverlay)}>
          <Text {...stylex.props(styles.avatarUploadOverlayText)}>上传中</Text>
        </View>
      )}
    </View>
  )
}

export default AvatarUpload
