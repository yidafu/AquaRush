import React from 'react'
import { View, Textarea, CommonEventFunction, CommonEvent } from '@tarojs/components'
import { AtList, AtListItem, AtInputNumber } from 'taro-ui'
import 'taro-ui/dist/style/components/list.scss'
import 'taro-ui/dist/style/components/input-number.scss'
import { formatCentsToCurrency } from '@aquarush/common'
import './index.scss'

interface Product {
  id: string
  price: number
}

// 订单信息
export interface OrderInfo {
  quantity: number
  isSelfCollect: boolean
  remark: string
}

interface OrderSummaryCardProps {
  product: Product | null
  orderInfo: OrderInfo
  onOrderInfoChange: (orderInfo: OrderInfo) => void
}

const OrderSummaryCard: React.FC<OrderSummaryCardProps> = ({
  product,
  orderInfo,
  onOrderInfoChange,
}) => {
  const { quantity, isSelfCollect, remark } = orderInfo

  // 计算总价
  const totalAmount = product ? product.price * quantity : 0

  const handleQuantityChange = (value: number) => {
    onOrderInfoChange({ ...orderInfo, quantity: value })
  }

  const handleSelfCollectChange = (evt: CommonEvent<{value: boolean}>) => {
    onOrderInfoChange({ ...orderInfo, isSelfCollect: evt.detail.value })
  }

  const handleRemarkChange = (value: string) => {
    onOrderInfoChange({ ...orderInfo, remark: value })
  }

  return (
    <View className='order-summary-card'>
      <AtList>
        <AtListItem title='商品单价' extraText={formatCentsToCurrency(product?.price ?? 0)} />
        <AtListItem
          title='数量'
          extraText={
            <View className='quantity-editor'>
              <AtInputNumber
                min={1}
                max={99}
                step={1}
                value={quantity}
                type='digit'
                onChange={handleQuantityChange}
              />
            </View>
          }
        />
        <AtListItem title='合计' extraText={formatCentsToCurrency(totalAmount)} />
        <AtListItem
          title='是否已收款'
          isSwitch
          switchIsCheck={isSelfCollect}
          onSwitchChange={handleSelfCollectChange}
        />
        <Textarea
          className='remark-input'
          value={remark}
          onInput={(e) => handleRemarkChange(e.detail.value)}
          placeholder='请输入订单备注（可选）'
          maxlength={500}
          autoHeight
        />
      </AtList>
    </View>
  )
}

export default OrderSummaryCard
