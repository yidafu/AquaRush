import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import OrderItemRow from '../../../components/OrderItemRow'

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
 * 商品清单(F-ORD-004 多商品下单;单价以服务端计价为准)
 * WeUI .weui-cells 组:标题行「商品清单 / 共 N 件」+ .weui-media-box 商品行(共享 OrderItemRow)。
 *
 * 样式:StyleX(原 ProductList.scss 迁移;750 基准 rpx,weui-* 类保留;
 * 原后代选择器拍平到子元素)
 */
const styles = stylex.create({
  productSection: {
    marginTop: '8rpx',
  },
  titleRow: {
    display: 'flex',
    alignItems: 'baseline',
    justifyContent: 'space-between',
  },
  titleExtra: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-2)',
  },
})
const ProductList: React.FC<ProductListProps> = ({ items }) => {
  const totalQuantity = items.reduce((sum, item) => sum + item.quantity, 0)

  return (
    <View {...stylex.props(styles.productSection)}>
      <View {...props('weui-cells__title', styles.titleRow)}>
        <Text>商品清单</Text>
        <Text {...stylex.props(styles.titleExtra)}>共 {totalQuantity} 件</Text>
      </View>
      <View className='weui-cells'>
        {items.map(item => {
          // 非目录价成交且目录价更高时划线展示(R-MKT-02)
          const showCatalogPrice =
            item.priceSource != null && item.priceSource !== 'CATALOG_PRICE' &&
            item.catalogPriceCents != null &&
            item.catalogPriceCents > item.unitPriceCents
          return (
            <OrderItemRow
              key={item.id}
              image={item.image}
              name={item.name}
              spec={item.specification}
              priceCents={item.unitPriceCents}
              originalPriceCents={showCatalogPrice ? item.catalogPriceCents : null}
              quantity={item.quantity}
            />
          )
        })}
      </View>
    </View>
  )
}

export default ProductList
