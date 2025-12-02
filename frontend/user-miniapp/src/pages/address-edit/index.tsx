import React, { useState, useEffect, useCallback } from 'react'
import { View, Text, Input, Button, Switch, Picker } from '@tarojs/components'
import { AtButton, AtForm, AtInput, AtSwitch, AtToast, AtMessage } from 'taro-ui'
import Taro from '@tarojs/taro'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/form.scss"
import "taro-ui/dist/style/components/input.scss"
import "taro-ui/dist/style/components/switch.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/message.scss"
import './index.scss'

interface AddressFormData {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
  postalCode: string
  isDefault: boolean
}

const AddressEdit: React.FC = () => {
  const [formData, setFormData] = useState<AddressFormData>({
    receiverName: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    postalCode: '',
    isDefault: false
  })

  const [errors, setErrors] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(false)
  const [isEdit, setIsEdit] = useState(false)
  const [addressId, setAddressId] = useState<string | null>(null)
  const [showToast, setShowToast] = useState(false)
  const [toastText, setToastText] = useState('')
  const [toastType, setToastType] = useState<'success' | 'error' | 'loading'>('success')
  const [provinceIndex, setProvinceIndex] = useState(0)
  const [cityIndex, setCityIndex] = useState(0)
  const [districtIndex, setDistrictIndex] = useState(0)

  const [provinces] = useState(['广东省', '北京市', '上海市', '江苏省', '浙江省'])
  const [cities, setCities] = useState(['深圳市', '广州市', '珠海市', '佛山市'])
  const [districts, setDistricts] = useState(['南山区', '福田区', '罗湖区', '宝安区'])

  const showToastMessage = useCallback((text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    setShowToast(true)
    setToastText(text)
    setToastType(type)
  }, [])

  const hideToast = useCallback(() => {
    setShowToast(false)
  }, [])

  const handleInputChange = useCallback((field: keyof AddressFormData, value: string | boolean) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }))

    // 清除对应字段的错误信息
    if (errors[field]) {
      setErrors(prev => ({
        ...prev,
        [field]: ''
      }))
    }
  }, [errors])

  // 简化的地区数据获取方法，实际项目中应该从后端获取完整的省市区数据
  const getCitiesByProvince = useCallback((province: string): string[] => {
    const cityMap: Record<string, string[]> = {
      '广东省': ['深圳市', '广州市', '珠海市', '佛山市', '东莞市'],
      '北京市': ['东城区', '西城区', '朝阳区', '海淀区', '丰台区'],
      '上海市': ['黄浦区', '徐汇区', '长宁区', '静安区', '普陀区'],
      '江苏省': ['南京市', '苏州市', '无锡市', '常州市', '南通市'],
      '浙江省': ['杭州市', '宁波市', '温州市', '嘉兴市', '湖州市']
    }
    return cityMap[province] || ['深圳市']
  }, [])

  const getDistrictsByCity = useCallback((city: string): string[] => {
    const districtMap: Record<string, string[]> = {
      '深圳市': ['南山区', '福田区', '罗湖区', '宝安区', '龙岗区'],
      '广州市': ['天河区', '越秀区', '海珠区', '白云区', '番禺区'],
      '珠海市': ['香洲区', '斗门区', '金湾区'],
      '佛山市': ['禅城区', '南海区', '顺德区', '高明区', '三水区'],
      '东莞市': ['莞城区', '南城区', '东城区', '万江区', '石龙镇'],
      '东城区': ['东华门街道', '景山街道', '交道口街道', '安定门街道'],
      '西城区': ['西长安街街道', '新街口街道', '月坛街道', '展览路街道'],
      '朝阳区': ['建国门外街道', '朝外街道', '呼家楼街道', '三里屯街道'],
      '海淀区': ['万寿路街道', '永定路街道', '羊坊店街道', '甘家口街道'],
      '南京市': ['玄武区', '秦淮区', '建邺区', '鼓楼区', '浦口区'],
      '苏州市': ['姑苏区', '虎丘区', '吴中区', '相城区', '吴江区'],
      '杭州市': ['上城区', '下城区', '江干区', '拱墅区', '西湖区']
    }
    return districtMap[city] || ['南山区']
  }, [])

  const loadAddressDetail = useCallback(async (addressId: string) => {
    try {
      // TODO: 实际项目中这里应该调用获取地址详情的API
      // const address = await getAddressDetail(addressId)

      // 模拟地址详情数据
      const mockAddress = {
        id: addressId,
        receiverName: '张三',
        phone: '13800138000',
        province: '广东省',
        city: '深圳市',
        district: '南山区',
        detailAddress: '科技园南区深南大道9988号',
        postalCode: '518000',
        isDefault: true
      }

      // 设置地区选择器的索引
      const pIndex = provinces.indexOf(mockAddress.province)
      const cIndex = cities.indexOf(mockAddress.city)
      const dIndex = districts.indexOf(mockAddress.district)

      setFormData(mockAddress)
      setProvinceIndex(pIndex >= 0 ? pIndex : 0)
      setCityIndex(cIndex >= 0 ? cIndex : 0)
      setDistrictIndex(dIndex >= 0 ? dIndex : 0)
    } catch (error) {
      console.error('获取地址详情失败:', error)
      showToastMessage('获取地址详情失败', 'error')
    }
  }, [provinces, cities, districts, showToastMessage])

  const loadWechatAddress = useCallback(() => {
    try {
      const wechatAddress = Taro.getStorageSync('wechatAddress')
      if (wechatAddress) {
        // 设置地区选择器的索引
        const pIndex = provinces.indexOf(wechatAddress.province)
        const cIndex = cities.indexOf(wechatAddress.city)
        const dIndex = districts.indexOf(wechatAddress.district)

        setFormData(wechatAddress)
        setProvinceIndex(pIndex >= 0 ? pIndex : 0)
        setCityIndex(cIndex >= 0 ? cIndex : 0)
        setDistrictIndex(dIndex >= 0 ? dIndex : 0)

        // 清除缓存的微信地址
        Taro.removeStorageSync('wechatAddress')
      }
    } catch (error) {
      console.error('加载微信地址失败:', error)
    }
  }, [provinces, cities, districts])

  const handleProvinceChange = useCallback((e: any) => {
    const pIndex = e.detail.value
    const province = provinces[pIndex]

    // 根据省份更新城市列表
    const newCities = getCitiesByProvince(province)
    const newDistricts = getDistrictsByCity(newCities[0] || '')

    setProvinceIndex(pIndex)
    setFormData(prev => ({
      ...prev,
      province,
      city: newCities[0] || '',
      district: newDistricts[0] || ''
    }))
    setCityIndex(0)
    setDistrictIndex(0)
    setCities(newCities)
    setDistricts(newDistricts)
  }, [provinces, getCitiesByProvince, getDistrictsByCity])

  const handleCityChange = useCallback((e: any) => {
    const cIndex = e.detail.value
    const city = cities[cIndex]

    // 根据城市更新区县列表
    const newDistricts = getDistrictsByCity(city)

    setCityIndex(cIndex)
    setFormData(prev => ({
      ...prev,
      city,
      district: newDistricts[0] || ''
    }))
    setDistrictIndex(0)
    setDistricts(newDistricts)
  }, [cities, getDistrictsByCity])

  const handleDistrictChange = useCallback((e: any) => {
    const dIndex = e.detail.value
    const district = districts[dIndex]

    setDistrictIndex(dIndex)
    setFormData(prev => ({
      ...prev,
      district
    }))
  }, [districts])

  const validateForm = useCallback((): boolean => {
    const newErrors: Record<string, string> = {}

    if (!formData.receiverName.trim()) {
      newErrors.receiverName = '请输入收货人姓名'
    } else if (formData.receiverName.length < 2) {
      newErrors.receiverName = '收货人姓名至少2个字符'
    } else if (formData.receiverName.length > 20) {
      newErrors.receiverName = '收货人姓名不能超过20个字符'
    }

    if (!formData.phone.trim()) {
      newErrors.phone = '请输入手机号'
    } else if (!/^1[3-9]\d{9}$/.test(formData.phone)) {
      newErrors.phone = '手机号格式不正确'
    }

    if (!formData.province.trim()) {
      newErrors.province = '请选择省份'
    }

    if (!formData.city.trim()) {
      newErrors.city = '请选择城市'
    }

    if (!formData.district.trim()) {
      newErrors.district = '请选择区域'
    }

    if (!formData.detailAddress.trim()) {
      newErrors.detailAddress = '请输入详细地址'
    } else if (formData.detailAddress.length < 5) {
      newErrors.detailAddress = '详细地址至少5个字符'
    } else if (formData.detailAddress.length > 100) {
      newErrors.detailAddress = '详细地址不能超过100个字符'
    }

    if (formData.postalCode && !/^\d{6}$/.test(formData.postalCode)) {
      newErrors.postalCode = '邮政编码格式不正确'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }, [formData])

  const handleSubmit = useCallback(async () => {
    if (!validateForm()) {
      return
    }

    setLoading(true)

    try {
      // TODO: 实际项目中这里应该调用创建或更新地址的API
      if (isEdit && addressId) {
        // await updateAddress(addressId, formData)
        console.log('更新地址:', { id: addressId, ...formData })
      } else {
        // await createAddress(formData)
        console.log('创建地址:', formData)
      }

      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 1000))

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
  }, [validateForm, isEdit, addressId, formData, showToastMessage])

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
        <View className='form-item'>
          <View className='form-label required'>收货人</View>
          <AtInput
            name='receiverName'
            title=''
            type='text'
            placeholder='请输入收货人姓名'
            value={formData.receiverName}
            onChange={(value) => handleInputChange('receiverName', value)}
            error={!!errors.receiverName}
          />
          {errors.receiverName && (
            <Text className='error-text'>{errors.receiverName}</Text>
          )}
        </View>

        {/* 手机号 */}
        <View className='form-item'>
          <View className='form-label required'>手机号</View>
          <AtInput
            name='phone'
            title=''
            type='phone'
            placeholder='请输入手机号'
            value={formData.phone}
            onChange={(value) => handleInputChange('phone', value)}
            error={!!errors.phone}
          />
          {errors.phone && (
            <Text className='error-text'>{errors.phone}</Text>
          )}
        </View>

        {/* 地区选择 */}
        <View className='form-item'>
          <View className='form-label required'>所在地区</View>
          <View className='region-picker'>
            <Picker
              mode='selector'
              range={provinces}
              value={provinceIndex}
              onChange={handleProvinceChange}
              className='picker-item'
            >
              <View className={`picker-display ${errors.province ? 'error' : ''}`}>
                {formData.province || '请选择省份'}
              </View>
            </Picker>

            <Picker
              mode='selector'
              range={cities}
              value={cityIndex}
              onChange={handleCityChange}
              className='picker-item'
            >
              <View className={`picker-display ${errors.city ? 'error' : ''}`}>
                {formData.city || '请选择城市'}
              </View>
            </Picker>

            <Picker
              mode='selector'
              range={districts}
              value={districtIndex}
              onChange={handleDistrictChange}
              className='picker-item'
            >
              <View className={`picker-display ${errors.district ? 'error' : ''}`}>
                {formData.district || '请选择区域'}
              </View>
            </Picker>
          </View>
          {(errors.province || errors.city || errors.district) && (
            <Text className='error-text'>
              {errors.province || errors.city || errors.district || '请选择完整的地区信息'}
            </Text>
          )}
        </View>

        {/* 详细地址 */}
        <View className='form-item'>
          <View className='form-label required'>详细地址</View>
          <Input
            className={`detail-input ${errors.detailAddress ? 'error' : ''}`}
            placeholder='街道、门牌号、小区、楼号等详细信息'
            value={formData.detailAddress}
            onInput={(e) => handleInputChange('detailAddress', e.detail.value)}
            maxlength={100}
          />
          {errors.detailAddress && (
            <Text className='error-text'>{errors.detailAddress}</Text>
          )}
          <Text className='input-hint'>请输入详细的收货地址，不少于5个字符</Text>
        </View>

        {/* 邮政编码 */}
        <View className='form-item'>
          <View className='form-label'>邮政编码</View>
          <AtInput
            name='postalCode'
            title=''
            type='number'
            placeholder='请输入邮政编码（选填）'
            value={formData.postalCode}
            onChange={(value) => handleInputChange('postalCode', value)}
            error={!!errors.postalCode}
          />
          {errors.postalCode && (
            <Text className='error-text'>{errors.postalCode}</Text>
          )}
        </View>

        {/* 设为默认地址 */}
        <View className='form-item switch-item'>
          <View className='form-label'>设为默认地址</View>
          <AtSwitch
            checked={formData.isDefault}
            onChange={(value) => handleInputChange('isDefault', value)}
          />
        </View>
      </AtForm>

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