/**
 * Select(antd 形状,spec [S2.3]):Radix Popover 承载弹层与外点关闭 / Esc,
 * 自绘 listbox 语义(option/aria)。
 *
 * 之所以不用 @radix-ui/react-select:现网 Select 有 showSearch + onSearch +
 * notFoundContent(服务端搜索,3 组表单)与多选 tags,radix-select 的 trigger
 * 不允许嵌入搜索输入(官方亦推荐此类场景用 Popover/combobox 模式)。
 *
 * 值语义与 antd 一致:onChange 收裸值(非事件);mode="multiple" 收数组;
 * allowClear 单选清空回 undefined、多选回 []。
 */
import React from 'react'
import * as Popover from '@radix-ui/react-popover'
import * as stylex from '@stylexjs/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { CloseCircleOutlined, CloseOutlined, DownOutlined, SyncOutlined } from './icons'

export interface SelectOption<V = unknown> {
  value: V
  label: React.ReactNode
  disabled?: boolean
}

export interface SelectProps<V = unknown> {
  options?: Array<SelectOption<V>>
  value?: V | V[] | null
  /** antd 语义:裸值;multiple 为数组;allowClear 单选清空为 undefined */
  onChange?: (value: V | V[] | undefined) => void
  mode?: 'multiple'
  placeholder?: string
  disabled?: boolean
  allowClear?: boolean
  showSearch?: boolean
  /** false = 服务端搜索(不过滤本地);函数 = 自定义过滤;缺省按 optionFilterProp 过滤 */
  filterOption?: boolean | ((input: string, option: SelectOption<V>) => boolean)
  optionFilterProp?: 'label' | 'value'
  onSearch?: (searchText: string) => void
  size?: 'small' | 'middle' | 'large'
  loading?: boolean
  notFoundContent?: React.ReactNode
  maxTagCount?: number | 'responsive'
  onFocus?: () => void
  onBlur?: () => void
  className?: string
  style?: React.CSSProperties
  id?: string
  name?: string
  'aria-labelledby'?: string
  /** antd 静态子组件 <Select.Option> 形态(现网 8 个文件) */
  children?: React.ReactNode
}

/** 标记型子组件:自身不渲染,由 Select 提取 props(label 可来自 children) */
function OptionMarker(
  _props: Omit<SelectOption<unknown>, 'label'> & { label?: React.ReactNode; children?: React.ReactNode },
): null {
  void _props
  return null
}

const spinKeyframes = stylex.keyframes({
  from: { transform: 'rotate(0deg)' },
  to: { transform: 'rotate(360deg)' },
})

