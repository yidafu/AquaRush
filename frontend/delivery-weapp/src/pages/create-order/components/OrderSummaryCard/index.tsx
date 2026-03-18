import React from 'react'
import { View, Textarea } from '@tarojs/components'
import { AtList, AtListItem, AtInputNumber } from 'taro-ui'
import 'taro-ui/dist/style/components/list.scss'
import 'taro-ui/dist/style/components/input-number.scss'
import { formatCentsToCurrency } from '@aquarush/common'
import './index.scss'

interface Product {
  id: string
  price: number
}

interface OrderSummaryCardProps {
  product: Product | null
  quantity: number
  onQuantityChange: (quantity: number) => void
  isSelfCollect: boolean
  onSelfCollectChange: (value: boolean) => void
  remark: string
  onRemarkChange: (value: string) => void
}

const OrderSummaryCard: React.FC<OrderSummaryCardProps> = ({
  product,
  quantity,
  onQuantityChange,
  isSelfCollect,
  onSelfCollectChange,
  remark,
  onRemarkChange,
}) => {
  // 计算总价
  const totalAmount = product ? product.price * quantity : 0

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
                onChange={(value: number) => onQuantityChange(value)}
              />
            </View>
          }
        />
        <AtListItem title='合计' extraText={formatCentsToCurrency(totalAmount)} />
        <AtListItem
          title='是否已收款'
          isSwitch
          switchIsCheck={isSelfCollect}
          onSwitchChange={(e: any) => onSelfCollectChange(e.value)}
        />
        <Textarea
          className='remark-input'
          value={remark}
          onInput={(e) => onRemarkChange(e.detail.value)}
          placeholder='请输入订单备注（可选）'
          maxlength={500}
          autoHeight
        />
      </AtList>
    </View>
  )
}

export default OrderSummaryCard
