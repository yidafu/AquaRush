/**
 * Form 门面单测(spec [S2.4] 验收:validateFields reject、setFieldsValue、
 * resetFields、数值类型保持 number)。
 *
 * 全部走 createFormFacade + 注入的假 RhfLike —— 纯逻辑,无 React 渲染;
 * 校验规则由 convertRules 真实翻译后在假 trigger 里执行,覆盖中文文案断言。
 */
import { describe, expect, it, jest } from '@jest/globals'
import {
  convertRules,
  createFieldOnChange,
  createFormFacade,
  type FormInstance,
  type RhfFieldRules,
  type RhfLike,
} from './useForm'

interface FakeStore {
  rhf: RhfLike<Record<string, unknown>>
  /** 模拟 Form.Item 注册规则(经 convertRules 翻译) */
  registerRules: (name: string, rules: RhfFieldRules) => void
}

function createFakeRhf(initial: Record<string, unknown>): FakeStore {
  let values: Record<string, unknown> = { ...initial }
  let errors: Record<string, string[]> = {}
  const ruleMap = new Map<string, RhfFieldRules>()

  const rhf: RhfLike<Record<string, unknown>> = {
    getValues: () => values,
    setValue: (name, value) => {
      values = { ...values, [name]: value }
    },
    reset: (next) => {
      values = { ...(next ?? {}) }
      errors = {}
    },
    trigger: async (names?) => {
      const targets = names ?? [...ruleMap.keys()]
      for (const target of targets) {
        const rules = ruleMap.get(target)
        if (rules === undefined) continue
        const messages: string[] = []
        for (const rule of rules) {
          if (rule.validate === undefined) continue
          const result = await rule.validate(values[target])
          if (result !== true) messages.push(typeof result === 'string' ? result : '校验失败')
        }
        if (messages.length > 0) errors[target] = messages
        else delete errors[target]
      }
      return Object.keys(errors).length === 0
    },
    getErrors: () => errors,
  }

  return {
    rhf,
    registerRules: (name, rules) => {
      ruleMap.set(name, rules)
    },
  }
}

