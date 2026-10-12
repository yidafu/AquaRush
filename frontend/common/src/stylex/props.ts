/**
 * stylex.props 的类型放宽包装:混入全局类名字符串(weui-*、at-* 等小程序/历史类)
 *
 * @stylexjs/stylex 0.19 的 props() 运行时支持字符串类名(原样并入 className),
 * 但类型签名未放开;本包装集中做一次断言,业务代码可写:
 *   import { props } from '@aquarush/common/stylex'
 *   <View {...props('weui-cell', styles.x, cond && styles.y)} />
 */
import * as stylex from '@stylexjs/stylex'
import type { CompiledStyles } from '@stylexjs/stylex'

type Mixin = string | CompiledStyles | boolean | null | undefined

export function props(
  ...styles: ReadonlyArray<Mixin | ReadonlyArray<Mixin>>
): ReturnType<typeof stylex.props> {
  // stylex 运行时按字符串原样并入 className,这里仅放宽类型
  return (stylex.props as (...args: unknown[]) => ReturnType<typeof stylex.props>)(...styles)
}
