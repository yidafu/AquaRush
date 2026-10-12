import type { ReactNode } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ThemedText } from '@/components/themed-text';
import { DialogFrame } from '@aquarush/delivery-ui';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

interface BottomSheetProps {
  visible: boolean;
  onClose: () => void;
  title?: string;
  children: ReactNode;
}

/**
 * 底部弹层：overlay 遮罩 + card 面板顶圆角 24 + 拖拽条，可滚动。
 * 基于 ui/DialogFrame（@rn-primitives/dialog），见 docs/ui-spec.md §5。
 */
export function BottomSheet({ visible, onClose, title, children }: BottomSheetProps) {
  const theme = useTheme();
  const insets = useSafeAreaInsets();

  return (
    <DialogFrame
      visible={visible}
      onClose={onClose}
      animation="slide"
      overlayColor={theme.overlay}
      containerStyle={styles.container}
      contentStyle={[
        styles.panel,
        { backgroundColor: theme.card, paddingBottom: insets.bottom + Spacing.three },
      ]}>
      <View style={[styles.handle, { backgroundColor: theme.borderStrong }]} />
      {title ? (
        <ThemedText type="heading" style={styles.title}>
          {title}
        </ThemedText>
      ) : null}
      <ScrollView bounces={false}>{children}</ScrollView>
    </DialogFrame>
  );
}

const styles = StyleSheet.create({
  container: {
    justifyContent: 'flex-end',
  },
  panel: {
    width: '100%',
    maxHeight: '80%',
    borderTopLeftRadius: Radius.xl,
    borderTopRightRadius: Radius.xl,
    paddingTop: Spacing.two,
    paddingHorizontal: Spacing.four,
  },
  handle: {
    alignSelf: 'center',
    width: 40,
    height: 3,
    borderRadius: Radius.pill,
    marginBottom: Spacing.three,
  },
  title: {
    textAlign: 'center',
    marginBottom: Spacing.three,
  },
});
