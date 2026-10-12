/**
 * 设计 token 的「真实值」常量表(三端共享)
 *
 * 用途:需要真实色值的场景 —— 图表填充(recharts)、标签色、
 * ConfigProvider theme 等;样式表场景请用 tokens.stylex(CSS 变量)。
 * 值必须与 tokens.stylex.ts 严格一致,由 tokens.test.ts 守卫。
 */
export const COLORS = {
  /** 湖蓝 · 主色(§2.1) */
  brand: '#3A9BD9',
  /** 品牌浅蓝(渐变起点) */
  brandLight: '#4DB6E5',
  /** 深湖蓝 · 按压态 */
  brandDark: '#2E7FB8',
  /** 品牌 10% tint 底 */
  brandTint: 'rgba(58, 155, 217, 0.10)',
  /** 强调橙(仅消费者端最高优先级 CTA + 优惠券;管理端禁用) */
  action: '#FF5436',
  /** 金额红(微信电商惯例) */
  price: '#FA5151',
  /** 页面底 */
  bg: '#F5F7FA',
  /** 卡片白 */
  card: '#FFFFFF',
  /** 主文字 */
  text: '#1A1A1A',
  /** 次要文字 */
  textSub: '#8C8C8C',
  /** 弱文字/占位 */
  textWeak: '#B8B8B8',
  /** 发丝线 */
  border: 'rgba(0, 0, 0, 0.06)',
  success: '#52C41A',
  warning: '#FAAD14',
  error: '#FF4D4F',
} as const
