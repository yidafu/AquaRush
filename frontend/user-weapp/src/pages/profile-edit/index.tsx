import React, { useCallback, useEffect, useRef, useState } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { pageTheme } from '../../styles/theme.stylex'
import { authService, type UserInfo } from '../../utils/auth'
import { userActionTracker } from '../../utils/tracking'
import { validateProfileForm } from './utils/profileValidation'

// 子组件
import ProfileHeader from './components/ProfileHeader'
import BasicInfoForm from './components/BasicInfoForm'
import AccountInfoSection from './components/AccountInfoSection'

interface ProfileFormData {
  nickname: string
  phone: string
  avatarUrl: string
}

const EMPTY_FORM: ProfileFormData = { nickname: '', phone: '', avatarUrl: '' }

/**
 * 编辑资料页(profile-edit.html:WeUI 分组表单 cells)——
 * 页面仅保留用户信息加载/保存/校验编排,渲染由 components/ 完成。
 *
 * 样式:StyleX(原 index.scss 迁移,750 基准 rpx;weui- / 全局类保留)
 */
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    backgroundColor: 'var(--weui-BG-0)',
    boxSizing: 'border-box',
    paddingBottom: 'env(safe-area-inset-bottom)',
  },
  profileLoading: {
    paddingTop: '40vh',
  },
  profileTip: {
    paddingBottom: '48rpx',
  },
  // 轻提示(.weui-toast_text 深色胶囊;原后代选择器拍平到子元素)
  toastText: {
    color: tokens.onBrand,
    wordBreak: 'break-all',
  },
})
const ProfileEditPage: React.FC = () => {
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null)
  const [formData, setFormData] = useState<ProfileFormData>(EMPTY_FORM)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState<boolean>(true)
  const [hasChanges, setHasChanges] = useState<boolean>(false)
  const [showToast, setShowToast] = useState<boolean>(false)
  const [toastText, setToastText] = useState<string>('')

  const originalDataRef = useRef<Partial<UserInfo>>({})

  // type 仅保留调用方语义(success / error / loading),样式变体类无定义不迁移
  const showToastMessage = useCallback((text: string, _type: 'success' | 'error' | 'loading' = 'success') => {
    setShowToast(true)
    setToastText(text)
  }, [])

  const hideToast = useCallback(() => {
    setShowToast(false)
  }, [])

  const applyUserInfo = useCallback((info: UserInfo) => {
    const newFormData = {
      nickname: info.nickname || '',
      phone: info.phone || '',
      avatarUrl: info.avatarUrl || ''
    }

    originalDataRef.current = { ...info }
    setUserInfo(info)
    setFormData(newFormData)
  }, [])

  const loadUserInfo = useCallback(async () => {
    setLoading(true)

    try {
      if (!authService.isAuthenticated()) {
        showToastMessage('请先登录', 'error')
        setTimeout(() => {
          Taro.navigateBack()
        }, 1500)
        return
      }

      const cachedUserInfo = authService.getUserInfo()
      if (!cachedUserInfo) {
        // 如果没有用户信息，尝试重新获取
        const refreshedUserInfo = await authService.getCurrentUser()
        if (refreshedUserInfo) {
          applyUserInfo(refreshedUserInfo)
        } else {
          showToastMessage('获取用户信息失败', 'error')
          setTimeout(() => {
            Taro.navigateBack()
          }, 1500)
        }
      } else {
        applyUserInfo(cachedUserInfo)
      }
    } catch (error) {
      console.error('Load user info failed:', error)
      const errorMsg = error instanceof Error ? error.message : '获取用户信息失败'
      showToastMessage(errorMsg, 'error')
    } finally {
      setLoading(false)
    }
  }, [applyUserInfo, showToastMessage])

  // 对比原始数据,判断是否有修改
  const checkForChanges = useCallback((newFormData: ProfileFormData): boolean => {
    const original = originalDataRef.current
    if (!original.nickname && !original.phone && !original.avatarUrl) {
      return false
    }

    return (
      newFormData.nickname.trim() !== (original.nickname || '').trim() ||
      newFormData.phone.trim() !== (original.phone || '').trim() ||
      newFormData.avatarUrl !== (original.avatarUrl || '')
    )
  }, [])

  const handleInputChange = useCallback((field: 'nickname' | 'phone', value: string) => {
    setFormData(prev => {
      const newFormData = { ...prev, [field]: value }

      // 检查是否有变化
      setHasChanges(checkForChanges(newFormData))
      return newFormData
    })

    // 清除对应字段的错误
    setErrors(prev => {
      const newErrors = { ...prev }
      delete newErrors[field]
      return newErrors
    })
  }, [checkForChanges])

  const handleAvatarChange = useCallback((avatarUrl: string) => {
    setFormData(prev => {
      const newFormData = { ...prev, avatarUrl }
      setHasChanges(checkForChanges(newFormData))
      return newFormData
    })

    // 清除可能的头像相关错误
    setErrors(prev => {
      const newErrors = { ...prev }
      delete newErrors.avatarUrl
      return newErrors
    })
  }, [checkForChanges])

  const handleAvatarError = useCallback((error: string) => {
    setErrors(prev => ({
      ...prev,
      avatarUrl: error
    }))
  }, [])

  const validateForm = useCallback((): boolean => {
    const newErrors = validateProfileForm(formData)
    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }, [formData])

  const handleSave = useCallback(async () => {
    if (!validateForm()) {
      showToastMessage('请修正表单错误', 'error')
      return
    }

    if (!hasChanges) {
      showToastMessage('没有修改内容', 'error')
      return
    }

    setLoading(true)
    showToastMessage('正在保存...', 'loading')

    try {
      const cachedUserInfo = authService.getUserInfo()

      if (!cachedUserInfo) {
        throw new Error('用户信息不存在')
      }

      // 准备更新数据
      const updateData: Partial<UserInfo> = {
        nickname: formData.nickname.trim(),
        phone: formData.phone.trim(),
        avatarUrl: formData.avatarUrl
      }

      // 调用更新API
      const updatedUserInfo = await authService.updateUserInfo(updateData)

      // 更新本地状态
      originalDataRef.current = { ...updatedUserInfo }
      setUserInfo(updatedUserInfo)
      setFormData({
        nickname: updatedUserInfo.nickname || '',
        phone: updatedUserInfo.phone || '',
        avatarUrl: updatedUserInfo.avatarUrl || ''
      })
      setHasChanges(false)
      setLoading(false)

      showToastMessage('保存成功', 'success')

      // 1.5秒后返回上一页
      setTimeout(() => {
        Taro.navigateBack()
      }, 1500)
    } catch (error) {
      console.error('Save failed:', error)
      const errorMsg = error instanceof Error ? error.message : '保存失败'
      showToastMessage(errorMsg, 'error')
      setLoading(false)
    }
  }, [validateForm, hasChanges, formData, showToastMessage])

  const handleCancel = useCallback(() => {
    if (hasChanges) {
      Taro.showModal({
        title: '确认取消',
        content: '您有未保存的修改，确定要取消吗？',
        success: (res) => {
          if (res.confirm) {
            Taro.navigateBack()
          }
        }
      })
    } else {
      Taro.navigateBack()
    }
  }, [hasChanges])

  // 挂载时加载用户信息
  useEffect(() => {
    loadUserInfo()
  }, [loadUserInfo])

  // 页面显示时刷新用户信息
  useDidShow(() => {
    userActionTracker.trackPageView()
    loadUserInfo()
  })

  if (loading && !userInfo) {
    return (
      <View {...stylex.props(pageTheme, styles.page)}>
        <View {...props('weui-loadmore', 'weui-loadmore_loading', styles.profileLoading)}>
          <View className='spinner' />
          <Text>加载中...</Text>
        </View>
      </View>
    )
  }

  return (
    <View {...stylex.props(pageTheme, styles.page)}>
      {/* 顶部操作条(取消 / 保存) */}
      <ProfileHeader
        hasChanges={hasChanges}
        onCancel={handleCancel}
        onSave={handleSave}
      />

      {/* 表单内容(头像行 / 昵称行 / 手机号行;校验逻辑见 utils/profileValidation) */}
      <BasicInfoForm
        avatarUrl={formData.avatarUrl}
        nickname={formData.nickname}
        phone={formData.phone}
        errors={errors}
        loading={loading}
        onAvatarChange={handleAvatarChange}
        onAvatarError={handleAvatarError}
        onInputChange={handleInputChange}
      />

      {/* 其他信息（只读显示） */}
      <AccountInfoSection userInfo={userInfo} />

      <View {...props('weui-cells__tips', styles.profileTip)}>
        点击头像可更换头像（相册 / 拍照）。
      </View>

      {/* 底部安全区域 */}
      <View className='safe-bottom' />

      {/* Toast 提示(.weui-toast_text 深色轻提示) */}
      {showToast && (
        <View className='weui-toast weui-toast_text' onClick={hideToast}>
          <Text {...props('weui-toast__text', styles.toastText)}>{toastText}</Text>
        </View>
      )}
    </View>
  )
}

export default ProfileEditPage
