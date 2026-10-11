import { useCallback, useEffect, useRef, useState } from 'react';
import { Image, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { Money } from '@aquarush/common';

import { Button } from '@/components/button';
import { Input } from '@/components/input';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { DialogFrame } from '@/components/ui';
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

      {/* 全屏商品选择器:内容自带背景铺满,遮罩透明仅拦截触摸;点外不关闭(与原 Modal 一致),靠完成按钮关闭 */}
      <DialogFrame
        visible={modalVisible}
        onClose={() => setModalVisible(false)}
        animation="slide"
        dismissOnOverlayPress={false}
        contentStyle={[StyleSheet.absoluteFill, styles.modalContainer]}>
        <ThemedView type="pageBackground" style={styles.modalFill}>
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
                <Pressable key={product.id} onPress={() => handleAdd(product)}>
                  <ThemedView type="card" style={styles.item}>
                    {renderProductItem(product)}
                    {/* 整行点击即加入明细(F-ORD-012 多选:弹窗保持打开可继续添加);不嵌套 Pressable,避免外层跳详情吞掉加购事件 */}
                    <ThemedText type="small" themeColor="primary" style={styles.addHint}>
                      +
                    </ThemedText>
                  </ThemedView>
                </Pressable>
              ))
            )}
          </ScrollView>
          <Button title="完成" variant="primary" onPress={() => setModalVisible(false)} />
        </ThemedView>
      </DialogFrame>
    </>
  );
}

const styles = StyleSheet.create({
  triggerCard: {
    borderRadius: Radius.lg,
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
    padding: Spacing.four,
    gap: Spacing.three,
  },
  modalFill: {
    flex: 1,
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
    borderRadius: Radius.lg,
    padding: Spacing.three,
    marginBottom: Spacing.two,
  },
  addHint: {
    fontSize: 22,
    marginLeft: 'auto',
  },
  emptyText: {
    textAlign: 'center',
    paddingVertical: Spacing.five,
  },
});
