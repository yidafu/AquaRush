import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtButton } from 'taro-ui'
import Taro from '@tarojs/taro'
import "taro-ui/dist/style/components/button.scss"

interface ActionButtonsProps {
  isFavorite: boolean
  onToggleFavorite: () => void
  onAddToCart: () => void
  onBuyNow: () => void
  onContactService: () => void
  onShare: () => void
  stock: number
  status: string
}

const ActionButtons: React.FC<ActionButtonsProps> = ({
  isFavorite,
  onToggleFavorite,
  onAddToCart,
  onBuyNow,
  onContactService,
  onShare,
  stock,
  status
}) => {
  return (
    <View className='fixed bottom-0 left-0 right-0 z-50 p-2 bg-white border-t border-gray-200 shadow-lg animate-slide-in-left'>
      <View className='flex items-center gap-2'>
        {/* 收藏按钮 */}
        <View className='flex-shrink-0'>
          <View
            className={`w-10 h-10 flex items-center justify-center rounded-full bg-gray-50 transition-all duration-300 cursor-pointer ${isFavorite ? 'bg-gradient-to-br from-red-500 to-red-400 transform scale-105 shadow-lg' : ''}`}
            onClick={onToggleFavorite}
          >
            <Text className={`text-xl transition-colors duration-300 ${isFavorite ? 'text-white' : 'text-gray-400'}`}>{isFavorite ? '♥' : '♡'}</Text>
          </View>
        </View>
        {/* 客服按钮 */}
        <View
          className='flex flex-col items-center justify-center w-10 h-10 transition-all duration-300 rounded cursor-pointer bg-gray-50 active:bg-gray-200 active:scale-95'
          onClick={onContactService}
        >
          <Text className='mb-1 text-base'>📞</Text>
          {/* <Text className='text-sm font-medium text-gray-600'>客服</Text> */}
        </View>

        {/* 主要操作按钮 */}
        {status === 'ONLINE' && (
          <View className='flex-1 h-10'>
              {/* 立即购买按钮 */}
              <AtButton
                type='primary'
                size='small'
                className='h-10'
                customStyle={{
                  backgroundColor: stock <= 0 ? '#f5f5f5' : '#ff6700',
                  borderColor: stock <= 0 ? '#f5f5f5' : '#ff6700',
                  color: stock <= 0 ? '#999' : '#ffffff'
                }}
                onClick={onBuyNow}
                disabled={stock <= 0}
              >
                {stock <= 0 ? '暂时缺货' : '立即购买'}
              </AtButton>
          </View>
        )}

      </View>
    </View>
  )
}

export default ActionButtons
