import { Component } from 'react'
import { View, Text, Button, Image } from '@tarojs/components'
import { AtButton, AtCard, AtIcon, AtToast, AtModal, AtModalHeader, AtModalContent, AtModalAction } from 'taro-ui'
import Taro from '@tarojs/taro'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/icon.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/modal.scss"
import './index.scss'

interface Address {
  id: string
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
  postalCode: string
  isDefault: boolean
}

interface AddressListState {
  addresses: Address[]
  loading: boolean
  isSelectMode: boolean
  deleteModalVisible: boolean
  deleteAddressId: string | null
  showToast: boolean
  toastText: string
  toastType: 'success' | 'error' | 'loading'
}

export default class AddressList extends Component<{}, AddressListState> {
  isFromOrderConfirm: boolean = false

  constructor(props) {
    super(props)
    this.state = {
      addresses: [],
      loading: true,
      isSelectMode: false,
      deleteModalVisible: false,
      deleteAddressId: null,
      showToast: false,
      toastText: '',
      toastType: 'success'
    }
  }

  componentDidMount() {
    // 检查是否从订单确认页面跳转过来（选择地址模式）
    const instance = Taro.getCurrentInstance()
    const { select } = instance.router?.params || {}

    this.isFromOrderConfirm = select === 'true'
    this.setState({ isSelectMode: this.isFromOrderConfirm })

    this.loadAddresses()
  }

  componentDidShow() {
    // 页面显示时刷新地址列表（从地址编辑页面返回时）
    this.loadAddresses()
  }

  onPullDownRefresh = () => {
    this.loadAddresses().finally(() => {
      Taro.stopPullDownRefresh()
    })
  }

  loadAddresses = async () => {
    try {
      this.setState({ loading: true })

      // TODO: 实际项目中这里应该调用获取地址列表的API
      // const addresses = await getUserAddresses()

      // 模拟地址数据
      const mockAddresses: Address[] = [
        {
          id: '1',
          receiverName: '张三',
          phone: '13800138000',
          province: '广东省',
          city: '深圳市',
          district: '南山区',
          detailAddress: '科技园南区深南大道9988号',
          postalCode: '518000',
          isDefault: true
        },
        {
          id: '2',
          receiverName: '李四',
          phone: '13900139000',
          province: '广东省',
          city: '深圳市',
          district: '福田区',
          detailAddress: '华强北电子世界1栋',
          postalCode: '518000',
          isDefault: false
        },
        {
          id: '3',
          receiverName: '王五',
          phone: '13700137000',
          province: '广东省',
          city: '深圳市',
          district: '宝安区',
          detailAddress: '宝安中心区新安街道',
          postalCode: '518000',
          isDefault: false
        }
      ]

      this.setState({ addresses: mockAddresses })
    } catch (error) {
      console.error('获取地址列表失败:', error)
      this.showToast('获取地址列表失败', 'error')
    } finally {
      this.setState({ loading: false })
    }
  }

  showToast = (text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    this.setState({
      showToast: true,
      toastText: text,
      toastType: type
    })
  }

  hideToast = () => {
    this.setState({ showToast: false })
  }

  handleAddAddress = () => {
    Taro.navigateTo({
      url: '/pages/address-edit/index'
    })
  }

  handleEditAddress = (addressId: string) => {
    Taro.navigateTo({
      url: `/pages/address-edit/index?id=${addressId}`
    })
  }

  handleSelectAddress = (address: Address) => {
    if (!this.isFromOrderConfirm) return

    // 将选中的地址存储到本地，供订单确认页面使用
    Taro.setStorageSync('selectedAddress', address)

    // 返回订单确认页面
    Taro.navigateBack()
  }

  handleSetDefault = async (addressId: string) => {
    try {
      // TODO: 实际项目中这里应该调用设置默认地址的API
      // await setDefaultAddress(addressId)

      // 更新本地状态
      this.setState({
        addresses: this.state.addresses.map(addr => ({
          ...addr,
          isDefault: addr.id === addressId
        }))
      })

      this.showToast('设置默认地址成功', 'success')
    } catch (error) {
      console.error('设置默认地址失败:', error)
      this.showToast('设置默认地址失败', 'error')
    }
  }

  handleDeleteClick = (addressId: string) => {
    this.setState({
      deleteModalVisible: true,
      deleteAddressId: addressId
    })
  }

