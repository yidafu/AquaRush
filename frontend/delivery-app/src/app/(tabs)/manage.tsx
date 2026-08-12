import { useEffect, useState } from 'react';
import { StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';

import { AdminOrders } from '@/components/manage/admin-orders';
import { AdminWorkers } from '@/components/manage/admin-workers';
import { ManageDashboard } from '@/components/manage/manage-dashboard';
import { SegmentedTabs } from '@/components/segmented-tabs';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/hooks/use-auth';

const SEGMENTS = ['看板', '全部订单', '送水员'];

/** 管理枢纽：仅 ADMIN/SUPER_ADMIN 可见（Tab 条件渲染 + 此处兜底）。 */
export default function ManageScreen() {
  const { isAdmin } = useAuth();
  const router = useRouter();
  const [current, setCurrent] = useState(0);

  useEffect(() => {
    if (!isAdmin) router.replace('/');
  }, [isAdmin, router]);

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <SafeAreaView style={styles.safeArea} edges={['top']}>
        <SegmentedTabs current={current} tabs={SEGMENTS.map((t) => ({ title: t }))} onChange={setCurrent} />
        {current === 0 ? <ManageDashboard /> : null}
        {current === 1 ? <AdminOrders /> : null}
        {current === 2 ? <AdminWorkers /> : null}
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
    gap: Spacing.three,
    padding: Spacing.four,
  },
});
