import React from 'react'
import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import CustomIcon from '@/components/CustomIcon'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

import './index.scss'

interface AssetEntry {
  /** trackTarget 后缀 */
  track: string
  icon: string
  title: string
  url: string
}

/**
 * 我的资产聚合页(spec/design/user-weapp/04-marketing/assets.html,F-MKT-010):
 * 收口「我的」页平铺的收藏 / 优惠券 / 水票 / 水桶入口,统一聚合下钻。
 * 水桶账户(水桶账户 → barrel-mine 押金/退押)单行入口(G06 押金模型回退后
 * 买水桶页已删除,购桶不再开放);不做数量统计(需额外查询,后续再接);
 * 无储值余额(用户储值不做,2026-10-09 合规决策)。
 */
const ASSET_ENTRIES: AssetEntry[] = [
  {
    track: 'favorites',
    icon: '/assets/icons/service/heart',
    title: '我的收藏',
    url: '/pages/favorites/index'
  },
  {
    track: 'coupon-list',
    icon: '/assets/icons/service/coupon',
    title: '优惠券',
    url: '/pages/coupon-list/index'
  },
  {
    track: 'ticket-list',
    icon: '/assets/icons/service/ticket',
    title: '我的水票',
    url: '/pages/ticket-list/index'
  },
  {
    track: 'barrel-mine',
    icon: '/assets/icons/order/check-circle',
    title: '水桶账户',
    url: '/pages/barrel-mine/index'
  }
]

const AssetsPage: React.FC = () => {
  usePageViewTracking()

  const handleEntryClick = (entry: AssetEntry): void => {
    Taro.navigateTo({ url: entry.url })
  }

  return (
    <View className='assets-page'>
      <View className='weui-cells__title assets-title'>资产概览</View>
      <View className='weui-cells'>
        {ASSET_ENTRIES.map((entry) => (
          <View
            key={entry.track}
            className='weui-cell'
            trackTarget={`assets-entry-${entry.track}`}
            trackElementType='cell'
            trackElementText={entry.title}
            onClick={() => handleEntryClick(entry)}
          >
            <View className='weui-cell__hd'>
              <View className='weui-cell__icon assets-entry-icon'>
                <CustomIcon value={entry.icon} size={24} />
              </View>
            </View>
            <View className='weui-cell__bd'>
              <Text>{entry.title}</Text>
            </View>
            <View className='weui-cell__ft weui-cell__ft_in-access' />
          </View>
        ))}
      </View>
      <View className='weui-cells__tips'>
        优惠券与水票均为预购凭证:有效期以票面为准;水桶押金可退。
      </View>

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default AssetsPage
