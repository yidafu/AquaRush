import { Component } from 'react'
import { View, Text, Image } from '@tarojs/components'
import { AtAvatar, AtToast } from 'taro-ui'
import Taro from '@tarojs/taro'
import { authService } from '../../../utils/auth'

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

interface AvatarUploadState {
  uploading: boolean
  previewUrl: string
  showToast: boolean
  toastText: string
  toastType: 'success' | 'error' | 'loading'
}

export default class AvatarUpload extends Component<AvatarUploadProps, AvatarUploadState> {
  constructor(props: AvatarUploadProps) {
    super(props)
    this.state = {
      uploading: false,
      previewUrl: props.currentAvatar || '',
      showToast: false,
      toastText: '',
      toastType: 'success'
    }
  }

  static defaultProps: Partial<AvatarUploadProps> = {
    size: 'large',
    disabled: false
  }

  componentDidUpdate(prevProps: AvatarUploadProps) {
    if (prevProps.currentAvatar !== this.props.currentAvatar) {
      this.setState({ previewUrl: this.props.currentAvatar || '' })
    }
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

  private validateFile = (filePath: string): boolean => {
    // 检查文件大小（5MB限制）
    try {
      const fileInfo = Taro.getFileSystemManager().statSync(filePath)
      const maxSize = 5 * 1024 * 1024 // 5MB

      if (fileInfo.size > maxSize) {
        this.showToast('图片大小不能超过5MB', 'error')
        return false
      }

      // 检查文件扩展名
      const allowedExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']
      const extension = filePath.split('.').pop()?.toLowerCase()

      if (!extension || !allowedExtensions.includes(extension)) {
        this.showToast('请选择JPG、PNG、GIF或WebP格式的图片', 'error')
        return false
      }

      return true
    } catch (error) {
      console.error('File validation failed:', error)
      this.showToast('文件验证失败', 'error')
      return false
    }
  }

  private handleAvatarSelect = async () => {
    if (this.props.disabled || this.state.uploading) {
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

      // 验证文件
      if (!this.validateFile(tempFilePath)) {
        return
      }

      // 显示预览
      this.setState({ previewUrl: tempFilePath })

      // 上传文件
      await this.uploadAvatar(tempFilePath)

    } catch (error) {
      console.error('Avatar selection failed:', error)
      const errorMsg = error instanceof Error ? error.message : '选择图片失败'
      this.showToast(errorMsg, 'error')
      this.props.onError?.(errorMsg)
    }
  }

  private uploadAvatar = async (filePath: string): Promise<void> => {
    const token = authService.getToken()

    if (!token) {
      throw new Error('请先登录')
    }

    this.setState({ uploading: true })
    this.showToast('正在上传头像...', 'loading')

    try {
      const uploadResult = await Taro.uploadFile({
        url: `${process.env.NODE_ENV === 'development' ? 'http://localhost:8080' : 'https://api.aquarush.com'}/api/v1/storage/files`,
        filePath,
        name: 'file',
        formData: {
          fileType: 'AVATAR',
          isPublic: 'true',
          description: '用户头像'
        },
        header: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'multipart/form-data'
        }
      })

      if (uploadResult.statusCode !== 200) {
        throw new Error(`上传失败，状态码: ${uploadResult.statusCode}`)
      }

      const response = JSON.parse(uploadResult.data)

      if (!response.success) {
        throw new Error(response.message || '上传失败')
      }

      const avatarUrl = response.data.url
      this.setState({ previewUrl: avatarUrl })

      // 通知父组件
      this.props.onAvatarChange(avatarUrl)

      this.showToast('头像上传成功', 'success')

    } catch (error) {
      console.error('Avatar upload failed:', error)

      // 恢复原始头像
      this.setState({ previewUrl: this.props.currentAvatar || '' })

      const errorMsg = error instanceof Error ? error.message : '上传失败'
      this.showToast(errorMsg, 'error')
      this.props.onError?.(errorMsg)

      throw error
    } finally {
      this.setState({ uploading: false })
    }
  }

  render() {
    const { disabled, size } = this.props
    const { uploading, previewUrl, showToast, toastText, toastType } = this.state

    const avatarSize = size === 'large' ? 'large' : size === 'small' ? 'small' : 'normal'

    return (
      <View className='avatar-upload'>
        <View
          className={`avatar-container ${disabled ? 'disabled' : ''} ${uploading ? 'uploading' : ''}`}
          onClick={this.handleAvatarSelect}
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
            支持 JPG、PNG、GIF 格式，文件大小不超过 5MB
          </Text>
        </View>

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
