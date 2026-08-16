import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  Share,
  StyleSheet,
  View,
} from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { BottomSheet } from '@/components/bottom-sheet';
import { Button } from '@/components/button';
import { Card } from '@/components/card';
import { AddressCard } from '@/components/order-detail/address-card';
import { DeliveryInfoCard } from '@/components/order-detail/delivery-info-card';
import { OperationsCard } from '@/components/order-detail/operations-card';
import { OrderInfoCard } from '@/components/order-detail/order-info-card';
import { ProductCard } from '@/components/order-detail/product-card';
import { DestinationMap } from '@/components/map/destination-map';
import { capturePosterView, PosterView, toPosterOrderData } from '@/components/order-poster';
import { StatusBadge } from '@/components/status-badge';
import { StatusSteps } from '@/components/status-steps';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { ORDER_STATUS_MAP, getCurrentStep } from '@/constants/order';
import { Radius, Spacing } from '@/constants/theme';
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
} from '@/services/delivery';
import { buildOrderDeepLink, shareOrderToWechat } from '@/services/wechat-share';
import { DeliveryWorkerStatus } from '@aquarush/common';
import type { DeliveryWorker, Order } from '@aquarush/common';

const PAYMENT_TYPES = [
  { value: 'WATER_TICKET', label: '水票收款' },
  { value: 'CASH', label: '现金收款' },
  { value: 'QR_CODE', label: '二维码收款' },
];

