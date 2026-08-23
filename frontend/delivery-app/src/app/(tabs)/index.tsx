import { useCallback, useEffect, useRef, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect, useRouter } from 'expo-router';
import { OrderStatus, ORDER_STATUS_TEXT, usePagedList } from '@aquarush/common';
import type { Order } from '@aquarush/common';
import { toast } from 'sonner-native';

import { EmptyState } from '@/components/empty-state';
import { FloatingButton } from '@/components/floating-button';
import { OrderCard } from '@/components/order-card';
import { SegmentedTabs } from '@/components/segmented-tabs';
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
  const { userInfo, isAdmin, isDeliveryWorker } = useAuth();

  // 三个任务列表(待接单/待配送/配送中)各自独立分页状态机 —— common usePagedList
  const pendingList = usePagedList<Order>({
    fetchPage: async () => {
      const res = await getPendingDeliveryOrders();
      return { items: [...(res?.pendingDeliveryOrders ?? [])], hasMore: false };
    },
  });
  const assignedList = usePagedList<Order>({
    fetchPage: async () => {
      const res = await getMyAssignedOrders();
      return { items: [...(res?.assignedOrders ?? [])], hasMore: false };
    },
  });
  const deliveringList = usePagedList<Order>({
    fetchPage: async () => {
      const res = await getMyDeliveringOrders();
      return { items: [...(res?.deliveringOrders ?? [])], hasMore: false };
    },
  });

  const loadAllData = useCallback(() => {
    return Promise.all([
      pendingList.refresh(),
      assignedList.refresh(),
      deliveringList.refresh(),
    ]);
  }, [pendingList.refresh, assignedList.refresh, deliveringList.refresh]);

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

  // 管理员兼送水(F-USR-023):管理员账号同样可在「订单」Tab 执行任务流,
  // 「管理」Tab 仍保留经营看板;不再重定向。
  // 首屏由 usePagedList 自动加载;再次聚焦时刷新。
  const firstFocus = useRef(true);
  useFocusEffect(
    useCallback(() => {
      if (firstFocus.current) {
        firstFocus.current = false;
        return;
      }
      loadAllData().catch((e) => console.error('加载订单列表失败:', e));
    }, [loadAllData]),
  );

  const handleRefresh = useCallback(() => {
    return loadAllData();
  }, [loadAllData]);

  const handleAccept = useCallback(
    async (orderId: string) => {
      const toastId = toast.loading('接单中...');
      try {
        await acceptDelivery(orderId);
        toast.success('接单成功', { id: toastId });
        loadAllData();
      } catch (e: any) {
        toast.error(e?.message || '接单失败', { id: toastId });
      }
    },
    [loadAllData],
  );

  const handleStartDelivery = useCallback(
    async (orderId: string) => {
      const toastId = toast.loading('开始配送中...');
      try {
        await startDelivery(orderId);
        toast.success('已开始配送', { id: toastId });
        loadAllData();
      } catch (e: any) {
        toast.error(e?.message || '操作失败', { id: toastId });
      }
    },
    [loadAllData],
  );

  const lists = [pendingList.items, assignedList.items, deliveringList.items];
  const empties = ['暂无未接单订单', '暂无已接单订单', '暂无配送中订单'];
  // 「待接单」为送水工视角本地措辞(common 统一口径为「待派单」);其余标题取 common ORDER_STATUS_TEXT
  const tabTitles = [
    `待接单 (${pendingList.items.length})`,
    `${ORDER_STATUS_TEXT[OrderStatus.PENDING_DELIVERY]} (${assignedList.items.length})`,
    `${ORDER_STATUS_TEXT[OrderStatus.DELIVERING]} (${deliveringList.items.length})`,
  ];

  const renderCards = () => {
    const list = lists[currentTab];
    if (list.length === 0) {
      return (
        <EmptyState
          icon={TAB_EMPTY_ICONS[currentTab]}
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
            <RefreshControl
              refreshing={
                pendingList.refreshing || assignedList.refreshing || deliveringList.refreshing
              }
              onRefresh={handleRefresh}
            />
          }>
          {renderCards()}
        </ScrollView>
        {/* 代客下单(F-ORD-012)：送水工/管理员/超管均可见 —— 送水工 ORDER_CREATE、
            管理员兼送水 F-USR-023 亦覆盖(见 PRD 01-overview §角色权限矩阵) */}
        {isAdmin || isDeliveryWorker ? <FloatingButton onPress={() => router.push('/create-order')} /> : null}
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
});
