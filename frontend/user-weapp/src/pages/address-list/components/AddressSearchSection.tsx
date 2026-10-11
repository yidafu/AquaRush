import React from 'react'
import { View, Text, Input } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'

// 搜索栏 + 统计导入行(原 AddressSearchSection.scss;后代选择器拆到各子元素,
// 长度保持 scss 来源 750 基准 rpx)
const styles = stylex.create({
  // sticky 顶部白底搜索栏
  searchSection: {
    position: 'sticky',
    top: 0,
    zIndex: 100,
    backgroundColor: 'var(--weui-BG-2)',
  },
  // 放大镜图标圆环
  searchIcon: {
    width: '28rpx',
    height: '28rpx',
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto',
    borderWidth: '3rpx',
    borderStyle: 'solid',
    borderColor: 'var(--weui-FG-2)',
    borderRadius: '50%',
    position: 'relative',
    '::after': {
      content: '""',
      position: 'absolute',
      right: '-8rpx',
      bottom: '-6rpx',
      width: '12rpx',
      height: '3rpx',
      backgroundColor: 'var(--weui-FG-2)',
      borderRadius: '2rpx',
      transform: 'rotate(45deg)',
    },
  },
  searchInput: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: 'auto',
    minWidth: 0,
    height: '48rpx',
    lineHeight: '48rpx',
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-0)',
  },
  searchClear: {
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto',
    width: '40rpx',
    height: '40rpx',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '32rpx',
    color: 'var(--weui-FG-2)',
  },
  // 统计 + 导入行(原 .import-row,水平内边距同 mp-pad-16,一并收进 stylex)
  importRow: {
    paddingTop: '16rpx',
    paddingRight: '32rpx',
    paddingBottom: '16rpx',
    paddingLeft: '32rpx',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  addressCount: {
    fontSize: '24rpx',
    color: 'var(--weui-FG-2)',
  },
})

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
      <View {...stylex.props(styles.searchSection)}>
        <View className='weui-searchbar'>
          <View className='weui-searchbar__box start'>
            <View {...stylex.props(styles.searchIcon)} />
            <Input
              {...stylex.props(styles.searchInput)}
              value={searchValue}
              onInput={(e) => onSearch(e.detail.value)}
              placeholder='搜索手机号或地址'
            />
            {!!searchValue && (
              <View
                {...stylex.props(styles.searchClear)}
                onClick={onSearchClear}
              >
                ×
              </View>
            )}
          </View>
        </View>
      </View>

      {/* 统计 + 微信地址导入(address-list.html .mp-pad-16 row between) */}
      <View {...stylex.props(styles.importRow)}>
        <Text {...stylex.props(styles.addressCount)}>共 {count} 个地址</Text>
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
