import { StyleSheet, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { ToggleGroup } from '@aquarush/delivery-ui';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export interface SegmentedTabItem {
  title: string;
  /** 可选:携带业务标识(如筛选值),由调用方在 onChange 中通过索引回查 */
  key?: string;
}

interface SegmentedTabsProps {
  current: number;
  tabs: SegmentedTabItem[];
  onChange: (index: number) => void;
}

/**
 * 分段选择条：激活项 primaryBg 底 + primary 字。
 * 基于 ui/ToggleGroup（@rn-primitives/toggle-group，单选语义、不可取消选中），
 * 见 docs/ui-spec.md §5。
 */
export function SegmentedTabs({ current, tabs, onChange }: SegmentedTabsProps) {
  const theme = useTheme();

  return (
    <ToggleGroup.Root
      type="single"
      value={String(current)}
      onValueChange={(value) => {
        // toggle-group 单选再次点击会给出 undefined（取消选中）；分段条不允许取消，忽略之
        if (value !== undefined) {
          onChange(Number(value));
        }
      }}
      style={[styles.container, { backgroundColor: theme.backgroundElement }]}>
      {tabs.map((tab, index) => (
        <ToggleGroup.Item
          key={tab.title}
          value={String(index)}
          style={({ pressed }) => [styles.item, pressed && styles.pressed]}>
          {index === current ? (
            <ThemedView type="primaryBg" style={styles.itemBg}>
              <ThemedText type="small" themeColor="primary">
                {tab.title}
              </ThemedText>
            </ThemedView>
          ) : (
            <View style={styles.itemBg}>
              <ThemedText type="small" themeColor="textSecondary">
                {tab.title}
              </ThemedText>
            </View>
          )}
        </ToggleGroup.Item>
      ))}
    </ToggleGroup.Root>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    borderRadius: Radius.pill,
    padding: Spacing.half,
  },
  item: {
    flex: 1,
    borderRadius: Radius.pill,
  },
  itemBg: {
    paddingVertical: Spacing.two,
    alignItems: 'center',
    borderRadius: Radius.pill,
  },
  pressed: {
    opacity: 0.8,
  },
});
