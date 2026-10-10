import { StyleSheet, View } from 'react-native';

import { Card } from '@/components/card';
import { QuantityStepper } from '@/components/quantity-stepper';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import type { EmptyBarrelRecoveryInput } from '@aquarush/common';

export interface EmptyBarrelRecycleCardProps {
  /** 订单商品规格列表(由 items 聚合去重,含空规格的商品不展示) */
  specifications: string[];
  /** 规格 → 回收数量(0 = 未回收) */
  counts: Record<string, number>;
  onChange: (counts: Record<string, number>) => void;
}

/** 计数 → completeDelivery 的 emptyBarrels 变量(全 0 时返回 undefined,不传该参数) */
export const toEmptyBarrels = (
  counts: Record<string, number>,
): EmptyBarrelRecoveryInput[] | undefined => {
  const list = Object.entries(counts)
    .filter(([specification, count]) => specification && count > 0)
    .map(([specification, count]) => ({ specification, count }));
  return list.length > 0 ? list : undefined;
};

/**
 * 回收空桶登记(F-ORD-011,完成配送时可选登记回收数量):
 * 按订单商品规格逐行列出计数器,非必填;0/不登记不传 emptyBarrels 参数。
 */
export function EmptyBarrelRecycleCard({
  specifications,
  counts,
  onChange,
}: EmptyBarrelRecycleCardProps) {
  if (specifications.length === 0) return null;

  const setCount = (specification: string, count: number) => {
    onChange({ ...counts, [specification]: count });
  };

  return (
    <Card title="回收空桶（选填）">
      <ThemedText type="small" themeColor="textSecondary" style={styles.hint}>
        回收空桶时按规格登记数量,用于押金与桶账核对;无回收可跳过。
      </ThemedText>
      {specifications.map((specification) => (
        <View key={specification} style={styles.row}>
          <View style={styles.info}>
            <ThemedText type="smallBold">{specification}</ThemedText>
            <ThemedText type="caption" themeColor="textSecondary">
              回收 {counts[specification] ?? 0} 个
            </ThemedText>
          </View>
          <QuantityStepper
            min={0}
            max={99}
            value={counts[specification] ?? 0}
            onChange={(value) => setCount(specification, value)}
          />
        </View>
      ))}
    </Card>
  );
}

const styles = StyleSheet.create({
  hint: {
    marginBottom: Spacing.two,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: Spacing.two,
  },
  info: {
    gap: Spacing.one,
  },
});
