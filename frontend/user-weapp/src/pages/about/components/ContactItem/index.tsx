import React from 'react'
import { View, Text } from '@tarojs/components'

interface ContactItemProps {
  label: string
  value: string
  /** 值是否为可点链接形态(客服电话,--weui-LINK 蓝) */
  link?: boolean
}

/** 联系我们行(about.html .weui-cell:左标签 + 右值,链接值用 --weui-LINK) */
const ContactItem: React.FC<ContactItemProps> = ({ label, value, link = false }) => {
  return (
    <View className='weui-cell'>
      <View className='weui-cell__bd'>{label}</View>
      <View className={`weui-cell__ft contact-value ${link ? 'contact-value-link' : ''}`}>
        <Text>{value}</Text>
      </View>
    </View>
  )
}

export default ContactItem
