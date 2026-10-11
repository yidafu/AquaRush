import { useEffect, useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { StatusBar } from 'expo-status-bar';
import Constants from 'expo-constants';
import { usePathname, useRouter, type Href } from 'expo-router';

import { Button } from '@/components/button';
import { Gradient } from '@/components/gradient';
import { Input } from '@/components/input';
import { Switch } from '@/components/switch';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Radius, Shadow, Spacing } from '@/constants/theme';
import { useAuth } from '@/hooks/use-auth';
import { useTheme } from '@/hooks/use-theme';
import { storage } from '@/services/storage';

/** 版本号:优先 app 配置版本(expo-constants),取不到回退 1.0.0(原型 login.html 底部版本号)。 */
const APP_VERSION = `v${Constants.expoConfig?.version ?? Constants.nativeApplicationVersion ?? '1.0.0'}`;

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
  const [remember, setRemember] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // 启动回填记住的账号(仅用户名,不缓存密码)
  useEffect(() => {
    storage
      .getRememberedUsername()
      .then((saved) => {
        if (saved) {
          setUsername(saved);
          setRemember(true);
        }
      })
      .catch((e) => console.error('读取记住账号失败:', e));
  }, []);

  const handleSubmit = async () => {
    setError('');
    if (!username.trim() || !password) {
      setError('请输入用户名和密码');
      return;
    }
    setSubmitting(true);
    try {
      const info = await login(username.trim(), password);
      // 记住账号(原型 login.html:仅本地记住用户名,不缓存密码)
      if (remember) {
        await storage.setRememberedUsername(username.trim());
      } else {
        await storage.clearRememberedUsername();
      }
      const isAdmin = info.role === 'ADMIN' || info.role === 'SUPER_ADMIN';
      // 管理员登录落在「管理」Tab；送水员保持深链/首页
      router.replace(isAdmin ? '/manage' : (pendingRoute ?? '/'));
    } catch (e: any) {
      setError(e?.response?.message ?? e?.message ?? '登录失败，请检查用户名和密码');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <ThemedView type="pageBackground" style={styles.flex}>
      <StatusBar style="auto" />
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          {/* 品牌区(原型 login.html):渐变圆角方块 logo + 奔泉字标 + 端名副标题 */}
          <View style={styles.logoSection}>
            <Gradient
              colors={[theme.headerGradientStart, theme.headerGradientEnd] as const}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
              style={styles.logoMark}>
              <ThemedText type="subtitle" themeColor="headerText" style={styles.logoLetter}>
                A
              </ThemedText>
            </Gradient>
            <ThemedText type="subtitle">奔泉</ThemedText>
            <ThemedText type="caption" themeColor="textSecondary">
              送水配送 · 运营管理
            </ThemedText>
          </View>

          <ThemedText type="small" themeColor="textSecondary" style={styles.welcome}>
            欢迎使用奔泉配送与运营平台
          </ThemedText>

          <View style={[styles.panel, { backgroundColor: theme.card }]}>
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

            {/* 记住账号(仅记住用户名,启动回填) */}
            <View style={styles.rememberRow}>
              <ThemedText type="small" themeColor="textSecondary">
                记住账号
              </ThemedText>
              <Switch
                value={remember}
                onValueChange={setRemember}
                disabled={submitting}
              />
            </View>

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

          {/* 底部版本号(原型 login.html) */}
          <ThemedText type="caption" themeColor="textTertiary" style={styles.version}>
            {`奔泉配送版 ${APP_VERSION}`}
          </ThemedText>
        </ScrollView>
      </KeyboardAvoidingView>
    </ThemedView>
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
    marginBottom: Spacing.four,
  },
  logoMark: {
    width: 64,
    height: 64,
    borderRadius: Radius.lg,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.two,
  },
  logoLetter: {
    fontWeight: '700',
  },
  welcome: {
    textAlign: 'center',
    marginBottom: Spacing.four,
  },
  panel: {
    borderRadius: Radius.xl,
    padding: Spacing.four,
    gap: Spacing.three,
    ...Shadow.lg,
  },
  error: {
    textAlign: 'center',
  },
  rememberRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  tips: {
    textAlign: 'center',
    marginTop: Spacing.one,
  },
  agreementLink: {
    textDecorationLine: 'underline',
  },
  version: {
    textAlign: 'center',
    marginTop: Spacing.five,
  },
});
