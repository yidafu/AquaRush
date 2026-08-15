'use strict'

/**
 * babel-plugin-aquarush-track
 *
 * 声明式埋点编译插件:把 JSX 元素上的 `track-*` props 编译成
 * `<Track>` 包裹(来自 @aquarush/common/tracking),并自动注入 import。
 *
 * 输入:
 *   <Button trackTarget="login-submit" trackElementText="登录"
 *           trackProps={{ username }} onClick={handle}>登录</Button>
 * 输出:
 *   <Track target="login-submit" elementText="登录" props={{ username }}>
 *     <Button onClick={handle}>登录</Button>
 *   </Track>
 *   + import { Track } from '@aquarush/common/tracking'
 *
 * 契约(与 common TrackAttributes 对齐):
 *   trackTarget      → target(必填,缺失则跳过该元素并告警)
 *   trackAction      → action
 *   trackProps       → props(对象表达式 / JSON 字符串 / 其他表达式)
 *   trackElementId   → elementId
 *   trackElementType → elementType
 *   trackElementText → elementText
 *   trackDisabled    → disabled(动态跳过上报,仍转发事件)
 *   trackIgnore      → 值为真时剥离全部 track-* 属性、不包裹
 *   其他 trackXxx    → 首字母小写为 xxx,合并进 props
 *
 * 约束:
 *   - 只处理 JSX 属性;createElement 调用不在范围。
 *   - 元素已有的 onClick/onPress/onSubmit/onConfirm 保留在子元素上
 *     (Track 注入的 composed handler 会链式调用,不冲突、不重复上报)。
 *   - key 移到外层 <Track>,ref 留在内层元素;JSXSpreadAttribute 原样保留。
 *   - 不要在一个 <Track> 内再放带 track-* 的子元素(避免双重上报)。
 */

import { types as t } from '@babel/core'
import type { ConfigAPI, PluginObj, PluginPass } from '@babel/core'
import type { NodePath } from '@babel/traverse'

const KEBAB = /^track-/

/** track-target → trackTarget(首字母大写 + 驼峰);camelCase 原样返回 */
function normalizeName(name: string): string {
  if (KEBAB.test(name)) {
    const rest = name.slice(6).replace(/-([a-z])/g, (_, c) => c.toUpperCase())
    return 'track' + rest.charAt(0).toUpperCase() + rest.slice(1)
  }
  return name
}

/** JSXAttribute 的属性名(JSXNamespacedName 返回空串,不参与匹配) */
function attrName(p: NodePath<t.JSXAttribute>): string {
  const name = p.node.name
  return t.isJSXIdentifier(name) ? name.name : ''
}

/** 保留键:trackXxx → Track prop 名 */
const RESERVED: Record<string, string | null> = {
  trackTarget: 'target',
  trackAction: 'action',
  trackProps: 'props',
  trackElementId: 'elementId',
  trackElementType: 'elementType',
  trackElementText: 'elementText',
  trackDisabled: 'disabled', // 动态跳过上报(仍转发事件)
  trackIgnore: null, // 特殊处理:静态剥离模式
}

/** 每文件状态(存于 PluginPass;closure 状态会被 babel 跨文件缓存复用,不可用) */
interface TrackFileState {
  wrapCount: number
  trackLocalName: string | null
}

type PluginPassWithTrack = PluginPass & { track?: TrackFileState }

type PropsBase =
  | { kind: 'object'; node: t.ObjectExpression }
  | { kind: 'spread'; node: t.Expression }

type TrackAttrValue = t.JSXAttribute['value']

namespace babelPluginAquarushTrack {
  export interface TrackPluginOptions {
    /** Track 组件来源,默认 '@aquarush/common/tracking' */
    moduleSource?: string
  }
}

