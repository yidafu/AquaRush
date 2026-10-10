import { StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Button } from '@/components/button';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import type { OrderVo } from '@aquarush/common';

interface OrderActionsBarProps {
  order: OrderVo;
  isAdmin: boolean;
  actionLoading: boolean;
  assignLoading: boolean;
  assigning: boolean;
  /** 已拍摄配送照片数(确认送达按钮可用性依据) */
  deliveryPhotosCount: number;
  onOpenAssign: () => void;
  onAccept: () => void;
  onStart: () => void;
  /** 点击确认送达(自收直接完成,否则由页面打开收款方式选择) */
  onCompletePress: () => void;
  onShare: () => void;
}

/**
 * 订单详情底部操作栏:
 * 管理员派单区(待派单/待配送) + 接单/开始配送/确认送达/分享海报按状态渲染按钮。
 */
export function OrderActionsBar({
  order,
  isAdmin,
  actionLoading,
  assignLoading,
  assigning,
  deliveryPhotosCount,
  onOpenAssign,
  onAccept,
  onStart,
  onCompletePress,
  onShare,
}: OrderActionsBarProps) {
  const theme = useTheme();
  const insets = useSafeAreaInsets();

  return (
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
              onPress={onOpenAssign}
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
          onPress={onAccept}
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
          onPress={onStart}
          style={styles.actionButton}
        />
      ) : null}
      {order.status === 'DELIVERING' ? (
        <Button
          title={deliveryPhotosCount > 0 ? '确认送达' : '请先填写配送信息'}
          size="lg"
          loading={actionLoading}
          disabled={deliveryPhotosCount === 0}
          onPress={onCompletePress}
          style={styles.actionButton}
        />
      ) : null}
      {order.status === 'COMPLETED' ? (
        <Button
          title="分享海报"
          size="lg"
          variant="ghost"
          onPress={onShare}
          style={styles.actionButton}
        />
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  bottomBar: {
    flexDirection: 'row',
    padding: Spacing.three,
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  actionButton: {
    flex: 1,
  },
  assigneeNote: {
    flex: 1,
    textAlign: 'center',
  },
});
