import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import { AtButton } from 'taro-ui'
import { displayCents } from '@aquarush/common'
import { getDisplayPrice } from '../../../utils/productPrice'
import type { HomeProduct } from '../../../services/HomeService'

import "taro-ui/dist/style/components/button.scss"
import './HomeProductCard.scss'

interface HomeProductCardProps {
  product: HomeProduct
  /** true = 最常买横滑卡(带「立即订水」CTA);false = 更多商品单列卡 */
  action?: boolean
  onClick: () => void
  onQuickOrder: () => void
}

/**
 * 商品卡(最常买 280px 横滑 / 更多商品单列共用):
 * 图片 + 名称 + 规格 + 促销价展示(R-MKT-02 单价链)+ 「立即订水」CTA(强调橙 #FF5436)
 */
const HomeProductCard: React.FC<HomeProductCardProps> = ({ product, action, onClick, onQuickOrder }) => {
  const { price, tag } = getDisplayPrice(product)

  return (
    <View
      key={product.id}
      className={action ? 'hot-card' : 'more-card'}
      onClick={onClick}
    >
      <View className='product-img-wrap'>
        <Image
          src={product.coverImageUrl}
          mode='aspectFill'
          className='product-img'
          lazyLoad
        />
      </View>
      <View className='product-info'>
        <Text className='product-name' numberOfLines={1}>{product.name}</Text>
        <Text className='product-spec' numberOfLines={1}>{product.subtitle || product.specification}</Text>
        <View className='product-bottom'>
          <View className='product-price-wrap'>
            <Text className='product-price'>{displayCents(price)}</Text>
            {tag && <Text className='product-price-tag'>{tag}</Text>}
          </View>
          {action && (
            <AtButton
              size='small'
              className='quick-order-btn'
              onClick={(e: any) => {
                e.stopPropagation?.()
                onQuickOrder()
              }}
            >
              立即订水
            </AtButton>
          )}
        </View>
      </View>
    </View>
  )
}

export default HomeProductCard
