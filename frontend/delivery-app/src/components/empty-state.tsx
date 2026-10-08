import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

interface EmptyStateProps {
  /** emoji 字符串或任意 ReactNode(字符串按 28px emoji 渲染) */
  icon: string | ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
}

/** 空态占位：primaryBg 圆 + primary 图标 + 标题/说明 + 可选 CTA。见 docs/ui-spec.md §5。 */
export function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  const theme = useTheme();

  return (
    <View style={styles.container}>
      <View style={[styles.iconCircle, { backgroundColor: theme.primaryBg }]}>
        {typeof icon === 'string' ? <ThemedText style={styles.iconText}>{icon}</ThemedText> : icon}
      </View>
      <ThemedText type="smallBold" themeColor="textSecondary" style={styles.title}>
        {title}
      </ThemedText>
      {description ? (
        <ThemedText type="small" themeColor="textTertiary" style={styles.description}>
          {description}
        </ThemedText>
      ) : null}
      {action ? <View style={styles.action}>{action}</View> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    paddingVertical: Spacing.six,
    paddingHorizontal: Spacing.four,
  },
  iconCircle: {
    width: 64,
    height: 64,
    borderRadius: 32,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.three,
  },
  iconText: {
    fontSize: 28,
    lineHeight: 32,
  },
  title: {
    textAlign: 'center',
  },
  description: {
    textAlign: 'center',
    marginTop: Spacing.one,
  },
  action: {
    marginTop: Spacing.four,
  },
});
