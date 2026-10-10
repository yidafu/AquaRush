import React from 'react'
import { View, Text, Image, Swiper, SwiperItem } from '@tarojs/components'
import type { BannerVo } from '@aquarush/common'

import './CarouselSection.scss'

interface CarouselSectionProps {
  banners: BannerVo[]
  onBannerClick: (linkType: string, linkTarget: string) => void
}

/**
 * 活动轮播(卡片化,无生效条目隐藏)
 */
const CarouselSection: React.FC<CarouselSectionProps> = ({ banners, onBannerClick }) => {
  if (banners.length === 0) {
    return null
  }

  return (
    <View className='carousel-section'>
      <Swiper
        className='carousel-swiper'
        indicatorColor='rgba(58, 155, 217, 0.25)'
        indicatorActiveColor='#3A9BD9'
        indicatorDots
        circular
        autoplay
        interval={3000}
        duration={500}
        onChange={() => undefined}
      >
        {banners.map((item) => (
          <SwiperItem
            key={item.id}
            className='carousel-swiper-item'
            onClick={() => onBannerClick(item.linkType, item.linkTarget)}
          >
            <View className='carousel-card'>
              <Image src={item.imageUrl} mode='aspectFill' className='carousel-img' />
              {(item.title || item.subtitle) && (
                <View className='carousel-mask'>
                  <Text className='carousel-title'>{item.title}</Text>
                  {item.subtitle && <Text className='carousel-subtitle'>{item.subtitle}</Text>}
                </View>
              )}
            </View>
          </SwiperItem>
        ))}
      </Swiper>
    </View>
  )
}

export default CarouselSection
