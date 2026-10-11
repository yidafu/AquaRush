import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'

const styles = stylex.create({
  value: {
    color: 'var(--weui-FG-1)',
  },
  valueLink: {
    color: 'var(--weui-LINK)',
  },
})

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
      <View {...props('weui-cell__ft', styles.value, link && styles.valueLink)}>
        <Text>{value}</Text>
      </View>
    </View>
  )
}

export default ContactItem
