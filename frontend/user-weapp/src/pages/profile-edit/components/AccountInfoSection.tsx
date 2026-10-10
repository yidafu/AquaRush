import React from 'react'
import { View, Text } from '@tarojs/components'
import type { UserInfo } from '../../../utils/auth'

import './AccountInfoSection.scss'

interface AccountInfoSectionProps {
  userInfo: UserInfo | null
}

/**
 * 账户信息只读区块(profile-edit.html:weui-cells 只读行,值弱灰)
 * 注:会员等级/余额/积分/VIP 字段已从后端 `me` 查询移除(shared-types.graphqls UserInfo 仅
 * id/wechatOpenId/nickname/phone/avatarUrl/createdAt/updatedAt),原展示会运行期崩溃
 * (balance.toFixed(2)),故移除失效行 —— 余额字段不再规划(用户储值不做,2026-10-09 合规
 * 决策),会员等级/积分随 F-MKT-011/012 落地后恢复。
 */
const AccountInfoSection: React.FC<AccountInfoSectionProps> = ({ userInfo }) => {
  if (!userInfo) {
    return null
  }

  return (
    <View className='weui-cells mt3 profile-cells'>
      <View className='weui-cell'>
        <View className='weui-cell__bd'>
          <Text>用户ID</Text>
        </View>
        <View className='weui-cell__ft profile-readonly-value'>
          <Text>{userInfo.id}</Text>
        </View>
      </View>
    </View>
  )
}

export default AccountInfoSection
