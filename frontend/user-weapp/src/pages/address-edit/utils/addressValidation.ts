import { AddressFormData } from '../../../types/address'

export type AddressFormErrors = Record<keyof AddressFormData, string>

export interface AddressValidationResult {
  valid: boolean
  errors: AddressFormErrors
  /** 首个错误文案(校验失败时提示用,避免「点击保存无任何反应」的静默失败) */
  firstError?: string
}

/**
 * 地址表单校验(新增/编辑地址页共用,纯函数便于单测):
 * - 收货人:必填,2-20 字符
 * - 手机号:必填,中国大陆 1[3-9] 开头 11 位
 * - 省/市/区:必选
 * - 详细地址:必填,5-100 字符
 * - 楼层:选填,填了必须 ≥ 1(配送费楼层判定用 —— R-MON-02)
 */
export const validateAddressForm = (formData: AddressFormData): AddressValidationResult => {
  const errors = {} as AddressFormErrors

  // 收货人姓名验证
  if (!formData.receiverName.trim()) {
    errors.receiverName = '请输入收货人姓名'
  } else if (formData.receiverName.length < 2) {
    errors.receiverName = '收货人姓名至少2个字符'
  } else if (formData.receiverName.length > 20) {
    errors.receiverName = '收货人姓名不能超过20个字符'
  }

  // 手机号验证
  if (!formData.phone.trim()) {
    errors.phone = '请输入手机号'
  } else if (!/^1[3-9]\d{9}$/.test(formData.phone)) {
    errors.phone = '手机号格式不正确'
  }

  // 地区验证
  if (!formData.province.trim()) {
    errors.province = '请选择省份'
  }

  if (!formData.city.trim()) {
    errors.city = '请选择城市'
  }

  if (!formData.district.trim()) {
    errors.district = '请选择区域'
  }

  // 详细地址验证
  if (!formData.detailAddress.trim()) {
    errors.detailAddress = '请输入详细地址'
  } else if (formData.detailAddress.length < 5) {
    errors.detailAddress = '详细地址至少5个字符'
  } else if (formData.detailAddress.length > 100) {
    errors.detailAddress = '详细地址不能超过100个字符'
  }

  // 楼层验证(选填;填了必须 ≥ 1,配送费楼层判定用 —— R-MON-02)
  if (formData.floor !== undefined && formData.floor !== null && formData.floor < 1) {
    errors.floor = '楼层必须大于0'
  }

  const firstError = (Object.values(errors) as string[]).find((msg) => !!msg)
  return { valid: Object.keys(errors).length === 0, errors, firstError }
}
