import { useCallback, useEffect, useState } from 'react';
import { FlatList, RefreshControl, StyleSheet, TouchableOpacity, View } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { toast } from 'sonner-native';

import { EmptyState } from '@/components/empty-state';
import { SegmentedTabs } from '@/components/segmented-tabs';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Radius, Shadow, Spacing } from '@/constants/theme';
import { wsPushService, WsPushMessage } from '@/services/wsPush';
import { notificationService, WorkerNotificationType, WorkerNotificationVO } from '@/services/notification';
import { useTheme } from '@/hooks/use-theme';

const PAGE_SIZE = 10;

type TypeTabKey = 'ALL' | WorkerNotificationType;
type ReadTabKey = 'ALL' | 'UNREAD' | 'READ';

const TYPE_TABS: { title: string; key: TypeTabKey }[] = [
  { title: '全部', key: 'ALL' },
  { title: '任务', key: 'ORDER' },
  { title: '系统', key: 'SYSTEM' },
];

const READ_TABS: { title: string; key: ReadTabKey }[] = [
  { title: '全部', key: 'ALL' },
  { title: '未读', key: 'UNREAD' },
  { title: '已读', key: 'READ' },
];

/** 状态 Tab → read 筛选值:全部=不过滤,未读=false,已读=true */
function readTabToValue(key: ReadTabKey): boolean | undefined {
  if (key === 'ALL') return undefined;
  return key === 'UNREAD' ? false : true;
}

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
 * 双行筛选:第一行类型 Tab(全部/任务/系统),第二行已读状态 Tab(全部/未读/已读),两维度组合过滤。
 */
