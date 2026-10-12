import { useEffect, useState } from 'react';
import { Animated, StyleSheet, type StyleProp, type ViewStyle } from 'react-native';

import { SwitchPrimitive } from '@aquarush/delivery-ui';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

const TRACK_WIDTH = 46;
const TRACK_HEIGHT = 28;
const THUMB_SIZE = 24;
const THUMB_PADDING = 2;
const THUMB_TRAVEL = TRACK_WIDTH - THUMB_SIZE - THUMB_PADDING * 2;
const ANIMATION_MS = 150;

interface SwitchProps {
  value: boolean;
  onValueChange: (value: boolean) => void;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
}

/**
 * 开关：轨道 on=primary / off=backgroundSelected，白色 thumb 滑动。
 * 基于 ui/SwitchPrimitive（@rn-primitives/switch），见 docs/ui-spec.md §5。
 */
export function Switch({ value, onValueChange, disabled = false, style }: SwitchProps) {
  const theme = useTheme();
  // Animated.Value 放 state 惰性初始化：渲染期不读 ref.current（react-hooks/refs）
  const [thumbX] = useState(() => new Animated.Value(value ? 1 : 0));

  useEffect(() => {
    Animated.timing(thumbX, {
      toValue: value ? 1 : 0,
      duration: ANIMATION_MS,
      useNativeDriver: true,
    }).start();
  }, [value, thumbX]);

  const translateX = thumbX.interpolate({ inputRange: [0, 1], outputRange: [0, THUMB_TRAVEL] });

  return (
    <SwitchPrimitive.Root
      checked={value}
      onCheckedChange={onValueChange}
      disabled={disabled}
      hitSlop={Spacing.two}
      style={[
        styles.track,
        { backgroundColor: value ? theme.primary : theme.backgroundSelected },
        disabled && styles.disabled,
        style,
      ]}>
      <SwitchPrimitive.Thumb asChild>
        <Animated.View
          style={[styles.thumb, { backgroundColor: theme.card, transform: [{ translateX }] }]}
        />
      </SwitchPrimitive.Thumb>
    </SwitchPrimitive.Root>
  );
}

const styles = StyleSheet.create({
  track: {
    width: TRACK_WIDTH,
    height: TRACK_HEIGHT,
    borderRadius: TRACK_HEIGHT / 2,
    padding: THUMB_PADDING,
    justifyContent: 'center',
  },
  thumb: {
    width: THUMB_SIZE,
    height: THUMB_SIZE,
    borderRadius: THUMB_SIZE / 2,
  },
  disabled: {
    opacity: 0.6,
  },
});
