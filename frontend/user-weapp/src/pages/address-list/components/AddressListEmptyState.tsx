import React from 'react'
import { View, Text, Image } from '@tarojs/components'

import './AddressListEmptyState.scss'

interface AddressListEmptyStateProps {
  searching: boolean
}

/**
 * 地址列表空态:搜索无结果 / 无收货地址
 */
const AddressListEmptyState: React.FC<AddressListEmptyStateProps> = ({ searching }) => {
  return (
    <View className='empty-container'>
      {searching ? (
        <>
          <Text className='empty-text'>未找到匹配地址</Text>
          <Text className='empty-desc'>试试其他搜索词</Text>
        </>
      ) : (
        <>
          <Image
            src='/assets/empty-address.png'
            mode='aspectFit'
            className='empty-image'
          />
          <Text className='empty-text'>暂无收货地址</Text>
          <Text className='empty-desc'>添加您的收货地址，方便快速下单</Text>
        </>
      )}
    </View>
  )
}

export default AddressListEmptyState
