import type { StyleProp, ViewStyle } from 'react-native';
import { ActivityIndicator, StyleSheet } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

interface LoadingScreenProps {
  /** spinner 下方文案;空字符串则只显示 spinner(如 boot overlay) */
  label?: string;
  style?: StyleProp<ViewStyle>;
}

/** 全屏/遮罩加载态：pageBackground 底 + 居中 ActivityIndicator(+ 可选文案)。 */
export function LoadingScreen({ label = '加载中...', style }: LoadingScreenProps) {
  const theme = useTheme();

  return (
    <ThemedView type="pageBackground" style={[styles.container, style]}>
      <ActivityIndicator size="large" color={theme.primary} />
      {label ? (
        <ThemedText themeColor="textSecondary" style={styles.label}>
          {label}
        </ThemedText>
      ) : null}
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.three,
  },
  label: {
    textAlign: 'center',
  },
});
