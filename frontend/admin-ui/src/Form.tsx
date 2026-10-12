/**
 * Form 门面组件(spec [S2.4]):antd 形状的 <Form> / <Form.Item> / Form.List /
 * Form.useForm / Form.useWatch,内部为 react-hook-form + ./useForm 门面。
 *
 * - <Form form={form}>:layout(initialValues onFinish onFinishFailed onValuesChange
 *   disabled size labelCol wrapperCol preserve);未传 form 时内部自建实例
 *   (现网 AssignDeliveryModal 等裸 Form 用法);
 * - <Form.Item>:name / label / rules / required / extra / tooltip / valuePropName /
 *   trigger / validateTrigger / initialValue / dependencies / noStyle /
 *   shouldUpdate(值变更即重渲染函数子节点);无 name 为纯布局容器(与 antd 一致,
 *   rules 不生效);
 * - 校验时机由 useForm 的 mode:'onChange' 统一承担(见 ./useForm 注释)。
 */
import React from 'react'
import { Controller } from 'react-hook-form'
import * as stylex from '@stylexjs/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import {
  convertRules,
  createFieldOnChange,
  normalizeName,
  useForm,
  type FormFinishFailedInfo,
  type FormInstance,
  type NamePath,
  type Rule,
} from './useForm'

export type { FormInstance } from './useForm'
export type { NamePath, Rule } from './useForm'

export type FormLayout = 'horizontal' | 'vertical' | 'inline'

export interface ColConfig {
  /** 24 栅格;仅 horizontal 布局生效(现网暂无调用,保留类型兼容) */
  span?: number
}

interface FormBinding {
  facade: FormInstance
  layout: FormLayout
  labelCol?: ColConfig
  wrapperCol?: ColConfig
  disabled?: boolean
  size?: 'small' | 'middle' | 'large'
}

const FormBindingContext = React.createContext<FormBinding | null>(null)

function useBinding(): FormBinding | null {
  return React.useContext(FormBindingContext)
}

/**
 * Form.List 名称前缀(antd 语义:列表内 <Form.Item name={[i,'x']}> 自动拼上列表
 * 路径;dependencies 不受前缀影响,保持绝对路径)。
 */
const ListPrefixContext = React.createContext<string[] | null>(null)

/**
 * Form.List 级错误上下文:FormList 注入本列表的 meta.errors,
 * 子树中的 <Form.ErrorList> 无 props 也能渲染(antd「就近 ErrorList」语义)。
 */
const ListErrorsContext = React.createContext<string[] | null>(null)

function toSegments(name: NamePath): string[] {
  return Array.isArray(name) ? name.map(String) : [String(name)]
}

// ---------------------------------------------------------------------------
// 样式(StyleX longhand;token 唯一来源 @aquarush/common/theme/tokens.stylex)
// ---------------------------------------------------------------------------

const styles = stylex.create({
  form: {
    display: 'block',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
    marginRight: '0px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    color: tokens.text,
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
  },
  formInline: {
    display: 'flex',
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'flex-start',
    columnGap: '8px',
    rowGap: '8px',
  },
  item: {
    display: 'block',
    marginTop: '0px',
    marginBottom: '20px',
    marginLeft: '0px',
    marginRight: '0px',
  },
  itemLastTight: {
    marginBottom: '0px',
  },
  itemInline: {
    display: 'inline-flex',
    flexDirection: 'row',
    flexWrap: 'nowrap',
    alignItems: 'flex-start',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
    marginRight: '8px',
  },
  itemHorizontal: {
    display: 'flex',
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'flex-start',
  },
  labelRowVertical: {
    display: 'block',
    marginBottom: '4px',
  },
  labelRowInline: {
    display: 'inline-block',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
    marginRight: '8px',
    paddingTop: '5px', // 与 32px 高控件视觉对齐
    verticalAlign: 'top',
  },
  labelRowHorizontal: {
    display: 'flex',
    flexDirection: 'row',
    flexWrap: 'nowrap',
    alignItems: 'flex-start',
    justifyContent: 'flex-end',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
    marginRight: '12px',
    paddingTop: '5px',
    textAlign: 'right',
    flexGrow: '0',
    flexShrink: '0',
    flexBasis: 'auto',
  },
  label: {
    color: tokens.text,
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
    textAlign: 'inherit',
  },
  labelRequired: {
    fontWeight: '500',
  },
  requiredMark: {
    marginRight: '4px',
    color: tokens.error,
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
  },
  tooltipMark: {
    marginLeft: '4px',
    color: tokens.textWeak,
    fontSize: '12px',
    lineHeight: tokens.lhAux,
    cursor: 'help',
    userSelect: 'none',
  },
  control: {
    display: 'block',
    minWidth: '0px',
  },
  controlHorizontal: {
    display: 'flex',
    flexDirection: 'column',
    flexGrow: '1',
    flexShrink: '1',
    flexBasis: '0%',
    minWidth: '0px',
  },
  error: {
    display: 'block',
    marginTop: '4px',
    color: tokens.error,
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
  },
  warning: {
    display: 'block',
    marginTop: '4px',
    color: tokens.warning,
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
  },
  extra: {
    display: 'block',
    marginTop: '4px',
    color: tokens.textSub,
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
  },
})

