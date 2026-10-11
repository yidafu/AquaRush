import React from 'react'
import { View, Text } from '@tarojs/components'
import { DeliverySlotVo } from '@aquarush/common'
import './DeliveryTimeSelector.scss'

interface DeliveryTimeSelectorProps {
  /** 配送时段选项(F-SYS-002:由后台「订单与履约参数」配置驱动,已过滤启用项) */
  slots: DeliverySlotVo[]
  selectedTime: string
  onTimeChange: (time: string) => void
}

/**
 * 配送时段选择器(AC-ORD-001-05:id 与后端 DeliveryTimeSlot 枚举对应,提交时大写随单落库)
 * WeUI .weui-segmented 分段控件(选中白底高亮)+ 时段说明提示行;
 * 选项不写死:由后台配置(启用/时间窗/立即配送时限)下发,页面经 enabledDeliverySlots 过滤后传入。
 */
const DeliveryTimeSelector: React.FC<DeliveryTimeSelectorProps> = ({
  slots,
  selectedTime,
  onTimeChange
}) => {
  const selectedSlot = slots.find(slot => slot.id === selectedTime)

  return (
    <View className='delivery-section'>
      <View className='weui-cells__title'>配送时间</View>
      <View className='weui-cells'>
        <View className='weui-cell weui-cell_vertical'>
          <View className='weui-segmented delivery-segmented'>
            {slots.map(slot => {
              const selected = selectedTime === slot.id
              return (
                <View
                  key={slot.id}
                  className={`weui-segmented__item${selected ? ' on' : ''}`}
                  onClick={() => onTimeChange(slot.id)}
                >
                  <Text>{slot.name}</Text>
                </View>
              )
            })}
          </View>
          {selectedSlot?.hint && (
            <Text className='delivery-hint'>{selectedSlot.hint}</Text>
          )}
        </View>
      </View>
    </View>
  )
}

export default DeliveryTimeSelector
