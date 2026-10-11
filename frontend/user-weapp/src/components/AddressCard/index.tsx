import React from 'react'
import { View, Text } from '@tarojs/components'
import { Address } from '../../types/address'

import './index.scss'

interface AddressCardProps {
  address: Address
  isSelectMode?: boolean
  hideActions?: boolean
  onSelect?: (address: Address) => void
  onEdit?: (addressId: string) => void
  onSetDefault?: (addressId: string) => void
  onDelete?: (addressId: string) => void
}

/**
 * 收货地址卡(CRUD 版:选择/编辑/删除)—— WeUI 2.x 度量
 * (17px 主文字 / 发丝线 / 间距 32,色值一律 var(--weui-*));
 * 只读收货/配送地址卡是另一变体(AddressSummaryCard),不合并。
 */
const AddressCard: React.FC<AddressCardProps> = ({
  address,
  isSelectMode = false,
  hideActions = false,
  onSelect,
  onEdit,
  onSetDefault,
  onDelete
}) => {
  const handleSelectAddress = () => {
    if (onSelect && isSelectMode) {
      onSelect(address)
    }
  }

  const handleEditAddress = () => {
    if (onEdit) {
      onEdit(address.id)
    }
  }

  const handleSetDefault = () => {
    if (onSetDefault) {
      onSetDefault(address.id)
    }
  }

  const handleDeleteClick = () => {
    if (onDelete) {
      onDelete(address.id)
    }
  }

  return (
    <View className='address-card'>
      <View className='address-head'>
        <View className='address-name-row'>
          {address.isDefault && <Text className='weui-tag weui-tag--assign'>默认</Text>}
          <Text className='address-name'>{address.receiverName}</Text>
        </View>
        <Text className='address-phone'>{address.phone}</Text>
      </View>

      <Text className='address-detail'>
        {address.province} {address.city} {address.district} {address.detailAddress}
        {address.floor ? ` ${address.floor}楼` : ''}
      </Text>

      {!hideActions && (
        <View className='address-actions'>
          <View className='address-action' onClick={handleSetDefault}>
            默认地址
          </View>
          <View className='address-action' onClick={handleEditAddress}>
            编辑
          </View>
          <View className='address-action address-action--danger' onClick={handleDeleteClick}>
            删除
          </View>
        </View>
      )}

      {isSelectMode && (
        <View className='address-select'>
          <View
            className='weui-btn weui-btn_primary weui-btn_mini'
            onClick={handleSelectAddress}
          >
            选择此地址
          </View>
        </View>
      )}
    </View>
  )
}

export default AddressCard
