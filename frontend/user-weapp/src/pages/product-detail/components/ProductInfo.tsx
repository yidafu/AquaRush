import React from "react";
import { View, Text } from "@tarojs/components";
import { useProduct } from "../context/ProductContext";
import { displayCents } from "@/utils/money";

const ProductInfo: React.FC = () => {
  const { product } = useProduct();

  // Price calculations
  const hasMarketPrice =
    product.originalPrice && parseFloat(product.originalPrice) > 0;

  return (
    <View className="px-4 py-2 mt-4 bg-white animate-fade-in-up">
      {/* 产品标题和认证 */}
      <View className="mb-2">
        {/* 品牌名 + 商品名 + 规格 + 特性短语 */}
        <View className="mb-1">
          <Text className="mb-1 text-xl font-bold leading-tight text-gray-900">
            {product.name}
          </Text>
        </View>

        {/* 副标题/认证信息 */}
        {product.subtitle && (
          <View className="flex flex-col gap-1">
            <Text className="text-sm font-normal leading-relaxed text-gray-600">
              {product.subtitle}
            </Text>
          </View>
        )}
      </View>

      {/* 价格部分 */}
      <View className="flex flex-col mb-2 border-b border-gray-100 animate-fade-in-up">
        {/* 价格信息 */}
        <View className="flex items-baseline gap-1">
          <Text className="text-xl font-extrabold tracking-tight text-red-500">
            {displayCents(parseFloat(product.price))}
          </Text>
        </View>
        <View className="flex flex-row justify-between">
          <View>
            {/* 原价显示 */}
            {hasMarketPrice && (
              <View className="flex flex-col items-end gap-2 text-right">
                <Text className="text-xs font-normal text-gray-400">
                  原价 <Text className="line-through">
                  {displayCents(parseFloat(product.originalPrice!))}
                  </Text>

                </Text>
              </View>
            )}
          </View>

          <View className="flex items-end gap-2 text-right">
            <Text className="text-xs font-normal text-gray-400">
              已售 122 | 剩余 234
            </Text>
          </View>
        </View>
      </View>

      {/* 已选规格和发货信息 */}
      <View className="py-1 mb-2 border-t border-b border-gray-100 animate-fade-in-up">
        <View className="flex items-center gap-4 mb-4">
          <Text className="text-sm font-medium text-gray-600 min-w-20">
            规格
          </Text>
          <Text className="flex-1 text-sm font-normal text-gray-800">
            {product.specification}
          </Text>
        </View>

        <View className="flex items-center gap-4 mb-0">
          <Text className="text-sm font-medium text-gray-600 min-w-20">
            送货
          </Text>
          <Text className="flex-1 text-sm font-normal text-gray-800">
            当日达
          </Text>
        </View>
      </View>

    </View>
  );
};

export default ProductInfo;
