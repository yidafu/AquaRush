import { StyleSheet, View } from 'react-native';
import { Money } from '@aquarush/common';
import type { RefundRequestVo, RefundStatus } from '@aquarush/common';
import type { StatusTone } from '@/constants/theme';

import { Button } from '@/components/button';
import { Card } from '@/components/card';
import { OrderInfoRow } from '@/components/order-info-row';
import { StatusBadge } from '@/components/status-badge';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

/** 退款状态 → 文案/tone(对齐 admin-client 退款管理页) */
export const REFUND_STATUS_MAP: Record<RefundStatus, { label: string; tone: StatusTone }> = {
  PENDING: { label: '待审核', tone: 'info' },
  APPROVED: { label: '已通过', tone: 'info' },
  REJECTED: { label: '已拒绝', tone: 'error' },
  COMPLETED: { label: '已完成', tone: 'success' },
  FAILED: { label: '失败', tone: 'warning' },
};

interface RefundRequestCardProps {
  request: RefundRequestVo;
  /** 通过/重试(FAILED 时文案为「重试」) */
  onApprove?: (request: RefundRequestVo) => void;
  /** 驳回(仅 PENDING) */
  onReject?: (request: RefundRequestVo) => void;
}

/** 退单(退款)申请卡片(R-PAY-02):金额/原因/状态 + 通过/驳回操作。 */
export function RefundRequestCard({ request, onApprove, onReject }: RefundRequestCardProps) {
  const theme = useTheme();
  const info = REFUND_STATUS_MAP[request.status] ?? { label: request.status, tone: 'info' as const };
  const canApprove = request.status === 'PENDING' || request.status === 'FAILED';
  const canReject = request.status === 'PENDING';

  return (
    <Card>
      <View style={styles.header}>
        <ThemedText type="smallBold">订单 #{request.orderId}</ThemedText>
        <StatusBadge status={request.status} label={info.label} tone={info.tone} />
      </View>

      <OrderInfoRow
        label="退款金额"
        value={
          <ThemedText type="smallBold" themeColor="danger">
            <Money cents={request.amountCents} />
          </ThemedText>
        }
      />
      <OrderInfoRow label="申请时间" value={new Date(request.requestedAt).toLocaleString('zh-CN')} />
      <OrderInfoRow label="退款原因" value={request.reason} />
      {request.processedAt ? (
        <OrderInfoRow label="处理时间" value={new Date(request.processedAt).toLocaleString('zh-CN')} />
      ) : null}
      {request.adminNote ? (
        <ThemedText type="caption" themeColor="textSecondary" style={styles.note}>
          审核备注: {request.adminNote}
        </ThemedText>
      ) : null}

      {(canApprove || canReject) && (onApprove || onReject) ? (
        <View style={[styles.actions, { borderTopColor: theme.border }]}>
          {canApprove && onApprove ? (
            <Button
              title={request.status === 'FAILED' ? '重试' : '通过'}
              size="sm"
              style={styles.actionButton}
              onPress={() => onApprove(request)}
            />
          ) : null}
          {canReject && onReject ? (
            <Button
              title="驳回"
              size="sm"
              variant="dangerGhost"
              style={styles.actionButton}
              onPress={() => onReject(request)}
            />
          ) : null}
        </View>
      ) : null}
    </Card>
  );
}

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.two,
  },
  note: {
    marginTop: Spacing.one,
  },
  actions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: Spacing.two,
    marginTop: Spacing.three,
    paddingTop: Spacing.three,
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  actionButton: {
    minWidth: 88,
  },
});
