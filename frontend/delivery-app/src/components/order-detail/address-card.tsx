import { Pressable, StyleSheet } from 'react-native';

import { Card } from '@/components/card';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import type { AddressVo } from '@aquarush/common';

interface AddressCardProps {
  address?: AddressVo;
  /** 提供时渲染「修改」入口(F-ORD-024 订单页地址信息修正) */
  onEdit?: () => void;
}

export function AddressCard({ address, onEdit }: AddressCardProps) {
  const theme = useTheme();
  if (!address) return null;

  const hasCoords = address.longitude != null && address.latitude != null;

  return (
    <Card title="收货地址">
      <ThemedView style={styles.row}>
        <ThemedText type="smallBold">{address.receiverName}</ThemedText>
        <ThemedText type="small" themeColor="textSecondary">
          {address.phone}
        </ThemedText>
      </ThemedView>
      <ThemedText type="small">
        {address.province}
        {address.city}
        {address.district}
        {address.detailAddress}
      </ThemedText>
      <ThemedView style={styles.metaRow}>
        {address.floor != null ? (
          <ThemedText type="caption" themeColor="textTertiary">
            {address.floor}楼
          </ThemedText>
        ) : null}
        {hasCoords ? (
          <ThemedText type="caption" themeColor="textTertiary">
            定位:{Number(address.latitude).toFixed(4)},{Number(address.longitude).toFixed(4)}
          </ThemedText>
        ) : (
          <ThemedText type="caption" themeColor="textTertiary">
            暂无定位
          </ThemedText>
        )}
      </ThemedView>
      {onEdit ? (
        <Pressable
          onPress={onEdit}
          style={({ pressed }) => [
            styles.editButton,
            { borderColor: theme.border, backgroundColor: pressed ? theme.backgroundSelected : theme.background },
          ]}>
          <ThemedText type="smallBold" themeColor="primary">
            修改地址信息(定位/楼层等)
          </ThemedText>
        </Pressable>
      ) : null}
    </Card>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: Spacing.one,
  },
  metaRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: Spacing.one,
    gap: Spacing.two,
  },
  editButton: {
    marginTop: Spacing.three,
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: Radius.sm,
    paddingVertical: Spacing.two,
    alignItems: 'center',
  },
});
