/**
 * 资料编辑校验(纯函数,便于单测):
 * - 昵称:必填,2-20 字符,仅中文/英文/数字/下划线/空格
 * - 手机号:必填,中国大陆 1[3-9] 开头 11 位
 */
export const validateNickname = (nickname: string): string => {
  if (!nickname || !nickname.trim()) {
    return '请输入昵称'
  }
  if (nickname.trim().length < 2) {
    return '昵称至少2个字符'
  }
  if (nickname.trim().length > 20) {
    return '昵称不能超过20个字符'
  }
  // 支持中文、英文、数字、下划线、空格
  const nicknameRegex = /^[\u4e00-\u9fa5a-zA-Z0-9_\s]+$/
  if (!nicknameRegex.test(nickname.trim())) {
    return '昵称只能包含中文、英文、数字、下划线和空格'
  }
  return ''
}

export const validatePhone = (phone: string): string => {
  if (!phone || !phone.trim()) {
    return '请输入手机号'
  }
  // 中国手机号格式验证
  const phoneRegex = /^1[3-9]\d{9}$/
  if (!phoneRegex.test(phone.trim())) {
    return '请输入正确的手机号格式'
  }
  return ''
}

export interface ProfileFormValues {
  nickname: string
  phone: string
}

/**
 * 整表校验:返回错误映射(仅含失败字段,key 与表单字段对应)
 */
export const validateProfileForm = (form: ProfileFormValues): Record<string, string> => {
  const errors: Record<string, string> = {}

  const nicknameError = validateNickname(form.nickname)
  if (nicknameError) {
    errors.nickname = nicknameError
  }

  const phoneError = validatePhone(form.phone)
  if (phoneError) {
    errors.phone = phoneError
  }

  return errors
}
