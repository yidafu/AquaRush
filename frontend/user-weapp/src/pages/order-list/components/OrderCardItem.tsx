import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtCard, AtIcon, AtButton } from 'taro-ui'
import { Money, OrderStatus, normalizeCents } from '@aquarush/common'
import StatusTag from '../../../components/StatusTag'
import OrderItemRow from '../../../components/OrderItemRow'
import type { OrderItem } from './constants'

import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/icon.scss"
import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/tag.scss"
import './OrderCardItem.scss'

interface OrderCardItemProps {
  order: OrderItem
  onPay: (order: OrderItem) => void
  onCancel: (order: OrderItem) => void
  onReorder: (order: OrderItem) => void
  onClick: (order: OrderItem) => void
}

/**
 * 订单列表卡片(F-ORD-004 多商品:首件 + 「等N件」):
 * 状态头 + 首件商品(命中营销价划线目录价,AC-ORD-002-09 —— R-MKT-01/02) +
 * 收货地址 + 营销优惠标签行(AC-ORD-002-10 —— R-MKT-07/08) +
 * 底部实付金额 + 按状态操作按钮
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
      <View className='order-discount-tags'>
        {tags.map(tag => (
          <Text key={tag} className='discount-tag'>{tag}</Text>
        ))}
      </View>
    )
  }

  return (
    <AtCard key={order.id} className='order-card' onClick={() => onClick(order)}>
      <View className='order-header'>
        <View className='order-info'>
          <Text className='order-no'>订单号：{order.orderNo}</Text>
          <StatusTag status={order.status} size='small' />
        </View>
        <Text className='order-time'>{order.createTime}</Text>
      </View>

      {order.product && (
        <View className='order-items'>
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

      {order.address && (
        <View className='order-address'>
          <AtIcon value='map-pin' size='14' color='var(--text-sub)' />
          <Text className='address-text'>
            {order.address.receiverName} {order.address.phone}
            {order.address.province} {order.address.city} {order.address.district} {order.address.detailAddress}
          </Text>
        </View>
      )}

      {/* 营销优惠标签(AC-ORD-002-10:满减/首单立减/优惠券/水票,有优惠时展示) */}
      {renderDiscountTags()}

      <View className='order-footer'>
        <View className='order-amount'>
          {discount > 0 && (
            <Text className='amount-discount'>已优惠 <Money cents={discount} /></Text>
          )}
          <Text className='amount-label'>实付：</Text>
          <Text className='amount-value'><Money cents={order.payAmountCents} /></Text>
        </View>

        <View className='order-actions'>
          {order.status === OrderStatus.PENDING_PAYMENT && (
            <>
              <AtButton
                trackTarget="order-cancel"
                trackElementType="button"
                trackElementText="取消订单"
                trackProps={{ orderId: order.id }}
                onClick={() => onCancel(order)}
                type='secondary'
                size='small'
                className='action-button'
              >
                  取消订单
              </AtButton>
              <AtButton
                trackTarget="order-pay"
                trackElementType="button"
                trackElementText="立即支付"
                trackProps={{ orderId: order.id }}
                onClick={() => onPay(order)}
                type='primary'
                size='small'
                className='action-button'
              >
                  立即支付
              </AtButton>
            </>
          )}

          {(order.status === OrderStatus.PENDING_DISPATCH || order.status === OrderStatus.PENDING_DELIVERY) && (
            <>
              <AtButton
                type='secondary'
                size='small'
                onClick={() => onClick(order)}
                className='action-button'
              >
                查看详情
              </AtButton>
              <AtButton
                type='secondary'
                size='small'
                onClick={() => onCancel(order)}
                className='action-button'
              >
                取消订单
              </AtButton>
            </>
          )}

          {order.status === OrderStatus.DELIVERING && (
            <AtButton
              type='secondary'
              size='small'
              onClick={() => onClick(order)}
              className='action-button'
            >
              查看详情
            </AtButton>
          )}

          {order.status === OrderStatus.COMPLETED && (
            <AtButton
              trackTarget="order-reorder"
              trackElementType="button"
              trackElementText="再次购买"
              trackProps={{ orderId: order.id }}
              onClick={() => onReorder(order)}
              type='secondary'
              size='small'
              className='action-button'
            >
              再次购买
            </AtButton>
          )}
        </View>
      </View>
    </AtCard>
  )
}

export default OrderCardItem
