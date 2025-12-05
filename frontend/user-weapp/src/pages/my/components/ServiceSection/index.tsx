import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtCard } from 'taro-ui'
import CustomIcon from '@/components/CustomIcon'

import "taro-ui/dist/style/components/card.scss"
import './index.scss'

interface ServiceItem {
  icon: string
  title: string
  description: string
  onClick: () => void
}

interface ServiceSectionProps {
  serviceItems: ServiceItem[]
}

const ServiceSection: React.FC<ServiceSectionProps> = ({ serviceItems }) => {
  return (
    <AtCard title='常用服务' className='service-section'>
      <View className='at-row'>
        {serviceItems.map((item, index) => (
          <View
            key={index}
            className='at-col-3 service-item'
            onClick={item.onClick}
          >
            <View className='service-icon'>
              <CustomIcon value={item.icon} size={24} color='var(--theme-primary)' />
            </View>
            <Text className='service-title'>{item.title}</Text>
            <Text className='service-desc'>{item.description}</Text>
          </View>
        ))}
      </View>
    </AtCard>
  )
}

export default ServiceSection
