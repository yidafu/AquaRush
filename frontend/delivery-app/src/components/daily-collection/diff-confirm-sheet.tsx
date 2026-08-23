import { StyleSheet, View } from 'react-native';

import { BottomSheet } from '@/components/bottom-sheet';
import { Button } from '@/components/button';
import { Input } from '@/components/input';
import { OrderInfoRow } from '@/components/order-info-row';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { formatCentsToCurrency } from '@aquarush/common';

interface DiffConfirmSheetProps {
  visible: boolean;
  cashDiffCents: number;
  waterTicketDiff: number;
  qrCodeDiffCents: number;
  notes: string;
  onNotesChange: (text: string) => void;
  onConfirm: () => void;
  onCancel: () => void;
}

/**
 * 金额差异确认 BottomSheet(任一子项超出容差时二次确认,须填写差异原因说明)
 */
export function DiffConfirmSheet({
  visible,
  cashDiffCents,
  waterTicketDiff,
  qrCodeDiffCents,
  notes,
  onNotesChange,
  onConfirm,
  onCancel,
}: DiffConfirmSheetProps) {
  return (
    <BottomSheet visible={visible} onClose={onCancel} title="金额差异确认">
      <View style={styles.body}>
        <OrderInfoRow label="现金差异" value={formatCentsToCurrency(cashDiffCents)} />
        <OrderInfoRow
          label="水票差异"
          value={`${waterTicketDiff > 0 ? `+${waterTicketDiff}` : waterTicketDiff} 张`}
        />
        <OrderInfoRow label="扫码差异" value={formatCentsToCurrency(qrCodeDiffCents)} />
        <ThemedText type="small" themeColor="textSecondary" style={styles.label}>
          请输入差异原因说明：
        </ThemedText>
        <Input
          multiline
          value={notes}
          onChangeText={onNotesChange}
          maxLength={200}
          placeholder="请输入情况说明（如：某订单未收款、额外收到水票等）"
        />
        <View style={styles.actions}>
          <Button title="取消" variant="ghost" onPress={onCancel} style={styles.button} />
          <Button
            title="确定"
            onPress={onConfirm}
            disabled={!notes.trim()}
            style={styles.button}
          />
        </View>
      </View>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  body: {
    gap: Spacing.two,
  },
  label: {
    marginTop: Spacing.two,
  },
  actions: {
    flexDirection: 'row',
    gap: Spacing.two,
    marginTop: Spacing.four,
    marginBottom: Spacing.two,
  },
  button: {
    flex: 1,
  },
});
