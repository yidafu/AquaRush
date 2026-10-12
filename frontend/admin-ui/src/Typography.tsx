import { useEffect, useRef, useState } from 'react';
import type { CSSProperties, FC, HTMLAttributes } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { CheckOutlined, CopyOutlined } from './icons';

export type TextType = 'secondary' | 'success' | 'warning' | 'danger';

const styles = stylex.create({
  // —— 文字色 ——
  secondary: {
    color: tokens.textSub,
  },
  danger: {
    color: tokens.error,
  },
  warning: {
    color: tokens.warning,
  },
  success: {
    color: tokens.success,
  },
  strong: {
    fontWeight: '600',
  },
  // antd code:85% 字号 + 0.2em/0.4em 内边距 + 灰底(设计 token 无对应,字面量保留)
  code: {
    fontFamily: tokens.fontNum,
    fontSize: '0.85em',
    paddingTop: '0.2em',
    paddingBottom: '0.2em',
    paddingLeft: '0.4em',
    paddingRight: '0.4em',
    backgroundColor: 'rgba(0, 0, 0, 0.06)',
    borderRadius: '3px',
  },
  deleted: {
    textDecoration: 'line-through',
  },
  underline: {
    textDecoration: 'underline',
  },
  deletedUnderline: {
    textDecoration: 'line-through underline',
  },
  mark: {
    backgroundColor: tokens.statusWarningBg,
    color: 'inherit',
  },
  // —— copyable ——
  copyBtn: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    verticalAlign: 'middle',
    marginLeft: '4px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderStyle: 'none',
    borderWidth: '0px',
    backgroundColor: 'transparent',
    color: tokens.textWeak,
    cursor: 'pointer',
    fontSize: '12px',
    ':hover': {
      color: tokens.brand,
    },
  },
  copied: {
    display: 'inline-block',
    marginLeft: '4px',
    fontSize: '12px',
    lineHeight: '16px',
    color: tokens.success,
    verticalAlign: 'middle',
  },
  // —— Title ——
  title: {
    marginTop: '0px',
    marginLeft: '0px',
    marginRight: '0px',
    marginBottom: '0.5em',
    fontWeight: '600',
    color: tokens.text,
    letterSpacing: 'normal',
  },
  h1: {
    fontSize: '38px',
    lineHeight: '46px',
  },
  h2: {
    fontSize: '30px',
    lineHeight: '38px',
  },
  h3: {
    fontSize: '24px',
    lineHeight: '32px',
  },
  h4: {
    fontSize: '20px',
    lineHeight: '28px',
  },
  h5: {
    fontSize: '16px',
    lineHeight: '24px',
  },
  // —— Paragraph ——
  paragraph: {
    marginTop: '0px',
    marginBottom: '1em',
    wordBreak: 'break-word',
  },
  // ellipsis 单行(display/overflow 静态,行数经 style={} 注入)
  ellipsisBox: {
    display: '-webkit-box',
    WebkitBoxOrient: 'vertical',
    overflow: 'hidden',
  },
  ellipsisSingle: {
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
});

function typeClass(type?: TextType) {
  switch (type) {
    case 'secondary':
      return styles.secondary;
    case 'danger':
      return styles.danger;
    case 'warning':
      return styles.warning;
    case 'success':
      return styles.success;
    default:
      return undefined;
  }
}

function textDecorationClass(deleted: boolean, underline: boolean) {
  if (deleted && underline) return styles.deletedUnderline;
  if (deleted) return styles.deleted;
  if (underline) return styles.underline;
  return undefined;
}

/** 复制按钮 + 瞬时「已复制」提示(不依赖 message,本地状态实现) */
const CopyableControl: FC<{ getText: () => string }> = ({ getText }) => {
  const [copied, setCopied] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(
    () => () => {
      if (timerRef.current != null) clearTimeout(timerRef.current);
    },
    [],
  );

  const handleClick = () => {
    const text = getText();
    void navigator.clipboard?.writeText(text).then(
      () => {
        setCopied(true);
        if (timerRef.current != null) clearTimeout(timerRef.current);
        timerRef.current = setTimeout(() => setCopied(false), 2000);
      },
      () => {
        /* 剪贴板不可用时静默失败(antd 亦仅提示) */
      },
    );
  };

  return (
    <>
      <button
        type="button"
        aria-label={copied ? '已复制' : '复制'}
        {...stylex.props(styles.copyBtn)}
        onClick={handleClick}
      >
        {copied ? <CheckOutlined /> : <CopyOutlined />}
      </button>
      {copied && <span {...stylex.props(styles.copied)}>已复制</span>}
    </>
  );
};

