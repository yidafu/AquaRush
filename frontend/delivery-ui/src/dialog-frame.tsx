import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Animated, Platform, StyleSheet, useWindowDimensions, type StyleProp, type ViewStyle } from 'react-native';

import { Content, Overlay, Portal, Root } from '@rn-primitives/dialog';

const ENTER_DURATION_MS = 250;
const EXIT_DURATION_MS = 200;

/** web 侧阻止 radix「点击外部关闭」（全屏弹层进出场期间由遮罩接管点击） */
interface WebDismissGuard {
  onInteractOutside?: (event: { preventDefault: () => void }) => void;
}

export interface DialogFrameProps {
  /** 父级控制的可见性；关闭时先播退场动画再卸载 */
  visible: boolean;
  /** 请求关闭（遮罩点击 / 返回键 / Esc 均汇聚到此） */
  onClose: () => void;
  /** 进出场动画：slide 自底滑入（弹层/选择器）/ fade 淡入（海报、预览） */
  animation?: 'slide' | 'fade';
  /** 遮罩颜色，默认透明（仅拦截触摸；全屏内容自带背景时使用） */
  overlayColor?: string;
  /** 点击遮罩关闭；全屏选择器进出场期间设 false，仅拦截触摸不关闭 */
  dismissOnOverlayPress?: boolean;
  /** 内容动画层（绝对铺满）附加布局样式，如居中、留白 */
  containerStyle?: StyleProp<ViewStyle>;
  /** Dialog.Content 自身样式：定位、背景、圆角 */
  contentStyle?: StyleProp<ViewStyle>;
  children: ReactNode;
}

/**
 * 弹层装载框：@rn-primitives/dialog 之上补齐进出场动画、遮罩与挂载生命周期。
 * 无品牌样式——颜色与排版由样式层按 docs/ui-spec.md 注入。
 * 返回 null 时不挂载任何 Dialog 节点，返回键监听仅在打开期间生效。
 */
export function DialogFrame({
  visible,
  onClose,
  animation = 'fade',
  overlayColor = 'transparent',
  dismissOnOverlayPress = true,
  containerStyle,
  contentStyle,
  children,
}: DialogFrameProps) {
  const { height } = useWindowDimensions();
  const [mounted, setMounted] = useState(visible);
  const mountedRef = useRef(visible);
  // Animated.Value 放 state 惰性初始化：渲染期不读 ref.current（react-hooks/refs）
  const [progress] = useState(() => new Animated.Value(0));

  // 进入：render 期条件挂载（React 状态调整模式），effect 只驱动动画，避免 effect 内同步 setState
  if (visible && !mounted) {
    setMounted(true);
  }

  useEffect(() => {
    if (visible) {
      mountedRef.current = true;
      progress.stopAnimation();
      Animated.timing(progress, {
        toValue: 1,
        duration: ENTER_DURATION_MS,
        useNativeDriver: Platform.OS !== 'web',
      }).start();
    } else if (mountedRef.current) {
      progress.stopAnimation();
      Animated.timing(progress, {
        toValue: 0,
        duration: EXIT_DURATION_MS,
        useNativeDriver: Platform.OS !== 'web',
      }).start(({ finished }) => {
        if (finished) {
          mountedRef.current = false;
          setMounted(false);
        }
      });
    }
    return () => {
      progress.stopAnimation();
    };
  }, [visible, progress]);

  if (!mounted) {
    return null;
  }

  const contentAnimation: StyleProp<ViewStyle> =
    animation === 'slide'
      ? { transform: [{ translateY: progress.interpolate({ inputRange: [0, 1], outputRange: [height, 0] }) }] }
      : { opacity: progress };
  const overlayNativeProps = Platform.OS !== 'web' ? { closeOnPress: dismissOnOverlayPress } : {};
  const webDismissGuard: WebDismissGuard =
    Platform.OS === 'web' && !dismissOnOverlayPress
      ? { onInteractOutside: (event) => event.preventDefault() }
      : {};

  return (
    <Root open onOpenChange={(open) => { if (!open && visible) onClose(); }}>
      <Portal>
        {/* 遮罩：常驻拦截进出场期间的穿透点击；opacity 随进出场淡入淡出 */}
        <Animated.View pointerEvents="box-none" style={[StyleSheet.absoluteFill, { opacity: progress }]}>
          <Overlay
            {...overlayNativeProps}
            style={[StyleSheet.absoluteFill, { backgroundColor: overlayColor }]}
          />
        </Animated.View>
        {/* 内容层：box-none 使遮罩区域的点击落到 Overlay；退场期间禁用交互 */}
        <Animated.View
          pointerEvents={visible ? 'box-none' : 'none'}
          style={[StyleSheet.absoluteFill, containerStyle, contentAnimation]}>
          <Content {...webDismissGuard} style={contentStyle}>
            {children}
          </Content>
        </Animated.View>
      </Portal>
    </Root>
  );
}
