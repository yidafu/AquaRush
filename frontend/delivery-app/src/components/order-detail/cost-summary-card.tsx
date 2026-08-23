import { Card } from '@/components/card';
import { OrderInfoRow } from '@/components/order-info-row';
import { useTheme } from '@/hooks/use-theme';
import { buildCostRows } from '@aquarush/common/utils/order-cost';

interface CostSummaryCardProps {
  /** 订单营销快照(F-ORD-003 费用明细卡,金额均为分 R-MON-01) */
  order: {
    items?: ReadonlyArray<{
      unitPriceCents?: number | string | null;
      quantity?: number;
    }>;
    deliveryFeeCents?: number | string | null;
    payAmountCents?: number | string | null;
    promotionDiscountCents?: number | string | null;
    promotionActivityName?: string | null;
    firstOrderDiscountCents?: number | string | null;
    firstOrderActivityName?: string | null;
    couponDiscountCents?: number | string | null;
    couponName?: string | null;
    ticketRedeemCount?: number | null;
    ticketDiscountCents?: number | string | null;
    giftItems?: ReadonlyArray<{ productName?: string; quantity?: number }> | null;
  };
}

/**
 * 订单费用明细卡(送水工端订单详情):
 * 商品金额/水票抵扣/满减/首单立减/优惠券/赠品/配送费/实付,
 * 营销优惠明细来自订单快照(R-MKT-02/07/08/09/10),扣减行语义红、实付行主色加粗。
 */
export function CostSummaryCard({ order }: CostSummaryCardProps) {
  const theme = useTheme();
  const rows = buildCostRows(order);
  if (rows.length === 0) return null;

  return (
    <Card title="费用明细">
      {rows.map((row) => (
        <OrderInfoRow
          key={row.key}
          label={row.label}
          value={row.value}
          valueStyle={[
            row.total ? { color: theme.primary, fontWeight: '700' } : null,
            row.discount ? { color: theme.danger } : null,
          ]}
        />
      ))}
    </Card>
  );
}

export default CostSummaryCard;
