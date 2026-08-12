import { DarkTheme, DefaultTheme, Stack, ThemeProvider } from 'expo-router';
import { ActivityIndicator, StyleSheet, useColorScheme, View } from 'react-native';

import { RootStatusBar } from '@/components/root-status-bar';
import { TypeScale } from '@/constants/theme';
import { AuthProvider, useAuth } from '@/hooks/use-auth';
import { useTheme } from '@/hooks/use-theme';

function RootNavigator() {
  const { isHydrating, isAuthenticated } = useAuth();
  const theme = useTheme();

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
