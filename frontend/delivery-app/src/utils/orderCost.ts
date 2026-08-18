/**
 * 送水工端订单费用明细行组装(订单详情页费用明细卡)
 * 输入为订单营销快照(服务端 orderByNo 返回;商品金额=Σ(明细成交单价×数量));
 * 输出顺序:商品金额 → 水票抵扣 → 满减 → 首单立减 → 优惠券 → 赠品 → 配送费 → 实付,
 * 与用户端确认/详情页口径一致(R-MKT-02/07/08/09/10)。
 * 金额均为分(R-MON-01),仅展示格式化。
 */
import { displayCents, normalizeCents } from '@aquarush/common';

export interface CostRow {
  /** 行唯一标识(渲染 key 与单测断言用) */
  key: string;
  /** 行标签(含活动名/桶数等说明) */
  label: string;
  /** 展示值(已格式化,含 ¥) */
  value: string;
  /** 扣减项(优惠/抵扣,语义红展示) */
  discount?: boolean;
  /** 实付合计行 */
  total?: boolean;
}

export interface OrderCostInput {
  /** 商品明细(F-ORD-004):商品金额 = Σ(成交单价×数量),由 items 推导 */
  items?: ReadonlyArray<{
    unitPriceCents?: number | string | null;
    quantity?: number;
  }>;
  /** 配送费(分) */
  deliveryFeeCents?: number | string | null;
  /** 实付合计(分) */
  payAmountCents?: number | string | null;
  /** 满减/折扣优惠(分,叠加① —— R-MKT-07) */
  promotionDiscountCents?: number | string | null;
  promotionActivityName?: string | null;
  /** 首单立减优惠(分,叠加①与满减互斥取最大 —— R-MKT-10) */
  firstOrderDiscountCents?: number | string | null;
  firstOrderActivityName?: string | null;
  /** 优惠券优惠(分,叠加③ —— R-MKT-07) */
  couponDiscountCents?: number | string | null;
  couponName?: string | null;
  /** 水票核销张数(一票抵一桶 —— R-MKT-08) */
  ticketRedeemCount?: number | null;
  /** 水票抵扣金额(分) */
  ticketDiscountCents?: number | string | null;
  /** 赠品行(附加 0 价行 —— R-MKT-09) */
  giftItems?: ReadonlyArray<{ productName?: string; quantity?: number }> | null;
}

export const buildCostRows = (input: OrderCostInput): CostRow[] => {
  const rows: CostRow[] = [];

  const subtotalCents = (input.items ?? []).reduce(
    (sum, item) => sum + (normalizeCents(item.unitPriceCents) ?? 0) * (item.quantity ?? 0),
    0,
  );
  const deliveryFeeCents = normalizeCents(input.deliveryFeeCents) ?? 0;

  rows.push({ key: 'subtotal', label: '商品金额', value: displayCents(subtotalCents) });

  const ticketDiscountCents = normalizeCents(input.ticketDiscountCents) ?? 0;
  if ((input.ticketRedeemCount ?? 0) > 0 && ticketDiscountCents > 0) {
    rows.push({
      key: 'ticket',
      label: `水票抵扣（${input.ticketRedeemCount}桶）`,
      value: `-${displayCents(ticketDiscountCents)}`,
      discount: true,
    });
  }

  const promotionDiscountCents = normalizeCents(input.promotionDiscountCents) ?? 0;
  if (promotionDiscountCents > 0) {
    rows.push({
      key: 'promotion',
      label: input.promotionActivityName ? `满减（${input.promotionActivityName}）` : '满减',
      value: `-${displayCents(promotionDiscountCents)}`,
      discount: true,
    });
  }

  const firstOrderDiscountCents = normalizeCents(input.firstOrderDiscountCents) ?? 0;
  if (firstOrderDiscountCents > 0) {
    rows.push({
      key: 'firstOrder',
      label: input.firstOrderActivityName ? `首单立减（${input.firstOrderActivityName}）` : '首单立减',
      value: `-${displayCents(firstOrderDiscountCents)}`,
      discount: true,
    });
  }

  const couponDiscountCents = normalizeCents(input.couponDiscountCents) ?? 0;
  if (couponDiscountCents > 0) {
    rows.push({
      key: 'coupon',
      label: input.couponName ? `优惠券（${input.couponName}）` : '优惠券',
      value: `-${displayCents(couponDiscountCents)}`,
      discount: true,
    });
  }

  const giftItems = input.giftItems ?? [];
  if (giftItems.length > 0) {
    const giftText = giftItems.map((gift) => `${gift.productName ?? '赠品'} ×${gift.quantity ?? 0}`).join('、');
    rows.push({ key: 'gift', label: '赠品', value: `${giftText}（${displayCents(0)}）` });
  }

  rows.push({
    key: 'delivery',
    label: '配送费',
    value: deliveryFeeCents === 0 ? '免配送费' : displayCents(deliveryFeeCents),
  });

  rows.push({
    key: 'total',
    label: '实付金额',
    value: displayCents(normalizeCents(input.payAmountCents) ?? 0),
    total: true,
  });

  return rows;
};
