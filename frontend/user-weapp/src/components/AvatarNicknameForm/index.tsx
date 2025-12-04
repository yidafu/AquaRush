import React, { useState, useEffect } from 'react'
import { View, Text, Button, Input, Image } from '@tarojs/components'
import { AtAvatar, AtButton, AtModal, AtModalHeader, AtModalContent, AtModalAction, AtToast } from 'taro-ui'
import Taro from '@tarojs/taro'
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
  const handleNicknameInput = (e: any): void => {
    setNickname(e.detail.value)
  }

  // 上传头像到服务器
  const uploadAvatar = async (tempFilePath: string): Promise<string> => {
    return new Promise((resolve, reject) => {
      Taro.uploadFile({
        url: 'http://localhost:8080/api/upload/avatar',
        filePath: tempFilePath,
        name: 'avatar',
        header: {
          'Authorization': `Bearer ${Taro.getStorageSync('token')}`
        },
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            if (data.code === 0) {
              resolve(data.data.url)
            } else {
              reject(new Error(data.message || '头像上传失败'))
            }
          } catch (error) {
            reject(new Error('头像上传响应解析失败'))
          }
        },
        fail: (error) => {
          reject(new Error('头像上传失败'))
        }
      })
    })
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

  // 关闭弹窗
  const handleClose = (): void => {
    // 重置状态
    setAvatarUrl(initialData.avatarUrl || '')
    setNickname(initialData.nickname || '')
    setIsSubmitting(false)
    onClose()
  }

  return (
    <>
      {/* <AtModal
        isOpened
        title='标题'
        cancelText='取消'
        confirmText='确认'
        onClose={handleClose}
        onCancel={handleClose}
        onConfirm={handleSubmit}
        content='欢迎加入京东凹凸实验室\n\r欢迎加入京东凹凸实验室'
      /> */}
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
          <View className='avatar-nickname-form'>
            <Text className='form-tip'>请选择您的头像并设置昵称</Text>

            {/* 头像和昵称左右布局 */}
            <View className='form-content'>
              {/* 头像选择区域 */}
              <View className='avatar-section'>
                <Text className='section-title'>头像</Text>
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
                <Text className='avatar-hint'>点击选择头像</Text>
              </View>

              {/* 昵称输入区域 */}
              <View className='nickname-section'>
                <Text className='section-title'>昵称</Text>
                <Input
                  type='nickname'
                  className='nickname-input'
                  placeholder='请输入昵称（1-20个字符）'
                  value={nickname}
                  onInput={handleNicknameInput}
                  maxlength={20}
                />
              </View>
            </View>
          </View>
        </AtModalContent>

        <AtModalAction>
          <AtButton type='secondary' onClick={handleClose}>取消</AtButton>
          <AtButton type='primary' onClick={handleSubmit} loading={isSubmitting}>
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
