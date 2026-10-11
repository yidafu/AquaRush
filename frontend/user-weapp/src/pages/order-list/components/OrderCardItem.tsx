import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { Money, OrderStatus, normalizeCents } from '@aquarush/common'
import StatusTag from '../../../components/StatusTag'
import OrderItemRow from '../../../components/OrderItemRow'
import type { OrderItem } from './constants'

interface OrderCardItemProps {
  order: OrderItem
  onPay: (order: OrderItem) => void
  onCancel: (order: OrderItem) => void
  onReorder: (order: OrderItem) => void
  onClick: (order: OrderItem) => void
}

// 订单列表卡:WeUI .weui-cells 组卡(全幅白底 + 发丝线,原型 02-order/order-list.html mt3 间距)
const styles = stylex.create({
  orderCard: {
    marginTop: '24rpx',
  },
  orderNo: {
    display: 'block',
    fontSize: 'var(--weui-fs-cell)',
    fontWeight: 600,
    color: 'var(--weui-FG-0)',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    fontVariantNumeric: 'tabular-nums',
  },
  orderTime: {
    display: 'block',
    marginTop: '8rpx',
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-2)',
    fontVariantNumeric: 'tabular-nums',
  },
  // 商品 cell:内边距交给 OrderItemRow 的 .weui-media-box
  orderProductCell: {
    paddingTop: 0,
    paddingRight: 0,
    paddingBottom: 0,
    paddingLeft: 0,
  },
  orderDiscountTags: {
    display: 'flex',
    flexWrap: 'wrap',
    rowGap: '12rpx',
    columnGap: '12rpx',
  },
  payDiscount: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
  },
  payLabel: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
  },
  orderAddress: {
    display: 'block',
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  orderActions: {
    flexWrap: 'wrap',
    justifyContent: 'flex-end',
    rowGap: '16rpx',
    columnGap: '16rpx',
  },
})

/**
 * 订单列表卡 —— WeUI .weui-cells 组卡(设计稿 02-order/order-list.html 骨架):
 * 头 cell(订单号 + 下单时间 + 状态 .weui-tag--*)→ 商品 cell(.weui-media-box 行)→
 * 营销优惠标签 cell(AC-ORD-002-10 —— R-MKT-07/08)→ 下单时间 + 实付 cell(.weui-price)→
 * 地址 + 操作 cell(.weui-btn_xs 按状态出按钮;送达即完成,无确认收货 —— PRD R-ORD-11)。
 * 点卡进详情;操作按钮 stopPropagation 防误跳转。
 */
