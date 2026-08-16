import { useCallback, useState } from 'react';
import { ActivityIndicator, StyleSheet, View } from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import WebView from 'react-native-webview';

import { Button } from '@/components/button';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { productDetailH5Url } from '@/constants/api';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

/**
 * 商品详情页:直接加载 user-weapp 商详页的 H5 编译产物(admin-client 托管于 /weapp/)。
 * 与 PC 端 admin 预览共享同一份 H5,详情逻辑只维护 Taro 一份,两端零重复。
 * H5 端通过 from=rn 识别本场景,隐藏购买/收藏等交互(纯展示)。
 */
export default function ProductDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const handleReload = useCallback(() => {
    setError(null);
    setReloadKey((k) => k + 1);
  }, []);

  if (!id) {
    return (
      <ThemedView type="pageBackground" style={styles.center}>
        <ThemedText themeColor="textSecondary">商品不存在</ThemedText>
      </ThemedView>
    );
  }

  const initialUri = productDetailH5Url(String(id));

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <WebView
        key={reloadKey}
        source={{ uri: initialUri }}
        style={styles.webview}
        originWhitelist={['*']}
        onLoadStart={() => {
          setLoading(true);
          setError(null);
        }}
        onLoadEnd={() => setLoading(false)}
        onError={() => {
          setLoading(false);
          setError('页面加载失败,请检查网络后重试');
        }}
        onShouldStartLoadWithRequest={(request) => {
          // 只读展示:拦截用户点击/表单提交产生的页面导航(富文本链接等),资源加载放行
          return request.navigationType !== 'click' && request.navigationType !== 'formsubmit';
        }}
        onOpenWindow={() => {
          // 忽略 window.open(如大图预览),禁止弹出新页面 —— 纯展示
        }}
      />
      {loading ? (
        <View style={styles.loadingOverlay} pointerEvents="none">
          <ActivityIndicator size="large" color={theme.primary} />
        </View>
      ) : null}
      {error && !loading ? (
        <View style={styles.center}>
          <ThemedText themeColor="danger" style={styles.errorText}>
            {error}
          </ThemedText>
          <Button title="重试" size="md" onPress={handleReload} />
        </View>
      ) : null}
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  webview: {
    flex: 1,
    backgroundColor: 'transparent',
  },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.three,
    padding: Spacing.four,
  },
  loadingOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    alignItems: 'center',
    justifyContent: 'center',
  },
  errorText: {
    textAlign: 'center',
  },
});
