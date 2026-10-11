import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import { displayCents } from '@aquarush/common'
import { getDisplayPrice } from '../../../utils/productPrice'
import type { ProductListItem } from './constants'

import './ProductCard.scss'

interface ProductCardProps {
  product: ProductListItem
  onClick: () => void
}

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
      className='mp-prod product-card'
      trackTarget='product-list-item'
      trackElementType='card'
      trackElementText={product.name}
      onClick={onClick}
    >
      <View className='mp-prod__cover product-cover'>
        <Image
          src={product.coverImageUrl}
          mode='aspectFill'
          className='product-img'
          lazyLoad
        />
        {outOfStock && (
          <View className='product-sold-out'>
            <Text className='product-sold-out-text'>暂时缺货</Text>
          </View>
        )}
      </View>
      <View className='mp-prod__body'>
        <View className='mp-prod__name'>{product.name}</View>
        <View className='mp-prod__desc'>{product.subtitle || product.specification}</View>
        <View className='mp-prod__foot'>
          <View className='product-price-col'>
            <View className='product-price-row'>
              <View className='weui-price weui-price--md'>
                <Text className='weui-price__symbol'>¥</Text>
                <Text className='weui-price__int'>{intPart}</Text>
                <Text className='weui-price__dec'>.{decPart}</Text>
              </View>
              {hasDiscount && (
                <Text className='weui-price--origin'>¥{displayCents(product.priceCents)}</Text>
              )}
            </View>
            <View className='product-tag-row'>
              {tag !== '' && <Text className='weui-tag weui-tag--brand'>{tag}</Text>}
              {product.salesVolume > 0 && (
                <Text className='product-sales'>已售 {product.salesVolume}</Text>
              )}
            </View>
          </View>
        </View>
      </View>
    </View>
  )
}

export default ProductCard
