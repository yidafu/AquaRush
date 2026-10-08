import { Pressable, StyleSheet, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Radius, Spacing } from '@/constants/theme';

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

/** 分段选择条：激活项 primaryBg 底 + primary 字。见 docs/ui-spec.md §5。 */
export function SegmentedTabs({ current, tabs, onChange }: SegmentedTabsProps) {
  return (
    <ThemedView type="backgroundElement" style={styles.container}>
      {tabs.map((tab, index) => {
        const active = index === current;
        return (
          <Pressable
            key={tab.title}
            onPress={() => onChange(index)}
            style={({ pressed }) => [styles.item, pressed && styles.pressed]}>
            {active ? (
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
          </Pressable>
        );
      })}
    </ThemedView>
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
