import { useCallback, useEffect, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { Money, centsToYuan } from '@aquarush/common';
import type {
  DeliveryWorkerOverviewVo,
  DeliveryWorkerRankingItemVo,
  DailyStatisticVo,
  PendingOrderBreakdownVo,
  UserStatisticsVo,
} from '@aquarush/common';

import { Card } from '@/components/card';
import { StatCard } from '@/components/stat-card';
import { StatCardGrid } from '@/components/stat-card-grid';
import { ThemedText } from '@/components/themed-text';
import { WeekLineChart } from '@/components/week-line-chart';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import {
  getDailyStatistics,
  getDeliveryWorkerRanking,
  getDeliveryWorkerStatistics,
  getPendingOrderBreakdown,
  getUserStatistics,
} from '@/services/admin';

interface DashboardData {
  breakdown: PendingOrderBreakdownVo | null;
  workers: DeliveryWorkerOverviewVo | null;
  users: Pick<UserStatisticsVo, 'totalUsers' | 'todayNewUsers' | 'activeUsers'> | null;
  ranking: readonly DeliveryWorkerRankingItemVo[];
  daily: readonly DailyStatisticVo[];
}

function daysAgo(days: number): Date {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d;
}

export function ManageDashboard() {
  const theme = useTheme();
  const [data, setData] = useState<DashboardData>({
    breakdown: null,
    workers: null,
    users: null,
    ranking: [],
    daily: [],
  });
  const [refreshing, setRefreshing] = useState(false);

  const loadAll = useCallback(async () => {
    const results = await Promise.all([
      getPendingOrderBreakdown(),
      getDeliveryWorkerStatistics(),
      getUserStatistics(),
      getDeliveryWorkerRanking(10),
      getDailyStatistics(daysAgo(6), new Date()),
    ]);
    setData({
      breakdown: results[0]?.pendingOrderBreakdown ?? null,
      workers: results[1]?.deliveryWorkerStatistics ?? null,
      users: results[2]?.userStatistics ?? null,
      ranking: results[3]?.deliveryWorkerRanking ?? [],
      daily: results[4]?.dailyStatistics ?? [],
    });
  }, []);

  useEffect(() => {
    loadAll().catch((e) => console.error('加载经营看板失败:', e));
  }, [loadAll]);

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await loadAll();
    } finally {
      setRefreshing(false);
    }
  }, [loadAll]);

  const breakdown = data.breakdown;
  const workers = data.workers;
  const users = data.users;

  const chartData = data.daily.map((d) => ({
    date: d.date,
    orderCount: d.orderCount,
    earning: centsToYuan(d.revenueCents),
  }));

  return (
    <ScrollView
      style={styles.scroll}
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}>
      <ThemedText type="small" themeColor="textSecondary">
        待处理订单
      </ThemedText>
      <StatCardGrid style={styles.cards}>
        <StatCard label="待支付" value={<ThemedText type="subtitle">{breakdown?.pendingPayment ?? 0}</ThemedText>} />
        <StatCard label="待派单" value={<ThemedText type="subtitle">{breakdown?.pendingDispatch ?? 0}</ThemedText>} />
        <StatCard label="待配送" value={<ThemedText type="subtitle">{breakdown?.pendingDelivery ?? 0}</ThemedText>} />
        <StatCard
          label="合计"
          highlight
          value={
            <ThemedText type="subtitle" themeColor="headerText">
              {breakdown?.total ?? 0}
            </ThemedText>
          }
        />
      </StatCardGrid>

      <ThemedText type="small" themeColor="textSecondary">
        送水员
      </ThemedText>
      <StatCardGrid style={styles.cards}>
        <StatCard label="送水员总数" value={<ThemedText type="subtitle">{workers?.totalWorkers ?? 0}</ThemedText>} />
        <StatCard label="今日在岗" value={<ThemedText type="subtitle">{workers?.todayActiveWorkers ?? 0}</ThemedText>} />
        <StatCard label="配送中订单" value={<ThemedText type="subtitle">{workers?.deliveringOrders ?? 0}</ThemedText>} />
        <StatCard label="今日完成" value={<ThemedText type="subtitle">{workers?.todayCompletedOrders ?? 0}</ThemedText>} />
      </StatCardGrid>

      <ThemedText type="small" themeColor="textSecondary">
        用户
      </ThemedText>
      <StatCardGrid style={styles.cards}>
        <StatCard label="总用户" value={<ThemedText type="subtitle">{users?.totalUsers ?? 0}</ThemedText>} />
        <StatCard label="今日新增" value={<ThemedText type="subtitle">{users?.todayNewUsers ?? 0}</ThemedText>} />
        <StatCard label="活跃用户" value={<ThemedText type="subtitle">{users?.activeUsers ?? 0}</ThemedText>} />
      </StatCardGrid>

      <Card
        title="送水员排行榜"
        right={
          <ThemedText type="caption" themeColor="textSecondary">
            按今日完成单量
          </ThemedText>
        }>
        {data.ranking.length === 0 ? (
          <ThemedText type="small" themeColor="textTertiary">
            暂无排行数据
          </ThemedText>
        ) : (
          data.ranking.map((item, index) => (
            <View
              key={item.workerId}
              style={[
                styles.rankRow,
                index > 0 && { borderTopColor: theme.border },
                index > 0 && styles.rankRowBorder,
              ]}>
              <ThemedText type="smallBold" themeColor={index < 3 ? 'primary' : 'textSecondary'}>
                {index + 1}
              </ThemedText>
              <ThemedText type="smallBold" style={styles.rankName}>
                {item.name}
              </ThemedText>
              <ThemedText type="caption" themeColor="textTertiary">
                今日 {item.todayCompletedOrders} 单
              </ThemedText>
              <ThemedText type="smallBold" themeColor="primary">
                <Money cents={item.totalEarningsCents} />
              </ThemedText>
            </View>
          ))
        )}
      </Card>

      <Card
        title="近7天趋势"
        right={
          <ThemedText type="caption" themeColor="textSecondary">
            订单 + 营收
          </ThemedText>
        }>
        <WeekLineChart data={chartData} />
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scroll: {
    flex: 1,
  },
  content: {
    gap: Spacing.two,
    paddingBottom: Spacing.six,
  },
  cards: {
    marginBottom: Spacing.two,
  },
  rankRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    paddingVertical: Spacing.two,
  },
  rankRowBorder: {
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  rankName: {
    flex: 1,
  },
});
