import React, { useState, useEffect } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useReady, useDidShow, usePullDownRefresh } from '@tarojs/taro'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { pageTheme } from '../../styles/theme.stylex'
import AddressService from '../../services/AddressService'
import { Address } from '../../types/address'
import AddressCard from '../../components/AddressCard'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

// 子组件
import AddressSearchSection from './components/AddressSearchSection'
import AddressListEmptyState from './components/AddressListEmptyState'
import DeleteAddressModal from './components/DeleteAddressModal'

// 收货地址列表(原 index.scss;后代选择器拆到各子元素,长度保持 scss 来源
// 750 基准 rpx)
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    boxSizing: 'border-box',
    backgroundColor: 'var(--weui-BG-0)',
    // 为底部「新增收货地址」操作栏预留空间
    paddingBottom: '160rpx',
  },
  // 选择模式提示文案单行省略(原 .select-notice 后代规则)
  selectNoticeText: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: 'auto',
    minWidth: 0,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  addressList: {
    // AddressCard 自带留白,这里只做组间距
    paddingBottom: '16rpx',
  },
  // 底部新增收货地址主按钮(原 .add-address-bar,固定底栏 + iOS 安全区)
  addAddressBar: {
    position: 'fixed',
    left: '0',
    right: '0',
    bottom: '0',
    zIndex: 100,
    paddingBottom: 'calc(16rpx + env(safe-area-inset-bottom))',
  },
  // 深色胶囊轻提示文案(原 .address-toast .weui-toast__text 后代选择器)
  toastText: {
    color: tokens.onBrand,
    wordBreak: 'break-all',
  },
})

const AddressList: React.FC = () => {
  usePageViewTracking()

  const [addresses, setAddresses] = useState<Address[]>([])
  const [filteredAddresses, setFilteredAddresses] = useState<Address[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [isSelectMode, setIsSelectMode] = useState<boolean>(false)
  const [deleteModalVisible, setDeleteModalVisible] = useState<boolean>(false)
  const [deleteAddressId, setDeleteAddressId] = useState<string | null>(null)
  const [showToast, setShowToast] = useState<boolean>(false)
  const [toastText, setToastText] = useState<string>('')
  // 原 toastType 仅用于 address-toast--* 类(全局无定义的死类,已随 index.scss 删除)
  const [searchValue, setSearchValue] = useState<string>('')

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
        setFilteredAddresses(result.data)
      } else {
        setToastText(result.error?.message || '获取地址列表失败')
        setShowToast(true)
      }
    } catch (error) {
      console.error('获取地址列表失败:', error)
      setToastText('获取地址列表失败')
      setShowToast(true)
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = (value: string) => {
    setSearchValue(value)
    if (!value.trim()) {
      // 如果搜索词为空，显示所有地址
      setFilteredAddresses(addresses)
      return
    }

    const searchLower = value.toLowerCase().trim()
    const filtered = addresses.filter(address => {
      // 按手机号搜索
      if (address.phone.includes(searchLower)) {
        return true
      }
      // 按收货人姓名搜索
      if (address.receiverName.toLowerCase().includes(searchLower)) {
        return true
      }
      // 按地址关键词搜索（省市区 + 详细地址）
      const fullAddress = `${address.province}${address.city}${address.district}${address.detailAddress}`.toLowerCase()
      if (fullAddress.includes(searchLower)) {
        return true
      }
      return false
    })

    setFilteredAddresses(filtered)
  }

  const handleSearchClear = () => {
    setSearchValue('')
    setFilteredAddresses(addresses)
  }

  const handleShowToast = (text: string, _type: 'success' | 'error' | 'loading' = 'success') => {
    setToastText(text)
    setShowToast(true)
  }

  const handleHideToast = () => {
    setShowToast(false)
  }

  const handleAddAddress = () => {
    // 新增收货地址:无 id 进入编辑页为新增模式(F-ADR-002)
    Taro.navigateTo({
      url: '/pages/address-edit/index'
    })
  }

  const handleEditAddress = (addressId: string) => {
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

  const handleSetDefault = async (addressId: string) => {
    try {
      const result = await addressService.setDefaultAddress(addressId)

      if (result.success) {
        // 更新本地状态
        setAddresses(addresses.map(addr => ({
          ...addr,
          isDefault: addr.id === addressId
        })))
        setFilteredAddresses(filteredAddresses.map(addr => ({
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

  const handleDeleteClick = (addressId: string) => {
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
        setFilteredAddresses(filteredAddresses.filter(addr => addr.id !== deleteAddressId))
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
    <View {...stylex.props(pageTheme, styles.page)}>
      {/* 搜索栏 + 统计/微信地址导入(address-list.html) */}
      <AddressSearchSection
        searchValue={searchValue}
        count={filteredAddresses.length}
        onSearch={handleSearch}
        onSearchClear={handleSearchClear}
        onWechatImport={handleWechatImport}
      />

      {/* 选择模式提示(address-list.html 状态三 .weui-notice--brand) */}
      {isSelectMode && (
        <View className='weui-notice weui-notice--brand'>
          <Text {...stylex.props(styles.selectNoticeText)}>选择地址后将回填至确认订单页</Text>
        </View>
      )}

      {/* 地址列表(收货地址 weui cells 组) */}
      <View {...stylex.props(styles.addressList)}>
        {loading ? (
          <View className='weui-loadmore weui-loadmore_loading'>
            <View className='spinner' />
            <Text>加载中...</Text>
          </View>
        ) : filteredAddresses.length === 0 ? (
          <AddressListEmptyState searching={!!searchValue} />
        ) : (
          <>
            <View className='weui-cells__title'>收货地址</View>
            {filteredAddresses.map((address) => (
              <AddressCard
                key={address.id}
                address={address}
                isSelectMode={isSelectMode}
                onSelect={handleSelectAddress}
                onEdit={handleEditAddress}
                onSetDefault={handleSetDefault}
                onDelete={handleDeleteClick}
              />
            ))}
          </>
        )}
      </View>

      {/* 删除确认弹窗(.weui-mask + .weui-dialog) */}
      <DeleteAddressModal
        visible={deleteModalVisible}
        onCancel={handleDeleteCancel}
        onConfirm={handleDeleteConfirm}
      />

      {/* Toast 提示(.weui-toast_text 深色胶囊) */}
      {showToast && (
        <View className='weui-toast weui-toast_text' onClick={handleHideToast}>
          <Text {...props('weui-toast__text', styles.toastText)}>{toastText}</Text>
        </View>
      )}

      {/* 底部新增收货地址主按钮(address-list.html .mp-footbar) */}
      <View {...props('mp-footbar hair-t', styles.addAddressBar)}>
        <View
          className='weui-btn weui-btn_primary flex1'
          trackTarget='address-add'
          trackAction='CLICK'
          trackElementType='button'
          trackElementText='新增收货地址'
          onClick={handleAddAddress}
        >
          新增收货地址
        </View>
      </View>

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default AddressList
