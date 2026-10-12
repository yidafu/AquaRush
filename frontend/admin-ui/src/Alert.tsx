import { useState } from 'react';
import type { CSSProperties, FC, MouseEvent, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { CheckCircleOutlined, CloseCircleOutlined, CloseOutlined, MessageOutlined, WarningOutlined } from './icons';

export type AlertType = 'success' | 'info' | 'warning' | 'error';

export interface AlertProps {
  type?: AlertType;
  message: ReactNode;
  description?: ReactNode;
  /** 展示类型图标,缺省 false(antd 同义);type 图标: success→√ / warning→! / error→× / info→ℹ */
  showIcon?: boolean;
  /** 自定义图标(优先于类型图标) */
  icon?: ReactNode;
  closable?: boolean;
  onClose?: (e: MouseEvent<HTMLElement>) => void;
  /** 右侧操作区(antd 同名同义,如重试按钮) */
  action?: ReactNode;
  style?: CSSProperties;
  className?: string;
}

const styles = stylex.create({
  base: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'flex-start',
    boxSizing: 'border-box',
    // 设计 token 无告警圆角专用值,用小元素圆角 8(rSm)
    borderRadius: tokens.rSm,
    fontSize: '14px',
    lineHeight: '22px',
    color: tokens.text,
    borderStyle: 'solid',
    borderWidth: '1px',
  },
  // —— 类型底色(状态 10% tint,与 STATUS_TONE_COLORS 对齐)——
  success: {
    backgroundColor: tokens.statusSuccessBg,
    borderColor: tokens.statusSuccessBg,
  },
  info: {
    backgroundColor: tokens.statusInfoBg,
    borderColor: tokens.statusInfoBg,
  },
  warning: {
    backgroundColor: tokens.statusWarningBg,
    borderColor: tokens.statusWarningBg,
  },
  error: {
    backgroundColor: tokens.statusErrorBg,
    borderColor: tokens.statusErrorBg,
  },
  // —— 内边距:无描述 8×12,有描述 16×16 ——
  paddingSimple: {
    paddingTop: '8px',
    paddingBottom: '8px',
    paddingLeft: '12px',
    paddingRight: '12px',
  },
  paddingDescription: {
    paddingTop: '16px',
    paddingBottom: '16px',
    paddingLeft: '16px',
    paddingRight: '16px',
  },
  // —— 类型图标 ——
  icon: {
    display: 'inline-flex',
    alignItems: 'center',
    flexShrink: '0',
    fontSize: '16px',
    lineHeight: '22px',
    marginRight: '8px',
    marginTop: '3px',
    color: 'inherit',
  },
  iconSuccess: {
    color: tokens.success,
  },
  iconInfo: {
    color: tokens.brand,
  },
  iconWarning: {
    color: tokens.warning,
  },
  iconError: {
    color: tokens.error,
  },
  content: {
    flexGrow: '1',
    flexShrink: '1',
    flexBasis: 'auto',
    minWidth: '0px',
  },
  message: {
    fontSize: '14px',
    lineHeight: '22px',
    color: tokens.text,
    wordBreak: 'break-word',
  },
  action: {
    marginLeft: 'auto',
    flexShrink: '0',
    alignSelf: 'center',
    whiteSpace: 'nowrap',
  },
  description: {
    marginTop: '4px',
    fontSize: '14px',
    lineHeight: '22px',
    color: tokens.textSub,
    wordBreak: 'break-word',
  },
  close: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: '0',
    alignSelf: 'flex-start',
    marginLeft: '8px',
    marginTop: '3px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '4px',
    paddingRight: '4px',
    borderStyle: 'none',
    borderWidth: '0px',
    backgroundColor: 'transparent',
    color: tokens.textSub,
    fontSize: '14px',
    lineHeight: '22px',
    cursor: 'pointer',
    ':hover': {
      color: tokens.text,
    },
  },
});

function baseTypeClass(type: AlertType) {
  switch (type) {
    case 'success':
      return styles.success;
    case 'warning':
      return styles.warning;
    case 'error':
      return styles.error;
    default:
      return styles.info;
  }
}

function iconTypeClass(type: AlertType) {
  switch (type) {
    case 'success':
      return styles.iconSuccess;
    case 'warning':
      return styles.iconWarning;
    case 'error':
      return styles.iconError;
    default:
      return styles.iconInfo;
  }
}

function defaultIcon(type: AlertType) {
  switch (type) {
    case 'success':
      return <CheckCircleOutlined />;
    case 'warning':
      return <WarningOutlined />;
    case 'error':
      return <CloseCircleOutlined />;
    default:
      // icons 集合暂缺 InfoCircleOutlined(见交付报告 gap),info 用气泡图标近似
      return <MessageOutlined />;
  }
}

export const Alert: FC<AlertProps> = ({
  type = 'info',
  message,
  description,
  showIcon = false,
  icon,
  closable = false,
  onClose,
  action,
  style,
  className,
}) => {
  const [dismissed, setDismissed] = useState(false);
  if (dismissed) return null;

  const hasDescription = description != null && description !== false;
  const iconNode = showIcon
    ? (icon ?? defaultIcon(type))
    : (icon ?? null);

  const handleClose = (e: MouseEvent<HTMLElement>) => {
    onClose?.(e);
    if (!e.defaultPrevented) setDismissed(true);
  };

  const sp = stylex.props(
    styles.base,
    baseTypeClass(type),
    hasDescription ? styles.paddingDescription : styles.paddingSimple,
  );
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style;

  return (
    <div className={mergedClassName} style={mergedStyle} role="alert">
      {iconNode != null && (
        <span {...stylex.props(styles.icon, icon == null && iconTypeClass(type))}>{iconNode}</span>
      )}
      <div {...stylex.props(styles.content)}>
        <div {...stylex.props(styles.message)}>{message}</div>
        {hasDescription && <div {...stylex.props(styles.description)}>{description}</div>}
      </div>
      {action != null && <div {...stylex.props(styles.action)}>{action}</div>}
      {closable && (
        <button type="button" aria-label="关闭" {...stylex.props(styles.close)} onClick={handleClose}>
          <CloseOutlined />
        </button>
      )}
    </div>
  );
};

export default Alert;
