import { useLocalSearchParams } from 'expo-router';
import { StyleSheet, View } from 'react-native';

import { productDetailH5Url } from '@/constants/api';

/**
 * 商品详情页(web 端):iframe 嵌入 user-weapp 商详页 H5 产物,
 * 与原生端 WebView / PC 端 admin 预览共享同一份 H5。
 */
export default function ProductDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();

  if (!id) return null;

  return (
    <View style={styles.container}>
      <iframe
        src={productDetailH5Url(String(id))}
        style={styles.frame}
        title="商品详情"
        // 只读展示:sandbox 禁弹窗/表单/顶层跳转,保留 scripts 与同源(React 应用与 localStorage 所需)
        sandbox="allow-scripts allow-same-origin"
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  frame: {
    flex: 1,
    width: '100%',
    borderWidth: 0,
  },
});
