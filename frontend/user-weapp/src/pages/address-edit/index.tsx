import React, { useState, useEffect, useCallback, useRef } from 'react'
import { View, Text, Picker } from '@tarojs/components'
import { AtButton, AtForm, AtInput, AtSwitch, AtToast, AtMessage, AtList, AtListItem, AtActivityIndicator } from 'taro-ui'
import Taro from '@tarojs/taro'
import { useRegionSelector } from '../../hooks/useRegionSelector'
import AddressMap from '../../components/AddressMap'
import AddressService from '../../services/AddressService'
import { AddressFormData } from '../../types/address'
import {
  AddressInput as GraphQLAddressInput,
  UpdateAddressInput as GraphQLUpdateAddressInput
} from '@aquarush/common'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

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

  const validateForm = useCallback((): { valid: boolean; firstError?: string } => {
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

    // 楼层验证(选填;填了必须 ≥ 1,配送费楼层判定用 —— R-MON-02)
    if (formData.floor !== undefined && formData.floor !== null && formData.floor < 1) {
      newErrors.floor = '楼层必须大于0'
    }


    setErrors(newErrors as Record<keyof AddressFormData, string>)
    const firstError = (Object.values(newErrors) as string[]).find((msg) => !!msg)
    return { valid: Object.keys(newErrors).length === 0, firstError }
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
      <AtForm className='address-form'>


        {/* 收货人 */}
        <AtInput
          name='receiverName'
          title='收货人'
          type='text'
          placeholder='请输入收货人姓名'
          value={formData.receiverName}
          onChange={(value) => handleInputChange('receiverName', value)}
          error={!!errors.receiverName}
        />
        {errors.receiverName && <Text className='input-hint error-text'>{errors.receiverName}</Text>}

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
        {errors.phone && <Text className='input-hint error-text'>{errors.phone}</Text>}

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
        {(errors.province || errors.city || errors.district) && (
          <Text className='input-hint error-text'>{errors.province || errors.city || errors.district}</Text>
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
          error={!!errors.detailAddress}
        />
        {errors.detailAddress ? (
          <Text className='input-hint error-text'>{errors.detailAddress}</Text>
        ) : (
          <Text className='input-hint'>请输入详细的收货地址，不少于5个字符</Text>
        )}

        {/* 楼层(配送费判定用:超过免费楼层加收运费) */}
        <AtInput
          name='floor'
          title='楼层'
          type='number'
          placeholder='选填,如 5'
          value={formData.floor !== undefined ? String(formData.floor) : ''}
          onChange={(value) => {
            const str = typeof value === 'number' ? value.toString() : value
            const floor = str.trim() === '' ? undefined : parseInt(str, 10)
            setFormData(prev => ({
              ...prev,
              floor: floor === undefined || isNaN(floor) ? undefined : floor
            }))
            setErrors(prev => (prev.floor ? { ...prev, floor: '' } : prev))
          }}
          error={!!errors.floor}
        />
        <Text className='input-hint'>配送费按楼层计算:免费楼层内免运费,超过免费楼层加收运费</Text>

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
            trackTarget="address-save"
            trackAction="FORM_SUBMIT"
            trackElementType="button"
            trackElementText="保存地址"
            trackDisabled={loading}
            onClick={handleSubmit}
            type='primary'
            size='normal'
            loading={loading}
            disabled={loading}
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
