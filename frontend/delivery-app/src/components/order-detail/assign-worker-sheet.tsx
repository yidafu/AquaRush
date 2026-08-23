import { Pressable, StyleSheet, View } from 'react-native';

import { BottomSheet } from '@/components/bottom-sheet';
import { StatusBadge } from '@/components/status-badge';
import { ThemedText } from '@/components/themed-text';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { DeliveryWorkerStatus } from '@aquarush/common';
import type { DeliveryWorker } from '@aquarush/common';

interface AssignWorkerSheetProps {
  visible: boolean;
  onClose: () => void;
  workers: DeliveryWorker[];
  isSelfCollect: boolean;
  assigning: boolean;
  /** 当前订单配送员 id(用于「当前」角标) */
  currentWorkerId?: string;
  onToggleSelfCollect: () => void;
  onAssign: (workerId: string) => void;
}

/**
 * 管理员派单 BottomSheet:自收/到付切换 + 送水员列表(在线/离线/当前角标)
 */
export function AssignWorkerSheet({
  visible,
  onClose,
  workers,
  isSelfCollect,
  assigning,
  currentWorkerId,
  onToggleSelfCollect,
  onAssign,
}: AssignWorkerSheetProps) {
  const theme = useTheme();

  return (
    <BottomSheet visible={visible} onClose={onClose} title="分配送水员">
      <Pressable
        style={({ pressed }) => [styles.item, pressed && { backgroundColor: theme.backgroundSelected }]}
        onPress={onToggleSelfCollect}>
        <ThemedText type="smallBold" themeColor={isSelfCollect ? 'primary' : 'textSecondary'}>
          {isSelfCollect ? '☑ 已收款（自收）' : '☐ 到付（货到收款）'}
        </ThemedText>
      </Pressable>
      <View style={[styles.divider, { borderTopColor: theme.border }]} />
      {workers.map((worker) => {
        const isCurrent = currentWorkerId === worker.id;
        return (
          <Pressable
            key={worker.id}
            style={({ pressed }) => [styles.workerRow, pressed && { backgroundColor: theme.backgroundSelected }]}
            onPress={() => onAssign(worker.id)}
            disabled={assigning}>
            <View style={styles.workerInfo}>
              <ThemedText type="smallBold">{worker.name}</ThemedText>
              <ThemedText type="caption" themeColor="textTertiary">
                {worker.phone}
              </ThemedText>
            </View>
            <View style={styles.workerBadges}>
              {worker.onlineStatus === DeliveryWorkerStatus.ONLINE ? (
                <StatusBadge label="在线" tone="success" />
              ) : (
                <StatusBadge label="离线" tone="info" />
              )}
              {isCurrent ? <StatusBadge label="当前" tone="accent" /> : null}
            </View>
          </Pressable>
        );
      })}
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  item: {
    paddingVertical: Spacing.three,
    alignItems: 'center',
    borderRadius: Radius.sm,
  },
  divider: {
    borderTopWidth: StyleSheet.hairlineWidth,
    marginTop: Spacing.two,
  },
  workerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: Spacing.three,
    borderRadius: Radius.sm,
    gap: Spacing.two,
  },
  workerInfo: {
    flex: 1,
    gap: Spacing.half,
  },
  workerBadges: {
    flexDirection: 'row',
    gap: Spacing.two,
    alignItems: 'center',
  },
});
