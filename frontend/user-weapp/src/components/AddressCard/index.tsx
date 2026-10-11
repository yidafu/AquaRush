import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { Address } from '../../types/address'

/**
 * 样式:StyleX(原 index.scss 迁移,长度值保持 750 基准 rpx;weui-* 平台变量保留字符串引用)
 */
const styles = stylex.create({
  addressCard: {
    backgroundColor: 'var(--weui-BG-2)',
    borderRadius: 'var(--weui-radius)',
    paddingTop: '32rpx',
    paddingRight: '32rpx',
    paddingBottom: '32rpx',
    paddingLeft: '32rpx',
    marginBottom: '24rpx',
    boxShadow: 'inset 0 0 0 2rpx var(--weui-FG-3)',
  },
  addressHead: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    columnGap: '16rpx',
  },
  addressNameRow: {
    display: 'flex',
    alignItems: 'center',
    columnGap: '12rpx',
    minWidth: 0,
  },
  addressName: {
    fontSize: 'var(--weui-fs-cell)',
    fontWeight: 600,
    color: 'var(--weui-FG-0)',
  },
  addressPhone: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
    fontVariantNumeric: 'tabular-nums',
  },
  addressDetail: {
    display: 'block',
    marginTop: '16rpx',
    paddingBottom: '24rpx',
    borderBottomWidth: '2rpx',
    borderBottomStyle: 'solid',
    borderBottomColor: 'var(--weui-FG-3)',
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
    lineHeight: 1.5,
  },
  addressActions: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'flex-end',
    columnGap: '40rpx',
    paddingTop: '24rpx',
  },
  addressAction: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-0)',
  },
  addressActionDanger: {
    color: 'var(--weui-RED)',
  },
  addressSelect: {
    display: 'flex',
    justifyContent: 'center',
    paddingTop: '24rpx',
  },
})

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
    <View {...stylex.props(styles.addressCard)}>
      <View {...stylex.props(styles.addressHead)}>
        <View {...stylex.props(styles.addressNameRow)}>
          {address.isDefault && <Text className='weui-tag weui-tag--assign'>默认</Text>}
          <Text {...stylex.props(styles.addressName)}>{address.receiverName}</Text>
        </View>
        <Text {...stylex.props(styles.addressPhone)}>{address.phone}</Text>
      </View>

      <Text {...stylex.props(styles.addressDetail)}>
        {address.province} {address.city} {address.district} {address.detailAddress}
        {address.floor ? ` ${address.floor}楼` : ''}
      </Text>

      {!hideActions && (
        <View {...stylex.props(styles.addressActions)}>
          <View {...stylex.props(styles.addressAction)} onClick={handleSetDefault}>
            默认地址
          </View>
          <View {...stylex.props(styles.addressAction)} onClick={handleEditAddress}>
            编辑
          </View>
          <View
            {...stylex.props(styles.addressAction, styles.addressActionDanger)}
            onClick={handleDeleteClick}
          >
            删除
          </View>
        </View>
      )}

      {isSelectMode && (
        <View {...stylex.props(styles.addressSelect)}>
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
