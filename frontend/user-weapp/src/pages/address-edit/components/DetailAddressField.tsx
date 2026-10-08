import React from 'react'
import { Text } from '@tarojs/components'
import { AtInput } from 'taro-ui'

import "taro-ui/dist/style/components/input.scss"
import './fields.scss'

interface DetailAddressFieldProps {
  value: string
  error?: string
  onChange: (value: string) => void
}

/**
 * 详细地址输入项(街道、门牌号、小区、楼号等,5-100 字符)
 */
const DetailAddressField: React.FC<DetailAddressFieldProps> = ({ value, error, onChange }) => {
  return (
    <>
      {/* 详细地址 */}
      <AtInput
        name='detailAddress'
        title='详细地址'
        type='text'
        placeholder='街道、门牌号、小区、楼号等详细信息'
        value={value}
        onChange={(v) => onChange(v as string)}
        maxlength={100}
        error={!!error}
      />
      {error ? (
        <Text className='input-hint error-text'>{error}</Text>
      ) : (
        <Text className='input-hint'>请输入详细的收货地址，不少于5个字符</Text>
      )}
    </>
  )
}

export default DetailAddressField
