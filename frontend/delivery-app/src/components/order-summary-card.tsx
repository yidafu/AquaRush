import { Pressable, StyleSheet, Switch, View } from 'react-native';
import { Money } from '@aquarush/common';

import { Card } from '@/components/card';
import { Input } from '@/components/input';
import { OrderInfoRow } from '@/components/order-info-row';
import { QuantityStepper } from '@/components/quantity-stepper';
import { ThemedText } from '@/components/themed-text';
import { Spacing, LabelColumnWidth } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

/** 代客下单商品明细行(F-ORD-012 多商品下单,items 为唯一口径 —— F-ORD-004) */
export interface OrderLine {
  product: {
    id: string;
    name: string;
    priceCents?: number | string;
  };
  quantity: number;
}

export interface OrderInfo {
  isSelfCollect: boolean;
  remark: string;
}

interface OrderSummaryCardProps {
  lines: OrderLine[];
  onLinesChange: (lines: OrderLine[]) => void;
  orderInfo: OrderInfo;
  onOrderInfoChange: (orderInfo: OrderInfo) => void;
}

export function OrderSummaryCard({ lines, onLinesChange, orderInfo, onOrderInfoChange }: OrderSummaryCardProps) {
  const theme = useTheme();
  const { isSelfCollect, remark } = orderInfo;

  const totalAmount = lines.reduce(
    (sum, line) => sum + Number(line.product.priceCents ?? 0) * line.quantity,
    0,
  );

  const handleQuantityChange = (productId: string, quantity: number) => {
    onLinesChange(
      lines
        .map((line) => (line.product.id === productId ? { ...line, quantity } : line))
        .filter((line) => line.quantity > 0),
    );
  };

  const handleRemove = (productId: string) => {
    onLinesChange(lines.filter((line) => line.product.id !== productId));
  };

  return (
    <Card title={`订单详情${lines.length > 1 ? `（共${lines.length}种商品）` : ''}`}>
      {lines.length === 0 ? (
        <ThemedText type="small" themeColor="textSecondary">
          请先添加商品
        </ThemedText>
      ) : (
        lines.map((line) => (
          <View key={line.product.id} style={styles.line}>
            <View style={styles.lineInfo}>
              <ThemedText type="smallBold" numberOfLines={1} style={styles.lineName}>
                {line.product.name}
              </ThemedText>
              <ThemedText type="small" themeColor="textSecondary">
                <Money cents={line.product.priceCents} />
              </ThemedText>
            </View>
            <QuantityStepper
              value={line.quantity}
              onChange={(next) => handleQuantityChange(line.product.id, next)}
            />
            <Pressable onPress={() => handleRemove(line.product.id)} hitSlop={8}>
              <ThemedText type="small" themeColor="textTertiary" style={styles.remove}>
                ×
              </ThemedText>
            </Pressable>
          </View>
        ))
      )}

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
  line: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    marginBottom: Spacing.two,
  },
  lineInfo: {
    flex: 1,
    gap: Spacing.half,
  },
  lineName: {
    flexShrink: 1,
  },
  remove: {
    fontSize: 22,
    paddingHorizontal: Spacing.one,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: Spacing.two,
  },
  rowLabel: {
    width: LabelColumnWidth,
  },
  remarkInput: {
    marginTop: Spacing.two,
  },
});
