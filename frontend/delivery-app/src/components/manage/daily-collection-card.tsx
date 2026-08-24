import { StyleSheet, View } from 'react-native';
import { Money } from '@aquarush/common';
import type { DailyCollectionRecord } from '@aquarush/common';

import { Button } from '@/components/button';
import { Card } from '@/components/card';
import { OrderInfoRow } from '@/components/order-info-row';
import { StatusBadge } from '@/components/status-badge';
import { ThemedText } from '@/components/themed-text';
import { DAILY_COLLECTION_STATUS_MAP, DAILY_COLLECTION_STATUS_TONE, TOLERANCE_CENTS } from '@/constants/collection';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

interface DailyCollectionCardProps {
  record: DailyCollectionRecord;
  /** 管理员确认收款 */
  onConfirm?: (record: DailyCollectionRecord) => void;
  /** 管理员驳回收款 */
  onReject?: (record: DailyCollectionRecord) => void;
}

/** 收银对账-每日收款记录卡片(F-COL-010):送水员/日期/填报与系统计算值/差异/状态 + 确认/驳回操作。 */
export function DailyCollectionCard({ record, onConfirm, onReject }: DailyCollectionCardProps) {
  const theme = useTheme();
  const discrepancyMatched = Math.abs(record.differenceAmountCents) <= TOLERANCE_CENTS;
  const actionable = record.status === 'SUBMITTED' || record.status === 'DISPUTED';

  return (
    <Card>
      <View style={styles.header}>
        <ThemedText type="smallBold">送水员 #{record.deliveryWorkerId}</ThemedText>
        <StatusBadge
          status={record.status}
          label={DAILY_COLLECTION_STATUS_MAP[record.status] ?? record.status}
          tone={DAILY_COLLECTION_STATUS_TONE[record.status]}
        />
      </View>

      <OrderInfoRow label="收款日期" value={record.collectionDate} />
      <View style={styles.statsRow}>
        <OrderInfoRow label="现金" value={<Money cents={record.cashAmountCents} />} style={styles.statItem} />
        <OrderInfoRow label="扫码" value={<Money cents={record.qrCodeAmountCents} />} style={styles.statItem} />
        <OrderInfoRow label="水票" value={`${record.waterTicketCount} 张`} style={styles.statItem} />
      </View>
      <View style={styles.statsRow}>
        <OrderInfoRow label="订单数" value={`${record.orderCount} 单`} style={styles.statItem} />
        <OrderInfoRow label="订单金额" value={<Money cents={record.orderAmountCents} />} style={styles.statItem} />
        <OrderInfoRow
          label="差异"
          value={
            <ThemedText type="small" themeColor={discrepancyMatched ? 'success' : 'danger'}>
              <Money cents={record.differenceAmountCents} />
            </ThemedText>
          }
          style={styles.statItem}
        />
      </View>
      <View style={styles.statsRow}>
        <OrderInfoRow
          label="系统现金"
          value={<Money cents={record.calculatedCashAmountCents} />}
          style={styles.statItem}
        />
        <OrderInfoRow
          label="系统扫码"
          value={<Money cents={record.calculatedQrCodeAmountCents} />}
          style={styles.statItem}
        />
        <OrderInfoRow
          label="系统水票"
          value={`${record.calculatedWaterTicketCount} 张`}
          style={styles.statItem}
        />
      </View>

      {record.notes ? (
        <ThemedText type="caption" themeColor="textSecondary" style={styles.note}>
          差异说明: {record.notes}
        </ThemedText>
      ) : null}
      {record.rejectReason ? (
        <ThemedText type="caption" themeColor="danger" style={styles.note}>
          驳回: {record.rejectReason}
        </ThemedText>
      ) : null}

      {actionable && (onConfirm || onReject) ? (
        <View style={[styles.actions, { borderTopColor: theme.border }]}>
          {onConfirm ? (
            <Button title="确认" size="sm" style={styles.actionButton} onPress={() => onConfirm(record)} />
          ) : null}
          {onReject ? (
            <Button
              title="驳回"
              size="sm"
              variant="dangerGhost"
              style={styles.actionButton}
              onPress={() => onReject(record)}
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
  statsRow: {
    flexDirection: 'row',
  },
  statItem: {
    flex: 1,
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
