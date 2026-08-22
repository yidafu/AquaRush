import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Platform,
  ScrollView,
  Share,
  StyleSheet,
  View,
} from 'react-native';
import { useLocalSearchParams } from 'expo-router';

import { Card } from '@/components/card';
import { AddressCard } from '@/components/order-detail/address-card';
import { AssignWorkerSheet } from '@/components/order-detail/assign-worker-sheet';
import { CostSummaryCard } from '@/components/order-detail/cost-summary-card';
import { DeliveryInfoCard } from '@/components/order-detail/delivery-info-card';
import { OrderAddressEditSheet } from '@/components/order-detail/order-address-edit-sheet';
import { OrderActionsBar } from '@/components/order-detail/order-actions-bar';
import { OperationsCard } from '@/components/order-detail/operations-card';
import { OrderInfoCard } from '@/components/order-detail/order-info-card';
import { PaymentCollectionSheet } from '@/components/order-detail/payment-collection-sheet';
import { PosterModal } from '@/components/order-detail/poster-modal';
import { ProductCard } from '@/components/order-detail/product-card';
import { DestinationMap } from '@/components/map/destination-map';
import { capturePosterView, toPosterOrderData } from '@/components/order-poster';
import { StatusSteps } from '@/components/status-steps';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { ORDER_STATUS_MAP, getCurrentStep } from '@/constants/order';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/hooks/use-auth';
import { useTheme } from '@/hooks/use-theme';
import { assignDeliveryWorker, getDeliveryWorkers } from '@/services/admin';
import { userActionTracker } from '@/utils/tracking';
import { trajectoryReporter } from '@/utils/trajectory-reporter';
import {
  acceptDelivery,
  completeDelivery,
  getOrderDetail,
  getOrderOperations,
  startDelivery,
  updateOrderAddress,
} from '@/services/delivery';
import { buildOrderDeepLink, shareOrderToWechat } from '@/services/wechat-share';
import type { DeliveryWorker, Order, UpdateOrderAddressInput } from '@aquarush/common';

