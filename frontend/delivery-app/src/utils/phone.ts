/**
 * 号码保护(F-ORD-032/R-ORD-17)拨号判定:
 * 服务端仅在配送阶段(待配送/配送中)下发完整号码,非配送阶段只下发
 * 脱敏串(前 3 + **** + 后 4)。前端不自行脱敏,仅按「是否为完整号码」
 * 决定渲染 `tel:` 拨号入口 —— 脱敏串含掩码字符,天然不满足完整号码形态,
 * 因此非配送阶段拿不到拨号入口(AC-ORD-032-05,无「查看完整号码」旁路)。
 */

/** 完整号码形态:7~15 位纯数字(本地手机号 11 位;含固话/带区号短号) */
const FULL_PHONE_PATTERN = /^\d{7,15}$/;

/** 是否完整号码(可渲染拨号入口);脱敏串/空值/异常格式均不可拨 */
export function isDialablePhone(phone?: string | null): boolean {
  if (!phone) return false;
  return FULL_PHONE_PATTERN.test(phone.trim());
}
