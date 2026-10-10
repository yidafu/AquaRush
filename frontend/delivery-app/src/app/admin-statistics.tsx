import { useCallback, useEffect, useMemo, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { useRouter } from 'expo-router';
import { Money, ORDER_STATUS_TEXT, centsToYuan } from '@aquarush/common';
import type { DeliveryWorkerRankingItemVo, DailyStatisticVo, OrderStatisticsVo, OrderStatus } from '@aquarush/common';

import { Card } from '@/components/card';
import { SegmentedTabs } from '@/components/segmented-tabs';
import { StatCard } from '@/components/stat-card';
import { StatCardGrid } from '@/components/stat-card-grid';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { WeekLineChart } from '@/components/week-line-chart';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/hooks/use-auth';
import { useTheme } from '@/hooks/use-theme';
import { getAdminStatistics } from '@/services/admin';

interface AdminStatisticsData {
  daily: readonly DailyStatisticVo[];
  summary: OrderStatisticsVo | null;
  ranking: readonly DeliveryWorkerRankingItemVo[];
}

function daysAgo(days: number): Date {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d;
}

const RANGE_OPTIONS = ['近7天', '近30天'];

/** 数据统计页(F-STA-001/002/005 移动端聚合视图):今日概览 + 日趋势 + 订单状态分布 + 送水员排行。 */
export default function AdminStatisticsScreen() {
  const router = useRouter();
  const theme = useTheme();
  const { isAdmin } = useAuth();
  const [rangeIndex, setRangeIndex] = useState(0);
  const [data, setData] = useState<AdminStatisticsData>({ daily: [], summary: null, ranking: [] });
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    if (!isAdmin) router.replace('/');
  }, [isAdmin, router]);

  const rangeDays = rangeIndex === 0 ? 7 : 30;

  const loadData = useCallback(async () => {
    const res = await getAdminStatistics(daysAgo(rangeDays - 1), new Date());
    setData({
      daily: [...(res?.dailyStatistics ?? [])],
      summary: res?.orderStatistics ?? null,
      ranking: [...(res?.deliveryWorkerRanking ?? [])],
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rangeDays]);

  useEffect(() => {
    loadData().catch((e) => console.error('加载数据统计失败:', e));
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
      data.daily.map((d) => ({
        date: d.date,
        orderCount: d.orderCount,
        earning: centsToYuan(d.revenueCents),
      })),
    [data.daily],
  );

  const statusDistribution = useMemo(() => {
    const max = Math.max(...(data.summary?.statusDistribution.map((s) => s.count) ?? []), 0);
    return (data.summary?.statusDistribution ?? []).map((s) => ({
      ...s,
      ratio: max > 0 ? s.count / max : 0,
      label: ORDER_STATUS_TEXT[s.status as OrderStatus] ?? s.status,
    }));
  }, [data.summary]);

  const summary = data.summary;

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}>
        <SegmentedTabs
          current={rangeIndex}
          tabs={RANGE_OPTIONS.map((t) => ({ title: t }))}
          onChange={setRangeIndex}
        />

        <StatCardGrid>
          <StatCard
            label="订单总数"
            value={<ThemedText type="subtitle">{summary?.totalOrders ?? 0}</ThemedText>}
          />
          <StatCard
            label="营收总额"
            highlight
            value={
              <ThemedText type="subtitle" themeColor="headerText">
                <Money cents={summary?.totalRevenueCents ?? 0} />
              </ThemedText>
            }
          />
          <StatCard
            label="已完成"
            value={<ThemedText type="subtitle">{summary?.completedOrders ?? 0}</ThemedText>}
          />
          <StatCard
            label="平均客单价"
            value={
              <ThemedText type="subtitle">
                <Money cents={summary?.averageOrderValueCents ?? 0} />
              </ThemedText>
            }
          />
        </StatCardGrid>

        <Card
          title={`近${rangeDays}天订单与营收`}
          right={
            <ThemedText type="caption" themeColor="textSecondary">
              订单 + 营收(元)
            </ThemedText>
          }>
          <WeekLineChart data={chartData} />
        </Card>

        <Card title="订单状态分布">
          {statusDistribution.length === 0 ? (
            <ThemedText type="small" themeColor="textTertiary">
              暂无数据
            </ThemedText>
          ) : (
            <View style={styles.distList}>
              {statusDistribution.map((item) => (
                <View key={item.status} style={styles.distRow}>
                  <ThemedText type="caption" themeColor="textSecondary" style={styles.distLabel}>
                    {item.label}
                  </ThemedText>
                  <View style={[styles.distTrack, { backgroundColor: theme.border }]}>
                    <View
                      style={[
                        styles.distFill,
                        { backgroundColor: theme.primary, width: `${Math.max(item.ratio * 100, 2)}%` },
                      ]}
                    />
                  </View>
                  <ThemedText type="caption" style={styles.distCount}>
                    {item.count}
                  </ThemedText>
                </View>
              ))}
            </View>
          )}
        </Card>

        <Card title="送水员排行 Top5">
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
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    gap: Spacing.three,
    padding: Spacing.three,
    paddingBottom: Spacing.six,
  },
  distList: {
    gap: Spacing.two,
  },
  distRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  distLabel: {
    width: 72,
  },
  distTrack: {
    flex: 1,
    height: 8,
    borderRadius: 4,
    overflow: 'hidden',
  },
  distFill: {
    height: '100%',
    borderRadius: 4,
  },
  distCount: {
    width: 40,
    textAlign: 'right',
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
