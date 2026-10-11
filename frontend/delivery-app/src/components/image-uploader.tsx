import { useState } from 'react';
import { Image, Pressable, ScrollView, StyleSheet } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { toast } from 'sonner-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { DialogFrame } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { resolveAssetUrl } from '@/utils/url';
import { uploadPhoto } from '@/utils/upload';

interface ImageUploaderProps {
  images: string[];
  onChange: (images: string[]) => void;
  max?: number;
  disabled?: boolean;
  label?: string;
}

const DEFAULT_MAX = 9;

export function ImageUploader({
  images,
  onChange,
  max = DEFAULT_MAX,
  disabled = false,
  label = '',
}: ImageUploaderProps) {
  const theme = useTheme();
  const [uploading, setUploading] = useState(false);
  const [previewIndex, setPreviewIndex] = useState<number | null>(null);

  const pickAndUpload = async (source: 'camera' | 'album') => {
    if (images.length >= max) {
      toast.warning(`最多上传${max}张图片`);
      return;
    }

    if (source === 'camera') {
      const perm = await ImagePicker.requestCameraPermissionsAsync();
      if (!perm.granted) {
        toast.warning('需要相机权限才能拍摄配送照片');
        return;
      }
    } else {
      const perm = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (!perm.granted) {
        toast.warning('需要相册权限才能选择配送照片');
        return;
      }
    }

    const remaining = max - images.length;
    const result =
      source === 'camera'
        ? await ImagePicker.launchCameraAsync({ quality: 0.8, mediaTypes: ['images'] })
        : await ImagePicker.launchImageLibraryAsync({
            quality: 0.8,
            mediaTypes: ['images'],
            allowsMultipleSelection: true,
            selectionLimit: remaining,
          });

    if (result.canceled || !result.assets?.length) return;

    setUploading(true);
    try {
      const uploaded: string[] = [];
      for (const asset of result.assets) {
        const name = asset.fileName ?? `photo-${Date.now()}.jpg`;
        const mime = asset.mimeType ?? 'image/jpeg';
        const url = await uploadPhoto(asset.uri, name, mime);
        uploaded.push(url);
      }
      onChange([...images, ...uploaded]);
    } catch (e: any) {
      toast.error(e?.message || '图片上传失败');
    } finally {
      setUploading(false);
    }
  };

  return (
    <ThemedView style={styles.container}>
      {label ? (
        <ThemedText type="smallBold" style={styles.label}>
          {label}（{images.length}/{max}）
        </ThemedText>
      ) : null}

      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.grid}>
        {images.map((photo, index) => (
          <Pressable key={index} onPress={() => setPreviewIndex(index)}>
            <Image source={{ uri: resolveAssetUrl(photo) }} style={styles.photo} />
            {!disabled ? (
              <Pressable
                style={[styles.deleteBtn, { backgroundColor: theme.overlay }]}
                hitSlop={8}
                onPress={() => onChange(images.filter((_, i) => i !== index))}>
                <ThemedText style={[styles.deleteIcon, { color: theme.headerText }]}>×</ThemedText>
              </Pressable>
            ) : null}
          </Pressable>
        ))}

        {images.length < max && !disabled ? (
          <ThemedView style={styles.addArea}>
            <Pressable
              style={[styles.addBtn, { borderColor: theme.borderStrong }]}
              disabled={uploading}
              onPress={() => pickAndUpload('album')}>
              <ThemedText style={[styles.addIcon, { color: theme.textTertiary }]}>
                {uploading ? '…' : '+'}
              </ThemedText>
            </Pressable>
            <ThemedText type="small" themeColor="textSecondary">
              拍照/相册
            </ThemedText>
            {images.length === 0 ? (
              <Pressable style={styles.cameraLink} onPress={() => pickAndUpload('camera')}>
                <ThemedText type="small" themeColor="primary">
                  拍照
                </ThemedText>
              </Pressable>
            ) : null}
          </ThemedView>
        ) : null}
      </ScrollView>

      {/* 图片预览:淡入全屏,遮罩点外即关(承接原 Pressable 关闭行为) */}
      <DialogFrame
        visible={previewIndex !== null}
        onClose={() => setPreviewIndex(null)}
        animation="fade"
        overlayColor={theme.overlayStrong}
        contentStyle={StyleSheet.absoluteFill}>
        <Pressable style={styles.previewOverlay} onPress={() => setPreviewIndex(null)}>
          {previewIndex !== null ? (
            <Image
              source={{ uri: resolveAssetUrl(images[previewIndex]) }}
              style={styles.previewImage}
              resizeMode="contain"
            />
          ) : null}
          <ThemedText themeColor="textSecondary" style={styles.previewHint}>
            点击关闭
          </ThemedText>
        </Pressable>
      </DialogFrame>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: Spacing.two,
  },
  label: {
    marginBottom: Spacing.one,
  },
  grid: {
    flexDirection: 'row',
    gap: Spacing.two,
    alignItems: 'center',
  },
  photo: {
    width: 72,
    height: 72,
    borderRadius: Radius.sm,
  },
  deleteBtn: {
    position: 'absolute',
    top: -6,
    right: -6,
    width: 20,
    height: 20,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  deleteIcon: {
    fontSize: 14,
    lineHeight: 16,
  },
  addArea: {
    alignItems: 'center',
    gap: Spacing.half,
  },
  addBtn: {
    width: 72,
    height: 72,
    borderRadius: Radius.sm,
    borderWidth: 1,
    borderStyle: 'dashed',
    alignItems: 'center',
    justifyContent: 'center',
  },
  addIcon: {
    fontSize: 28,
  },
  cameraLink: {
    marginTop: Spacing.half,
  },
  previewOverlay: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },  previewImage: {
    width: '100%',
    height: '80%',
  },
  previewHint: {
    marginTop: Spacing.three,
  },
});
