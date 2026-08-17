import React from 'react'
import { View, Text } from '@tarojs/components'
import CustomIcon from '@/components/CustomIcon'
import Taro from '@tarojs/taro'
import { CONTACT_INFO } from '@/constants'

import './index.scss'

interface ServiceEntry {
  icon: string
  title: string
  description: string
  badge?: number
  onClick: () => void
}

interface ServiceSectionProps {
  /** 消息中心未读数(入口角标,F-NTF-002) */
  unreadCount?: number
}

const ServiceSection: React.FC<ServiceSectionProps> = ({ unreadCount = 0 }) => {
  // 营销入口卡:优惠券/水票/收藏(§8.3,列表行 行高44)
  const assetEntries: ServiceEntry[] = [
    {
      icon: '/assets/icons/service/heart',
      title: '我的收藏',
      description: '查看收藏商品',
      onClick: () => {
        Taro.navigateTo({ url: '/pages/favorites/index' })
      }
    },
    {
      icon: '/assets/icons/service/coupon',
      title: '优惠券',
      description: '查看我的优惠券',
      onClick: () => {
        Taro.navigateTo({ url: '/pages/coupon-list/index' })
      }
    },
    {
      icon: '/assets/icons/service/ticket',
      title: '我的水票',
      description: '查看已购水票',
      onClick: () => {
        Taro.navigateTo({ url: '/pages/ticket-list/index' })
      }
    }
  ]

  // 服务卡(§8.4):消息中心(未读角标)/收货地址/领券中心/购物车/客服中心/意见反馈/反馈记录/关于
  const serviceEntries: ServiceEntry[] = [
    {
      icon: '/assets/icons/service/bell',
      title: '消息中心',
      description: '查看消息通知',
      badge: unreadCount,
      onClick: () => {
        Taro.navigateTo({ url: '/pages/message-center/index' })
      }
    },
    {
      icon: '/assets/icons/service/map-pin',
      title: '收货地址',
      description: '管理收货地址',
      onClick: () => {
        Taro.navigateTo({ url: '/pages/address-list/index' })
      }
    },
    {
      icon: '/assets/icons/service/coupon',
      title: '领券中心',
      description: '优惠券免费领',
      onClick: () => {
        Taro.navigateTo({ url: '/pages/coupon-center/index' })
      }
    },
    {
      icon: '/assets/icons/service/cart',
      title: '购物车',
      description: '多商品合并结算',
      onClick: () => {
        // 购物车是 tabBar 页,用 switchTab 切换
        Taro.switchTab({ url: '/pages/cart/index' })
      }
    },
    {
      icon: '/assets/icons/service/comments',
      title: '客服中心',
      description: '联系在线客服',
      onClick: () => {
        Taro.makePhoneCall({ phoneNumber: CONTACT_INFO.SERVICE_HOTLINE })
      }
    },
    {
      icon: '/assets/icons/service/feedback',
      title: '意见反馈',
      description: '帮助我们改进',
      onClick: () => {
        Taro.navigateTo({ url: '/pages/feedback/index' })
      }
    },
    {
      icon: '/assets/icons/service/clipboard',
      title: '反馈记录',
      description: '查看处理结果',
      onClick: () => {
        Taro.navigateTo({ url: '/pages/feedback-records/index' })
      }
    },
    {
      icon: '/assets/icons/service/info-circle',
      title: '关于我们',
      description: '了解好喝山泉',
      onClick: () => {
        Taro.navigateTo({ url: '/pages/about/index' })
      }
    }
  ]

  const renderEntry = (item: ServiceEntry): React.ReactElement => (
    <View key={item.title} className='service-row' onClick={item.onClick}>
      <View className='service-icon'>
        <CustomIcon value={item.icon} size={36} />
        {!!item.badge && item.badge > 0 && (
          <View className='service-badge'>
            <Text className='service-badge-text'>
              {item.badge > 99 ? '99+' : item.badge}
            </Text>
          </View>
        )}
      </View>
      <View className='service-info'>
        <Text className='service-title'>{item.title}</Text>
        <Text className='service-desc'>{item.description}</Text>
      </View>
      <CustomIcon value='chevron-right' size={14} color='var(--text-weak)' />
    </View>
  )

  return (
    <>
      <View className='service-card page-card'>
        <View className='section-header'>
          <Text className='section-title'>我的资产</Text>
        </View>
        {assetEntries.map(renderEntry)}
      </View>

      <View className='service-card page-card'>
        <View className='section-header'>
          <Text className='section-title'>常用服务</Text>
        </View>
        {serviceEntries.map(renderEntry)}
      </View>
    </>
  )
}

export default ServiceSection
