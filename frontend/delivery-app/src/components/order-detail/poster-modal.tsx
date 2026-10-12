import { Platform, Pressable, StyleSheet, View } from 'react-native';
import type { RefObject } from 'react';

import { Button } from '@/components/button';
import { PosterView, toPosterOrderData } from '@/components/order-poster';
import { ThemedText } from '@/components/themed-text';
import { DialogFrame } from '@aquarush/delivery-ui';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import type { OrderVo } from '@aquarush/common';

interface PosterModalProps {
  visible: boolean;
  onClose: () => void;
  order: OrderVo;
  wechatConfigured: boolean;
  sharing: boolean;
  /** 海报 View 引用(由父组件持有,供 capturePosterView 截图) */
  posterViewRef: RefObject<View | null>;
  onShareToWechat: () => void;
  onSystemShare: () => void;
}

/**
 * 分享订单海报 Modal:海报预览(固定白卡) + 微信分享(配置时)/系统分享按钮
 */
export function PosterModal({
  visible,
  onClose,
  order,
  wechatConfigured,
  sharing,
  posterViewRef,
  onShareToWechat,
  onSystemShare,
}: PosterModalProps) {
  const theme = useTheme();

  return (
    <DialogFrame
      visible={visible}
      onClose={onClose}
      animation="fade"
      overlayColor={theme.overlayStrong}
      containerStyle={styles.backdrop}
      contentStyle={styles.body}>
      <ThemedText type="heading" themeColor="headerText" style={styles.title}>
        分享订单海报
      </ThemedText>
      <View style={styles.posterCard} ref={posterViewRef}>
        <PosterView order={toPosterOrderData(order)} />
      </View>
      <View style={styles.actions}>
        {wechatConfigured ? (
          <Button
            title="分享到微信"
            size="md"
            loading={sharing}
            onPress={onShareToWechat}
            style={styles.button}
          />
        ) : null}
        {Platform.OS !== 'web' ? (
          <Button
            title="系统分享"
            size="md"
            variant="ghost"
            loading={sharing}
            onPress={onSystemShare}
            style={styles.button}
          />
        ) : null}
      </View>
      <Pressable onPress={onClose} style={styles.close}>
        <ThemedText type="small" themeColor="headerTextSecondary">
          关闭
        </ThemedText>
      </Pressable>
    </DialogFrame>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: Spacing.four,
  },
  body: {
    width: '100%',
    maxWidth: 420,
    alignItems: 'center',
  },
  title: {
    marginBottom: Spacing.three,
  },
  posterCard: {
    // 海报预览白底：分享产物固定白卡（暗色下也必须白底），刻意保留固定色
    borderRadius: Radius.lg,
    backgroundColor: '#ffffff',
    padding: Spacing.three,
    marginBottom: Spacing.four,
  },
  actions: {
    flexDirection: 'row',
    gap: Spacing.two,
    width: '100%',
  },
  button: {
    flex: 1,
  },
  close: {
    marginTop: Spacing.four,
    padding: Spacing.two,
  },
});