const styles = stylex.create({
  wrapper: {
    position: 'relative',
    display: 'block',
    width: '100%',
    verticalAlign: 'middle',
    fontFamily: tokens.font,
  },
  trigger: {
    display: 'flex',
    flexDirection: 'row',
    flexWrap: 'nowrap',
    alignItems: 'center',
    width: '100%',
    minHeight: '32px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '10px',
    paddingRight: '8px',
    backgroundColor: tokens.card,
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.borderStrong,
    borderTopLeftRadius: tokens.rSm,
    borderTopRightRadius: tokens.rSm,
    borderBottomRightRadius: tokens.rSm,
    borderBottomLeftRadius: tokens.rSm,
    color: tokens.text,
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
    textAlign: 'left',
    cursor: 'pointer',
    transitionProperty: 'border-color, box-shadow',
    transitionDuration: '0.2s',
    transitionTimingFunction: 'ease',
    ':hover': {
      borderColor: tokens.brand,
    },
    ':focus-visible': {
      outlineStyle: 'none',
      borderColor: tokens.brand,
      boxShadow: '0 0 0 2px rgba(58, 155, 217, 0.16)', // 品牌蓝 focus 环
    },
    ':disabled': {
      backgroundColor: tokens.bg,
      borderColor: tokens.borderStrong,
      color: tokens.textWeak,
      cursor: 'not-allowed',
    },
  },
  triggerSmall: {
    minHeight: '24px',
    paddingLeft: '6px',
  },
  triggerLarge: {
    minHeight: '40px',
    paddingLeft: '14px',
  },
  valueArea: {
    display: 'flex',
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    flexGrow: '1',
    flexShrink: '1',
    flexBasis: '0%',
    minWidth: '0px',
    rowGap: '0px',
    columnGap: '0px',
    paddingTop: '3px',
    paddingBottom: '3px',
    paddingLeft: '0px',
    paddingRight: '0px',
    overflow: 'hidden',
  },
  placeholder: {
    color: tokens.textWeak,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  singleValue: {
    color: tokens.text,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  tag: {
    display: 'inline-flex',
    flexDirection: 'row',
    alignItems: 'center',
    maxWidth: '100%',
    paddingTop: '1px',
    paddingBottom: '1px',
    paddingLeft: '6px',
    paddingRight: '4px',
    marginTop: '1px',
    marginBottom: '1px',
    marginLeft: '0px',
    marginRight: '4px',
    backgroundColor: tokens.statusInfoBg,
    borderStyle: 'none',
    borderRadius: '4px',
    color: tokens.statusInfoFg,
    fontSize: tokens.fsNote,
    lineHeight: '18px',
  },
  tagText: {
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  tagClose: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '14px',
    height: '14px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '2px',
    marginRight: '0px',
    borderStyle: 'none',
    backgroundColor: 'transparent',
    color: 'inherit',
    cursor: 'pointer',
    fontSize: '10px',
  },
  overflowTag: {
    paddingTop: '1px',
    paddingBottom: '1px',
    paddingLeft: '4px',
    paddingRight: '4px',
    marginTop: '1px',
    marginBottom: '1px',
    marginLeft: '0px',
    marginRight: '4px',
    color: tokens.textSub,
    fontSize: tokens.fsNote,
    lineHeight: '18px',
  },
  suffix: {
    display: 'flex',
    alignItems: 'center',
    flexShrink: '0',
    paddingLeft: '6px',
    color: tokens.textWeak,
    fontSize: '12px',
  },
  clearBtn: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '14px',
    height: '14px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
    marginRight: '0px',
    borderStyle: 'none',
    backgroundColor: 'transparent',
    color: tokens.textWeak,
    cursor: 'pointer',
    ':hover': {
      color: tokens.textSub,
    },
  },
  content: {
    zIndex: '1050',
    backgroundColor: tokens.card,
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.border,
    borderTopLeftRadius: tokens.rSm,
    borderTopRightRadius: tokens.rSm,
    borderBottomRightRadius: tokens.rSm,
    borderBottomLeftRadius: tokens.rSm,
    boxShadow: tokens.shModal,
    overflow: 'hidden',
    fontFamily: tokens.font,
  },
  searchBox: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    paddingTop: '6px',
    paddingBottom: '6px',
    paddingLeft: '8px',
    paddingRight: '8px',
    borderBottomStyle: 'solid',
    borderBottomWidth: '1px',
    borderBottomColor: tokens.border,
  },
  searchInput: {
    flexGrow: '1',
    flexShrink: '1',
    flexBasis: '0%',
    minWidth: '0px',
    paddingTop: '4px',
    paddingBottom: '4px',
    paddingLeft: '6px',
    paddingRight: '6px',
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.borderStrong,
    borderTopLeftRadius: '4px',
    borderTopRightRadius: '4px',
    borderBottomRightRadius: '4px',
    borderBottomLeftRadius: '4px',
    outlineStyle: 'none',
    color: tokens.text,
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    backgroundColor: tokens.card,
    '::placeholder': {
      color: tokens.textWeak,
    },
    ':focus': {
      borderColor: tokens.brand,
    },
  },
  list: {
    maxHeight: '264px',
    paddingTop: '4px',
    paddingBottom: '4px',
    paddingLeft: '0px',
    paddingRight: '0px',
    marginLeft: '0px',
    marginRight: '0px',
    overflowY: 'auto',
    overscrollBehavior: 'contain',
  },
  item: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: '6px',
    paddingBottom: '6px',
    paddingLeft: '10px',
    paddingRight: '10px',
    color: tokens.text,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
    cursor: 'pointer',
    backgroundColor: 'transparent',
    ':hover': {
      backgroundColor: tokens.brandTint,
    },
  },
  itemActive: {
    backgroundColor: tokens.brandTint,
  },
  itemSelected: {
    color: tokens.brand,
    fontWeight: '600',
  },
  itemDisabled: {
    color: tokens.textWeak,
    cursor: 'not-allowed',
    backgroundColor: 'transparent',
  },
  checkMark: {
    marginLeft: '8px',
    color: tokens.brand,
    fontSize: '12px',
  },
  empty: {
    paddingTop: '16px',
    paddingBottom: '16px',
    paddingLeft: '12px',
    paddingRight: '12px',
    color: tokens.textWeak,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
    textAlign: 'center',
  },
  spinner: {
    display: 'inline-block',
    width: '14px',
    height: '14px',
    color: tokens.textWeak,
    animationName: spinKeyframes,
    animationDuration: '1s',
    animationTimingFunction: 'linear',
    animationIterationCount: 'infinite',
  },
})


function sameValue(a: unknown, b: unknown): boolean {
  if (a === b) return true
  if (a === null || b === null || a === undefined || b === undefined) return false
  return String(a) === String(b)
}

