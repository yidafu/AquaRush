import React from 'react'
import { Text } from '@tarojs/components'
import { AtInput } from 'taro-ui'

import "taro-ui/dist/style/components/input.scss"
import './fields.scss'

interface FloorFieldProps {
  value: string
  error?: string
  onChange: (floor: number | undefined) => void
}

/**
 * 楼层输入项(选填;配送费按楼层计算:免费楼层内免运费,超过免费楼层加收运费 —— R-MON-02)
 */
const FloorField: React.FC<FloorFieldProps> = ({ value, error, onChange }) => {
  return (
    <>
      {/* 楼层(配送费判定用:超过免费楼层加收运费) */}
      <AtInput
        name='floor'
        title='楼层'
        type='number'
        placeholder='选填,如 5'
        value={value}
        onChange={(v) => {
          const str = typeof v === 'number' ? v.toString() : v
          const floor = str.trim() === '' ? undefined : parseInt(str, 10)
          onChange(floor === undefined || isNaN(floor) ? undefined : floor)
        }}
        error={!!error}
      />
      <Text className='input-hint'>配送费按楼层计算:免费楼层内免运费,超过免费楼层加收运费</Text>
    </>
  )
}

export default FloorField
