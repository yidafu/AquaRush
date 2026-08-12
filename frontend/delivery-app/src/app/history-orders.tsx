import { useCallback, useEffect, useRef, useState } from 'react';
import { Alert, FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { useFocusEffect, useLocalSearchParams } from 'expo-router';
import { OrderStatus } from '@aquarush/common';

import { EmptyState } from '@/components/empty-state';
import { Input } from '@/components/input';
import { OrderCard, type OrderCardData } from '@/components/order-card';
import { SegmentedTabs, type SegmentedTabItem } from '@/components/segmented-tabs';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { getDeliveryWorkerHistoryOrders } from '@/services/delivery';

const PAGE_SIZE = 20;

const TAB_LIST: { title: string; status?: OrderStatus }[] = [
  { title: '全部' },
  { title: '已完成', status: OrderStatus.COMPLETED },
  { title: '已取消', status: OrderStatus.CANCELLED },
  { title: '已退款', status: OrderStatus.REFUNDED },
];

export default function HistoryOrdersScreen() {
  const { tab } = useLocalSearchParams<{ tab?: string }>();

  const [currentTab, setCurrentTab] = useState(0);
  const [orders, setOrders] = useState<OrderCardData[]>([]);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const keywordRef = useRef('');

  // 统计页深链：?tab=0/1/2（今日总订单→全部、已完成→已完成、未完成→已取消）
  useEffect(() => {
    if (tab === undefined) return;
    const index = parseInt(tab, 10);
    if (!Number.isNaN(index) && index >= 0 && index < TAB_LIST.length) {
      setCurrentTab(index);
      setPage(0);
      setOrders([]);
      setHasMore(true);
    }
  }, [tab]);

  const loadHistoryOrders = useCallback(
    async (reset: boolean) => {
      const currentPage = reset ? 0 : page;
      const status = TAB_LIST[currentTab].status;
      try {
        setLoading(true);
        const res = await getDeliveryWorkerHistoryOrders(
          status,
          keywordRef.current || undefined,
          currentPage,
          PAGE_SIZE,
        );
        const data = res?.deliveryWorkerHistoryOrders;
        if (data) {
          const content = [...(data.content ?? [])] as OrderCardData[];
          setOrders((prev) => (reset ? content : [...prev, ...content]));
          setHasMore(currentPage + 1 < data.totalPages);
          setPage(currentPage + 1);
        }
      } catch (e) {
        console.error('加载历史订单失败:', e);
        Alert.alert('加载失败');
      } finally {
        setLoading(false);
      }
    },
    [page, currentTab],
  );

  useFocusEffect(
    useCallback(() => {
      setPage(0);
      setOrders([]);
      setHasMore(true);
      loadHistoryOrders(true).catch(() => {});
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentTab]),
  );

  const handleTabClick = useCallback(
    (index: number) => {
      if (index === currentTab) return;
      setCurrentTab(index);
      setPage(0);
      setOrders([]);
      setHasMore(true);
    },
    [currentTab],
  );

  const handleSearch = useCallback(() => {
    keywordRef.current = keyword.trim();
    setPage(0);
    setOrders([]);
    setHasMore(true);
    loadHistoryOrders(true).catch(() => {});
  }, [keyword, loadHistoryOrders]);

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await loadHistoryOrders(true);
    } finally {
      setRefreshing(false);
    }
  }, [loadHistoryOrders]);

  const handleLoadMore = useCallback(() => {
    if (!loading && hasMore) {
      loadHistoryOrders(false).catch(() => {});
    }
  }, [loading, hasMore, loadHistoryOrders]);

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
        <SegmentedTabs
          current={currentTab}
          tabs={TAB_LIST as SegmentedTabItem[]}
          onChange={handleTabClick}
        />
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
            <EmptyState
              icon={<ThemedText style={styles.emptyIcon}>📋</ThemedText>}
              title="暂无历史订单"
              description="下拉可刷新"
            />
          )
        }
        ListFooterComponent={
          loading ? (
            <ThemedText type="caption" themeColor="textTertiary" style={styles.footer}>
              加载中...
            </ThemedText>
          ) : !hasMore && orders.length > 0 ? (
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
  emptyIcon: {
    fontSize: 28,
    lineHeight: 32,
  },
  footer: {
    textAlign: 'center',
    paddingVertical: Spacing.three,
  },
});
