import { Pressable, StyleSheet, View } from 'react-native';

import { BottomSheet } from '@/components/bottom-sheet';
import { ThemedText } from '@/components/themed-text';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

const PAYMENT_TYPES = [
  { value: 'WATER_TICKET', label: '水票收款' },
  { value: 'CASH', label: '现金收款' },
  { value: 'QR_CODE', label: '二维码收款' },
];

interface PaymentCollectionSheetProps {
  visible: boolean;
  onClose: () => void;
  onSelect: (paymentType: string) => void;
  /** 本次回收空桶摘要(如「19L 桶 × 2」;空则不展示,F-ORD-011) */
  recycleSummary?: string;
}

/**
 * 确认送达收款方式选择 BottomSheet(水票/现金/二维码/取消)
 * 顶部回显已登记的回收空桶,提示送水员核对后选择收款方式(F-ORD-011)。
 */
export function PaymentCollectionSheet({
  visible,
  onClose,
  onSelect,
  recycleSummary,
}: PaymentCollectionSheetProps) {
  const theme = useTheme();

  return (
    <BottomSheet visible={visible} onClose={onClose} title="选择收款方式">
      {recycleSummary ? (
        <ThemedText type="small" themeColor="textSecondary" style={styles.recycleHint}>
          回收空桶：{recycleSummary}（可在订单页修改）
        </ThemedText>
      ) : null}
      {PAYMENT_TYPES.map(({ value, label }) => (
        <Pressable
          key={value}
          style={({ pressed }) => [
            styles.item,
            pressed && { backgroundColor: theme.backgroundSelected },
          ]}
          onPress={() => onSelect(value)}>
          <ThemedText type="smallBold">{label}</ThemedText>
        </Pressable>
      ))}
      <View style={[styles.divider, { borderTopColor: theme.border }]} />
      <Pressable
        style={({ pressed }) => [styles.item, pressed && { backgroundColor: theme.backgroundSelected }]}
        onPress={onClose}>
        <ThemedText type="smallBold" themeColor="textSecondary">
          取消
        </ThemedText>
      </Pressable>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  recycleHint: {
    marginBottom: Spacing.two,
    textAlign: 'center',
  },
  item: {
    paddingVertical: Spacing.three,
    alignItems: 'center',
    borderRadius: Radius.sm,
  },
  divider: {
    borderTopWidth: StyleSheet.hairlineWidth,
    marginTop: Spacing.two,
  },
});
