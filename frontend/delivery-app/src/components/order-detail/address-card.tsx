import { StyleSheet } from 'react-native';

import { Card } from '@/components/card';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import type { Address } from '@aquarush/common';

interface AddressCardProps {
  address?: Address;
}

export function AddressCard({ address }: AddressCardProps) {
  if (!address) return null;

  return (
    <Card title="收货地址">
      <ThemedView style={styles.row}>
        <ThemedText type="smallBold">{address.receiverName}</ThemedText>
        <ThemedText type="small" themeColor="textSecondary">
          {address.phone}
        </ThemedText>
      </ThemedView>
      <ThemedText type="small">
        {address.province}
        {address.city}
        {address.district}
        {address.detailAddress}
      </ThemedText>
    </Card>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: Spacing.one,
  },
});
