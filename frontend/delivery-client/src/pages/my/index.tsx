import * as React from 'react';
import { View, Text } from 'remax/wechat';

const MyPage: React.FC = () => {
  return (
    <View className="my-page">
      <View className="user-info">
        <Text>配送员信息</Text>
      </View>
    </View>
  );
};

export default MyPage;
