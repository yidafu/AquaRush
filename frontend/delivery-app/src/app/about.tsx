import { useCallback } from 'react';
import { Linking, Platform, StyleSheet, View } from 'react-native';
import { Image } from 'expo-image';

import { Button } from '@/components/button';
import { Card } from '@/components/card';
import { OrderInfoRow } from '@/components/order-info-row';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

const VERSION = '1.0.0';
const SERVICE_PHONE = '400-888-8888';

const FEATURES = [
  { icon: '📋', name: '订单管理', desc: '查看和处理配送订单' },
  { icon: '📊', name: '数据统计', desc: '查看配送统计数据' },
  { icon: '📜', name: '历史订单', desc: '回顾已完成订单记录' },
];

export default function AboutScreen() {
  const theme = useTheme();

  const handleCallService = useCallback(() => {
    if (Platform.OS === 'web') return;
    Linking.openURL(`tel:${SERVICE_PHONE}`).catch((e) =>
      console.error('拨打电话失败:', e),
    );
  }, []);

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <View style={styles.content}>
        <View style={styles.appInfo}>
          <View style={[styles.logo, { backgroundColor: theme.primaryBg }]}>
            <Image
              source={require('@/assets/images/icon.png')}
              style={styles.logoImg}
              contentFit="contain"
            />
          </View>
          <ThemedText type="subtitle">奔泉配送版</ThemedText>
          <ThemedText type="small" themeColor="textTertiary">
            版本 {VERSION}
          </ThemedText>
        </View>

        <Card title="应用简介">
          <ThemedText type="small" themeColor="textSecondary" style={styles.intro}>
            奔泉配送版是一款专业的桶装水配送管理小程序，为配送员提供订单管理、统计查询等功能，提升配送效率，优化用户体验。
          </ThemedText>
        </Card>

        <Card title="主要功能">
          {FEATURES.map((feature) => (
            <View key={feature.name} style={styles.featureItem}>
              <ThemedText style={styles.featureIcon}>{feature.icon}</ThemedText>
              <View style={styles.featureText}>
                <ThemedText type="smallBold">{feature.name}</ThemedText>
                <ThemedText type="caption" themeColor="textTertiary">
                  {feature.desc}
                </ThemedText>
              </View>
            </View>
          ))}
        </Card>

        <Card title="联系我们">
          <OrderInfoRow label="客服热线" value={SERVICE_PHONE} />
          <OrderInfoRow label="工作时间" value="周一至周日 9:00-18:00" />
          {Platform.OS !== 'web' ? (
            <Button title="拨打电话" variant="ghost" onPress={handleCallService} style={styles.callBtn} />
          ) : null}
        </Card>

        <View style={styles.copyright}>
          <ThemedText type="caption" themeColor="textTertiary">
            © 2026 奔泉
          </ThemedText>
          <ThemedText type="caption" themeColor="textTertiary">
            版权所有 保留一切权利
          </ThemedText>
        </View>
      </View>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: Spacing.three,
    paddingBottom: Spacing.six,
  },
  appInfo: {
    alignItems: 'center',
    gap: Spacing.two,
    marginVertical: Spacing.four,
  },
  logo: {
    width: 72,
    height: 72,
    borderRadius: Radius.lg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  logoImg: {
    width: 56,
    height: 56,
  },
  intro: {
    lineHeight: 22,
  },
  featureItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    paddingVertical: Spacing.two,
  },
  featureIcon: {
    fontSize: 22,
    lineHeight: 28,
  },
  featureText: {
    gap: Spacing.half,
  },
  callBtn: {
    marginTop: Spacing.three,
  },
  copyright: {
    alignItems: 'center',
    gap: Spacing.one,
    marginTop: Spacing.four,
  },
});
