import React from 'react'
import { View, Text } from '@tarojs/components'
import './index.scss'

interface ContactItemProps {
  label: string
  value: string
}

const ContactItem: React.FC<ContactItemProps> = ({ label, value }) => {
  return (
    <View className='contact-item'>
      <Text className='contact-label'>{label}</Text>
      <Text className='contact-value'>{value}</Text>
    </View>
  )
}

export default ContactItem