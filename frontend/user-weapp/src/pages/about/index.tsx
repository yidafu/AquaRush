import React, { useState, useEffect } from 'react'
import { View, Text, Image } from '@tarojs/components'
import { useLoad } from '@tarojs/taro'
import './index.scss'

// 导入拆分出的组件
import ContentSection from './components/ContentSection'
import FeatureItem from './components/FeatureItem'
import ContactItem from './components/ContactItem'

// 导入常量(店铺信息未配置时兜底展示)
import {
  BRAND_INFO,
  WATER_SOURCE_INFO,
  BRAND_FEATURES} from './constants'
import {
  CONTACT_INFO,
} from '@/constants'
import StoreProfileService from '../../services/StoreProfileService'
import type { StoreProfileVo } from '@aquarush/common'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

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
      label: '服务热线',
      value: hotline
    },
    {
      label: '客服时间',
      value: serviceHours
    },
    {
      label: '品牌合作',
      value: CONTACT_INFO.PARTNER_EMAIL
    },
    {
      label: '水源地地址',
      value: WATER_SOURCE_INFO.LOCATION
    }
  ]

  return (
    <View className='about-page'>
      <View className='header'>
        <View className='logo-container'>
          <View className='logo'>
            {storeProfile?.logoUrl ? (
              <Image src={storeProfile.logoUrl} className='logo-image' mode='aspectFit' />
            ) : (
              <View className='logo-icon'>{BRAND_INFO.LOGO_EMOJI}</View>
            )}
            <Text className='logo-text'>{shopName}</Text>
          </View>
          <Text className='logo-subtitle'>{BRAND_INFO.SUBTITLE}</Text>
        </View>
      </View>

      <View className='content'>
        <ContentSection title='店铺简介'>
          <Text className='section-text'>
            {shopIntro || '好喝山泉始于2020年，是一家专注于天然山泉水的品牌企业。我们坚持从海拔1000米以上的原始森林深处取水，每一滴水都经过大自然层层过滤，富含多种矿物质和微量元素。'}
          </Text>
        </ContentSection>

        <ContentSection title='品牌承诺'>
          <View className='feature-list'>
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

        <ContentSection title='水源地介绍'>
          <Text className='section-text'>
            我们的水源地位于{WATER_SOURCE_INFO.LOCATION}，这里四季分明，雨量充沛，森林覆盖率高达{WATER_SOURCE_INFO.FOREST_COVERAGE}。山泉水在地下深层岩石中经过数十年的天然过滤和矿化，形成了独特的甘甜口感和丰富的营养成分。
          </Text>
          <Text className='section-text'>
            水源地周边30公里范围内无工业污染，我们建立了严格的生态保护措施，确保水源的纯净与可持续性。每一滴水都承载着大自然的馈赠和大山的生命力。
          </Text>
        </ContentSection>

        <ContentSection title='联系我们'>
          <View className='contact-list'>
            {contactInfo.map((contact, index) => (
              <ContactItem
                key={index}
                label={contact.label}
                value={contact.value}
              />
            ))}
          </View>
        </ContentSection>

        {storeProfile?.aboutUs && (
          <ContentSection title='关于我们'>
            <Text className='section-text'>{storeProfile.aboutUs}</Text>
          </ContentSection>
        )}

        <ContentSection title='品牌荣誉'>
          <Text className='section-text'>
            好喝山泉凭借卓越的品质和诚信的经营理念，先后获得"中国名牌产品"、"绿色食品认证"、"国际饮用水品质金奖"等多项荣誉。我们始终坚持品质至上，用心服务每一位消费者。
          </Text>
        </ContentSection>
      </View>

      <View className='footer'>
        <Text className='copyright'>© 2024 好喝山泉. 保留所有权利</Text>
        <Text className='version'>Version 1.0.0</Text>
      </View>
    </View>
  )
}

export default About
