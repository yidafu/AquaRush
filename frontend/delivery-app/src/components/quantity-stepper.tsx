import { Pressable, StyleSheet, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

interface QuantityStepperProps {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
}

/** 数量步进器：− 值 ＋。见 docs/ui-spec.md §5。 */
export function QuantityStepper({ value, onChange, min = 1, max = 99 }: QuantityStepperProps) {
  const theme = useTheme();

  const canDecrease = value > min;
  const canIncrease = value < max;

  const change = (delta: number) => {
    onChange(Math.min(max, Math.max(min, value + delta)));
  };

  return (
    <View style={styles.stepper}>
      <Pressable
        onPress={() => change(-1)}
        disabled={!canDecrease}
        style={({ pressed }) => [
          styles.stepBtn,
          { borderColor: theme.border },
          pressed && styles.pressed,
          !canDecrease && styles.disabled,
        ]}>
        <ThemedText style={styles.stepText}>−</ThemedText>
      </Pressable>
      <ThemedText type="smallBold" style={styles.stepValue}>
        {value}
      </ThemedText>
      <Pressable
        onPress={() => change(1)}
        disabled={!canIncrease}
        style={({ pressed }) => [
          styles.stepBtn,
          { borderColor: theme.border },
          pressed && styles.pressed,
          !canIncrease && styles.disabled,
        ]}>
        <ThemedText style={styles.stepText}>＋</ThemedText>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  stepper: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
  },
  stepBtn: {
    width: 32,
    height: 32,
    borderRadius: Spacing.one,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepText: {
    fontSize: 18,
    lineHeight: 22,
  },
  stepValue: {
    minWidth: 28,
    textAlign: 'center',
  },
  pressed: {
    opacity: 0.7,
  },
  disabled: {
    opacity: 0.4,
  },
});