// ---------------------------------------------------------------------------
// props 类型
// ---------------------------------------------------------------------------

export interface FormProps
  extends Omit<React.ComponentPropsWithoutRef<'form'>, 'onSubmit' | 'children'> {
  /** 由 useForm / Form.useForm 创建;缺省时内部自建实例 */
  form?: FormInstance
  layout?: FormLayout
  initialValues?: Record<string, unknown>
  // eslint-disable-next-line @typescript-eslint/no-explicit-any -- antd onFinish 为未类型化表单值;调用点回调参数是具体表单接口,改 unknown 会因逆变无法编译
  onFinish?: (values: any) => void
  onFinishFailed?: (info: FormFinishFailedInfo<unknown>) => void
  // eslint-disable-next-line @typescript-eslint/no-explicit-any -- 同 onFinish:全量表单值须保持 any,泛型化会波及调用点
  onValuesChange?: (changedValues: Record<string, unknown>, allValues: any) => void
  disabled?: boolean
  size?: 'small' | 'middle' | 'large'
  labelCol?: ColConfig
  wrapperCol?: ColConfig
  /** 接受但近似忽略:RHF 默认保留卸载字段值,现网弹窗均以 resetFields/setFieldsValue 复位(报告已记录) */
  preserve?: boolean
  children?: React.ReactNode
}

export interface FormItemProps {
  name?: NamePath
  label?: React.ReactNode
  rules?: Rule[]
  /** 显式必填星号;缺省按 rules 中 required 推导 */
  required?: boolean
  extra?: React.ReactNode
  tooltip?: React.ReactNode
  /** 受控子组件的值属性名:默认 value;'checked' 用于 Checkbox/Switch(现网 13 处) */
  valuePropName?: string
  /** 接受:onChange|onBlur;实际时机由 useForm mode:'onChange' 统一控制 */
  trigger?: string
  /** 同上,接受并忽略细分配置 */
  validateTrigger?: string | string[]
  initialValue?: unknown
  dependencies?: NamePath[]
  /** true 或比较函数:函数子节点在任意值变更时重渲染(现网 1 处) */
  shouldUpdate?: boolean | ((prev: Record<string, unknown>, curr: Record<string, unknown>) => boolean)
  /** 跳过 label / 错误文案 / 外层排版(现网 2 处) */
  noStyle?: boolean
  colon?: boolean
  htmlFor?: string
  className?: string
  style?: React.CSSProperties
  children?: React.ReactNode | ((form: FormInstance) => React.ReactNode)
  /** 其余属性透传到包裹 div(如 stylex className) */
  [key: string]: unknown
}

export interface FormListField {
  key: number
  name: number
}

export interface FormListOperation {
  add: (defaultValue?: Record<string, unknown>) => void
  remove: (index: number) => void
  move: (from: number, to: number) => void
}

export interface FormListProps {
  name: NamePath
  /** 列表级校验(antd Form.List rules):失败文案经 validateFields errorFields
   *  (name=列表路径)与第三参 meta.errors / Form.ErrorList 展示 */
  rules?: Rule[]
  children: (
    fields: FormListField[],
    operation: FormListOperation,
    meta: { errors: string[] },
  ) => React.ReactNode
}

// ---------------------------------------------------------------------------
// 工具
// ---------------------------------------------------------------------------

