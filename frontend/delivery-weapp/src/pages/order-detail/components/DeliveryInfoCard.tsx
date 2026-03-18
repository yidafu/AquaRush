import React from 'react'
import { View, Text, Image, Textarea } from '@tarojs/components'
import Taro from '@tarojs/taro'

interface DeliveryInfoCardProps {
  photos: string[]
  remark: string
  onPhotosChange: (photos: string[]) => void
  onRemarkChange: (remark: string) => void
  disabled?: boolean
}

const MAX_PHOTOS = 9

export const DeliveryInfoCard: React.FC<DeliveryInfoCardProps> = ({
  photos,
  remark,
  onPhotosChange,
  onRemarkChange,
  disabled = false,
}) => {
  // Upload single photo and return URL
  const uploadPhoto = async (filePath: string): Promise<string> => {
    try {
      const uploadResult = await Taro.uploadFile({
        url: `/upload`,
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
    if (photos.length >= MAX_PHOTOS) {
      Taro.showToast({ title: `最多上传${MAX_PHOTOS}张图片`, icon: 'none' })
      return
    }

    try {
      const result = await Taro.chooseImage({
        count: MAX_PHOTOS - photos.length,
        sizeType: ['compressed'],
        sourceType: ['camera', 'album'],
      })

      if (result.errMsg === 'chooseImage:ok' && result.tempFilePaths?.length) {
        Taro.showLoading({ title: '上传中...' })

        const newPhotos: string[] = []
        for (const filePath of result.tempFilePaths) {
          const url = await uploadPhoto(filePath)
          newPhotos.push(url)
        }

        Taro.hideLoading()
        onPhotosChange([...photos, ...newPhotos])
      }
    } catch (error) {
      console.error('选择图片失败:', error)
      Taro.showToast({ title: '选择图片失败', icon: 'none' })
    }
  }

  // Delete photo
  const handleDeletePhoto = (index: number) => {
    const newPhotos = [...photos]
    newPhotos.splice(index, 1)
    onPhotosChange(newPhotos)
  }

  // Preview image
  const handlePreview = (index: number) => {
    Taro.previewImage({
      urls: photos,
      current: photos[index],
    })
  }

  return (
    <View className='card delivery-info-card'>
      <View className='card-header'>
        <Text className='card-title'>配送信息</Text>
      </View>
      <View className='card-content'>
        {/* Photos Section */}
        <View className='photos-section'>
          <Text className='section-label'>配送照片（{photos.length}/{MAX_PHOTOS}）</Text>
          <View className='photos-grid'>
            {photos.map((photo, index) => (
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
            {photos.length < MAX_PHOTOS && !disabled && (
              <View className='photo-add' onClick={handleTakePhoto}>
                <Text className='photo-add-icon'>+</Text>
                <Text className='photo-add-text'>拍照</Text>
              </View>
            )}
          </View>
        </View>

        {/* Remark Section */}
        <View className='remark-section'>
          <Text className='section-label'>备注</Text>
          {disabled ? (
            <Text className='remark-content'>{remark || '无备注'}</Text>
          ) : (
            <Textarea
              className='remark-input'
              value={remark}
              onInput={(e) => onRemarkChange(e.detail.value)}
              placeholder='请输入配送备注（选填）'
              placeholderClass='remark-placeholder'
              maxlength={500}
            />
          )}
        </View>
      </View>
    </View>
  )
}
