import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import { displayCents } from '@aquarush/common'
import './ProductList.scss'

interface ProductListProps {
  name: string
  image: string
  specification?: string
  quantity: number
  /** 成交单价(分,来自服务端计价预览 unitPriceCents) */
  unitPriceCents: number
  /** 目录价(分,非目录价成交时划线展示) */
  catalogPriceCents?: number | null
  /** 是否命中熟客价(分组价) */
  hasGroupPrice?: boolean
  /** 成交单价来源(preview.priceSource,特价/熟客价/新人价时划线目录价 —— R-MKT-02) */
  priceSource?: string | null
}

/**
 * 商品信息列表(单价以服务端 orderPreview.unitPriceCents 为准,特价/熟客价/新人价划线展示目录价)
 */
const ProductList: React.FC<ProductListProps> = ({
  name,
  image,
  specification,
  quantity,
  unitPriceCents,
  catalogPriceCents,
  hasGroupPrice,
  priceSource
}) => {
  const showCatalogPrice =
    (!!hasGroupPrice || (priceSource != null && priceSource !== 'CATALOG_PRICE')) &&
    catalogPriceCents != null &&
    catalogPriceCents > unitPriceCents

  return (
    <View className='section product-section'>
      <View className='section-header'>
        <Text className='section-title'>商品信息</Text>
      </View>
      <View className='product-list'>
        <View className='product-item'>
          <Image
            src={image}
            mode='aspectFill'
            className='product-image'
          />
          <View className='product-info'>
            <Text className='product-name'>{name}</Text>
            {specification && (
              <Text className='product-spec'>规格：{specification}</Text>
            )}
            <View className='product-bottom'>
              <View className='product-price-wrap'>
                <Text className='product-price'>{displayCents(unitPriceCents)}</Text>
                {showCatalogPrice && (
                  <Text className='product-catalog-price'>{displayCents(catalogPriceCents)}</Text>
                )}
              </View>
              <Text className='product-quantity'>x{quantity}</Text>
            </View>
          </View>
        </View>
      </View>
    </View>
  )
}

export default ProductList