/** 事件 / 裸值 → 表单值:对象事件取 target[valuePropName|value],其余原样 */
function adaptChange(arg: unknown, valuePropName: string): unknown {
  if (arg !== null && typeof arg === 'object' && 'target' in arg) {
    const target = (arg as { target: Record<string, unknown> }).target
    if (valuePropName in target) return target[valuePropName]
    if ('value' in target) return target.value
    if ('checked' in target) return target.checked
    return undefined
  }
  return arg
}

function isElementChild(child: React.ReactNode): child is React.ReactElement<Record<string, unknown>> {
  return React.isValidElement(child)
}

// ---------------------------------------------------------------------------
// FormItem —— 函数子节点(shouldUpdate / render-prop)
// ---------------------------------------------------------------------------

function FormItemFunctionChildren(props: {
  render: (form: FormInstance) => React.ReactNode
}): React.ReactElement | null {
  const binding = useBinding()
  const [, setTick] = React.useState(0)
  React.useEffect(() => {
    if (binding === null) return undefined
    return binding.facade.__subscribe(() => setTick((tick) => tick + 1))
  }, [binding])
  if (binding === null) return null
  return <>{props.render(binding.facade)}</>
}

// ---------------------------------------------------------------------------
// FormItem
// ---------------------------------------------------------------------------

