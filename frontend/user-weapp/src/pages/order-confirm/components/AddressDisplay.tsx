import React from 'react'
import { View, Text } from '@tarojs/components'
import { Address } from '../../../types/address'
import './AddressDisplay.scss'

interface AddressDisplayProps {
  selectedAddress: Address | null
  onAddressSelect: () => void
}

/**
 * 收货地址卡(确认订单页 F-ORD-001)—— WeUI .weui-cells 链接单元格
 * (设计稿 02-order/order-confirm.html:紧凑两行 收货人+「默认」标签+电话 / 完整地址,
 * 右侧 weui 箭头;空态「请选择收货地址」+「选择地址」主色小按钮)。
 * 不再内嵌共享 AddressCard,避免「卡中卡」与字号混合。
 */
const AddressDisplay: React.FC<AddressDisplayProps> = ({
  selectedAddress,
  onAddressSelect
}) => {
  return (
    <View className='address-section'>
      <View className='weui-cells address-cells' onClick={onAddressSelect}>
        {selectedAddress ? (
          <View className='weui-cell weui-cell_link'>
            <View className='weui-cell__bd'>
              <View className='address-name-row'>
                <Text className='address-name'>{selectedAddress.receiverName}</Text>
                <Text className='address-phone'>{selectedAddress.phone}</Text>
                {selectedAddress.isDefault && (
                  <Text className='weui-tag weui-tag--brand'>默认</Text>
                )}
              </View>
              <Text className='address-detail'>
                {selectedAddress.province} {selectedAddress.city} {selectedAddress.district} {selectedAddress.detailAddress}
                {selectedAddress.floor ? ` ${selectedAddress.floor}楼` : ''}
              </Text>
            </View>
            <View className='weui-cell__ft weui-cell__ft_in-access' />
          </View>
        ) : (
          <View className='weui-cell weui-cell_link'>
            <View className='weui-cell__bd'>
              <Text className='address-name'>请选择收货地址</Text>
              <Text className='address-detail'>添加收货地址后即可下单</Text>
            </View>
            <View className='weui-cell__ft'>
              <Text className='weui-btn weui-btn_primary weui-btn_mini'>选择地址</Text>
            </View>
          </View>
        )}
      </View>
    </View>
  )
}

export default AddressDisplay
