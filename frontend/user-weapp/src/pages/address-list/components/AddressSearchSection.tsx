import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtSearchBar, AtButton } from 'taro-ui'

import "taro-ui/dist/style/components/search-bar.scss"
import "taro-ui/dist/style/components/button.scss"
import './AddressSearchSection.scss'

interface AddressSearchSectionProps {
  searchValue: string
  onSearch: (value: string) => void
  onSearchClear: () => void
  onWechatImport: () => void
}

/**
 * 搜索栏(sticky 顶部) + 微信地址导入按钮
 */
const AddressSearchSection: React.FC<AddressSearchSectionProps> = ({
  searchValue,
  onSearch,
  onSearchClear,
  onWechatImport
}) => {
  return (
    <>
      {/* 搜索栏 */}
      <View className='search-section'>
        <AtSearchBar
          value={searchValue}
          onChange={onSearch}
          onClear={onSearchClear}
          placeholder='搜索手机号或地址'
          showActionButton={false}
        />
      </View>

      {/* 微信地址导入按钮 */}
      <View className='flex justify-end import-section'>
        <AtButton
          type='secondary'
          size='small'
          onClick={onWechatImport}
          className='import-button'
        >
          <Text>导入微信地址</Text>
        </AtButton>
      </View>
    </>
  )
}

export default AddressSearchSection
