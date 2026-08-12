import { useState } from 'react';
import { Alert, ScrollView, StyleSheet, View } from 'react-native';
import { useRouter } from 'expo-router';

import { AddressDisplay } from '@/components/address-display';
import { Button } from '@/components/button';
import { OrderSummaryCard, OrderInfo } from '@/components/order-summary-card';
import { ProductSelector, Product } from '@/components/product-selector';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { createOrder } from '@/services/delivery';
import type { Address } from '@aquarush/common';

export default function CreateOrderScreen() {
  const router = useRouter();
  const theme = useTheme();

  const [selectedAddress, setSelectedAddress] = useState<Address | null>(null);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [orderInfo, setOrderInfo] = useState<OrderInfo>({
    quantity: 1,
    isSelfCollect: false,
    remark: '',
  });
  const [submitting, setSubmitting] = useState(false);

  const canSubmit = Boolean(selectedAddress && selectedProduct && orderInfo.quantity >= 1);

  const handleSubmit = async () => {
    if (!selectedAddress) {
      Alert.alert('请选择地址');
      return;
    }
    if (!selectedProduct) {
      Alert.alert('请选择商品');
      return;
    }
    if (orderInfo.quantity < 1) {
      Alert.alert('数量不能少于1');
      return;
    }

    setSubmitting(true);
    try {
      await createOrder(
        selectedProduct.id,
        selectedAddress.id,
        orderInfo.quantity,
        orderInfo.isSelfCollect,
        orderInfo.remark || undefined,
      );
      Alert.alert('创建成功');
      router.back();
    } catch (e: any) {
      Alert.alert(e?.message || '创建失败');
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
        <ProductSelector selectedProductId={selectedProduct?.id} onSelect={setSelectedProduct} />

        <OrderSummaryCard product={selectedProduct} orderInfo={orderInfo} onOrderInfoChange={setOrderInfo} />

        <Button
          title="创建送水订单"
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
