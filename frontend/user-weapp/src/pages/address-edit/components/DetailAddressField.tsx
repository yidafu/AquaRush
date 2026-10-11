import React from 'react'
import { View, Text, Textarea } from '@tarojs/components'

import './fields.scss'

interface DetailAddressFieldProps {
  value: string
  error?: string
  onChange: (value: string) => void
}

/**
 * 详细地址输入项(address-edit.html:weui-cell_vertical + .weui-textarea,
 * 街道、门牌号、小区、楼号等,5-100 字符)
 */
const DetailAddressField: React.FC<DetailAddressFieldProps> = ({ value, error, onChange }) => {
  return (
    <>
      {/* 详细地址 */}
      <View className='weui-cell weui-cell_vertical address-field-cell'>
        <View className='weui-label weui-label--req'>详细地址</View>
        <Textarea
          className='weui-textarea detail-textarea'
          placeholderClass='ph'
          placeholder='街道、门牌号、小区、楼号等详细信息'
          value={value}
          onInput={(e) => onChange(e.detail.value)}
          maxlength={100}
        />
        {!error && (
          <Text className='field-hint'>请输入详细的收货地址，不少于5个字符</Text>
        )}
      </View>
      {error && (
        <View className='field-error'>
          <Text>{error}</Text>
        </View>
      )}
    </>
  )
}

export default DetailAddressField
