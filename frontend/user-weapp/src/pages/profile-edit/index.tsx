import { Component } from 'react'
import { View, Text } from '@tarojs/components'
import { AtForm, AtInput, AtButton, AtToast, AtDivider } from 'taro-ui'
import Taro, { showToast } from '@tarojs/taro'
import { authService, type UserInfo } from '../../utils/auth'
import AvatarUpload from './components/AvatarUpload'

import "taro-ui/dist/style/components/form.scss"
import "taro-ui/dist/style/components/input.scss"
import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/divider.scss"
import './index.scss'

interface ProfileEditPageState {
  userInfo: UserInfo | null
  formData: {
    nickname: string
    phone: string
    avatarUrl: string
  }
  errors: Record<string, string>
  loading: boolean
  uploading: boolean
  hasChanges: boolean
  originalData: Partial<UserInfo>
  showToast: boolean
  toastText: string
  toastType: 'success' | 'error' | 'loading'
}

export default class ProfileEditPage extends Component<{}, ProfileEditPageState> {
  private originalDataRef: Partial<UserInfo> = {}

  constructor(props) {
    super(props)
    this.state = {
      userInfo: null,
      formData: {
        nickname: '',
        phone: '',
        avatarUrl: ''
      },
      errors: {},
      loading: true,
      uploading: false,
      hasChanges: false,
      originalData: {},
      showToast: false,
      toastText: '',
      toastType: 'success'
    }
  }

  componentDidMount() {
    this.loadUserInfo()
  }

  componentDidShow() {
    // 页面显示时刷新用户信息
    this.loadUserInfo()
  }

  private showToast = (text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    this.setState({
      showToast: true,
      toastText: text,
      toastType: type
    })
  }

  private hideToast = () => {
    this.setState({ showToast: false })
  }

  private loadUserInfo = async () => {
    this.setState({ loading: true })

    try {
      if (!authService.isAuthenticated()) {
        this.showToast('请先登录', 'error')
        setTimeout(() => {
          Taro.navigateBack()
        }, 1500)
        return
      }

      const userInfo = authService.getUserInfo()
      if (!userInfo) {
        // 如果没有用户信息，尝试重新获取
        const refreshedUserInfo = await authService.getCurrentUser()
        if (refreshedUserInfo) {
          this.setUserInfo(refreshedUserInfo)
        } else {
          this.showToast('获取用户信息失败', 'error')
          setTimeout(() => {
            Taro.navigateBack()
          }, 1500)
        }
      } else {
        this.setUserInfo(userInfo)
      }

    } catch (error) {
      console.error('Load user info failed:', error)
      const errorMsg = error instanceof Error ? error.message : '获取用户信息失败'
      this.showToast(errorMsg, 'error')
    } finally {
      this.setState({ loading: false })
    }
  }

  private setUserInfo = (userInfo: UserInfo) => {
    const formData = {
      nickname: userInfo.nickname || '',
      phone: userInfo.phone || '',
      avatarUrl: userInfo.avatarUrl || ''
    }

    this.originalDataRef = { ...userInfo }
    this.setState({
      userInfo,
      formData,
      originalData: { ...userInfo }
    })
  }

  private validateNickname = (nickname: string): string => {
    if (!nickname || !nickname.trim()) {
      return '请输入昵称'
    }
    if (nickname.trim().length < 2) {
      return '昵称至少2个字符'
    }
    if (nickname.trim().length > 20) {
      return '昵称不能超过20个字符'
    }
    // 支持中文、英文、数字、下划线、空格
    const nicknameRegex = /^[\u4e00-\u9fa5a-zA-Z0-9_\s]+$/
    if (!nicknameRegex.test(nickname.trim())) {
      return '昵称只能包含中文、英文、数字、下划线和空格'
    }
    return ''
  }

