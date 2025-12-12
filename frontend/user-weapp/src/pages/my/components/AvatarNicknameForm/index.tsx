import React, { useState, useEffect } from 'react'
import { View, Text, Button, Image } from '@tarojs/components'
import { AtAvatar, AtButton, AtModal, AtModalHeader, AtModalContent, AtModalAction, AtToast, AtForm, AtInput } from 'taro-ui'
import { fileUploadService } from '@/services/FileUploadService'
import { UploadError } from '@/types/storage'
import "taro-ui/dist/style/components/avatar.scss"
import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/modal.scss"
import "taro-ui/dist/style/components/toast.scss"
import './index.scss'

interface AvatarNicknameFormProps {
  visible: boolean
  onClose: () => void
  onSubmit: (data: { avatarUrl: string; nickname: string }) => void
  initialData?: {
    avatarUrl?: string
    nickname?: string
  }
}

const AvatarNicknameForm: React.FC<AvatarNicknameFormProps> = ({
  visible,
  onClose,
  onSubmit,
  initialData = {
    avatarUrl: '',
    nickname: ''
  }
}) => {
  const [avatarUrl, setAvatarUrl] = useState<string>(initialData.avatarUrl || '')
  const [nickname, setNickname] = useState<string>(initialData.nickname || '')
  const [showToast, setShowToast] = useState<boolean>(false)
  const [toastText, setToastText] = useState<string>('')
  const [toastType, setToastType] = useState<'success' | 'error' | 'loading'>('success')
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)

  useEffect(() => {
    if (initialData) {
      setAvatarUrl(initialData.avatarUrl || '')
      setNickname(initialData.nickname || '')
    }
  }, [initialData])

  const showToastMessage = (text: string, type: 'success' | 'error' | 'loading' = 'success'): void => {
    setShowToast(true)
    setToastText(text)
    setToastType(type)
  }

  const hideToast = (): void => {
    setShowToast(false)
  }

  // 选择头像
  const handleChooseAvatar = (e: any): void => {
    const { avatarUrl: selectedAvatarUrl } = e.detail
    setAvatarUrl(selectedAvatarUrl)
  }

  // 昵称输入
  const handleNicknameInput = (value: string, e: any): void => {
    setNickname(value)
  }

  // 上传头像到服务器
  const uploadAvatar = async (tempFilePath: string): Promise<string> => {
    try {
      return await fileUploadService.uploadAvatar(tempFilePath)
    } catch (error) {
      console.error('Avatar upload failed:', error)

      if (error instanceof Error) {
        throw error
      }

      // 处理UploadError
      if ((error as UploadError).code) {
        const uploadError = error as UploadError
        throw new Error(uploadError.message)
      }

      throw new Error('头像上传失败')
    }
  }

  // 提交表单
  const handleSubmit = async (): Promise<void> => {
    // 验证
    if (!nickname.trim()) {
      showToastMessage('请输入昵称', 'error')
      return
    }

    if (nickname.trim().length > 20) {
      showToastMessage('昵称不能超过20个字符', 'error')
      return
    }

    if (!avatarUrl) {
      showToastMessage('请选择头像', 'error')
      return
    }

    try {
      setIsSubmitting(true)
      showToastMessage('正在保存...', 'loading')

      let finalAvatarUrl = avatarUrl

      // 如果是临时文件路径，需要上传到服务器
      if (avatarUrl.startsWith('wxfile://') || avatarUrl.startsWith('http://tmp/') || avatarUrl.startsWith('file://')) {
        finalAvatarUrl = await uploadAvatar(avatarUrl)
      }

      // 提交数据
      await onSubmit({
        avatarUrl: finalAvatarUrl,
        nickname: nickname.trim()
      })

      showToastMessage('保存成功', 'success')

      // 延迟关闭
      setTimeout(() => {
        handleClose()
      }, 1000)

    } catch (error) {
      console.error('保存头像昵称失败:', error)
      const errorMsg = error instanceof Error ? error.message : '保存失败'
      showToastMessage(errorMsg, 'error')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleReset = () => {
    setAvatarUrl(initialData.avatarUrl || '')
    setNickname(initialData.nickname || '')
    setIsSubmitting(false)
  }

  // 关闭弹窗
  const handleClose = (): void => {
    // 重置状态
    handleReset()
    onClose()
  }

  return (
    <>
      <AtModal
        isOpened={visible}
        onClose={handleClose}
        className='avatar-nickname-modal'
        confirmText='确认'
        cancelText='取消'
        onConfirm={handleSubmit}
      >
        <AtModalHeader>完善个人资料</AtModalHeader>

        <AtModalContent>
          <Text className='form-tip'>请选择您的头像并设置昵称</Text>

          <AtForm>
            {/* 头像选择区域 */}
            <View className='border-b avatar-section border-b-stone-300'>
              <Text className='section-title'>头像</Text>
              <View className='flex flex-col justify-center'>

              <Button
                className='avatar-button'
                openType='chooseAvatar'
                onChooseAvatar={handleChooseAvatar}
              >
                {avatarUrl ? (
                  <Image
                    src={avatarUrl}
                    className='avatar-preview'
                    mode='aspectFill'
                  />
                ) : (
                  <AtAvatar
                    size='large'
                    circle
                    text='头像'
                    className='default-avatar'
                  />
                )}
              </Button>
                <Text className='text-center avatar-hint '>点击选择头像</Text>
              </View>

            </View>

            <AtInput
              title='昵称'
              type='nickname'
              name='nickname'
              value={nickname}
              onChange={handleNicknameInput}
            />
          </AtForm>
        </AtModalContent>

        <AtModalAction>
          <AtButton full onClick={handleClose} >取消</AtButton>
          <AtButton full type='primary' onClick={handleSubmit} loading={isSubmitting}>
            确认
          </AtButton>
        </AtModalAction>

      </AtModal>
      {/* Toast 提示 */}
      <AtToast
        isOpened={showToast}
        text={toastText}
        status={toastType}
        onClose={hideToast}
        duration={2000}
      />
    </>
  )
}

export default AvatarNicknameForm
