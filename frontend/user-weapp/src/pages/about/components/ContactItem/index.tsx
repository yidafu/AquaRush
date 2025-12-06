import React from 'react'
import { View, Text } from '@tarojs/components'

interface ContactItemProps {
  label: string
  value: string
}

const ContactItem: React.FC<ContactItemProps> = ({ label, value }) => {
  return (
    <View className='flex justify-between items-center py-6 border-b border-border'>
      <Text className='contact-label'>{label}</Text>
      <Text className='contact-value'>{value}</Text>
    </View>
  )
}

export default ContactItem