import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { displayCents } from '@aquarush/common'
import { getDisplayPrice } from '../../../utils/productPrice'
import type { ProductListItem } from './constants'

interface ProductCardProps {
  product: ProductListItem
  onClick: () => void
}

// 商品卡(product-list.html .mp-prod:白底圆角卡,封面 1:1,名称 2 行截断;
// 位于 .mp-grid2 内,宽度由网格决定;cover 用占位 hack 铺满图片)
const styles = stylex.create({
  productImg: {
    position: 'absolute',
    left: 0,
    top: 0,
    width: '100%',
    height: '100%',
  },
  productSoldOut: {
    position: 'absolute',
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255, 255, 255, 0.55)',
  },
  productSoldOutText: {
    paddingTop: '6rpx',
    paddingRight: '16rpx',
    paddingBottom: '6rpx',
    paddingLeft: '16rpx',
    borderRadius: 'var(--weui-radius)',
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
    color: tokens.onBrand,
    fontSize: '24rpx',
  },
  productPriceCol: {
    minWidth: 0,
  },
  productPriceRow: {
    display: 'flex',
    alignItems: 'baseline',
    columnGap: '8rpx',
    minWidth: 0,
  },
  productTagRow: {
    marginTop: '8rpx',
    display: 'flex',
    alignItems: 'center',
    columnGap: '8rpx',
  },
  productSales: {
    fontSize: '22rpx',
    color: 'var(--weui-FG-2)',
  },
})

/**
 * 商品网格卡(product-list.html .mp-prod,2 列):
 * 封面 1:1 + 缺货遮罩 + 名称(2 行) + 规格 +
 * 促销价展示(R-MKT-02 单价链,.weui-price--md + 划线目录价)+ 已售数量
 */
const ProductCard: React.FC<ProductCardProps> = ({ product, onClick }) => {
  const { price, tag } = getDisplayPrice(product)
  const outOfStock = product.stock <= 0
  const hasDiscount = tag !== '' && price !== product.priceCents
  const [intPart, decPart = '00'] = displayCents(price).split('.')

  return (
    <View
      className='mp-prod'
      trackTarget='product-list-item'
      trackElementType='card'
      trackElementText={product.name}
      onClick={onClick}
    >
      <View className='mp-prod__cover'>
        <Image
          src={product.coverImageUrl}
          mode='aspectFill'
          {...stylex.props(styles.productImg)}
          lazyLoad
        />
        {outOfStock && (
          <View {...stylex.props(styles.productSoldOut)}>
            <Text {...stylex.props(styles.productSoldOutText)}>暂时缺货</Text>
          </View>
        )}
      </View>
      <View className='mp-prod__body'>
        <View className='mp-prod__name'>{product.name}</View>
        <View className='mp-prod__desc'>{product.subtitle || product.specification}</View>
        <View className='mp-prod__foot'>
          <View {...stylex.props(styles.productPriceCol)}>
            <View {...stylex.props(styles.productPriceRow)}>
              <View className='weui-price weui-price--md'>
                <Text className='weui-price__symbol'>¥</Text>
                <Text className='weui-price__int'>{intPart}</Text>
                <Text className='weui-price__dec'>.{decPart}</Text>
              </View>
              {hasDiscount && (
                <Text className='weui-price--origin'>¥{displayCents(product.priceCents)}</Text>
              )}
            </View>
            <View {...stylex.props(styles.productTagRow)}>
              {tag !== '' && <Text className='weui-tag weui-tag--brand'>{tag}</Text>}
              {product.salesVolume > 0 && (
                <Text {...stylex.props(styles.productSales)}>已售 {product.salesVolume}</Text>
              )}
            </View>
          </View>
        </View>
      </View>
    </View>
  )
}

export default ProductCard
