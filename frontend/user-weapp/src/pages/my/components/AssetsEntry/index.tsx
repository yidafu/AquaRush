import React from 'react'
import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import CustomIcon from '@/components/CustomIcon'
import { authService } from '@/utils/auth'

import './index.scss'

/**
 * 「我的资产」聚合入口(my.html:单个 weui-cell):
 * 图标 + 标题 + 摘要行(登录态区分文案)+ 箭头;点击进 assets 聚合页。
 * 摘要行不做数量统计(需额外查询,后续再接);无储值余额(2026-10-09 合规决策)。
 */
const AssetsEntry: React.FC = () => {
  const isLoggedIn = authService.isAuthenticated()
  const summary = isLoggedIn
    ? '优惠券 · 水票 · 收藏 · 水桶账户'
    : '登录后查看优惠券 · 水票 · 收藏 · 水桶账户'

  const handleClick = (): void => {
    Taro.navigateTo({ url: '/pages/assets/index' })
  }

  return (
    <>
      <View className='weui-cells__title'>我的资产</View>
      <View className='weui-cells'>
        <View
          className='weui-cell'
          trackTarget='my-assets-entry'
          trackElementType='cell'
          trackElementText='我的资产'
          onClick={handleClick}
        >
          <View className='weui-cell__hd'>
            <View className='weui-cell__icon assets-entry__icon'>
              <CustomIcon value='/assets/icons/order/credit-card' size={24} />
            </View>
          </View>
          <View className='weui-cell__bd'>
            <View>
              <Text>我的资产</Text>
            </View>
            <View className='assets-entry__summary'>
              <Text>{summary}</Text>
            </View>
          </View>
          <View className='weui-cell__ft weui-cell__ft_in-access' />
        </View>
      </View>
    </>
  )
}

export default AssetsEntry
