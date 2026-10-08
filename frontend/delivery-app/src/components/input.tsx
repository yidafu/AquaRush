import { useState } from 'react';
import { StyleSheet, TextInput, View, type StyleProp, type TextInputProps, type ViewStyle } from 'react-native';

import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

interface InputProps extends TextInputProps {
  size?: 'md' | 'lg';
  containerStyle?: StyleProp<ViewStyle>;
}

/** 统一的文本输入框：focus 时边框变 primary。见 docs/ui-spec.md §5。 */
export function Input({ size = 'md', containerStyle, style, onFocus, onBlur, multiline, ...rest }: InputProps) {
  const theme = useTheme();
  const [focused, setFocused] = useState(false);

  return (
    <View
      style={[
        styles.container,
        {
          borderColor: focused ? theme.primary : theme.border,
          backgroundColor: theme.background,
          minHeight: multiline ? 96 : size === 'lg' ? 56 : 48,
        },
        focused && { borderColor: theme.primary },
        containerStyle,
      ]}>
      <TextInput
        multiline={multiline}
        placeholderTextColor={theme.textTertiary}
        style={[styles.input, multiline && styles.multiline, { color: theme.text }, style]}
        onFocus={(e) => {
          setFocused(true);
          onFocus?.(e);
        }}
        onBlur={(e) => {
          setFocused(false);
          onBlur?.(e);
        }}
        {...rest}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    borderWidth: 1,
    borderRadius: Radius.sm,
    paddingHorizontal: Spacing.three,
    justifyContent: 'center',
  },
  input: {
    fontSize: 16,
    lineHeight: 24,
    paddingVertical: Spacing.two,
  },
  multiline: {
    minHeight: 88,
    textAlignVertical: 'top',
  },
});
