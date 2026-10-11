import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'

interface ProfileHeaderProps {
  hasChanges: boolean
  onCancel: () => void
  onSave: () => void
}

/**
 * 页面头部:取消 / 保存(profile-edit.html:保存按钮 weui-btn_mini primary,
 * 未修改时置灰禁用;保留 track* 埋点)
 *
 * 样式:StyleX(原 ProfileHeader.scss 迁移,750 基准 rpx;weui-* 保留)
 */
const styles = stylex.create({
  pageHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: '16rpx',
    paddingRight: '32rpx',
    paddingBottom: '16rpx',
    paddingLeft: '32rpx',
    backgroundColor: 'var(--weui-BG-0)',
  },
  cancelBtn: {
    fontSize: '28rpx',
    color: 'var(--weui-FG-1)',
    ':active': {
      opacity: 0.6,
    },
  },
  saveBtn: {
    minWidth: '120rpx',
  },
})

const ProfileHeader: React.FC<ProfileHeaderProps> = ({ hasChanges, onCancel, onSave }) => {
  return (
    <View {...stylex.props(styles.pageHeader)}>
      <View onClick={onCancel}>
        <Text {...stylex.props(styles.cancelBtn)}>取消</Text>
      </View>
      <View>
        <Text
          trackTarget="profile-save"
          trackAction="FORM_SUBMIT"
          trackElementType="button"
          trackElementText="保存"
          trackDisabled={!hasChanges}
          onClick={hasChanges ? onSave : undefined}
          {...props(
            styles.saveBtn,
            'weui-btn',
            'weui-btn_mini',
            hasChanges ? 'weui-btn_primary' : 'weui-btn_disabled'
          )}
        >
          保存
        </Text>
      </View>
    </View>
  )
}

export default ProfileHeader
