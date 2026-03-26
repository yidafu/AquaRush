import React from 'react'
import { View, Text, Image, Button } from '@tarojs/components'
import Taro from '@tarojs/taro'
import './index.scss'
import { PageContainer } from '../../components/PageContainer'

const AboutPage: React.FC = () => {
  const version = '1.0.0'

  const handleCallService = () => {
    Taro.makePhoneCall({
      phoneNumber: '400-888-8888',
    })
  }

  return (
    <PageContainer title="关于我们">
      <View className='about-page'>
        {/* Logo 和应用信息 */}
        <View className='app-info'>
          <View className='logo'>
            <Image src='./assets/task.png' mode='aspectFit' className='logo-img' />
          </View>
          <Text className='app-name'>AquaRush 配送版</Text>
          <Text className='app-version'>版本 {version}</Text>
        </View>

        {/* 简介 */}
        <View className='section'>
          <Text className='section-title'>应用简介</Text>
          <View className='section-content'>
            <Text>
              AquaRush 配送版是一款专业的桶装水配送管理小程序，为配送员提供订单管理、统计查询等功能，提升配送效率，优化用户体验。
            </Text>
          </View>
        </View>

        {/* 功能介绍 */}
        <View className='section'>
          <Text className='section-title'>主要功能</Text>
          <View className='feature-list'>
            <View className='feature-item'>
              <Text className='feature-icon'>📋</Text>
              <View className='feature-text'>
                <Text className='feature-name'>订单管理</Text>
                <Text className='feature-desc'>查看和处理配送订单</Text>
              </View>
            </View>
            <View className='feature-item'>
              <Text className='feature-icon'>📊</Text>
              <View className='feature-text'>
                <Text className='feature-name'>数据统计</Text>
                <Text className='feature-desc'>查看配送统计数据</Text>
              </View>
            </View>
            <View className='feature-item'>
              <Text className='feature-icon'>📜</Text>
              <View className='feature-text'>
                <Text className='feature-name'>历史订单</Text>
                <Text className='feature-desc'>回顾已完成订单记录</Text>
              </View>
            </View>
          </View>
        </View>

        {/* 联系方式 */}
        <View className='section'>
          <Text className='section-title'>联系我们</Text>
          <View className='contact-list'>
            <View className='contact-item'>
              <Text className='contact-label'>客服热线</Text>
              <Text className='contact-value'>400-888-8888</Text>
            </View>
            <View className='contact-item'>
              <Text className='contact-label'>工作时间</Text>
              <Text className='contact-value'>周一至周日 9:00-18:00</Text>
            </View>
          </View>
          <Button className='call-btn' onClick={handleCallService}>
            拨打电话
          </Button>
        </View>

        {/* 版权信息 */}
        <View className='copyright'>
          <Text>© 2024 AquaRush</Text>
          <Text>版权所有 保留一切权利</Text>
        </View>
      </View>
    </PageContainer>
  )
}

export default AboutPage
