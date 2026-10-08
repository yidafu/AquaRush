import type { ReactNode } from 'react';
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';

import { Spacing } from '@/constants/theme';

interface StatCardGridProps {
  children: ReactNode;
  style?: StyleProp<ViewStyle>;
}

/** 统计卡片自适应网格(两列换行)。statistics / manage-dashboard / admin-workers 共用。 */
export function StatCardGrid({ children, style }: StatCardGridProps) {
  return <View style={[styles.grid, style]}>{children}</View>;
}

const styles = StyleSheet.create({
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.three,
  },
});
