import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { Money } from '@aquarush/common'
import type { OrderDetail, ReviewCheckState } from './constants'

interface OrderActionsProps {
  order: OrderDetail
  reviewInfo: ReviewCheckState | null
  onPayment: () => void
  onCancel: () => void
  onReview: () => void
  onReorder: () => void
  onTrack: () => void
  onUrge: () => void
  onContinueShopping: () => void
}

/**
 * 底部操作栏 —— WeUI .mp-footbar(信息列 实付 + .weui-price--lg + 按状态按钮组,
 * 设计稿 02-order/order-detail.html;保留全部 track* 埋点):
 * 待付款 = 取消 + 立即支付;配送中 = 催单 + 查看配送;
 * 已完成 = 评价/再次购买/继续购物(送达即完成,无确认收货 —— PRD R-ORD-11 简化);
 * 待配送/已取消 = 再次购买
 *
 * 样式:StyleX(原 OrderActions.scss 迁移;750 基准 rpx,weui / mp-footbar 类保留;
 * 原后代选择器拍平到子元素;原双写 padding-bottom(constant + env)收敛为 env)
 */
const styles = stylex.create({
  orderFootbar: {
    position: 'fixed',
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 100,
    paddingBottom: 'calc(16rpx + env(safe-area-inset-bottom))',
  },
  footbarBtn: {
    flexGrow: 0,
    flexShrink: 1,
    flexBasis: 'auto',
    minWidth: 0,
    paddingTop: 0,
    paddingBottom: 0,
    paddingLeft: '24rpx',
    paddingRight: '24rpx',
  },
  footbarFlex1: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: '0%',
  },
})
const OrderActions: React.FC<OrderActionsProps> = ({
  order,
  reviewInfo,
  onPayment,
  onCancel,
  onReview,
  onReorder,
  onTrack,
  onUrge,
  onContinueShopping
}) => {
  return (
    <View {...props('mp-footbar hair-t', styles.orderFootbar)}>
      <View className='mp-footbar__info'>
        <View className='mp-footbar__lbl'>实付</View>
        <Text className='weui-price weui-price--lg'>
          <Money cents={order.finalAmount} />
        </Text>
      </View>

      {order.status === 'pending_payment' && (
        <>
          <View
            {...props('weui-btn weui-btn--no-min weui-btn_plain-default', styles.footbarBtn)}
            trackTarget='order-cancel'
            trackElementType='button'
            trackElementText='取消订单'
            trackProps={{ orderId: order.id }}
            onClick={onCancel}
          >
            取消订单
          </View>
          <View
            {...props('weui-btn weui-btn--no-min weui-btn_primary', styles.footbarBtn)}
            trackTarget='order-pay'
            trackElementType='button'
            trackElementText='立即支付'
            trackProps={{ orderId: order.id }}
            onClick={onPayment}
          >
            立即支付
          </View>
        </>
      )}

      {order.status === 'delivering' && (
        <>
          <View
            {...props('weui-btn weui-btn--no-min weui-btn_plain-default', styles.footbarBtn)}
            trackTarget='order-urge'
            trackElementType='button'
            trackElementText='催单'
            trackProps={{ orderId: order.id }}
            onClick={onUrge}
          >
            催单
          </View>
          <View
            {...props('weui-btn weui-btn--no-min weui-btn_primary', styles.footbarBtn)}
            onClick={onTrack}
          >
            查看配送
          </View>
        </>
      )}

      {order.status === 'completed' && (
        <>
          <View
            {...props('weui-btn weui-btn--no-min weui-btn_primary', styles.footbarBtn)}
            trackTarget='order-review'
            trackElementType='button'
            trackElementText={reviewInfo?.hasReviewed ? '查看评价' : '评价'}
            trackProps={{ orderId: order.id }}
            onClick={onReview}
          >
            {reviewInfo?.hasReviewed ? '查看评价' : '评价'}
          </View>
          <View
            {...props('weui-btn weui-btn--no-min weui-btn_plain-default', styles.footbarBtn)}
            trackTarget='order-reorder'
            trackElementType='button'
            trackElementText='再次购买'
            trackProps={{ orderId: order.id }}
            onClick={onReorder}
          >
            再次购买
          </View>
          <View
            {...props('weui-btn weui-btn--no-min weui-btn_plain-default', styles.footbarBtn)}
            onClick={onContinueShopping}
          >
            继续购物
          </View>
        </>
      )}

      {(order.status === 'pending_delivery' || order.status === 'cancelled') && (
        <View
          {...props('weui-btn weui-btn--no-min weui-btn_plain-primary', styles.footbarBtn, styles.footbarFlex1)}
          onClick={onReorder}
        >
          再次购买
        </View>
      )}
    </View>
  )
}

export default OrderActions
