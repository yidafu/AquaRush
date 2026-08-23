import React from 'react'
import EmptyState from '../../../components/EmptyState'

interface AddressListEmptyStateProps {
  searching: boolean
}

/**
 * 地址列表空态:搜索无结果 / 无收货地址(渲染统一走共享 EmptyState)
 */
const AddressListEmptyState: React.FC<AddressListEmptyStateProps> = ({ searching }) => {
  return searching ? (
    <EmptyState title='未找到匹配地址' description='试试其他搜索词' />
  ) : (
    <EmptyState
      image='/assets/empty-address.png'
      title='暂无收货地址'
      description='添加您的收货地址，方便快速下单'
    />
  )
}

export default AddressListEmptyState
