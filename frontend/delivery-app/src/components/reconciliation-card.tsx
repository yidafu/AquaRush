import { StyleSheet, View } from 'react-native';
import { Money } from '@aquarush/common';
import type { DailyReconciliationVo } from '@aquarush/common';

import { Card } from '@/components/card';
import { OrderInfoRow } from '@/components/order-info-row';
import { StatusBadge } from '@/components/status-badge';
import { ThemedText } from '@/components/themed-text';
import { TOLERANCE_CENTS, RECONCILIATION_STATUS_MAP, RECONCILIATION_STATUS_TONE } from '@/constants/collection';
import { Spacing } from '@/constants/theme';

/** 对账记录卡片：日期 + 状态 + 六项统计 + 复核信息。见 weapp reconciliation-records。 */
export function ReconciliationCard({ record }: { record: DailyReconciliationVo }) {
  const discrepancyMatched = Math.abs(record.discrepancyAmountCents) <= TOLERANCE_CENTS;

  return (
    <Card>
      <View style={styles.header}>
        <ThemedText type="smallBold">{record.reconciliationDate}</ThemedText>
        <StatusBadge
          status={record.status}
          label={RECONCILIATION_STATUS_MAP[record.status] ?? record.status}
          tone={RECONCILIATION_STATUS_TONE[record.status]}
        />
      </View>

      <View style={styles.statsRow}>
        <OrderInfoRow label="订单总数" value={`${record.totalOrderCount} 单`} style={styles.statItem} />
        <OrderInfoRow label="订单金额" value={<Money cents={record.totalOrderAmountCents} />} style={styles.statItem} />
      </View>
      <View style={styles.statsRow}>
        <OrderInfoRow label="现金收款" value={<Money cents={record.totalCashAmountCents} />} style={styles.statItem} />
        <OrderInfoRow label="扫码收款" value={<Money cents={record.totalQrCodeAmountCents} />} style={styles.statItem} />
      </View>
      <View style={styles.statsRow}>
        <OrderInfoRow label="水票数量" value={`${record.totalWaterTicketCount} 张`} style={styles.statItem} />
        <OrderInfoRow
          label="差异金额"
          value={
            <ThemedText type="small" themeColor={discrepancyMatched ? 'success' : 'danger'}>
              <Money cents={record.discrepancyAmountCents} />
            </ThemedText>
          }
          style={styles.statItem}
        />
      </View>

      {record.reviewedAt ? (
        <ThemedText type="caption" themeColor="textTertiary" style={styles.review}>
          复核于 {record.reviewedAt}
          {record.reviewNotes ? ` - ${record.reviewNotes}` : ''}
        </ThemedText>
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
  review: {
    marginTop: Spacing.one,
  },
});
