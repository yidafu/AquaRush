import { useCallback, useEffect, useRef, useState } from 'react';
import { Image, Modal, Pressable, ScrollView, StyleSheet, View } from 'react-native';
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
  selectedProductId?: string;
  onSelect: (product: Product) => void;
}

export function ProductSelector({ selectedProductId, onSelect }: ProductSelectorProps) {
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

  const selectedProduct = products.find((p) => p.id === selectedProductId);

  const handleSelect = (product: Product) => {
    onSelect?.(product);
    setModalVisible(false);
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
            {selectedProductId && selectedProduct ? (
              renderProductItem(selectedProduct)
            ) : (
              <ThemedText themeColor="textSecondary">点击选择商品</ThemedText>
            )}
          </View>
          <ThemedText themeColor="textTertiary" style={styles.chevron}>
            ›
          </ThemedText>
        </ThemedView>
      </Pressable>

      <Modal visible={modalVisible} transparent animationType="slide" onRequestClose={() => setModalVisible(false)}>
        <ThemedView type="pageBackground" style={styles.modalContainer}>
          <ThemedText type="heading" style={styles.modalTitle}>
            选择商品
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
              products.map((product) => {
                const selected = selectedProductId === product.id;
                return (
                  <Pressable key={product.id} onPress={() => handleSelect(product)}>
                    <ThemedView
                      type={selected ? 'primaryBg' : 'card'}
                      style={[
                        styles.item,
                        selected && { borderWidth: 1, borderColor: theme.primary },
                      ]}>
                      {renderProductItem(product)}
                    </ThemedView>
                  </Pressable>
                );
              })
            )}
          </ScrollView>
          <Button title="取消" variant="ghost" onPress={() => setModalVisible(false)} />
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
  emptyText: {
    textAlign: 'center',
    paddingVertical: Spacing.five,
  },
});
