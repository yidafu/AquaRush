/**
 * Ant Design ConfigProvider theme token(admin-client 专用)
 *
 * antd 需要真实值而不是 CSS 变量引用;此处数值必须与 tokens.stylex.ts 严格
 * 一致,由 tokens.test.ts 做源码级守卫。
 */
export const ANTD_THEME_TOKEN = {
  colorPrimary: '#3A9BD9',
  colorLink: '#3A9BD9',
  colorBgLayout: '#F5F7FA',
  colorBgContainer: '#FFFFFF',
  colorText: '#1A1A1A',
  colorTextSecondary: '#8C8C8C',
  colorError: '#FF4D4F',
  colorSuccess: '#52C41A',
  colorWarning: '#FAAD14',
  borderRadius: 8, // tokens.rSm
  borderRadiusLG: 16, // tokens.rCard
  colorBorderSecondary: 'rgba(0, 0, 0, 0.06)', // tokens.border
} as const
