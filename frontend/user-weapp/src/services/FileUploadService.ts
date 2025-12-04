import Taro from '@tarojs/taro'
import NetworkManager, { NetworkError } from '../utils/network'
import {
  FileMetadataResponse,
  FileMetadata,
  UploadOptions,
  ValidationOptions,
  UploadError,
  FileType,
  DEFAULT_VALIDATION,
  AVATAR_UPLOAD_CONFIG
} from '../types/storage'

class FileUploadService {
  private networkManager: NetworkManager
  private static instance: FileUploadService

  constructor() {
    this.networkManager = NetworkManager.getInstance()
  }

  static getInstance(): FileUploadService {
    if (!FileUploadService.instance) {
      FileUploadService.instance = new FileUploadService()
    }
    return FileUploadService.instance
  }

  /**
   * 获取上传API的基础URL
   */
  private getUploadBaseUrl(): string {
    const config = this.networkManager.getConfig()
    const baseUrl = config.baseURL.replace('/graphql', '')
    return `${baseUrl}/api/v1/storage/files`
  }

  /**
   * 验证文件
   */
  private validateFile(filePath: string, options: ValidationOptions): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        const fileInfo = Taro.getFileSystemManager().statSync(filePath)

        // 检查文件大小
        if (fileInfo.size > options.maxSize) {
          const error: UploadError = {
            code: 'FILE_TOO_LARGE',
            message: `文件大小不能超过 ${(options.maxSize / 1024 / 1024).toFixed(1)}MB`
          }
          return reject(error)
        }

        // 检查文件扩展名
        const extension = filePath.split('.').pop()?.toLowerCase()
        if (!extension || !options.allowedExtensions.includes(extension)) {
          const error: UploadError = {
            code: 'INVALID_FORMAT',
            message: `不支持的文件格式，请选择 ${options.allowedExtensions.join('、')} 格式的文件`
          }
          return reject(error)
        }

        resolve()
      } catch (error) {
        const uploadError: UploadError = {
          code: 'INVALID_FORMAT',
          message: '文件验证失败，请重试',
          details: error
        }
        reject(uploadError)
      }
    })
  }

  /**
   * 上传文件到存储服务
   */
  async uploadFile(
    filePath: string,
    options: UploadOptions = {}
  ): Promise<FileMetadata> {
    const uploadOptions = {
      fileType: FileType.IMAGE,
      isPublic: true,
      description: '',
      ...options
    }

    // 使用默认验证配置
    const validationOptions: ValidationOptions = {
      maxSize: uploadOptions.maxSize || DEFAULT_VALIDATION.maxSize,
      allowedExtensions: uploadOptions.allowedExtensions || DEFAULT_VALIDATION.allowedExtensions
    }

    try {
      // 验证文件
      await this.validateFile(filePath, validationOptions)

      // 准备上传参数
      const formData: Record<string, string> = {
        fileType: uploadOptions.fileType,
        isPublic: uploadOptions.isPublic.toString(),
        description: uploadOptions.description
      }

      // 上传文件
      const response = await this.networkManager.uploadFile<FileMetadataResponse>({
        url: this.getUploadBaseUrl(),
        filePath,
        name: 'file',
        formData
      })

      // 检查上传结果
      if (!response.success) {
        const error: UploadError = {
          code: 'UPLOAD_FAILED',
          message: response.message || '上传失败'
        }
        throw error
      }

      return response.data

    } catch (error) {
      console.error('File upload failed:', error)

      // 处理网络错误
      if (error instanceof NetworkError) {
        const uploadError: UploadError = {
          code: 'NETWORK_ERROR',
          message: '网络连接失败，请检查网络设置',
          details: error
        }
        throw uploadError
      }

      // 重新抛出上传错误
      if ((error as UploadError).code) {
        throw error
      }

      // 未知错误
      const uploadError: UploadError = {
        code: 'UPLOAD_FAILED',
        message: '上传失败，请重试',
        details: error
      }
      throw uploadError
    }
  }

  /**
   * 上传头像
   */
  async uploadAvatar(filePath: string): Promise<string> {
    try {
      const result = await this.uploadFile(filePath, AVATAR_UPLOAD_CONFIG)
      return result.fileUrl
    } catch (error) {
      console.error('Avatar upload failed:', error)
      throw error
    }
  }

  /**
   * 批量上传文件
   */
  async uploadMultipleFiles(
    filePaths: string[],
    options: UploadOptions = {}
  ): Promise<FileMetadata[]> {
    const uploadPromises = filePaths.map(filePath =>
      this.uploadFile(filePath, options)
    )

    try {
      const results = await Promise.all(uploadPromises)
      return results
    } catch (error) {
      console.error('Multiple files upload failed:', error)
      throw error
    }
  }

  /**
   * 获取文件信息
   */
  async getFileInfo(fileId: number): Promise<FileMetadata> {
    try {
      const response = await this.networkManager.get<FileMetadataResponse>(
        `${this.getUploadBaseUrl()}/${fileId}`
      )

      if (!response.success) {
        throw new Error(response.message || '获取文件信息失败')
      }

      return response.data
    } catch (error) {
      console.error('Get file info failed:', error)
      throw error
    }
  }

  /**
   * 删除文件
   */
  async deleteFile(fileId: number): Promise<void> {
    try {
      const response = await this.networkManager.delete<{success: boolean; message?: string}>(
        `${this.getUploadBaseUrl()}/${fileId}`
      )

      if (!response.success) {
        throw new Error(response.message || '删除文件失败')
      }
    } catch (error) {
      console.error('Delete file failed:', error)
      throw error
    }
  }

  /**
   * 格式化文件大小
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B'

    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))

    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
  }

  /**
   * 检查文件类型是否为图片
   */
  isImageFile(mimeType: string): boolean {
    return mimeType.startsWith('image/')
  }

  /**
   * 检查文件类型是否为视频
   */
  isVideoFile(mimeType: string): boolean {
    return mimeType.startsWith('video/')
  }

  /**
   * 检查文件类型是否为音频
   */
  isAudioFile(mimeType: string): boolean {
    return mimeType.startsWith('audio/')
  }
}

// 导出单例实例
export const fileUploadService = FileUploadService.getInstance()
export default fileUploadService