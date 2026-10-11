import React from 'react'
import { View, Text, Picker } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { Region } from '../../../types/region'

import { fieldStyles } from './fields.stylex'

// 地区选择单元格(原 RegionPickerField.scss;长度保持 scss 来源 750 基准 rpx)
const styles = stylex.create({
  regionPlaceholder: {
    color: 'var(--weui-FG-2)',
  },
})

interface RegionPickerFieldProps {
  provinces: Region[]
  cities: Region[]
  districts: Region[]
  selectedIndex: {
    province: number
    city: number
    district: number
  }
  loading: {
    provinces: boolean
    cities: boolean
    districts: boolean
  }
  getFullRegionText: () => string
  handleColumnChange: (event: any) => Promise<void>
  handlePickerChange: (event: any) => void
  error?: string
}

/**
 * 省市区三级联动选择器(address-edit.html:weui-cell + .weui-cell__ft_in-access 地区文案;
 * 地区级联加载,加载中禁用并展示加载提示)
 */
const RegionPickerField: React.FC<RegionPickerFieldProps> = ({
  provinces,
  cities,
  districts,
  selectedIndex,
  loading,
  getFullRegionText,
  handleColumnChange,
  handlePickerChange,
  error
}) => {
  const regionLoading = loading.provinces || loading.cities || loading.districts

  return (
    <>
      {/* 地区选择(Picker 包裹 weui-cell,级联加载逻辑不变) */}
      {provinces.length > 0 ? (
        <Picker
          mode='multiSelector'
          range={[
            provinces.map(p => p.name),
            cities.map(c => c.name),
            districts.map(d => d.name)
          ]}
          value={[
            selectedIndex.province,
            selectedIndex.city,
            selectedIndex.district
          ]}
          onColumnChange={handleColumnChange}
          onChange={handlePickerChange}
          disabled={regionLoading}
        >
          <View className='weui-cell'>
            <View className='weui-cell__hd'>
              <View className='weui-label weui-label--req'>所在地区</View>
            </View>
            <View className='weui-cell__bd' />
            <View {...props('weui-cell__ft weui-cell__ft_in-access', !getFullRegionText() && styles.regionPlaceholder)}>
              <Text>{regionLoading ? '正在加载地区数据...' : (getFullRegionText() || '请选择省市区')}</Text>
            </View>
          </View>
        </Picker>
      ) : (
        <View className='weui-cell'>
          <View className='weui-cell__hd'>
            <View className='weui-label weui-label--req'>所在地区</View>
          </View>
          <View className='weui-cell__bd' />
          <View {...props('weui-cell__ft weui-cell__ft_in-access', styles.regionPlaceholder)}>
            <Text>正在加载地区数据...</Text>
          </View>
        </View>
      )}
      {error && (
        <View {...props(fieldStyles.fieldError)}>
          <Text>{error}</Text>
        </View>
      )}
    </>
  )
}

export default RegionPickerField
