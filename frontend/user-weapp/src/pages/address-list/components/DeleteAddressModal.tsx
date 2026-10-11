import React from 'react'
import { View } from '@tarojs/components'

// 原 DeleteAddressModal.scss 仅含注释(视觉全部来自 weui.scss 的
// weui-mask / weui-dialog 体系,无页内覆盖),已随 scss 删除

interface DeleteAddressModalProps {
  visible: boolean
  onCancel: () => void
  onConfirm: () => void
}

/**
 * 删除地址确认弹窗(address-list.html .weui-mask + .weui-dialog,
 * 危险操作按钮用 .weui-dialog__btn_warn)
 */
const DeleteAddressModal: React.FC<DeleteAddressModalProps> = ({ visible, onCancel, onConfirm }) => {
  if (!visible) return null

  return (
    <>
      <View className='weui-mask' onClick={onCancel} />
      <View className='weui-dialog'>
        <View className='weui-dialog__hd'>
          <View className='weui-dialog__title'>确认删除</View>
        </View>
        <View className='weui-dialog__bd'>确定要删除这个地址吗？删除后无法恢复。</View>
        <View className='weui-dialog__ft'>
          <View
            className='weui-dialog__btn weui-dialog__btn_default'
            onClick={onCancel}
          >
            取消
          </View>
          <View
            className='weui-dialog__btn weui-dialog__btn_warn'
            onClick={onConfirm}
          >
            删除
          </View>
        </View>
      </View>
    </>
  )
}

export default DeleteAddressModal
