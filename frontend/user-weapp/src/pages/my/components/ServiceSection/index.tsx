import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import CustomIcon from '@/components/CustomIcon'
import Taro from '@tarojs/taro'
import { CONTACT_INFO } from '@/constants'

interface ServiceEntry {
  icon: string
  title: string
  /** trackTarget 后缀(编译期声明式埋点) */
  track: string
  badge?: number
  onClick: () => void
}

interface ServiceSectionProps {
  /** 消息中心未读数(入口角标,F-NTF-002) */
  unreadCount?: number
}

/**
 * 「常用服务」(my.html:.weui-cells 8 行):
 * 收货地址 / 购物车 / 领券中心 / 消息中心(未读角标) / 客服中心 / 意见反馈 / 反馈记录 / 关于我们。
 * 收藏 / 优惠券 / 水票 / 水桶已收口进「我的资产」聚合页,此处不再重复。
 *
 * 样式:StyleX(原 index.scss 迁移,长度值保持 750 基准 rpx;weui-* 类保留)
 */
const styles = stylex.create({
  // 「常用服务」weui-cells 行:品牌蓝图标 + 标题 + 箭头(my.html)
  serviceCellIcon: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: 'var(--weui-BRAND)',
  },
  // 消息未读角标(语义红,F-NTF-002;>99 显示 99+)
  serviceCellBadge: {
    position: 'absolute',
    top: '-10rpx',
    left: '28rpx',
  },
})
const ServiceSection: React.FC<ServiceSectionProps> = ({ unreadCount = 0 }) => {
  const serviceEntries: ServiceEntry[] = [
    {
      icon: '/assets/icons/service/map-pin',
      title: '收货地址',
      track: 'address',
      onClick: () => {
        Taro.navigateTo({ url: '/pages/address-list/index' })
      }
    },
    {
      icon: '/assets/icons/service/cart',
      title: '购物车',
      track: 'cart',
      onClick: () => {
        // 购物车已非 tabBar 页(3 Tab 改造后),navigateTo 打开
        Taro.navigateTo({ url: '/pages/cart/index' })
      }
    },
    {
      icon: '/assets/icons/service/coupon',
      title: '领券中心',
      track: 'coupon-center',
      onClick: () => {
        Taro.navigateTo({ url: '/pages/coupon-center/index' })
      }
    },
    {
      icon: '/assets/icons/service/bell',
      title: '消息中心',
      track: 'message-center',
      badge: unreadCount,
      onClick: () => {
        Taro.navigateTo({ url: '/pages/message-center/index' })
      }
    },
    {
      icon: '/assets/icons/service/comments',
      title: '客服中心',
      track: 'hotline',
      onClick: () => {
        Taro.makePhoneCall({ phoneNumber: CONTACT_INFO.SERVICE_HOTLINE })
      }
    },
    {
      icon: '/assets/icons/service/feedback',
      title: '意见反馈',
      track: 'feedback',
      onClick: () => {
        Taro.navigateTo({ url: '/pages/feedback/index' })
      }
    },
    {
      icon: '/assets/icons/service/clipboard',
      title: '反馈记录',
      track: 'feedback-records',
      onClick: () => {
        Taro.navigateTo({ url: '/pages/feedback-records/index' })
      }
    },
    {
      icon: '/assets/icons/service/info-circle',
      title: '关于我们',
      track: 'about',
      onClick: () => {
        Taro.navigateTo({ url: '/pages/about/index' })
      }
    }
  ]

  return (
    <>
      <View className='weui-cells__title'>常用服务</View>
      <View className='weui-cells'>
        {serviceEntries.map((item) => (
          <View
            key={item.title}
            className='weui-cell'
            trackTarget={`my-service-${item.track}`}
            trackElementType='cell'
            trackElementText={item.title}
            onClick={item.onClick}
          >
            <View className='weui-cell__hd'>
              <View {...props('weui-cell__icon', styles.serviceCellIcon)}>
                <CustomIcon value={item.icon} size={24} />
                {!!item.badge && item.badge > 0 && (
                  <View {...props('weui-badge', styles.serviceCellBadge)}>
                    <Text>{item.badge > 99 ? '99+' : item.badge}</Text>
                  </View>
                )}
              </View>
            </View>
            <View className='weui-cell__bd'>
              <Text>{item.title}</Text>
            </View>
            <View className='weui-cell__ft weui-cell__ft_in-access' />
          </View>
        ))}
      </View>
    </>
  )
}

export default ServiceSection
