import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Radius, Shadow, Spacing } from '@/constants/theme';

interface CardProps {
  title?: string;
  /** 标题右侧插槽（如金额/状态标签） */
  right?: ReactNode;
  children: ReactNode;
}

/** 卡片容器：card 底 + 16px 大圆角 + 软阴影。见 docs/ui-spec.md §5。 */
export function Card({ title, right, children }: CardProps) {
  return (
    <ThemedView type="card" style={styles.card}>
      {title || right ? (
        <View style={styles.header}>
          {title ? (
            <ThemedText type="heading" style={styles.title}>
              {title}
            </ThemedText>
          ) : null}
          {right ? <View style={styles.right}>{right}</View> : null}
        </View>
      ) : null}
      {children}
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: Radius.lg,
    padding: Spacing.three,
    marginBottom: Spacing.three,
    ...Shadow.md,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: Spacing.two,
  },
  title: {
    marginBottom: 0,
  },
  right: {
    marginLeft: Spacing.two,
  },
});