export default function OrderDetailScreen() {
  const { orderNo } = useLocalSearchParams<{ orderNo: string }>();
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const { isAdmin } = useAuth();

  const [order, setOrder] = useState<Order | null>(null);
  const [operations, setOperations] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [paymentSheetVisible, setPaymentSheetVisible] = useState(false);

  const [deliveryPhotos, setDeliveryPhotos] = useState<string[]>([]);
  const [deliveryRemark, setDeliveryRemark] = useState('');

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

        <AddressCard address={order.address ?? undefined} />

        {order.address?.longitude != null && order.address?.latitude != null ? (
          <DestinationMap
            longitude={order.address.longitude}
            latitude={order.address.latitude}
            height={200}
          />
        ) : null}

        <ProductCard items={order.items ?? undefined} remark={order.remark ?? undefined} />

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

      <View
        style={[
          styles.bottomBar,
          {
            backgroundColor: theme.card,
            borderTopColor: theme.border,
            paddingBottom: Spacing.three + insets.bottom,
          },
        ]}>
        {isAdmin ? (
          <>
            {order.status === 'PENDING_DISPATCH' || order.status === 'PENDING_DELIVERY' ? (
              <Button
                title={order.deliveryWorker?.name ? `重新分配送水员（${order.deliveryWorker.name}）` : '分配送水员'}
                size="lg"
                loading={assignLoading || assigning}
                onPress={handleOpenAssignSheet}
                style={styles.actionButton}
              />
            ) : null}
            {order.deliveryWorker?.name &&
            order.status !== 'PENDING_DISPATCH' &&
            order.status !== 'PENDING_DELIVERY' ? (
              <ThemedText type="small" themeColor="textSecondary" style={styles.assigneeNote}>
                当前配送员：{order.deliveryWorker.name}
              </ThemedText>
            ) : null}
          </>
        ) : null}
        {order.status === 'PENDING_DISPATCH' ? (
          <Button
            title="接单"
            size="lg"
            loading={actionLoading}
            trackTarget="order-accept"
            trackElementType="button"
            trackElementText="接单"
            trackProps={{ orderNo: order.orderNo }}
            onPress={handleAccept}
            style={styles.actionButton}
          />
        ) : null}
        {order.status === 'PENDING_DELIVERY' ? (
          <Button
            title="开始配送"
            size="lg"
            loading={actionLoading}
            trackTarget="order-start"
            trackElementType="button"
            trackElementText="开始配送"
            trackProps={{ orderNo: order.orderNo }}
            onPress={handleStart}
            style={styles.actionButton}
          />
        ) : null}
        {order.status === 'DELIVERING' ? (
          <Button
            title={deliveryPhotos.length > 0 ? '确认送达' : '请先填写配送信息'}
            size="lg"
            loading={actionLoading}
            disabled={deliveryPhotos.length === 0}
            onPress={() =>
              order.isSelfCollect ? handlePaymentCollection(null) : setPaymentSheetVisible(true)
            }
            style={styles.actionButton}
          />
        ) : null}
        {isCompleted ? (
          <Button
            title="分享海报"
            size="lg"
            variant="ghost"
            onPress={() => setPosterVisible(true)}
            style={styles.actionButton}
          />
        ) : null}
      </View>

      <Modal
        visible={posterVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setPosterVisible(false)}>
        <View style={[styles.posterModalBackdrop, { backgroundColor: theme.overlayStrong }]}>
          <Pressable style={StyleSheet.absoluteFill} onPress={() => setPosterVisible(false)} />
          <View style={styles.posterModalBody}>
            <ThemedText type="heading" themeColor="headerText" style={styles.posterModalTitle}>
              分享订单海报
            </ThemedText>
            <View style={styles.posterCard} ref={posterViewRef}>
              <PosterView order={toPosterOrderData(order)} />
            </View>
            <View style={styles.posterActions}>
              {wechatConfigured ? (
                <Button
                  title="分享到微信"
                  size="md"
                  loading={sharing}
                  onPress={handleShareToWechat}
                  style={styles.posterButton}
                />
              ) : null}
              {Platform.OS !== 'web' ? (
                <Button
                  title="系统分享"
                  size="md"
                  variant="ghost"
                  loading={sharing}
                  onPress={handleSystemShare}
                  style={styles.posterButton}
                />
              ) : null}
            </View>
            <Pressable onPress={() => setPosterVisible(false)} style={styles.posterClose}>
              <ThemedText type="small" themeColor="headerTextSecondary">
                关闭
              </ThemedText>
            </Pressable>
          </View>
        </View>
      </Modal>

      <BottomSheet visible={paymentSheetVisible} onClose={() => setPaymentSheetVisible(false)} title="选择收款方式">
        {PAYMENT_TYPES.map(({ value, label }) => (
          <Pressable
            key={value}
            style={({ pressed }) => [
              styles.sheetItem,
              pressed && { backgroundColor: theme.backgroundSelected },
            ]}
            onPress={() => handlePaymentCollection(value)}>
            <ThemedText type="smallBold">{label}</ThemedText>
          </Pressable>
        ))}
        <View style={[styles.sheetDivider, { borderTopColor: theme.border }]} />
        <Pressable
          style={({ pressed }) => [styles.sheetItem, pressed && { backgroundColor: theme.backgroundSelected }]}
          onPress={() => setPaymentSheetVisible(false)}>
          <ThemedText type="smallBold" themeColor="textSecondary">
            取消
          </ThemedText>
        </Pressable>
      </BottomSheet>

      <BottomSheet visible={assignSheetVisible} onClose={() => setAssignSheetVisible(false)} title="分配送水员">
        <Pressable
          style={({ pressed }) => [styles.sheetItem, pressed && { backgroundColor: theme.backgroundSelected }]}
          onPress={() => setAssignIsSelfCollect((v) => !v)}>
          <ThemedText type="smallBold" themeColor={assignIsSelfCollect ? 'primary' : 'textSecondary'}>
            {assignIsSelfCollect ? '☑ 已收款（自收）' : '☐ 到付（货到收款）'}
          </ThemedText>
        </Pressable>
        <View style={[styles.sheetDivider, { borderTopColor: theme.border }]} />
        {assignWorkers.map((worker) => {
          const isCurrent = order?.deliveryWorker?.id === worker.id;
          return (
            <Pressable
              key={worker.id}
              style={({ pressed }) => [styles.workerRow, pressed && { backgroundColor: theme.backgroundSelected }]}
              onPress={() => handleAssign(worker.id)}
              disabled={assigning}>
              <View style={styles.workerInfo}>
                <ThemedText type="smallBold">{worker.name}</ThemedText>
                <ThemedText type="caption" themeColor="textTertiary">
                  {worker.phone}
                </ThemedText>
              </View>
              <View style={styles.workerBadges}>
                {worker.onlineStatus === DeliveryWorkerStatus.ONLINE ? (
                  <StatusBadge label="在线" tone="success" />
                ) : (
                  <StatusBadge label="离线" tone="processing" />
                )}
                {isCurrent ? <StatusBadge label="当前" tone="accent" /> : null}
              </View>
            </Pressable>
          );
        })}
      </BottomSheet>
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
  bottomBar: {
    flexDirection: 'row',
    padding: Spacing.three,
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  actionButton: {
    flex: 1,
  },
  sheetItem: {
    paddingVertical: Spacing.three,
    alignItems: 'center',
    borderRadius: Radius.sm,
  },
  posterModalBackdrop: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: Spacing.four,
  },
  posterModalBody: {
    width: '100%',
    maxWidth: 420,
    alignItems: 'center',
  },
  posterModalTitle: {
    marginBottom: Spacing.three,
  },
  posterCard: {
    borderRadius: Radius.lg,
    backgroundColor: '#ffffff',
    padding: Spacing.three,
    marginBottom: Spacing.four,
  },
  posterActions: {
    flexDirection: 'row',
    gap: Spacing.two,
    width: '100%',
  },
  posterButton: {
    flex: 1,
  },
  posterClose: {
    marginTop: Spacing.four,
    padding: Spacing.two,
  },
  sheetDivider: {
    borderTopWidth: StyleSheet.hairlineWidth,
    marginTop: Spacing.two,
  },
  assigneeNote: {
    flex: 1,
    textAlign: 'center',
  },
  workerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: Spacing.three,
    borderRadius: Radius.sm,
    gap: Spacing.two,
  },
  workerInfo: {
    flex: 1,
    gap: Spacing.half,
  },
  workerBadges: {
    flexDirection: 'row',
    gap: Spacing.two,
    alignItems: 'center',
  },
});