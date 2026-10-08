import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import CustomIcon from '@/components/CustomIcon'
import { authService, type UserInfo } from '@/utils/auth'

import './index.scss'

interface UserSectionProps {
  userInfo: UserInfo | null
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
  const avatarText = (userInfo.nickname || '客').slice(0, 1)

  return (
    <View className='user-section'>
      <View
        className={`user-info ${!isLoggedIn ? 'not-logged-in' : ''}`}
        onClick={onProfileEdit}
      >
        <View className='avatar-section'>
          {userInfo.avatarUrl ? (
            <Image
              src={userInfo.avatarUrl}
              className='user-avatar'
              mode='aspectFill'
            />
          ) : (
            <View className='default-avatar'>
              <Text className='default-avatar-text'>{avatarText}</Text>
            </View>
          )}
        </View>

        <View className='user-details'>
          <View className='user-name-section'>
            <Text className='user-name'>{userInfo.nickname}</Text>
          </View>
          {userInfo.phone && (
            <Text className='user-phone'>{userInfo.phone}</Text>
          )}
          {!isLoggedIn && (
            <Text className='login-hint'>点击头像登录</Text>
          )}
        </View>

        <CustomIcon value='chevron-right' size={16} color='rgba(255,255,255,0.7)' />
      </View>
    </View>
  )
}

export default UserSection
