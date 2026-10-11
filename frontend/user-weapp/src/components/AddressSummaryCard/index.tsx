import React from 'react'
import { View, Text } from '@tarojs/components'
import './index.scss'

/**
 * 只读收货/配送地址卡 —— WeUI 2.x 视觉(.weui-cells__title + .weui-cell,
 * 设计稿 02-order/order-detail.html 收货地址口径:联系人/电话一行 + 完整地址含楼层)。
 * 收敛 order-detail OrderAddressCard 与 delivery-tracking DeliveryAddressCard
 * 两处近重复实现(title 区分「收货地址/配送地址」);
 * 共享 CRUD 版 AddressCard(选择/编辑/删除)是另一变体,不合并。
 */
interface AddressSummaryCardProps {
  title?: string
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
}

const AddressSummaryCard: React.FC<AddressSummaryCardProps> = ({
  title = '收货地址',
  receiverName,
  phone,
  province,
  city,
  district,
  detailAddress
}) => {
  return (
    <View className='address-summary'>
      <View className='weui-cells__title'>{title}</View>
      <View className='weui-cells'>
        <View className='weui-cell'>
          <View className='weui-cell__bd'>
            <View className='address-head'>
              <Text className='receiver-name'>{receiverName}</Text>
              <Text className='receiver-phone'>{phone}</Text>
            </View>
            <Text className='address-detail'>
              {province} {city} {district} {detailAddress}
            </Text>
          </View>
        </View>
      </View>
    </View>
  )
}

export default AddressSummaryCard
