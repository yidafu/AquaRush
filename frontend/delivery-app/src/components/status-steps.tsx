import { Fragment } from 'react';
import { StyleSheet, View } from 'react-native';

import { Card } from '@/components/card';
import { ThemedText } from '@/components/themed-text';
import { ORDER_STEPS } from '@aquarush/common/utils/order-status';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

interface StatusStepsProps {
  current: number;
}

/** 配送进度步骤条：完成/当前 = primary 实心点 + 连接线，未到 = textTertiary 空心。见 docs/ui-spec.md §5。 */
export function StatusSteps({ current }: StatusStepsProps) {
  const theme = useTheme();

  return (
    <Card>
      <View style={styles.container}>
        {ORDER_STEPS.map((step, index) => {
          const done = index < current;
          const active = index === current;
          const finished = done || active;
          const connectorFilled = index <= current;
          return (
            <Fragment key={step}>
              {index > 0 ? (
                <View
                  style={[
                    styles.connector,
                    { backgroundColor: connectorFilled ? theme.primary : theme.border },
                  ]}
                />
              ) : null}
              <View style={styles.step}>
                <View
                  style={[
                    styles.dot,
                    {
                      borderColor: finished ? theme.primary : theme.textTertiary,
                      backgroundColor: finished ? theme.primary : 'transparent',
                    },
                  ]}
                />
                <ThemedText
                  type="caption"
                  style={{ color: active ? theme.primary : done ? theme.text : theme.textTertiary }}>
                  {step}
                </ThemedText>
              </View>
            </Fragment>
          );
        })}
      </View>
    </Card>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  connector: {
    flex: 1,
    height: 2,
    borderRadius: Radius.xs,
    marginTop: 6,
    marginHorizontal: Spacing.one,
  },
  step: {
    alignItems: 'center',
    gap: Spacing.one,
    width: 56,
  },
  dot: {
    width: 12,
    height: 12,
    borderRadius: 6,
    borderWidth: 2,
  },
});
