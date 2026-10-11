import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect, useRouter, type Href } from 'expo-router';
import { Money, centsToYuan } from '@aquarush/common';
import type { TodayStatisticsVo, WeekStatisticsVo } from '@aquarush/common';

import { Card } from '@/components/card';
import { QuickEntryGrid, type QuickEntryItem } from '@/components/quick-entry-grid';
import { StatCard } from '@/components/stat-card';
import { StatCardGrid } from '@/components/stat-card-grid';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { WeekLineChart } from '@/components/week-line-chart';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/hooks/use-auth';
import { getWorkbenchData } from '@/services/delivery';

interface WorkbenchData {
  today: TodayStatisticsVo | null;
  week: WeekStatisticsVo | null;
}

/** 时段问候(原型 workbench 问候卡:上午好/中午好/下午好/晚上好)。 */
function getGreeting(hour: number): string {
  if (hour < 11) return '上午好';
  if (hour < 13) return '中午好';
  if (hour < 18) return '下午好';
  return '晚上好';
}

export default function WorkbenchScreen() {
  const router = useRouter();
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

  // 常用四宫格(原型 workbench quick-actions:去接单/代客下单/提交日报/历史订单)
  const commonEntries: QuickEntryItem[] = [
    {
      key: 'orders',
      icon: '📍',
      label: '去接单',
      onPress: () => router.push('/orders'),
    },
    {
      key: 'create-order',
      icon: '➕',
      label: '代客下单',
      onPress: () => router.push('/create-order'),
    },
    {
      key: 'collection',
      icon: '🧾',
      label: '提交日报',
      onPress: () => router.push('/daily-collection'),
    },
    {
      key: 'history',
      icon: '📋',
      label: '历史订单',
      onPress: () => router.push('/history-orders'),
    },
  ];

  // 管理宫格:常用四宫格装不下的既有入口(对账/消息为送水员入口,其余为管理员经营入口)
  const manageEntries: QuickEntryItem[] = [
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
    ...(isAdmin
      ? [
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
            icon: '💰',
            label: '收银对账',
            onPress: () => router.push('/admin-collection' as Href),
          },
          {
            key: 'refunds',
            icon: '↩️',
            label: '退单处置',
            onPress: () => router.push('/refund-management' as Href),
          },
        ]
      : []),
  ];

  const displayName = userInfo?.realName || userInfo?.username || '';
  const greeting = useMemo(() => getGreeting(new Date().getHours()), []);
  const unfinished = data.today?.unfinishedOrders;

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <SafeAreaView style={styles.safeArea} edges={['top']}>
        <ScrollView
          contentContainerStyle={styles.content}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}>
          {/* ① 问候卡:时段问候 + 骑手名(在线/离线开关:服务层暂无接单状态切换 API,不做假开关) */}
          <Card>
            <View style={styles.greetingBlock}>
              <ThemedText type="heading">
                {greeting}，{displayName}
              </ThemedText>
              <ThemedText type="small" themeColor="textSecondary">
                {unfinished != null
                  ? `今日还有 ${unfinished} 单未完成，路上注意安全`
                  : '路上注意安全'}
              </ThemedText>
            </View>
          </Card>

          {/* ② 今日统计 2×2(现有口径:总订单/完成订单数/未完成/今日收款;分态数无独立数据源) */}
          <StatCardGrid>
            <StatCard
              label="今日总订单"
              value={<ThemedText type="subtitle">{data.today?.totalOrders ?? 0}</ThemedText>}
              onPress={() => openHistory(0)}
            />
            <StatCard
              label="完成订单数"
              value={<ThemedText type="subtitle">{data.today?.completedOrders ?? 0}</ThemedText>}
              onPress={() => openHistory(1)}
            />
            <StatCard
              label="未完成"
              value={<ThemedText type="subtitle">{data.today?.unfinishedOrders ?? 0}</ThemedText>}
              onPress={() => openHistory(2)}
            />
            <StatCard
              label="今日收款"
              highlight
              value={
                <ThemedText type="subtitle" themeColor="headerText">
                  <Money cents={data.today?.earningCents ?? 0} />
                </ThemedText>
              }
            />
          </StatCardGrid>

          {/* ③ 快捷入口:原型四宫格 + 管理宫格 */}
          <Card title="快捷入口">
            <QuickEntryGrid entries={commonEntries} columns={4} />
          </Card>

          <Card title="管理">
            <QuickEntryGrid entries={manageEntries} columns={4} />
          </Card>

          {/* 近7天送水数据(统计能力收敛进工作台,保留折线卡) */}
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
  greetingBlock: {
    gap: Spacing.one,
  },
});