  handleDeleteConfirm = async () => {
    const { deleteAddressId } = this.state

    if (!deleteAddressId) return

    try {
      // TODO: 实际项目中这里应该调用删除地址的API
      // await deleteAddress(deleteAddressId)

      // 从本地状态中移除地址
      this.setState({
        addresses: this.state.addresses.filter(addr => addr.id !== deleteAddressId),
        deleteModalVisible: false,
        deleteAddressId: null
      })

      this.showToast('删除地址成功', 'success')
    } catch (error) {
      console.error('删除地址失败:', error)
      this.showToast('删除地址失败', 'error')
    }
  }

  handleDeleteCancel = () => {
    this.setState({
      deleteModalVisible: false,
      deleteAddressId: null
    })
  }

  handleWechatImport = () => {
    // 微信地址导入功能
    Taro.chooseAddress({
      success: (res) => {
        const newAddress: Partial<Address> = {
          receiverName: res.userName || '',
          phone: res.telNumber || '',
          province: res.provinceName || '',
          city: res.cityName || '',
          district: res.countyName || '',
          detailAddress: `${res.detailInfo || ''}`.trim(),
          postalCode: res.postalCode || '',
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

  render() {
    const {
      addresses,
      loading,
      isSelectMode,
      deleteModalVisible,
      showToast,
      toastText,
      toastType
    } = this.state

    return (
      <View className='address-list-page'>
        {/* 微信地址导入按钮 */}
        <View className='import-section'>
          <AtButton
            type='secondary'
            size='small'
            onClick={this.handleWechatImport}
            className='import-button'
          >
            <AtIcon value='download' size='14' color='#667eea' />
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
              <AtCard
                key={address.id}
                className='address-card'
              >
                <View className='address-content'>
                  {address.isDefault && (
                    <View className='default-badge'>
                      <Text>默认</Text>
                    </View>
                  )}

                  <View className='address-header'>
                    <Text className='receiver-name'>{address.receiverName}</Text>
                    <Text className='receiver-phone'>{address.phone}</Text>
                  </View>

                  <Text className='address-detail'>
                    {address.province} {address.city} {address.district} {address.detailAddress}
                  </Text>

                  <View className='address-footer'>
                    {isSelectMode ? (
                      <AtButton
                        type='primary'
                        size='small'
                        onClick={() => this.handleSelectAddress(address)}
                        className='select-button'
                      >
                        选择此地址
                      </AtButton>
                    ) : (
                      <View className='action-buttons'>
                        {!address.isDefault && (
                          <AtButton
                            type='secondary'
                            size='small'
                            onClick={() => this.handleSetDefault(address.id)}
                            className='default-button'
                          >
                            设为默认
                          </AtButton>
                        )}

                        <AtButton
                          type='secondary'
                          size='small'
                          onClick={() => this.handleEditAddress(address.id)}
                          className='edit-button'
                        >
                          <AtIcon value='edit' size='14' color='#666' />
                          <Text>编辑</Text>
                        </AtButton>

                        <AtButton
                          type='secondary'
                          size='small'
                          onClick={() => this.handleDeleteClick(address.id)}
                          className='delete-button'
                        >
                          <AtIcon value='trash' size='14' color='#ff6b35' />
                          <Text>删除</Text>
                        </AtButton>
                      </View>
                    )}
                  </View>
                </View>
              </AtCard>
            ))
          )}
        </View>

        {/* 底部添加按钮 */}
        <View className='bottom-actions'>
          <AtButton
            type='primary'
            size='normal'
            onClick={this.handleAddAddress}
            className='add-button'
          >
            <AtIcon value='add' size='16' color='white' />
            <Text>新增收货地址</Text>
          </AtButton>
        </View>

        {/* 删除确认弹窗 */}
        <AtModal
          isOpened={deleteModalVisible}
          onClose={this.handleDeleteCancel}
        >
          <AtModalHeader>确认删除</AtModalHeader>
          <AtModalContent>
            <Text>确定要删除这个地址吗？删除后无法恢复。</Text>
          </AtModalContent>
          <AtModalAction>
            <Button onClick={this.handleDeleteCancel}>取消</Button>
            <Button onClick={this.handleDeleteConfirm}>删除</Button>
          </AtModalAction>
        </AtModal>

        {/* Toast 提示 */}
        <AtToast
          isOpened={showToast}
          text={toastText}
          status={toastType}
          onClose={this.hideToast}
        />

        {/* 底部安全区域 */}
        <View className='safe-bottom' />
      </View>
    )
  }
}