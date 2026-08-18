/**
 * 送水工端订单费用明细行组装单测(订单详情页费用明细卡)
 * 覆盖:全优惠栈顺序 / 仅水票 / 仅满减 / 仅首单 / 仅优惠券 / 赠品 / 免运费 / 无优惠。
 */
import { buildCostRows, type CostRow, type OrderCostInput } from './orderCost';

describe('buildCostRows', () => {
  it('全优惠栈:顺序 = 商品金额/水票/满减/首单/优惠券/赠品/配送费/实付,扣减项标记 discount', () => {
    const input: OrderCostInput = {
      items: [
        { unitPriceCents: 2500, quantity: 2 },
        { unitPriceCents: 1500, quantity: 1 },
      ],
      deliveryFeeCents: 800,
      payAmountCents: 3000,
      ticketRedeemCount: 1,
      ticketDiscountCents: 2500,
      promotionDiscountCents: 500,
      promotionActivityName: '满100减10',
      firstOrderDiscountCents: 300,
      firstOrderActivityName: '新人立减',
      couponDiscountCents: 200,
      couponName: '满50减3',
      giftItems: [{ productName: '矿泉水', quantity: 1 }],
    };

    const rows = buildCostRows(input);

    expect(rows.map((r) => r.key)).toEqual([
      'subtotal',
      'ticket',
      'promotion',
      'firstOrder',
      'coupon',
      'gift',
      'delivery',
      'total',
    ]);
    expect(rows[0]).toMatchObject({ label: '商品金额', value: '¥65.00' });
    expect(rows[1]).toMatchObject({ label: '水票抵扣（1桶）', value: '-¥25.00', discount: true });
    expect(rows[2]).toMatchObject({ label: '满减（满100减10）', value: '-¥5.00', discount: true });
    expect(rows[3]).toMatchObject({ label: '首单立减（新人立减）', value: '-¥3.00', discount: true });
    expect(rows[4]).toMatchObject({ label: '优惠券（满50减3）', value: '-¥2.00', discount: true });
    expect(rows[5]).toMatchObject({ label: '赠品', value: '矿泉水 ×1（¥0.00）' });
    expect(rows[6]).toMatchObject({ label: '配送费', value: '¥8.00' });
    expect(rows[7]).toMatchObject({ label: '实付金额', value: '¥30.00', total: true });
  });

  it('仅水票:水票行按桶数与金额展示', () => {
    const rows = buildCostRows({
      items: [{ unitPriceCents: 2000, quantity: 3 }],
      ticketRedeemCount: 2,
      ticketDiscountCents: 4000,
      payAmountCents: 2000,
    });
    expect(rows.map((r) => r.key)).toEqual(['subtotal', 'ticket', 'delivery', 'total']);
    expect(rows[1]).toMatchObject({ label: '水票抵扣（2桶）', value: '-¥40.00', discount: true });
  });

  it('无任何优惠:仅 商品金额/配送费/实付,免配送费文案', () => {
    const rows = buildCostRows({
      items: [{ unitPriceCents: 1000, quantity: 1 }],
      deliveryFeeCents: 0,
      payAmountCents: 1000,
    });
    expect(rows.map((r) => r.key)).toEqual(['subtotal', 'delivery', 'total']);
    expect(rows[1]).toMatchObject({ label: '配送费', value: '免配送费' });
    expect(rows.some((r) => r.discount)).toBe(false);
  });

  it('Long 字符串字段经 normalizeCents 归一化', () => {
    const rows = buildCostRows({
      items: [{ unitPriceCents: '2500', quantity: 1 }],
      deliveryFeeCents: '200',
      payAmountCents: '2700',
      couponDiscountCents: '0',
    });
    expect(rows[0]).toMatchObject({ value: '¥25.00' });
    expect(rows[1]).toMatchObject({ label: '配送费', value: '¥2.00' });
    expect(rows[2]).toMatchObject({ value: '¥27.00', total: true });
  });
});
