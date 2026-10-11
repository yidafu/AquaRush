import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { props } from '@aquarush/common/stylex'
import { gradientBox } from '@/styles/theme.stylex'
import CustomIcon from '@/components/CustomIcon'
import { authService, type UserInfo } from '@/utils/auth'

interface UserSectionProps {
  userInfo: UserInfo | null
  onProfileEdit: () => void
}

/**
 * 样式:StyleX(原 index.scss 迁移,长度值保持 750 基准 rpx)
 * 渐变头部原为 theme.scss 品牌渐变的双写(标准 135deg + 过时的 webkit 前缀),
 * 迁移统一用 gradientBox(tokens.brandGradient, 135deg)。
 * 原 `.user-info.not-logged-in` 复合选择器拍平为 userInfoNotLoggedIn 独立类;
 * 原 `.default-avatar .default-avatar-text` 后代选择器拍平到 Text 子元素。
 */
const styles = stylex.create({
  // 用户信息区(my.html:渐变头部,品牌渐变 + 白字 + 右上装饰圆)
  userSection: {
    paddingTop: '32rpx',
    paddingRight: '32rpx',
    paddingBottom: '32rpx',
    paddingLeft: '32rpx',
    color: tokens.onBrand,
    position: 'relative',
    overflow: 'hidden',
    // 原型 .hero::before/::after 装饰圆(静态)
    '::before': {
      content: '',
      position: 'absolute',
      right: '-80rpx',
      top: '-60rpx',
      width: '340rpx',
      height: '340rpx',
      borderRadius: '50%',
      backgroundColor: 'rgba(255, 255, 255, 0.10)',
    },
    '::after': {
      content: '',
      position: 'absolute',
      right: '80rpx',
      top: '120rpx',
      width: '180rpx',
      height: '180rpx',
      borderRadius: '50%',
      backgroundColor: 'rgba(255, 255, 255, 0.07)',
    },
  },
  userInfo: {
    display: 'flex',
    alignItems: 'center',
    rowGap: '24rpx',
    columnGap: '24rpx',
    position: 'relative',
    zIndex: 1,
    cursor: 'pointer',
    transitionProperty: 'all',
    transitionDuration: '0.2s',
    transitionTimingFunction: 'ease',
  },
  userInfoNotLoggedIn: {
    borderWidth: '2rpx',
    borderStyle: 'dashed',
    borderColor: 'rgba(255, 255, 255, 0.4)',
    borderRadius: tokens.rCard,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    paddingTop: '16rpx',
    paddingRight: '16rpx',
    paddingBottom: '16rpx',
    paddingLeft: '16rpx',
    ':active': {
      transform: 'scale(0.98)',
    },
  },
  avatarSection: {
    position: 'relative',
    flexShrink: 0,
  },
  // 头像:圆 96rpx(原型 48px ×2),白描边(.user-avatar / .default-avatar 共用)
  userAvatar: {
    width: '96rpx',
    height: '96rpx',
    borderRadius: '50%',
    borderWidth: '4rpx',
    borderStyle: 'solid',
    borderColor: 'rgba(255, 255, 255, 0.35)',
    boxSizing: 'border-box',
  },
  defaultAvatar: {
    width: '96rpx',
    height: '96rpx',
    borderRadius: '50%',
    borderWidth: '4rpx',
    borderStyle: 'solid',
    borderColor: 'rgba(255, 255, 255, 0.35)',
    boxSizing: 'border-box',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255, 255, 255, 0.24)',
  },
  defaultAvatarText: {
    fontSize: '40rpx',
    fontWeight: 600,
    color: tokens.onBrand,
    lineHeight: 1,
  },
  userDetails: {
    flexGrow: 1,
    flexShrink: 1,
    display: 'flex',
    flexDirection: 'column',
    rowGap: '8rpx',
    columnGap: '8rpx',
    minWidth: 0,
  },
  userNameSection: {
    display: 'flex',
    alignItems: 'center',
    rowGap: '12rpx',
    columnGap: '12rpx',
  },
  // 原型 20px ×2 = 40rpx,600 白字
  userName: {
    fontSize: '40rpx',
    fontWeight: 600,
    color: tokens.onBrand,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  userPhone: {
    fontSize: '26rpx',
    opacity: 0.88,
  },
  loginHint: {
    fontSize: '22rpx',
    color: 'rgba(255, 255, 255, 0.9)',
    backgroundColor: 'rgba(0, 0, 0, 0.2)',
    paddingTop: '4rpx',
    paddingRight: '12rpx',
    paddingBottom: '4rpx',
    paddingLeft: '12rpx',
    borderRadius: tokens.rSm,
    display: 'inline-block',
    alignSelf: 'flex-start',
  },
})

const UserSection: React.FC<UserSectionProps> = ({ userInfo, onProfileEdit }) => {
  if (!userInfo) {
    return (
      <View {...stylex.props(gradientBox.gradient, styles.userSection)}>
        <Text>加载中...</Text>
      </View>
    )
  }

  const isLoggedIn = authService.isAuthenticated()
  const avatarText = (userInfo.nickname || '客').slice(0, 1)

  return (
    <View {...stylex.props(gradientBox.gradient, styles.userSection)}>
      <View
        {...props(styles.userInfo, !isLoggedIn && styles.userInfoNotLoggedIn)}
        onClick={onProfileEdit}
      >
        <View {...stylex.props(styles.avatarSection)}>
          {userInfo.avatarUrl ? (
            <Image
              src={userInfo.avatarUrl}
              {...stylex.props(styles.userAvatar)}
              mode='aspectFill'
            />
          ) : (
            <View {...stylex.props(styles.defaultAvatar)}>
              <Text {...stylex.props(styles.defaultAvatarText)}>{avatarText}</Text>
            </View>
          )}
        </View>

        <View {...stylex.props(styles.userDetails)}>
          <View {...stylex.props(styles.userNameSection)}>
            <Text {...stylex.props(styles.userName)}>{userInfo.nickname}</Text>
          </View>
          {userInfo.phone && (
            <Text {...stylex.props(styles.userPhone)}>{userInfo.phone}</Text>
          )}
          {!isLoggedIn && (
            <Text {...stylex.props(styles.loginHint)}>点击头像登录</Text>
          )}
        </View>

        <CustomIcon value='chevron-right' size={16} color='rgba(255,255,255,0.7)' />
      </View>
    </View>
  )
}

export default UserSection
