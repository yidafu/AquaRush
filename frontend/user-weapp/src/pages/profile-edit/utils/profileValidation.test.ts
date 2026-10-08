import { validateNickname, validatePhone, validateProfileForm } from './profileValidation'

describe('validateNickname', () => {
  it('accepts a valid nickname', () => {
    expect(validateNickname('张三')).toBe('')
    expect(validateNickname('user_01')).toBe('')
  })

  it('rejects empty nickname', () => {
    expect(validateNickname('')).toBe('请输入昵称')
    expect(validateNickname('   ')).toBe('请输入昵称')
  })

  it('rejects nickname shorter than 2 chars', () => {
    expect(validateNickname('张')).toBe('昵称至少2个字符')
  })

  it('rejects nickname longer than 20 chars', () => {
    expect(validateNickname('张'.repeat(21))).toBe('昵称不能超过20个字符')
  })

  it('rejects illegal characters', () => {
    expect(validateNickname('abc!@#')).toBe('昵称只能包含中文、英文、数字、下划线和空格')
  })
})

describe('validatePhone', () => {
  it('accepts a valid phone', () => {
    expect(validatePhone('13800138000')).toBe('')
  })

  it('rejects empty phone', () => {
    expect(validatePhone('')).toBe('请输入手机号')
  })

  it('rejects malformed phone', () => {
    expect(validatePhone('123456')).toBe('请输入正确的手机号格式')
    expect(validatePhone('23800138000')).toBe('请输入正确的手机号格式')
  })
})

describe('validateProfileForm', () => {
  it('returns no errors for a valid form', () => {
    const errors = validateProfileForm({ nickname: '张三', phone: '13800138000' })
    expect(Object.keys(errors).length).toBe(0)
  })

  it('collects both field errors', () => {
    const errors = validateProfileForm({ nickname: '', phone: '' })
    expect(errors.nickname).toBe('请输入昵称')
    expect(errors.phone).toBe('请输入手机号')
  })
})
