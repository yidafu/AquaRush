import * as React from 'react';
import { View, Text } from 'remax/wechat';

const MyPage: React.FC = () => {
  return (
    <View className="my-page">
      <View className="user-info">
        <Text>我的</Text>
      </View>
      
      <View className="menu-list">
        <View className="menu-item">
          <Text>收货地址</Text>
        </View>
        <View className="menu-item">
          <Text>我的订单</Text>
        </View>
      </View>
    </View>
  );
};

export default MyPage;
