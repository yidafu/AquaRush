import { useCallback, useEffect, useRef, useState } from 'react';
import { Image, Modal, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { router } from 'expo-router';
import { Money } from '@aquarush/common';

import { Button } from '@/components/button';
import { Input } from '@/components/input';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Radius, Shadow, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { searchProducts } from '@/services/delivery';
import { resolveAssetUrl } from '@/utils/url';

export interface Product {
  id: string;
  name: string;
  priceCents: number | string;
  coverImageUrl?: string | null;
  stock?: number;
  status?: string;
}

interface ProductSelectorProps {
  /** 已添加商品数(F-ORD-012 多商品,触发卡片展示) */
  addedCount: number;
  /** 选择商品 → 加入代客下单明细(同商品由父级合并数量,弹窗保持打开可继续添加) */
  onAdd: (product: Product) => void;
}

export function ProductSelector({ addedCount, onAdd }: ProductSelectorProps) {
  const theme = useTheme();
  const [modalVisible, setModalVisible] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [products, setProducts] = useState<Product[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [initialLoadDone, setInitialLoadDone] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const loadProducts = useCallback((kw: string) => {
    setSearchLoading(true);
    searchProducts(kw, 20)
      .then((res) => {
        const list = [...(res?.activeProducts?.list ?? [])];
        setProducts(list);
        setInitialLoadDone(true);
      })
      .catch((err) => console.error('Load products failed:', err))
      .finally(() => setSearchLoading(false));
  }, []);

  useEffect(() => {
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => loadProducts(keyword), 300);
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [keyword, loadProducts]);

  const handleOpen = () => {
    setModalVisible(true);
    if (!initialLoadDone) {
      loadProducts('');
    }
  };

  const handleAdd = (product: Product) => {
    onAdd?.(product);
    // 多选添加:弹窗保持打开,可继续添加其他商品
    setKeyword('');
  };

  const renderProductItem = (product: Product) => (
    <View style={styles.row}>
      {product.coverImageUrl ? (
        <Image
          source={{ uri: resolveAssetUrl(product.coverImageUrl) }}
          style={[styles.thumb, { backgroundColor: theme.backgroundElement }]}
        />
      ) : null}
      <View style={styles.info}>
        <ThemedText type="smallBold">{product.name}</ThemedText>
        <ThemedText type="small" themeColor="primary">
          <Money cents={product.priceCents} />
        </ThemedText>
        <ThemedText type="small" themeColor="textSecondary">
          库存: {product.stock ?? 0}
        </ThemedText>
      </View>
    </View>
  );

  return (
    <>
      <Pressable onPress={handleOpen}>
        <ThemedView type="card" style={styles.triggerCard}>
          <View style={styles.cardContent}>
            <ThemedText type="smallBold">
              {addedCount > 0 ? `已选 ${addedCount} 种商品（点击继续添加）` : '点击添加商品'}
            </ThemedText>
          </View>
          <ThemedText themeColor="textTertiary" style={styles.chevron}>
            ›
          </ThemedText>
        </ThemedView>
      </Pressable>

      <Modal visible={modalVisible} transparent animationType="slide" onRequestClose={() => setModalVisible(false)}>
        <ThemedView type="pageBackground" style={styles.modalContainer}>
          <ThemedText type="heading" style={styles.modalTitle}>
            选择商品（可多选）
          </ThemedText>
          <Input
            placeholder="搜索商品名称"
            value={keyword}
            onChangeText={setKeyword}
            autoCorrect={false}
          />
          <ScrollView style={styles.list}>
            {searchLoading && products.length === 0 ? (
              <ThemedText themeColor="textSecondary" style={styles.emptyText}>
                加载中...
              </ThemedText>
            ) : products.length === 0 ? (
              <ThemedText themeColor="textSecondary" style={styles.emptyText}>
                {initialLoadDone ? '暂无可用商品' : '没有匹配的商品'}
              </ThemedText>
            ) : (
              products.map((product) => (
                <Pressable key={product.id} onPress={() => router.push(`/product/${product.id}`)}>
                  <ThemedView type="card" style={styles.item}>
                    {renderProductItem(product)}
                    {/* 点 + 直接加购(与进详情拆分,避免误触) */}
                    <Pressable onPress={() => handleAdd(product)} hitSlop={12} style={styles.addHit}>
                      <ThemedText type="small" themeColor="primary" style={styles.addHint}>
                        +
                      </ThemedText>
                    </Pressable>
                  </ThemedView>
                </Pressable>
              ))
            )}
          </ScrollView>
          <Button title="完成" variant="primary" onPress={() => setModalVisible(false)} />
        </ThemedView>
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  triggerCard: {
    borderRadius: Radius.md,
    padding: Spacing.three,
    marginBottom: Spacing.two,
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    ...Shadow.md,
  },
  cardContent: {
    flex: 1,
  },
  chevron: {
    fontSize: 24,
    lineHeight: 28,
  },
  row: {
    flexDirection: 'row',
    gap: Spacing.three,
    alignItems: 'center',
  },
  thumb: {
    width: 56,
    height: 56,
    borderRadius: Radius.sm,
  },
  info: {
    flex: 1,
    gap: Spacing.half,
  },
  modalContainer: {
    flex: 1,
    padding: Spacing.four,
    gap: Spacing.three,
  },
  modalTitle: {
    textAlign: 'center',
  },
  list: {
    flex: 1,
  },
  item: {
    flexDirection: 'row',
    gap: Spacing.three,
    alignItems: 'center',
    borderRadius: Radius.md,
    padding: Spacing.three,
    marginBottom: Spacing.two,
  },
  addHint: {
    fontSize: 22,
    marginLeft: 'auto',
  },
  addHit: {
    padding: Spacing.two,
  },
  emptyText: {
    textAlign: 'center',
    paddingVertical: Spacing.five,
  },
});
