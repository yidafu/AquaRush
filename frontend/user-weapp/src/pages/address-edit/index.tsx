import React, { useState, useEffect, useCallback, useRef } from 'react'
import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useRegionSelector } from '../../hooks/useRegionSelector'
import AddressService from '../../services/AddressService'
import { AddressFormData } from '../../types/address'
import {
  AddressInput as GraphQLAddressInput,
  UpdateAddressInput as GraphQLUpdateAddressInput
} from '@aquarush/common'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import { validateAddressForm } from './utils/addressValidation'

// 子组件
import ContactFields from './components/ContactFields'
import RegionPickerField from './components/RegionPickerField'
import DetailAddressField from './components/DetailAddressField'
import FloorField from './components/FloorField'
import MapPreviewSection from './components/MapPreviewSection'
import SubmitSection from './components/SubmitSection'

import './index.scss'

const AddressEdit: React.FC = () => {
  usePageViewTracking()

  const [formData, setFormData] = useState<AddressFormData>({
    receiverName: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    isDefault: false,
    floor: undefined
  })

  const [errors, setErrors] = useState<Record<keyof AddressFormData, string>>({} as Record<keyof AddressFormData, string>)
  const [loading, setLoading] = useState(false)
  const [isEdit, setIsEdit] = useState(false)
  const [addressId, setAddressId] = useState<string | null>(null)
  const [showToast, setShowToast] = useState(false)
  const [toastText, setToastText] = useState('')
  const [toastType, setToastType] = useState<'success' | 'error' | 'loading'>('success')

  // 是否为「新增地址」模式(无 id 且非微信导入):路由参数同步可得,用于
  // 地区级联加载,避免依赖异步 setState 的 isEdit 造成竞态
  const isCreateModeRef = useRef(false)
  // 地区级联加载步骤:0 未开始 / 1 城市已请求 / 2 区县已请求 / 3 完成
  const regionCascadeStepRef = useRef(0)

  // Initialize services
  const addressService = AddressService.getInstance()

  // Initialize region selector
  const regionSelector = useRegionSelector({
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

  // 楼层变更:更新 formData 并清除楼层错误
  const handleFloorChange = useCallback((floor: number | undefined) => {
    setFormData(prev => ({ ...prev, floor }))
    setErrors(prev => (prev.floor ? { ...prev, floor: '' } : prev))
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

    // 地区变化时清除对应校验错误
    setErrors(prev => {
      if (!prev.province && !prev.city && !prev.district) return prev
      return { ...prev, province: '', city: '', district: '' }
    })
  }, [regionSelector.selectedProvince?.code, regionSelector.selectedCity?.code, regionSelector.selectedDistrict?.code])

  // 新增地址:省份自动选中后级联加载城市/区县(F-ADR-002)。
  // useRegionSelector 首次仅自动加载省份并选中第一项;城市/区县需滑动
  // 选择器列才加载。直接打开选择器点「确定」时城市/区县为空 → 表单校验
  // 静默失败(无请求、无提示)。此处为新增流程自动补齐级联数据,使选择器
  // 首次打开即有完整数据且默认选中,保存可直接提交。
  useEffect(() => {
    if (isCreateModeRef.current !== true) return
    const r = regionSelector
    const step = regionCascadeStepRef.current

    if (step === 0 && r.selectedProvince && r.cities.length === 0) {
      // 第一步:为自动选中的首个省份加载城市
      regionCascadeStepRef.current = 1
      r.handleColumnChange({ detail: { column: 0, value: r.selectedIndex.province } })
    } else if (step === 1 && r.selectedCity && r.districts.length === 0) {
      // 第二步:为自动选中的首个城市加载区县
      regionCascadeStepRef.current = 2
      r.handleColumnChange({ detail: { column: 1, value: r.selectedIndex.city } })
    } else if (step === 1 || step === 2) {
      // 城市/区县已就绪(或该省份/城市无下级数据),级联完成
      regionCascadeStepRef.current = 3
    }
  }, [
    regionSelector.selectedProvince?.code,
    regionSelector.selectedCity?.code,
    regionSelector.cities.length,
    regionSelector.districts.length
  ])

  const loadAddressDetail = useCallback(async (addressId: string) => {
    try {
      setLoading(true)
      showToastMessage('正在加载地址详情...', 'loading')

      const result = await addressService.getAddressDetail(addressId)

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
        longitude: address.longitude,
        floor: address.floor ?? undefined
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

  // 校验(纯函数见 utils/addressValidation,失败返回首个错误供 toast 提示)
  const validateForm = useCallback((): { valid: boolean; firstError?: string } => {
    const { valid, errors: newErrors, firstError } = validateAddressForm(formData)
    setErrors(newErrors)
    return { valid, firstError }
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
    const { valid, firstError } = validateForm()
    if (!valid) {
      // 校验失败:toast 提示首个错误,避免「点击保存无任何反应」的静默失败
      // (此前地区未选中/错误不可见,导致点击只上报 track、不发地址请求)
      showToastMessage(firstError || '请完善地址信息', 'error')
      return
    }

    setLoading(true)

    try {
      // Transform form data directly to GraphQL input format
      if (isEdit && addressId) {
        // Update existing address
        const updateInput: GraphQLUpdateAddressInput = {
          receiverName: formData.receiverName,
          phone: formData.phone,
          province: formData.province,
          provinceCode: formData.provinceCode || '',
          city: formData.city,
          cityCode: formData.cityCode || '',
          district: formData.district,
          districtCode: formData.districtCode || '',
          detailAddress: formData.detailAddress,
          floor: formData.floor,
          longitude: formData.longitude,
          latitude: formData.latitude,
          isDefault: formData.isDefault
        }

        console.log('更新地址:', { id: addressId, updateInput })
        const result = await addressService.updateAddress(addressId, updateInput)

        if (!result.success) {
          showToastMessage(result.error?.message || '地址更新失败，请重试', 'error')
          return
        }
      } else {
        // Create new address
        const createInput: GraphQLAddressInput = {
          receiverName: formData.receiverName,
          phone: formData.phone,
          province: formData.province,
          city: formData.city,
          district: formData.district,
          detailAddress: formData.detailAddress,
          floor: formData.floor,
          longitude: formData.longitude || undefined,
          latitude: formData.latitude || undefined,
          isDefault: formData.isDefault,
          provinceCode: formData.provinceCode || '',
          cityCode: formData.cityCode || '',
          districtCode: formData.districtCode || ''
        }

        console.log('创建地址:', createInput)
        const result = await addressService.createAddress(createInput)

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

    // 新增地址模式(无 id 且非微信导入)才级联加载地区;编辑/微信导入跳过,
    // 避免覆盖编辑回显或微信预填的省市区
    isCreateModeRef.current = !id && fromWechat !== 'true'

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
      {/* 收货人 + 手机号 + 所在地区 + 详细地址(address-edit.html 第一组 cells) */}
      <View className='weui-cells mt2'>
        <ContactFields
          receiverName={formData.receiverName}
          phone={formData.phone}
          errors={errors}
          onChange={handleInputChange}
        />

        {/* 地区选择(省市区级联) */}
        <RegionPickerField
          provinces={regionSelector.provinces}
          cities={regionSelector.cities}
          districts={regionSelector.districts}
          selectedIndex={regionSelector.selectedIndex}
          loading={regionSelector.loading}
          getFullRegionText={regionSelector.getFullRegionText}
          handleColumnChange={regionSelector.handleColumnChange}
          handlePickerChange={regionSelector.handlePickerChange}
          error={errors.province || errors.city || errors.district}
        />

        {/* 详细地址 */}
        <DetailAddressField
          value={formData.detailAddress}
          error={errors.detailAddress}
          onChange={(value) => handleInputChange('detailAddress', value)}
        />
      </View>

      {/* 楼层(配送费判定用:超过免费楼层加收运费) */}
      <View className='weui-cells mt3'>
        <FloorField
          value={formData.floor !== undefined ? String(formData.floor) : ''}
          error={errors.floor}
          onChange={handleFloorChange}
        />
      </View>

      {/* 地址地图预览(地图选点) */}
      <MapPreviewSection
        detailAddress={formData.detailAddress}
        province={formData.province}
        city={formData.city}
        district={formData.district}
        latitude={formData.latitude}
        longitude={formData.longitude}
        onLocationChange={handleMapLocationChange}
      />

      {/* 设为默认地址 + 保存按钮 */}
      <SubmitSection
        isDefault={formData.isDefault}
        loading={loading}
        onDefaultChange={(checked) => handleInputChange('isDefault', checked)}
        onSubmit={handleSubmit}
      />

      {/* Toast 提示(.weui-toast_text 深色轻提示;toastType 保留原状态语义) */}
      {showToast && (
        <View className={`address-toast address-toast--${toastType} weui-toast weui-toast_text`} onClick={hideToast}>
          <Text className='weui-toast__text'>{toastText}</Text>
        </View>
      )}

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default AddressEdit
