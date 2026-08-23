import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import { Money } from '@aquarush/common'

import './index.scss'

/**
 * 商品行(订单/购物车/收藏/确认订单通用):
 * 图片 + 名称 + 规格 + 成交价(命中营销价时划线目录价,AC-ORD-003-09 —— R-MKT-01/02) + 数量
 *
 * 收敛来源:order-detail OrderItemsCard / order-list OrderCardItem / order-confirm ProductList /
 * cart / favorites 五处近重复实现(类名相同,SCSS 漂移)。外层布局(卡片容器/等N件徽标等)
 * 由调用方保留,badge 可传徽标内容(渲染在图片右上)。
 */
interface OrderItemRowProps {
  image: string
  name: string
  spec?: string | null
  /** 成交单价(分) */
  priceCents?: number | null
  /** 目录价(分,命中营销价时划线展示) */
  originalPriceCents?: number | null
  quantity?: number
  /** 附加徽标(如「等N件」) */
  badge?: React.ReactNode
}

const OrderItemRow: React.FC<OrderItemRowProps> = ({
  image,
  name,
  spec,
  priceCents,
  originalPriceCents,
  quantity,
  badge
}) => {
  return (
    <View className='order-item'>
      {badge && <Text className='item-badge'>{badge}</Text>}
      <Image
        src={image || '/assets/default-product.png'}
        mode='aspectFill'
        className='item-image'
      />
      <View className='item-info'>
        <Text className='item-name' numberOfLines={1}>{name || '商品名称'}</Text>
        {spec && <Text className='item-spec' numberOfLines={1}>规格：{spec}</Text>}
        <View className='item-bottom'>
          <View className='item-price-row'>
            <Text className='item-price'><Money cents={priceCents ?? 0} /></Text>
            {originalPriceCents != null && (
              <Text className='item-original-price'><Money cents={originalPriceCents} /></Text>
            )}
          </View>
          <Text className='item-quantity'>x{quantity ?? 0}</Text>
        </View>
      </View>
    </View>
  )
}

export default OrderItemRow
