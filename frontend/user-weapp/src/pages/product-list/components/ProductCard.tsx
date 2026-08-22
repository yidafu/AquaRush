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
 * 商品网格卡(2 列):图片 + 缺货遮罩 + 名称(2 行) + 规格 +
 * 促销价展示(R-MKT-02 单价链)+ 已售数量
 */
const ProductCard: React.FC<ProductCardProps> = ({ product, onClick }) => {
  const { price, tag } = getDisplayPrice(product)
  const outOfStock = product.stock <= 0

  return (
    <View
      key={product.id}
      className='product-card'
      onClick={onClick}
    >
      <View className='product-img-wrap'>
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
      <View className='product-info'>
        <Text className='product-name' numberOfLines={2}>{product.name}</Text>
        <Text className='product-spec' numberOfLines={1}>{product.subtitle || product.specification}</Text>
        <View className='product-bottom'>
          <View className='product-price-wrap'>
            <Text className='product-price'>{displayCents(price)}</Text>
            {tag && <Text className='product-price-tag'>{tag}</Text>}
          </View>
          {product.salesVolume > 0 && (
            <Text className='product-sales'>已售{product.salesVolume}</Text>
          )}
        </View>
      </View>
    </View>
  )
}

export default ProductCard
