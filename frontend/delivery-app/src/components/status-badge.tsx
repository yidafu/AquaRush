import { StyleSheet, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ORDER_STATUS_MAP } from '@/constants/order';
import { Radius, Spacing, StatusTints, type StatusTone } from '@/constants/theme';
import { useColorScheme } from '@/hooks/use-color-scheme';

/** 订单状态 → 标签 tone 的映射。见 docs/ui-spec.md §2。 */
export const STATUS_TONE: Record<string, StatusTone> = {
  PENDING_DISPATCH: 'processing',
  PENDING_DELIVERY: 'processing',
  DELIVERING: 'success',
  COMPLETED: 'success',
  CANCELLED: 'error',
  REFUNDED: 'warning',
  PENDING_PAYMENT: 'warning',
};

interface StatusBadgeProps {
  /** 订单状态 key，自动取文案与 tone */
  status?: string;
  /** 覆盖文案（如「自收」） */
  label?: string;
  /** 覆盖 tone */
  tone?: StatusTone;
}

/** 订单状态标签：10% tint 底 + 状态色文字。 */
export function StatusBadge({ status, label, tone }: StatusBadgeProps) {
  const scheme = useColorScheme();
  const tints = StatusTints[scheme === 'dark' ? 'dark' : 'light'];
  const resolvedTone = tone ?? (status ? STATUS_TONE[status] : undefined);
  const resolvedLabel = label ?? (status ? ORDER_STATUS_MAP[status] ?? status : undefined);

  if (!resolvedTone || !resolvedLabel) return null;

  const palette = tints[resolvedTone];
  return (
    <View style={[styles.badge, { backgroundColor: palette.bg }]}>
      <ThemedText style={[styles.text, { color: palette.text }]}>{resolvedLabel}</ThemedText>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    alignSelf: 'flex-start',
    borderRadius: Radius.pill,
    paddingHorizontal: Spacing.two,
    paddingVertical: Spacing.half,
  },
  text: {
    fontSize: 12,
    lineHeight: 16,
    fontWeight: '600',
  },
});
