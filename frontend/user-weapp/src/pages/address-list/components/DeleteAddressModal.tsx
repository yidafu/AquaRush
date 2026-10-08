import React from 'react'
import { Text, Button } from '@tarojs/components'
import { AtModal, AtModalHeader, AtModalContent, AtModalAction } from 'taro-ui'

import "taro-ui/dist/style/components/modal.scss"
import './DeleteAddressModal.scss'

interface DeleteAddressModalProps {
  visible: boolean
  onCancel: () => void
  onConfirm: () => void
}

/**
 * 删除地址确认弹窗(AtModal,含 taro-ui modal 品牌化覆盖)
 */
const DeleteAddressModal: React.FC<DeleteAddressModalProps> = ({ visible, onCancel, onConfirm }) => {
  return (
    <AtModal
      isOpened={visible}
      onClose={onCancel}
    >
      <AtModalHeader>确认删除</AtModalHeader>
      <AtModalContent>
        <Text>确定要删除这个地址吗？删除后无法恢复。</Text>
      </AtModalContent>
      <AtModalAction>
        <Button onClick={onCancel}>取消</Button>
        <Button onClick={onConfirm}>删除</Button>
      </AtModalAction>
    </AtModal>
  )
}

export default DeleteAddressModal
