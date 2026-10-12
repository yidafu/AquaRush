/**
 * 组件库统一出口（spec admin-radix-ui [S2.2]）。
 * 业务代码只从 '@aquarush/admin-ui' 导入；图标单独从 '@aquarush/admin-ui/icons' 导入。
 * 注意:star export 中与 Input 重名的 TextAreaProps 由显式导出仲裁。
 */

// —— 展示类 ——
export * from './Button'
export * from './Card'
export * from './Tag'
export * from './Space'
export * from './Grid'
export * from './Typography'
export * from './Alert'
export * from './Empty'
export * from './Spin'
export * from './Divider'
export * from './Avatar'
export * from './List'
export * from './Statistic'
export * from './Descriptions'
export * from './Breadcrumb'
export * from './Pagination'
export * from './Image'

// —— 交互类 ——
export * from './Modal'
export * from './Popconfirm'
export * from './Tooltip'
export * from './Dropdown'
export * from './Tabs'
export * from './Collapse'
export * from './Segmented'
export * from './Switch'
export * from './Checkbox'
export * from './Radio'
export * from './Rate'
export * from './Slider'
export * from './Progress'
export * from './Upload'
export * from './Menu'

// —— 表单 ——
export * from './useForm'
export * from './Form'
export * from './Input'
export * from './Textarea'
export * from './InputNumber'
export * from './Select'
export * from './Cascader'

// —— 数据 ——
export * from './Table'

// —— 日期时间 ——
export * from './DatePicker'
export * from './TimePicker'

// —— 全局提示 ——
export { message } from './toast'
export type { MessageConfig, MessageType } from './toast'
