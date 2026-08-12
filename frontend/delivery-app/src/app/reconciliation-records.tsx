import { useCallback, useState } from 'react';
import { Alert, FlatList, RefreshControl, StyleSheet } from 'react-native';
import { useFocusEffect } from 'expo-router';
import type { DailyReconciliation } from '@aquarush/common';

import { EmptyState } from '@/components/empty-state';
import { ReconciliationCard } from '@/components/reconciliation-card';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { getDailyReconciliations } from '@/services/dailyCollection';

const PAGE_SIZE = 10;

export default function ReconciliationRecordsScreen() {
  const [records, setRecords] = useState<DailyReconciliation[]>([]);
  const [allData, setAllData] = useState<DailyReconciliation[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  const loadRecords = useCallback(async () => {
    try {
      setLoading(true);
      const res = await getDailyReconciliations();
      if (res?.dailyReconciliations) {
        // 按日期倒序（weapp 行为）
        const sorted = [...res.dailyReconciliations].sort(
          (a, b) =>
            new Date(b.reconciliationDate).getTime() - new Date(a.reconciliationDate).getTime(),
        );
        setAllData(sorted);
        setRecords(sorted.slice(0, PAGE_SIZE));
        setPage(1);
        setHasMore(sorted.length > PAGE_SIZE);
      }
    } catch (e) {
      console.error('加载对账记录失败:', e);
      Alert.alert('加载失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      loadRecords().catch(() => {});
    }, [loadRecords]),
  );

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    setAllData([]);
    setPage(0);
    try {
      await loadRecords();
    } finally {
      setRefreshing(false);
    }
  }, [loadRecords]);

  const handleLoadMore = useCallback(() => {
    if (loading || !hasMore || allData.length === 0) return;
    const start = page * PAGE_SIZE;
    const end = start + PAGE_SIZE;
    setRecords((prev) => [...prev, ...allData.slice(start, end)]);
    setPage(page + 1);
    setHasMore(end < allData.length);
  }, [loading, hasMore, allData, page]);

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <FlatList
        data={records}
        keyExtractor={(record) => record.id}
        renderItem={({ item }) => <ReconciliationCard record={item} />}
        contentContainerStyle={styles.listContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}
        onEndReached={handleLoadMore}
        onEndReachedThreshold={0.3}
        ListEmptyComponent={
          loading ? null : (
            <EmptyState
              icon={<ThemedText style={styles.emptyIcon}>📜</ThemedText>}
              title="暂无对账记录"
              description="下拉可刷新"
            />
          )
        }
        ListFooterComponent={
          loading ? (
            <ThemedText type="caption" themeColor="textTertiary" style={styles.footer}>
              加载中...
            </ThemedText>
          ) : !hasMore && records.length > 0 ? (
            <ThemedText type="caption" themeColor="textTertiary" style={styles.footer}>
              没有更多了
            </ThemedText>
          ) : null
        }
      />
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  listContent: {
    padding: Spacing.three,
    paddingBottom: Spacing.six,
    flexGrow: 1,
  },
  emptyIcon: {
    fontSize: 28,
    lineHeight: 32,
  },
  footer: {
    textAlign: 'center',
    paddingVertical: Spacing.three,
  },
});
