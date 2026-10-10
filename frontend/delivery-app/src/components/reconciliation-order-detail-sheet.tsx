import { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, View } from 'react-native';
import { Money } from '@aquarush/common';
import type { ReconciliationOrderDetailVo } from '@aquarush/common';

import { BottomSheet } from '@/components/bottom-sheet';
import { OrderInfoRow } from '@/components/order-info-row';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { getDailyReconciliationOrderDetails } from '@/services/dailyCollection';

const PAGE_SIZE = 10;

const PAYMENT_TYPE_TEXT: Record<string, string> = {
  CASH: '现金',
  QR_CODE: '扫码',
  WATER_TICKET: '水票',
};

interface ReconciliationOrderDetailSheetProps {
  visible: boolean;
  reconciliationDate: string | null;
  onClose: () => void;
}

/**
 * 对账逐单明细(G29):按对账日期分页取回当日已送达订单流水,
 * 与日报系统计算值同源,差异可定位到具体订单。
 */
export function ReconciliationOrderDetailSheet({
  visible,
  reconciliationDate,
  onClose,
}: ReconciliationOrderDetailSheetProps) {
  const [orders, setOrders] = useState<ReconciliationOrderDetailVo[]>([]);
  const [total, setTotal] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const loadingRef = useRef(false);

  const loadPage = useCallback(
    async (nextPage: number, append: boolean) => {
      if (!reconciliationDate || loadingRef.current) return;
      loadingRef.current = true;
      if (append) {
        setLoadingMore(true);
      } else {
        setLoading(true);
      }
      try {
        const res = await getDailyReconciliationOrderDetails(reconciliationDate, nextPage, PAGE_SIZE);
        const pageData = res?.dailyReconciliationOrderDetails;
        const list = pageData?.list ?? [];
        setOrders((prev) => (append ? [...prev, ...list] : [...list]));
        setTotal(pageData?.pageInfo.total ?? 0);
        setHasMore(pageData?.pageInfo.hasNext ?? false);
      } catch (e) {
        console.error('加载对账逐单明细失败:', e);
      } finally {
        loadingRef.current = false;
        if (append) {
          setLoadingMore(false);
        } else {
          setLoading(false);
        }
      }
    },
    [reconciliationDate],
  );

  // 每次打开重置并加载第一页
  useEffect(() => {
    if (visible) {
      setOrders([]);
      setTotal(0);
      loadPage(0, false).catch(() => {});
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible, reconciliationDate]);

  const handleLoadMore = useCallback(() => {
    loadPage(Math.ceil(orders.length / PAGE_SIZE), true).catch(() => {});
  }, [loadPage, orders.length]);

  return (
    <BottomSheet visible={visible} onClose={onClose} title={`对账逐单明细 · ${reconciliationDate ?? ''}`}>
      <ThemedText type="caption" themeColor="textSecondary" style={styles.hint}>
        当日已送达订单流水(含待确认收货/已完成),与日报系统计算值同源,共 {total} 单
      </ThemedText>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator />
        </View>
      ) : orders.length === 0 ? (
        <ThemedText type="small" themeColor="textTertiary" style={styles.center}>
          当日无已送达订单
        </ThemedText>
      ) : (
        <View style={styles.list}>
          {orders.map((order) => (
            <View key={order.orderId} style={styles.row}>
              <ThemedText type="smallBold">{order.orderNo}</ThemedText>
              <View style={styles.rowMeta}>
                <OrderInfoRow label="金额" value={<Money cents={order.amountCents} />} />
                <OrderInfoRow
                  label="收款"
                  value={order.paymentType ? PAYMENT_TYPE_TEXT[order.paymentType] ?? order.paymentType : '-'}
                />
                <OrderInfoRow
                  label="送达"
                  value={
                    order.deliveredAt ? new Date(order.deliveredAt).toLocaleString('zh-CN') : '-'
                  }
                />
              </View>
            </View>
          ))}
          {hasMore && (
            <Pressable style={styles.loadMore} onPress={handleLoadMore} disabled={loadingMore}>
              {loadingMore ? (
                <ActivityIndicator size="small" />
              ) : (
                <ThemedText type="small" themeColor="primary">
                  加载更多
                </ThemedText>
              )}
            </Pressable>
          )}
        </View>
      )}
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  hint: {
    marginBottom: Spacing.three,
  },
  center: {
    paddingVertical: Spacing.five,
    alignItems: 'center',
  },
  list: {
    gap: Spacing.two,
  },
  row: {
    paddingVertical: Spacing.two,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#e5e5e5',
  },
  rowMeta: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.three,
    marginTop: Spacing.one,
  },
  loadMore: {
    alignItems: 'center',
    paddingVertical: Spacing.three,
  },
});
