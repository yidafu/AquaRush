import React from 'react'
import { View, Text, Image, Button } from '@tarojs/components'
import { AtFloatLayout, AtInputNumber } from 'taro-ui'
import "taro-ui/dist/style/components/float-layout.scss"
import "taro-ui/dist/style/components/input-number.scss"
import { Money, calculateTotalCents, formatCentsToCurrency } from '@aquarush/common'

interface BuyModalProps {
  isOpened: boolean
  product: {
    id: string
    name: string
    priceCents: number
    originalPriceCents?: number
    specification: string
    coverImageUrl: string
    stock: number
  } | null
  quantity: number
  onQuantityChange: (value: number) => void
  onConfirm: () => void
  onCancel: () => void
}

const BuyModal: React.FC<BuyModalProps> = ({
  isOpened,
  product,
  quantity,
  onQuantityChange,
  onConfirm,
  onCancel
}) => {
  if (!product) return null

  // 水桶费不随商品订单捆绑收取(2026-08 改造,见 14-misc.md §G06)
  const totalCents = calculateTotalCents(quantity, product.priceCents)

  return (
    <AtFloatLayout
      isOpened={isOpened}
      onClose={onCancel}
      style={{
        minHeight: '300px'
      }}
      title="确认购买"
    >
      <View
        className='p-4'
      >
        {/* 产品信息 */}
        <View className='flex gap-6 pb-3 mb-4 border-b border-gray-200'>
          <Image
            src={product.coverImageUrl}
            mode='aspectFill'
            className='object-cover w-20 h-20 rounded-xl'
          />
          <View className='flex flex-col flex-1 gap-3'>
            <Text className='text-base font-semibold leading-tight text-gray-900'>{product.name}</Text>
            <Text className='text-sm font-bold text-red-500'><Money cents={product.priceCents} /></Text>
            <Text className='text-sm text-gray-600'>规格: {product.specification}</Text>
            <Text className='text-sm text-gray-600'>库存: {product.stock}</Text>
          </View>
        </View>

        {/* 数量选择 */}
        <View className='flex items-center justify-between mb-8'>
          <Text className='block mb-0 text-base font-medium text-gray-900'>购买数量</Text>
          <AtInputNumber
            value={quantity}
            min={1}
            max={product.stock || 1}
            onChange={onQuantityChange}
            size='normal'
            type='number'
          />
        </View>

        {/* 总价 */}
        <View
          className='flex items-center justify-between pt-2 border-t border-gray-200'
        >
          <Text className='text-base text-gray-600'>合计：</Text>
          <Text className='text-xl font-bold text-red-500'>
            {formatCentsToCurrency(totalCents)}
          </Text>
        </View>
      </View>

      {/* 自定义操作按钮 */}
      <View
        className='buy-modal-actions'
        style={{
          position: 'sticky',
          bottom: '0',
          background: 'white',
          padding: '16px 20px',
          borderTop: '1px solid #f0f0f0',
          display: 'flex',
          gap: '12px'
        }}
      >
        <Button
          onClick={onCancel}
          style={{
            flex: 1,
            height: '32px',
            fontSize: '16px',
            borderRadius: '8px',
            backgroundColor: 'var(--bg)',
            color: 'var(--text-sub)'
          }}
        >
          取消
        </Button>
        <Button
          trackTarget="buy-confirm"
          trackElementType="button"
          trackElementText="确认购买"
          trackProps={{ productId: product.id }}
          onClick={onConfirm}
          type='primary'
          style={{
            flex: 1,
            height: '32px',
            fontSize: '16px',
            borderRadius: '8px',
            backgroundColor: 'var(--action)', // 立即购买 CTA:强调橙
            borderColor: 'var(--action)'
          }}
        >
          确认购买
        </Button>
      </View>
    </AtFloatLayout>
  )
}

export default BuyModal
