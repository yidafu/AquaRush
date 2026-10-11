/**
 * AquaRush 设计 Token(StyleX defineVars)—— user-weapp 与 admin-client 共享
 *
 * 权威定义:spec/design/ui-design-system.md V1.1 / spec/design/assets/tokens.css。
 * 值为 web(375 设计稿)基准;user-weapp 端经 createTheme 覆盖为 750 基准
 * (见 frontend/user-weapp/src/styles/theme.stylex.ts),颜色两端一致。
 *
 * 约定:
 * - 禁止在页面内硬编码 hex,一律 import 本文件 token
 *   (stylex 使用方必须从 '@aquarush/common/theme/tokens.stylex' 直接导入,
 *   不能走 '@aquarush/common/theme' barrel —— stylex babel 插件无法跟随 re-export
 *   解析 var 名);
 * - 状态色对必须与 @aquarush/common/utils/status-tone 的 STATUS_TONE_COLORS 一致,
 *   由 tokens.test.ts 守卫;
 * - fs、lh 系列 token 与 font、fontNum 仅为 admin(web)使用;
 *   user-weapp 页面尺寸一律 rpx 字面量。
 */
import * as stylex from '@stylexjs/stylex'

export const tokens = stylex.defineVars({
  // —— 品牌色板(三端共享)——
  brand: '#3A9BD9', // 湖蓝 · 主色
  brandLight: '#4DB6E5', // 浅蓝
  brandDark: '#2E7FB8', // 深湖蓝 · 按压态
  brandTint: 'rgba(58, 155, 217, 0.10)',
  brandTintStrong: 'rgba(58, 155, 217, 0.18)',
  brandGradient: 'linear-gradient(135deg, #4DB6E5 0%, #3A9BD9 100%)',

  // —— 强调色(⚠️ 仅限消费者端最高优先级 CTA + 优惠券品牌元素)——
  action: '#FF5436',
  actionShadow: '0 2px 8px rgba(255, 84, 54, 0.30)',
  actionTint: 'rgba(255, 84, 54, 0.10)',

  // —— 金额红(微信电商惯例;仅金额文字,禁用于按钮/图标)——
  price: '#FA5151',
  priceLight: '#FF7A7E',

  // —— 基底 / 文字 ——
  bg: '#F5F7FA',
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

  // —— 间距刻度 ——
  s1: '4px',
  s2: '8px',
  s3: '12px',
  s4: '16px',
  s5: '24px',

  // —— 圆角 ——
  rCard: '16px',
  rBtn: '24px', // 全圆角按钮
  rSm: '8px', // 标签 / 缩略图 / 输入框

  // —— 阴影 ——
  shCard: '0 4px 16px rgba(0, 0, 0, 0.04)',
  shBtn: '0 2px 8px rgba(255, 84, 54, 0.30)',
  shModal: '0 8px 32px rgba(0, 0, 0, 0.12)',

  // —— 订单/业务状态标签(10% tint 底 + 状态色文字;与 STATUS_TONE_COLORS 对应)——
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

  // —— 管理后台(admin-client 专用)——
  side: '#001529',
  sideSub: '#000C17',
  sideActive: '#3A9BD9',

  // —— 字体排版刻度(admin/web 基准;user-weapp 不使用)——
  fsPage: '20px',
  lhPage: '28px',
  fsBlock: '16px',
  lhBlock: '24px',
  fsBody: '15px',
  lhBody: '22px',
  fsAux: '14px',
  lhAux: '20px',
  fsNote: '12px',
  lhNote: '16px',

  font: '-apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif',
  fontNum: '"SF Mono", "DIN Alternate", ui-monospace, Menlo, Consolas, monospace',
})
