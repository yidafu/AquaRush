import { useCallback, useEffect, useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { Money, DeliveryWorkerStatus } from '@aquarush/common';
import type { DeliveryWorker, DeliveryWorkerOverview } from '@aquarush/common';

import { Card } from '@/components/card';
import { EmptyState } from '@/components/empty-state';
import { OrderInfoRow } from '@/components/order-info-row';
import { StatCard } from '@/components/stat-card';
import { StatCardGrid } from '@/components/stat-card-grid';
import { StatusBadge } from '@/components/status-badge';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { getDeliveryWorkerStatistics, getDeliveryWorkers } from '@/services/admin';

interface WorkersData {
  overview: DeliveryWorkerOverview | null;
  workers: readonly DeliveryWorker[];
}

export function AdminWorkers() {
  const [data, setData] = useState<WorkersData>({ overview: null, workers: [] });
  const [refreshing, setRefreshing] = useState(false);

  const loadAll = useCallback(async () => {
    const results = await Promise.all([getDeliveryWorkerStatistics(), getDeliveryWorkers()]);
    setData({
      overview: results[0]?.deliveryWorkerStatistics ?? null,
      workers: results[1]?.deliveryWorkers ?? [],
    });
  }, []);

  useEffect(() => {
    loadAll().catch((e) => console.error('加载送水员失败:', e));
  }, [loadAll]);

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await loadAll();
    } finally {
      setRefreshing(false);
    }
  }, [loadAll]);

  const overview = data.overview;

  return (
    <FlatList
      style={styles.list}
      data={data.workers}
      keyExtractor={(item) => item.id}
      renderItem={({ item }) => {
        const online = item.onlineStatus === DeliveryWorkerStatus.ONLINE;
        return (
          <Card>
            <View style={styles.header}>
              <ThemedText type="smallBold">{item.name}</ThemedText>
              <View style={styles.badges}>
                <StatusBadge label={online ? '在线' : '离线'} tone={online ? 'success' : 'info'} />
                <StatusBadge label={item.isAvailable ? '可接单' : '休息中'} tone={item.isAvailable ? 'success' : 'warning'} />
              </View>
            </View>
            <OrderInfoRow label="电话" value={item.phone || '-'} />
            <OrderInfoRow label="在途任务" value={String(item.currentTaskCount)} />
            <OrderInfoRow
              label="累计收入"
              value={
                <ThemedText type="smallBold" themeColor="primary">
                  <Money cents={item.earningCents ?? 0} />
                </ThemedText>
              }
            />
          </Card>
        );
      }}
      contentContainerStyle={styles.listContent}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}
      ListHeaderComponent={
        <StatCardGrid style={styles.cards}>
          <StatCard label="送水员总数" value={<ThemedText type="subtitle">{overview?.totalWorkers ?? 0}</ThemedText>} />
          <StatCard label="今日在岗" value={<ThemedText type="subtitle">{overview?.todayActiveWorkers ?? 0}</ThemedText>} />
          <StatCard label="配送中订单" value={<ThemedText type="subtitle">{overview?.deliveringOrders ?? 0}</ThemedText>} />
          <StatCard label="今日完成" value={<ThemedText type="subtitle">{overview?.todayCompletedOrders ?? 0}</ThemedText>} />
        </StatCardGrid>
      }
      ListEmptyComponent={
        <EmptyState icon="🚚" title="暂无送水员" description="下拉可刷新" />
      }
    />
  );
}

const styles = StyleSheet.create({
  list: {
    flex: 1,
  },
  listContent: {
    paddingBottom: Spacing.six,
    gap: Spacing.three,
  },
  cards: {
    marginBottom: Spacing.two,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.two,
  },
  badges: {
    flexDirection: 'row',
    gap: Spacing.two,
    alignItems: 'center',
  },
});
