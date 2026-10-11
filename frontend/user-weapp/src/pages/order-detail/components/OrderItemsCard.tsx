import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import type { CostRow } from '@aquarush/common'
import OrderItemRow from '../../../components/OrderItemRow'
import CostRowList from '../../../components/CostRowList'
import type { OrderItem } from './constants'

interface OrderItemsCardProps {
  items: OrderItem[]
  costRows: CostRow[]
}

/**
 * 商品清单 + 金额明细 —— WeUI .weui-cells 组(设计稿 02-order/order-detail.html:
 * 「商品清单 / 共 N 件」标题 + .weui-media-box 商品行 + 「金额明细」标题 + 费用行,
 * 营销优惠明细来自订单快照 —— R-MKT-02/07/08/09/10,由父组件 buildCostRows 组装)。
 * 商品行与费用明细行渲染分别复用共享 OrderItemRow / CostRowList。
 *
 * 样式:StyleX(原 OrderItemsCard.scss 迁移;750 基准 rpx,weui-* 类保留;
 * 原后代选择器拍平到子元素;原 .weui-cells .order-item 底色与父级
 * .weui-cells 的 var(--weui-BG-2) 同值属冗余,随 scss 删除;
 * items-card 纯锚点无自有样式按死类删除)
 */
const styles = stylex.create({
  titleRow: {
    display: 'flex',
    alignItems: 'baseline',
    justifyContent: 'space-between',
  },
  titleExtra: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-2)',
  },
  itemsEmpty: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-2)',
  },
})
const OrderItemsCard: React.FC<OrderItemsCardProps> = ({ items, costRows }) => {
  const safeItems = Array.isArray(items) ? items : []
  const totalQuantity = safeItems.reduce((sum, item) => sum + (item.quantity || 0), 0)

  return (
    <View>
      <View {...props('weui-cells__title', styles.titleRow)}>
        <Text>商品清单</Text>
        {safeItems.length > 0 && <Text {...stylex.props(styles.titleExtra)}>共 {totalQuantity} 件</Text>}
      </View>
      <View className='weui-cells'>
        {safeItems.length === 0 ? (
          <View className='weui-cell'><Text {...stylex.props(styles.itemsEmpty)}>暂无商品信息</Text></View>
        ) : (
          safeItems.map((item, index) => (
            <OrderItemRow
              key={`${item.id}-${index}`}
              image={item.image}
              name={item.name}
              spec={item.specifications && item.specifications.length > 0 ? item.specifications.join(', ') : undefined}
              priceCents={item.priceCents || 0}
              originalPriceCents={item.hasMarketingPrice && item.originalPriceCents != null ? item.originalPriceCents : null}
              quantity={item.quantity || 0}
            />
          ))
        )}
      </View>

      {costRows.length > 0 && (
        <>
          <View className='weui-cells__title'>金额明细</View>
          <View className='weui-cells'>
            <View className='weui-cell weui-cell_vertical'>
              <CostRowList rows={costRows} />
            </View>
          </View>
        </>
      )}
    </View>
  )
}

export default OrderItemsCard
