import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { DeliverySlotVo } from '@aquarush/common'
import { PAYMENT_METHOD_MAP, DELIVERY_TIME_SLOT_MAP } from './constants'
import type { OrderDetail } from './constants'
import { deliverySlotLabel } from '../../../utils/deliverySlots'

interface OrderInfoCardProps {
  order: OrderDetail
  // 配送时段配置(F-SYS-002:全量含已停用,历史订单时段文案按配置渲染,兜底见 DELIVERY_TIME_SLOT_MAP)
  deliverySlots: DeliverySlotVo[]
  /** 复制订单号(设计稿「订单信息」首行:订单编号 + 复制) */
  onCopyOrderNo: () => void
}

/**
 * 订单信息卡 —— WeUI .weui-cells 组(设计稿 02-order/order-detail.html「订单信息」:
 * 订单编号(等宽数字,可复制)/ 下单时间 / 支付方式 / 配送时段 / 支付时间 / 完成时间 / 订单备注)
 *
 * 样式:StyleX(原 OrderInfoCard.scss 迁移;750 基准 rpx,weui-* 类保留;
 * 原 .info-card .info-value .info-text 后代选择器拍平到值文本;
 * info-card / info-value 为纯锚点无自有样式,按死类删除)
 */
const styles = stylex.create({
  infoText: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
    fontVariantNumeric: 'tabular-nums',
    textAlign: 'right',
  },
})
const OrderInfoCard: React.FC<OrderInfoCardProps> = ({ order, deliverySlots, onCopyOrderNo }) => {
  return (
    <View>
      <View className='weui-cells__title'>订单信息</View>
      <View className='weui-cells'>
        <View className='weui-cell'>
          <View className='weui-cell__bd'>订单编号</View>
          <View className='weui-cell__ft'>
            <Text {...stylex.props(styles.infoText)}>{order.orderNo}</Text>
            <Text
              className='weui-btn weui-btn_plain-primary weui-btn_xs'
              onClick={onCopyOrderNo}
            >
              复制
            </Text>
          </View>
        </View>
        <View className='weui-cell'>
          <View className='weui-cell__bd'>支付方式</View>
          <View className='weui-cell__ft'>
            <Text {...stylex.props(styles.infoText)}>
              {PAYMENT_METHOD_MAP[order.paymentMethod] || order.paymentMethod}
              {/* 核销水票时展示水票支付(F-MKT-007,R-MKT-08:被核销桶按水票支付) */}
              {(order.ticketRedeemCount ?? 0) > 0 && ` + 水票${order.ticketRedeemCount}张`}
            </Text>
          </View>
        </View>
        <View className='weui-cell'>
          <View className='weui-cell__bd'>下单时间</View>
          <View className='weui-cell__ft'>
            <Text {...stylex.props(styles.infoText)}>{order.createTime}</Text>
          </View>
        </View>
        {order.deliveryTimeSlot && (
          <View className='weui-cell'>
            <View className='weui-cell__bd'>配送时段</View>
            <View className='weui-cell__ft'>
              <Text {...stylex.props(styles.infoText)}>
                {deliverySlotLabel(deliverySlots, order.deliveryTimeSlot) ||
                  DELIVERY_TIME_SLOT_MAP[order.deliveryTimeSlot] ||
                  order.deliveryTimeSlot}
              </Text>
            </View>
          </View>
        )}
        {order.paymentTime && (
          <View className='weui-cell'>
            <View className='weui-cell__bd'>支付时间</View>
            <View className='weui-cell__ft'>
              <Text {...stylex.props(styles.infoText)}>{order.paymentTime}</Text>
            </View>
          </View>
        )}
        {order.completedTime && (
          <View className='weui-cell'>
            <View className='weui-cell__bd'>完成时间</View>
            <View className='weui-cell__ft'>
              <Text {...stylex.props(styles.infoText)}>{order.completedTime}</Text>
            </View>
          </View>
        )}
        {order.remark && (
          <View className='weui-cell'>
            <View className='weui-cell__bd'>订单备注</View>
            <View className='weui-cell__ft'>
              <Text {...stylex.props(styles.infoText)}>{order.remark}</Text>
            </View>
          </View>
        )}
      </View>
    </View>
  )
}

export default OrderInfoCard
