import React, { useState, useEffect } from 'react'
import { View, Text, Image } from '@tarojs/components'
import { useLoad } from '@tarojs/taro'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { pageTheme, gradientBox } from '../../styles/theme.stylex'

// 导入拆分出的组件
import ContentSection from './components/ContentSection'
import FeatureItem from './components/FeatureItem'
import ContactItem from './components/ContactItem'

// 导入常量(店铺信息未配置时兜底展示)
import {
  BRAND_INFO,
  APP_INFO,
  WATER_SOURCE_INFO,
  BRAND_FEATURES} from './constants'
import {
  CONTACT_INFO,
} from '@/constants'
import StoreProfileService from '../../services/StoreProfileService'
import type { StoreProfileVo } from '@aquarush/common'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

/**
 * 关于我们(about.html:品牌渐变 hero + weui-cells 分区:
 * 品牌承诺 media-box / 水源地介绍 / 联系我们 cells / 版权页脚)
 *
 * 样式:StyleX(原 index.scss 迁移,长度值保持 750 基准 rpx;weui-* 类保留)
 */
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    boxSizing: 'border-box',
    backgroundColor: 'var(--weui-BG-0)',
    color: 'var(--weui-FG-0)',
  },
  hero: {
    paddingTop: '56rpx',
    paddingRight: '32rpx',
    paddingBottom: '48rpx',
    paddingLeft: '32rpx',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    rowGap: '12rpx',
    color: tokens.onBrand,
  },
  heroLogo: {
    width: '112rpx',
    height: '112rpx',
    borderRadius: '24rpx',
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: '12rpx',
    overflow: 'hidden',
  },
  heroLogoImg: {
    width: '100%',
    height: '100%',
  },
  heroLogoEmoji: {
    fontSize: '56rpx',
    lineHeight: 1,
  },
  heroName: {
    fontSize: '40rpx',
    fontWeight: 600,
    letterSpacing: '2rpx',
  },
  heroSubtitle: {
    fontSize: '26rpx',
    opacity: 0.92,
  },
  heroVersion: {
    marginTop: '8rpx',
    fontSize: '22rpx',
    opacity: 0.85,
  },
  paragraph: {
    fontSize: '30rpx',
    lineHeight: 1.6,
    color: 'var(--weui-FG-1)',
  },
  footer: {
    paddingTop: '36rpx',
    paddingBottom: '48rpx',
    display: 'flex',
    justifyContent: 'center',
  },
  footerCopyright: {
    fontSize: '24rpx',
    color: 'var(--weui-FG-2)',
  },
})
const About: React.FC = () => {
  usePageViewTracking()

  // 店铺信息(F-OPS-004):后台可维护,未配置时按默认常量兜底
  const [storeProfile, setStoreProfile] = useState<StoreProfileVo | null>(null)

  useLoad(() => {
    console.log('About page loaded.')
  })

  useEffect(() => {
    loadStoreProfile()
  }, [])

  const loadStoreProfile = async () => {
    const result = await StoreProfileService.getStoreProfile()
    if (result.success) {
      setStoreProfile(result.data)
    } else {
      console.error('Load store profile failed:', result.error.message)
      // 加载失败保持兜底常量展示
      setStoreProfile(null)
    }
  }

  // 兜底:配置缺失时回退硬编码常量(PRD 12-operations-config.md §F-OPS-004:未配置时按默认值兜底)
  const shopName = storeProfile?.shopName || BRAND_INFO.NAME
  const hotline = storeProfile?.serviceHotline || CONTACT_INFO.SERVICE_HOTLINE
  const serviceHours =
    storeProfile?.businessHoursStart && storeProfile?.businessHoursEnd
      ? `${storeProfile.businessHoursStart} - ${storeProfile.businessHoursEnd}`
      : CONTACT_INFO.SERVICE_HOURS
  const shopIntro = storeProfile?.shopIntro

  const features = BRAND_FEATURES

  const contactInfo = [
    {
      label: '客服电话',
      value: hotline,
      link: true
    },
    {
      label: '客服时间',
      value: serviceHours,
      link: false
    },
    {
      label: '品牌合作',
      value: CONTACT_INFO.PARTNER_EMAIL,
      link: false
    },
    {
      label: '水源地地址',
      value: WATER_SOURCE_INFO.LOCATION,
      link: false
    }
  ]

  return (
    <View {...stylex.props(pageTheme, styles.page)}>
      {/* 品牌渐变 hero(about.html:居中 logo + 品牌名 + slogan + 版本) */}
      <View {...stylex.props(gradientBox.gradient, styles.hero)}>
        <View {...stylex.props(styles.heroLogo)}>
          {storeProfile?.logoUrl ? (
            <Image {...stylex.props(styles.heroLogoImg)} src={storeProfile.logoUrl} mode='aspectFit' />
          ) : (
            <Text {...stylex.props(styles.heroLogoEmoji)}>{BRAND_INFO.LOGO_EMOJI}</Text>
          )}
        </View>
        <Text {...stylex.props(styles.heroName)}>{shopName}</Text>
        <Text {...stylex.props(styles.heroSubtitle)}>{BRAND_INFO.SUBTITLE}</Text>
        <Text {...stylex.props(styles.heroVersion)}>版本号 v{APP_INFO.VERSION}</Text>
      </View>

      {/* 店铺简介 */}
      <ContentSection title='店铺简介'>
        <View className='weui-cells'>
          <View className='weui-cell weui-cell_vertical'>
            <View {...props('weui-cell__bd', styles.paragraph)}>
              {shopIntro || BRAND_INFO.DESCRIPTION}
            </View>
          </View>
        </View>
      </ContentSection>

      {/* 品牌承诺(weui media-box 行) */}
      <ContentSection title='品牌承诺'>
        <View className='weui-cells'>
          {features.map((feature, index) => (
            <FeatureItem
              key={index}
              icon={feature.icon}
              title={feature.title}
              description={feature.description}
            />
          ))}
        </View>
      </ContentSection>

      {/* 水源地介绍 */}
      <ContentSection title='水源地介绍'>
        <View className='weui-cells'>
          <View className='weui-cell weui-cell_vertical'>
            <View {...props('weui-cell__bd', styles.paragraph)}>
              我们的水源地位于{WATER_SOURCE_INFO.LOCATION}，这里四季分明，雨量充沛，森林覆盖率高达{WATER_SOURCE_INFO.FOREST_COVERAGE}。{WATER_SOURCE_INFO.DESCRIPTION}
            </View>
          </View>
        </View>
      </ContentSection>

      {/* 联系我们(weui cells 行,电话用链接蓝) */}
      <ContentSection title='联系我们'>
        <View className='weui-cells'>
          {contactInfo.map((contact, index) => (
            <ContactItem
              key={index}
              label={contact.label}
              value={contact.value}
              link={contact.link}
            />
          ))}
        </View>
      </ContentSection>

      {storeProfile?.aboutUs && (
        <ContentSection title='关于我们'>
          <View className='weui-cells'>
            <View className='weui-cell weui-cell_vertical'>
              <View {...props('weui-cell__bd', styles.paragraph)}>{storeProfile.aboutUs}</View>
            </View>
          </View>
        </ContentSection>
      )}

      {/* 品牌荣誉 */}
      <ContentSection title='品牌荣誉'>
        <View className='weui-cells'>
          <View className='weui-cell weui-cell_vertical'>
            <View {...props('weui-cell__bd', styles.paragraph)}>
              好喝山泉凭借卓越的品质和诚信的经营理念，先后获得"中国名牌产品"、"绿色食品认证"、"国际饮用水品质金奖"等多项荣誉。我们始终坚持品质至上，用心服务每一位消费者。
            </View>
          </View>
        </View>
      </ContentSection>

      {/* 版权页脚(about.html 居中弱灰) */}
      <View {...stylex.props(styles.footer)}>
        <Text {...stylex.props(styles.footerCopyright)}>{APP_INFO.COPYRIGHT}</Text>
      </View>
    </View>
  )
}

export default About
