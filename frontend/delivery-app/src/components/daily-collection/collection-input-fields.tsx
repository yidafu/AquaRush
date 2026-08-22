import { StyleSheet, View } from 'react-native';

import { Input } from '@/components/input';
import { ThemedText } from '@/components/themed-text';
import { Radius, Spacing } from '@/constants/theme';
import { formatCentsToCurrency } from '@aquarush/common';
import { useTheme } from '@/hooks/use-theme';

interface CollectionInputFieldsProps {
  cashAmount: string;
  waterTicketCount: string;
  qrCodeAmount: string;
  totalCents: number;
  onCashAmountChange: (text: string) => void;
  onWaterTicketCountChange: (text: string) => void;
  onQrCodeAmountChange: (text: string) => void;
}

/**
 * 今日收款录入表单(现金/水票/扫码三行 + 合计)
 */
export function CollectionInputFields({
  cashAmount,
  waterTicketCount,
  qrCodeAmount,
  totalCents,
  onCashAmountChange,
  onWaterTicketCountChange,
  onQrCodeAmountChange,
}: CollectionInputFieldsProps) {
  const theme = useTheme();

  const formField = (
    label: string,
    value: string,
    onChangeText: (t: string) => void,
    unit: string,
    keyboardType: 'decimal-pad' | 'number-pad',
  ) => (
    <View style={styles.formRow}>
      <ThemedText type="small" themeColor="textSecondary" style={styles.formLabel}>
        {label}
      </ThemedText>
      <View style={[styles.inputWrap, { borderColor: theme.border, backgroundColor: theme.background }]}>
        <Input
          value={value}
          onChangeText={onChangeText}
          keyboardType={keyboardType}
          placeholder={`请输入${label}`}
          containerStyle={styles.input}
        />
        <ThemedText type="small" themeColor="textTertiary" style={styles.unit}>
          {unit}
        </ThemedText>
      </View>
    </View>
  );

  return (
    <View style={styles.formSection}>
      {formField('现金收款', cashAmount, onCashAmountChange, '元', 'decimal-pad')}
      {formField('水票数量', waterTicketCount, onWaterTicketCountChange, '张', 'number-pad')}
      {formField('扫码收款', qrCodeAmount, onQrCodeAmountChange, '元', 'decimal-pad')}

      <View style={styles.totalRow}>
        <ThemedText type="smallBold">合计：</ThemedText>
        <ThemedText type="smallBold" themeColor="primary">
          {formatCentsToCurrency(totalCents)}
        </ThemedText>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  formSection: {
    marginTop: Spacing.three,
    gap: Spacing.three,
  },
  formRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  formLabel: {
    width: 76,
    flexShrink: 0,
  },
  inputWrap: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: Radius.sm,
    borderWidth: 1,
  },
  input: {
    flex: 1,
    borderWidth: 0,
    backgroundColor: 'transparent',
  },
  unit: {
    paddingRight: Spacing.three,
  },
  totalRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: Spacing.three,
  },
});
