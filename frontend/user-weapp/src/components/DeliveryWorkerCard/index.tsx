import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import StatusTag from '../StatusTag'

/**
 * 样式:StyleX(原 index.scss 迁移,长度值保持 750 基准 rpx;weui-* 平台变量保留字符串引用;
 * 原 `.worker-card .weui-cell` 后代选择器拍平为元素级 style,worker-card 锚点类无自有样式已删)
 */
const styles = stylex.create({
  workerAvatar: {
    width: '96rpx',
    height: '96rpx',
    borderRadius: '50%',
    backgroundColor: 'var(--weui-BRAND-100)',
    overflow: 'hidden',
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto',
  },
  workerNameRow: {
    display: 'flex',
    alignItems: 'center',
    columnGap: '16rpx',
  },
  workerName: {
    fontSize: 'var(--weui-fs-cell)',
    fontWeight: 600,
    color: 'var(--weui-FG-0)',
  },
  workerMeta: {
    display: 'block',
    marginTop: '12rpx',
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
  },
  workerEta: {
    color: 'var(--weui-FG-0)',
    fontWeight: 600,
  },
})

/**
 * 配送员信息卡(配送中订单)—— WeUI 2.x 视觉(.weui-cells 单元格:
 * 头像 + 姓名 + 配送中标签(.weui-tag--deliver)+ 电话/位置行 + 右侧操作按钮,
 * 设计稿 02-order/order-detail.html 配送员信息卡口径)。
 * 收敛 order-detail 与 delivery-tracking 两处同名实现;状态标签走共享 StatusTag
 * (tone 色值见 common STATUS_TONE_COLORS)。
 */
interface DeliveryWorkerCardProps {
  name?: string
  phone?: string
  avatar?: string
  /** 当前位置文案 */
  location?: string
  /** 距离文案(如「500m」) */
  distance?: string
  /** 展示配送中状态行(姓名旁标签 + 预计送达行) */
  showStatus?: boolean
  /** 预计送达文案 */
  estimatedDeliveryTime?: string
  onContact: () => void
  onTrack?: () => void
}

const DeliveryWorkerCard: React.FC<DeliveryWorkerCardProps> = ({
  name,
  phone,
  avatar,
  location,
  distance,
  showStatus = false,
  estimatedDeliveryTime,
  onContact,
  onTrack
}) => {
  return (
    <View>
      <View className='weui-cells'>
        <View className='weui-cell'>
          <View className='weui-cell__hd'>
            <Image
              src={avatar || '/assets/default-avatar.png'}
              mode='aspectFill'
              {...stylex.props(styles.workerAvatar)}
            />
          </View>
          <View className='weui-cell__bd'>
            <View {...stylex.props(styles.workerNameRow)}>
              <Text {...stylex.props(styles.workerName)}>{name || '配送员'}</Text>
              {showStatus && <StatusTag tone='success' text='配送中' size='small' />}
            </View>
            <Text {...stylex.props(styles.workerMeta)}>
              {phone ? `电话 ${phone}` : ''}
              {location ? ` · ${location}` : ''}
              {distance ? ` · 距你约 ${distance}` : ''}
            </Text>
          </View>
          <View className='weui-cell__ft'>
            {onTrack && (
              <View
                className='weui-btn weui-btn_plain-default weui-btn_xs'
                onClick={onTrack}
              >
                查看跟踪
              </View>
            )}
            {onContact && (
              <View
                className='weui-btn weui-btn_plain-primary weui-btn_xs'
                onClick={onContact}
              >
                联系
              </View>
            )}
          </View>
        </View>

        {showStatus && estimatedDeliveryTime && (
          <View className='weui-cell'>
            <View className='weui-cell__bd'>预计送达</View>
            <View {...props('weui-cell__ft', styles.workerEta)}>{estimatedDeliveryTime}</View>
          </View>
        )}
      </View>
    </View>
  )
}

export default DeliveryWorkerCard
