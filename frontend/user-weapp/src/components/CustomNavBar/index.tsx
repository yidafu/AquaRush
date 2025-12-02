import React, { useState } from '@tarojs/react'
import { View, Input, Image } from '@tarojs/components'
import { AtIcon } from 'taro-ui'
import './styles.scss'

interface CustomNavBarProps {
  onSearch?: (query: string) => void
  onProfileClick?: () => void
  onCategoryToggle?: () => void
  placeholder?: string
  showUserAvatar?: boolean
}

const CustomNavBar: React.FC<CustomNavBarProps> = ({
  onSearch = () => {},
  onProfileClick = () => {},
  onCategoryToggle = () => {},
  placeholder = '搜索商品',
  showUserAvatar = true
}) => {
  const [searchQuery, setSearchQuery] = useState('')
  const [isSearchFocused, setIsSearchFocused] = useState(false)

  const handleSearch = (value: string) => {
    setSearchQuery(value)
    onSearch(value)
  }

  const handleSearchFocus = () => {
    setIsSearchFocused(true)
  }

  const handleSearchBlur = () => {
    setIsSearchFocused(false)
  }

  return (
    <View className='custom-nav-bar'>
      {/* Logo/Brand Area */}
      <View className='nav-logo'>
        <View className='logo-icon'>💧</View>
        <View className='logo-text'>AquaRush</View>
      </View>

      {/* Search Bar */}
      <View className={`search-container ${isSearchFocused ? 'focused' : ''}`}>
        <AtIcon
          value='search'
          size='16'
          color='var(--theme-text-tertiary)'
          className='search-icon'
        />
        <Input
          className='search-input'
          placeholder={placeholder}
          value={searchQuery}
          onInput={(e) => handleSearch(e.detail.value)}
          onFocus={handleSearchFocus}
          onBlur={handleSearchBlur}
          confirmType='search'
          onConfirm={() => handleSearch(searchQuery)}
        />
      </View>

      {/* Category Toggle */}
      <View className='category-toggle' onClick={onCategoryToggle}>
        <AtIcon
          value='menu'
          size='20'
          color='var(--theme-text-secondary)'
        />
      </View>

      {/* User Avatar */}
      {showUserAvatar && (
        <View className='user-avatar' onClick={onProfileClick}>
          <View className='avatar-placeholder'>
            <AtIcon
              value='user'
              size='16'
              color='var(--theme-primary)'
            />
          </View>
        </View>
      )}
    </View>
  )
}

export default CustomNavBar