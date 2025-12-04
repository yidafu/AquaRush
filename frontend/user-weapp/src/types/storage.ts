/**
 * 文件存储相关类型定义
 */

export enum FileType {
  IMAGE = 'IMAGE',
  VIDEO = 'VIDEO',
  AUDIO = 'AUDIO',
  DOCUMENT = 'DOCUMENT',
  SPREADSHEET = 'SPREADSHEET',
  PRESENTATION = 'PRESENTATION',
  FRONTEND = 'FRONTEND',
  ARCHIVE = 'ARCHIVE',
  EXECUTABLE = 'EXECUTABLE',
  BACKUP = 'BACKUP',
  OTHER = 'OTHER'
}

export interface FileMetadataResponse {
  success: boolean
  data: FileMetadata
  message?: string
}

export interface FileMetadata {
  id: number
  fileName: string
  fileType: string
  fileSize: number
  mimeType: string
  extension: string
  createdAt: string
  updatedAt: string
  isPublic: boolean
  description: string
  fileUrl: string
  fileSizeFormatted: string
}

export interface UploadOptions {
  fileType?: FileType
  isPublic?: boolean
  description?: string
  // 文件大小限制（字节）
  maxSize?: number
  // 允许的文件扩展名
  allowedExtensions?: string[]
}

export interface ValidationOptions {
  maxSize: number
  allowedExtensions: string[]
}

export interface UploadError {
  code: 'FILE_TOO_LARGE' | 'INVALID_FORMAT' | 'UPLOAD_FAILED' | 'NETWORK_ERROR' | 'AUTH_ERROR'
  message: string
  details?: any
}

// 默认验证配置
export const DEFAULT_VALIDATION: ValidationOptions = {
  maxSize: 5 * 1024 * 1024, // 5MB
  allowedExtensions: ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']
}

// 头像上传配置
export const AVATAR_UPLOAD_CONFIG: UploadOptions = {
  fileType: FileType.IMAGE,
  isPublic: true,
  description: '用户头像',
  maxSize: 5 * 1024 * 1024, // 5MB
  allowedExtensions: ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']
}

// 商品图片上传配置
export const PRODUCT_UPLOAD_CONFIG: UploadOptions = {
  fileType: FileType.IMAGE,
  isPublic: true,
  description: '商品图片',
  maxSize: 10 * 1024 * 1024, // 10MB
  allowedExtensions: ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']
}

// 配送照片上传配置
export const DELIVERY_UPLOAD_CONFIG: UploadOptions = {
  fileType: FileType.IMAGE,
  isPublic: true,
  description: '配送照片',
  maxSize: 10 * 1024 * 1024, // 10MB
  allowedExtensions: ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']
}