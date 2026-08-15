import { Image, StyleSheet } from 'react-native';
import { Money } from '@aquarush/common';

import { Card } from '@/components/card';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { resolveAssetUrl } from '@/utils/url';

interface ProductCardProps {
  /** 商品明细(F-ORD-004 多商品下单) */
  items?: ReadonlyArray<{
    productId?: string | number;
    quantity?: number;
    unitPriceCents?: number | string;
    product?: {
      id?: string;
      name?: string;
      priceCents?: number | string;
      coverImageUrl?: string | null;
      specification?: string | null;
    };
  }>;
  remark?: string;
}

export function ProductCard({ items, remark }: ProductCardProps) {
  if (!items || items.length === 0) return null;

  return (
    <Card title="商品详情">
      {items.map((item, index) => (
        <ThemedView key={String(item.productId || index)} style={[styles.productRow, index < items.length - 1 ? styles.rowGap : null]}>
          {item.product?.coverImageUrl ? (
            <Image source={{ uri: resolveAssetUrl(item.product.coverImageUrl) }} style={styles.image} />
          ) : null}
          <ThemedView style={styles.details}>
            <ThemedText type="smallBold">{item.product?.name || '商品'}</ThemedText>
            <ThemedText type="small" themeColor="primary">
              <Money cents={item.unitPriceCents ?? item.product?.priceCents} /> x {item.quantity}
            </ThemedText>
          </ThemedView>
        </ThemedView>
      ))}
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
  rowGap: {
    marginBottom: Spacing.three,
  },
  details: {
    flex: 1,
    gap: Spacing.one,
  },
  remark: {
    marginTop: Spacing.two,
  },
});