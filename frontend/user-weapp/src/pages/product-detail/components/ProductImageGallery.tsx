import React from 'react'
import { View, Image, Swiper, SwiperItem, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'

interface ProductImageGalleryProps {
  images: string[]
  onImagePress: (index: number) => void
  imagesLoaded: boolean[]
  imagesWithError: boolean[]
  imageLoadingStates: boolean[]
  onImageLoad: (index: number) => void
  onImageError: (index: number) => void
}

const ProductImageGallery: React.FC<ProductImageGalleryProps> = ({
  images,
  onImagePress,
  imagesLoaded,
  imagesWithError,
  imageLoadingStates,
  onImageLoad,
  onImageError
}) => {
  return (
    <View className='px-0 pt-2 bg-white'>
      <Swiper
        className='flex items-center justify-center w-full mb-2 overflow-hidden shadow-sm h-105 rounded-xl bg-gray-50 md:h-115 md:rounded-3xl'
        indicatorDots
        autoplay
        interval={3000}
        duration={500}
        indicatorColor='rgba(255, 255, 255, 0.5)'
        indicatorActiveColor='#667eea'
        circular
      >
        {images.map((image, index) => (
          <SwiperItem key={index}>
            <View className='relative flex items-center justify-center w-full h-full'>
              {imageLoadingStates[index] && !imagesWithError[index] && (
                <View className='absolute top-0 bottom-0 left-0 right-0 flex items-center justify-center rounded bg-gray-50'>
                  <Text className='text-base text-gray-400'>加载中...</Text>
                </View>
              )}
              {imagesWithError[index] ? (
                <View className='absolute top-0 bottom-0 left-0 right-0 flex flex-col items-center justify-center rounded bg-gray-50'>
                  <Text className='mb-1 text-base text-gray-400'>图片加载失败</Text>
                </View>
              ) : (
                <Image
                  src={image}
                  mode='aspectFit'
                  className='w-full h-full transition-transform duration-300 rounded cursor-pointer hover:scale-105'
                  onLoad={() => onImageLoad(index)}
                  onError={() => onImageError(index)}
                  onClick={() => onImagePress(index)}
                />
              )}
            </View>
          </SwiperItem>
        ))}
      </Swiper>
    </View>
  )
}

export default ProductImageGallery