  private validatePhone = (phone: string): string => {
    if (!phone || !phone.trim()) {
      return '请输入手机号'
    }
    // 中国手机号格式验证
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!phoneRegex.test(phone.trim())) {
      return '请输入正确的手机号格式'
    }
    return ''
  }

  private validateForm = (): boolean => {
    const { formData } = this.state
    const errors: Record<string, string> = {}

    const nicknameError = this.validateNickname(formData.nickname)
    if (nicknameError) {
      errors.nickname = nicknameError
    }

    const phoneError = this.validatePhone(formData.phone)
    if (phoneError) {
      errors.phone = phoneError
    }

    this.setState({ errors })
    return Object.keys(errors).length === 0
  }

  private handleInputChange = (field: keyof typeof this.state.formData, value: string) => {
    const { formData, errors } = this.state

    const newFormData = {
      ...formData,
      [field]: value
    }

    // 清除对应字段的错误
    const newErrors = { ...errors }
    delete newErrors[field]

    // 检查是否有变化
    const hasChanges = this.checkForChanges(newFormData)

    this.setState({
      formData: newFormData,
      errors: newErrors,
      hasChanges
    })
  }

  private checkForChanges = (newFormData: typeof this.state.formData): boolean => {
    if (!this.originalDataRef.nickname && !this.originalDataRef.phone && !this.originalDataRef.avatarUrl) {
      return false
    }

    return (
      newFormData.nickname.trim() !== (this.originalDataRef.nickname || '').trim() ||
      newFormData.phone.trim() !== (this.originalDataRef.phone || '').trim() ||
      newFormData.avatarUrl !== (this.originalDataRef.avatarUrl || '')
    )
  }

  private handleAvatarChange = (avatarUrl: string) => {
    const { formData, errors } = this.state

    const newFormData = {
      ...formData,
      avatarUrl
    }

    // 清除可能的头像相关错误
    const newErrors = { ...errors }
    delete newErrors.avatarUrl

    // 检查是否有变化
    const hasChanges = this.checkForChanges(newFormData)

    this.setState({
      formData: newFormData,
      errors: newErrors,
      hasChanges
    })
  }

  private handleAvatarError = (error: string) => {
    this.setState({
      errors: {
        ...this.state.errors,
        avatarUrl: error
      }
    })
  }

  private handleSave = async () => {
    if (!this.validateForm()) {
      this.showToast('请修正表单错误', 'error')
      return
    }

    if (!this.state.hasChanges) {
      this.showToast('没有修改内容', 'error')
      return
    }

    this.setState({ loading: true })
    this.showToast('正在保存...', 'loading')

    try {
      const { formData } = this.state
      const userInfo = authService.getUserInfo()

      if (!userInfo) {
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
      this.originalDataRef = { ...updatedUserInfo }
      this.setState({
        userInfo: updatedUserInfo,
        formData: {
          nickname: updatedUserInfo.nickname || '',
          phone: updatedUserInfo.phone || '',
          avatarUrl: updatedUserInfo.avatarUrl || ''
        },
        hasChanges: false,
        loading: false
      })

      this.showToast('保存成功', 'success')

      // 1.5秒后返回上一页
      setTimeout(() => {
        Taro.navigateBack()
      }, 1500)

    } catch (error) {
      console.error('Save failed:', error)
      const errorMsg = error instanceof Error ? error.message : '保存失败'
      this.showToast(errorMsg, 'error')
      this.setState({ loading: false })
    }
  }

  private handleCancel = () => {
    if (this.state.hasChanges) {
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
  }

  private handleNavigateBack = async () => {
    if (this.state.hasChanges) {
      const result = await new Promise<boolean>((resolve) => {
        Taro.showModal({
          title: '确认返回',
          content: '您有未保存的修改，确定要返回吗？',
          success: (res) => {
            resolve(res.confirm)
          },
          fail: () => {
            resolve(false)
          }
        })
      })

      if (result) {
        Taro.navigateBack()
      }
    } else {
      Taro.navigateBack()
    }
  }

  render() {
    const {
      formData,
      errors,
      loading,
      showToast,
      toastText,
      toastType,
      hasChanges
    } = this.state

    const { userInfo } = this.state

    if (loading && !userInfo) {
      return (
        <View className='profile-edit-page'>
          <View className='loading-container'>
            <Text>加载中...</Text>
          </View>
        </View>
      )
    }

    return (
      <View className='profile-edit-page'>
        {/* 页面头部 */}
        <View className='page-header'>
          <View className='header-left' onClick={this.handleCancel}>
            <Text className='cancel-btn'>取消</Text>
          </View>
          <View className='header-center'>
            <Text className='page-title'>编辑资料</Text>
          </View>
          <View className='header-right'>
            <Text
              className={`save-btn ${hasChanges ? 'active' : 'disabled'}`}
              onClick={hasChanges ? this.handleSave : undefined}
            >
              保存
            </Text>
          </View>
        </View>

        {/* 表单内容 */}
        <View className='form-container'>
          <AtForm>
            {/* 头像上传 */}
            <View className='form-section avatar-section'>
              <Text className='section-title'>头像</Text>
              <View className='avatar-upload-wrapper'>
                <AvatarUpload
                  currentAvatar={formData.avatarUrl}
                  onAvatarChange={this.handleAvatarChange}
                  onError={this.handleAvatarError}
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
                value={formData.nickname}
                onChange={(value) => this.handleInputChange('nickname', value as string)}
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
                value={formData.phone}
                onChange={(value) => this.handleInputChange('phone', value as string)}
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

            {/* 其他信息（只读显示） */}
            {userInfo && (
              <>
                <AtDivider />
                <View className='form-section readonly-section'>
                  <Text className='section-title'>账户信息</Text>

                  <View className='readonly-field'>
                    <Text className='field-label'>用户ID</Text>
                    <Text className='field-value'>{userInfo.id}</Text>
                  </View>

                  <View className='readonly-field'>
                    <Text className='field-label'>会员等级</Text>
                    <Text className='field-value'>{userInfo.level}</Text>
                  </View>

                  <View className='readonly-field'>
                    <Text className='field-label'>账户余额</Text>
                    <Text className='field-value'>¥{userInfo.balance.toFixed(2)}</Text>
                  </View>

                  <View className='readonly-field'>
                    <Text className='field-label'>积分</Text>
                    <Text className='field-value'>{userInfo.points}</Text>
                  </View>

                  {userInfo.isVip && (
                    <View className='readonly-field'>
                      <Text className='field-label'>VIP到期时间</Text>
                      <Text className='field-value'>
                        {userInfo.vipExpireTime ? new Date(userInfo.vipExpireTime).toLocaleDateString() : '永久'}
                      </Text>
                    </View>
                  )}
                </View>
              </>
            )}
          </AtForm>
        </View>

        {/* 底部安全区域 */}
        <View className='safe-bottom' />

        {/* Toast 提示 */}
        <AtToast
          isOpened={showToast}
          text={toastText}
          status={toastType}
          onClose={this.hideToast}
          duration={2000}
        />
      </View>
    )
  }
}