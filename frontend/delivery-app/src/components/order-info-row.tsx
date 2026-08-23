import { ReactNode } from 'react';
import { StyleSheet, View, type StyleProp, type TextStyle, type ViewStyle } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { LabelColumnWidth, Spacing } from '@/constants/theme';

interface OrderInfoRowProps {
  label: string;
  value?: string | number | ReactNode;
  valueStyle?: StyleProp<TextStyle>;
  style?: StyleProp<ViewStyle>;
}

export function OrderInfoRow({ label, value, valueStyle, style }: OrderInfoRowProps) {
  return (
    <View style={[styles.row, style]}>
      <ThemedText type="small" themeColor="textSecondary" style={styles.label}>
        {label}
      </ThemedText>
      {typeof value === 'string' || typeof value === 'number' ? (
        <ThemedText type="small" style={[styles.value, valueStyle]}>
          {value}
        </ThemedText>
      ) : (
        <View style={styles.value}>{value}</View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: Spacing.two,
    marginBottom: Spacing.one,
  },
  label: {
    width: LabelColumnWidth,
    flexShrink: 0,
  },
  value: {
    flex: 1,
  },
});