export function FormItem(props: FormItemProps): React.ReactElement {
  const {
    name,
    label,
    rules,
    required,
    extra,
    tooltip,
    valuePropName,
    trigger: _trigger,
    validateTrigger: _validateTrigger,
    initialValue,
    dependencies,
    shouldUpdate: _shouldUpdate,
    noStyle,
    colon: _colon,
    htmlFor,
    className,
    style,
    children,
    ...rest
  } = props
  void _trigger
  void _validateTrigger
  void _shouldUpdate
  void _colon

  const binding = useBinding()
  const listPrefix = React.useContext(ListPrefixContext)

  // 列表内自动拼前缀(见 ListPrefixContext)
  const effectiveName: NamePath | undefined =
    name === undefined
      ? undefined
      : listPrefix === null || listPrefix.length === 0
        ? name
        : [...listPrefix, ...toSegments(name)]

  // Form.Item 级 initialValue 记账(resetFields 恢复目标)+ dependencies 反向登记。
  // 注意:必须先于任何 early return 调用(hook 顺序稳定)。
  React.useEffect(() => {
    if (binding === null || effectiveName === undefined) return undefined
    if (initialValue !== undefined) {
      binding.facade.__registerFieldInitialValue(effectiveName, initialValue)
    }
    if (dependencies !== undefined && dependencies.length > 0) {
      binding.facade.__registerDependencies(effectiveName, dependencies)
    }
    return undefined
  })

  // warningOnly 警示不经 RHF fieldState,需自行订阅:仅含 warningOnly 规则的字段
  // 在警示产生/清除时重渲染(按本路径文案变化过滤,避免全表单抖动)。
  const [, setWarnTick] = React.useState(0)
  React.useEffect(() => {
    if (binding === null || effectiveName === undefined || rules === undefined || rules.length === 0) {
      return undefined
    }
    const hasWarning = rules.some(
      (rule) => (typeof rule === 'function' ? rule(binding.facade) : rule)?.warningOnly === true,
    )
    if (!hasWarning) return undefined
    const key = normalizeName(effectiveName)
    let last = JSON.stringify(binding.facade.__getWarnings()[key] ?? [])
    return binding.facade.__subscribe(() => {
      const next = JSON.stringify(binding.facade.__getWarnings()[key] ?? [])
      if (next !== last) {
        last = next
        setWarnTick((tick) => tick + 1)
      }
    })
  })

  if (typeof children === 'function') {
    return <FormItemFunctionChildren render={children} />
  }
  if (binding === null) {
    return <>{children}</>
  }

  const { facade, layout, labelCol, wrapperCol, disabled, size } = binding

  const labelStr = typeof label === 'string' ? label : undefined
  const rhfRules = convertRules(rules, labelStr, facade, effectiveName)
  const showRequired =
    required ??
    (rules ?? []).some((rule) => (typeof rule === 'function' ? rule(facade) : rule)?.required === true)
  // warningOnly 警示(并列渲染;不阻断提交)
  const warnings =
    effectiveName === undefined ? [] : facade.__getWarnings()[normalizeName(effectiveName)] ?? []

  const labelText =
    label === undefined ? null : (
      <label
        htmlFor={htmlFor}
        style={
          layout === 'horizontal' && labelCol?.span !== undefined
            ? { width: `${(labelCol.span / 24) * 100}%` } // 动态:labelCol.span 驱动(仅 horizontal)
            : undefined
        }
        className={stylex.props(styles.label).className}
      >
        {showRequired && (
          <span {...stylex.props(styles.requiredMark)} aria-hidden="true">
            *
          </span>
        )}
        {label}
        {tooltip !== undefined && (
          <span title={typeof tooltip === 'string' ? tooltip : undefined} {...stylex.props(styles.tooltipMark)} aria-label="说明">
            ⓘ
          </span>
        )}
      </label>
    )

  const labelRowClass =
    layout === 'vertical'
      ? styles.labelRowVertical
      : layout === 'inline'
        ? styles.labelRowInline
        : styles.labelRowHorizontal
  const labelNode =
    noStyle === true || label === undefined ? null : <div {...stylex.props(labelRowClass)}>{labelText}</div>

  const controlNode = (() => {
    if (effectiveName === undefined || !isElementChild(children)) {
      return <>{children}</>
    }

    const child = children
    const childProps = child.props
    const vp = valuePropName ?? 'value'

    const enhance: Record<string, unknown> = {}
    if (vp === 'checked') enhance.checked = Boolean(childProps.checked)
    else enhance.value = childProps.value

    // Form 级 disabled / size 透传(antd 语义:作用于 Form 内控件)
    if (disabled === true && childProps.disabled === undefined) enhance.disabled = true
    if (size !== undefined && childProps.size === undefined) enhance.size = size

    return (
      <Controller
        control={facade.__control!}
        name={effectiveName as never}
        rules={rhfRules as never}
        defaultValue={initialValue as never}
        render={({ field, fieldState }) => {
          const injected: Record<string, unknown> = {
            ...enhance,
            // onChange: 先写表单值,再以原始入参回调子组件自身 onChange
            // (antd rc-field-form getControlled 同语义);onBlur 与子组件自身回调并存
            onChange: createFieldOnChange(
              (value: unknown) => field.onChange(value),
              childProps.onChange as ((arg: unknown) => void) | undefined,
              (arg: unknown) => adaptChange(arg, vp),
            ),
            onBlur: (event: unknown) => {
              const own = childProps.onBlur as ((e: unknown) => void) | undefined
              own?.(event)
              field.onBlur()
            },
            ref: field.ref,
          }
          if (vp === 'checked') injected.checked = Boolean(field.value)
          else injected.value = field.value
          const errorMessage = fieldState.error?.message
          return (
            <>
              {React.cloneElement(child, injected)}
              {noStyle !== true && errorMessage !== undefined && (
                <div role="alert" {...stylex.props(styles.error)}>
                  {errorMessage}
                </div>
              )}
              {noStyle !== true && warnings.length > 0 && (
                <div role="status" {...stylex.props(styles.warning)}>
                  {warnings.map((warning) => (
                    <div key={warning}>{warning}</div>
                  ))}
                </div>
              )}
              {noStyle !== true && extra !== undefined && (
                <div {...stylex.props(styles.extra)}>{extra}</div>
              )}
            </>
          )
        }}
      />
    )
  })()

  // 无 name 的纯布局容器(antd:rules 不生效,仍渲染 label / extra)
  const plainControl =
    effectiveName === undefined || !isElementChild(children) ? (
      <>
        {children}
        {noStyle !== true && extra !== undefined && (
          <div {...stylex.props(styles.extra)}>{extra}</div>
        )}
      </>
    ) : null

  if (noStyle === true) {
    return <>{controlNode}</>
  }

  const itemClass =
    layout === 'inline'
      ? styles.itemInline
      : layout === 'horizontal'
        ? [styles.item, styles.itemHorizontal]
        : [styles.item]

  const wrapperProps = stylex.props(itemClass as never)
  const controlClass = layout === 'horizontal' ? styles.controlHorizontal : styles.control
  const wrapperStyle = {
    ...wrapperProps.style,
    ...style,
    ...(layout === 'horizontal' && wrapperCol?.span !== undefined
      ? { width: `${(wrapperCol.span / 24) * 100}%` } // 动态:wrapperCol.span 驱动(仅 horizontal)
      : undefined),
  }
  const mergedClass = [wrapperProps.className, className].filter(Boolean).join(' ') || undefined

  return (
    <div className={mergedClass} style={wrapperStyle} {...(rest as Record<string, unknown>)}>
      {labelNode}
      <div className={stylex.props(controlClass).className} style={stylex.props(controlClass).style}>
        {effectiveName !== undefined && isElementChild(children) ? controlNode : plainControl}
      </div>
    </div>
  )
}

