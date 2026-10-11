/**
 * 管理端品牌色常量 —— 值已收敛到 @aquarush/common/theme 的 COLORS
 * (单一事实来源:spec/design/ui-design-system.md V1.1 / tokens.stylex.ts,
 * 守卫测试:common tokens.test.ts)。
 * 本文件保留原导出名,存量引用零改动。
 * 管理端禁止消费者强调橙 #FF5436;危险操作用 error 红 #FF4D4F。
 */
import { COLORS } from '@aquarush/common/theme'

export const BRAND_PRIMARY = COLORS.brand;
export const BRAND_PRIMARY_LIGHT = COLORS.brandTint;
export const BRAND_GRADIENT_START = COLORS.brandLight;
export const BRAND_GRADIENT_END = COLORS.brand;
export const COLOR_SUCCESS = COLORS.success;
export const COLOR_WARNING = COLORS.warning;
export const COLOR_ERROR = COLORS.error;
export const TEXT_MAIN = COLORS.text;
export const TEXT_SECONDARY = COLORS.textSub;
