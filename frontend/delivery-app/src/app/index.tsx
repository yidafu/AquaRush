import { StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';

export default function TasksScreen() {
  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.safeArea}>
        <ThemedView style={styles.header}>
          <ThemedText type="subtitle">订单</ThemedText>
          <ThemedText themeColor="textSecondary">
            查看待接单与配送中的订单
          </ThemedText>
        </ThemedView>

        <ThemedView type="backgroundElement" style={styles.empty}>
          <ThemedText themeColor="textSecondary">任务列表即将上线</ThemedText>
        </ThemedView>
      </SafeAreaView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  safeArea: {
    flex: 1,
    gap: Spacing.four,
    padding: Spacing.four,
  },
  header: {
    gap: Spacing.one,
  },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: Spacing.four,
  },
});
