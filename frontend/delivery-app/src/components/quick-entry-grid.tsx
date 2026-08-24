import { Pressable, StyleSheet, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export interface QuickEntryItem {
  /** 唯一 key */
  key: string;
  /** 图标(emoji,置于 10% tint 圆底) */
  icon: string;
  label: string;
  onPress: () => void;
}

interface QuickEntryGridProps {
  entries: QuickEntryItem[];
  columns?: number;
}

/** 九宫格快捷入口:3 列 emoji 图标 + 文案网格,点击跳转对应页面。见 docs/ui-spec.md §5。 */
export function QuickEntryGrid({ entries, columns = 3 }: QuickEntryGridProps) {
  const theme = useTheme();

  return (
    <View style={styles.grid}>
      {entries.map((entry) => (
        <Pressable
          key={entry.key}
          onPress={entry.onPress}
          style={({ pressed }) => [styles.item, pressed && styles.pressed]}>
          <View style={[styles.iconCircle, { backgroundColor: theme.primaryBg }]}>
            <ThemedText type="subtitle">{entry.icon}</ThemedText>
          </View>
          <ThemedText type="caption" themeColor="textSecondary">
            {entry.label}
          </ThemedText>
        </Pressable>
      ))}
      {/* 补齐不完整行占位,保持九宫格对齐 */}
      {entries.length % columns !== 0
        ? Array.from({ length: columns - (entries.length % columns) }).map((_, i) => (
            <View key={`placeholder-${i}`} style={styles.item} />
          ))
        : null}
    </View>
  );
}

const styles = StyleSheet.create({
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  item: {
    width: `${100 / 3}%`,
    alignItems: 'center',
    gap: Spacing.one,
    paddingVertical: Spacing.three,
  },
  pressed: {
    opacity: 0.6,
  },
  iconCircle: {
    width: 48,
    height: 48,
    borderRadius: Radius.pill,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
