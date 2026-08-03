import React from 'react'
import { View, Text, Button, Image } from '@tarojs/components'
import Taro from '@tarojs/taro'
import './index.scss'
import { useAuth } from '../../hooks/useAuth'
import {PageContainer} from '../../components/PageContainer'

const MyPage: React.FC = () => {
  const { workerInfo, logout } = useAuth()

  const handleLogout = () => {
    Taro.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          logout()
        }
      },
    })
  }

  // workerInfo 为空时不渲染，PageContainer 会处理重定向
  if (!workerInfo) {
    return (
      <PageContainer title='我的'>
        <View />
      </PageContainer>
    )
  }

  return (
    <PageContainer title='我的'>
      <View className='my-page'>
        {/* 用户信息 */}
        <View className='user-info'>
          <View className='avatar'>
            {workerInfo.avatarUrl ? (
              <Image src={workerInfo.avatarUrl} className='avatar-img' mode='aspectFill' />
            ) : (
              <Text className='avatar-text'>{workerInfo.name.charAt(0)}</Text>
            )}
          </View>
          <View className='user-detail'>
            <Text className='user-name'>{workerInfo.name}</Text>
            <Text className='user-phone'>{workerInfo.phone}</Text>
          </View>
        </View>

        {/* 功能菜单 */}
        <View className='menu-list'>

          <View
            className='menu-item'
            onClick={() => Taro.navigateTo({ url: '/pages/daily-collection/index' })}
          >
            <Text className='menu-label'>每日收款</Text>
            <Text className='menu-arrow'>›</Text>
          </View>

          <View
            className='menu-item'
            onClick={() => Taro.navigateTo({ url: '/pages/reconciliation-records/index' })}
          >
            <Text className='menu-label'>对账记录</Text>
            <Text className='menu-arrow'>›</Text>
          </View>

          <View
            className='menu-item'
            onClick={() => Taro.navigateTo({ url: '/pages/history-orders/index' })}
          >
            <Text className='menu-label'>历史订单</Text>
            <Text className='menu-arrow'>›</Text>
          </View>

          <View
            className='menu-item'
            onClick={() => Taro.navigateTo({ url: '/pages/about/index' })}
          >
            <Text className='menu-label'>关于我们</Text>
            <Text className='menu-arrow'>›</Text>
          </View>
        </View>

        {/* 退出登录 */}
        <View className='logout-btn'>
          <Button onClick={handleLogout} className='logout-button'>
            退出登录
          </Button>
        </View>

        <View className='version'>
          <Text>AquaRush 配送版 v1.0.0</Text>
        </View>
      </View>
    </PageContainer>
  )
}

export default MyPage
