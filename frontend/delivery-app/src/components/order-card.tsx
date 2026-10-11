import { useCallback } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { useRouter } from 'expo-router';
import { Money, formatDateTime } from '@aquarush/common';

import { Button } from '@/components/button';
import { Card } from '@/components/card';
import { StatusBadge } from '@/components/status-badge';
import { ThemedText } from '@/components/themed-text';
import { Fonts, Spacing } from '@/constants/theme';

export interface OrderCardData {
  id: string;
  orderNo: string;
  amountCents?: number | string;
  status?: string;
  isSelfCollect?: boolean;
  createdAt?: string | null;
  completedAt?: string | null;
  address?: {
    receiverName?: string | null;
    phone?: string | null;
    detailAddress?: string | null;
  } | null;
  user?: {
    nickname?: string | null;
    phone?: string | null;
  } | null;
  /** 商品明细(F-ORD-004 多商品下单,首件用于卡片展示) */
  items?: ReadonlyArray<{
    productId?: string | number | null;
    quantity?: number;
    product?: {
      name?: string | null;
      coverImageUrl?: string | null;
    } | null;
  }> | null;
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

/**
 * 订单卡密度化布局(对照原型 tasks.html 任务卡):
 * 头部 = 等宽字体订单号 + StatusBadge;
 * 主体 = 收货地址块(详细地址加粗醒目 + 联系人次行)+ 商品摘要一行 + meta 行(下单时间 · 金额);
 * 底部 = 按状态的右侧主操作钮(沿用既有 action 逻辑)。
 * 被 任务/历史/管理 三处列表复用,props 保持兼容。
 */
export function OrderCard({ order, type = 'history', onAccept, onStartDelivery }: OrderCardProps) {
  const router = useRouter();

  const handleClick = useCallback(() => {
    router.push(`/order/${order.orderNo}`);
  }, [router, order.orderNo]);

  const contactName = order.address?.receiverName || order.user?.nickname || '未知';
  // 号码可见性由服务端控制(F-ORD-032,前端不自行脱敏)
  const contactPhone = order.address?.phone || order.user?.phone || '';
  const detailAddress = order.address?.detailAddress || '未知';

  const itemsSummary =
    order.items && order.items.length > 0
      ? (order.items[0]?.product?.name || '未知') +
        (order.items[0]?.quantity ? ` ×${order.items[0].quantity}` : '') +
        (order.items.length > 1 ? ` 等${order.items.length}件` : '')
      : '未知';

  const metaParts: string[] = [];
  if (order.createdAt) metaParts.push(`下单 ${formatDateTime(order.createdAt)}`);
  if (type === 'history' && order.completedAt) {
    metaParts.push(`完成 ${formatDateTime(order.completedAt)}`);
  }
  if (order.deliveryWorker?.name) metaParts.push(`配送员 ${order.deliveryWorker.name}`);

  return (
    <Pressable onPress={handleClick} style={({ pressed }) => pressed && styles.pressed}>
      <Card>
        {/* 头部:等宽订单号 + 状态标签 */}
        <View style={styles.header}>
          <ThemedText type="code" themeColor="textSecondary" style={styles.orderNo}>
            No. {order.orderNo}
          </ThemedText>
          <View style={styles.headerRight}>
            {order.status ? <StatusBadge status={order.status} /> : null}
            {order.isSelfCollect ? <StatusBadge label="自收" tone="accent" /> : null}
          </View>
        </View>

        {/* 收货地址块:详细地址醒目,联系人/电话次行(脱敏逻辑由服务端下发) */}
        <View style={styles.addressBlock}>
          <ThemedText type="default" style={styles.address} numberOfLines={2}>
            {'📍 '}
            {detailAddress}
          </ThemedText>
          <ThemedText type="small" themeColor="textSecondary" numberOfLines={1}>
            {contactName}
            {contactPhone ? ` · ${contactPhone}` : ''}
          </ThemedText>
        </View>

        {/* 商品摘要 + 金额一行 */}
        <View style={styles.summaryRow}>
          <ThemedText type="small" themeColor="textSecondary" style={styles.summary} numberOfLines={1}>
            {itemsSummary}
          </ThemedText>
          <ThemedText type="smallBold" themeColor="primary">
            <Money cents={order.amountCents} />
          </ThemedText>
        </View>

        {/* meta 行:下单时间 · 完成时间 · 配送员 */}
        {metaParts.length > 0 ? (
          <ThemedText type="caption" themeColor="textTertiary" style={styles.meta} numberOfLines={1}>
            {metaParts.join(' · ')}
          </ThemedText>
        ) : null}

        {/* 按状态的右侧主操作钮(行为不变) */}
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
  orderNo: {
    fontFamily: Fonts.mono,
    flexShrink: 1,
  },
  headerRight: {
    flexDirection: 'row',
    gap: Spacing.two,
    alignItems: 'center',
  },
  addressBlock: {
    gap: Spacing.half,
    marginBottom: Spacing.two,
  },
  address: {
    fontWeight: '600',
  },
  summaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: Spacing.three,
  },
  summary: {
    flexShrink: 1,
  },
  meta: {
    marginTop: Spacing.one,
  },
  actions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    marginTop: Spacing.two,
  },
});
