import { useCallback, useMemo, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { useRouter } from 'expo-router';
import { toast } from 'sonner-native';

import { AddressDisplay } from '@/components/address-display';
import { Button } from '@/components/button';
import { OrderSummaryCard, OrderLine, OrderInfo } from '@/components/order-summary-card';
import { ProductSelector, Product } from '@/components/product-selector';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { createOrder, CreateDeliveryOrderItem } from '@/services/delivery';
import type { AddressVo } from '@aquarush/common';

export default function CreateOrderScreen() {
  const router = useRouter();
  const theme = useTheme();

  const [selectedAddress, setSelectedAddress] = useState<AddressVo | null>(null);
  const [lines, setLines] = useState<OrderLine[]>([]);
  const [orderInfo, setOrderInfo] = useState<OrderInfo>({
    isSelfCollect: false,
    remark: '',
  });
  const [submitting, setSubmitting] = useState(false);

  const totalQuantity = useMemo(
    () => lines.reduce((sum, line) => sum + line.quantity, 0),
    [lines],
  );
  const canSubmit = Boolean(selectedAddress) && lines.length > 0 && lines.every((line) => line.quantity >= 1);

  /** 选择商品 → 加入明细(F-ORD-012 多商品:同商品合并数量) */
  const handleAddProduct = useCallback((product: Product) => {
    setLines((prev) => {
      const existing = prev.find((line) => line.product.id === product.id);
      if (existing) {
        return prev.map((line) =>
          line.product.id === product.id ? { ...line, quantity: line.quantity + 1 } : line,
        );
      }
      return [...prev, { product: { id: product.id, name: product.name, priceCents: product.priceCents }, quantity: 1 }];
    });
  }, []);

  const handleSubmit = async () => {
    if (!selectedAddress) {
      toast.warning('请选择地址');
      return;
    }
    if (lines.length === 0) {
      toast.warning('请选择商品');
      return;
    }
    if (lines.some((line) => line.quantity < 1)) {
      toast.warning('数量不能少于1');
      return;
    }

    setSubmitting(true);
    const toastId = toast.loading('创建中...');
    try {
      const items: CreateDeliveryOrderItem[] = lines.map((line) => ({
        productId: line.product.id,
        quantity: line.quantity,
      }));
      await createOrder(
        items,
        selectedAddress.id,
        orderInfo.isSelfCollect,
        orderInfo.remark || undefined,
      );
      toast.success('创建成功', { id: toastId });
      router.back();
    } catch (e: any) {
      toast.error(e?.message || '创建失败', { id: toastId });
    } finally {
      setSubmitting(false);
    }
  };

  const renderSectionTitle = (title: string) => (
    <View style={styles.sectionTitleRow}>
      <View style={[styles.sectionBar, { backgroundColor: theme.primary }]} />
      <ThemedText type="heading">{title}</ThemedText>
    </View>
  );

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        {renderSectionTitle('选择地址')}
        <AddressDisplay selectedId={selectedAddress?.id} onSelect={setSelectedAddress} />

        {renderSectionTitle('选择商品')}
        <ProductSelector addedCount={lines.length} onAdd={handleAddProduct} />

        <OrderSummaryCard
          lines={lines}
          onLinesChange={setLines}
          orderInfo={orderInfo}
          onOrderInfoChange={setOrderInfo}
        />

        <Button
          title={totalQuantity > 0 ? `创建送水订单（${totalQuantity}件）` : '创建送水订单'}
          size="lg"
          loading={submitting}
          disabled={!canSubmit}
          onPress={handleSubmit}
          style={styles.submit}
        />
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: Spacing.three,
    paddingBottom: Spacing.six,
  },
  sectionTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    marginBottom: Spacing.two,
    marginTop: Spacing.two,
  },
  sectionBar: {
    width: 4,
    height: 16,
    borderRadius: Radius.xs,
  },
  submit: {
    marginTop: Spacing.four,
  },
});