FormItem.displayName = 'Form.Item'

// ---------------------------------------------------------------------------
// Form.List(现网 5 处:subscriptions / system.order / promotions / delivery-fee)
// ---------------------------------------------------------------------------

export function FormList(props: FormListProps): React.ReactElement {
  const { name, rules, children } = props
  const binding = useBinding()
  const parentPrefix = React.useContext(ListPrefixContext)
  const prefix: string[] = [...(parentPrefix ?? []), ...toSegments(name)]
  const [, setTick] = React.useState(0)
  React.useEffect(() => {
    if (binding === null) return undefined
    return binding.facade.__subscribe(() => setTick((tick) => tick + 1))
  }, [binding])
  // 列表级 rules 覆盖式登记(每次渲染同步最新闭包);卸载摘除,避免 validateFields
  // 为已卸载列表产出幻影 errorFields
  React.useEffect(() => {
    if (binding === null) return undefined
    binding.facade.__registerListRules(prefix, rules)
    return () => {
      binding.facade.__registerListRules(prefix, undefined)
    }
  })
  if (binding === null) return <>{null}</>

  const { facade } = binding
  const raw = facade.getFieldValue(prefix)
  const list: unknown[] = Array.isArray(raw) ? [...raw] : []

  const apply = (next: unknown[]): void => {
    facade.setFieldValue(prefix, next)
    facade.__clearErrors([prefix])
    // 值变更后重跑列表级 rules(live 出现/清除,如删空最后一行)
    void facade.__validateList(prefix)
  }

  const operation: FormListOperation = {
    add: (defaultValue) => {
      apply([...list, defaultValue])
    },
    remove: (index) => {
      apply(list.filter((_, i) => i !== index))
    },
    move: (from, to) => {
      if (from === to || from < 0 || to < 0 || from >= list.length || to >= list.length) return
      const next = [...list]
      const [moved] = next.splice(from, 1)
      next.splice(to, 0, moved)
      apply(next)
    },
  }

  const fields: FormListField[] = list.map((_, index) => ({ key: index, name: index }))
  const errors = facade.__getListErrors(prefix)

  return (
    <ListPrefixContext.Provider value={prefix}>
      <ListErrorsContext.Provider value={errors}>
        {children(fields, operation, { errors })}
      </ListErrorsContext.Provider>
    </ListPrefixContext.Provider>
  )
}

FormList.displayName = 'Form.List'

// ---------------------------------------------------------------------------
// Form.ErrorList(antd:渲染列表级错误;列表内就近取 meta,独立用法渲染入参)
// ---------------------------------------------------------------------------

export interface ErrorListProps {
  /** 独立使用(不在 Form.List 内)时渲染的错误文案;列表内与上下文合并去重 */
  errors?: React.ReactNode[]
  /** 可见性变化回调(antd 同形;文案有无 → true/false) */
  onVisibleChanged?: (visible: boolean) => void
}

export function ErrorList(props: ErrorListProps): React.ReactElement | null {
  const { errors, onVisibleChanged } = props
  const listErrors = React.useContext(ListErrorsContext)

  const callbackRef = React.useRef(onVisibleChanged)
  React.useEffect(() => {
    callbackRef.current = onVisibleChanged
  })
  const visible = (listErrors?.length ?? 0) + (errors?.length ?? 0) > 0
  const prevVisibleRef = React.useRef(false)
  React.useEffect(() => {
    if (visible !== prevVisibleRef.current) {
      prevVisibleRef.current = visible
      callbackRef.current?.(visible)
    }
  })

  if (listErrors === null) {
    if (errors === undefined || errors.length === 0) return null
    return (
      <>
        {errors.map((error, index) => (
          <div key={index} role="alert" {...stylex.props(styles.error)}>
            {error}
          </div>
        ))}
      </>
    )
  }
  // 列表内:meta.errors 与上下文同源,按序合并去重(防同文案重复渲染)
  const merged: React.ReactNode[] = [...(errors ?? []), ...listErrors].filter(
    (item, index, arr) => arr.indexOf(item) === index,
  )
  if (merged.length === 0) return null
  return (
    <>
      {merged.map((error, index) => (
        <div key={index} role="alert" {...stylex.props(styles.error)}>
          {error}
        </div>
      ))}
    </>
  )
}

