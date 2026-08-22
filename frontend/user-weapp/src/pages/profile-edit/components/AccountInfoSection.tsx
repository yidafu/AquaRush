import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtDivider } from 'taro-ui'
import type { UserInfo } from '../../../utils/auth'

import "taro-ui/dist/style/components/divider.scss"
import './AccountInfoSection.scss'

interface AccountInfoSectionProps {
  userInfo: UserInfo | null
}

/**
 * 账户信息只读区块(用户ID/会员等级/账户余额/积分/VIP 到期时间)
 */
const AccountInfoSection: React.FC<AccountInfoSectionProps> = ({ userInfo }) => {
  if (!userInfo) {
    return null
  }

  return (
    <>
      <AtDivider />
      <View className='form-section readonly-section'>
        <Text className='section-title'>账户信息</Text>

        <View className='readonly-field'>
          <Text className='field-label'>用户ID</Text>
          <Text className='field-value'>{userInfo.id}</Text>
        </View>

        <View className='readonly-field'>
          <Text className='field-label'>会员等级</Text>
          <Text className='field-value'>{userInfo.level}</Text>
        </View>

        <View className='readonly-field'>
          <Text className='field-label'>账户余额</Text>
          <Text className='field-value'>¥{userInfo.balance.toFixed(2)}</Text>
        </View>

        <View className='readonly-field'>
          <Text className='field-label'>积分</Text>
          <Text className='field-value'>{userInfo.points}</Text>
        </View>

        {userInfo.isVip && (
          <View className='readonly-field'>
            <Text className='field-label'>VIP到期时间</Text>
            <Text className='field-value'>
              {userInfo.vipExpireTime ? new Date(userInfo.vipExpireTime).toLocaleDateString() : '永久'}
            </Text>
          </View>
        )}
      </View>
    </>
  )
}

export default AccountInfoSection
