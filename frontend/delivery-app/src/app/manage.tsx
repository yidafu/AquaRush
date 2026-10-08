import { useCallback, useEffect, useRef, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';

import { AdminOrders } from '@/components/manage/admin-orders';
import { AdminWorkers } from '@/components/manage/admin-workers';
import { ManageDashboard } from '@/components/manage/manage-dashboard';
import { SegmentedTabs } from '@/components/segmented-tabs';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/hooks/use-auth';

const SEGMENTS = ['看板', '全部订单', '送水员'];

function parseTabParam(raw?: string): number | null {
  if (raw === undefined) return null;
  const index = parseInt(raw, 10);
  return !Number.isNaN(index) && index >= 0 && index < SEGMENTS.length ? index : null;
}

/** 管理枢纽：仅 ADMIN/SUPER_ADMIN 可见(工作台「经营看板」入口,原「管理」Tab 已移除;此处兜底非管理员跳回工作台)。 */
export default function ManageScreen() {
  const { isAdmin } = useAuth();
  const router = useRouter();
  const { tab } = useLocalSearchParams<{ tab?: string }>();
  const [current, setCurrent] = useState(() => parseTabParam(tab) ?? 0);

  useEffect(() => {
    if (!isAdmin) router.replace('/');
  }, [isAdmin, router]);

  // 工作台九宫格「全部订单」深链(?tab=1)切换分段;Tab 切换后参数变化同样生效
  const lastTabParam = useRef(tab);
  useFocusEffect(
    useCallback(() => {
      if (tab !== lastTabParam.current) {
        lastTabParam.current = tab;
        const parsed = parseTabParam(tab);
        if (parsed !== null) {
          setCurrent(parsed);
        }
      }
    }, [tab]),
  );

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <View style={styles.safeArea}>
        <SegmentedTabs current={current} tabs={SEGMENTS.map((t) => ({ title: t }))} onChange={setCurrent} />
        {current === 0 ? <ManageDashboard /> : null}
        {current === 1 ? <AdminOrders /> : null}
        {current === 2 ? <AdminWorkers /> : null}
      </View>
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
