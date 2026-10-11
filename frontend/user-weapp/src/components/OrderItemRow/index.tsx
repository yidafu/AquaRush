import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import { Money } from '@aquarush/common'

import './index.scss'

/**
 * 商品行(订单/购物车/收藏/确认订单通用)—— WeUI 2.x 视觉(.weui-media-box 行:
 * 96 缩略图 + 名称 + 规格 + .weui-price 成交价(命中营销价划线目录价,AC-ORD-003-09 ——
 * R-MKT-01/02)+ ×N,设计稿 02-order/order-detail.html 商品清单口径)。
 *
 * 收敛来源:order-detail OrderItemsCard / order-list OrderCardItem / order-confirm ProductList /
 * cart / favorites 五处近重复实现。外层布局(卡片容器/等N件徽标等)
 * 由调用方保留,badge 可传徽标内容(渲染在图片左上角)。
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
    <View className='order-item weui-media-box'>
      {badge && <Text className='item-badge weui-tag weui-tag--assign'>{badge}</Text>}
      <View className='weui-media-box__hd'>
        <Image
          src={image || '/assets/default-product.png'}
          mode='aspectFill'
          className='weui-media-box__thumb weui-media-box__thumb--sm item-image'
        />
      </View>
      <View className='weui-media-box__bd'>
        <Text className='weui-media-box__title'>{name || '商品名称'}</Text>
        {spec && <Text className='weui-media-box__desc'>规格:{spec}</Text>}
        <View className='item-bottom'>
          <View className='item-price-row'>
            <Text className='weui-price weui-price--md'>
              <Money cents={priceCents ?? 0} />
            </Text>
            {originalPriceCents != null && (
              <Text className='weui-price--origin'>
                <Money cents={originalPriceCents} />
              </Text>
            )}
          </View>
          <Text className='item-quantity'>× {quantity ?? 0}</Text>
        </View>
      </View>
    </View>
  )
}

export default OrderItemRow
