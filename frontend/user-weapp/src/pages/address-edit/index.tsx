import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, Picker } from '@tarojs/components'
import { AtButton, AtForm, AtInput, AtSwitch, AtToast, AtMessage, AtList, AtListItem, AtActivityIndicator } from 'taro-ui'
import Taro from '@tarojs/taro'
import { useRegionSelector } from '../../hooks/useRegionSelector'
import AddressMap from '../../components/AddressMap'
import AddressService from '../../services/AddressService'
import { AddressFormData, transformFormToGraphQLInput } from '../../types/address'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/form.scss"
import "taro-ui/dist/style/components/input.scss"
import "taro-ui/dist/style/components/switch.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/message.scss"
import "taro-ui/dist/style/components/activity-indicator.scss"
import "taro-ui/dist/style/components/icon.scss"
import './index.scss'

const AddressEdit: React.FC = () => {
  const [formData, setFormData] = useState<AddressFormData>({
    receiverName: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    isDefault: false
  })

  const [errors, setErrors] = useState<Record<keyof AddressFormData, string>>({} as Record<keyof AddressFormData, string>)
  const [loading, setLoading] = useState(false)
  const [isEdit, setIsEdit] = useState(false)
  const [addressId, setAddressId] = useState<string | null>(null)
  const [showToast, setShowToast] = useState(false)
  const [toastText, setToastText] = useState('')
  const [toastType, setToastType] = useState<'success' | 'error' | 'loading'>('success')

  // Initialize services
  const addressService = AddressService.getInstance()

  // Initialize region selector
  const regionSelector = useRegionSelector({
    // initialSelection: initialRegionSelection,
    onError: (error) => {
      showToastMessage(`地区加载失败: ${error}`, 'error')
    }
  })

  const showToastMessage = useCallback((text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    setShowToast(true)
    setToastText(text)
    setToastType(type)
  }, [])

  const hideToast = useCallback(() => {
    setShowToast(false)
  }, [])

  // Handle map location changes from AddressMap component
  const handleMapLocationChange = useCallback((location: {
    latitude: number
    longitude: number
    address: string
  }) => {
    setFormData(prev => ({
      ...prev,
      latitude: location.latitude,
      longitude: location.longitude
    }))
    showToastMessage('地图位置已更新', 'success')
  }, [showToastMessage])

  const handleInputChange = useCallback((field: keyof AddressFormData, value: string | number | boolean) => {
    // AtInput 的 onChange 可能返回 number，需要转换为 string
    const processedValue = typeof value === 'number' ? value.toString() : value

    setFormData(prev => {
      // 检查值是否真的发生了变化
      if (prev[field] === processedValue) {
        return prev
      }
      return {
        ...prev,
        [field]: processedValue
      }
    })

    // 清除对应字段的错误信息
    setErrors(prev => {
      if (!prev[field]) return prev
      return {
        ...prev,
        [field]: ''
      }
    })
  }, [])

  // Sync form data with region selection changes - 优化：减少不必要的重新渲染
  useEffect(() => {
    const selectedNames = regionSelector.getSelectedNames()
    const selectedCodes = regionSelector.getSelectedCodes()

    setFormData(prev => {
      // 检查是否真的需要更新
      const hasChanges =
        prev.province !== (selectedNames.province || '') ||
        prev.city !== (selectedNames.city || '') ||
        prev.district !== (selectedNames.district || '') ||
        prev.provinceCode !== selectedCodes.provinceCode ||
        prev.cityCode !== selectedCodes.cityCode ||
        prev.districtCode !== selectedCodes.districtCode

      if (!hasChanges) return prev

      return {
        ...prev,
        province: selectedNames.province || '',
        city: selectedNames.city || '',
        district: selectedNames.district || '',
        provinceCode: selectedCodes.provinceCode,
        cityCode: selectedCodes.cityCode,
        districtCode: selectedCodes.districtCode
      }
    })
  }, [regionSelector.selectedProvince?.code, regionSelector.selectedCity?.code, regionSelector.selectedDistrict?.code])

  const loadAddressDetail = useCallback(async (addressId: string) => {
    try {
      setLoading(true)
      showToastMessage('正在加载地址详情...', 'loading')

      const result = await addressService.getAddressDetail(parseInt(addressId))

      if (!result.success || !result.data) {
        showToastMessage(result.error?.message || '地址不存在', 'error')
        setTimeout(() => {
          Taro.navigateBack()
        }, 1500)
        return
      }

      const address = result.data

      // Set initial region selection for the selector
      if (address.provinceCode || address.cityCode || address.districtCode) {
        regionSelector.initializeWithSelection({
          provinceCode: address.provinceCode,
          cityCode: address.cityCode,
          districtCode: address.districtCode
        })
      }

      // Update form data with fetched address
      setFormData(prev => ({
        ...prev,
        receiverName: address.receiverName || '',
        phone: address.phone || '',
        province: address.province || '',
        city: address.city || '',
        district: address.district || '',
        provinceCode: address.provinceCode,
        cityCode: address.cityCode,
        districtCode: address.districtCode,
        detailAddress: address.detailAddress || '',
        isDefault: address.isDefault || false,
        latitude: address.latitude,
        longitude: address.longitude
      }))

      showToastMessage('地址详情加载成功', 'success')
    } catch (error) {
      console.error('获取地址详情失败:', error)
      showToastMessage('获取地址详情失败', 'error')
      setTimeout(() => {
        Taro.navigateBack()
      }, 1500)
    } finally {
      setLoading(false)
    }
  }, [addressService, showToastMessage])

  const loadWechatAddress = useCallback(() => {
    try {
      const wechatAddress = Taro.getStorageSync('wechatAddress')
      if (wechatAddress) {
        setFormData(wechatAddress)
        // 清除缓存的微信地址
        Taro.removeStorageSync('wechatAddress')
      }
    } catch (error) {
      console.error('加载微信地址失败:', error)
    }
  }, [])

  const validateForm = useCallback((): boolean => {
    const newErrors: Partial<Record<keyof AddressFormData, string>> = {}

    // 收货人姓名验证
    if (!formData.receiverName.trim()) {
      newErrors.receiverName = '请输入收货人姓名'
    } else if (formData.receiverName.length < 2) {
      newErrors.receiverName = '收货人姓名至少2个字符'
    } else if (formData.receiverName.length > 20) {
      newErrors.receiverName = '收货人姓名不能超过20个字符'
    }

    // 手机号验证
    if (!formData.phone.trim()) {
      newErrors.phone = '请输入手机号'
    } else if (!/^1[3-9]\d{9}$/.test(formData.phone)) {
      newErrors.phone = '手机号格式不正确'
    }

    // 地区验证
    if (!formData.province.trim()) {
      newErrors.province = '请选择省份'
    }

    if (!formData.city.trim()) {
      newErrors.city = '请选择城市'
    }

    if (!formData.district.trim()) {
      newErrors.district = '请选择区域'
    }

    // 详细地址验证
    if (!formData.detailAddress.trim()) {
      newErrors.detailAddress = '请输入详细地址'
    } else if (formData.detailAddress.length < 5) {
      newErrors.detailAddress = '详细地址至少5个字符'
    } else if (formData.detailAddress.length > 100) {
      newErrors.detailAddress = '详细地址不能超过100个字符'
    }


    setErrors(newErrors as Record<keyof AddressFormData, string>)
    return Object.keys(newErrors).length === 0
  }, [formData])

  // Handle map selection results
  useEffect(() => {
    const checkMapSelection = () => {
      const selectedLocation = Taro.getStorageSync('selectedLocation')
      if (selectedLocation) {
        // Update form with selected coordinates
        setFormData(prev => ({
          ...prev,
          latitude: selectedLocation.latitude,
          longitude: selectedLocation.longitude,
          detailAddress: selectedLocation.address && selectedLocation.address.trim() ? selectedLocation.address : prev.detailAddress
        }))

        // Clear the stored location
        Taro.removeStorageSync('selectedLocation')

        showToastMessage('已选择地图位置', 'success')
      }
    }

    checkMapSelection()
  }, [showToastMessage])

  const handleSubmit = useCallback(async () => {
    if (!validateForm()) {
      return
    }

    setLoading(true)

    try {
      // Transform form data to GraphQL input format
      // Note: receiverName and phone are excluded as they're not supported in backend schema
      // TODO: Update this when backend schema supports receiverName and phone fields
      const graphqlInput = transformFormToGraphQLInput(formData)

      if (isEdit && addressId) {
        // Update existing address
        console.log('更新地址:', { id: addressId, graphqlInput })
        const result = await addressService.updateAddress(parseInt(addressId), graphqlInput)

        if (!result.success) {
          showToastMessage(result.error?.message || '地址更新失败，请重试', 'error')
          return
        }
      } else {
        // Create new address
        console.log('创建地址:', graphqlInput)
        const result = await addressService.createAddress(graphqlInput)

        if (!result.success) {
          showToastMessage(result.error?.message || '地址添加失败，请重试', 'error')
          return
        }
      }

      showToastMessage(isEdit ? '地址更新成功' : '地址添加成功', 'success')

      // 延迟返回上一页
      setTimeout(() => {
        Taro.navigateBack()
      }, 1500)
    } catch (error) {
      console.error('保存地址失败:', error)
      showToastMessage('保存失败，请重试', 'error')
    } finally {
      setLoading(false)
    }
  }, [validateForm, isEdit, addressId, formData, showToastMessage, addressService])

  // 初始化
  useEffect(() => {
    // 获取页面参数
    const instance = Taro.getCurrentInstance()
    const { id, fromWechat } = instance.router?.params || {}

    if (id) {
      setIsEdit(true)
      setAddressId(id)
      loadAddressDetail(id)
    } else if (fromWechat === 'true') {
      // 从微信地址导入
      loadWechatAddress()
    }

    // 设置导航栏标题
    Taro.setNavigationBarTitle({
      title: id ? '编辑地址' : '新增地址'
    })
  }, [loadAddressDetail, loadWechatAddress])

  return (
    <View className='address-edit-page'>
      <AtForm className='address-form'>


        {/* 收货人 */}
        <AtInput
          name='receiverName'
          title='收货人'
          type='text'
          placeholder='请输入收货人姓名'
          value={formData.receiverName}
          onChange={(value) => handleInputChange('receiverName', value)}
        />

        {/* 手机号 */}
        <AtInput
          name='phone'
          title='手机号'
          type='phone'
          placeholder='请输入手机号'
          value={formData.phone}
          onChange={(value) => handleInputChange('phone', value)}
          error={!!errors.phone}
        />

        {/* 地区选择 */}
        {regionSelector.provinces.length > 0 ? (
          <Picker
            mode='multiSelector'
            range={[
              regionSelector.provinces.map(p => p.name),
              regionSelector.cities.map(c => c.name),
              regionSelector.districts.map(d => d.name)
            ]}
            value={[
              regionSelector.selectedIndex.province,
              regionSelector.selectedIndex.city,
              regionSelector.selectedIndex.district
            ]}
            onColumnChange={regionSelector.handleColumnChange}
            onChange={regionSelector.handlePickerChange}
          >
            <AtList>
              <AtListItem
                title='所在地区'
                extraText={regionSelector.getFullRegionText() || '请选择省市区'}
                arrow='right'
              />
            </AtList>
          </Picker>
        ) : (
          <AtList>
            <AtListItem
              title='所在地区'
              extraText='正在加载地区数据...'
              arrow='right'
              disabled={true}
            />
          </AtList>
        )}

        {/* Loading indicator for regions */}
        {(regionSelector.loading.provinces || regionSelector.loading.cities || regionSelector.loading.districts) && (
          <View className='region-loading'>
            <AtActivityIndicator content='加载地区数据...' />
          </View>
        )}

        {/* 详细地址 */}
        <AtInput
          name='detailAddress'
          title='详细地址'
          type='text'
          placeholder='街道、门牌号、小区、楼号等详细信息'
          value={formData.detailAddress}
          onChange={(value) => handleInputChange('detailAddress', value)}
          maxlength={100}
        />
        <Text className='input-hint'>请输入详细的收货地址，不少于5个字符</Text>

        {/* 地址地图预览 */}
        <View className='address-map-section'>
          <Text className='section-title'>地址预览</Text>
          <AddressMap
            address={formData.detailAddress}
            province={formData.province}
            city={formData.city}
            district={formData.district}
            latitude={formData.latitude}
            longitude={formData.longitude}
            height='200px'
            className='address-edit-map'
            showControls={true}
            onLocationChange={handleMapLocationChange}
          />
          <Text className='map-hint'>地图会根据地址信息自动定位，也可以手动选择位置</Text>
        </View>


        {/* 设为默认地址 */}
        <AtSwitch
          title='设为默认地址'
          checked={formData.isDefault}
          onChange={(value) => handleInputChange('isDefault', value)}
        />


        {/* 提交按钮 */}
        <View className='submit-section'>
          <AtButton
            type='primary'
            size='normal'
            loading={loading}
            disabled={loading}
            onClick={handleSubmit}
            className='submit-button'
          >
            {loading ? '保存中...' : '保存地址'}
          </AtButton>
        </View>

      </AtForm>


      {/* Toast 提示 */}
      <AtToast
        isOpened={showToast}
        text={toastText}
        status={toastType}
        onClose={hideToast}
      />

      {/* 全局消息 */}
      <AtMessage />

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default AddressEdit
