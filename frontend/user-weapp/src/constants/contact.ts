/**
 * 联系信息常量
 */
export const CONTACT_INFO = {
  // 服务热线
  SERVICE_HOTLINE: '400-800-8888',

  // 客服时间
  SERVICE_HOURS: '8:00 - 21:00 (节假日照常服务)',

  // 品牌合作邮箱
  PARTNER_EMAIL: 'partner@haoheshanquan.com',

  // 水源地地址
  WATER_SOURCE_ADDRESS: '浙江省杭州市临安区天目山风景区',

  // 客服邮箱
  SUPPORT_EMAIL: 'support@haoheshanquan.com',

  // 投诉热线
  COMPLAINT_HOTLINE: '400-800-9999'
} as const

/**
 * 联系类型枚举
 */
export enum ContactType {
  SERVICE = 'service',
  PARTNER = 'partner',
  SUPPORT = 'support',
  COMPLAINT = 'complaint'
}

