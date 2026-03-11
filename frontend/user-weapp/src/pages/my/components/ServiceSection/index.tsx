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
  onClick: () => void
}

interface ServiceSectionProps {
  // 不再需要外部传入serviceItems
}

const ServiceSection: React.FC<ServiceSectionProps> = () => {
  // 在组件内部定义所有服务项目
  const serviceItems: ServiceItem[] = React.useMemo(() => [
    {
      icon: '/assets/icons/service/bucket.png',
      title: '押桶管理',
      description: '押桶与退还',
      onClick: () => {
        Taro.navigateTo({
          url: '/pages/bucket-deposit/list/index'
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
      icon: '/assets/icons/service/info-circle.png',
      title: '关于我们',
      description: '了解好喝山泉',
      onClick: () => {
        Taro.navigateTo({
          url: '/pages/about/index'
        })
      }
    }
  ], [])
  return (
    <AtCard title='常用服务' className='p-4 mb-4'>
      <View className='at-row at-row--wrap'>
        {serviceItems.map((item, index) => (
          <View
            key={index}
            className='flex flex-col items-center my-2 at-col-3'
            onClick={item.onClick}
          >
            <View className='flex items-center justify-center w-6 h-6 mb-1 rounded-xl bg-primary-light bg-opacity-10'>
              <CustomIcon value={item.icon} size={24} color='var(--theme-primary)' />
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
