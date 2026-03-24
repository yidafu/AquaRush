import React from 'react'
import { View, Text, Textarea } from '@tarojs/components'
import ImageUploader from '../../../components/ImageUploader'

interface DeliveryInfoCardProps {
  photos: string[]
  remark: string
  onPhotosChange: (photos: string[]) => void
  onRemarkChange: (remark: string) => void
  disabled?: boolean
}

export const DeliveryInfoCard: React.FC<DeliveryInfoCardProps> = ({
  photos,
  remark,
  onPhotosChange,
  onRemarkChange,
  disabled = false,
}) => {
  return (
    <View className='card delivery-info-card'>
      <View className='card-header'>
        <Text className='card-title'>配送信息</Text>
      </View>
      <View className='card-content'>
        {/* Photos Section */}
        <View className='photos-section'>
          <ImageUploader
            images={photos}
            onChange={onPhotosChange}
            disabled={disabled}
            label='配送照片'
          />
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
