import { Alert, Platform, Pressable, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter, type Href } from 'expo-router';

import { Button } from '@/components/button';
import { Card } from '@/components/card';
import { Gradient } from '@/components/gradient';
import { OrderInfoRow } from '@/components/order-info-row';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Radius, Spacing } from '@/constants/theme';
import { useAuth } from '@/hooks/use-auth';
import { useTheme } from '@/hooks/use-theme';
import { userActionTracker } from '@/utils/tracking';

const ROLE_LABELS: Record<string, string> = {
  DELIVERY_WORKER: '送水员',
  ADMIN: '管理员',
  SUPER_ADMIN: '超级管理员',
};

const MENU_ITEMS: { label: string; href: Href }[] = [
  { label: '消息中心', href: '/messages' as Href },
  { label: '每日收款', href: '/daily-collection' },
  { label: '对账记录', href: '/reconciliation-records' },
  { label: '历史订单', href: '/history-orders' },
  { label: '关于我们', href: '/about' },
  { label: '用户协议', href: '/user-agreement' },
  { label: '隐私政策', href: '/privacy-policy' },
];

export default function ProfileScreen() {
  const { userInfo, isAdmin, logout } = useAuth();
  const router = useRouter();
  const theme = useTheme();

  const menuItems = [
    ...(isAdmin ? [{ label: '经营看板', href: '/manage' as Href }] : []),
    ...MENU_ITEMS,
  ];

  const handleLogout = () => {
    const doLogout = async () => {
      // 埋点:登出(显式携带用户上下文,避免登出清空本地存储后上报丢失归属)
      userActionTracker.track('CLICK', 'logout', {
        elementType: 'button',
        elementText: '退出登录',
        userId: userInfo?.id != null ? String(userInfo.id) : undefined,
        username: userInfo?.username || userInfo?.realName || undefined,
      });
      await logout();
      router.replace('/login');
    };

    if (Platform.OS === 'web') {
      if (typeof window !== 'undefined' && window.confirm('确定要退出登录吗？')) {
        doLogout();
      }
      return;
    }
    Alert.alert('退出登录', '确定要退出登录吗？', [
      { text: '取消', style: 'cancel' },
      { text: '退出', style: 'destructive', onPress: () => doLogout() },
    ]);
  };

  const displayName = userInfo?.realName || userInfo?.username || '未知';
  const avatarChar = displayName.charAt(0);
  const roleLabel = userInfo?.role ? (ROLE_LABELS[userInfo.role] ?? userInfo.role) : '-';

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <SafeAreaView style={styles.safeArea} edges={['top']}>
        <Gradient
          colors={[theme.headerGradientStart, theme.headerGradientEnd] as const}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={styles.userCard}>
          <View style={[styles.avatar, { backgroundColor: theme.primaryBg }]}>
            <ThemedText type="heading" themeColor="primary">
              {avatarChar}
            </ThemedText>
          </View>
          <View style={styles.userInfo}>
            <ThemedText type="heading" themeColor="headerText">
              {displayName}
            </ThemedText>
            <View style={[styles.roleBadge, { backgroundColor: theme.headerBadgeBg }]}>
              <ThemedText type="caption" themeColor="headerText">
                {roleLabel}
              </ThemedText>
            </View>
          </View>
        </Gradient>

        <Card>
          <OrderInfoRow label="姓名" value={displayName} />
          <OrderInfoRow label="账号" value={userInfo?.username || '-'} />
          <OrderInfoRow label="角色" value={roleLabel} />
        </Card>

        <Card>
          {menuItems.map((item, index) => (
            <Pressable
              key={item.label}
              onPress={() => router.push(item.href)}
              style={({ pressed }) => [
                styles.menuItem,
                index < menuItems.length - 1 && {
                  borderBottomWidth: StyleSheet.hairlineWidth,
                  borderBottomColor: theme.border,
                },
                pressed && styles.menuItemPressed,
              ]}>
              <ThemedText type="smallBold">{item.label}</ThemedText>
              <ThemedText type="small" themeColor="textTertiary">
                ›
              </ThemedText>
            </Pressable>
          ))}
        </Card>

        <Button title="退出登录" variant="dangerGhost" onPress={handleLogout} />

        <ThemedText type="caption" themeColor="textTertiary" style={styles.version}>
          AquaRush 配送版 v1.0.0
        </ThemedText>
      </SafeAreaView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  safeArea: {
    flex: 1,
    gap: Spacing.three,
    padding: Spacing.four,
  },
  userCard: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: Radius.lg,
    padding: Spacing.four,
    gap: Spacing.three,
    marginTop: Spacing.one,
  },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
  },
  userInfo: {
    gap: Spacing.one,
  },
  roleBadge: {
    alignSelf: 'flex-start',
    borderRadius: Radius.pill,
    paddingHorizontal: Spacing.two,
    paddingVertical: Spacing.half,
  },
  menuItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    minHeight: 48,
    paddingVertical: Spacing.two,
  },
  menuItemPressed: {
    opacity: 0.6,
  },
  version: {
    textAlign: 'center',
    marginTop: Spacing.two,
  },
});
