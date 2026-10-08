import { useCallback, useEffect, useRef, useState } from 'react';
import { FlatList, Pressable, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { OrderStatus, ORDER_STATUS_TEXT } from '@aquarush/common';
import type { Order, OrderVoPage } from '@aquarush/common';

import { EmptyState } from '@/components/empty-state';
import { Input } from '@/components/input';
import { ListFooterStatus } from '@/components/list-footer-status';
import { OrderCard } from '@/components/order-card';
import { ThemedText } from '@/components/themed-text';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { getAdminOrders } from '@/services/admin';

const PAGE_SIZE = 20;

const STATUS_OPTIONS: { label: string; value?: OrderStatus }[] = [
  { label: '全部' },
  ...Object.values(OrderStatus).map((value) => ({
    label: ORDER_STATUS_TEXT[value] ?? value,
    value,
  })),
];

export function AdminOrders() {
  const theme = useTheme();
  const [orders, setOrders] = useState<Order[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<OrderStatus | undefined>(undefined);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const loadPage = useCallback(async (targetPage: number, filter: OrderStatus | undefined, kw: string, append: boolean) => {
    const res = await getAdminOrders({ status: filter, search: kw || undefined, page: targetPage, size: PAGE_SIZE });
    const pageData: OrderVoPage | undefined = res?.orders;
    if (!pageData) return;
    setTotalPages(pageData.totalPages);
    setPage(pageData.number);
    setOrders((prev) => (append ? [...prev, ...pageData.content] : [...pageData.content]));
  }, []);

  const reload = useCallback(
    (filter: OrderStatus | undefined = statusFilter, kw: string = keyword) => {
      setLoading(true);
      loadPage(0, filter, kw, false)
        .catch((e) => console.error('加载全部订单失败:', e))
        .finally(() => setLoading(false));
    },
    [loadPage, statusFilter, keyword],
  );

  // 首次加载 + 状态/关键字变化时重新加载（关键字 400ms 防抖）
  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => reload(), keyword === '' ? 0 : 400);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter, keyword]);

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await loadPage(0, statusFilter, keyword, false);
    } finally {
      setRefreshing(false);
    }
  }, [loadPage, statusFilter, keyword]);

  const handleEndReached = useCallback(() => {
    if (loading || loadingMore || page >= totalPages - 1) return;
    setLoadingMore(true);
    loadPage(page + 1, statusFilter, keyword, true)
      .catch((e) => console.error('加载更多订单失败:', e))
      .finally(() => setLoadingMore(false));
  }, [loading, loadingMore, page, totalPages, statusFilter, keyword, loadPage]);

  return (
    <FlatList
      style={styles.list}
      data={orders}
      keyExtractor={(item) => item.id}
      renderItem={({ item }) => <OrderCard order={item} type="history" />}
      contentContainerStyle={styles.listContent}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}
      onEndReached={handleEndReached}
      onEndReachedThreshold={0.4}
      ListHeaderComponent={
        <View style={styles.header}>
          <Input
            placeholder="搜索订单号"
            value={keyword}
            onChangeText={setKeyword}
            containerStyle={styles.search}
            returnKeyType="search"
          />
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.chips}>
            {STATUS_OPTIONS.map((option) => {
              const active = statusFilter === option.value;
              return (
                <Pressable
                  key={option.label}
                  onPress={() => setStatusFilter(option.value)}
                  style={({ pressed }) => [
                    styles.chip,
                    { borderColor: theme.border, backgroundColor: active ? theme.primaryBg : theme.card },
                    pressed && styles.chipPressed,
                  ]}>
                  <ThemedText type="small" themeColor={active ? 'primary' : 'textSecondary'}>
                    {option.label}
                  </ThemedText>
                </Pressable>
              );
            })}
          </ScrollView>
          {loading ? (
            <ThemedText type="caption" themeColor="textTertiary" style={styles.tip}>
              加载中...
            </ThemedText>
          ) : null}
        </View>
      }
      ListEmptyComponent={
        loading ? null : (
          <EmptyState icon="📦" title="暂无订单" description="下拉刷新或调整筛选条件" />
        )
      }
      ListFooterComponent={
        <ListFooterStatus
          loading={loadingMore}
          noMore={page >= totalPages - 1}
          hasItems={orders.length > 0}
        />
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
  header: {
    gap: Spacing.two,
    marginBottom: Spacing.one,
  },
  search: {
    marginBottom: Spacing.one,
  },
  chips: {
    gap: Spacing.two,
    paddingVertical: Spacing.one,
  },
  chip: {
    borderRadius: Radius.pill,
    borderWidth: 1,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.one,
  },
  chipPressed: {
    opacity: 0.7,
  },
  tip: {
    textAlign: 'center',
    paddingVertical: Spacing.two,
  },
});
