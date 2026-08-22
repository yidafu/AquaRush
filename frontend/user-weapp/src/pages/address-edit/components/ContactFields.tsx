import React from 'react'
import { Text } from '@tarojs/components'
import { AtInput } from 'taro-ui'
import { AddressFormData } from '../../../types/address'

import "taro-ui/dist/style/components/input.scss"
import './fields.scss'

interface ContactFieldsProps {
  receiverName: string
  phone: string
  errors: Partial<Record<keyof AddressFormData, string>>
  onChange: (field: keyof AddressFormData, value: string | number | boolean) => void
}

/**
 * 收货人 + 手机号输入项(地址编辑页 F-ADR-002)
 */
const ContactFields: React.FC<ContactFieldsProps> = ({ receiverName, phone, errors, onChange }) => {
  return (
    <>
      {/* 收货人 */}
      <AtInput
        name='receiverName'
        title='收货人'
        type='text'
        placeholder='请输入收货人姓名'
        value={receiverName}
        onChange={(value) => onChange('receiverName', value)}
        error={!!errors.receiverName}
      />
      {errors.receiverName && <Text className='input-hint error-text'>{errors.receiverName}</Text>}

      {/* 手机号 */}
      <AtInput
        name='phone'
        title='手机号'
        type='phone'
        placeholder='请输入手机号'
        value={phone}
        onChange={(value) => onChange('phone', value)}
        error={!!errors.phone}
      />
      {errors.phone && <Text className='input-hint error-text'>{errors.phone}</Text>}
    </>
  )
}

export default ContactFields
