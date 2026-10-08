import type { ReactNode } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import { Gradient } from '@/components/gradient';
import { ThemedText } from '@/components/themed-text';
import { Radius, Shadow, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

interface StatCardProps {
  label: string;
  /** 值节点：高亮卡需由调用方包一层 ThemedText themeColor="headerText"（白字） */
  value: ReactNode;
  /** 高亮卡：primary 渐变底 + 阴影（营收卡） */
  highlight?: boolean;
  onPress?: () => void;
}

/** 统计数字卡：值 + 标签，可点击（跳转历史订单 tab）。见 weapp StatCard。 */
export function StatCard({ label, value, highlight = false, onPress }: StatCardProps) {
  const theme = useTheme();

  const content = (
    <View
      style={[
        styles.card,
        highlight ? styles.highlightCard : { backgroundColor: theme.card },
      ]}>
      {highlight ? (
        <Gradient
          colors={[theme.primary, theme.primaryDark] as const}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={StyleSheet.absoluteFill}
        />
      ) : null}
      <View style={styles.value}>{value}</View>
      <ThemedText
        type="small"
        themeColor={highlight ? 'headerText' : 'textSecondary'}
        style={styles.label}>
        {label}
      </ThemedText>
    </View>
  );

  if (!onPress) return content;

  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [styles.pressable, pressed && styles.pressed]}>
      {content}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  pressable: {
    width: '48%',
  },
  pressed: {
    opacity: 0.85,
  },
  card: {
    borderRadius: Radius.lg,
    padding: Spacing.three,
    minHeight: 88,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
    ...Shadow.sm,
  },
  highlightCard: {
    ...Shadow.primaryGlow,
  },
  value: {
    marginBottom: Spacing.one,
  },
  label: {
    textAlign: 'center',
  },
});
