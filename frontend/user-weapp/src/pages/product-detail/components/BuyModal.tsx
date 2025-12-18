import React from 'react'
import { View, Text, Image, Button } from '@tarojs/components'
import { AtFloatLayout, AtInputNumber } from 'taro-ui'
import "taro-ui/dist/style/components/float-layout.scss"
import "taro-ui/dist/style/components/input-number.scss"
import { displayCents, calculateAndFormatTotal } from '@/utils/money'

interface BuyModalProps {
  isOpened: boolean
  product: {
    id: string
    name: string
    price: string
    originalPrice?: string
    depositPrice?: string
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

  const hasDeposit = product.depositPrice && parseFloat(product.depositPrice) > 0

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
            <Text className='text-sm font-bold text-red-500'>{displayCents(parseFloat(product.price))}</Text>
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
            {calculateAndFormatTotal(quantity, parseFloat(product.price))}
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
            backgroundColor: '#f5f5f5',
            color: '#666'
          }}
        >
          取消
        </Button>
        <Button
          type='primary'
          onClick={onConfirm}
          style={{
            flex: 1,
            height: '32px',
            fontSize: '16px',
            borderRadius: '8px',
            backgroundColor: '#ff6700',
            borderColor: '#ff6700'
          }}
        >
          确认购买
        </Button>
      </View>
    </AtFloatLayout>
  )
}

export default BuyModal
