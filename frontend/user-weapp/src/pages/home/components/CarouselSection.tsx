import React from 'react'
import { View, Text, Image, Swiper, SwiperItem } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { COLORS } from '@aquarush/common/theme'
import type { BannerVo } from '@aquarush/common'

interface CarouselSectionProps {
  banners: BannerVo[]
  onBannerClick: (linkType: string, linkTarget: string) => void
}

/**
 * 活动轮播(卡片化,无生效条目隐藏)
 * 样式:StyleX(原 CarouselSection.scss 迁移;750 基准 rpx,嵌套后代选择器拍平)
 */
const styles = stylex.create({
  swiper: {
    height: '296rpx',
    borderRadius: '32rpx',
    overflow: 'hidden',
    backgroundColor: tokens.card,
    boxShadow: tokens.shCard,
  },
  swiperItem: {
    height: '100%',
  },
  card: {
    position: 'relative',
    width: '100%',
    height: '100%',
  },
  img: {
    width: '100%',
    height: '100%',
  },
  mask: {
    position: 'absolute',
    left: '0',
    right: '0',
    bottom: '0',
    paddingTop: '16rpx',
    paddingBottom: '16rpx',
    paddingLeft: '32rpx',
    paddingRight: '32rpx',
    backgroundImage: 'linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.45) 100%)',
    borderRadius: '0 0 32rpx 32rpx',
  },
  title: {
    display: 'block',
    fontSize: '30rpx',
    fontWeight: 600,
    color: tokens.onBrand,
  },
  subtitle: {
    display: 'block',
    marginTop: '4rpx',
    fontSize: '24rpx',
    color: 'rgba(255, 255, 255, 0.85)',
  },
})

const CarouselSection: React.FC<CarouselSectionProps> = ({ banners, onBannerClick }) => {
  if (banners.length === 0) {
    return null
  }

  return (
    <View>
      <Swiper
        {...stylex.props(styles.swiper)}
        indicatorColor='rgba(58, 155, 217, 0.25)'
        indicatorActiveColor={COLORS.brand}
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
            {...stylex.props(styles.swiperItem)}
            onClick={() => onBannerClick(item.linkType, item.linkTarget)}
          >
            <View {...stylex.props(styles.card)}>
              <Image src={item.imageUrl} mode='aspectFill' {...stylex.props(styles.img)} />
              {(item.title || item.subtitle) && (
                <View {...stylex.props(styles.mask)}>
                  <Text {...stylex.props(styles.title)}>{item.title}</Text>
                  {item.subtitle && <Text {...stylex.props(styles.subtitle)}>{item.subtitle}</Text>}
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
