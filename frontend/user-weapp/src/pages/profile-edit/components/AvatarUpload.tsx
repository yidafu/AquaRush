import React, { useState, useEffect, useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import { AtAvatar, AtToast } from 'taro-ui'
import Taro from '@tarojs/taro'
import { fileUploadService } from '../../../services/FileUploadService'
import { UploadError } from '../../../types/storage'

import "taro-ui/dist/style/components/avatar.scss"
import "taro-ui/dist/style/components/toast.scss"
import './AvatarUpload.scss'

interface AvatarUploadProps {
  currentAvatar?: string
  onAvatarChange: (avatarUrl: string) => void
  onError: (error: string) => void
  disabled?: boolean
  size?: 'large' | 'normal' | 'small'
}

const AvatarUpload: React.FC<AvatarUploadProps> = ({
  currentAvatar,
  onAvatarChange,
  onError,
  disabled = false,
  size = 'large'
}) => {
  const [uploading, setUploading] = useState<boolean>(false)
  const [previewUrl, setPreviewUrl] = useState<string>(currentAvatar || '')
  const [showToast, setShowToast] = useState<boolean>(false)
  const [toastText, setToastText] = useState<string>('')
  const [toastType, setToastType] = useState<'success' | 'error' | 'loading'>('success')

  // 更新预览URL
  useEffect(() => {
    setPreviewUrl(currentAvatar || '')
  }, [currentAvatar])

  // 显示Toast提示
  const showToastMessage = useCallback((text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    setShowToast(true)
    setToastText(text)
    setToastType(type)
  }, [])

  // 隐藏Toast提示
  const hideToast = useCallback(() => {
    setShowToast(false)
  }, [])

  // 上传头像
  const uploadAvatar = useCallback(async (filePath: string): Promise<void> => {
    try {
      setUploading(true)
      showToastMessage('正在上传头像...', 'loading')

      const avatarUrl = await fileUploadService.uploadAvatar(filePath)
      setPreviewUrl(avatarUrl)

      // 通知父组件
      onAvatarChange(avatarUrl)

      showToastMessage('头像上传成功', 'success')

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

      showToastMessage(errorMsg, 'error')
      onError?.(errorMsg)

      throw error
    } finally {
      setUploading(false)
    }
  }, [currentAvatar, onAvatarChange, onError, showToastMessage])

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

      showToastMessage(errorMsg, 'error')
      onError?.(errorMsg)
    }
  }, [disabled, uploading, currentAvatar, uploadAvatar, showToastMessage, onError])

  const avatarSize = size === 'large' ? 'large' : size === 'small' ? 'small' : 'normal'

  return (
    <View className='avatar-upload'>
      <View
        className={`avatar-container ${disabled ? 'disabled' : ''} ${uploading ? 'uploading' : ''}`}
        onClick={handleAvatarSelect}
      >
        {previewUrl ? (
          <AtAvatar
            image={previewUrl}
            size={avatarSize}
            circle
            className='avatar-preview'
          />
        ) : (
          <AtAvatar
            size={avatarSize}
            circle
            text='上传头像'
            className='avatar-placeholder'
          />
        )}

        {uploading && (
          <View className='uploading-overlay'>
            <Text className='uploading-text'>上传中...</Text>
          </View>
        )}

        {!disabled && !uploading && (
          <View className='upload-hint'>
            <Text className='hint-text'>点击上传</Text>
          </View>
        )}
      </View>

      <View className='avatar-tips'>
        <Text className='tips-text'>
          支持 JPG、PNG、GIF、WebP 格式，文件大小不超过 5MB
        </Text>
      </View>

      {/* Toast 提示 */}
      <AtToast
        isOpened={showToast}
        text={toastText}
        status={toastType}
        onClose={hideToast}
        duration={2000}
      />
    </View>
  )
}

export default AvatarUpload
