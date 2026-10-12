import type { CSSProperties, FC, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

export type SpinSize = 'small' | 'default' | 'large';

export interface SpinProps {
  /** 有 children 时控制加载遮罩;缺省 true(antd 同义) */
  spinning?: boolean;
  /** 遮罩/独立指示器下方文案 */
  tip?: ReactNode;
  size?: SpinSize;
  style?: CSSProperties;
  className?: string;
  children?: ReactNode;
}

const spinKeyframes = stylex.keyframes({
  from: { transform: 'rotate(0deg)' },
  to: { transform: 'rotate(360deg)' },
});

const styles = stylex.create({
  // —— 指示器(圆弧)——
  spinner: {
    display: 'inline-block',
    boxSizing: 'border-box',
    flexShrink: '0',
    borderStyle: 'solid',
    borderTopColor: tokens.brand,
    borderRightColor: 'transparent',
    borderBottomColor: 'transparent',
    borderLeftColor: 'transparent',
    borderRadius: '50%',
    animationName: spinKeyframes,
    animationDuration: '0.8s',
    animationTimingFunction: 'linear',
    animationIterationCount: 'infinite',
  },
  spinnerSmall: {
    width: '14px',
    height: '14px',
    borderWidth: '2px',
  },
  spinnerDefault: {
    width: '24px',
    height: '24px',
    borderWidth: '3px',
  },
  spinnerLarge: {
    width: '40px',
    height: '40px',
    borderWidth: '4px',
  },
  tip: {
    fontSize: '14px',
    lineHeight: '20px',
    color: tokens.textSub,
    textAlign: 'center',
  },
  // —— 独立(无 children)——
  standalone: {
    display: 'inline-flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    rowGap: '8px',
    columnGap: '0px',
  },
  // —— 包裹 children(antd <Spin spinning>children</Spin> 形态)——
  wrapper: {
    position: 'relative',
    display: 'block',
    width: '100%',
  },
  content: {
    minHeight: '0px',
  },
  blurred: {
    // 加载中内容压暗(antd .ant-spin-blur 同义)
    opacity: '0.5',
  },
  overlay: {
    position: 'absolute',
    top: '0px',
    right: '0px',
    bottom: '0px',
    left: '0px',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    rowGap: '8px',
    columnGap: '0px',
    zIndex: '10',
  },
});

function spinnerSizeClass(size: SpinSize) {
  switch (size) {
    case 'small':
      return styles.spinnerSmall;
    case 'large':
      return styles.spinnerLarge;
    default:
      return styles.spinnerDefault;
  }
}

/**
 * 加载指示器。
 * - 无 children:`<Spin size="small" />` 独立指示器;
 * - 有 children:`<Spin spinning={loading}>…</Spin>` 内容压暗 + 居中遮罩(antd 同形)。
 */
export const Spin: FC<SpinProps> = ({ spinning = true, tip, size = 'default', style, className, children }) => {
  const spinner = (
    <span {...stylex.props(styles.spinner, spinnerSizeClass(size))} aria-hidden="true" />
  );
  const tipNode = tip != null && tip !== false ? <span {...stylex.props(styles.tip)}>{tip}</span> : null;

  if (children == null) {
    const sp = stylex.props(styles.standalone);
    const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
    const mergedStyle = sp.style ? { ...sp.style, ...style } : style;
    return (
      <span className={mergedClassName} style={mergedStyle} role="status">
        {spinner}
        {tipNode}
      </span>
    );
  }

  const sp = stylex.props(styles.wrapper);
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style;
  return (
    <div className={mergedClassName} style={mergedStyle}>
      <div {...stylex.props(styles.content, spinning && styles.blurred)}>{children}</div>
      {spinning && (
        <div {...stylex.props(styles.overlay)} role="status">
          {spinner}
          {tipNode}
        </div>
      )}
    </div>
  );
};

export default Spin;
