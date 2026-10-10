import React from 'react'
import { View, Text, Input } from '@tarojs/components'

import './AddressSearchSection.scss'

interface AddressSearchSectionProps {
  searchValue: string
  /** 地址总数(展示「共 N 个地址」) */
  count: number
  onSearch: (value: string) => void
  onSearchClear: () => void
  onWechatImport: () => void
}

/**
 * 搜索栏(address-list.html .weui-searchbar,sticky 顶部)+
 * 统计/导入行(「共 N 个地址」+ 导入微信地址 .weui-btn_plain-default mini)
 */
const AddressSearchSection: React.FC<AddressSearchSectionProps> = ({
  searchValue,
  count,
  onSearch,
  onSearchClear,
  onWechatImport
}) => {
  return (
    <>
      {/* 搜索栏 */}
      <View className='search-section'>
        <View className='weui-searchbar'>
          <View className='weui-searchbar__box start address-search-box'>
            <View className='address-search-icon' />
            <Input
              className='address-search-input'
              value={searchValue}
              onInput={(e) => onSearch(e.detail.value)}
              placeholder='搜索手机号或地址'
            />
            {!!searchValue && (
              <View
                className='address-search-clear'
                onClick={onSearchClear}
              >
                ×
              </View>
            )}
          </View>
        </View>
      </View>

      {/* 统计 + 微信地址导入(address-list.html .mp-pad-16 row between) */}
      <View className='mp-pad-16 import-row'>
        <Text className='address-count'>共 {count} 个地址</Text>
        <View
          className='weui-btn weui-btn_plain-default weui-btn_mini'
          trackTarget='address-import-wechat'
          trackElementType='button'
          trackElementText='导入微信地址'
          onClick={onWechatImport}
        >
          导入微信地址
        </View>
      </View>
    </>
  )
}

export default AddressSearchSection
