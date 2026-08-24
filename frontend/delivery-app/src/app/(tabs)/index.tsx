import { useCallback, useEffect, useRef, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect, useRouter, type Href } from 'expo-router';
import { Money, centsToYuan } from '@aquarush/common';
import type { TodayStatistics, WeekStatistics } from '@aquarush/common';

import { Card } from '@/components/card';
import { Gradient } from '@/components/gradient';
import { QuickEntryGrid, type QuickEntryItem } from '@/components/quick-entry-grid';
import { StatCard } from '@/components/stat-card';
import { StatCardGrid } from '@/components/stat-card-grid';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { WeekLineChart } from '@/components/week-line-chart';
import { Radius, Spacing } from '@/constants/theme';
import { useAuth } from '@/hooks/use-auth';
import { useTheme } from '@/hooks/use-theme';
import { getWorkbenchData } from '@/services/delivery';
import { todayDateString } from '@/utils/date';

interface WorkbenchData {
  today: TodayStatistics | null;
  week: WeekStatistics | null;
}

export default function WorkbenchScreen() {
  const router = useRouter();
  const theme = useTheme();
  const { userInfo, isAdmin } = useAuth();
  const [data, setData] = useState<WorkbenchData>({ today: null, week: null });
  const [refreshing, setRefreshing] = useState(false);

  const loadData = useCallback(async () => {
    const res = await getWorkbenchData();
    setData({
      today: res?.todayStatistics ?? null,
      week: res?.weekStatistics ?? null,
    });
  }, []);

  // 首屏加载;再次聚焦时刷新
  const firstFocus = useRef(true);
  useFocusEffect(
    useCallback(() => {
      if (firstFocus.current) {
        firstFocus.current = false;
        return;
      }
      loadData().catch((e) => console.error('刷新工作台数据失败:', e));
    }, [loadData]),
  );

  useEffect(() => {
    loadData().catch((e) => console.error('加载工作台数据失败:', e));
  }, [loadData]);

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await loadData();
    } finally {
      setRefreshing(false);
    }
  }, [loadData]);

  const chartData =
    data.week?.dailyStats?.map((d) => ({
      date: d.date,
      orderCount: d.orderCount,
      earning: centsToYuan(d.earningCents),
    })) ?? [];

  const openHistory = useCallback(
    (tabIndex: number) => {
      router.push({ pathname: '/history-orders', params: { tab: String(tabIndex) } });
    },
    [router],
  );

  const commonEntries: QuickEntryItem[] = [
    {
      key: 'history',
      icon: '📋',
      label: '历史订单',
      onPress: () => router.push('/history-orders'),
    },
    {
      key: 'collection',
      icon: '💵',
      label: '每日收款',
      onPress: () => router.push('/daily-collection'),
    },
    {
      key: 'reconciliation',
      icon: '📜',
      label: '对账记录',
      onPress: () => router.push('/reconciliation-records'),
    },
    {
      key: 'messages',
      icon: '🔔',
      label: '消息中心',
      onPress: () => router.push('/messages'),
    },
  ];

  const adminEntries: QuickEntryItem[] = [
    {
      key: 'dashboard',
      icon: '📊',
      label: '经营看板',
      onPress: () => router.push('/manage'),
    },
    {
      key: 'all-orders',
      icon: '📦',
      label: '全部订单',
      onPress: () => router.push({ pathname: '/manage', params: { tab: '1' } }),
    },
    {
      key: 'statistics',
      icon: '📈',
      label: '数据统计',
      onPress: () => router.push('/admin-statistics' as Href),
    },
    {
      key: 'admin-collection',
      icon: '🧾',
      label: '收银对账',
      onPress: () => router.push('/admin-collection' as Href),
    },
    {
      key: 'refunds',
      icon: '↩️',
      label: '退单处置',
      onPress: () => router.push('/refund-management' as Href),
    },
  ];

  const displayName = userInfo?.realName || userInfo?.username || '';

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <SafeAreaView style={styles.safeArea} edges={['top']}>
        <ScrollView
          contentContainerStyle={styles.content}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}>
          <Gradient
            colors={[theme.headerGradientStart, theme.headerGradientEnd] as const}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.hero}>
            <ThemedText type="heading" themeColor="headerText">
              工作台
            </ThemedText>
            <ThemedText type="small" themeColor="headerTextSecondary">
              {displayName ? `${displayName} · ` : ''}
              {todayDateString()}
            </ThemedText>
          </Gradient>

          {/* 本人今日配送概览(仅本人数据) */}
          <StatCardGrid>
            <StatCard
              label="今日总订单"
              value={<ThemedText type="subtitle">{data.today?.totalOrders ?? 0}</ThemedText>}
              onPress={() => openHistory(0)}
            />
            <StatCard
              label="已完成"
              value={<ThemedText type="subtitle">{data.today?.completedOrders ?? 0}</ThemedText>}
              onPress={() => openHistory(1)}
            />
            <StatCard
              label="未完成"
              value={<ThemedText type="subtitle">{data.today?.unfinishedOrders ?? 0}</ThemedText>}
              onPress={() => openHistory(2)}
            />
            <StatCard
              label="今日营收"
              highlight
              value={
                <ThemedText type="subtitle" themeColor="headerText">
                  <Money cents={data.today?.earningCents ?? 0} />
                </ThemedText>
              }
            />
          </StatCardGrid>

          {/* 本人近7天送水数据 */}
          <Card
            title="近7天送水数据"
            right={
              <ThemedText type="caption" themeColor="textSecondary">
                订单: {data.week?.totalOrders ?? 0} | 营收:{' '}
                <Money cents={data.week?.totalEarningCents ?? 0} />
              </ThemedText>
            }>
            <WeekLineChart data={chartData} />
          </Card>

          {/* 九宫格快捷入口(管理员额外显示经营/管理入口) */}
          <Card title="快捷入口">
            <QuickEntryGrid entries={isAdmin ? [...commonEntries, ...adminEntries] : commonEntries} />
          </Card>
        </ScrollView>
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
    paddingHorizontal: Spacing.four,
  },
  content: {
    gap: Spacing.three,
    paddingBottom: Spacing.six,
  },
  hero: {
    borderRadius: Radius.lg,
    padding: Spacing.four,
    gap: Spacing.one,
    marginTop: Spacing.one,
  },
});
