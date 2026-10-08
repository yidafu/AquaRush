import { StyleSheet } from 'react-native';

import { Card } from '@/components/card';
import { ImageUploader } from '@/components/image-uploader';
import { Input } from '@/components/input';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';

interface DeliveryInfoCardProps {
  photos: string[];
  remark: string;
  onPhotosChange: (photos: string[]) => void;
  onRemarkChange: (remark: string) => void;
  disabled?: boolean;
}

export function DeliveryInfoCard({
  photos,
  remark,
  onPhotosChange,
  onRemarkChange,
  disabled = false,
}: DeliveryInfoCardProps) {
  return (
    <Card title="配送信息">
      <ImageUploader images={photos} onChange={onPhotosChange} disabled={disabled} label="配送照片" />

      <ThemedText type="smallBold" style={styles.remarkLabel}>
        备注
      </ThemedText>
      {disabled ? (
        <ThemedText type="small" themeColor="textSecondary">
          {remark || '无备注'}
        </ThemedText>
      ) : (
        <Input
          value={remark}
          onChangeText={onRemarkChange}
          placeholder="请输入配送备注（选填）"
          multiline
          maxLength={500}
        />
      )}
    </Card>
  );
}

const styles = StyleSheet.create({
  remarkLabel: {
    marginTop: Spacing.three,
    marginBottom: Spacing.one,
  },
});
