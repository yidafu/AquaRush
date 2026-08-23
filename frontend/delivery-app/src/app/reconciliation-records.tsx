import { useCallback, useRef } from 'react';
import { FlatList, RefreshControl, StyleSheet } from 'react-native';
import { useFocusEffect } from 'expo-router';
import { usePagedList } from '@aquarush/common';
import type { DailyReconciliation } from '@aquarush/common';

import { EmptyState } from '@/components/empty-state';
import { ListFooterStatus } from '@/components/list-footer-status';
import { ReconciliationCard } from '@/components/reconciliation-card';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { getDailyReconciliations } from '@/services/dailyCollection';

const PAGE_SIZE = 10;

export default function ReconciliationRecordsScreen() {
  const {
    items: records,
    loading,
    refreshing,
    loadingMore,
    hasMore,
    refresh,
    loadMore,
  } = usePagedList<DailyReconciliation>({
    fetchPage: async (page) => {
      const res = await getDailyReconciliations();
      // 按日期倒序（weapp 行为），客户端分页
      const sorted = [...(res?.dailyReconciliations ?? [])].sort(
        (a, b) =>
          new Date(b.reconciliationDate).getTime() - new Date(a.reconciliationDate).getTime(),
      );
      const start = page * PAGE_SIZE;
      return {
        items: sorted.slice(start, start + PAGE_SIZE),
        hasMore: start + PAGE_SIZE < sorted.length,
      };
    },
  });

  // 首屏由 usePagedList 自动加载;再次聚焦时刷新(返回本页取最新数据)
  const firstFocus = useRef(true);
  useFocusEffect(
    useCallback(() => {
      if (firstFocus.current) {
        firstFocus.current = false;
        return;
      }
      refresh().catch(() => {});
    }, [refresh]),
  );

  const handleRefresh = useCallback(() => {
    refresh().catch(() => {});
  }, [refresh]);

  const handleLoadMore = useCallback(() => {
    loadMore().catch(() => {});
  }, [loadMore]);

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
            <EmptyState icon="📜" title="暂无对账记录" description="下拉可刷新" />
          )
        }
        ListFooterComponent={
          <ListFooterStatus
            loading={loading || loadingMore}
            noMore={!hasMore}
            hasItems={records.length > 0}
          />
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
});
