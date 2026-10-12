/**
 * Form 门面(docs/compose/spec/admin-radix-ui.md [S2.4]):
 * 把 antd 形状的表单 API 压在 react-hook-form 之上,保证迁移时页面调用面不改。
 *
 * - `createFormFacade(rhf, options?)` 是纯适配器:只依赖最小化的 `RhfLike`
 *   接口,可注入假实现脱离 React 单测(useForm.test.ts);
 * - `useForm<T>()` 是真实 hook:内部调用 react-hook-form 的 `useForm`,
 *   把 methods 适配成 RhfLike 后组装门面,返回 antd 同款元组 `[form]`;
 * - 规则翻译:antd `rules` 子集(required/message/min/max/len/pattern/type/
 *   validator/whitespace/warningOnly,以及 `(form) => rule` 函数型规则)经
 *   `convertRules` 转为 RHF 规则。message 覆盖优先;缺省中文文案对齐 antd zh-CN 默认
 *   (required → `请输入${label}`;其余见 convertRules 内注释)。
 * - warningOnly 失败只收集警示(`__getWarnings`,不阻断);Form.List 级 `rules`
 *   经 `__registerListRules` 登记,validateFields 一并校验(name=列表路径)。
 */
import { useRef } from 'react'
import { useForm as useRhfForm } from 'react-hook-form'
import type { Control, FieldValues } from 'react-hook-form'

/** 字段路径:字符串(点分)、下标、或路径数组(与 antd NamePath 一致) */
export type NamePath = string | number | ReadonlyArray<string | number>

/** antd rules 子集 —— 全部现有调用面实测出现的键(见 spec [S2.4]) */
export interface AntdRule {
  required?: boolean
  message?: string
  min?: number
  max?: number
  len?: number
  pattern?: RegExp
  type?: 'string' | 'number' | 'email' | 'url' | 'array'
  whitespace?: boolean
  /**
   * antd 自定义校验:reject 语义(失败时 reject Error)。
   * value 用 any 与 antd Rule 同形 —— 调用点普遍标注 (value: number|string|undefined),
   * unknown 在 strictFunctionTypes 下逆变不兼容(现网多处校验器会编译失败)。
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  validator?: (rule: unknown, value: any) => void | Promise<void>
  /**
   * antd 警示规则:失败只出警示文案,不进 errorFields、不阻断
   * validateFields / submit(FormItem 以警示色并列渲染,门面经 __getWarnings 暴露)。
   */
  warningOnly?: boolean
}

/** 规则项:对象,或接收表单实例的函数(现网 stock / delivery-fee-rule 用到) */
export type Rule = AntdRule | ((form: FormInstance) => AntdRule)

/** validateFields() reject 搭载的错误信息(antd 同形状,页面 catch 逻辑不改) */
export interface FormValidateError {
  errorFields: Array<{ name: Array<string | number>; errors: string[] }>
  outOfDate: boolean
}

/** antd onFinishFailed 入参形状 */
export interface FormFinishFailedInfo<T> {
  values: T
  errorFields: FormValidateError['errorFields']
  outOfDate: boolean
}

export type FormValueListener = (info: { name?: string; values: unknown }) => void

export interface FormSubmitHandlers<T> {
  onFinish?: (values: T) => void
  onFinishFailed?: (info: FormFinishFailedInfo<T>) => void
}

