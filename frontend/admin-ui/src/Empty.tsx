import type { CSSProperties, FC, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

export interface EmptyProps {
  /** 自定义插图(节点或 antd 静态图地址;缺省用内置品牌蓝空态图) */
  image?: ReactNode;
  /** 描述文案,缺省「暂无数据」 */
  description?: ReactNode;
  style?: CSSProperties;
  className?: string;
  /** 操作区(按钮等),渲染在描述下方 */
  children?: ReactNode;
}

/** antd 静态图常量(locale 兼容;均走内置插图) */
const PRESENTED_IMAGE_DEFAULT = '__default__';
const PRESENTED_IMAGE_SIMPLE = '__simple__';

const styles = stylex.create({
  root: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    textAlign: 'center',
  },
  image: {
    // 设计规范:空态 = 品牌蓝 tint 圆底 + 品牌蓝图形(spec/design/ui-design-system.md §4)
    color: tokens.brand,
    display: 'block',
  },
  imageCircle: {
    fill: tokens.brandTint,
  },
  imageGlyph: {
    fill: 'currentColor',
  },
  description: {
    marginTop: '8px',
    fontSize: '14px',
    lineHeight: '20px',
    color: tokens.textSub,
  },
  action: {
    marginTop: '16px',
  },
});

/** 内置空态插图:品牌蓝 tint 圆底 + 水滴图形(自绘 SVG,颜色走 token) */
const EmptyImage: FC = () => (
  <svg
    {...stylex.props(styles.image)}
    width="96"
    height="96"
    viewBox="0 0 96 96"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <circle {...stylex.props(styles.imageCircle)} cx="48" cy="48" r="34" />
    <path
      {...stylex.props(styles.imageGlyph)}
      d="M48 26 C55 37 62 45.5 62 53 a14 14 0 0 1 -28 0 C34 45.5 41 37 48 26 Z M48 58 a5 5 0 0 0 5 -5 C53 50 48 45 48 45 S43 50 43 53 a5 5 0 0 0 5 5 Z"
    />
  </svg>
);

/** 自定义插图:静态图标记走内置插图;URL 字符串渲染为 img;节点原样渲染 */
function renderImage(image: ReactNode): ReactNode {
  if (image == null || image === false) return <EmptyImage />;
  if (image === PRESENTED_IMAGE_DEFAULT || image === PRESENTED_IMAGE_SIMPLE) return <EmptyImage />;
  if (typeof image === 'string') {
    return <img {...stylex.props(styles.image)} src={image} alt="" />;
  }
  return image;
}

/**
 * 空态。antd 同形:`<Empty description="…" />`,children 为操作区;
 * Table/Select 等直接内嵌。
 */
type EmptyComponent = FC<EmptyProps> & {
  /** antd 静态图常量名兼容(locale 可能引用;语义均为内置插图) */
  PRESENTED_IMAGE_DEFAULT: string;
  PRESENTED_IMAGE_SIMPLE: string;
};

const EmptyBase: FC<EmptyProps> = ({ image, description = '暂无数据', style, className, children }) => {
  const sp = stylex.props(styles.root);
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style;
  return (
    <div className={mergedClassName} style={mergedStyle}>
      {renderImage(image)}
      <div {...stylex.props(styles.description)}>{description}</div>
      {children != null && <div {...stylex.props(styles.action)}>{children}</div>}
    </div>
  );
};

export const Empty: EmptyComponent = Object.assign(EmptyBase, {
  PRESENTED_IMAGE_DEFAULT,
  PRESENTED_IMAGE_SIMPLE,
});

export default Empty;
