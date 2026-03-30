import { formatCurrency, formatDateTime, Order, OrderStatus } from "@aquarush/common"
import { AtList, AtListItem } from "taro-ui"
import { View } from "@tarojs/components"

interface OrderInfoCardProps {
  order: Order
}

const getPaymentTypeLabel = (paymentType: string) => {
  switch (paymentType) {
    case 'CASH':
      return '现金'
    case 'QR_CODE':
      return '二维码'
    case 'WATER_TICKET':
      return '水票'
    default:
      return paymentType
  }
}

function formatPayment(isSelfCollect: boolean,paymentType?: string) {
  if (isSelfCollect) {
    return '已收款'
  }

  return paymentType ? getPaymentTypeLabel(
    paymentType) : '未付款'
}

export const OrderInfoCard: React.FC<OrderInfoCardProps> = ({ order }) => {
  return (
    <View className='card order-info-card'>
      <AtList>
        <AtListItem title='订单号' extraText={order.orderNo} />
        <AtListItem title='下单时间' extraText={formatDateTime(order.createdAt)} />
        <AtListItem title='订单金额' extraText={formatCurrency(order.amount)} />
        <AtListItem title='是否自收' extraText={order.isSelfCollect ? '是' : '否'} />
        {(order.status === OrderStatus.COMPLETED || order.status === OrderStatus.DELIVERING) && (
          <AtListItem
            title='收款状态'
            extraText={formatPayment(order.isSelfCollect, order.paymentType)}
          />
        )}
      </AtList>
    </View>
  )
}