/**
 * antd 形状的表单实例。`__` 前缀成员供 `<Form>` / `<Form.Item>` /
 * `Form.List` 内部接线使用,页面只使用常规 API。
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any -- antd 门面须保留无类型默认(FormProps.form 等联用);泛型化会波及全部裸 Form.useForm() 调用点
export interface FormInstance<T = any> {
  /** 全量取值;传 nameList 时返回子集(现网 AddressForm 两种都用) */
  getFieldsValue(): T
  getFieldsValue(names: NamePath[]): Partial<T>
  /** 合并写入(顶层键,嵌套对象整体替换;对齐 rc-field-form set 语义) */
  setFieldsValue(values: Partial<T>): void
  /** 单字段写入(name 支持点分字符串 / 路径数组) */
  setFieldValue(name: NamePath, value: unknown): void
  getFieldValue(name: NamePath): unknown
  /** 无参=全部恢复初始值;带 names=仅恢复这些字段(现网 43 处均无参) */
  resetFields(names?: NamePath[]): void
  /** 校验全部注册字段:通过 resolve 值;失败 reject FormValidateError(antd 风格) */
  validateFields(): Promise<T>
  /** 触发提交(等价 validateFields + onFinish/onFinishFailed) */
  submit(): void

  // —— 内部接线(@internal,页面勿用)——
  /** react-hook-form control,供 <Form.Item> Controller 绑定 */
  readonly __control?: Control<FieldValues>
  /** <Form> 挂载时回灌 initialValues(对齐 antd「初次生效」语义) */
  __setInitialValues(values: Partial<T>): void
  /** Form.Item 的 initialValue 记账,resetFields 时恢复 */
  __registerFieldInitialValue(name: NamePath, value: unknown): void
  /** dependencies 反向登记:被依赖字段变更时重校验依赖方(仅已有错误的) */
  __registerDependencies(field: NamePath, deps: NamePath[]): void
  /** 订阅值变更(reset/setFieldsValue/控件编辑),返回退订函数 */
  __subscribe(listener: FormValueListener): () => void
  /** <Form> 登记 onFinish / onFinishFailed 供 submit() 使用 */
  __attachSubmit(handlers: FormSubmitHandlers<T>): void
  /** 扁平校验错误(RHF 原始错误表;Form.List 级错误走 __getListErrors) */
  __getErrors(): Record<string, string[]>
  /** 清除指定前缀下的校验错误(Form.List add/remove 后调用;含列表级错误) */
  __clearErrors(names?: NamePath[]): void
  /** warningOnly 警示文案(字段路径 → 消息;不进 errorFields,Form.Item 并列渲染) */
  __getWarnings(): Record<string, string[]>
  /** convertRules 注入:登记/清除字段第 index 条 warningOnly 规则的警示(有变化时通知订阅者) */
  __reportWarning(name: NamePath | undefined, index: number, message: string | undefined): void
  /** Form.List rules 覆盖式登记(FormList 每次渲染登记,卸载传 undefined 摘除) */
  __registerListRules(name: NamePath, rules: readonly Rule[] | undefined): void
  /** 列表级 rules 单路径重跑(Form.List add/remove 后刷新 meta.errors) */
  __validateList(name: NamePath): Promise<void>
  /** Form.List 级错误(第三参 meta.errors / Form.ErrorList):硬错误 + warningOnly 警示 */
  __getListErrors(name: NamePath): string[]
}

/** 门面依赖的最小 RHF 接口 —— 纯函数可注入,单测不引 React */
export interface RhfLike<T> {
  getValues(): T
  setValue(name: string, value: unknown, options?: { shouldValidate?: boolean; shouldDirty?: boolean }): void
  reset(values?: Partial<T>, options?: { keepDefaultValues?: boolean }): void
  /** 全量(或指定字段)校验,返回是否通过 */
  trigger(names?: string | string[]): Promise<boolean>
  /** 扁平错误表:`字段路径 → 消息列表` */
  getErrors(): Record<string, string[]>
  /** 值变更订阅(可选),返回退订函数 */
  watch?(listener: (values: T, info: { name?: string }) => void): () => void
}

export interface FormFacadeOptions<T> {
  /** 初始值快照(resetFields 的恢复目标);真实链路由 <Form initialValues> 回灌 */
  initialValues?: Partial<T>
  /** react-hook-form control(仅 useForm 注入;测试可省) */
  control?: Control<FieldValues>
}

// ---------------------------------------------------------------------------
// 路径工具
// ---------------------------------------------------------------------------

export function normalizeName(name: NamePath): string {
  return Array.isArray(name) ? name.map(String).join('.') : String(name)
}

function splitPath(name: NamePath): string[] {
  return Array.isArray(name) ? name.map(String) : String(name).split('.')
}

function getIn(source: unknown, path: string[]): unknown {
  let cur: unknown = source
  for (const seg of path) {
    if (cur === null || cur === undefined || typeof cur !== 'object') return undefined
    cur = (cur as Record<string, unknown>)[seg]
  }
  return cur
}

