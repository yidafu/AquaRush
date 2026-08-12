import { StyleSheet, Switch, View } from 'react-native';
import { Money } from '@aquarush/common';

import { Card } from '@/components/card';
import { Input } from '@/components/input';
import { OrderInfoRow } from '@/components/order-info-row';
import { QuantityStepper } from '@/components/quantity-stepper';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export interface OrderInfo {
  quantity: number;
  isSelfCollect: boolean;
  remark: string;
}

interface OrderSummaryCardProps {
  product?: {
    id: string;
    priceCents?: number | string;
  } | null;
  orderInfo: OrderInfo;
  onOrderInfoChange: (orderInfo: OrderInfo) => void;
}

export function OrderSummaryCard({ product, orderInfo, onOrderInfoChange }: OrderSummaryCardProps) {
  const theme = useTheme();
  const { quantity, isSelfCollect, remark } = orderInfo;
  const priceCents = Number(product?.priceCents ?? 0);
  const totalAmount = product ? priceCents * quantity : 0;

  return (
    <Card title="订单详情">
      <OrderInfoRow
        label="商品单价"
        value={
          <ThemedText type="small">
            <Money cents={priceCents} />
          </ThemedText>
        }
      />

      <View style={styles.row}>
        <ThemedText type="small" themeColor="textSecondary" style={styles.rowLabel}>
          数量
        </ThemedText>
        <QuantityStepper value={quantity} onChange={(next) => onOrderInfoChange({ ...orderInfo, quantity: next })} />
      </View>

      <OrderInfoRow
        label="合计"
        value={
          <ThemedText type="smallBold" themeColor="primary">
            <Money cents={totalAmount} />
          </ThemedText>
        }
      />

      <View style={styles.row}>
        <ThemedText type="small" themeColor="textSecondary" style={styles.rowLabel}>
          是否已收款
        </ThemedText>
        <Switch
          value={isSelfCollect}
          onValueChange={(value) => onOrderInfoChange({ ...orderInfo, isSelfCollect: value })}
          trackColor={{ true: theme.primary }}
        />
      </View>

      <Input
        style={styles.remarkInput}
        value={remark}
        onChangeText={(value) => onOrderInfoChange({ ...orderInfo, remark: value })}
        placeholder="请输入订单备注（可选）"
        multiline
        maxLength={500}
      />
    </Card>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: Spacing.two,
  },
  rowLabel: {
    width: 76,
  },
  remarkInput: {
    marginTop: Spacing.two,
  },
});