describe('createFormFacade / validateFields', () => {
  it('规则全部通过时 resolve 表单值(antd 形状)', async () => {
    const fake = createFakeRhf({ username: 'alice', age: 18 })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    fake.registerRules('username', convertRules([{ required: true, message: '用户名必填' }], '用户名', form))
    fake.registerRules('age', convertRules([{ type: 'number', min: 18, message: '须满 18' }], '年龄', form))

    await expect(form.validateFields()).resolves.toEqual({ username: 'alice', age: 18 })
  })

  it('required 失败时 reject,且缺省文案为「请输入${label}」', async () => {
    const fake = createFakeRhf({ username: '' })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    fake.registerRules('username', convertRules([{ required: true }], '用户名', form))

    await expect(form.validateFields()).rejects.toEqual({
      errorFields: [{ name: ['username'], errors: ['请输入用户名'] }],
      outOfDate: false,
    })
  })

  it('message 覆盖优先于缺省文案', async () => {
    const fake = createFakeRhf({ remark: 'x'.repeat(501) })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    fake.registerRules('remark', convertRules([{ max: 500, message: '备注长度不能超过500个字符' }], '备注', form))

    await expect(form.validateFields()).rejects.toEqual({
      errorFields: [{ name: ['remark'], errors: ['备注长度不能超过500个字符'] }],
      outOfDate: false,
    })
  })

  it('pattern 失败时 reject,message 生效', async () => {
    const fake = createFakeRhf({ phone: '12345' })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    fake.registerRules(
      'phone',
      convertRules(
        [{ required: true, message: '请输入手机号' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号' }],
        '手机号',
        form,
      ),
    )

    await expect(form.validateFields()).rejects.toEqual({
      errorFields: [{ name: ['phone'], errors: ['请输入正确的11位手机号'] }],
      outOfDate: false,
    })
  })

  it('type:number + min 失败时 reject(message 覆盖 + 缺省两档)', async () => {
    const withMessage = createFakeRhf({ keepDays: 0 })
    const formA = createFormFacade<Record<string, unknown>>(withMessage.rhf)
    withMessage.registerRules(
      'keepDays',
      convertRules([{ required: true, message: '参数无效' }, { type: 'number', min: 1, max: 3650, message: '参数无效' }], '保留天数', formA),
    )
    await expect(formA.validateFields()).rejects.toEqual({
      errorFields: [{ name: ['keepDays'], errors: ['参数无效'] }],
      outOfDate: false,
    })

    const withDefault = createFakeRhf({ keepDays: 4000 })
    const formB = createFormFacade<Record<string, unknown>>(withDefault.rhf)
    withDefault.registerRules('keepDays', convertRules([{ type: 'number', min: 1, max: 3650 }], '保留天数', formB))
    await expect(formB.validateFields()).rejects.toEqual({
      errorFields: [{ name: ['keepDays'], errors: ['保留天数最大值为3650'] }],
      outOfDate: false,
    })
  })

  it('validator(现网自定义校验)失败时带出 Error.message', async () => {
    const fake = createFakeRhf({ thresholdYuan: 0 })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    fake.registerRules(
      'thresholdYuan',
      convertRules(
        [
          {
            validator: (_rule: unknown, value: unknown) =>
              value !== undefined && (value as number) >= 0.01
                ? Promise.resolve()
                : Promise.reject(new Error('门槛无效(0.01-99999元)')),
          },
        ],
        '满(元)',
        form,
      ),
    )

    await expect(form.validateFields()).rejects.toEqual({
      errorFields: [{ name: ['thresholdYuan'], errors: ['门槛无效(0.01-99999元)'] }],
      outOfDate: false,
    })
  })

  it('修复失败值后再次 validateFields resolve', async () => {
    const fake = createFakeRhf({ username: '' })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    fake.registerRules('username', convertRules([{ required: true }], '用户名', form))

    await expect(form.validateFields()).rejects.toBeDefined()
    fake.rhf.setValue('username', 'bob')
    await expect(form.validateFields()).resolves.toEqual({ username: 'bob' })
  })
})

describe('createFormFacade / 值操作', () => {
  it('setFieldsValue 合并写入(不覆盖未提及字段)', () => {
    const fake = createFakeRhf({ a: 1, b: 2 })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    form.setFieldsValue({ a: 9 })

    expect(form.getFieldsValue()).toEqual({ a: 9, b: 2 })
    expect(form.getFieldValue('b')).toBe(2)
  })

  it('setFieldValue 只更新单个键', () => {
    const fake = createFakeRhf({ a: 1, b: 2 })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    form.setFieldValue('b', 42)

    expect(form.getFieldsValue()).toEqual({ a: 1, b: 42 })
    expect(form.getFieldValue('a')).toBe(1)
  })

  it('getFieldsValue(nameList) 返回子集(现网 AddressForm 用法)', () => {
    const fake = createFakeRhf({ province: '广东省', city: '深圳市', other: 'x' })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)

    expect(form.getFieldsValue(['province', 'city'])).toEqual({
      province: '广东省',
      city: '深圳市',
    })
  })

  it('resetFields 恢复 options.initialValues 快照', () => {
    const fake = createFakeRhf({})
    const form = createFormFacade<Record<string, unknown>>(fake.rhf, {
      initialValues: { status: 'OFFLINE', sortOrder: 0 },
    })

    form.setFieldsValue({ status: 'ONLINE', sortOrder: 5 })
    expect(form.getFieldsValue()).toEqual({ status: 'ONLINE', sortOrder: 5 })

    form.resetFields()
    expect(form.getFieldsValue()).toEqual({ status: 'OFFLINE', sortOrder: 0 })
  })

  it('__setInitialValues 保留已有值、回灌缺失值(<Form initialValues> 语义)', () => {
    const fake = createFakeRhf({})
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    form.setFieldsValue({ preset: 'kept' })

    form.__setInitialValues({ preset: 'ignored', fresh: 'applied' })
    expect(form.getFieldsValue()).toEqual({ preset: 'kept', fresh: 'applied' })

    form.resetFields()
    expect(form.getFieldsValue()).toEqual({ preset: 'ignored', fresh: 'applied' })
  })

  it('numeric 数值经门面保持 number 类型(不被字符串化)', async () => {
    const fake = createFakeRhf({})
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    form.setFieldsValue({ count: 42, price: 3.5 })

    const values = form.getFieldsValue() as Record<string, unknown>
    expect(typeof values.count).toBe('number')
    expect(values.count).toBe(42)
    expect(typeof values.price).toBe('number')

    fake.registerRules('count', convertRules([{ type: 'number', min: 1 }], '数量', form))
    await expect(form.validateFields()).resolves.toEqual({ count: 42, price: 3.5 })

    // 字符串化的数字会被 type:number 拒绝(说明门面没有静默转换)
    fake.rhf.setValue('count', '42')
    await expect(form.validateFields()).rejects.toEqual({
      errorFields: [{ name: ['count'], errors: ['数量必须是数字'] }],
      outOfDate: false,
    })
  })

  it('validateFields reject 形状支持嵌套路径(errorFields.name 按点拆分)', async () => {
    const fake = createFakeRhf({ user: { name: '' } })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    // 假 trigger 以扁平键模拟 RHF 深层错误
    fake.registerRules('user.name', convertRules([{ required: true }], '姓名', form))

    await expect(form.validateFields()).rejects.toEqual({
      errorFields: [{ name: ['user', 'name'], errors: ['请输入姓名'] }],
      outOfDate: false,
    })
  })
})

describe('createFormFacade / submit', () => {
  it('submit 通过时回调 onFinish,失败时回调 onFinishFailed', async () => {
    const fake = createFakeRhf({ name: '' })
    const form: FormInstance<Record<string, unknown>> = createFormFacade<Record<string, unknown>>(fake.rhf)
    fake.registerRules('name', convertRules([{ required: true, message: '请填写名称' }], '名称', form))

    const finished: Array<Record<string, unknown>> = []
    const failed: unknown[] = []
    form.__attachSubmit({
      onFinish: (values) => finished.push(values),
      onFinishFailed: (info) => failed.push(info),
    })

    form.submit()
    await new Promise((resolve) => setTimeout(resolve, 0))
    expect(failed).toEqual([
      { values: { name: '' }, errorFields: [{ name: ['name'], errors: ['请填写名称'] }], outOfDate: false },
    ])
    expect(finished).toEqual([])

    fake.rhf.setValue('name', 'ok')
    form.submit()
    await new Promise((resolve) => setTimeout(resolve, 0))
    expect(finished).toEqual([{ name: 'ok' }])
  })
})

describe('createFormFacade / warningOnly', () => {
  const urlRule = { type: 'url', message: '请输入有效的URL地址', warningOnly: true } as const

  it('warningOnly 失败时 validateFields 仍 resolve,警示收集进 __getWarnings', async () => {
    const fake = createFakeRhf({ coverImageUrl: 'not-a-url' })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    fake.registerRules('coverImageUrl', convertRules([urlRule], '封面图片URL', form, 'coverImageUrl'))

    await expect(form.validateFields()).resolves.toEqual({ coverImageUrl: 'not-a-url' })
    expect(form.__getWarnings()).toEqual({ coverImageUrl: ['请输入有效的URL地址'] })
  })

  it('修复后再次校验清除警示', async () => {
    const fake = createFakeRhf({ coverImageUrl: 'not-a-url' })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    fake.registerRules('coverImageUrl', convertRules([urlRule], '封面图片URL', form, 'coverImageUrl'))
    await expect(form.validateFields()).resolves.toBeDefined()
    expect(form.__getWarnings()).toEqual({ coverImageUrl: ['请输入有效的URL地址'] })

    fake.rhf.setValue('coverImageUrl', 'https://example.com/a.png')
    await expect(form.validateFields()).resolves.toBeDefined()
    expect(form.__getWarnings()).toEqual({})
  })

  it('同批硬错误仍 reject,且 errorFields 不含 warningOnly 警示', async () => {
    const fake = createFakeRhf({ coverImageUrl: 'not-a-url', username: '' })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    fake.registerRules('coverImageUrl', convertRules([urlRule], '封面图片URL', form, 'coverImageUrl'))
    fake.registerRules('username', convertRules([{ required: true, message: '用户名必填' }], '用户名', form, 'username'))

    await expect(form.validateFields()).rejects.toEqual({
      errorFields: [{ name: ['username'], errors: ['用户名必填'] }],
      outOfDate: false,
    })
    expect(form.__getWarnings()).toEqual({ coverImageUrl: ['请输入有效的URL地址'] })
  })
})

describe('createFieldOnChange / Form.Item onChange 组合链', () => {
  /** 与 Form.tsx adaptChange 同形的事件适配(事件 → target.value) */
  const adaptEvent = (arg: unknown): unknown =>
    arg !== null && typeof arg === 'object' && 'target' in arg
      ? (arg as { target: Record<string, unknown> }).target.value
      : arg

  it('先写表单值再回调子组件 onChange(antd 顺序),子组件收到原始入参', () => {
    const order: string[] = []
    const event = { target: { value: '广东省' } }
    const fieldSet = jest.fn((adapted: unknown) => {
      order.push('form')
      expect(adapted).toBe('广东省')
    })
    const childOnChange = jest.fn((arg: unknown) => {
      order.push('child')
      expect(arg).toBe(event)
    })

    const handler = createFieldOnChange(fieldSet, childOnChange, adaptEvent)
    handler(event)

    expect(order).toEqual(['form', 'child'])
    expect(fieldSet).toHaveBeenCalledTimes(1)
    expect(childOnChange).toHaveBeenCalledWith(event)
  })

  it('子组件无自身 onChange 时只写表单值(行为与原实现一致)', () => {
    const formValues: unknown[] = []
    const handler = createFieldOnChange((adapted) => formValues.push(adapted), undefined, adaptEvent)

    expect(() => handler({ target: { value: 'bare' } })).not.toThrow()
    expect(formValues).toEqual(['bare'])
  })

  it('fieldSet 收到适配后的表单值,子组件收到原始事件对象', () => {
    const event = { target: { value: '深圳市' } }
    const formValues: unknown[] = []
    const childArgs: unknown[] = []

    const handler = createFieldOnChange(
      (adapted) => formValues.push(adapted),
      (arg) => childArgs.push(arg),
      adaptEvent,
    )
    handler(event)

    expect(formValues).toEqual(['深圳市'])
    expect(childArgs).toEqual([event])
  })

  it('缺省 adapt 时透传裸值(非事件入参,如 Select/自定义组件)', () => {
    const formValues: unknown[] = []
    const childArgs: unknown[] = []
    const handler = createFieldOnChange(
      (adapted) => formValues.push(adapted),
      (arg) => childArgs.push(arg),
    )

    handler('tier-2')
    expect(formValues).toEqual(['tier-2'])
    expect(childArgs).toEqual(['tier-2'])
  })
})

describe('createFormFacade / Form.List rules', () => {
  const listValidator = (_rule: unknown, value: unknown): Promise<void> =>
    Array.isArray(value) && value.length > 0
      ? Promise.resolve()
      : Promise.reject(new Error('请至少配置一个档位'))

  it('列表 rules 失败:validateFields reject 且 errorFields.name = 列表路径', async () => {
    const fake = createFakeRhf({ tiers: [] })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    form.__registerListRules('tiers', [{ validator: listValidator }])

    await expect(form.validateFields()).rejects.toEqual({
      errorFields: [{ name: ['tiers'], errors: ['请至少配置一个档位'] }],
      outOfDate: false,
    })
    expect(form.__getListErrors('tiers')).toEqual(['请至少配置一个档位'])

    fake.rhf.setValue('tiers', [{ fromFloor: 1 }])
    await expect(form.validateFields()).resolves.toBeDefined()
    expect(form.__getListErrors('tiers')).toEqual([])
  })

  it('列表 rules 的 warningOnly:警示进 __getListErrors 但不阻断 validateFields', async () => {
    const fake = createFakeRhf({ tiers: [] })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    form.__registerListRules('tiers', [{ validator: listValidator, warningOnly: true }])

    await expect(form.validateFields()).resolves.toBeDefined()
    expect(form.__getListErrors('tiers')).toEqual(['请至少配置一个档位'])
  })

  it('__validateList 单路径重跑(Form.List add/remove 后 live 刷新)', async () => {
    const fake = createFakeRhf({ tiers: [] })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    form.__registerListRules('tiers', [{ validator: listValidator }])

    await form.__validateList('tiers')
    expect(form.__getListErrors('tiers')).toEqual(['请至少配置一个档位'])

    fake.rhf.setValue('tiers', [{ fromFloor: 5 }])
    await form.__validateList('tiers')
    expect(form.__getListErrors('tiers')).toEqual([])
  })

  it('__registerListRules(undefined) 摘除后不再产出列表错误', async () => {
    const fake = createFakeRhf({ tiers: [] })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    form.__registerListRules('tiers', [{ validator: listValidator }])
    await expect(form.validateFields()).rejects.toBeDefined()

    form.__registerListRules('tiers', undefined)
    await expect(form.validateFields()).resolves.toBeDefined()
    expect(form.__getListErrors('tiers')).toEqual([])
  })

  it('__clearErrors 清除列表级错误(Form.List add/remove 入口语义)', async () => {
    const fake = createFakeRhf({ tiers: [] })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    form.__registerListRules('tiers', [{ validator: listValidator }])
    await expect(form.validateFields()).rejects.toBeDefined()

    form.__clearErrors(['tiers'])
    expect(form.__getListErrors('tiers')).toEqual([])
  })

  it('嵌套列表路径以点分归一进 errorFields.name', async () => {
    const fake = createFakeRhf({ orders: [{ items: [] }] })
    const form = createFormFacade<Record<string, unknown>>(fake.rhf)
    form.__registerListRules(['orders', 0, 'items'], [{ validator: listValidator }])

    await expect(form.validateFields()).rejects.toEqual({
      errorFields: [{ name: ['orders', '0', 'items'], errors: ['请至少配置一个档位'] }],
      outOfDate: false,
    })
  })
})
