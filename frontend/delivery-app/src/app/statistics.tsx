import { StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';

export default function StatisticsScreen() {
  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.safeArea}>
        <ThemedView style={styles.header}>
          <ThemedText type="subtitle">统计</ThemedText>
          <ThemedText themeColor="textSecondary">
            工作统计与收入汇总
          </ThemedText>
        </ThemedView>

        <ThemedView type="backgroundElement" style={styles.empty}>
          <ThemedText themeColor="textSecondary">统计功能即将上线</ThemedText>
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
