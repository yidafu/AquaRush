import { validateAddressForm } from './addressValidation'
import { AddressFormData } from '../../../types/address'

const validForm: AddressFormData = {
  receiverName: '张三',
  phone: '13800138000',
  province: '广东省',
  city: '深圳市',
  district: '南山区',
  detailAddress: '科技园南区深南大道9988号',
  isDefault: false
}

describe('validateAddressForm', () => {
  it('returns valid for a complete form', () => {
    const result = validateAddressForm(validForm)
    expect(result.valid).toBe(true)
    expect(result.firstError).toBeUndefined()
  })

  it('rejects empty receiver name', () => {
    const result = validateAddressForm({ ...validForm, receiverName: '' })
    expect(result.valid).toBe(false)
    expect(result.errors.receiverName).toBe('请输入收货人姓名')
    expect(result.firstError).toBe('请输入收货人姓名')
  })

  it('rejects receiver name shorter than 2 chars', () => {
    const result = validateAddressForm({ ...validForm, receiverName: '张' })
    expect(result.errors.receiverName).toBe('收货人姓名至少2个字符')
  })

  it('rejects receiver name longer than 20 chars', () => {
    const result = validateAddressForm({ ...validForm, receiverName: '张'.repeat(21) })
    expect(result.errors.receiverName).toBe('收货人姓名不能超过20个字符')
  })

  it('rejects empty phone', () => {
    const result = validateAddressForm({ ...validForm, phone: '' })
    expect(result.errors.phone).toBe('请输入手机号')
  })

  it('rejects malformed phone', () => {
    const result = validateAddressForm({ ...validForm, phone: '12345' })
    expect(result.errors.phone).toBe('手机号格式不正确')
  })

  it('rejects missing province/city/district', () => {
    const result = validateAddressForm({ ...validForm, province: '', city: '', district: '' })
    expect(result.errors.province).toBe('请选择省份')
    expect(result.errors.city).toBe('请选择城市')
    expect(result.errors.district).toBe('请选择区域')
  })

  it('rejects empty detail address', () => {
    const result = validateAddressForm({ ...validForm, detailAddress: '' })
    expect(result.errors.detailAddress).toBe('请输入详细地址')
  })

  it('rejects detail address shorter than 5 chars', () => {
    const result = validateAddressForm({ ...validForm, detailAddress: '短' })
    expect(result.errors.detailAddress).toBe('详细地址至少5个字符')
  })

  it('rejects floor below 1 when provided', () => {
    const result = validateAddressForm({ ...validForm, floor: 0 })
    expect(result.errors.floor).toBe('楼层必须大于0')
  })

  it('accepts undefined floor', () => {
    const result = validateAddressForm({ ...validForm, floor: undefined })
    expect(result.errors.floor).toBeUndefined()
    expect(result.valid).toBe(true)
  })

  it('returns first error in field order (receiverName first)', () => {
    const result = validateAddressForm({
      receiverName: '',
      phone: '',
      province: '',
      city: '',
      district: '',
      detailAddress: '',
      isDefault: false
    })
    expect(result.firstError).toBe('请输入收货人姓名')
  })
})
