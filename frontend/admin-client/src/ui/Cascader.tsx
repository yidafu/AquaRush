/**
 * Cascader —— 仅服务 `components/RegionCascader` 一处(spec [S2.3] 末条):
 * 选项树 + 多列弹层(Radix Popover),支持逐级懒加载(loadData 回调会拿到
 * 当前路径节点数组,RegionCascader 直接在节点上挂 children/loading 并刷新
 * options 引用即可回填)。
 *
 * - value: string[] 路径,onChange(values, selectedOptions) 与现网 handleChange
 *   签名一致(第一参路径数组、第二参选中节点数组);
 * - expandTrigger="hover" 时悬停展开,click 选中叶子(changeOnSelect=false);
 * - fieldNames 兼容 RegionCascader 传入的 {label,value,children}(与默认一致)。
 */
import React from 'react'
import * as Popover from '@radix-ui/react-popover'
import * as stylex from '@stylexjs/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { DownOutlined } from './icons'

export interface CascaderOption {
  value: string
  label: React.ReactNode
  children?: CascaderOption[]
  /** RegionCascader 懒加载态:节点自身标记 loading / isLeaf */
  loading?: boolean
  isLeaf?: boolean
  [key: string]: unknown
}

export interface CascaderFieldNames {
  label?: string
  value?: string
  children?: string
}

export interface CascaderProps {
  options?: CascaderOption[]
  /** 受控路径(RegionCascader 同时传 value 与 defaultValue) */
  value?: string[]
  defaultValue?: string[]
  onChange?: (values: string[], selectedOptions: CascaderOption[]) => void
  /** 展开非叶子节点时回调:入参为该节点整条路径(节点为 options 中的真实引用) */
  loadData?: (selectedOptions: CascaderOption[]) => void | Promise<void>
  placeholder?: string
  disabled?: boolean
  /** true = 点击任意层级即可选中(false 时仅叶子可选中) */
  changeOnSelect?: boolean
  expandTrigger?: 'hover' | 'click'
  fieldNames?: CascaderFieldNames
  className?: string
  style?: React.CSSProperties
  id?: string
  name?: string
  'aria-labelledby'?: string
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
      color: tokens.textWeak,
      cursor: 'not-allowed',
    },
  },
  text: {
    flexGrow: '1',
    flexShrink: '1',
    flexBasis: '0%',
    minWidth: '0px',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  placeholder: {
    color: tokens.textWeak,
  },
  suffix: {
    display: 'flex',
    alignItems: 'center',
    flexShrink: '0',
    paddingLeft: '6px',
    color: tokens.textWeak,
    fontSize: '12px',
  },
  content: {
    zIndex: '1050',
    display: 'flex',
    flexDirection: 'row',
    flexWrap: 'nowrap',
    alignItems: 'stretch',
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
  column: {
    minWidth: '148px',
    maxWidth: '200px',
    maxHeight: '280px',
    paddingTop: '4px',
    paddingBottom: '4px',
    paddingLeft: '0px',
    paddingRight: '0px',
    overflowY: 'auto',
    borderRightStyle: 'solid',
    borderRightWidth: '1px',
    borderRightColor: tokens.border,
    ':last-child': {
      borderRightWidth: '0px',
    },
  },
  item: {
    display: 'flex',
    flexDirection: 'row',
    flexWrap: 'nowrap',
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
    color: tokens.brand,
  },
  itemSelected: {
    color: tokens.brand,
    fontWeight: '600',
  },
  arrow: {
    marginLeft: '8px',
    color: tokens.textWeak,
    fontSize: '10px',
    flexShrink: '0',
  },
  loadingBox: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: '14px',
    paddingBottom: '14px',
    paddingLeft: '12px',
    paddingRight: '12px',
    color: tokens.textWeak,
    fontSize: '12px',
  },
  spinner: {
    display: 'inline-block',
    width: '14px',
    height: '14px',
    animationName: spinKeyframes,
    animationDuration: '1s',
    animationTimingFunction: 'linear',
    animationIterationCount: 'infinite',
  },
  emptyBox: {
    paddingTop: '12px',
    paddingBottom: '12px',
    paddingLeft: '12px',
    paddingRight: '12px',
    color: tokens.textWeak,
    fontSize: tokens.fsAux,
    textAlign: 'center',
  },
})

