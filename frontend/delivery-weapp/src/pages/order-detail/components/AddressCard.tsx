import { Address } from "@aquarush/common"
import { View, Text } from "@tarojs/components"


interface AddressCardProps {
  address?: Address
}

export const AddressCard: React.FC<AddressCardProps> = ({ address }) => {
  if (!address) return null

  return (
    <View className='card address-card'>
      <View className='card-header'>
        <Text className='card-title'>收货地址</Text>
      </View>
      <View className='card-content'>
        <View className='address-info'>
          <Text className='receiver-name'>{address.receiverName}</Text>
          <Text className='phone'>{address.phone}</Text>
        </View>
        <Text className='address-detail'>
          {address.province}{address.city}{address.district}{address.detailAddress}
        </Text>
      </View>
    </View>
  )
}
