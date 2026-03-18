import { formatCentsToCurrency, Product } from "@aquarush/common"
import { View, Text, Image } from "@tarojs/components"


interface ProductCardProps {
  product?: Product
  quantity: number
  remark?: string
}

export const ProductCard: React.FC<ProductCardProps> = ({ product, quantity, remark }) => {
  if (!product) return null

  return (
    <View className='card product-card'>
      <View className='card-header'>
        <Text className='card-title'>商品详情</Text>
      </View>
      <View className='card-content'>
        <View className='product-info'>
          {product.coverImageUrl && (
            <Image
              className='product-image'
              src={product.coverImageUrl}
              mode='aspectFill'
            />
          )}
          <View className='product-details'>
            <Text className='product-name'>{product.name}</Text>
            <View className='product-price-row'>
              <Text className='product-price'>
                {formatCentsToCurrency(product.price)}
              </Text>
              <Text className='product-quantity'>x {quantity}</Text>
            </View>
          </View>
        </View>
        {remark && (
          <View className='remark'>
            <Text className='remark-label'>备注：</Text>
            <Text className='remark-content'>{remark}</Text>
          </View>
        )}
      </View>
    </View>
  )
}
