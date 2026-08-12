import { useCallback } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { useRouter } from 'expo-router';
import { Money } from '@aquarush/common';

import { Button } from '@/components/button';
import { Card } from '@/components/card';
import { OrderInfoRow } from '@/components/order-info-row';
import { StatusBadge } from '@/components/status-badge';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';

export interface OrderCardData {
  id: string;
  orderNo: string;
  quantity: number;
  amountCents?: number | string;
  status?: string;
  isSelfCollect?: boolean;
  createdAt?: string | null;
  deliveryConfirmedAt?: string | null;
  address?: {
    receiverName?: string | null;
    phone?: string | null;
    detailAddress?: string | null;
  } | null;
  user?: {
    nickname?: string | null;
    phone?: string | null;
  } | null;
  product?: {
    name?: string | null;
  } | null;
  /** 已分配送水员（管理端列表展示） */
  deliveryWorker?: {
    id?: string | null;
    name?: string | null;
  } | null;
}

type OrderCardType = 'pending' | 'assigned' | 'delivering' | 'history';

interface OrderCardProps {
  order: OrderCardData;
  type?: OrderCardType;
  onAccept?: (orderId: string) => void;
  onStartDelivery?: (orderId: string) => void;
}

function formatDateTime(dateStr?: string | null) {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function OrderCard({ order, type = 'history', onAccept, onStartDelivery }: OrderCardProps) {
  const router = useRouter();

  const handleClick = useCallback(() => {
    router.push(`/order/${order.orderNo}`);
  }, [router, order.orderNo]);

  return (
    <Pressable onPress={handleClick} style={({ pressed }) => pressed && styles.pressed}>
      <Card>
        <View style={styles.header}>
          <ThemedText type="smallBold">{order.orderNo}</ThemedText>
          <View style={styles.headerRight}>
            {type === 'history' && order.status ? <StatusBadge status={order.status} /> : null}
            {order.isSelfCollect ? <StatusBadge label="自收" tone="accent" /> : null}
          </View>
        </View>

        <OrderInfoRow label="客户" value={order.address?.receiverName || order.user?.nickname || '未知'} />
        <OrderInfoRow label="电话" value={order.address?.phone || order.user?.phone || '未知'} />
        <OrderInfoRow label="地址" value={order.address?.detailAddress || '未知'} />
        <OrderInfoRow label="商品" value={`${order.product?.name || '未知'} x ${order.quantity}`} />
        <OrderInfoRow label="下单时间" value={formatDateTime(order.createdAt)} />
        {order.deliveryWorker?.name ? (
          <OrderInfoRow label="配送员" value={order.deliveryWorker.name} />
        ) : null}
        {type === 'history' && order.deliveryConfirmedAt ? (
          <OrderInfoRow label="完成时间" value={formatDateTime(order.deliveryConfirmedAt)} />
        ) : null}

        <OrderInfoRow
          label="金额"
          value={
            <ThemedText type="smallBold" themeColor="primary">
              <Money cents={order.amountCents} />
            </ThemedText>
          }
        />

        {type !== 'history' ? (
          <View style={styles.actions}>
            {type === 'pending' && onAccept ? (
              <Button title="接单" size="sm" onPress={() => onAccept(order.id)} />
            ) : null}
            {type === 'assigned' && onStartDelivery ? (
              <Button title="开始配送" size="sm" onPress={() => onStartDelivery(order.id)} />
            ) : null}
            {type === 'delivering' ? (
              <Button title="去确认" size="sm" variant="ghost" onPress={handleClick} />
            ) : null}
          </View>
        ) : null}
      </Card>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  pressed: {
    opacity: 0.85,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.two,
  },
  headerRight: {
    flexDirection: 'row',
    gap: Spacing.two,
    alignItems: 'center',
  },
  actions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    marginTop: Spacing.two,
  },
});
