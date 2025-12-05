import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtAvatar } from 'taro-ui'
import CustomIcon from '@/components/CustomIcon'
import { authService, type AuthUserInfo } from '@/utils/auth'

import "taro-ui/dist/style/components/avatar.scss"
import './index.scss'

interface UserSectionProps {
  userInfo: AuthUserInfo | null
  onProfileEdit: () => void
}

const UserSection: React.FC<UserSectionProps> = ({ userInfo, onProfileEdit }) => {
  if (!userInfo) {
    return (
      <View className='user-section'>
        <Text>加载中...</Text>
      </View>
    )
  }

  const isLoggedIn = authService.isAuthenticated()

  return (
    <View className='user-section'>
      <View
        className={`user-info ${!isLoggedIn ? 'not-logged-in' : ''}`}
        onClick={onProfileEdit}
      >
        <View className='avatar-section'>
          {userInfo.avatarUrl ? (
            <AtAvatar
              image={userInfo.avatarUrl}
              className='user-avatar'
              circle
            />
          ) : (
            <AtAvatar
              size='large'
              circle
              text={userInfo.nickname ?? '游客'}
              className='default-avatar'
            />
          )}
        </View>

        <View className='user-details'>
          <View className='user-name-section'>
            <Text className='user-name'>{userInfo.nickname}</Text>
            <View className='user-level'>
              <CustomIcon value='bookmark' size={12} color='#ff6b35' />
              <Text className='level-text'>普通用户</Text>
            </View>
          </View>
          {userInfo.phone && (
            <Text className='user-phone'>{userInfo.phone}</Text>
          )}
          {!isLoggedIn && (
            <Text className='login-hint'>点击头像登录</Text>
          )}
        </View>

        <CustomIcon value='chevron-right' size={16} color='#999' />
      </View>
    </View>
  )
}

export default UserSection
