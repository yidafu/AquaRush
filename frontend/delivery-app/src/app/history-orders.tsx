import { useCallback, useEffect, useRef, useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { useFocusEffect, useLocalSearchParams } from 'expo-router';
import { OrderStatus, ORDER_STATUS_TEXT, usePagedList } from '@aquarush/common';

import { EmptyState } from '@/components/empty-state';
import { Input } from '@/components/input';
import { ListFooterStatus } from '@/components/list-footer-status';
import { OrderCard, type OrderCardData } from '@/components/order-card';
import { SegmentedTabs, type SegmentedTabItem } from '@/components/segmented-tabs';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { getDeliveryWorkerHistoryOrders } from '@/services/delivery';

const PAGE_SIZE = 20;

// 历史订单 Tab:全部/已完成/已取消/已退款 —— 标题取 common ORDER_STATUS_TEXT(单来源);
// 送达即完成(无「待收货」态,PRD R-ORD-11 2026-10 简化)
const HISTORY_STATUSES: (OrderStatus | null)[] = [
  null,
  OrderStatus.COMPLETED,
  OrderStatus.CANCELLED,
  OrderStatus.REFUNDED,
];

const TAB_LIST: SegmentedTabItem[] = HISTORY_STATUSES.map((status) => ({
  title: status == null ? '全部' : ORDER_STATUS_TEXT[status],
}));

export default function HistoryOrdersScreen() {
  const { tab } = useLocalSearchParams<{ tab?: string }>();

  const [currentTab, setCurrentTab] = useState(() => {
    if (tab === undefined) return 0;
    const index = parseInt(tab, 10);
    return !Number.isNaN(index) && index >= 0 && index < TAB_LIST.length ? index : 0;
  });
  const [keyword, setKeyword] = useState('');
  const keywordRef = useRef('');

  const {
    items: orders,
    loading,
    refreshing,
    loadingMore,
    hasMore,
    refresh,
    loadMore,
    setParams,
  } = usePagedList<OrderCardData>({
    fetchPage: async (page, params) => {
      const { status, keyword: kw } = params as { status?: OrderStatus; keyword?: string };
      const res = await getDeliveryWorkerHistoryOrders(status, kw || undefined, page, PAGE_SIZE);
      const data = res?.deliveryWorkerHistoryOrders;
      return {
        items: [...(data?.content ?? [])] as OrderCardData[],
        hasMore: (data?.totalPages ?? 0) > page + 1,
      };
    },
    // 统计页深链:?tab=0/1/2(今日总订单→全部、已完成→已完成、未完成→已取消)
    initialParams: {
      status: HISTORY_STATUSES[currentTab],
      keyword: '',
    },
  });

  // Tab 深链参数变化(如从统计页跳转后再次进入)时切换并回到第一页
  useEffect(() => {
    if (tab === undefined) return;
    const index = parseInt(tab, 10);
    if (!Number.isNaN(index) && index >= 0 && index < TAB_LIST.length && index !== currentTab) {
      setCurrentTab(index);
      setParams({ status: HISTORY_STATUSES[index], keyword: keywordRef.current }).catch(() => {});
    }
  }, [tab, currentTab, setParams]);

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

  const handleTabClick = useCallback(
    (index: number) => {
      if (index === currentTab) return;
      setCurrentTab(index);
      setParams({ status: HISTORY_STATUSES[index], keyword: keywordRef.current }).catch(() => {});
    },
    [currentTab, setParams],
  );

  const handleSearch = useCallback(() => {
    keywordRef.current = keyword.trim();
    setParams({ status: HISTORY_STATUSES[currentTab], keyword: keywordRef.current }).catch(
      () => {},
    );
  }, [keyword, currentTab, setParams]);

  const handleRefresh = useCallback(() => {
    refresh().catch(() => {});
  }, [refresh]);

  const handleLoadMore = useCallback(() => {
    loadMore().catch(() => {});
  }, [loadMore]);

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <View style={styles.searchBar}>
        <Input
          placeholder="搜索地址、姓名、手机号、商品"
          value={keyword}
          onChangeText={setKeyword}
          returnKeyType="search"
          onSubmitEditing={handleSearch}
          containerStyle={styles.searchInput}
        />
      </View>
      <View style={styles.tabs}>
        <SegmentedTabs current={currentTab} tabs={TAB_LIST} onChange={handleTabClick} />
      </View>
      <FlatList
        data={orders}
        keyExtractor={(order) => order.id}
        renderItem={({ item }) => <OrderCard order={item} type="history" />}
        contentContainerStyle={styles.listContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}
        onEndReached={handleLoadMore}
        onEndReachedThreshold={0.3}
        ListEmptyComponent={
          loading ? null : (
            <EmptyState icon="📋" title="暂无历史订单" description="下拉可刷新" />
          )
        }
        ListFooterComponent={
          <ListFooterStatus
            loading={loading || loadingMore}
            noMore={!hasMore}
            hasItems={orders.length > 0}
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
  searchBar: {
    paddingHorizontal: Spacing.three,
    paddingTop: Spacing.three,
  },
  searchInput: {
    borderRadius: Spacing.two,
  },
  tabs: {
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.three,
  },
  listContent: {
    paddingHorizontal: Spacing.three,
    paddingBottom: Spacing.six,
    flexGrow: 1,
  },
});
