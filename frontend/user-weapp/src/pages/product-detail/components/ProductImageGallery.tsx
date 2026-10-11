import React from 'react'
import { View, Image, Swiper, SwiperItem, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { COLORS } from '@aquarush/common/theme'
import { tokens } from '@aquarush/common/theme/tokens.stylex'

// 原 tailwind 工具类(web 375 基准)→ StyleX,长度 ×2 转 rpx(750 基准)。
// gray-50 #F9FAFB、shadow-sm 为本组件局部色值,未入共享 token。
const styles = stylex.create({
  section: {
    paddingTop: '16rpx',
    backgroundColor: tokens.card,
  },
  swiper: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    marginBottom: '16rpx',
    overflow: 'hidden',
    boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
    height: '840rpx',
    borderRadius: '24rpx',
    backgroundColor: '#F9FAFB',
    '@media (min-width: 768px)': {
      height: '920rpx',
      borderRadius: '48rpx',
    },
  },
  slide: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    height: '100%',
  },
  overlay: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    left: 0,
    right: 0,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: '8rpx',
    backgroundColor: '#F9FAFB',
  },
  overlayCol: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    left: 0,
    right: 0,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: '8rpx',
    backgroundColor: '#F9FAFB',
  },
  overlayText: {
    fontSize: '32rpx',
    color: tokens.textWeak,
  },
  overlayTextGap: {
    marginBottom: '8rpx',
    fontSize: '32rpx',
    color: tokens.textWeak,
  },
  image: {
    width: '100%',
    height: '100%',
    borderRadius: '8rpx',
    transitionProperty: 'transform',
    transitionDuration: '0.3s',
    cursor: 'pointer',
    ':hover': {
      transform: 'scale(1.05)',
    },
  },
})

interface ProductImageGalleryProps {
  images: string[]
  /** 大图预览回调(嵌入只读场景不传,图片不可点击) */
  onImagePress?: (index: number) => void
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
    <View {...stylex.props(styles.section)}>
      <Swiper
        {...stylex.props(styles.swiper)}
        indicatorDots
        autoplay
        interval={3000}
        duration={500}
        indicatorColor='rgba(255, 255, 255, 0.5)'
        indicatorActiveColor={COLORS.brand}
        circular
      >
        {images.map((image, index) => (
          <SwiperItem key={index}>
            <View {...stylex.props(styles.slide)}>
              {imageLoadingStates[index] && !imagesWithError[index] && (
                <View {...stylex.props(styles.overlay)}>
                  <Text {...stylex.props(styles.overlayText)}>加载中...</Text>
                </View>
              )}
              {imagesWithError[index] ? (
                <View {...stylex.props(styles.overlayCol)}>
                  <Text {...stylex.props(styles.overlayTextGap)}>图片加载失败</Text>
                </View>
              ) : (
                <Image
                  src={image}
                  mode='aspectFit'
                  {...stylex.props(styles.image)}
                  onLoad={() => onImageLoad(index)}
                  onError={() => onImageError(index)}
                  onClick={onImagePress ? () => onImagePress(index) : undefined}
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
