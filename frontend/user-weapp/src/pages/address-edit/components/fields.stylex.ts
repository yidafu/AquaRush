/**
 * 地址编辑页表单字段共用样式(原 fields.scss,被 FloorField /
 * DetailAddressField / ContactFields 三处引用)
 *
 * 长度值保持 scss 来源 750 基准 rpx;weui-* 类在 JSX 侧混入,不在此定义。
 */
import * as stylex from '@stylexjs/stylex'

// 校验错误红字抖动(原 fields.scss @keyframes field-shake)
const fieldShake = stylex.keyframes({
  '0%': { transform: 'translateX(0)' },
  '25%': { transform: 'translateX(-4rpx)' },
  '75%': { transform: 'translateX(4rpx)' },
  '100%': { transform: 'translateX(0)' },
})

export const fieldStyles = stylex.create({
  // 内联文本域(去掉 weui-textarea 自带内边距,贴 cell 排版)
  detailTextarea: {
    minHeight: '128rpx',
    paddingTop: '16rpx',
    paddingRight: '0',
    paddingBottom: '0',
    paddingLeft: '0',
    backgroundColor: 'transparent',
  },
  // 单元格下方辅助文案
  fieldHint: {
    paddingTop: '12rpx',
    fontSize: '24rpx',
    lineHeight: 1.5,
    color: 'var(--weui-FG-2)',
  },
  // 表单 tips 行微调(楼层说明等)
  fieldTip: {
    paddingBottom: '8rpx',
  },
  // 校验错误红字(单元格下方红字)
  fieldError: {
    paddingTop: '12rpx',
    paddingRight: '32rpx',
    paddingBottom: '0',
    paddingLeft: '32rpx',
    fontSize: '24rpx',
    lineHeight: 1.5,
    color: 'var(--weui-RED)',
    animationName: fieldShake,
    animationDuration: '0.3s',
    animationTimingFunction: 'ease-in-out',
  },
})
