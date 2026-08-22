import React from 'react'
import { View, Text, Picker } from '@tarojs/components'
import { AtList, AtListItem, AtActivityIndicator } from 'taro-ui'
import { Region } from '../../../types/region'

import "taro-ui/dist/style/components/list.scss"
import "taro-ui/dist/style/components/activity-indicator.scss"
import './RegionPickerField.scss'

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
 * 省市区三级联动选择器(地区级联加载,加载中禁用并展示 ActivityIndicator)
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
  return (
    <>
      {/* 地区选择 */}
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
        >
          <AtList>
            <AtListItem
              title='所在地区'
              extraText={getFullRegionText() || '请选择省市区'}
              arrow='right'
            />
          </AtList>
        </Picker>
      ) : (
        <AtList>
          <AtListItem
            title='所在地区'
            extraText='正在加载地区数据...'
            arrow='right'
            disabled={true}
          />
        </AtList>
      )}
      {error && (
        <Text className='input-hint error-text'>{error}</Text>
      )}

      {/* Loading indicator for regions */}
      {(loading.provinces || loading.cities || loading.districts) && (
        <View className='region-loading'>
          <AtActivityIndicator content='加载地区数据...' />
        </View>
      )}
    </>
  )
}

export default RegionPickerField
