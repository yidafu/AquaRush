import { useCallback, useRef, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet } from 'react-native';
import { useFocusEffect } from 'expo-router';
import { usePagedList } from '@aquarush/common';
import type { DailyReconciliation } from '@aquarush/common';

import { EmptyState } from '@/components/empty-state';
import { ListFooterStatus } from '@/components/list-footer-status';
import { ReconciliationCard } from '@/components/reconciliation-card';
import { ReconciliationOrderDetailSheet } from '@/components/reconciliation-order-detail-sheet';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { getDailyReconciliations } from '@/services/dailyCollection';

const PAGE_SIZE = 10;

export default function ReconciliationRecordsScreen() {
  const [detailDate, setDetailDate] = useState<string | null>(null);
  const {
    items: records,
    loading,
    refreshing,
    loadingMore,
    hasMore,
    refresh,
    loadMore,
  } = usePagedList<DailyReconciliation>({
    // 服务端分页(规则 #13):按对账日期倒序,pageInfo.hasNext 驱动触底加载
    fetchPage: async (page) => {
      const res = await getDailyReconciliations(page, PAGE_SIZE);
      const pageData = res?.dailyReconciliations;
      return {
        items: [...(pageData?.list ?? [])],
        hasMore: pageData?.pageInfo.hasNext ?? false,
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

  const handleOpenDetail = useCallback((record: DailyReconciliation) => {
    setDetailDate(record.reconciliationDate);
  }, []);

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <FlatList
        data={records}
        keyExtractor={(record) => record.id}
        renderItem={({ item }) => (
          <Pressable onPress={() => handleOpenDetail(item)}>
            <ReconciliationCard record={item} />
          </Pressable>
        )}
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

      {/* G29 对账逐单明细:点击卡片查看当日订单流水 */}
      <ReconciliationOrderDetailSheet
        visible={detailDate !== null}
        reconciliationDate={detailDate}
        onClose={() => setDetailDate(null)}
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
