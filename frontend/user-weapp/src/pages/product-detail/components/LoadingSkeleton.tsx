import React from 'react'
import { View } from '@tarojs/components'

const LoadingSkeleton: React.FC = () => {
  return (
    <View className='min-h-screen bg-gray-50 pb-40 md:max-w-md md:mx-auto md:shadow-xl md:rounded-3xl md:overflow-hidden'>
      {/* 骨架屏图片区域 */}
      <View className='bg-white px-8 pt-10 pb-6 md:px-10 md:pt-12 md:pb-8'>
        <View className='w-full h-105 rounded-2xl bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 animate-shimmer bg-[length:1000px_100%] md:h-115 md:rounded-3xl'></View>
      </View>

      {/* 骨架屏产品信息 */}
      <View className='bg-white py-16 px-8'>
        <View className='mb-8'>
          <View className='mb-4 h-8 bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 animate-shimmer bg-[length:1000px_100%] rounded-lg w-4/5'></View>
          <View className='h-5 bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 animate-shimmer bg-[length:1000px_100%] rounded-lg w-3/5'></View>
        </View>

        <View className='py-6 mb-8 border-b border-gray-100'>
          <View className='h-10 bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 animate-shimmer bg-[length:1000px_100%] rounded-lg w-1/2'></View>
        </View>

        <View className='mb-8'>
          <View className='flex justify-between gap-4 mb-4'>
            <View className='flex-1 h-16 bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 animate-shimmer bg-[length:1000px_100%] rounded-2xl'></View>
            <View className='flex-1 h-16 bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 animate-shimmer bg-[length:1000px_100%] rounded-2xl'></View>
          </View>
          <View className='flex justify-between gap-4 mb-0'>
            <View className='flex-1 h-16 bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 animate-shimmer bg-[length:1000px_100%] rounded-2xl'></View>
            <View className='flex-1 h-16 bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 animate-shimmer bg-[length:1000px_100%] rounded-2xl'></View>
          </View>
        </View>

        <View className='flex justify-center py-5 border-t border-gray-100 gap-15'>
          <View className='h-5 bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 animate-shimmer bg-[length:1000px_100%] rounded-lg w-12'></View>
          <View className='h-5 bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 animate-shimmer bg-[length:1000px_100%] rounded-lg w-12'></View>
        </View>
      </View>
    </View>
  )
}

export default LoadingSkeleton