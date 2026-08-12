import { Image, StyleSheet } from 'react-native';
import { Money } from '@aquarush/common';

import { Card } from '@/components/card';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { resolveAssetUrl } from '@/utils/url';

interface ProductCardProps {
  product?: {
    id?: string;
    name?: string;
    priceCents?: number | string;
    coverImageUrl?: string | null;
  };
  quantity: number;
  remark?: string;
}

export function ProductCard({ product, quantity, remark }: ProductCardProps) {
  if (!product) return null;

  return (
    <Card title="商品详情">
      <ThemedView style={styles.productRow}>
        {product.coverImageUrl ? (
          <Image source={{ uri: resolveAssetUrl(product.coverImageUrl) }} style={styles.image} />
        ) : null}
        <ThemedView style={styles.details}>
          <ThemedText type="smallBold">{product.name}</ThemedText>
          <ThemedText type="small" themeColor="primary">
            <Money cents={product.priceCents} /> x {quantity}
          </ThemedText>
        </ThemedView>
      </ThemedView>
      {remark ? (
        <ThemedText type="small" themeColor="textSecondary" style={styles.remark}>
          备注：{remark}
        </ThemedText>
      ) : null}
    </Card>
  );
}

const styles = StyleSheet.create({
  productRow: {
    flexDirection: 'row',
    gap: Spacing.three,
    alignItems: 'center',
  },
  image: {
    width: 64,
    height: 64,
    borderRadius: Spacing.two,
  },
  details: {
    flex: 1,
    gap: Spacing.one,
  },
  remark: {
    marginTop: Spacing.two,
  },
});
