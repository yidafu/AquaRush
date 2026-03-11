import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import { displayCents } from '@/utils/money'
import './ProductList.scss'

interface OrderItem {
  id: string
  name: string
  price: number
  quantity: number
  image: string
  specifications?: string[]
}

interface ProductListProps {
  orderItems: OrderItem[]
}

const ProductList: React.FC<ProductListProps> = ({ orderItems }) => {
  return (
    <View className='section product-section'>
      <View className='section-header'>
        <Text className='section-title'>商品信息</Text>
      </View>
      <View className='product-list'>
        {orderItems.map((item, index) => (
          <View key={`${item.id}-${index}`} className='product-item'>
            <Image
              src={item.image}
              mode='aspectFill'
              className='product-image'
            />
            <View className='product-info'>
              <Text className='product-name'>{item.name}</Text>
              {item.specifications && item.specifications.length > 0 && (
                <Text className='product-spec'>
                  规格：{item.specifications.join(', ')}
                </Text>
              )}
              <View className='product-bottom'>
                <Text className='product-price'>{displayCents(item.price)}</Text>
                <Text className='product-quantity'>x{item.quantity}</Text>
              </View>
            </View>
          </View>
        ))}
      </View>
    </View>
  )
}

export default ProductList