export default function MessagesScreen() {
  const router = useRouter();
  const theme = useTheme();

  const [messages, setMessages] = useState<WorkerNotificationVO[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [activeTypeTab, setActiveTypeTab] = useState<TypeTabKey>('ALL');
  const [activeReadTab, setActiveReadTab] = useState<ReadTabKey>('ALL');

  const typeValue = activeTypeTab === 'ALL' ? undefined : activeTypeTab;
  const readValue = readTabToValue(activeReadTab);

  const loadMessages = useCallback(
    async (reset: boolean, type?: WorkerNotificationType, read?: boolean) => {
      const currentPage = reset ? 0 : page;
      try {
        setLoading(true);
        const data = await notificationService.getMyNotifications(
          type ?? typeValue,
          read ?? readValue,
          currentPage,
          PAGE_SIZE,
        );
        const content = data.content ?? [];
        setMessages((prev) => (reset ? content : [...prev, ...content]));
        setHasMore(currentPage + 1 < (data.totalPages ?? 0));
        setPage(currentPage + 1);
      } catch (e) {
        console.error('加载消息失败:', e);
        toast.error('加载失败', { description: '请稍后重试' });
      } finally {
        setLoading(false);
      }
    },
    [page, typeValue, readValue],
  );

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

  /** 切换类型 Tab:重置分页并以新类型过滤重载 */
  const handleTypeTabChange = useCallback(
    (index: number) => {
      const key = TYPE_TABS[index].key;
      setActiveTypeTab(key);
      setPage(0);
      loadMessages(true, key === 'ALL' ? undefined : key, undefined).catch(() => {});
    },
    [loadMessages],
  );

  /** 切换已读状态 Tab:重置分页并以新状态过滤重载 */
  const handleReadTabChange = useCallback(
    (index: number) => {
      const key = READ_TABS[index].key;
      setActiveReadTab(key);
      setPage(0);
      loadMessages(true, undefined, readTabToValue(key)).catch(() => {});
    },
    [loadMessages],
  );

  const handleMarkRead = useCallback(
    async (item: WorkerNotificationVO) => {
      if (!item.read) {
        if (activeReadTab === 'UNREAD') {
          // 未读 Tab 下点击即读:从列表移除(否则残留灰色已读项)
          setMessages((prev) => prev.filter((m) => m.id !== item.id));
        } else {
          setMessages((prev) => prev.map((m) => (m.id === item.id ? { ...m, read: true } : m)));
        }
        notificationService.markRead(item.id).catch((e) => console.error('标记已读失败:', e));
      }
      if (item.type === 'ORDER' && item.relatedNo) {
        router.push({ pathname: '/order/[orderNo]', params: { orderNo: item.relatedNo } });
      }
    },
    [router, activeReadTab],
  );

  const handleMarkAllRead = useCallback(async () => {
    try {
      await notificationService.markAllRead();
      if (activeReadTab === 'UNREAD') {
        // 未读 Tab 下全部置读后列表应为空,重新拉取
        loadMessages(true).catch(() => {});
      } else {
        setMessages((prev) => prev.map((m) => ({ ...m, read: true })));
      }
    } catch (e) {
      console.error('全部已读失败:', e);
      toast.error('操作失败', { description: '请稍后重试' });
    }
  }, [activeReadTab, loadMessages]);

  return (
    <SafeAreaView style={styles.safeArea} edges={['top']}>
      <ThemedView style={styles.header}>
        <ThemedText style={styles.headerTitle}>消息中心</ThemedText>
        {messages.length > 0 && (
          <TouchableOpacity onPress={handleMarkAllRead}>
            <ThemedText type="small" themeColor="primary" style={styles.markAll}>
              全部已读
            </ThemedText>
          </TouchableOpacity>
        )}
      </ThemedView>

      {/* 第一行:类型 Tab(全部/任务/系统) */}
      <View style={styles.tabRow}>
        <SegmentedTabs
          current={TYPE_TABS.findIndex((t) => t.key === activeTypeTab)}
          tabs={TYPE_TABS}
          onChange={handleTypeTabChange}
        />
      </View>
      {/* 第二行:已读状态 Tab(全部/未读/已读),与类型 Tab 组合筛选 */}
      <View style={styles.tabRow}>
        <SegmentedTabs
          current={READ_TABS.findIndex((t) => t.key === activeReadTab)}
          tabs={READ_TABS}
          onChange={handleReadTabChange}
        />
      </View>

      <FlatList
        data={messages}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.listContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}
        onEndReached={handleLoadMore}
        onEndReachedThreshold={0.3}
        ListEmptyComponent={
          !loading ? (
            <EmptyState
              icon="📭"
              title={activeReadTab === 'UNREAD' ? '暂无未读消息' : activeReadTab === 'READ' ? '暂无已读消息' : '暂无消息'}
              description="订单与任务动态将在此展示"
            />
          ) : null
        }
        ListFooterComponent={
          hasMore && messages.length > 0 ? (
            <View style={styles.footer}>
              <ThemedText type="caption" themeColor="textTertiary">
                上拉加载更多
              </ThemedText>
            </View>
          ) : null
        }
        renderItem={({ item }) => (
          <TouchableOpacity onPress={() => handleMarkRead(item)}>
            <ThemedView type="card" style={styles.item}>
              <View style={styles.itemBody}>
                <View style={styles.itemTitleRow}>
                  {!item.read && <View style={[styles.dot, { backgroundColor: theme.primary }]} />}
                  <ThemedText
                    style={[styles.itemTitle, !item.read && styles.itemTitleUnread]}
                    numberOfLines={1}>
                    {item.title}
                  </ThemedText>
                  <ThemedText type="caption" themeColor="textTertiary" style={styles.itemTime}>
                    {formatRelativeTime(item.createdAt)}
                  </ThemedText>
                </View>
                {item.summary ? (
                  <ThemedText themeColor="textSecondary" style={styles.itemSummary} numberOfLines={2}>
                    {item.summary}
                  </ThemedText>
                ) : null}
              </View>
            </ThemedView>
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
    fontSize: 14,
    lineHeight: 20,
  },
  tabRow: {
    paddingHorizontal: Spacing.three,
    paddingBottom: Spacing.two,
  },
  listContent: {
    paddingBottom: Spacing.six,
  },
  item: {
    marginHorizontal: Spacing.three,
    marginBottom: Spacing.three,
    padding: Spacing.three,
    borderRadius: Radius.lg,
    ...Shadow.sm,
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
    marginRight: 6,
  },
  itemTitle: {
    flex: 1,
    fontSize: 15,
    lineHeight: 21,
  },
  itemTitleUnread: {
    fontWeight: '600',
  },
  itemTime: {
    fontSize: 11,
    lineHeight: 16,
  },
  itemSummary: {
    marginTop: Spacing.one,
    fontSize: 13,
    lineHeight: 19,
  },
  footer: {
    paddingVertical: Spacing.three,
    alignItems: 'center',
  },
});
