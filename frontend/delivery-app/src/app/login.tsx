import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { usePathname, useRouter, type Href } from 'expo-router';

import { Button } from '@/components/button';
import { Gradient } from '@/components/gradient';
import { Input } from '@/components/input';
import { ThemedText } from '@/components/themed-text';
import { Radius, Shadow, Spacing } from '@/constants/theme';
import { useAuth } from '@/hooks/use-auth';
import { useTheme } from '@/hooks/use-theme';

export default function LoginScreen() {
  const { login } = useAuth();
  const router = useRouter();
  const theme = useTheme();

  // 深链登录守卫：未登录时 Stack.Protected 渲染登录页但 pathname 保持深链目标，
  // 登录后跳回该目标（如微信分享卡片直达订单详情）
  const pathname = usePathname();
  const pendingRoute: Href | null =
    pathname && !['', '/', '/login'].includes(pathname) ? (pathname as Href) : null;

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    setError('');
    if (!username.trim() || !password) {
      setError('请输入用户名和密码');
      return;
    }
    setSubmitting(true);
    try {
      const info = await login(username.trim(), password);
      const isAdmin = info.role === 'ADMIN' || info.role === 'SUPER_ADMIN';
      // 管理员登录落在「管理」Tab；送水员保持深链/首页
      router.replace(isAdmin ? '/manage' : (pendingRoute ?? '/'));
    } catch (e: any) {
      setError(e?.response?.message ?? e?.message ?? '登录失败，请检查用户名和密码');
    } finally {
      setSubmitting(false);
    }
  };

  // 登录页为全屏品牌渐变 hero（亮暗一致），状态栏恒为亮字（见 docs/ui-spec.md §6）
  const gradientColors = [theme.headerGradientStart, theme.headerGradientEnd] as const;

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <Gradient colors={gradientColors} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={styles.flex}>
        <StatusBar style="light" />
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          <View style={styles.logoSection}>
            <ThemedText type="title" style={{ color: theme.headerText }}>
              AquaRush
            </ThemedText>
            <ThemedText type="caption" themeColor="headerTextSecondary">
              送水配送 · 运营管理
            </ThemedText>
          </View>

          <View style={[styles.panel, { backgroundColor: theme.card }]}>
            <ThemedText type="heading" style={styles.welcome}>
              欢迎使用 AquaRush 配送与运营平台
            </ThemedText>

            {error ? (
              <ThemedText themeColor="danger" style={styles.error}>
                {error}
              </ThemedText>
            ) : null}

            <Input
              placeholder="用户名"
              autoCapitalize="none"
              autoCorrect={false}
              value={username}
              onChangeText={setUsername}
              editable={!submitting}
            />

            <Input
              placeholder="密码"
              secureTextEntry
              autoCapitalize="none"
              value={password}
              onChangeText={setPassword}
              onSubmitEditing={handleSubmit}
              editable={!submitting}
            />

            <Button
              title="登录"
              size="lg"
              loading={submitting}
              trackTarget="login-submit"
              trackElementType="button"
              trackElementText="登录"
              trackProps={{ username: username.trim() }}
              onPress={handleSubmit}
            />

            <ThemedText type="caption" themeColor="textTertiary" style={styles.tips}>
              {'登录即表示同意'}
              <Text
                style={[styles.agreementLink, { color: theme.primary }]}
                onPress={() => router.push('/user-agreement')}>
                《用户协议》
              </Text>
              {'和'}
              <Text
                style={[styles.agreementLink, { color: theme.primary }]}
                onPress={() => router.push('/privacy-policy')}>
                《隐私政策》
              </Text>
            </ThemedText>
          </View>
        </ScrollView>
      </Gradient>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: {
    flex: 1,
  },
  scroll: {
    flexGrow: 1,
    justifyContent: 'center',
    paddingHorizontal: Spacing.five,
  },
  logoSection: {
    alignItems: 'center',
    gap: Spacing.one,
    marginBottom: Spacing.five,
  },
  panel: {
    borderRadius: Radius.xl,
    padding: Spacing.four,
    gap: Spacing.three,
    ...Shadow.lg,
  },
  welcome: {
    textAlign: 'center',
    marginBottom: Spacing.one,
  },
  error: {
    textAlign: 'center',
  },
  tips: {
    textAlign: 'center',
    marginTop: Spacing.one,
  },
  agreementLink: {
    textDecorationLine: 'underline',
  },
});
