import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtButton, AtIcon } from 'taro-ui'
import { Address } from '../../types/address'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/icon.scss"
import './index.scss'

interface AddressCardProps {
  address: Address
  isSelectMode?: boolean
  onSelect?: (address: Address) => void
  onEdit?: (addressId: number) => void
  onSetDefault?: (addressId: number) => void
  onDelete?: (addressId: number) => void
}

const AddressCard: React.FC<AddressCardProps> = ({
  address,
  isSelectMode = false,
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
      <View className='flex justify-between flex-grow flow-row'>
        <View className='flex flex-row '>
          {address.isDefault && (
            <View className='flex items-center mr-1 default-badge'>
              <Text>默认</Text>
            </View>
          )}
          <Text className='text-base font-bold theme-text-primary'>{address.receiverName}</Text>
        </View>

        <Text className='theme-text-secondary'>{address.phone}</Text>

      </View>

      <View className='py-2 border-b-2 border-gray-300 border-solid'>
        <Text className='text-sm theme-text-tertiary'>
          {address.province} {address.city} {address.district} {address.detailAddress}
        </Text>
      </View>
      <View className='flex flex-row justify-between mt-2'>
        <View className='flex items-center justify-center' onClick={handleSetDefault}>
          <Text className='text-base'>
            默认地址
          </Text>
        </View>
        <View className='flex flex-row'>
          <View className='flex items-center mr-4' onClick={handleEditAddress}>
            <AtIcon value='edit' size='16' className='mr-2' />
            <Text className='text-base theme-text-secondary'>编辑</Text>
          </View>
          <View className='flex items-center' onClick={handleDeleteClick}>
            <AtIcon value='trash' size='16' className='mr-2' color='var(--theme-error)' />
            <Text className='text-base theme-error'>删除</Text>
          </View>
        </View>
      </View>
      {isSelectMode && (
        <View className='card-actions'>
          <AtButton
            type='primary'
            size='normal'
            onClick={handleSelectAddress}
            className='select-button'
          >
            选择此地址
          </AtButton>
        </View>
      )}
    </View>
  )
}

export default AddressCard