export interface TextProps extends HTMLAttributes<HTMLSpanElement> {
  type?: TextType;
  strong?: boolean;
  /** 行内代码样式 */
  code?: boolean;
  copyable?: boolean;
  delete?: boolean;
  underline?: boolean;
  mark?: boolean;
}

export const Text: FC<TextProps> = ({
  type,
  strong = false,
  code = false,
  copyable = false,
  delete: deleted = false,
  underline = false,
  mark = false,
  className,
  style,
  children,
  ...rest
}) => {
  const contentRef = useRef<HTMLSpanElement>(null);
  const sp = stylex.props(
    typeClass(type),
    strong && styles.strong,
    deleted || underline ? textDecorationClass(deleted, underline) : undefined,
    mark && styles.mark,
  );
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style;
  const content = (
    <span ref={contentRef}>
      {code ? <code {...stylex.props(styles.code)}>{children}</code> : children}
    </span>
  );
  return (
    <span className={mergedClassName} style={mergedStyle} {...rest}>
      {content}
      {copyable && (
        <CopyableControl getText={() => contentRef.current?.textContent ?? ''} />
      )}
    </span>
  );
};

export interface ParagraphProps extends HTMLAttributes<HTMLParagraphElement> {
  type?: TextType;
  strong?: boolean;
  code?: boolean;
  copyable?: boolean;
  /** 行数截断:ellipsis 或 { rows } */
  ellipsis?: boolean | { rows?: number };
  delete?: boolean;
  underline?: boolean;
  mark?: boolean;
}

export const Paragraph: FC<ParagraphProps> = ({
  type,
  strong = false,
  code = false,
  copyable = false,
  ellipsis,
  delete: deleted = false,
  underline = false,
  mark = false,
  className,
  style,
  children,
  ...rest
}) => {
  const contentRef = useRef<HTMLSpanElement>(null);
  const rows = typeof ellipsis === 'object' ? ellipsis.rows ?? 1 : 1;
  const ellipsisEnabled = ellipsis === true || typeof ellipsis === 'object';
  const sp = stylex.props(
    styles.paragraph,
    typeClass(type),
    strong && styles.strong,
    deleted || underline ? textDecorationClass(deleted, underline) : undefined,
    mark && styles.mark,
    ellipsisEnabled && (rows > 1 ? styles.ellipsisBox : styles.ellipsisSingle),
  );
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  // 动态:ellipsis 行数由 props 决定,保留 style={}
  const ellipsisStyle: CSSProperties | undefined =
    ellipsisEnabled && rows > 1 ? { WebkitLineClamp: rows } : undefined;
  const mergedStyle = { ...sp.style, ...ellipsisStyle, ...style };
  return (
    <p className={mergedClassName} style={mergedStyle} {...rest}>
      <span ref={contentRef}>
        {code ? <code {...stylex.props(styles.code)}>{children}</code> : children}
      </span>
      {copyable && <CopyableControl getText={() => contentRef.current?.textContent ?? ''} />}
    </p>
  );
};

export interface TitleProps extends HTMLAttributes<HTMLHeadingElement> {
  level?: 1 | 2 | 3 | 4 | 5;
  type?: TextType;
}

export const Title: FC<TitleProps> = ({ level = 1, type, className, style, children, ...rest }) => {
  const Heading = `h${level}` as 'h1' | 'h2' | 'h3' | 'h4' | 'h5';
  const sp = stylex.props(
    styles.title,
    level === 1 ? styles.h1 : level === 2 ? styles.h2 : level === 3 ? styles.h3 : level === 4 ? styles.h4 : styles.h5,
    typeClass(type),
  );
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style;
  return (
    <Heading className={mergedClassName} style={mergedStyle} {...rest}>
      {children}
    </Heading>
  );
};

/** antd Typography 命名空间兼容:`Typography.Text` / `Typography.Title` / `Typography.Paragraph` */
export const Typography = { Text, Title, Paragraph } as {
  Text: typeof Text;
  Title: typeof Title;
  Paragraph: typeof Paragraph;
};
