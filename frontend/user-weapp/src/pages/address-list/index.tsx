import React, { useState, useEffect } from 'react'
import { View, Text, Button, Image } from '@tarojs/components'
import { AtButton, AtToast, AtModal, AtModalHeader, AtModalContent, AtModalAction } from 'taro-ui'
import Taro, { useReady, useDidShow, usePullDownRefresh } from '@tarojs/taro'
import AddressService from '../../services/AddressService'
import { Address } from '../../types/address'
import AddressCard from '../../components/AddressCard'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/modal.scss"
import './index.scss'

interface AddressListProps {
  // Props can be added here if needed
}

const AddressList: React.FC<AddressListProps> = () => {
  const [addresses, setAddresses] = useState<Address[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [isSelectMode, setIsSelectMode] = useState<boolean>(false)
  const [deleteModalVisible, setDeleteModalVisible] = useState<boolean>(false)
  const [deleteAddressId, setDeleteAddressId] = useState<number | null>(null)
  const [showToast, setShowToast] = useState<boolean>(false)
  const [toastText, setToastText] = useState<string>('')
  const [toastType, setToastType] = useState<'success' | 'error' | 'loading'>('success')

  const addressService = AddressService.getInstance()

  // Standard React hooks
  useEffect(() => {
    // Component mounted logic
    console.log('AddressList component mounted')
  }, [])

  // Taro page lifecycle hooks
  useReady(() => {
    // 检查是否从订单确认页面跳转过来（选择地址模式）
    const instance = Taro.getCurrentInstance()
    const { select } = instance.router?.params || {}
    const isFromOrderConfirm = select === 'true'
    setIsSelectMode(isFromOrderConfirm)
  })

  useDidShow(() => {
    // 页面显示时刷新地址列表（从地址编辑页面返回时）
    loadAddresses()
  })

  usePullDownRefresh(async () => {
    try {
      await loadAddresses()
    } finally {
      Taro.stopPullDownRefresh()
    }
  })

  const loadAddresses = async () => {
    try {
      setLoading(true)

      const result = await addressService.getUserAddresses()

      if (result.success && result.data) {
        setAddresses(result.data)
      } else {
        setToastText(result.error?.message || '获取地址列表失败')
        setToastType('error')
        setShowToast(true)
      }
    } catch (error) {
      console.error('获取地址列表失败:', error)
      setToastText('获取地址列表失败')
      setToastType('error')
      setShowToast(true)
    } finally {
      setLoading(false)
    }
  }

  const handleShowToast = (text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    setToastText(text)
    setToastType(type)
    setShowToast(true)
  }

  const handleHideToast = () => {
    setShowToast(false)
  }

  const handleAddAddress = () => {
    Taro.navigateTo({
      url: '/pages/address-edit/index'
    })
  }

  const handleEditAddress = (addressId: number) => {
    Taro.navigateTo({
      url: `/pages/address-edit/index?id=${addressId}`
    })
  }

  const handleSelectAddress = (address: Address) => {
    if (!isSelectMode) return

    // 将选中的地址存储到本地，供订单确认页面使用
    Taro.setStorageSync('selectedAddress', address)

    // 返回订单确认页面
    Taro.navigateBack()
  }

  const handleSetDefault = async (addressId: number) => {
    try {
      const result = await addressService.setDefaultAddress(addressId)

      if (result.success) {
        // 更新本地状态
        setAddresses(addresses.map(addr => ({
          ...addr,
          isDefault: addr.id === addressId
        })))

        handleShowToast('设置默认地址成功', 'success')
      } else {
        handleShowToast(result.error?.message || '设置默认地址失败', 'error')
      }
    } catch (error) {
      console.error('设置默认地址失败:', error)
      handleShowToast('设置默认地址失败', 'error')
    }
  }

  const handleDeleteClick = (addressId: number) => {
    setDeleteModalVisible(true)
    setDeleteAddressId(addressId)
  }

  const handleDeleteConfirm = async () => {
    if (!deleteAddressId) return

    try {
      const result = await addressService.deleteAddress(deleteAddressId)

      if (result.success) {
        // 从本地状态中移除地址
        setAddresses(addresses.filter(addr => addr.id !== deleteAddressId))
        setDeleteModalVisible(false)
        setDeleteAddressId(null)
        handleShowToast('删除地址成功', 'success')
      } else {
        handleShowToast(result.error?.message || '删除地址失败', 'error')
      }
    } catch (error) {
      console.error('删除地址失败:', error)
      handleShowToast('删除地址失败', 'error')
    }
  }

  const handleDeleteCancel = () => {
    setDeleteModalVisible(false)
    setDeleteAddressId(null)
  }

  const handleWechatImport = () => {
    // 微信地址导入功能
    Taro.chooseAddress({
      success: (res) => {
        const newAddress = {
          receiverName: res.userName || '',
          phone: res.telNumber || '',
          province: res.provinceName || '',
          city: res.cityName || '',
          district: res.countyName || '',
          detailAddress: `${res.detailInfo || ''}`.trim(),
          isDefault: false
        }

        // 跳转到地址编辑页面，预填充微信地址信息
        Taro.setStorageSync('wechatAddress', newAddress)
        Taro.navigateTo({
          url: '/pages/address-edit/index?fromWechat=true'
        })
      },
      fail: (error) => {
        console.error('微信地址导入失败:', error)
        if (error.errMsg.includes('auth deny')) {
          Taro.showModal({
            title: '提示',
            content: '需要获取您的微信地址信息，请在设置中开启授权',
            showCancel: false
          })
        }
      }
    })
  }

  return (
    <View className='address-list-page'>
      {/* 微信地址导入按钮 */}
      <View className='flex justify-end import-section'>
        <AtButton
          type='secondary'
          size='small'
          onClick={handleWechatImport}
          className='import-button'
        >
          <Text>导入微信地址</Text>
        </AtButton>
      </View>

      {/* 地址列表 */}
      <View className='address-list'>
        {loading ? (
          <View className='loading-container'>
            <Text>加载中...</Text>
          </View>
        ) : addresses.length === 0 ? (
          <View className='empty-container'>
            <Image
              src='/assets/empty-address.png'
              mode='aspectFit'
              className='empty-image'
            />
            <Text className='empty-text'>暂无收货地址</Text>
            <Text className='empty-desc'>添加您的收货地址，方便快速下单</Text>
          </View>
        ) : (
          addresses.map((address) => (
            <AddressCard
              key={address.id}
              address={address}
              isSelectMode={isSelectMode}
              onSelect={handleSelectAddress}
              onEdit={handleEditAddress}
              onSetDefault={handleSetDefault}
              onDelete={handleDeleteClick}
            />
          ))
        )}
      </View>

      {/* 底部添加按钮 */}
      <View className='bottom-actions'>
        <AtButton
          type='primary'
          size='normal'
          onClick={handleAddAddress}
          className='add-button'
        >
          <Text>新增收货地址</Text>
        </AtButton>
      </View>

      {/* 删除确认弹窗 */}
      <AtModal
        isOpened={deleteModalVisible}
        onClose={handleDeleteCancel}
      >
        <AtModalHeader>确认删除</AtModalHeader>
        <AtModalContent>
          <Text>确定要删除这个地址吗？删除后无法恢复。</Text>
        </AtModalContent>
        <AtModalAction>
          <Button onClick={handleDeleteCancel}>取消</Button>
          <Button onClick={handleDeleteConfirm}>删除</Button>
        </AtModalAction>
      </AtModal>

      {/* Toast 提示 */}
      <AtToast
        isOpened={showToast}
        text={toastText}
        status={toastType}
        onClose={handleHideToast}
      />

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default AddressList