function useFieldKeys(fieldNames?: CascaderFieldNames): { labelKey: string; valueKey: string; childrenKey: string } {
  return {
    labelKey: fieldNames?.label ?? 'label',
    valueKey: fieldNames?.value ?? 'value',
    childrenKey: fieldNames?.children ?? 'children',
  }
}

export function Cascader(props: CascaderProps): React.ReactElement {
  const {
    options,
    value,
    defaultValue,
    onChange,
    loadData,
    placeholder = '请选择',
    disabled,
    changeOnSelect = false,
    expandTrigger = 'click',
    fieldNames,
    className,
    style,
    id,
    name,
    ...rest
  } = props

  const { labelKey, valueKey, childrenKey } = useFieldKeys(fieldNames)
  const [open, setOpen] = React.useState(false)
  const [chain, setChain] = React.useState<CascaderOption[]>([])
  const requested = React.useRef<Set<string>>(new Set())

  const childrenOf = React.useCallback(
    (node: CascaderOption): CascaderOption[] | undefined => node[childrenKey] as CascaderOption[] | undefined,
    [childrenKey],
  )
  const valueOf = React.useCallback(
    (node: CascaderOption): string => String(node[valueKey]),
    [valueKey],
  )
  const labelOf = (node: CascaderOption): React.ReactNode => node[labelKey] as React.ReactNode

  const nodePathKey = React.useCallback(
    (nodes: CascaderOption[]): string => nodes.map(valueOf).join('/'),
    [valueOf],
  )

  const findPath = React.useCallback(
    (path: string[] | undefined): CascaderOption[] => {
      if (path === undefined || path.length === 0 || options === undefined) return []
      const result: CascaderOption[] = []
      let level = options
      for (const segment of path) {
        const next = level.find((node) => valueOf(node) === segment)
        if (next === undefined) break
        result.push(next)
        const kids = childrenOf(next)
        if (kids === undefined || kids.length === 0) break
        level = kids
      }
      return result
    },
    [options, childrenOf, valueOf],
  )

  // 受控 value 变化(RegionCascader 预取路径后会回写)→ 同步展开链
  const valueKeyStr = value === undefined ? '' : value.join('/')
  React.useEffect(() => {
    if (!open) return
    setChain(findPath(value ?? defaultValue))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [valueKeyStr, open])

  const openWithInitial = React.useCallback(() => {
    setChain(findPath(value ?? defaultValue))
  }, [findPath, value, defaultValue])

  const maybeLoad = React.useCallback(
    (nodes: CascaderOption[]): void => {
      if (loadData === undefined || nodes.length === 0) return
      const last = nodes[nodes.length - 1]
      if (last.isLeaf === true) return
      const kids = childrenOf(last)
      if (kids !== undefined && kids.length > 0) return
      const key = nodePathKey(nodes)
      if (requested.current.has(key)) return
      requested.current.add(key)
      void loadData(nodes)
    },
    [loadData, childrenOf, nodePathKey],
  )

  const preview = (nodes: CascaderOption[]): void => {
    setChain(nodes)
    maybeLoad(nodes)
  }

  const commit = (nodes: CascaderOption[]): void => {
    if (nodes.length === 0) return
    setChain(nodes)
    onChange?.(nodes.map(valueOf), nodes)
    setOpen(false)
  }

  const isLeafNode = (node: CascaderOption): boolean => {
    if (node.isLeaf === true) return true
    const kids = childrenOf(node)
    if (kids === undefined) return true
    if (kids.length > 0) return false
    // children 为空数组:未请求过且可懒加载 → 还不是叶子
    if (loadData !== undefined && !requested.current.has(nodePathKey([...chain, node]))) {
      return false
    }
    return true
  }

  const onItemEnter = (nodes: CascaderOption[]): void => {
    if (expandTrigger !== 'hover' || disabled === true) return
    preview(nodes)
  }

  const onItemClick = (nodes: CascaderOption[]): void => {
    if (disabled === true) return
    const last = nodes[nodes.length - 1]
    preview(nodes)
    if (changeOnSelect || isLeafNode(last)) {
      commit(nodes)
    }
  }

  // 展示文本:按 value 路径回显 label(未加载到的层级回退原始值)
  const displayPath = React.useMemo(() => {
    const path = value ?? defaultValue
    if (path === undefined || path.length === 0) return []
    const nodes = findPath(path)
    return path.map((segment, index) => {
      const node = nodes[index]
      return node !== undefined ? labelOf(node) : segment
    })
  }, [value, defaultValue, findPath]) // eslint-disable-line react-hooks/exhaustive-deps

  const columns: CascaderOption[][] = [options ?? []]
  for (let i = 0; i < chain.length; i += 1) {
    const kids = childrenOf(chain[i])
    columns.push(kids ?? [])
  }

  const triggerProps = stylex.props(styles.trigger)
  const wrapperProps = stylex.props(styles.wrapper)
  const mergedClass = [wrapperProps.className, className].filter(Boolean).join(' ') || undefined

  return (
    <span className={mergedClass} style={{ ...wrapperProps.style, ...style }}>
      <Popover.Root
        open={open && disabled !== true}
        onOpenChange={(next) => {
          setOpen(next)
          if (next) openWithInitial()
        }}
      >
        <Popover.Trigger asChild>
          <button
            type="button"
            id={id}
            name={name}
            disabled={disabled}
            className={triggerProps.className}
            style={triggerProps.style}
            {...rest}
          >
            <span {...stylex.props(styles.text)}>
              {displayPath.length > 0 ? (
                displayPath.join(' / ')
              ) : (
                <span {...stylex.props(styles.placeholder)}>{placeholder}</span>
              )}
            </span>
            <span {...stylex.props(styles.suffix)}>
              <DownOutlined />
            </span>
          </button>
        </Popover.Trigger>
        <Popover.Portal>
          <Popover.Content
            sideOffset={4}
            collisionPadding={8}
            className={stylex.props(styles.content).className}
            style={stylex.props(styles.content).style}
          >
            {columns.map((nodes, level) => {
              const parent = level === 0 ? undefined : chain[level - 1]
              const active = chain[level]
              const loadingColumn = parent !== undefined && parent.loading === true
              return (
                <div key={`level-${level}`} {...stylex.props(styles.column)}>
                  {loadingColumn ? (
                    <div {...stylex.props(styles.loadingBox)}>
                      <span {...stylex.props(styles.spinner)} />
                    </div>
                  ) : nodes.length === 0 ? (
                    <div {...stylex.props(styles.emptyBox)}>暂无数据</div>
                  ) : (
                    nodes.map((node, index) => {
                      const nodeChain = [...chain.slice(0, level), node]
                      const selected = active !== undefined && valueOf(active) === valueOf(node)
                      const parts = stylex.props(
                        styles.item,
                        ...(selected ? [styles.itemActive] : []),
                      )
                      const leaf = isLeafNode(node)
                      return (
                        <div
                          key={`${valueOf(node)}-${index}`}
                          role="option"
                          aria-selected={selected}
                          aria-disabled={node.loading === true}
                          onMouseEnter={() => onItemEnter(nodeChain)}
                          onMouseDown={(event) => event.preventDefault()}
                          onClick={() => onItemClick(nodeChain)}
                          className={parts.className}
                          style={parts.style}
                        >
                          <span>{labelOf(node)}</span>
                          {!leaf && (
                            <span {...stylex.props(styles.arrow)}>
                              {node.loading === true ? <span {...stylex.props(styles.spinner)} /> : '›'}
                            </span>
                          )}
                        </div>
                      )
                    })
                  )}
                </div>
              )
            })}
          </Popover.Content>
        </Popover.Portal>
      </Popover.Root>
    </span>
  )
}

Cascader.displayName = 'Cascader'
