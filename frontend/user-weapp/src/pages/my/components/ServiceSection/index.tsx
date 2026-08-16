import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtCard } from 'taro-ui'
import CustomIcon from '@/components/CustomIcon'
import Taro from '@tarojs/taro'
import { CONTACT_INFO } from '@/constants'

import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/flex.scss"

interface ServiceItem {
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
  // 在组件内部定义所有服务项目
  const serviceItems: ServiceItem[] = React.useMemo(() => [
    {
      icon: '/assets/icons/service/comments.png',
      title: '消息中心',
      description: '查看消息通知',
      badge: unreadCount,
      onClick: () => {
        Taro.navigateTo({
          url: '/pages/message-center/index'
        })
      }
    },
    {
      icon: '/assets/icons/service/heart.png',
      title: '我的收藏',
      description: '查看收藏商品',
      onClick: () => {
        Taro.navigateTo({
          url: '/pages/favorites/index'
        })
      }
    },
    {
      icon: '/assets/icons/service/heart.png',
      title: '购物车',
      description: '多商品合并结算',
      onClick: () => {
        // 购物车是 tabBar 页,用 switchTab 切换
        Taro.switchTab({
          url: '/pages/cart/index'
        })
      }
    },
    {
      icon: '/assets/icons/service/map-pin.png',
      title: '收货地址',
      description: '管理收货地址',
      onClick: () => {
        Taro.navigateTo({
          url: '/pages/address-list/index'
        })
      }
    },
    {
      icon: '/assets/icons/service/info-circle.png',
      title: '领券中心',
      description: '优惠券免费领',
      onClick: () => {
        Taro.navigateTo({
          url: '/pages/coupon-center/index'
        })
      }
    },
    {
      icon: '/assets/icons/service/heart.png',
      title: '我的水票',
      description: '查看已购水票',
      onClick: () => {
        Taro.navigateTo({
          url: '/pages/ticket-list/index'
        })
      }
    },
    {
      icon: '/assets/icons/service/comments.png',
      title: '客服中心',
      description: '联系在线客服',
      onClick: () => {
        Taro.makePhoneCall({
          phoneNumber: CONTACT_INFO.SERVICE_HOTLINE
        })
      }
    },
    {
      icon: '/assets/icons/service/feedback.png',
      title: '意见反馈',
      description: '帮助我们改进',
      onClick: () => {
        Taro.navigateTo({
          url: '/pages/feedback/index'
        })
      }
    },
    {
      icon: '/assets/icons/service/comments.png',
      title: '反馈记录',
      description: '查看处理结果',
      onClick: () => {
        Taro.navigateTo({
          url: '/pages/feedback-records/index'
        })
      }
    },
    {
      icon: '/assets/icons/service/info-circle.png',
      title: '关于我们',
      description: '了解好喝山泉',
      onClick: () => {
        Taro.navigateTo({
          url: '/pages/about/index'
        })
      }
    }
  ], [unreadCount])
  return (
    <AtCard title='常用服务' className='p-4 mb-4'>
      <View className='at-row at-row--wrap'>
        {serviceItems.map((item, index) => (
          <View
            key={index}
            className='flex flex-col items-center my-2 at-col-3'
            onClick={item.onClick}
          >
            <View className='relative flex items-center justify-center w-6 h-6 mb-1 rounded-xl bg-primary-light bg-opacity-10'>
              <CustomIcon value={item.icon} size={24} color='var(--theme-primary)' />
              {!!item.badge && item.badge > 0 && (
                <View className='absolute -top-1 -right-2 min-w-4 h-4 px-1 rounded-full bg-red-500 flex items-center justify-center'>
                  <Text className='text-[10px] text-white leading-none'>
                    {item.badge > 99 ? '99+' : item.badge}
                  </Text>
                </View>
              )}
            </View>
            <Text className='mb-1 text-sm font-semibold text-center theme-text-primary'>{item.title}</Text>
            <Text className='text-xs leading-4 text-center theme-text-secondary'>{item.description}</Text>
          </View>
        ))}
      </View>
    </AtCard>
  )
}

export default ServiceSection