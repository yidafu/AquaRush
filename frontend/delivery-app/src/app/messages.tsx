import { useCallback, useEffect, useState } from 'react';
import { Alert, FlatList, RefreshControl, StyleSheet, TouchableOpacity, View } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';

import { EmptyState } from '@/components/empty-state';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { wsPushService, WsPushMessage } from '@/services/wsPush';
import { notificationService, WorkerNotificationVO } from '@/services/notification';
import { useAuth } from '@/hooks/use-auth';

const PAGE_SIZE = 10;

function formatRelativeTime(time: string): string {
  const date = new Date(time);
  const diff = Date.now() - date.getTime();
  const minute = 60_000;
  const hour = 60 * minute;
  const day = 24 * hour;
  if (diff < minute) return '刚刚';
  if (diff < hour) return `${Math.floor(diff / minute)}分钟前`;
  if (diff < day) return `${Math.floor(diff / hour)}小时前`;
  if (diff < 7 * day) return `${Math.floor(diff / day)}天前`;
  return date.toLocaleString('zh-CN');
}

/**
 * 送水工端消息中心(F-NTF-004,PRD 13-notification.md)
 * 本人任务/系统消息,分页 + 下拉刷新;点击标已读;WebSocket 推送到达时增量刷新。
 */
export default function MessagesScreen() {
  const router = useRouter();
  const { userInfo } = useAuth();

  const [messages, setMessages] = useState<WorkerNotificationVO[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  const loadMessages = useCallback(async (reset: boolean) => {
    const currentPage = reset ? 0 : page;
    try {
      setLoading(true);
      const data = await notificationService.getMyNotifications(currentPage, PAGE_SIZE);
      const content = data.content ?? [];
      setMessages((prev) => (reset ? content : [...prev, ...content]));
      setHasMore(currentPage + 1 < (data.totalPages ?? 0));
      setPage(currentPage + 1);
    } catch (e) {
      console.error('加载消息失败:', e);
      Alert.alert('加载失败', '请稍后重试');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useFocusEffect(
    useCallback(() => {
      loadMessages(true).catch(() => {});
    }, [loadMessages]),
  );

  // WebSocket 实时推送:到达时刷新第一页(新消息置顶)
  useEffect(() => {
    const unsubscribe = wsPushService.onMessage((_message: WsPushMessage) => {
      loadMessages(true).catch(() => {});
    });
    return unsubscribe;
  }, [loadMessages]);

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await loadMessages(true);
    } finally {
      setRefreshing(false);
    }
  }, [loadMessages]);

  const handleLoadMore = useCallback(() => {
    if (hasMore && !loading) {
      loadMessages(false).catch(() => {});
    }
  }, [hasMore, loading, loadMessages]);

  const handleMarkRead = useCallback(async (item: WorkerNotificationVO) => {
    if (!item.read) {
      setMessages((prev) => prev.map((m) => (m.id === item.id ? { ...m, read: true } : m)));
      notificationService.markRead(item.id).catch((e) => console.error('标记已读失败:', e));
    }
    if (item.type === 'ORDER' && item.relatedNo) {
      router.push({ pathname: '/order/[orderNo]', params: { orderNo: item.relatedNo } });
    }
  }, [router]);

  const handleMarkAllRead = useCallback(async () => {
    try {
      await notificationService.markAllRead();
      setMessages((prev) => prev.map((m) => ({ ...m, read: true })));
    } catch (e) {
      console.error('全部已读失败:', e);
      Alert.alert('操作失败', '请稍后重试');
    }
  }, []);

  return (
    <SafeAreaView style={styles.safeArea} edges={['top']}>
      <ThemedView style={styles.header}>
        <ThemedText style={styles.headerTitle}>消息中心</ThemedText>
        {messages.length > 0 && (
          <TouchableOpacity onPress={handleMarkAllRead}>
            <ThemedText style={styles.markAll}>全部已读</ThemedText>
          </TouchableOpacity>
        )}
      </ThemedView>

      <FlatList
        data={messages}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.listContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}
        onEndReached={handleLoadMore}
        onEndReachedThreshold={0.3}
        ListEmptyComponent={
          !loading ? <EmptyState icon="📭" title="暂无消息" description="订单与任务动态将在此展示" /> : null
        }
        ListFooterComponent={
          hasMore && messages.length > 0 ? (
            <View style={styles.footer}>
              <ThemedText style={styles.footerText}>上拉加载更多</ThemedText>
            </View>
          ) : null
        }
        renderItem={({ item }) => (
          <TouchableOpacity style={styles.item} onPress={() => handleMarkRead(item)}>
            <View style={styles.itemBody}>
              <View style={styles.itemTitleRow}>
                {!item.read && <View style={styles.dot} />}
                <ThemedText style={[styles.itemTitle, !item.read && styles.itemTitleUnread]} numberOfLines={1}>
                  {item.title}
                </ThemedText>
                <ThemedText style={styles.itemTime}>{formatRelativeTime(item.createdAt)}</ThemedText>
              </View>
              {item.summary ? (
                <ThemedText style={styles.itemSummary} numberOfLines={2}>
                  {item.summary}
                </ThemedText>
              ) : null}
            </View>
          </TouchableOpacity>
        )}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.four,
    paddingVertical: Spacing.three,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '600',
  },
  markAll: {
    color: '#1890ff',
    fontSize: 14,
  },
  listContent: {
    paddingBottom: Spacing.six,
  },
  item: {
    marginHorizontal: Spacing.three,
    marginBottom: Spacing.two,
    padding: Spacing.three,
    borderRadius: 12,
    backgroundColor: '#f0f7ff',
  },
  itemBody: {
    flex: 1,
  },
  itemTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#ff4d4f',
    marginRight: 6,
  },
  itemTitle: {
    flex: 1,
    fontSize: 15,
    color: '#666',
  },
  itemTitleUnread: {
    fontWeight: '600',
    color: '#333',
  },
  itemTime: {
    fontSize: 11,
    color: '#999',
  },
  itemSummary: {
    marginTop: Spacing.one,
    fontSize: 13,
    color: '#666',
    lineHeight: 19,
  },
  footer: {
    paddingVertical: Spacing.three,
    alignItems: 'center',
  },
  footerText: {
    color: '#999',
    fontSize: 12,
  },
});
