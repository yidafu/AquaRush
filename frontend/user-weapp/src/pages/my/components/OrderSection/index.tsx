import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import CustomIcon from '@/components/CustomIcon'

interface OrderStats {
  pendingPayment: number
  pendingDelivery: number
  delivering: number
  completed: number
  afterSales: number
}

interface OrderSectionProps {
  orderStats: OrderStats
  onOrderNavigation: (type: string) => void
}

/**
 * 「我的订单」(my.html:.weui-panel + .weui-grids 五列):
 * 全部/待付款/待配送/配送中/已完成,.weui-badge 角标(>99 显示 99+)。
 * 样式:StyleX(原同目录 scss 迁移;750 基准 rpx,weui 平台变量保留字符串引用)
 */
const styles = stylex.create({
  orderGrids: {
    gridTemplateColumns: 'repeat(5, 1fr)',
  },
  orderPanelLink: {
    color: 'var(--weui-LINK)',
  },
  // 角标挂图标右上角(.weui-badge 底色语义红由 weui.scss 提供)
  orderGridIcon: {
    position: 'relative',
  },
  orderGridBadge: {
    position: 'absolute',
    top: '-12rpx',
    left: '28rpx',
  },
})
const OrderSection: React.FC<OrderSectionProps> = ({ orderStats, onOrderNavigation }) => {
  const orderItems = [
    {
      icon: '/assets/icons/order/all',
      label: '全部',
      count: 0,
      type: 'all'
    },
    {
      icon: '/assets/icons/order/credit-card',
      label: '待付款',
      count: orderStats.pendingPayment,
      type: 'pendingPay'
    },
    {
      icon: '/assets/icons/order/shopping-bag',
      label: '待配送',
      count: orderStats.pendingDelivery,
      type: 'pendingDispatch'
    },
    {
      icon: '/assets/icons/order/truck',
      label: '配送中',
      count: orderStats.delivering,
      type: 'delivering'
    },
    {
      icon: '/assets/icons/order/check-circle',
      label: '已完成',
      count: orderStats.completed,
      type: 'completed'
    }
  ]

  return (
    <>
      <View className='weui-cells__title'>我的订单</View>
      <View className='weui-panel'>
        <View className='weui-panel__hd'>
          <View />
          <Text
            {...stylex.props(styles.orderPanelLink)}
            onClick={() => onOrderNavigation('all')}
          >
            查看全部
          </Text>
        </View>
        <View {...props('weui-grids', styles.orderGrids)}>
          {orderItems.map((item) => (
            <View
              key={item.type}
              className='weui-grid'
              onClick={() => onOrderNavigation(item.type)}
            >
              <View {...props('weui-grid__icon', styles.orderGridIcon)}>
                <CustomIcon value={item.icon} size={26} />
                {item.count > 0 && (
                  <View {...props('weui-badge', styles.orderGridBadge)}>
                    <Text>{item.count > 99 ? '99+' : item.count}</Text>
                  </View>
                )}
              </View>
              <Text className='weui-grid__label'>{item.label}</Text>
            </View>
          ))}
        </View>
      </View>
    </>
  )
}

export default OrderSection
