import { Money } from '@aquarush/common';

import { Card } from '@/components/card';
import { OrderInfoRow } from '@/components/order-info-row';
import { formatPaymentStatus } from '@/constants/order';
import { ThemedText } from '@/components/themed-text';

interface OrderInfoCardProps {
  order: {
    orderNo: string;
    createdAt?: string;
    amountCents?: number | string;
    payAmountCents?: number | string;
    isSelfCollect?: boolean;
    status?: string;
    paymentType?: string | null;
  };
}

function formatDateTime(dateStr?: string) {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function OrderInfoCard({ order }: OrderInfoCardProps) {
  const showPayment =
    order.status === 'COMPLETED' || order.status === 'DELIVERING';

  return (
    <Card title="订单信息">
      <OrderInfoRow label="订单号" value={order.orderNo} />
      <OrderInfoRow label="下单时间" value={formatDateTime(order.createdAt)} />
      <OrderInfoRow
        label="实付金额"
        value={
          <ThemedText type="smallBold">
            <Money cents={order.payAmountCents ?? order.amountCents} />
          </ThemedText>
        }
      />
      <OrderInfoRow label="是否自收" value={order.isSelfCollect ? '是' : '否'} />
      {showPayment ? (
        <OrderInfoRow
          label="收款状态"
          value={formatPaymentStatus(Boolean(order.isSelfCollect), order.paymentType ?? undefined)}
        />
      ) : null}
    </Card>
  );
}