const OrderCardItem: React.FC<OrderCardItemProps> = ({ order, onPay, onCancel, onReorder, onClick }) => {

  /**
   * 订单层总优惠(分)= 满减/折扣 + 优惠券 + 首单立减 + 水票抵扣(R-MKT-07/08 叠加,「已优惠」口径)
   */
  const totalDiscount = (): number =>
    (normalizeCents(order.promotionDiscountCents) ?? 0) +
    (normalizeCents(order.couponDiscountCents) ?? 0) +
    (normalizeCents(order.firstOrderDiscountCents) ?? 0) +
    (normalizeCents(order.ticketDiscountCents) ?? 0)

  const discount = totalDiscount()

  /**
   * 营销优惠标签行(AC-ORD-002-10 优惠明细:满减/首单立减/优惠券/水票 —— R-MKT-07/08)
   */
  const renderDiscountTags = (): React.ReactNode => {
    const tags: string[] = []
    if ((normalizeCents(order.promotionDiscountCents) ?? 0) > 0) {
      tags.push(order.promotionActivityName ? `满减·${order.promotionActivityName}` : '满减')
    }
    if ((normalizeCents(order.firstOrderDiscountCents) ?? 0) > 0) {
      tags.push(order.firstOrderActivityName ? `首单立减·${order.firstOrderActivityName}` : '首单立减')
    }
    if ((normalizeCents(order.couponDiscountCents) ?? 0) > 0) {
      tags.push(order.couponName ? `优惠券·${order.couponName}` : '优惠券')
    }
    if ((order.ticketRedeemCount ?? 0) > 0 && (normalizeCents(order.ticketDiscountCents) ?? 0) > 0) {
      tags.push(`水票抵扣${order.ticketRedeemCount}桶`)
    }
    if (tags.length === 0) return null

    return (
      <View className='weui-cell'>
        <View {...props('weui-cell__bd', styles.orderDiscountTags)}>
          {tags.map(tag => (
            <Text key={tag} className='weui-tag weui-tag--orange'>{tag}</Text>
          ))}
        </View>
      </View>
    )
  }

  const stop = (e: { stopPropagation: () => void }) => e.stopPropagation()

  return (
    <View {...stylex.props(styles.orderCard)}>
      <View className='weui-cells' onClick={() => onClick(order)}>
        {/* 头 cell:订单号 + 下单时间 + 状态标签 */}
        <View className='weui-cell'>
          <View className='weui-cell__bd'>
            <Text {...stylex.props(styles.orderNo)}>{order.orderNo}</Text>
            <Text {...stylex.props(styles.orderTime)}>{order.createTime}</Text>
          </View>
          <View className='weui-cell__ft'>
            <StatusTag status={order.status} size='small' />
          </View>
        </View>

        {/* 商品 cell(F-ORD-004 多商品:首件 + 「等N件」) */}
        {order.product && (
          <View {...props('weui-cell', styles.orderProductCell)}>
            <OrderItemRow
              image={order.product.image}
              name={order.product.name}
              spec={order.product.specification}
              priceCents={normalizeCents(order.unitPriceCents ?? order.product.priceCents)}
              originalPriceCents={order.hasMarketingPrice ? normalizeCents(order.product.priceCents) : null}
              quantity={order.quantity}
              badge={order.items.length > 1 ? `等${order.items.length}件` : undefined}
            />
          </View>
        )}

        {/* 营销优惠标签(AC-ORD-002-10:满减/首单立减/优惠券/水票,有优惠时展示) */}
        {renderDiscountTags()}

        {/* 下单时间 + 实付 cell */}
        <View className='weui-cell'>
          <View className='weui-cell__bd'>
            <Text {...stylex.props(styles.orderTime)}>{order.createTime}</Text>
          </View>
          <View className='weui-cell__ft'>
            {discount > 0 && (
              <Text {...stylex.props(styles.payDiscount)}>已优惠 <Money cents={discount} /></Text>
            )}
            <Text {...stylex.props(styles.payLabel)}>实付</Text>
            <Text className='weui-price weui-price--md'>
              <Money cents={order.payAmountCents} />
            </Text>
          </View>
        </View>

        {/* 地址 + 操作 cell(按钮按状态渲染) */}
        <View className='weui-cell'>
          <View className='weui-cell__bd'>
            {order.address && (
              <Text {...stylex.props(styles.orderAddress)}>
                {order.address.receiverName} {order.address.phone}
                {order.address.province} {order.address.city} {order.address.district} {order.address.detailAddress}
              </Text>
            )}
          </View>
          <View {...props('weui-cell__ft', styles.orderActions)}>
            {order.status === OrderStatus.PENDING_PAYMENT && (
              <>
                <View
                  className='weui-btn weui-btn_plain-default weui-btn_xs'
                  trackTarget='order-cancel'
                  trackElementType='button'
                  trackElementText='取消订单'
                  trackProps={{ orderId: order.id }}
                  onClick={(e) => { stop(e); onCancel(order) }}
                >
                  取消订单
                </View>
                <View
                  className='weui-btn weui-btn_primary weui-btn_xs'
                  trackTarget='order-pay'
                  trackElementType='button'
                  trackElementText='立即支付'
                  trackProps={{ orderId: order.id }}
                  onClick={(e) => { stop(e); onPay(order) }}
                >
                  立即支付
                </View>
              </>
            )}

            {(order.status === OrderStatus.PENDING_DISPATCH || order.status === OrderStatus.PENDING_DELIVERY) && (
              <>
                <View
                  className='weui-btn weui-btn_plain-default weui-btn_xs'
                  onClick={(e) => { stop(e); onClick(order) }}
                >
                  查看详情
                </View>
                <View
                  className='weui-btn weui-btn_plain-default weui-btn_xs'
                  onClick={(e) => { stop(e); onCancel(order) }}
                >
                  取消订单
                </View>
              </>
            )}

            {order.status === OrderStatus.DELIVERING && (
              <View
                className='weui-btn weui-btn_plain-default weui-btn_xs'
                onClick={(e) => { stop(e); onClick(order) }}
              >
                查看详情
              </View>
            )}

            {order.status === OrderStatus.COMPLETED && (
              <View
                className='weui-btn weui-btn_plain-primary weui-btn_xs'
                trackTarget='order-reorder'
                trackElementType='button'
                trackElementText='再次购买'
                trackProps={{ orderId: order.id }}
                onClick={(e) => { stop(e); onReorder(order) }}
              >
                再次购买
              </View>
            )}
          </View>
        </View>
      </View>
    </View>
  )
}

export default OrderCardItem
