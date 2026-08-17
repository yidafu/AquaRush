import { DarkTheme, DefaultTheme, Stack, ThemeProvider } from 'expo-router';
import { useEffect } from 'react';
import { ActivityIndicator, AppState, StyleSheet, useColorScheme, View } from 'react-native';

import { RootStatusBar } from '@/components/root-status-bar';
import { TypeScale } from '@/constants/theme';
import { AuthProvider, useAuth } from '@/hooks/use-auth';
import { usePageViewTracking } from '@/hooks/usePageViewTracking';
import { useTheme } from '@/hooks/use-theme';
import { userActionTracker } from '@/utils/tracking';
import { trajectoryReporter } from '@/utils/trajectory-reporter';

function RootNavigator() {
  const { isHydrating, isAuthenticated } = useAuth();
  const theme = useTheme();

  // 埋点:路由变化上报 PAGE_VIEW(覆盖全部页面,含登录页)
  usePageViewTracking();

  // 轨迹上报(F-ADR-012):登录/App 启动后恢复配送中订单的轨迹上报,
  // 登出时停止全部上报并清空活动订单。
  // 注:react-native-fitness-geolocation 已移除,trajectoryReporter 为空实现,
  // 以下调用保留作集成点,当前不产生任何定位行为。
  useEffect(() => {
    if (isAuthenticated) {
      void trajectoryReporter.resumeActive();
    } else {
      void trajectoryReporter.stopAll();
    }
  }, [isAuthenticated]);

  // 埋点:应用切后台时立即上报缓冲队列,避免事件滞留丢失
  useEffect(() => {
    const sub = AppState.addEventListener('change', (state) => {
      if (state !== 'active') {
        void userActionTracker.flush();
      }
    });
    return () => sub.remove();
  }, []);

  if (isHydrating) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator size="large" color={theme.primary} />
      </View>
    );
  }

  const headerOptions = {
    headerStyle: { backgroundColor: theme.background },
    headerTintColor: theme.text,
    headerTitleStyle: TypeScale.heading,
  };

  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Protected guard={isAuthenticated}>
        <Stack.Screen name="(tabs)" />
        <Stack.Screen
          name="order/[orderNo]"
          options={{ headerShown: true, title: '订单详情', ...headerOptions }}
        />
        <Stack.Screen
          name="create-order"
          options={{ headerShown: true, title: '创建送水订单', ...headerOptions }}
        />
        <Stack.Screen
          name="product/[id]"
          options={{ headerShown: true, title: '商品详情', ...headerOptions }}
        />
        <Stack.Screen
          name="history-orders"
          options={{ headerShown: true, title: '历史订单', ...headerOptions }}
        />
        <Stack.Screen
          name="daily-collection"
          options={{ headerShown: true, title: '每日收款', ...headerOptions }}
        />
        <Stack.Screen
          name="reconciliation-records"
          options={{ headerShown: true, title: '对账记录', ...headerOptions }}
        />
        <Stack.Screen
          name="about"
          options={{ headerShown: true, title: '关于我们', ...headerOptions }}
        />
      </Stack.Protected>
      <Stack.Screen name="login" />
      <Stack.Screen
        name="user-agreement"
        options={{ headerShown: true, title: '用户协议', ...headerOptions }}
      />
      <Stack.Screen
        name="privacy-policy"
        options={{ headerShown: true, title: '隐私政策', ...headerOptions }}
      />
    </Stack>
  );
}

export default function RootLayout() {
  const colorScheme = useColorScheme();
  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <RootStatusBar />
      <AuthProvider>
        <RootNavigator />
      </AuthProvider>
    </ThemeProvider>
  );
}

const styles = StyleSheet.create({
  loading: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
