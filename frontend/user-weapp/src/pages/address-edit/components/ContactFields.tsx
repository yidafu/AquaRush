import React from 'react'
import { View, Text, Input } from '@tarojs/components'
import { AddressFormData } from '../../../types/address'

import './fields.scss'

interface ContactFieldsProps {
  receiverName: string
  phone: string
  errors: Partial<Record<keyof AddressFormData, string>>
  onChange: (field: keyof AddressFormData, value: string | number | boolean) => void
}

/**
 * 收货人 + 手机号输入项(address-edit.html:weui-cell + .weui-label--req + .weui-input)
 */
const ContactFields: React.FC<ContactFieldsProps> = ({ receiverName, phone, errors, onChange }) => {
  return (
    <>
      {/* 收货人 */}
      <View className='weui-cell address-field-cell'>
        <View className='weui-cell__hd'>
          <View className='weui-label weui-label--req'>收货人</View>
        </View>
        <View className='weui-cell__bd'>
          <Input
            className='weui-input'
            placeholderClass='ph'
            type='text'
            placeholder='请输入收货人姓名'
            value={receiverName}
            onInput={(e) => onChange('receiverName', e.detail.value)}
          />
        </View>
      </View>
      {errors.receiverName && (
        <View className='field-error'>
          <Text>{errors.receiverName}</Text>
        </View>
      )}

      {/* 手机号 */}
      <View className='weui-cell address-field-cell'>
        <View className='weui-cell__hd'>
          <View className='weui-label weui-label--req'>手机号</View>
        </View>
        <View className='weui-cell__bd'>
          <Input
            className='weui-input'
            placeholderClass='ph'
            type='number'
            maxlength={11}
            placeholder='请输入手机号'
            value={phone}
            onInput={(e) => onChange('phone', e.detail.value)}
          />
        </View>
      </View>
      {errors.phone && (
        <View className='field-error'>
          <Text>{errors.phone}</Text>
        </View>
      )}
    </>
  )
}

export default ContactFields