export default function OrderDetailScreen() {
  const { orderNo } = useLocalSearchParams<{ orderNo: string }>();
  const theme = useTheme();
  const { isAdmin } = useAuth();

  const [order, setOrder] = useState<Order | null>(null);
  const [operations, setOperations] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [paymentSheetVisible, setPaymentSheetVisible] = useState(false);

  const [deliveryPhotos, setDeliveryPhotos] = useState<string[]>([]);
  const [deliveryRemark, setDeliveryRemark] = useState('');

  // 订单页地址信息修正(F-ORD-024)
  const [editAddressVisible, setEditAddressVisible] = useState(false);
  const [savingAddress, setSavingAddress] = useState(false);

  // 管理员派单
  const [assignSheetVisible, setAssignSheetVisible] = useState(false);
  const [assignWorkers, setAssignWorkers] = useState<DeliveryWorker[]>([]);
  const [assignIsSelfCollect, setAssignIsSelfCollect] = useState(false);
  const [assignLoading, setAssignLoading] = useState(false);
  const [assigning, setAssigning] = useState(false);

  // 分享海报
  const [posterVisible, setPosterVisible] = useState(false);
  const [posterUri, setPosterUri] = useState<string | null>(null);
  const [sharing, setSharing] = useState(false);
  const posterViewRef = useRef<View>(null);
  // web 端按钮保持可见，点击提示「不支持」（wechat-sdk.web.ts 桩实现）
  const wechatConfigured = Platform.OS === 'web' || !!process.env.EXPO_PUBLIC_WECHAT_MOBILE_APP_ID;

  const loadOrderDetail = useCallback(async () => {
    if (!orderNo) {
      Alert.alert('订单不存在');
      return;
    }
    setLoading(true);
    try {
      const res = await getOrderDetail(orderNo);
      if (res?.orderByNo) {
        setOrder(res.orderByNo);
        if (res.orderByNo.id) {
          const opsRes = await getOrderOperations(res.orderByNo.id);
          if (opsRes?.orderOperations) {
            setOperations(opsRes.orderOperations);
          }
        }
      }
    } catch (e) {
      console.error('获取订单详情失败:', e);
      Alert.alert('获取订单详情失败');
    } finally {
      setLoading(false);
    }
  }, [orderNo]);

  useEffect(() => {
    loadOrderDetail();
  }, [loadOrderDetail]);

  // 轨迹上报(F-ADR-012,R-ADR-03):订单进入「配送中」自动启动持续定位上报,
  // 「完成配送/取消/退款」停止;页面卸载不停止(配送中会话级持续,App 重启后由根布局恢复)。
  // 注:react-native-fitness-geolocation 已移除,trajectoryReporter 为空实现,
  // 以下调用保留作集成点,当前不产生任何定位行为。
  useEffect(() => {
    if (!order?.id) return;
    if (order.status === 'DELIVERING') {
      void trajectoryReporter.startForOrder(order.id);
    } else if (
      order.status === 'COMPLETED' ||
      order.status === 'CANCELLED' ||
      order.status === 'REFUNDED'
    ) {
      void trajectoryReporter.stopForOrder(order.id);
    }
  }, [order?.id, order?.status]);

  const handleAccept = useCallback(async () => {
    if (!order) return;
    setActionLoading(true);
    try {
      await acceptDelivery(order.id);
      Alert.alert('接单成功');
      loadOrderDetail();
    } catch (e: any) {
      Alert.alert(e?.message || '接单失败');
    } finally {
      setActionLoading(false);
    }
  }, [order, loadOrderDetail]);

  const handleStart = useCallback(async () => {
    if (!order) return;
    setActionLoading(true);
    try {
      await startDelivery(order.id);
      Alert.alert('开始配送成功');
      loadOrderDetail();
    } catch (e: any) {
      Alert.alert(e?.message || '开始配送失败');
    } finally {
      setActionLoading(false);
    }
  }, [order, loadOrderDetail]);

  const handlePaymentCollection = useCallback(
    async (paymentType: string | null) => {
      if (!order) return;
      setPaymentSheetVisible(false);

      if (deliveryPhotos.length === 0) {
        Alert.alert('请先拍摄配送照片');
        return;
      }

      // 埋点:确认送达
      userActionTracker.track('CLICK', 'order-complete', {
        elementType: 'button',
        elementText: '确认送达',
        properties: { orderNo: order.orderNo, paymentType: paymentType ?? undefined },
      });
      setActionLoading(true);
      try {
        await completeDelivery(order.id, deliveryPhotos, paymentType ?? undefined, deliveryRemark || undefined);
        Alert.alert('配送完成');
        setDeliveryPhotos([]);
        setDeliveryRemark('');
        loadOrderDetail();
      } catch (e: any) {
        Alert.alert(e?.message || '完成配送失败');
      } finally {
        setActionLoading(false);
      }
    },
    [order, deliveryPhotos, deliveryRemark, loadOrderDetail],
  );

  // 确认送达按钮:自收直接完成,否则打开收款方式选择
  const handleCompletePress = useCallback(() => {
    if (!order) return;
    if (order.isSelfCollect) {
      handlePaymentCollection(null);
    } else {
      setPaymentSheetVisible(true);
    }
  }, [order, handlePaymentCollection]);

  const ensurePoster = useCallback(async (): Promise<string | null> => {
    if (posterUri) return posterUri;
    const uri = await capturePosterView(posterViewRef);
    if (uri) setPosterUri(uri);
    return uri;
  }, [posterUri]);

  const handleShareToWechat = useCallback(async () => {
    if (!order) return;
    if (Platform.OS === 'web') {
      // web 端微信 SDK 不可用，直接提示不支持
      Alert.alert('微信分享不支持', '微信分享仅支持手机 App，请使用系统分享');
      return;
    }
    setSharing(true);
    try {
      const uri = await ensurePoster();
      const shared = await shareOrderToWechat(toPosterOrderData(order), uri);
      if (!shared) {
        // 未安装/未配置：降级为系统分享
        if (uri) {
          await Share.share({ url: uri, message: buildOrderDeepLink(order.orderNo) });
        } else {
          Alert.alert('分享失败');
        }
      }
    } catch (e) {
      console.error('分享失败:', e);
      Alert.alert('分享失败');
    } finally {
      setSharing(false);
    }
  }, [order, ensurePoster]);

  const handleSystemShare = useCallback(async () => {
    if (!order) return;
    setSharing(true);
    try {
      const uri = await ensurePoster();
      await Share.share({
        url: uri ?? undefined,
        message: uri ? buildOrderDeepLink(order.orderNo) : `${order.orderNo} ${buildOrderDeepLink(order.orderNo)}`,
      });
    } catch (e) {
      console.error('分享失败:', e);
      Alert.alert('分享失败');
    } finally {
      setSharing(false);
    }
  }, [order, ensurePoster]);

  const handleOpenAssignSheet = useCallback(async () => {
    if (!order) return;
    setAssignLoading(true);
    try {
      const res = await getDeliveryWorkers();
      setAssignWorkers([...(res?.deliveryWorkers ?? [])]);
      setAssignIsSelfCollect(Boolean(order.isSelfCollect));
      setAssignSheetVisible(true);
    } catch (e: any) {
      Alert.alert(e?.message || '加载送水员失败');
    } finally {
      setAssignLoading(false);
    }
  }, [order]);

  const handleAssign = useCallback(
    async (workerId: string) => {
      if (!order) return;
      setAssigning(true);
      try {
        await assignDeliveryWorker(order.id, workerId, assignIsSelfCollect);
        setAssignSheetVisible(false);
        Alert.alert('分配成功');
        loadOrderDetail();
      } catch (e: any) {
        Alert.alert(e?.message || '分配送水员失败');
      } finally {
        setAssigning(false);
      }
    },
    [order, assignIsSelfCollect, loadOrderDetail],
  );

  const handleSaveAddress = useCallback(
    async (input: UpdateOrderAddressInput) => {
      if (!order) return;
      setSavingAddress(true);
      try {
        await updateOrderAddress(order.id, input);
        setEditAddressVisible(false);
        Alert.alert('修改成功', '收货地址信息已更新');
        loadOrderDetail();
      } catch (e: any) {
        Alert.alert(e?.message || '修改地址信息失败');
      } finally {
        setSavingAddress(false);
      }
    },
    [order, loadOrderDetail],
  );

  if (loading) {
    return (
      <ThemedView type="pageBackground" style={styles.center}>
        <ActivityIndicator size="large" color={theme.primary} />
        <ThemedText themeColor="textSecondary">加载中...</ThemedText>
      </ThemedView>
    );
  }

  if (!order) {
    return (
      <ThemedView type="pageBackground" style={styles.center}>
        <ThemedText themeColor="textSecondary">订单不存在</ThemedText>
      </ThemedView>
    );
  }

  const cancelledOrRefunded = order.status === 'CANCELLED' || order.status === 'REFUNDED';
  const isCompleted = order.status === 'COMPLETED';

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        {!cancelledOrRefunded ? (
          <StatusSteps current={getCurrentStep(order.status)} />
        ) : (
          <Card>
            <ThemedText type="smallBold" themeColor="danger">
              {ORDER_STATUS_MAP[order.status] ?? order.status}
            </ThemedText>
          </Card>
        )}

        <AddressCard
          address={order.address ?? undefined}
          onEdit={cancelledOrRefunded || isCompleted ? undefined : () => setEditAddressVisible(true)}
        />

        {order.address?.longitude != null && order.address?.latitude != null ? (
          <DestinationMap
            longitude={order.address.longitude}
            latitude={order.address.latitude}
            height={200}
          />
        ) : null}

        <ProductCard items={order.items ?? undefined} remark={order.remark ?? undefined} />

        {/* 费用明细(营销优惠明细:水票抵扣/满减/首单立减/优惠券/赠品 —— R-MKT-02/07/08/09/10) */}
        <CostSummaryCard order={order} />

        {order.status === 'DELIVERING' || isCompleted ? (
          <DeliveryInfoCard
            photos={isCompleted ? [...(order.deliveryPhotos ?? [])] : deliveryPhotos}
            remark={isCompleted ? (order.remark ?? '') : deliveryRemark}
            onPhotosChange={setDeliveryPhotos}
            onRemarkChange={setDeliveryRemark}
            disabled={isCompleted}
          />
        ) : null}

        <OperationsCard operations={operations} />

        <OrderInfoCard order={order} />

        <View style={styles.bottomSpacer} />
      </ScrollView>

      {/* 底部操作栏(管理员派单 + 按状态操作按钮) */}
      <OrderActionsBar
        order={order}
        isAdmin={isAdmin}
        actionLoading={actionLoading}
        assignLoading={assignLoading}
        assigning={assigning}
        deliveryPhotosCount={deliveryPhotos.length}
        onOpenAssign={handleOpenAssignSheet}
        onAccept={handleAccept}
        onStart={handleStart}
        onCompletePress={handleCompletePress}
        onShare={() => setPosterVisible(true)}
      />

      {/* 分享海报 */}
      <PosterModal
        visible={posterVisible}
        onClose={() => setPosterVisible(false)}
        order={order}
        wechatConfigured={wechatConfigured}
        sharing={sharing}
        posterViewRef={posterViewRef}
        onShareToWechat={handleShareToWechat}
        onSystemShare={handleSystemShare}
      />

      {/* 收款方式选择(确认送达) */}
      <PaymentCollectionSheet
        visible={paymentSheetVisible}
        onClose={() => setPaymentSheetVisible(false)}
        onSelect={handlePaymentCollection}
      />

      {/* 管理员派单 */}
      <AssignWorkerSheet
        visible={assignSheetVisible}
        onClose={() => setAssignSheetVisible(false)}
        workers={assignWorkers}
        isSelfCollect={assignIsSelfCollect}
        assigning={assigning}
        currentWorkerId={order.deliveryWorker?.id}
        onToggleSelfCollect={() => setAssignIsSelfCollect((v) => !v)}
        onAssign={handleAssign}
      />

      <OrderAddressEditSheet
        visible={editAddressVisible}
        address={order.address ?? null}
        saving={savingAddress}
        onClose={() => setEditAddressVisible(false)}
        onSave={handleSaveAddress}
      />
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.three,
  },
  content: {
    padding: Spacing.three,
  },
  bottomSpacer: {
    height: Spacing.six,
  },
});
