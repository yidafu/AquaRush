import { Pressable, StyleSheet } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { Shadow, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

interface FloatingButtonProps {
  onPress: () => void;
}

/** 悬浮 "+" 按钮：primary 圆 + primaryGlow 阴影。见 docs/ui-spec.md §5。 */
export function FloatingButton({ onPress }: FloatingButtonProps) {
  const theme = useTheme();

  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        { backgroundColor: theme.primary },
        pressed && styles.pressed,
      ]}>
      <ThemedText style={[styles.icon, { color: theme.headerText }]}>+</ThemedText>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    position: 'absolute',
    right: Spacing.four,
    bottom: Spacing.five,
    width: 56,
    height: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
    ...Shadow.primaryGlow,
  },
  icon: {
    fontSize: 28,
    lineHeight: 32,
  },
  pressed: {
    transform: [{ scale: 0.94 }],
  },
});