function babelPluginAquarushTrack(
  api: ConfigAPI,
  options: babelPluginAquarushTrack.TrackPluginOptions = {},
): PluginObj {
  const moduleSource = options.moduleSource || '@aquarush/common/tracking'

  function fileState(state: PluginPass): TrackFileState {
    const wrapped = state as PluginPassWithTrack
    if (!wrapped.track) wrapped.track = { wrapCount: 0, trackLocalName: null }
    return wrapped.track
  }

  /** JSXAttribute 值路径求值(裸属性视为 true;字符串 'false'/'0'/'' 视为假) */
  function evaluateValue(valuePath: NodePath<any> | null | undefined): unknown {
    if (!valuePath || valuePath.node == null) return true
    if (valuePath.isStringLiteral()) {
      const v = valuePath.node.value
      if (v === 'false' || v === '0' || v === '') return false
      return v
    }
    if (valuePath.isBooleanLiteral()) return valuePath.node.value
    if (valuePath.isJSXExpressionContainer()) {
      const res = valuePath.get('expression').evaluate()
      return res.confident ? res.value : undefined
    }
    return undefined
  }

  /** 解析 trackLocalName:复用已有同源 import,冲突时生成唯一名 */
  function resolveTrackName(path: NodePath<t.JSXOpeningElement>): string {
    const scope = path.scope.getProgramParent()
    const binding = scope.getBinding('Track')
    if (binding) {
      const importDecl = binding.path.findParent((p) => p.isImportDeclaration()) as
        | NodePath<t.ImportDeclaration>
        | null
      if (importDecl && importDecl.node.source.value === moduleSource) {
        return 'Track' // 已有同源 import,直接复用
      }
      let i = 1
      while (scope.getBinding(`Track$${i}`)) i += 1
      return `Track$${i}`
    }
    return 'Track'
  }

  /** 散列键 trackOrderNo → 'orderNo' */
  function spreadKey(normalized: string): string {
    const key = normalized.slice('track'.length)
    return key.charAt(0).toLowerCase() + key.slice(1)
  }

  return {
    name: 'babel-plugin-aquarush-track',

    visitor: {
      JSXOpeningElement(path: NodePath<t.JSXOpeningElement>, state: PluginPass) {
        const allAttrs = path.get('attributes')
        const namedAttrs = allAttrs.filter(
          (p): p is NodePath<t.JSXAttribute> =>
            p.isJSXAttribute() && t.isJSXIdentifier(p.node.name),
        )
        const spreadAttrs = allAttrs.filter((p) => p.isJSXSpreadAttribute())
        const trackAttrs = namedAttrs.filter((p) =>
          normalizeName(attrName(p)).startsWith('track'),
        )
        if (trackAttrs.length === 0) return

        // trackIgnore 为真 → 剥离全部 track-* 属性,不包裹
        const ignoreAttr = trackAttrs.find(
          (p) => normalizeName(attrName(p)) === 'trackIgnore',
        )
        if (ignoreAttr && evaluateValue(ignoreAttr.get('value'))) {
          for (const p of trackAttrs) p.remove()
          return
        }

        // trackTarget 必填
        const targetAttr = trackAttrs.find(
          (p) => normalizeName(attrName(p)) === 'trackTarget',
        )
        if (!targetAttr) {
          console.warn(
            '[babel-plugin-aquarush-track] trackTarget is required, element skipped:',
            String(path.node.name),
          )
          return
        }

        // 组装 Track props
        const mapped: Record<string, TrackAttrValue> = {}
        let propsBase: PropsBase | null = null
        const extraProps: t.ObjectProperty[] = []

        for (const p of trackAttrs) {
          const normalized = normalizeName(attrName(p))
          const value: TrackAttrValue = p.node.value
          if (normalized === 'trackIgnore') continue
          if (normalized === 'trackProps') {
            const expr = value && t.isJSXExpressionContainer(value) ? value.expression : value
            if (t.isObjectExpression(expr)) {
              propsBase = { kind: 'object', node: expr }
            } else if (t.isStringLiteral(value)) {
              try {
                const node = t.valueToNode(JSON.parse(value.value))
                propsBase = t.isObjectExpression(node) ? { kind: 'object', node } : null
              } catch (error) {
                console.warn(
                  '[babel-plugin-aquarush-track] invalid trackProps json, ignored:',
                  value.value,
                  error,
                )
              }
            } else if (expr) {
              propsBase = { kind: 'spread', node: expr as t.Expression }
            }
            continue
          }
          const trackProp = RESERVED[normalized]
          if (trackProp) {
            mapped[trackProp] = value
          } else {
            // 散列键
            const valueNode: t.Expression =
              value && t.isJSXExpressionContainer(value)
                ? (value.expression as t.Expression)
                : value ?? t.booleanLiteral(true)
            extraProps.push(t.objectProperty(t.identifier(spreadKey(normalized)), valueNode))
          }
        }

        // 组装 props JSXAttribute 值
        let propsAttrValue: t.JSXExpressionContainer | null = null
        if (propsBase) {
          if (propsBase.kind === 'object') {
            propsAttrValue = t.jsxExpressionContainer(
              extraProps.length > 0
                ? t.objectExpression([...propsBase.node.properties, ...extraProps])
                : propsBase.node,
            )
          } else {
            // spread 表达式
            propsAttrValue = t.jsxExpressionContainer(
              extraProps.length > 0
                ? t.objectExpression([t.spreadElement(propsBase.node), ...extraProps])
                : propsBase.node,
            )
          }
        } else if (extraProps.length > 0) {
          propsAttrValue = t.jsxExpressionContainer(t.objectExpression(extraProps))
        }

        // 包装属性顺序:action, target, props, elementId, elementType, elementText, disabled
        const wrapperAttrNodes: (t.JSXAttribute | t.JSXSpreadAttribute)[] = []
        const order = [
          'action',
          'target',
          'props',
          'elementId',
          'elementType',
          'elementText',
          'disabled',
        ] as const
        for (const key of order) {
          if (key === 'props') {
            if (propsAttrValue) {
              wrapperAttrNodes.push(t.jsxAttribute(t.jsxIdentifier('props'), propsAttrValue))
            }
          } else if (mapped[key]) {
            wrapperAttrNodes.push(t.jsxAttribute(t.jsxIdentifier(key), mapped[key]))
          }
        }

        // 内层元素:去掉 track 属性;key 移到外层;ref 与 JSXSpreadAttribute 留在内层
        const innerAttrNodes: (t.JSXAttribute | t.JSXSpreadAttribute)[] = []
        for (const p of namedAttrs) {
          if (trackAttrs.includes(p)) continue
          const node = p.node
          if (t.isJSXIdentifier(node.name) && node.name.name === 'key') {
            wrapperAttrNodes.unshift(node)
            continue
          }
          innerAttrNodes.push(node)
        }
        for (const p of spreadAttrs) innerAttrNodes.push(p.node)

        // 解析最终标识符并构建包裹
        const s = fileState(state)
        const trackLocalName = s.trackLocalName ?? resolveTrackName(path)
        s.trackLocalName = trackLocalName
        const elementNode = path.parentPath!.node as t.JSXElement
        const opening = elementNode.openingElement
        const selfClosing = opening.selfClosing
        const innerOpen = t.jsxOpeningElement(opening.name, innerAttrNodes, selfClosing)
        const innerEl = selfClosing
          ? t.jsxElement(innerOpen, null, [])
          : t.jsxElement(innerOpen, elementNode.closingElement, elementNode.children)

        const wrapperOpen = t.jsxOpeningElement(
          t.jsxIdentifier(trackLocalName),
          wrapperAttrNodes,
          false,
        )
        const wrapperEl = t.jsxElement(
          wrapperOpen,
          t.jsxClosingElement(t.jsxIdentifier(trackLocalName)),
          [innerEl],
        )
        path.parentPath!.replaceWith(wrapperEl)
        s.wrapCount += 1
      },

      Program: {
        exit(path: NodePath<t.Program>, state: PluginPass) {
          const s = fileState(state)
          if (s.wrapCount === 0) return
          const trackLocalName = s.trackLocalName
          if (!trackLocalName) return
          const scope = path.scope
          const binding = scope.getBinding(trackLocalName)
          if (binding) {
            const importDecl = binding.path.findParent((p) => p.isImportDeclaration()) as
              | NodePath<t.ImportDeclaration>
              | null
            if (
              importDecl &&
              (binding.path.isImportSpecifier() || binding.path.isImportDefaultSpecifier()) &&
              importDecl.node.source.value === moduleSource
            ) {
              return // 已导入
            }
          }
          const importDecl = t.importDeclaration(
            [t.importSpecifier(t.identifier(trackLocalName), t.identifier('Track'))],
            t.stringLiteral(moduleSource),
          )
          const body = path.node.body
          let insertAt = 0
          for (let i = 0; i < body.length; i += 1) {
            if (t.isImportDeclaration(body[i])) {
              insertAt = i + 1
            } else {
              break
            }
          }
          body.splice(insertAt, 0, importDecl)
        },
      },
    },
  }
}

namespace babelPluginAquarushTrack {
  export interface TrackPluginOptions {
    /** Track 组件来源,默认 '@aquarush/common/tracking' */
    moduleSource?: string
  }
}

export = babelPluginAquarushTrack
