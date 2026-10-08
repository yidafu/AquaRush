/**
 * Below are the colors that are used in the app. The colors are defined in the light and dark mode.
 * There are many other ways to style your app. For example, [Nativewind](https://www.nativewind.dev/), [Tamagui](https://tamagui.dev/), [unistyles](https://reactnativeunistyles.vercel.app), etc.
 *
 * 设计规范见 docs/ui-spec.md —— 本文件是色彩/排版/圆角/阴影 token 的唯一事实来源。
 * 注意：新增 token 必须在 light 与 dark 两套中都存在（ThemeColor 取二者交集）。
 */

import '@/global.css';

import { Platform } from 'react-native';

import { STATUS_TONE_COLORS } from '@aquarush/common/utils/status-tone';

export const Colors = {
  light: {
    text: '#1A1A1A',
    background: '#ffffff',
    backgroundElement: '#F0F0F3',
    backgroundSelected: '#E0E1E6',
    textSecondary: '#8C8C8C',
    textTertiary: '#B8B8B8',
    primary: '#3A9BD9',
    primaryDark: '#2E7FB8',
    primaryBg: 'rgba(58,155,217,0.10)',
    accent: '#FA8C16',
    accentBg: 'rgba(250,140,22,0.10)',
    success: '#52c41a',
    warning: '#faad14',
    warningBg: 'rgba(250,173,20,0.10)',
    danger: '#ff4d4f',
    border: '#f0f0f0',
    borderStrong: '#d9d9d9',
    pageBackground: '#F5F7FA',
    card: '#ffffff',
    overlay: 'rgba(0,0,0,0.4)',
    overlayStrong: 'rgba(0,0,0,0.9)',
    headerGradientStart: '#4DB6E5',
    headerGradientEnd: '#3A9BD9',
    headerText: '#ffffff',
    headerTextSecondary: 'rgba(255,255,255,0.85)',
    headerBadgeBg: 'rgba(255,255,255,0.22)',
    shadowColor: '#000000',
  },
  dark: {
    text: '#EAEAEA',
    background: '#16191D',
    backgroundElement: '#212225',
    backgroundSelected: '#2E3135',
    textSecondary: '#A6ACB4',
    textTertiary: '#80868E',
    primary: '#4DB6E5',
    primaryDark: '#6FC3EC',
    primaryBg: 'rgba(77,182,229,0.16)',
    accent: '#FA8C16',
    accentBg: 'rgba(250,140,22,0.16)',
    success: '#52c41a',
    warning: '#faad14',
    warningBg: 'rgba(250,173,20,0.16)',
    danger: '#ff4d4f',
    border: '#26282c',
    borderStrong: '#3a3d42',
    pageBackground: '#101418',
    card: '#1c1e21',
    overlay: 'rgba(0,0,0,0.6)',
    overlayStrong: 'rgba(0,0,0,0.95)',
    headerGradientStart: '#3A9BD9',
    headerGradientEnd: '#2E7FB8',
    headerText: '#ffffff',
    headerTextSecondary: 'rgba(255,255,255,0.85)',
    headerBadgeBg: 'rgba(255,255,255,0.22)',
    shadowColor: '#000000',
  },
} as const;

export type ThemeColor = keyof typeof Colors.light & keyof typeof Colors.dark;

/** 订单状态标签的 tint 配色(亮暗区分,保证暗色下可读)。见 docs/ui-spec.md §2。
 * light 直接引用 common STATUS_TONE_COLORS(§2.2 的 10% tint 权威值);
 * dark 保留本地适配(16% tint + 亮色文字),tone 语义与 common 对齐。 */
export const StatusTints = {
  light: {
    info: STATUS_TONE_COLORS.info,
    success: STATUS_TONE_COLORS.success,
    warning: STATUS_TONE_COLORS.warning,
    error: STATUS_TONE_COLORS.error,
    neutral: STATUS_TONE_COLORS.neutral,
    accent: STATUS_TONE_COLORS.accent,
  },
  dark: {
    info: { text: '#6FC3EC', bg: 'rgba(77,182,229,0.16)' },
    success: { text: '#95de64', bg: 'rgba(82,196,26,0.16)' },
    warning: { text: '#ffc53d', bg: 'rgba(250,173,20,0.16)' },
    error: { text: '#ff7875', bg: 'rgba(255,77,79,0.16)' },
    neutral: { text: '#A6ACB4', bg: 'rgba(140,140,140,0.16)' },
    accent: { text: '#FA8C16', bg: 'rgba(250,140,22,0.16)' },
  },
} as const;

export type StatusTone = keyof typeof StatusTints.light & keyof typeof StatusTints.dark;

/** 信息行标签列宽(OrderInfoRow / OrderSummaryCard / CollectionInputFields 共享)。 */
export const LabelColumnWidth = 76;

export const Radius = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  pill: 999,
} as const;

export const Shadow = {
  sm: {
    shadowColor: '#000000',
    shadowOpacity: 0.05,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
  },
  md: {
    shadowColor: '#000000',
    shadowOpacity: 0.04,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 4 },
    elevation: 4,
  },
  lg: {
    shadowColor: '#000000',
    shadowOpacity: 0.18,
    shadowRadius: 24,
    shadowOffset: { width: 0, height: 8 },
    elevation: 8,
  },
  primaryGlow: {
    shadowColor: '#3A9BD9',
    shadowOpacity: 0.35,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 4 },
    elevation: 6,
  },
} as const;

/** 排版字阶。见 docs/ui-spec.md §3。 */
export const TypeScale = {
  title: { fontSize: 32, lineHeight: 40, fontWeight: '700' as const },
  subtitle: { fontSize: 22, lineHeight: 30, fontWeight: '600' as const },
  heading: { fontSize: 18, lineHeight: 26, fontWeight: '600' as const },
  body: { fontSize: 16, lineHeight: 24, fontWeight: '500' as const },
  small: { fontSize: 14, lineHeight: 20, fontWeight: '400' as const },
  caption: { fontSize: 12, lineHeight: 16, fontWeight: '400' as const },
} as const;

export const Fonts = Platform.select({
  ios: {
    /** iOS `UIFontDescriptorSystemDesignDefault` */
    sans: 'system-ui',
    /** iOS `UIFontDescriptorSystemDesignSerif` */
    serif: 'ui-serif',
    /** iOS `UIFontDescriptorSystemDesignRounded` */
    rounded: 'ui-rounded',
    /** iOS `UIFontDescriptorSystemDesignMonospaced` */
    mono: 'ui-monospace',
  },
  default: {
    sans: 'normal',
    serif: 'serif',
    rounded: 'normal',
    mono: 'monospace',
  },
  web: {
    sans: 'var(--font-display)',
    serif: 'var(--font-serif)',
    rounded: 'var(--font-rounded)',
    mono: 'var(--font-mono)',
  },
});

export const Spacing = {
  half: 2,
  one: 4,
  two: 8,
  three: 16,
  four: 24,
  five: 32,
  six: 64,
} as const;

export const MaxContentWidth = 800;