ErrorList.displayName = 'Form.ErrorList'

// ---------------------------------------------------------------------------
// Form.useWatch(现网 20 处:订阅单字段值)
// ---------------------------------------------------------------------------

// eslint-disable-next-line @typescript-eslint/no-explicit-any, react-refresh/only-export-components -- 规格要求 antd 同形复合导出(Form.useWatch 等);watch 返回未类型化表单值,泛型化会波及全部 20 处调用点
export function useWatch(name: NamePath, form: FormInstance): any {
  const [, setTick] = React.useState(0)
  const pathKey = normalizeName(name)
  React.useEffect(() => {
    return form.__subscribe(() => setTick((tick) => tick + 1))
  }, [form, pathKey])
  return form.getFieldValue(name)
}

// ---------------------------------------------------------------------------
// Form
// ---------------------------------------------------------------------------

function FormComponent(props: FormProps): React.ReactElement {
  const {
    form,
    layout = 'horizontal',
    initialValues,
    onFinish,
    onFinishFailed,
    onValuesChange,
    disabled,
    size,
    labelCol,
    wrapperCol,
    preserve: _preserve,
    children,
    ...rest
  } = props
  void _preserve

  // 无 form  prop 时自建实例;hook 无条件调用以保证渲染稳定
  const [fallback] = useForm()
  const facade = form ?? fallback

  // 回调 latest-ref:渲染期禁止写 ref,改在每次提交后的 effect 中同步
  const onFinishRef = React.useRef(onFinish)
  const onFinishFailedRef = React.useRef(onFinishFailed)
  const onValuesChangeRef = React.useRef(onValuesChange)
  React.useEffect(() => {
    onFinishRef.current = onFinish
    onFinishFailedRef.current = onFinishFailed
    onValuesChangeRef.current = onValuesChange
  })

  const initialValuesRef = React.useRef(false)
  React.useEffect(() => {
    facade.__attachSubmit({
      onFinish: (values) => onFinishRef.current?.(values),
      onFinishFailed: (info) => onFinishFailedRef.current?.(info),
    })
    if (!initialValuesRef.current) {
      initialValuesRef.current = true
      if (initialValues !== undefined) facade.__setInitialValues(initialValues)
    }
    return undefined
  }, [facade, initialValues])

  React.useEffect(() => {
    return facade.__subscribe((info) => {
      if (info.name === undefined) return
      const callback = onValuesChangeRef.current
      if (callback === undefined) return
      callback({ [info.name]: facade.getFieldValue(info.name) }, facade.getFieldsValue())
    })
  }, [facade])

  const binding = React.useMemo<FormBinding>(
    () => ({ facade, layout, labelCol, wrapperCol, disabled, size }),
    [facade, layout, labelCol, wrapperCol, disabled, size],
  )

  const formClass = layout === 'inline' ? [styles.form, styles.formInline] : [styles.form]
  const formProps = stylex.props(formClass as never)

  return (
    <form
      noValidate
      {...rest}
      className={[formProps.className, rest.className].filter(Boolean).join(' ') || undefined}
      style={{ ...formProps.style, ...rest.style }}
      onSubmit={(event) => {
        event.preventDefault()
        facade.submit()
      }}
    >
      <FormBindingContext.Provider value={binding}>{children}</FormBindingContext.Provider>
    </form>
  )
}

FormComponent.displayName = 'Form'

// ---------------------------------------------------------------------------
// antd 同形静态成员:Form.Item / Form.List / Form.useForm / Form.useWatch
// ---------------------------------------------------------------------------

export const Form = Object.assign(FormComponent, {
  displayName: 'Form',
  Item: FormItem,
  List: FormList,
  ErrorList,
  useForm,
  useWatch,
})
