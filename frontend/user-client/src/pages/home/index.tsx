import * as React from 'react';
import { View, Text, Image } from 'remax/wechat';
import './index.css';

const HomePage: React.FC = () => {
  const [products, setProducts] = React.useState([]);

  React.useEffect(() => {
    // TODO: 加载产品列表
    loadProducts();
  }, []);

  const loadProducts = async () => {
    // TODO: 调用 API 加载产品
  };

  return (
    <View className="home-page">
      <View className="banner">
        <Text className="banner-title">新鲜好水，送货上门</Text>
      </View>
      
      <View className="product-list">
        {products.length === 0 ? (
          <View className="empty">
            <Text>暂无产品</Text>
          </View>
        ) : (
          products.map((product: any) => (
            <View key={product.id} className="product-item">
              {/* TODO: 产品卡片组件 */}
            </View>
          ))
        )}
      </View>
    </View>
  );
};

export default HomePage;
