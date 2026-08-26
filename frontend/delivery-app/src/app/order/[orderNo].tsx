import { useCallback, useEffect, useRef, useState } from 'react';
import {
  Platform,
  ScrollView,
  Share,
  StyleSheet,
  View,
} from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { toast } from 'sonner-native';

import { Card } from '@/components/card';
import { LoadingScreen } from '@/components/loading-screen';
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
import { Spacing } from '@/constants/theme';
import { useOrderDetail } from '@aquarush/common';
import { useAuth } from '@/hooks/use-auth';
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
import type { DeliveryWorker, UpdateOrderAddressInput } from '@aquarush/common';

export default function OrderDetailScreen() {
  const { orderNo } = useLocalSearchParams<{ orderNo: string }>();
  const { isAdmin } = useAuth();

  const [operations, setOperations] = useState<any[]>([]);
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

  // 订单详情/费用明细/状态派生 —— common useOrderDetail 统一编排(单来源)。
  // 操作记录(getOrderOperations)不并入 Order 类型,单独按 order.id 拉取。
  const { order, loading, error, refresh, status } = useOrderDetail(orderNo, {
    fetchOrder: async () => {
      if (!orderNo) return null;
      const res = await getOrderDetail(orderNo);
      return res?.orderByNo ?? null;
    },
  });

  useEffect(() => {
    if (!order?.id) {
      setOperations([]);
      return;
    }
    let cancelled = false;
    getOrderOperations(order.id)
      .then((opsRes) => {
        if (!cancelled) setOperations(opsRes?.orderOperations ?? []);
      })
      .catch((e) => console.error('获取订单操作记录失败:', e));
    return () => {
      cancelled = true;
    };
  }, [order?.id]);

  // 加载失败提示(与原 loadOrderDetail 的 catch 行为一致)
  useEffect(() => {
    if (error) toast.error('获取订单详情失败');
  }, [error]);

  // 轨迹上报(F-ADR-012,R-ADR-03):订单进入「配送中」自动启动后台持续定位上报,
  // 「完成配送/取消/退款」停止;页面卸载不停止(配送中会话级持续,App 重启后由根布局恢复)。
  useEffect(() => {
    if (!order?.id) return;
    if (order.status === 'DELIVERING') {
      void trajectoryReporter.startForOrder(order.id, order.orderNo).then((started) => {
        // R-ADR-03:定位权限被拒 → 上报停止,配送主流程不受影响;提示开启定位
        if (!started) {
          toast.warning('需要开启定位权限才能上报配送位置');
        }
      });
    } else if (
      order.status === 'AWAITING_CONFIRMATION' ||
      order.status === 'COMPLETED' ||
      order.status === 'CANCELLED' ||
      order.status === 'REFUNDED'
    ) {
      void trajectoryReporter.stopForOrder(order.id);
    }
  }, [order?.id, order?.orderNo, order?.status]);

  const handleAccept = useCallback(async () => {
    if (!order) return;
    setActionLoading(true);
    const toastId = toast.loading('接单中...');
    try {
      await acceptDelivery(order.id);
      toast.success('接单成功', { id: toastId });
      refresh();
    } catch (e: any) {
      toast.error(e?.message || '接单失败', { id: toastId });
    } finally {
      setActionLoading(false);
    }
  }, [order, refresh]);

  const handleStart = useCallback(async () => {
    if (!order) return;
    setActionLoading(true);
    const toastId = toast.loading('开始配送中...');
    try {
      await startDelivery(order.id);
      toast.success('开始配送成功', { id: toastId });
      refresh();
    } catch (e: any) {
      toast.error(e?.message || '开始配送失败', { id: toastId });
    } finally {
      setActionLoading(false);
    }
  }, [order, refresh]);

  const handlePaymentCollection = useCallback(
    async (paymentType: string | null) => {
      if (!order) return;
      setPaymentSheetVisible(false);

      if (deliveryPhotos.length === 0) {
        toast.warning('请先拍摄配送照片');
        return;
      }

      // 埋点:确认送达
      userActionTracker.track('CLICK', 'order-complete', {
        elementType: 'button',
        elementText: '确认送达',
        properties: { orderNo: order.orderNo, paymentType: paymentType ?? undefined },
      });
      setActionLoading(true);
      const toastId = toast.loading('配送完成处理中...');
      try {
        await completeDelivery(order.id, deliveryPhotos, paymentType ?? undefined, deliveryRemark || undefined);
        toast.success('配送完成', { id: toastId });
        setDeliveryPhotos([]);
        setDeliveryRemark('');
        refresh();
      } catch (e: any) {
        toast.error(e?.message || '完成配送失败', { id: toastId });
      } finally {
        setActionLoading(false);
      }
    },
    [order, deliveryPhotos, deliveryRemark, refresh],
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
      toast.warning('微信分享不支持', { description: '微信分享仅支持手机 App，请使用系统分享' });
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
          toast.error('分享失败');
        }
      }
    } catch (e) {
      console.error('分享失败:', e);
      toast.error('分享失败');
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
      toast.error('分享失败');
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
      toast.error(e?.message || '加载送水员失败');
    } finally {
      setAssignLoading(false);
    }
  }, [order]);

  const handleAssign = useCallback(
    async (workerId: string) => {
      if (!order) return;
      setAssigning(true);
      const toastId = toast.loading('分配中...');
      try {
        await assignDeliveryWorker(order.id, workerId, assignIsSelfCollect);
        setAssignSheetVisible(false);
        toast.success('分配成功', { id: toastId });
        refresh();
      } catch (e: any) {
        toast.error(e?.message || '分配送水员失败', { id: toastId });
      } finally {
        setAssigning(false);
      }
    },
    [order, assignIsSelfCollect, refresh],
  );

  const handleSaveAddress = useCallback(
    async (input: UpdateOrderAddressInput) => {
      if (!order) return;
      setSavingAddress(true);
      const toastId = toast.loading('保存中...');
      try {
        await updateOrderAddress(order.id, input);
        setEditAddressVisible(false);
        toast.success('修改成功', { id: toastId, description: '收货地址信息已更新' });
        refresh();
      } catch (e: any) {
        toast.error(e?.message || '修改地址信息失败', { id: toastId });
      } finally {
        setSavingAddress(false);
      }
    },
    [order, refresh],
  );

  if (loading) {
    return <LoadingScreen label="加载中..." />;
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
  // 已送达(配送完成):待确认收货(用户待签收,R-ORD-11)与已完成均为送水工视角终态
  const isDelivered = order.status === 'AWAITING_CONFIRMATION' || isCompleted;

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        {!cancelledOrRefunded ? (
          <StatusSteps current={status.step} />
        ) : (
          <Card>
            <ThemedText type="smallBold" themeColor="danger">
              {status.text ?? order.status}
            </ThemedText>
          </Card>
        )}

        <AddressCard
          address={order.address ?? undefined}
          onEdit={cancelledOrRefunded || isDelivered ? undefined : () => setEditAddressVisible(true)}
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

        {order.status === 'DELIVERING' || isDelivered ? (
          <DeliveryInfoCard
            photos={isDelivered ? [...(order.deliveryPhotos ?? [])] : deliveryPhotos}
            remark={isDelivered ? (order.deliveryRemark ?? '') : deliveryRemark}
            onPhotosChange={setDeliveryPhotos}
            onRemarkChange={setDeliveryRemark}
            disabled={isDelivered}
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
