import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtCard } from 'taro-ui'
import CustomIcon from '@/components/CustomIcon'

import "taro-ui/dist/style/components/card.scss"

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
    <AtCard title='常用服务' className='mb-4 p-4'>
      <View className='at-row'>
        {serviceItems.map((item, index) => (
          <View
            key={index}
            className='at-col-3'
            onClick={item.onClick}
          >
            <View className='mb-3 flex items-center justify-center w-12 h-12 rounded-xl bg-primary-light bg-opacity-10'>
              <CustomIcon value={item.icon} size={24} color='var(--theme-primary)' />
            </View>
            <Text className='text-2xl font-semibold text-primary text-center mb-1'>{item.title}</Text>
            <Text className='text-xs text-secondary text-center leading-4'>{item.description}</Text>
          </View>
        ))}
      </View>
    </AtCard>
  )
}

export default ServiceSection