export function SelectImpl<V = unknown>(props: SelectProps<V>): React.ReactElement {
  const {
    options,
    value,
    onChange,
    mode,
    placeholder = '请选择',
    disabled,
    allowClear,
    showSearch,
    filterOption,
    optionFilterProp = 'value',
    onSearch,
    size = 'middle',
    loading,
    notFoundContent,
    maxTagCount,
    onFocus,
    onBlur,
    className,
    style,
    id,
    name,
    children,
    ...rest
  } = props

  const [open, setOpen] = React.useState(false)
  const [search, setSearch] = React.useState('')
  const [activeIndex, setActiveIndex] = React.useState(-1)

  // options prop + <Select.Option> 子组件 合并(现网两种形态并存)
  const mergedOptions = React.useMemo<Array<SelectOption<V>>>(() => {
    const fromChildren: Array<SelectOption<V>> = []
    React.Children.forEach(children, (child) => {
      if (!React.isValidElement(child)) return
      const el = child as React.ReactElement<Record<string, unknown>>
      if (el.type === OptionMarker) {
        fromChildren.push({
          value: el.props.value as V,
          label: (el.props.children ?? el.props.label) as React.ReactNode,
          disabled: el.props.disabled as boolean | undefined,
        })
      }
    })
    return [...(options ?? []), ...fromChildren]
  }, [options, children])

  const multiple = mode === 'multiple'
  // useMemo:保持数组引用稳定,下游 visibleTags 的手动 memo 才能被 React Compiler 保留
  const selectedValues = React.useMemo<unknown[]>(() => {
    if (multiple) return Array.isArray(value) ? [...(value as unknown[])] : []
    return value === undefined || value === null ? [] : [value]
  }, [multiple, value])

  const labelOf = React.useCallback(
    (optionValue: unknown): React.ReactNode => {
      const found = mergedOptions.find((option) => sameValue(option.value, optionValue))
      if (found !== undefined) return found.label
      return optionValue as React.ReactNode // 选项未加载(异步 options)时回显原始值
    },
    [mergedOptions],
  )

  const keyword = search.trim()
  const filtered = React.useMemo(() => {
    if (!showSearch || keyword === '') return mergedOptions
    if (filterOption === false) return mergedOptions
    const matcher =
      typeof filterOption === 'function'
        ? filterOption
        : (input: string, option: SelectOption<V>): boolean => {
            const target = optionFilterProp === 'value' ? option.value : option.label
            return String(target).toLowerCase().includes(input.toLowerCase())
          }
    return mergedOptions.filter((option) => matcher(keyword, option))
  }, [mergedOptions, showSearch, keyword, filterOption, optionFilterProp])

  const optionIndex = (optionValue: unknown): number =>
    selectedValues.findIndex((selected) => sameValue(selected, optionValue))

  const choose = (option: SelectOption<V>): void => {
    if (option.disabled === true || disabled === true) return
    if (multiple) {
      const has = optionIndex(option.value) >= 0
      const next = has
        ? selectedValues.filter((selected) => !sameValue(selected, option.value))
        : [...selectedValues, option.value]
      onChange?.(next as V[])
      return
    }
    onChange?.(option.value)
    setOpen(false)
    setSearch('')
  }

  const clear = (event: React.MouseEvent): void => {
    event.stopPropagation()
    if (disabled === true) return
    onChange?.(multiple ? ([] as V[]) : undefined)
    setSearch('')
  }

  const removeTag = (tagValue: unknown, event: React.MouseEvent): void => {
    event.stopPropagation()
    if (disabled === true) return
    onChange?.(selectedValues.filter((selected) => !sameValue(selected, tagValue)) as V[])
  }

  const visibleTags = React.useMemo(() => {
    const limit =
      typeof maxTagCount === 'number' && maxTagCount >= 0 ? maxTagCount : selectedValues.length
    return { shown: selectedValues.slice(0, limit), rest: Math.max(0, selectedValues.length - limit) }
  }, [selectedValues, maxTagCount])

  const handleListKeyDown = (event: React.KeyboardEvent): void => {
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setActiveIndex((prev) => (prev + 1) % Math.max(filtered.length, 1))
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      setActiveIndex((prev) => (prev <= 0 ? filtered.length - 1 : prev - 1))
    } else if (event.key === 'Enter') {
      event.preventDefault()
      const option = filtered[activeIndex]
      if (option !== undefined) choose(option)
    }
  }

  const triggerSize =
    size === 'small' ? styles.triggerSmall : size === 'large' ? styles.triggerLarge : null
  const triggerProps = triggerSize
    ? stylex.props(styles.trigger, triggerSize)
    : stylex.props(styles.trigger)
  const wrapperProps = stylex.props(styles.wrapper)
  const mergedClass = [wrapperProps.className, className].filter(Boolean).join(' ') || undefined

  const hasValue = selectedValues.length > 0
  const showClear = allowClear === true && hasValue && disabled !== true

  const valueNode = multiple ? (
    <span {...stylex.props(styles.valueArea)}>
      {visibleTags.shown.map((tagValue, index) => (
        <span key={`${String(tagValue)}-${index}`} {...stylex.props(styles.tag)}>
          <span {...stylex.props(styles.tagText)}>{labelOf(tagValue)}</span>
          <button
            type="button"
            aria-label="移除"
            tabIndex={-1}
            onClick={(event) => removeTag(tagValue, event)}
            {...stylex.props(styles.tagClose)}
          >
            <CloseOutlined />
          </button>
        </span>
      ))}
      {visibleTags.rest > 0 && <span {...stylex.props(styles.overflowTag)}>+{visibleTags.rest}</span>}
      {!hasValue && <span {...stylex.props(styles.placeholder)}>{placeholder}</span>}
    </span>
  ) : (
    <span {...stylex.props(styles.valueArea)}>
      {hasValue ? (
        <span {...stylex.props(styles.singleValue)}>{labelOf(selectedValues[0])}</span>
      ) : (
        <span {...stylex.props(styles.placeholder)}>{placeholder}</span>
      )}
    </span>
  )

  const listContent =
    loading === true && filtered.length === 0 ? (
      <div {...stylex.props(styles.empty)}>
        <SyncOutlined spin />
      </div>
    ) : filtered.length === 0 ? (
      <div {...stylex.props(styles.empty)}>{notFoundContent ?? '暂无数据'}</div>
    ) : (
      <div role="listbox" aria-multiselectable={multiple} {...stylex.props(styles.list)} onKeyDown={handleListKeyDown}>
        {filtered.map((option, index) => {
          const selected = optionIndex(option.value) >= 0
          const itemProps = stylex.props(
            styles.item,
            ...(index === activeIndex ? [styles.itemActive] : []),
            ...(selected ? [styles.itemSelected] : []),
            ...(option.disabled === true ? [styles.itemDisabled] : []),
          )
          return (
            <div
              key={`${String(option.value)}-${index}`}
              role="option"
              aria-selected={selected}
              aria-disabled={option.disabled === true}
              onMouseEnter={() => setActiveIndex(index)}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => choose(option)}
              className={itemProps.className}
              style={itemProps.style}
            >
              <span>{option.label}</span>
              {selected && <span {...stylex.props(styles.checkMark)}>✓</span>}
            </div>
          )
        })}
      </div>
    )

  return (
    <span className={mergedClass} style={{ ...wrapperProps.style, ...style }}>
      <Popover.Root open={open && disabled !== true} onOpenChange={(next) => setOpen(next)}>
        <Popover.Trigger asChild>
          <button
            type="button"
            id={id}
            name={name}
            role="combobox"
            aria-expanded={open}
            aria-controls={undefined}
            disabled={disabled}
            className={triggerProps.className}
            style={triggerProps.style}
            onFocus={() => onFocus?.()}
            onBlur={() => onBlur?.()}
            onClick={() => {
              setSearch('')
              setActiveIndex(-1)
            }}
            {...rest}
          >
            {valueNode}
            <span {...stylex.props(styles.suffix)}>
              {showClear && (
                <button
                  type="button"
                  aria-label="清空"
                  tabIndex={-1}
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={clear}
                  {...stylex.props(styles.clearBtn)}
                >
                  <CloseCircleOutlined />
                </button>
              )}
              {loading === true ? <SyncOutlined spin /> : <DownOutlined />}
            </span>
          </button>
        </Popover.Trigger>
        <Popover.Portal>
          <Popover.Content
            sideOffset={4}
            collisionPadding={8}
            className={stylex.props(styles.content).className}
            style={stylex.props(styles.content).style}
            onKeyDown={handleListKeyDown}
          >
            {showSearch && (
              <div {...stylex.props(styles.searchBox)}>
                {/* 动态:搜索词由组件状态驱动,保留内联输入 */}
                <input
                  autoFocus
                  value={search}
                  placeholder="搜索"
                  aria-label="搜索选项"
                  onChange={(event) => {
                    const next = event.target.value
                    setSearch(next)
                    setActiveIndex(-1)
                    onSearch?.(next)
                  }}
                  className={stylex.props(styles.searchInput).className}
                />
              </div>
            )}
            {listContent}
          </Popover.Content>
        </Popover.Portal>
      </Popover.Root>
    </span>
  )
}

SelectImpl.displayName = 'Select'

export const Select = Object.assign(SelectImpl, {
  displayName: 'Select',
  Option: OptionMarker,
})

export { OptionMarker as Option }
