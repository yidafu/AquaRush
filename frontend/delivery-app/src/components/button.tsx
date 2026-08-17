import { ActivityIndicator, Pressable, StyleSheet, Text, type StyleProp, type ViewStyle } from 'react-native';
import type { TrackAttributes } from '@aquarush/common/tracking';

import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export type ButtonVariant = 'primary' | 'ghost' | 'danger' | 'dangerGhost';
export type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends TrackAttributes {
  title: string;
  onPress: () => void;
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
}

const SIZES: Record<ButtonSize, number> = {
  sm: 44,
  md: 48,
  lg: 56,
};

/** 统一的按钮组件：primary/ghost/danger/dangerGhost × sm/md/lg，按压 scale(0.98)。见 docs/ui-spec.md §5。 */
export function Button({
  title,
  onPress,
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  style,
}: ButtonProps) {
  const theme = useTheme();
  const height = SIZES[size];
  const inactive = disabled || loading;

  const isFilled = variant === 'primary' || variant === 'danger';
  const textColor = isFilled ? theme.headerText : variant === 'ghost' ? theme.primary : theme.danger;
  const backgroundColor =
    variant === 'primary' ? theme.primary : variant === 'danger' ? theme.danger : 'transparent';
  const borderColor =
    variant === 'ghost' ? theme.primary : variant === 'dangerGhost' ? theme.danger : undefined;

  return (
    <Pressable
      onPress={onPress}
      disabled={inactive}
      style={({ pressed }) => [
        styles.base,
        {
          height,
          backgroundColor,
          borderColor,
          borderRadius: size === 'sm' ? Radius.pill : Radius.sm,
        },
        pressed && !inactive && styles.pressed,
        inactive && styles.disabled,
        style,
      ]}>
      {loading ? (
        <ActivityIndicator color={textColor} />
      ) : (
        <Text style={[styles.label, { color: textColor }]}>{title}</Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: Spacing.four,
  },
  label: {
    fontSize: 16,
    lineHeight: 24,
    fontWeight: '600',
  },
  pressed: {
    transform: [{ scale: 0.98 }],
  },
  disabled: {
    opacity: 0.6,
  },
});
