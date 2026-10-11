import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { Address } from '../../../types/address'

interface AddressDisplayProps {
  selectedAddress: Address | null
  onAddressSelect: () => void
}

/**
 * 收货地址卡(确认订单页 F-ORD-001)—— WeUI .weui-cells 链接单元格
 * (设计稿 02-order/order-confirm.html:紧凑两行 收货人+「默认」标签+电话 / 完整地址,
 * 右侧 weui 箭头;空态「请选择收货地址」+「选择地址」主色小按钮)。
 * 不再内嵌共享 AddressCard,避免「卡中卡」与字号混合。
 *
 * 样式:StyleX(原 AddressDisplay.scss 迁移;750 基准 rpx,weui-* 类保留;
 * 原 .address-cells .weui-cell 后代选择器拍平到子元素)
 */
const styles = stylex.create({
  addressSection: {
    marginTop: '24rpx',
  },
  addressNameRow: {
    display: 'flex',
    alignItems: 'baseline',
    flexWrap: 'wrap',
    rowGap: '16rpx',
    columnGap: '16rpx',
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
    marginTop: '8rpx',
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
    lineHeight: 1.5,
  },
})
const AddressDisplay: React.FC<AddressDisplayProps> = ({
  selectedAddress,
  onAddressSelect
}) => {
  return (
    <View {...stylex.props(styles.addressSection)}>
      <View className='weui-cells' onClick={onAddressSelect}>
        {selectedAddress ? (
          <View className='weui-cell weui-cell_link'>
            <View className='weui-cell__bd'>
              <View {...stylex.props(styles.addressNameRow)}>
                <Text {...stylex.props(styles.addressName)}>{selectedAddress.receiverName}</Text>
                <Text {...stylex.props(styles.addressPhone)}>{selectedAddress.phone}</Text>
                {selectedAddress.isDefault && (
                  <Text className='weui-tag weui-tag--brand'>默认</Text>
                )}
              </View>
              <Text {...stylex.props(styles.addressDetail)}>
                {selectedAddress.province} {selectedAddress.city} {selectedAddress.district} {selectedAddress.detailAddress}
                {selectedAddress.floor ? ` ${selectedAddress.floor}楼` : ''}
              </Text>
            </View>
            <View className='weui-cell__ft weui-cell__ft_in-access' />
          </View>
        ) : (
          <View className='weui-cell weui-cell_link'>
            <View className='weui-cell__bd'>
              <Text {...stylex.props(styles.addressName)}>请选择收货地址</Text>
              <Text {...stylex.props(styles.addressDetail)}>添加收货地址后即可下单</Text>
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
