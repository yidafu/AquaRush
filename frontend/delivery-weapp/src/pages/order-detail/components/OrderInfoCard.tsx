import { formatCurrency, formatDateTime, Order } from "@aquarush/common"
import { View, Text } from "@tarojs/components"



interface OrderInfoCardProps {
  order: Order
}

export const OrderInfoCard: React.FC<OrderInfoCardProps> = ({ order }) => {
  return (
    <View className='card order-info-card'>
      <View className='card-header'>
        <Text className='card-title'>订单信息</Text>
      </View>
      <View className='card-content'>
        <View className='info-row'>
          <Text className='info-label'>订单号</Text>
          <Text className='info-value'>{order.orderNo}</Text>
        </View>
        <View className='info-row'>
          <Text className='info-label'>下单时间</Text>
          <Text className='info-value'>{formatDateTime(order.createdAt)}</Text>
        </View>
        <View className='info-row'>
          <Text className='info-label'>订单金额</Text>
          <Text className='info-value amount'>{formatCurrency(order.amount)}</Text>
        </View>
        {order.paymentType && (
          <View className='info-row'>
            <Text className='info-label'>支付方式</Text>
            <Text className='info-value'>
              {order.paymentType === 'CASH' ? '现金' :
                order.paymentType === 'QR_CODE' ? '二维码' :
                  order.paymentType === 'WATER_TICKET' ? '水票' : order.paymentType}
            </Text>
          </View>
        )}
        {order.isSelfCollect && (
          <View className='info-row'>
            <Text className='info-label'>取货方式</Text>
            <Text className='info-value'>自提</Text>
          </View>
        )}
      </View>
    </View>
  )
}
