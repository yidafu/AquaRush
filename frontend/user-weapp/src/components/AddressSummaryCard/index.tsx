import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'

/**
 * 样式:StyleX(原 index.scss 迁移,长度值保持 750 基准 rpx;weui-* 平台变量保留字符串引用;
 * 原 `.address-summary .weui-cell` 后代选择器拍平为元素级 style)
 */
const styles = stylex.create({
  addressHead: {
    display: 'flex',
    alignItems: 'baseline',
    columnGap: '16rpx',
  },
  receiverName: {
    fontSize: 'var(--weui-fs-cell)',
    fontWeight: 600,
    color: 'var(--weui-FG-0)',
  },
  receiverPhone: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
    fontVariantNumeric: 'tabular-nums',
  },
  addressDetail: {
    display: 'block',
    marginTop: '16rpx',
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
    lineHeight: 1.5,
  },
})

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
    <View>
      <View className='weui-cells__title'>{title}</View>
      <View className='weui-cells'>
        <View className='weui-cell'>
          <View className='weui-cell__bd'>
            <View {...stylex.props(styles.addressHead)}>
              <Text {...stylex.props(styles.receiverName)}>{receiverName}</Text>
              <Text {...stylex.props(styles.receiverPhone)}>{phone}</Text>
            </View>
            <Text {...stylex.props(styles.addressDetail)}>
              {province} {city} {district} {detailAddress}
            </Text>
          </View>
        </View>
      </View>
    </View>
  )
}

export default AddressSummaryCard
