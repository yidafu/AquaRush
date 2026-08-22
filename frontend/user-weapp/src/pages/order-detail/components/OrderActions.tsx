import React from 'react'
import { View } from '@tarojs/components'
import { AtButton } from 'taro-ui'
import type { OrderDetail, ReviewCheckState } from './constants'

import "taro-ui/dist/style/components/button.scss"
import './OrderActions.scss'

interface OrderActionsProps {
  order: OrderDetail
  reviewInfo: ReviewCheckState | null
  onPayment: () => void
  onCancel: () => void
  onReview: () => void
  onReorder: () => void
  onTrack: () => void
  onContinueShopping: () => void
}

/**
 * 底部操作栏(按订单状态渲染对应按钮组,保留全部 track* 埋点):
 * 待付款 = 取消 + 立即支付;配送中 = 查看配送;已完成 = 评价/再次购买/继续购物;
 * 待配送/已取消 = 再次购买
 */
const OrderActions: React.FC<OrderActionsProps> = ({
  order,
  reviewInfo,
  onPayment,
  onCancel,
  onReview,
  onReorder,
  onTrack,
  onContinueShopping
}) => {
  return (
    <View className='bottom-actions shadow-action'>
      {order.status === 'pending_payment' && (
        <>
          <AtButton
            trackTarget="order-cancel"
            trackElementType="button"
            trackElementText="取消订单"
            trackProps={{ orderId: order.id }}
            onClick={onCancel}
            type='secondary'
            size='normal'
            className='action-button'
          >
            取消订单
          </AtButton>
          <AtButton
            trackTarget="order-pay"
            trackElementType="button"
            trackElementText="立即支付"
            trackProps={{ orderId: order.id }}
            onClick={onPayment}
            type='primary'
            size='normal'
            className='action-button primary'
          >
            立即支付
          </AtButton>
        </>
      )}

      {order.status === 'delivering' && (
        <AtButton
          type='secondary'
          size='normal'
          onClick={onTrack}
          className='action-button'
        >
          查看配送
        </AtButton>
      )}

      {order.status === 'completed' && (
        <>
          <AtButton
            trackTarget="order-review"
            trackElementType="button"
            trackElementText={reviewInfo?.hasReviewed ? '查看评价' : '评价'}
            trackProps={{ orderId: order.id }}
            onClick={onReview}
            type='secondary'
            size='normal'
            className='action-button'
          >
            {reviewInfo?.hasReviewed ? '查看评价' : '评价'}
          </AtButton>
          <AtButton
            trackTarget="order-reorder"
            trackElementType="button"
            trackElementText="再次购买"
            trackProps={{ orderId: order.id }}
            onClick={onReorder}
            type='secondary'
            size='normal'
            className='action-button'
          >
            再次购买
          </AtButton>
          <AtButton
            type='secondary'
            size='normal'
            onClick={onContinueShopping}
            className='action-button'
          >
            继续购物
          </AtButton>
        </>
      )}

      {(order.status === 'pending_delivery' || order.status === 'cancelled') && (
        <AtButton
          type='secondary'
          size='normal'
          onClick={onReorder}
          className='action-button full-width'
        >
          再次购买
        </AtButton>
      )}
    </View>
  )
}

export default OrderActions
