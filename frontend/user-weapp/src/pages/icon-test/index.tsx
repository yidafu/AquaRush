import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtCard, AtList, AtListItem } from 'taro-ui'
import CustomIcon from '../../components/CustomIcon'
import Taro from '@tarojs/taro'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/icon.scss"
import "taro-ui/dist/style/components/list.scss"
import './index.scss'

const IconTestPage: React.FC = () => {
  return (
    <View className='icon-test-page'>
      <View className='test-header'>
        <Text className='test-title'>AquaRush 图标展示测试</Text>
        <Text className='test-desc'>验证所有图标是否正确加载和显示</Text>
      </View>

      {/* 服务图标 */}
      <AtCard title='服务图标' className='test-section'>
        <View className='icon-grid'>
          <View className='icon-item'>
            <CustomIcon value='/assets/icons/service/map-pin.png' size='32' color='#667eea' />
            <Text className='icon-label'>收货地址</Text>
          </View>
          <View className='icon-item'>
            <CustomIcon value='/assets/icons/service/comments.png' size='32' color='#667eea' />
            <Text className='icon-label'>客服中心</Text>
          </View>
          <View className='icon-item'>
            <CustomIcon value='/assets/icons/service/feedback.png' size='32' color='#667eea' />
            <Text className='icon-label'>意见反馈</Text>
          </View>
          <View className='icon-item'>
            <CustomIcon value='/assets/icons/service/info-circle.png' size='32' color='#667eea' />
            <Text className='icon-label'>关于我们</Text>
          </View>
          <View className='icon-item'>
            <CustomIcon value='/assets/icons/service/settings.png' size='32' color='#667eea' />
            <Text className='icon-label'>设置</Text>
          </View>
        </View>
      </AtCard>

      {/* 订单图标 */}
      <AtCard title='订单图标' className='test-section'>
        <View className='icon-grid'>
          <View className='icon-item'>
            <CustomIcon value='/assets/icons/order/all.png' size='32' color='#667eea' />
            <Text className='icon-label'>全部订单</Text>
          </View>
          <View className='icon-item'>
            <CustomIcon value='/assets/icons/order/credit-card.png' size='32' color='#ff6b35' />
            <Text className='icon-label'>待付款</Text>
          </View>
          <View className='icon-item'>
            <CustomIcon value='/assets/icons/order/shopping-bag.png' size='32' color='#667eea' />
            <Text className='icon-label'>待配送</Text>
          </View>
          <View className='icon-item'>
            <CustomIcon value='/assets/icons/order/truck.png' size='32' color='#19be6b' />
            <Text className='icon-label'>配送中</Text>
          </View>
          <View className='icon-item'>
            <CustomIcon value='/assets/icons/order/check-circle.png' size='32' color='#999' />
            <Text className='icon-label'>已完成</Text>
          </View>
        </View>
      </AtCard>

      {/* 不同尺寸测试 */}
      <AtCard title='尺寸测试' className='test-section'>
        <View className='size-test'>
          <View className='size-row'>
            <Text className='size-label'>16px:</Text>
            <CustomIcon value='/assets/icons/service/map-pin.png' size='16' color='#667eea' />
            <CustomIcon value='/assets/icons/order/truck.png' size='16' color='#19be6b' />
          </View>
          <View className='size-row'>
            <Text className='size-label'>24px:</Text>
            <CustomIcon value='/assets/icons/service/map-pin.png' size='24' color='#667eea' />
            <CustomIcon value='/assets/icons/order/truck.png' size='24' color='#19be6b' />
          </View>
          <View className='size-row'>
            <Text className='size-label'>32px:</Text>
            <CustomIcon value='/assets/icons/service/map-pin.png' size='32' color='#667eea' />
            <CustomIcon value='/assets/icons/order/truck.png' size='32' color='#19be6b' />
          </View>
        </View>
      </AtCard>
    </View>
  )
}

export default IconTestPage
