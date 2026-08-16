import { useCallback, useEffect, useState } from 'react';
import { Alert, RefreshControl, ScrollView, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect, useRouter } from 'expo-router';

import { EmptyState } from '@/components/empty-state';
import { FloatingButton } from '@/components/floating-button';
import { OrderCard } from '@/components/order-card';
import { SegmentedTabs } from '@/components/segmented-tabs';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import {
  acceptDelivery,
  getMyAssignedOrders,
  getMyDeliveringOrders,
  getPendingDeliveryOrders,
  startDelivery,
} from '@/services/delivery';
import { wsPushService, connectWsPushIfLoggedIn, showPushBanner, WsPushMessage } from '@/services/wsPush';
import { useAuth } from '@/hooks/use-auth';

const TAB_EMPTY_ICONS = ['📋', '🕐', '🚚'];

export default function TasksScreen() {
  const router = useRouter();
  const [currentTab, setCurrentTab] = useState(0);
  const [pendingOrders, setPendingOrders] = useState<any[]>([]);
  const [assignedOrders, setAssignedOrders] = useState<any[]>([]);
  const [deliveringOrders, setDeliveringOrders] = useState<any[]>([]);
  const [refreshing, setRefreshing] = useState(false);
  const { userInfo } = useAuth();

  // WebSocket 实时推送(F-NTF-003):登录后建立连接;推送到达 → 刷新任务列表 + 横幅提示
  useEffect(() => {
    const workerId = userInfo?.id != null ? String(userInfo.id) : null;
    if (!workerId) {
      return;
    }
    connectWsPushIfLoggedIn().catch((e) => console.error('WS 连接失败:', e));
    const unsubscribe = wsPushService.onMessage((message: WsPushMessage) => {
      showPushBanner(message);
      loadAllData().catch((e) => console.error('推送刷新任务列表失败:', e));
    });
    return () => {
      unsubscribe();
      // 登出/切换账号时断开(由登出流程显式断开)
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userInfo?.id]);

  const loadAllData = useCallback(async () => {
    const results = await Promise.all([
      getPendingDeliveryOrders(),
      getMyAssignedOrders(),
      getMyDeliveringOrders(),
    ]);
    setPendingOrders([...(results[0]?.pendingDeliveryOrders ?? [])]);
    setAssignedOrders([...(results[1]?.assignedOrders ?? [])]);
    setDeliveringOrders([...(results[2]?.deliveringOrders ?? [])]);
  }, []);

  // 管理员兼送水(F-USR-023):管理员账号同样可在「订单」Tab 执行任务流,
  // 「管理」Tab 仍保留经营看板;不再重定向。
  useFocusEffect(
    useCallback(() => {
      loadAllData().catch((e) => console.error('加载订单列表失败:', e));
    }, [loadAllData]),
  );

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await loadAllData();
    } finally {
      setRefreshing(false);
    }
  }, [loadAllData]);

  const handleAccept = useCallback(
    async (orderId: string) => {
      try {
        await acceptDelivery(orderId);
        Alert.alert('接单成功');
        loadAllData();
      } catch (e: any) {
        Alert.alert(e?.message || '接单失败');
      }
    },
    [loadAllData],
  );

  const handleStartDelivery = useCallback(
    async (orderId: string) => {
      try {
        await startDelivery(orderId);
        Alert.alert('已开始配送');
        loadAllData();
      } catch (e: any) {
        Alert.alert(e?.message || '操作失败');
      }
    },
    [loadAllData],
  );

  const lists = [pendingOrders, assignedOrders, deliveringOrders];
  const empties = ['暂无未接单订单', '暂无已接单订单', '暂无配送中订单'];
  const tabTitles = [
    `未接单 (${pendingOrders.length})`,
    `已接单 (${assignedOrders.length})`,
    `配送中 (${deliveringOrders.length})`,
  ];

  const renderCards = () => {
    const list = lists[currentTab];
    if (list.length === 0) {
      return (
        <EmptyState
          icon={<ThemedText style={styles.emptyIcon}>{TAB_EMPTY_ICONS[currentTab]}</ThemedText>}
          title={empties[currentTab]}
          description="下拉可刷新订单列表"
        />
      );
    }
    if (currentTab === 0) {
      return list.map((order) => (
        <OrderCard key={order.id} order={order} type="pending" onAccept={handleAccept} />
      ));
    }
    if (currentTab === 1) {
      return list.map((order) => (
        <OrderCard key={order.id} order={order} type="assigned" onStartDelivery={handleStartDelivery} />
      ));
    }
    return list.map((order) => (
      <OrderCard key={order.id} order={order} type="delivering" />
    ));
  };

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <SafeAreaView style={styles.safeArea} edges={['top']}>
        <SegmentedTabs
          current={currentTab}
          tabs={tabTitles.map((t) => ({ title: t }))}
          onChange={setCurrentTab}
        />

        <ScrollView
          style={styles.list}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />
          }>
          {renderCards()}
        </ScrollView>
        <FloatingButton onPress={() => router.push('/create-order')} />
      </SafeAreaView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  safeArea: {
    flex: 1,
    gap: Spacing.three,
    padding: Spacing.four,
  },
  list: {
    flex: 1,
  },
  listContent: {
    paddingBottom: Spacing.six,
  },
  emptyIcon: {
    fontSize: 28,
    lineHeight: 32,
  },
});
