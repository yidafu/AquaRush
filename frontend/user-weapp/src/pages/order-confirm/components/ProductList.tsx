import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import { displayCents } from '@aquarush/common'
import './ProductList.scss'

/** 商品行(多商品 F-ORD-004:单价以服务端 orderPreview.items 为准,特价/熟客价/新人价划线展示目录价) */
export interface ProductListItem {
  id: string
  name: string
  image: string
  specification?: string
  quantity: number
  /** 成交单价(分,来自服务端计价预览) */
  unitPriceCents: number
  /** 目录价(分,非目录价成交时划线展示) */
  catalogPriceCents?: number | null
  /** 成交单价来源(特价/熟客价/新人价时划线目录价 —— R-MKT-02) */
  priceSource?: string | null
}

interface ProductListProps {
  items: ProductListItem[]
}

/**
 * 商品信息列表(F-ORD-004 多商品下单;单价以服务端计价为准)
 */
const ProductList: React.FC<ProductListProps> = ({ items }) => {
  return (
    <View className='section product-section'>
      <View className='section-header'>
        <Text className='section-title'>商品信息{items.length > 1 ? '（共' + items.length + '种）' : ''}</Text>
      </View>
      <View className='product-list'>
        {items.map(item => {
          const showCatalogPrice =
            item.priceSource != null && item.priceSource !== 'CATALOG_PRICE' &&
            item.catalogPriceCents != null &&
            item.catalogPriceCents > item.unitPriceCents
          return (
            <View key={item.id} className='product-item'>
              <Image
                src={item.image}
                mode='aspectFill'
                className='product-image'
              />
              <View className='product-info'>
                <Text className='product-name'>{item.name}</Text>
                {item.specification && (
                  <Text className='product-spec'>规格：{item.specification}</Text>
                )}
                <View className='product-bottom'>
                  <View className='product-price-wrap'>
                    <Text className='product-price'>{displayCents(item.unitPriceCents)}</Text>
                    {showCatalogPrice && (
                      <Text className='product-catalog-price'>{displayCents(item.catalogPriceCents)}</Text>
                    )}
                  </View>
                  <Text className='product-quantity'>x{item.quantity}</Text>
                </View>
              </View>
            </View>
          )
        })}
      </View>
    </View>
  )
}

export default ProductList
