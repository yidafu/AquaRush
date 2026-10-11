/**
 * user-weapp 端主题(StyleX createTheme)+ 共享工具样式
 *
 * weapp 无 :root(自定义属性按 WeUI 约定挂 page 选择器),而 stylex 的
 * defineVars 默认值挂在 :root 上,在 weapp 不可靠;因此页面用到的全部 token
 * 必须经 createTheme 覆盖,并把 pageTheme 挂到每个页面根元素:
 *   <View {...stylex.props(pageTheme, styles.page)}>
 *
 * 覆盖规则(保持与现有 750 设计稿基准渲染一致;designWidth=750 下
 * 源码 1px=1rpx=375 视口 0.5px):
 * - 颜色 token 与 web 基准一致,页面底按 §2.3 用微信官方灰 #EDEDED;
 * - 长度 token 覆盖为 rpx,数字与 web 基准值相同(web 12px ↔ weapp 12rpx)。
 * - fs、lh 系列 token 与 font、fontNum 仅为 admin(web)准备:
 *   weapp 页面尺寸一律 rpx 字面量,禁止引用这些 token。
 */
import * as stylex from '@stylexjs/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'

export const pageTheme = stylex.createTheme(tokens, {
  // —— 品牌色板 ——
  brand: '#3A9BD9',
  brandLight: '#4DB6E5',
  brandDark: '#2E7FB8',
  brandTint: 'rgba(58, 155, 217, 0.10)',
  brandTintStrong: 'rgba(58, 155, 217, 0.18)',
  brandGradient: 'linear-gradient(135deg, #4DB6E5 0%, #3A9BD9 100%)',

  // —— 强调色(仅消费者端最高优先级 CTA + 优惠券)——
  action: '#FF5436',
  actionShadow: '0 2rpx 8rpx rgba(255, 84, 54, 0.30)',
  actionTint: 'rgba(255, 84, 54, 0.10)',

  // —— 金额红 ——
  price: '#FA5151',
  priceLight: '#FF7A7E',

  // —— 基底 / 文字(页面底 = 微信官方灰,§2.3)——
  bg: '#EDEDED',
  card: '#FFFFFF',
  text: '#1A1A1A',
  textSub: '#8C8C8C',
  textWeak: '#B8B8B8',
  border: 'rgba(0, 0, 0, 0.06)',
  borderStrong: 'rgba(0, 0, 0, 0.12)',
  onBrand: '#FFFFFF',

  // —— 语义色 ——
  success: '#52C41A',
  warning: '#FAAD14',
  error: '#FF4D4F',

  // —— 间距刻度(rpx,数字同 web 基准)——
  s1: '4rpx',
  s2: '8rpx',
  s3: '12rpx',
  s4: '16rpx',
  s5: '24rpx',

  // —— 圆角 ——
  rCard: '16rpx',
  rBtn: '24rpx',
  rSm: '8rpx',

  // —— 阴影 ——
  shCard: '0 4rpx 16rpx rgba(0, 0, 0, 0.04)',
  shBtn: '0 2rpx 8rpx rgba(255, 84, 54, 0.30)',
  shModal: '0 8rpx 32rpx rgba(0, 0, 0, 0.12)',

  // —— 状态标签(色对同 STATUS_TONE_COLORS)——
  statusInfoFg: '#3A9BD9',
  statusInfoBg: 'rgba(58, 155, 217, 0.10)',
  statusSuccessFg: '#389E0D',
  statusSuccessBg: 'rgba(82, 196, 26, 0.10)',
  statusWarningFg: '#FA8C16',
  statusWarningBg: 'rgba(250, 173, 20, 0.10)',
  statusNeutralFg: '#8C8C8C',
  statusNeutralBg: 'rgba(140, 140, 140, 0.10)',
  statusErrorFg: '#CF1322',
  statusErrorBg: 'rgba(255, 77, 79, 0.10)',
  statusAccentFg: '#FF5436',
  statusAccentBg: 'rgba(255, 84, 54, 0.10)',
})

/** 品牌渐变背景(原 theme.scss .theme-gradient;stylex 禁用 background 简写) */
export const gradientBox = stylex.create({
  gradient: { backgroundImage: tokens.brandGradient },
})
