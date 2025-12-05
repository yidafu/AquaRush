/**
 * 应用配置常量
 */

// 品牌信息
export const BRAND_INFO = {
  NAME: '好喝山泉',
  SUBTITLE: '源自大山深处，滴滴甘甜入心',
  LOGO_EMOJI: '⛰️',
  DESCRIPTION: '让每个家庭都能喝上真正的好水'
} as const

// 应用信息
export const APP_INFO = {
  VERSION: '1.0.0',
  COPYRIGHT: '© 2024 好喝山泉. 保留所有权利',
  COMPANY: '好喝山泉有限公司'
} as const

// 水源地信息
export const WATER_SOURCE_INFO = {
  NAME: '天目山国家级自然保护区',
  LOCATION: '浙江省杭州市临安区天目山风景区',
  ALTITUDE: '海拔1000米以上',
  FOREST_COVERAGE: '98.7%',
  PH_RANGE: '7.8-8.2',
  DESCRIPTION: '这里四季分明，雨量充沛，森林覆盖率高达98.7%。山泉水在地下深层岩石中经过数十年的天然过滤和矿化，形成了独特的甘甜口感和丰富的营养成分。'
} as const

// 品牌承诺特性
export const BRAND_FEATURES = [
  {
    icon: '🏔️',
    title: '天然源头',
    description: '源自海拔1000米原始森林，天然矿化'
  },
  {
    icon: '🔬',
    title: '科学检测',
    description: '106项水质检测，国家A级标准认证'
  },
  {
    icon: '🚚',
    title: '新鲜配送',
    description: '48小时从水源地到家，保持最佳口感'
  },
  {
    icon: '💧',
    title: '健康保障',
    description: '富含天然矿物质，PH值7.8-8.2弱碱性'
  }
] as const

