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
    <AtCard title='常用服务' className='p-4 mb-4'>
      <View className='at-row'>
        {serviceItems.map((item, index) => (
          <View
            key={index}
            className='flex flex-col items-center at-col-3'
            onClick={item.onClick}
          >
            <View className='flex items-center justify-center w-12 h-12 mb-3 rounded-xl bg-primary-light bg-opacity-10'>
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
