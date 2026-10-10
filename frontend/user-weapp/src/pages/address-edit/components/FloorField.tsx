import React from 'react'
import { View, Text, Input } from '@tarojs/components'

import './fields.scss'

interface FloorFieldProps {
  value: string
  error?: string
  onChange: (floor: number | undefined) => void
}

/**
 * 楼层输入项(address-edit.html:weui-cell + .weui-label + .weui-input;
 * 选填,配送费按楼层计算:免费楼层内免运费,超过免费楼层加收运费 —— R-MON-02)
 */
const FloorField: React.FC<FloorFieldProps> = ({ value, error, onChange }) => {
  return (
    <>
      {/* 楼层(配送费判定用:超过免费楼层加收运费) */}
      <View className='weui-cell address-field-cell'>
        <View className='weui-cell__hd'>
          <View className='weui-label'>楼层</View>
        </View>
        <View className='weui-cell__bd'>
          <Input
            className='weui-input'
            placeholderClass='ph'
            type='number'
            placeholder='选填,如 5'
            value={value}
            onInput={(e) => {
              const str = e.detail.value
              const floor = str.trim() === '' ? undefined : parseInt(str, 10)
              onChange(floor === undefined || isNaN(floor) ? undefined : floor)
            }}
          />
        </View>
      </View>
      <View className='weui-cells__tips field-tip'>配送费按楼层计算:免费楼层内免运费,超过免费楼层加收运费</View>
      {error && (
        <View className='field-error'>
          <Text>{error}</Text>
        </View>
      )}
    </>
  )
}

export default FloorField
