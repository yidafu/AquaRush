import { useCallback, useEffect, useMemo, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Money, centsToYuan } from '@aquarush/common';
import type { TodayStatistics, WeekStatistics } from '@aquarush/common';

import { Card } from '@/components/card';
import { StatCard } from '@/components/stat-card';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { WeekLineChart } from '@/components/week-line-chart';
import { Spacing } from '@/constants/theme';
import { getTodayStatistics, getWeekStatistics } from '@/services/delivery';

function getTodayDate(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

export default function StatisticsScreen() {
  const router = useRouter();
  const [statistics, setStatistics] = useState<TodayStatistics | null>(null);
  const [weekStatistics, setWeekStatistics] = useState<WeekStatistics | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [lastUpdateTime, setLastUpdateTime] = useState('');

  const loadData = useCallback(async () => {
    const results = await Promise.all([getTodayStatistics(), getWeekStatistics()]);
    if (results[0]?.todayStatistics) {
      setStatistics(results[0].todayStatistics);
      setLastUpdateTime(new Date().toLocaleTimeString());
    }
    if (results[1]?.weekStatistics) {
      setWeekStatistics(results[1].weekStatistics);
    }
  }, []);

  // 管理员兼送水(F-USR-023):统计 Tab 展示本人(关联账号)配送统计,不再重定向管理枢纽
  // 首载 + 30 秒轮询（weapp 行为）
  useEffect(() => {
    loadData().catch((e) => console.error('加载统计数据失败:', e));
    const timer = setInterval(() => {
      loadData().catch((e) => console.error('加载统计数据失败:', e));
    }, 30000);
    return () => clearInterval(timer);
  }, [loadData]);

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await loadData();
    } finally {
      setRefreshing(false);
    }
  }, [loadData]);

  const chartData = useMemo(
    () =>
      weekStatistics?.dailyStats?.map((d) => ({
        date: d.date,
        orderCount: d.orderCount,
        earning: centsToYuan(d.earningCents),
      })) ?? [],
    [weekStatistics],
  );

  const openHistory = useCallback(
    (tabIndex: number) => {
      router.push({ pathname: '/history-orders', params: { tab: String(tabIndex) } });
    },
    [router],
  );

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <SafeAreaView style={styles.safeArea} edges={['top']}>
        <ScrollView
          contentContainerStyle={styles.content}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}>
          <ThemedText type="small" themeColor="textSecondary">
            {getTodayDate()}
          </ThemedText>

          <View style={styles.cards}>
            <StatCard
              label="今日总订单"
              value={<ThemedText type="subtitle">{statistics?.totalOrders ?? 0}</ThemedText>}
              onPress={() => openHistory(0)}
            />
            <StatCard
              label="已完成"
              value={<ThemedText type="subtitle">{statistics?.completedOrders ?? 0}</ThemedText>}
              onPress={() => openHistory(1)}
            />
            <StatCard
              label="未完成"
              value={<ThemedText type="subtitle">{statistics?.unfinishedOrders ?? 0}</ThemedText>}
              onPress={() => openHistory(2)}
            />
            <StatCard
              label="今日营收"
              highlight
              value={
                <ThemedText type="subtitle" themeColor="headerText">
                  <Money cents={statistics?.earningCents ?? 0} />
                </ThemedText>
              }
            />
          </View>

          <Card
            title="近7天统计"
            right={
              <ThemedText type="caption" themeColor="textSecondary">
                订单: {weekStatistics?.totalOrders ?? 0} | 营收:{' '}
                <Money cents={weekStatistics?.totalEarningCents ?? 0} />
              </ThemedText>
            }>
            <WeekLineChart data={chartData} />
          </Card>

          {lastUpdateTime ? (
            <ThemedText type="caption" themeColor="textTertiary" style={styles.tip}>
              数据更新于: {lastUpdateTime}
            </ThemedText>
          ) : null}
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
    gap: Spacing.three,
    padding: Spacing.four,
  },
  content: {
    gap: Spacing.three,
    paddingBottom: Spacing.six,
  },
  cards: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.three,
  },
  tip: {
    textAlign: 'center',
  },
});
