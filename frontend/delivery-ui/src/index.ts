/**
 * primitive 包装层：统一从 @rn-primitives 引入 headless 组件。
 * 样式层只从 `@aquarush/delivery-ui` 引用 primitive，不直接依赖 @rn-primitives/*。
 * 本层无品牌样式——颜色与排版由 delivery-app 样式层按 docs/ui-spec.md 注入。
 */
import * as DialogPrimitive from '@rn-primitives/dialog';
import * as SwitchPrimitive from '@rn-primitives/switch';
import * as ToggleGroupPrimitive from '@rn-primitives/toggle-group';

export { PortalHost } from '@rn-primitives/portal';
export { DialogPrimitive as Dialog };
export { SwitchPrimitive };
export { ToggleGroupPrimitive as ToggleGroup };
export { DialogFrame, type DialogFrameProps } from './dialog-frame';
