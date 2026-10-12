/**
 * 配送费规则配置页(F-OPS-005/R-MON-02/R-OPS-03/G96)表单值与回显 VO 类型,
 * 由 index.tsx 拆分。
 */

export interface FeeTierFormValue {
  fromFloor: number;
  toFloor?: number | null;
  feeYuan: number;
}

export interface FeeTierVo {
  fromFloor: number;
  toFloor?: number | null;
  feeCents: string | number;
}

export interface DistanceTierFormValue {
  upToKm?: number | null;
  feeYuan: number;
}

export interface DistanceTierVo {
  upToKm?: number | null;
  feeCents: string | number;
}

/** GetDeliveryFeeRule 查询回显数据(由 index.tsx 传入当前规则) */
export interface DeliveryFeeRuleData {
  freeFloorLimit: number;
  surchargeCents: number;
  tiers?: FeeTierVo[] | null;
  minimumOrderCents?: number | string | null;
  distanceTiers?: DistanceTierVo[] | null;
  updatedAt?: string | null;
}
