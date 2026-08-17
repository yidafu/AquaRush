import { useCallback, useEffect, useRef, useState } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, View } from 'react-native';

import { Button } from '@/components/button';
import { Input } from '@/components/input';
import { StatusBadge } from '@/components/status-badge';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Radius, Shadow, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { searchAllAddresses } from '@/services/delivery';
import type { Address } from '@aquarush/common';

interface AddressDisplayProps {
  selectedId?: string;
  onSelect?: (address: Address) => void;
}

export function AddressDisplay({ selectedId, onSelect }: AddressDisplayProps) {
  const theme = useTheme();
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [filtered, setFiltered] = useState<Address[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [initialLoading, setInitialLoading] = useState(true);
  const [searchLoading, setSearchLoading] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    searchAllAddresses('', 20)
      .then((res) => {
        const list = [...(res?.searchAllAddresses ?? [])];
        setAddresses(list);
        setFiltered(list);
      })
      .catch((err) => console.error('Load addresses failed:', err))
      .finally(() => setInitialLoading(false));
  }, []);

  const performSearch = useCallback(
    (kw: string) => {
      if (!kw.trim()) {
        setFiltered(addresses);
        setSearchLoading(false);
        return;
      }
      setSearchLoading(true);
      // NOTE: NetworkManager.query returns the data object directly — the weapp read `res?.data?....` (bug).
      searchAllAddresses(kw, 20)
        .then((res) => setFiltered([...(res?.searchAllAddresses ?? [])]))
        .catch((err) => {
          console.error('Search addresses failed:', err);
          const lower = kw.toLowerCase();
          setFiltered(
            addresses.filter((a) =>
              [a.phone, a.receiverName, a.province, a.city, a.district, a.detailAddress].some((v) =>
                (v ?? '').toLowerCase().includes(lower),
              ),
            ),
          );
        })
        .finally(() => setSearchLoading(false));
    },
    [addresses],
  );

  useEffect(() => {
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => performSearch(keyword), 300);
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [keyword, performSearch]);

  const selectedAddress = addresses.find((a) => a.id === selectedId);

  const handleSelect = (addr: Address) => {
    onSelect?.(addr);
    setModalVisible(false);
    setKeyword('');
  };

  const renderAddressInfo = (addr: Address) => (
    <>
      <View style={styles.infoRow}>
        <ThemedText type="smallBold">{addr.receiverName}</ThemedText>
        <ThemedText type="small" themeColor="textSecondary">
          {addr.phone}
        </ThemedText>
        {addr.isDefault ? <StatusBadge label="默认" tone="processing" /> : null}
      </View>
      <ThemedText type="small">
        {addr.province}
        {addr.city}
        {addr.district}
        {addr.detailAddress}
      </ThemedText>
    </>
  );

  return (
    <>
      <Pressable onPress={() => setModalVisible(true)}>
        <ThemedView type="card" style={styles.triggerCard}>
          <View style={styles.cardContent}>
            {initialLoading ? (
              <ThemedText themeColor="textSecondary">加载中...</ThemedText>
            ) : selectedAddress ? (
              renderAddressInfo(selectedAddress)
            ) : (
              <ThemedText themeColor="textSecondary">点击选择地址</ThemedText>
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
            选择地址
          </ThemedText>
          <Input
            placeholder="搜索手机号、姓名或地址"
            value={keyword}
            onChangeText={setKeyword}
            autoCorrect={false}
          />
          <ScrollView style={styles.list}>
            {searchLoading ? (
              <ThemedText themeColor="textSecondary" style={styles.emptyText}>
                搜索中...
              </ThemedText>
            ) : filtered.length === 0 ? (
              <ThemedText themeColor="textSecondary" style={styles.emptyText}>
                {addresses.length === 0 ? '暂无地址' : '没有匹配的地址'}
              </ThemedText>
            ) : (
              filtered.map((addr) => {
                const selected = selectedId === addr.id;
                return (
                  <Pressable key={addr.id} onPress={() => handleSelect(addr)}>
                    <ThemedView
                      type={selected ? 'primaryBg' : 'card'}
                      style={[
                        styles.item,
                        selected && { borderWidth: 1, borderColor: theme.primary },
                      ]}>
                      {renderAddressInfo(addr)}
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
    gap: Spacing.one,
  },
  chevron: {
    fontSize: 24,
    lineHeight: 28,
  },
  infoRow: {
    flexDirection: 'row',
    gap: Spacing.two,
    alignItems: 'center',
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
    borderRadius: Radius.lg,
    padding: Spacing.three,
    marginBottom: Spacing.two,
    gap: Spacing.one,
  },
  emptyText: {
    textAlign: 'center',
    paddingVertical: Spacing.five,
  },
});
