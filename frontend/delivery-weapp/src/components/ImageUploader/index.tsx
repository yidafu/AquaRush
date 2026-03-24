import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import Taro from '@tarojs/taro'
import './index.scss'

export interface ImageUploaderProps {
  /** 图片URL数组 */
  images: string[]
  /** 图片变化回调 */
  onChange: (images: string[]) => void
  /** 最大上传数量，默认9 */
  max?: number
  /** 是否禁用编辑 */
  disabled?: boolean
  /** 上传接口地址，默认'/upload' */
  uploadUrl?: string
  /** 图片压缩模式 */
  sizeType?: ('original' | 'compressed')[]
  /** 图片来源 */
  sourceType?: ('album' | 'camera')[]
  /** 自定义类名 */
  className?: string
  /** 区域标题 */
  label?: string
}

const DEFAULT_MAX = 9
const DEFAULT_UPLOAD_URL = '/upload'

export const ImageUploader: React.FC<ImageUploaderProps> = ({
  images,
  onChange,
  max = DEFAULT_MAX,
  disabled = false,
  uploadUrl = DEFAULT_UPLOAD_URL,
  sizeType = ['compressed'],
  sourceType = ['camera', 'album'],
  className = '',
  label = '',
}) => {
  // Upload single photo and return URL
  const uploadPhoto = async (filePath: string): Promise<string> => {
    try {
      const uploadResult = await Taro.uploadFile({
        url: uploadUrl,
        filePath: filePath,
        name: 'file',
      })

      if (uploadResult.statusCode === 200) {
        const data = JSON.parse(uploadResult.data)
        return data.url || filePath
      }
      return filePath
    } catch {
      return filePath
    }
  }

  // Take photo using Taro.chooseImage
  const handleTakePhoto = async () => {
    if (images.length >= max) {
      Taro.showToast({ title: `最多上传${max}张图片`, icon: 'none' })
      return
    }

    try {
      const result = await Taro.chooseImage({
        count: max - images.length,
        sizeType,
        sourceType,
      })

      if (result.errMsg === 'chooseImage:ok' && result.tempFilePaths?.length) {
        Taro.showLoading({ title: '上传中...' })

        const newPhotos: string[] = []
        for (const filePath of result.tempFilePaths) {
          const url = await uploadPhoto(filePath)
          newPhotos.push(url)
        }

        Taro.hideLoading()
        onChange([...images, ...newPhotos])
      }
    } catch (error) {
      console.error('选择图片失败:', error)
      Taro.showToast({ title: '选择图片失败', icon: 'none' })
    }
  }

  // Delete photo
  const handleDeletePhoto = (index: number) => {
    const newPhotos = [...images]
    newPhotos.splice(index, 1)
    onChange(newPhotos)
  }

  // Preview image
  const handlePreview = (index: number) => {
    Taro.previewImage({
      urls: images,
      current: images[index],
    })
  }

  const showLabel = label || (disabled ? '' : `（${images.length}/${max}）`)

  return (
    <View className={`image-uploader ${className}`}>
      {label && (
        <Text className='section-label'>
          {label}
          {showLabel && <Text className='label-count'>{showLabel}</Text>}
        </Text>
      )}
      <View className='photos-grid'>
        {images.map((photo, index) => (
          <View key={index} className='photo-item'>
            <Image
              className='photo-image'
              src={photo}
              mode='aspectFill'
              onClick={() => handlePreview(index)}
            />
            {!disabled && (
              <View
                className='photo-delete'
                onClick={() => handleDeletePhoto(index)}
              >
                <Text className='photo-delete-icon'>×</Text>
              </View>
            )}
          </View>
        ))}
        {images.length < max && !disabled && (
          <View className='photo-add' onClick={handleTakePhoto}>
            <Text className='photo-add-icon'>+</Text>
            <Text className='photo-add-text'>拍照</Text>
          </View>
        )}
      </View>
    </View>
  )
}

export default ImageUploader