function setIn(target: Record<string, unknown>, path: string[], value: unknown): void {
  if (path.length === 0) return
  let cur: Record<string, unknown> = target
  for (let i = 0; i < path.length - 1; i += 1) {
    const seg = path[i]
    const next = cur[seg]
    if (next === null || next === undefined || typeof next !== 'object') {
      cur[seg] = {}
    }
    cur = cur[seg] as Record<string, unknown>
  }
  cur[path[path.length - 1]] = value
}

/** RHF 嵌套 FieldErrors → 扁平 `路径 → 消息[]`(validateFields reject 用) */
export function flattenErrors(source: unknown, prefix = '', out: Record<string, string[]> = {}): Record<string, string[]> {
  if (source === null || source === undefined || typeof source !== 'object') return out
  if (Array.isArray(source)) {
    source.forEach((item, index) => {
      flattenErrors(item, prefix ? `${prefix}.${index}` : String(index), out)
    })
    return out
  }
  for (const [key, value] of Object.entries(source as Record<string, unknown>)) {
    if (value === null || value === undefined) continue
    const path = prefix ? `${prefix}.${key}` : key
    if (Array.isArray(value)) {
      value.forEach((item, index) => {
        flattenErrors(item, `${path}.${index}`, out)
      })
      continue
    }
    if (typeof value === 'object') {
      const leaf = value as { message?: unknown; type?: unknown; types?: unknown }
      if ('message' in leaf || 'type' in leaf || 'types' in leaf) {
        const messages: string[] = []
        if (typeof leaf.message === 'string') messages.push(leaf.message)
        if (leaf.types && typeof leaf.types === 'object') {
          for (const msg of Object.values(leaf.types as Record<string, unknown>)) {
            if (typeof msg === 'string') messages.push(msg)
            else flattenErrors(msg, path, out)
          }
        }
        if (messages.length > 0) out[path] = messages
        continue
      }
      flattenErrors(value, path, out)
    }
  }
  return out
}

