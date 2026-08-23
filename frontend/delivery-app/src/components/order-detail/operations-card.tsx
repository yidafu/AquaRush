import { StyleSheet } from 'react-native';
import { formatDateTime } from '@aquarush/common';

import { Card } from '@/components/card';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { OPERATION_TYPE_MAP } from '@/constants/order';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

interface OrderOperation {
  id: string;
  operationType: string;
  description?: string;
  createdAt: string;
}

interface OperationsCardProps {
  operations?: OrderOperation[];
}

export function OperationsCard({ operations }: OperationsCardProps) {
  const theme = useTheme();
  if (!operations?.length) return null;

  return (
    <Card title="订单记录">
      {operations.map((op, index) => (
        <ThemedView key={op.id || index} style={styles.item}>
          <ThemedView style={[styles.dot, { backgroundColor: theme.primary }]} />
          <ThemedView style={styles.content}>
            <ThemedText type="smallBold">
              {OPERATION_TYPE_MAP[op.operationType] || op.operationType}
            </ThemedText>
            {op.description ? (
              <ThemedText type="small" themeColor="textSecondary">
                {op.description}
              </ThemedText>
            ) : null}
            <ThemedText type="small" themeColor="textSecondary">
              {formatDateTime(op.createdAt)}
            </ThemedText>
          </ThemedView>
        </ThemedView>
      ))}
    </Card>
  );
}

const styles = StyleSheet.create({
  item: {
    flexDirection: 'row',
    gap: Spacing.two,
    marginBottom: Spacing.three,
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginTop: 6,
  },
  content: {
    flex: 1,
    gap: Spacing.half,
  },
});