// ---------------------------------------------------------------------------
// 规则翻译(antd → react-hook-form)
// ---------------------------------------------------------------------------

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const URL_RE = /^https?:\/\/[^\s/$.?#].[^\s]*$/i

function isEmptyValue(value: unknown): boolean {
  return (
    value === undefined ||
    value === null ||
    value === '' ||
    (typeof value === 'number' && Number.isNaN(value)) ||
    (Array.isArray(value) && value.length === 0)
  )
}

function labelOf(label: string | undefined): string {
  return label ?? ''
}

/**
 * 单条 antd 规则的执行体(返回 true=通过,字符串=中文错误文案)。
 *
 * 中文默认文案(全部有 message 覆盖时不会走到;与 antd zh-CN 默认对齐/更贴语境):
 * - required          → `请输入${label}`(缺 label 时「此项为必填项」)
 * - pattern           → `${label}格式不正确`
 * - type email/url    → `${label}不是一个有效邮箱/网址`
 * - type number       → `${label}必须是数字`
 * - min/max(数值)    → `${label}最小值为${min}` / `${label}最大值为${max}`
 * - min/max(字符串)  → `${label}最少${min}个字符` / `${label}最多${max}个字符`
 * - len               → `${label}须为${len}个字符`(数值 `必须等于${len}`)
 */
async function runAntdRule(rule: AntdRule, value: unknown, label: string): Promise<true | string> {
  const override = rule.message
  const L = labelOf(label)
  const fallback = (withLabel: string, withoutLabel: string): string => (L ? withLabel : withoutLabel)

  if (rule.whitespace && (value === undefined || value === null || String(value).trim() === '')) {
    return override ?? fallback(`${L}不能为空字符`, '不能为空字符')
  }
  if (rule.required && isEmptyValue(value)) {
    return override ?? fallback(`请输入${L}`, '此项为必填项')
  }
  if (rule.validator) {
    // 自定义 validator 接管该校验(对齐 antd async-validator):空值同样执行 ——
    // Form.List 空数组检测依赖此语义;无 validator 的规则才走下方空值跳过。
    try {
      await rule.validator({}, value)
      return true
    } catch (error) {
      if (error instanceof Error && error.message) return error.message
      return override ?? '校验未通过'
    }
  }
  // 其余规则跳过空值:空值由 required 负责(与 antd async-validator 一致)
  if (isEmptyValue(value)) return true

  switch (rule.type) {
    case 'number':
      if (typeof value !== 'number' || Number.isNaN(value)) {
        return override ?? fallback(`${L}必须是数字`, '请输入数字')
      }
      break
    case 'string':
      if (typeof value !== 'string') return override ?? fallback(`${L}必须是文本`, '请输入文本')
      break
    case 'email':
      if (typeof value !== 'string' || !EMAIL_RE.test(value)) {
        return override ?? fallback(`${L}不是一个有效邮箱`, '邮箱格式不正确')
      }
      break
    case 'url':
      if (typeof value !== 'string' || !URL_RE.test(value)) {
        return override ?? fallback(`${L}不是一个有效网址`, '网址格式不正确')
      }
      break
    case 'array':
      if (!Array.isArray(value)) return override ?? fallback(`${L}必须是列表`, '必须是列表')
      break
    default:
      break
  }

  const hasRange = rule.min !== undefined || rule.max !== undefined || rule.len !== undefined
  if (hasRange) {
    if (typeof value === 'number') {
      if (rule.len !== undefined && value !== rule.len) {
        return override ?? fallback(`${L}必须等于${rule.len}`, `必须等于${rule.len}`)
      }
      if (rule.min !== undefined && value < rule.min) {
        return override ?? fallback(`${L}最小值为${rule.min}`, `最小值为${rule.min}`)
      }
      if (rule.max !== undefined && value > rule.max) {
        return override ?? fallback(`${L}最大值为${rule.max}`, `最大值为${rule.max}`)
      }
    } else if (typeof value === 'string' || Array.isArray(value)) {
      const length = value.length
      if (rule.len !== undefined && length !== rule.len) {
        return override ?? fallback(`${L}须为${rule.len}个字符`, `须为${rule.len}个字符`)
      }
      if (rule.min !== undefined && length < rule.min) {
        return override ?? fallback(`${L}最少${rule.min}个字符`, `最少${rule.min}个字符`)
      }
      if (rule.max !== undefined && length > rule.max) {
        return override ?? fallback(`${L}最多${rule.max}个字符`, `最多${rule.max}个字符`)
      }
    }
  }

  if (rule.pattern && !rule.pattern.test(String(value))) {
    return override ?? fallback(`${L}格式不正确`, '格式不正确')
  }
  return true
}

/** 转换后的单字段 RHF 规则(元素仅含 validate,顺序与 antd 规则数组一致) */
export type RhfValidateOutcome = true | string
export type RhfFieldRules = Array<{
  validate: (value: unknown) => RhfValidateOutcome | Promise<RhfValidateOutcome>
}>

/**
 * antd rules → RHF 规则。函数型规则 `(form) => rule` 以传入的门面实例求值。
 * 每条 antd 规则产出一条 RHF validate(antd 同款:按数组顺序取第一条失败文案)。
 * warningOnly 规则:失败时经 __reportWarning 登记警示但对 RHF 返回通过
 * (不进 errorFields、不阻断提交);`name` 为字段路径(警示定位键,可省)。
 */
export function convertRules(
  rules: readonly Rule[] | undefined,
  label: string | undefined,
  form: FormInstance,
  name?: NamePath,
): RhfFieldRules {
  const out: RhfFieldRules = []
  const textLabel = typeof label === 'string' ? label : undefined
  let index = 0
  for (const raw of rules ?? []) {
    const ruleIndex = index
    index += 1
    if (!raw) continue
    const rule: AntdRule = typeof raw === 'function' ? raw(form) : raw
    if (!rule || typeof rule !== 'object') continue
    out.push({
      validate: async (value: unknown) => {
        const outcome = await runAntdRule(rule, value, textLabel ?? '')
        if (rule.warningOnly !== true) return outcome
        form.__reportWarning(name, ruleIndex, outcome === true ? undefined : outcome)
        return true
      },
    })
  }
  return out
}

// ---------------------------------------------------------------------------
// Form.Item onChange 组合(命名字段注入)
// ---------------------------------------------------------------------------

/**
 * Form.Item(named 字段)注入子组件的 onChange 组合链 —— antd rc-field-form
 * `getControlled` 同语义(originTriggerFunc):先写表单值,再以**原始入参**
 * 回调子组件自身的 onChange(顺序:表单先、子组件后)。
 *
 * @param fieldSet 写表单值(收到 adapt 适配后的值)
 * @param childOnChange 子组件自身 onChange(可选;缺省时只写表单值,
 *   与无自身回调的子组件行为一致)
 * @param adapt 事件/裸值 → 表单值(缺省恒等;Form.Item 传入 adaptChange)
 */
export function createFieldOnChange(
  fieldSet: (adapted: unknown) => void,
  childOnChange?: (arg: unknown) => void,
  adapt: (arg: unknown) => unknown = (arg) => arg,
): (arg: unknown) => void {
  return (arg: unknown): void => {
    fieldSet(adapt(arg))
    childOnChange?.(arg)
  }
}

// ---------------------------------------------------------------------------
// 门面(纯适配器)
// ---------------------------------------------------------------------------

/**
 * 创建 antd 形状门面。`rhf` 为最小 RHF 接口,测试注入假实现即可覆盖全部 API。
 */
export function createFormFacade<T>(
  rhf: RhfLike<T>,
  options: FormFacadeOptions<T> = {},
): FormInstance<T> {
  let snapshot: Partial<T> = { ...(options.initialValues ?? {}) }
  const fieldInitialValues = new Map<string, unknown>()
  const listeners = new Set<FormValueListener>()
  const dependents = new Map<string, Set<string>>()
  let submitHandlers: FormSubmitHandlers<T> | undefined
  let unwatch: (() => void) | undefined

  const revalidateDependents = (changed: string | undefined): void => {
    if (changed === undefined || dependents.size === 0) return
    const targets = dependents.get(changed)
    if (!targets || targets.size === 0) return
    // 仅重校验「已出现错误」的依赖字段:对齐 antd(未触碰的字段不因依赖变更冒出错误)
    const errors = rhf.getErrors()
    const keys = Object.keys(errors)
    const hit = [...targets].filter((field) => keys.some((k) => k === field || k.startsWith(`${field}.`)))
    if (hit.length > 0) {
      void rhf.trigger(hit).catch(() => undefined)
    }
  }

  const ensureWatch = (): void => {
    if (unwatch !== undefined || rhf.watch === undefined) return
    unwatch = rhf.watch((values, info) => {
      const name = info?.name
      if (name !== undefined) revalidateDependents(name)
      for (const listener of Array.from(listeners)) {
        listener({ name, values })
      }
    })
  }

  const collectInitialValues = (): Record<string, unknown> => {
    const base: Record<string, unknown> = { ...(snapshot as Record<string, unknown>) }
    for (const [key, value] of fieldInitialValues) {
      setIn(base, splitPath(key), value)
    }
    return base
  }

  // —— warningOnly 警示与 Form.List 级校验状态(门面自持,不经 RHF 错误表)——
  // 字段路径 → (规则下标 → 警示文案);Form.List 路径 → 硬错误 / 警示(单条文案)
  const warningsByPath = new Map<string, Map<number, string>>()
  const listRules = new Map<string, readonly Rule[]>()
  const listErrors = new Map<string, string[]>()
  const listWarnings = new Map<string, string[]>()

  /** 门面状态变化时通知订阅者(name 留空:非字段值变更,onValuesChange 不触发) */
  const notifyFacadeChange = (): void => {
    const values = rhf.getValues()
    for (const listener of Array.from(listeners)) {
      listener({ values })
    }
  }

  /**
   * 跑列表级 rules(硬错误取第一条失败文案,warningOnly 只收集警示),
   * 写回 listErrors / listWarnings;有变化时通知订阅者。
   * `only` 缺省为全部已登记列表(validateFields 全量;Form.List add/remove 后单路径)。
   */
  const runListRules = async (only?: string[]): Promise<void> => {
    const keys = only ?? Array.from(listRules.keys())
    let changed = false
    for (const key of keys) {
      const rules = listRules.get(key)
      let hard: string | undefined
      const warns: string[] = []
      if (rules !== undefined && rules.length > 0) {
        const value = getIn(rhf.getValues(), splitPath(key))
        for (const raw of rules) {
          if (!raw) continue
          const rule: AntdRule = typeof raw === 'function' ? raw(facade) : raw
          if (!rule || typeof rule !== 'object') continue
          const outcome = await runAntdRule(rule, value, '')
          if (outcome === true) continue
          if (rule.warningOnly === true) warns.push(outcome)
          else if (hard === undefined) hard = outcome
        }
      }
      const prevHard = listErrors.get(key)
      if (hard === undefined) {
        if (listErrors.delete(key)) changed = true
      } else if (prevHard === undefined || prevHard.length !== 1 || prevHard[0] !== hard) {
        listErrors.set(key, [hard])
        changed = true
      }
      const prevWarn = listWarnings.get(key)
      if (warns.length === 0) {
        if (listWarnings.delete(key)) changed = true
      } else if (prevWarn === undefined || prevWarn.join(' ') !== warns.join(' ')) {
        listWarnings.set(key, warns)
        changed = true
      }
    }
    if (changed) notifyFacadeChange()
  }

  const facade: FormInstance<T> = {
    getFieldsValue: ((names?: NamePath[]): T | Partial<T> => {
      const all = rhf.getValues() as Record<string, unknown>
      if (names === undefined) return all as unknown as T
      const subset: Record<string, unknown> = {}
      for (const name of names) {
        subset[normalizeName(name)] = getIn(all, splitPath(name))
      }
      return subset as unknown as T
    }) as FormInstance<T>['getFieldsValue'],

    setFieldsValue(values: Partial<T>): void {
      for (const [key, value] of Object.entries(values as Record<string, unknown>)) {
        rhf.setValue(key, value)
      }
    },

    setFieldValue(name: NamePath, value: unknown): void {
      rhf.setValue(normalizeName(name), value)
    },

    getFieldValue(name: NamePath): unknown {
      const all = rhf.getValues() as Record<string, unknown>
      return getIn(all, splitPath(name))
    },

    resetFields(names?: NamePath[]): void {
      const base = collectInitialValues()
      if (names !== undefined && names.length > 0) {
        const current = rhf.getValues() as Record<string, unknown>
        const merged = { ...current }
        for (const name of names) {
          const path = splitPath(name)
          setIn(merged, path, getIn(base, path))
        }
        rhf.reset(merged as Partial<T>)
      } else {
        rhf.reset(base as Partial<T>)
      }
    },

    async validateFields(): Promise<T> {
      let passed = false
      try {
        passed = await rhf.trigger()
      } catch {
        passed = false
      }
      // Form.List 级 rules:失败进 errorFields(name=列表路径);warningOnly 只留警示
      await runListRules()
      const flat = rhf.getErrors()
      const errorFields = Object.entries(flat).map(([key, errors]) => ({
        name: key.split('.'),
        errors,
      }))
      for (const [key, errors] of listErrors) {
        if (errors.length > 0) errorFields.push({ name: key.split('.'), errors })
      }
      if (passed && errorFields.length === 0) {
        return rhf.getValues()
      }
      const error: FormValidateError = { errorFields, outOfDate: false }
      throw error
    },

    submit(): void {
      void facade
        .validateFields()
        .then((values) => {
          submitHandlers?.onFinish?.(values)
        })
        .catch((caught: unknown) => {
          const values = facade.getFieldsValue()
          const errorFields =
            caught && typeof caught === 'object' && Array.isArray((caught as FormValidateError).errorFields)
              ? (caught as FormValidateError).errorFields
              : []
          submitHandlers?.onFinishFailed?.({ values, errorFields, outOfDate: false })
        })
    },

    __control: options.control,

    __setInitialValues(values: Partial<T>): void {
      snapshot = { ...values }
      // 初始值只填「当前尚无值」的字段:保留挂载前 setFieldsValue / Form.Item initialValue
      const current = rhf.getValues() as Record<string, unknown>
      const merged: Record<string, unknown> = { ...(values as Record<string, unknown>) }
      for (const [key, value] of Object.entries(current)) {
        if (value !== undefined) merged[key] = value
      }
      rhf.reset(merged as Partial<T>)
    },

    __registerFieldInitialValue(name: NamePath, value: unknown): void {
      fieldInitialValues.set(normalizeName(name), value)
    },

    __registerDependencies(field: NamePath, deps: NamePath[]): void {
      const dependent = normalizeName(field)
      for (const dep of deps) {
        const key = normalizeName(dep)
        let set = dependents.get(key)
        if (set === undefined) {
          set = new Set()
          dependents.set(key, set)
        }
        set.add(dependent)
      }
      ensureWatch()
    },

    __subscribe(listener: FormValueListener): () => void {
      ensureWatch()
      listeners.add(listener)
      return () => {
        listeners.delete(listener)
      }
    },

    __attachSubmit(handlers: FormSubmitHandlers<T>): void {
      submitHandlers = handlers
    },

    __getErrors(): Record<string, string[]> {
      return rhf.getErrors()
    },

    __clearErrors(names?: NamePath[]): void {
      const impl = rhf as RhfLike<T> & {
        clearErrors?: (names?: string | string[]) => void
      }
      if (names === undefined) impl.clearErrors?.()
      else impl.clearErrors?.(names.map(normalizeName))
      if (names === undefined) return
      let changed = false
      for (const name of names) {
        const key = normalizeName(name)
        if (listErrors.delete(key)) changed = true
        if (listWarnings.delete(key)) changed = true
      }
      if (changed) notifyFacadeChange()
    },

    __getWarnings(): Record<string, string[]> {
      const out: Record<string, string[]> = {}
      for (const [key, slot] of warningsByPath) {
        out[key] = Array.from(slot.values())
      }
      return out
    },

    __reportWarning(name: NamePath | undefined, index: number, message: string | undefined): void {
      if (name === undefined) return
      const key = normalizeName(name)
      let slot = warningsByPath.get(key)
      if (slot === undefined) {
        if (message === undefined) return
        slot = new Map<number, string>()
        warningsByPath.set(key, slot)
      }
      if (slot.get(index) === message) return
      if (message === undefined) slot.delete(index)
      else slot.set(index, message)
      if (slot.size === 0) warningsByPath.delete(key)
      notifyFacadeChange()
    },

    __registerListRules(name: NamePath, rules: readonly Rule[] | undefined): void {
      const key = normalizeName(name)
      if (rules === undefined || rules.length === 0) {
        listRules.delete(key)
        listErrors.delete(key)
        listWarnings.delete(key)
      } else {
        listRules.set(key, rules)
      }
    },

    __validateList(name: NamePath): Promise<void> {
      return runListRules([normalizeName(name)])
    },

    __getListErrors(name: NamePath): string[] {
      const key = normalizeName(name)
      const errors = listErrors.get(key)
      const warnings = listWarnings.get(key)
      if (errors === undefined || errors.length === 0) return warnings ?? []
      if (warnings === undefined || warnings.length === 0) return errors
      return [...errors, ...warnings]
    },
  }

  // 尽早订阅:让 setFieldsValue 引发的监听(dependencies 重校验等)无需等首个订阅者
  ensureWatch()
  return facade
}

// ---------------------------------------------------------------------------
// hook 入口
// ---------------------------------------------------------------------------

export interface UseFormOptions<T> {
  defaultValues?: Partial<T>
}

/**
 * antd `Form.useForm()` 等价物。返回元组 `[form]`(现网 33 处全部按
 * `const [form] = Form.useForm()` 解构)。内部固定 `mode: 'onChange'`,
 * 对齐 antd「change 即校验」的默认时机(validateFields 强制全量校验)。
 */
export function useForm<T extends FieldValues>(
  options: UseFormOptions<T> = {},
): [FormInstance<T>] {
  const rhf = useRhfForm<T>({
    mode: 'onChange',
    reValidateMode: 'onChange',
    defaultValues: options.defaultValues as never,
  })
  const ref = useRef<FormInstance<T> | null>(null)
  if (ref.current === null) {
    const adapter: RhfLike<T> = {
      getValues: () => rhf.getValues(),
      setValue: (name, value, opts) => {
        rhf.setValue(name as never, value as never, opts)
      },
      reset: (values, opts) => {
        rhf.reset(values as never, opts)
      },
      trigger: (names) => rhf.trigger(names as never),
      getErrors: () => flattenErrors(rhf.getErrors()),
      watch: (listener) => {
        // eslint-disable-next-line react-hooks/incompatible-library -- RHF watch 回调无法被 React Compiler 记忆化,官方建议忽略;不影响运行时
        const subscription = rhf.watch((values, info) => {
          const name = info?.name
          listener(values as T, { name: typeof name === 'string' ? name : undefined })
        })
        return () => {
          subscription.unsubscribe()
        }
      },
    }
    ref.current = createFormFacade<T>(adapter, { control: rhf.control as unknown as Control<FieldValues> })
  }
  return [ref.current]
}
