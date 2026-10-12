import { useEffect, useRef, useState } from 'react';
import type { FC, ReactNode } from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import { createRoot } from 'react-dom/client';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { Button } from './Button';

/** okButtonProps/cancelButtonProps 透传字段(antd Modal.ButtonStyle 子集) */
export interface ModalButtonProps {
  type?: 'primary' | 'default' | 'dashed' | 'text' | 'link';
  danger?: boolean;
  disabled?: boolean;
  loading?: boolean;
  style?: React.CSSProperties;
  className?: string;
}

export interface ModalProps {
  open?: boolean;
  title?: ReactNode;
  /** 底部内容;null = 无底部;缺省 = 取消+确定按钮 */
  footer?: ReactNode | null;
  width?: number | string;
  onOk?: () => void | Promise<unknown>;
  onCancel?: () => void;
  /** 确定按钮 loading(由外部控制);onOk 返回 Promise 时也会自动展示 loading */
  confirmLoading?: boolean;
  okText?: string;
  cancelText?: string;
  okButtonProps?: ModalButtonProps;
  cancelButtonProps?: ModalButtonProps;
  /** 关闭时销毁内容(Radix 关闭即卸载,该 prop 为 antd 兼容别名) */
  destroyOnClose?: boolean;
  /** antd v6 的 destroyOnHidden 改名,语义同 destroyOnClose(Radix 下恒为卸载重建) */
  destroyOnHidden?: boolean;
  /** 点击遮罩关闭,默认 true */
  maskClosable?: boolean;
  /** 垂直居中(默认距顶 100px) */
  centered?: boolean;
  /** Esc 关闭,默认 true */
  keyboard?: boolean;
  /**
   * 打开/关闭变化后的回调(antd 兼容)。
   * Radix Dialog 不暴露过渡结束事件,近似为 open 变化后的下一帧(首帧挂载不触发)。
   */
  afterOpenChange?: (open: boolean) => void;
  className?: string;
  style?: React.CSSProperties;
  children?: ReactNode;
}

const styles = stylex.create({
  overlay: {
    position: 'fixed',
    top: '0px',
    right: '0px',
    bottom: '0px',
    left: '0px',
    // 遮罩色沿用 antd(设计 token 无对应值,保留字面量)
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'flex-start',
    paddingTop: '100px',
    zIndex: '1000',
    overflowX: 'hidden',
    overflowY: 'auto',
  },
  overlayCentered: {
    paddingTop: '32px',
    justifyContent: 'center',
  },
  content: {
    position: 'relative',
    marginTop: '0px',
    marginBottom: '32px',
    display: 'flex',
    flexDirection: 'column',
    maxWidth: 'calc(100vw - 32px)',
    maxHeight: 'calc(100vh - 64px)',
    backgroundColor: tokens.card,
    borderRadius: tokens.rCard,
    boxShadow: tokens.shModal,
    // width 依赖 width prop,保留 style={{ }} 动态注入
  },
  header: {
    paddingTop: '16px',
    paddingBottom: '16px',
    paddingLeft: '24px',
    paddingRight: '48px',
    flexShrink: '0',
  },
  title: {
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
    marginRight: '0px',
    fontSize: '16px',
    fontWeight: '600',
    lineHeight: '24px',
    color: tokens.text,
    letterSpacing: 'normal',
    wordBreak: 'break-word',
  },
  close: {
    position: 'absolute',
    top: '14px',
    right: '16px',
    width: '24px',
    height: '24px',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderWidth: '0px',
    borderStyle: 'none',
    borderRadius: tokens.rSm,
    backgroundColor: 'transparent',
    color: tokens.textSub,
    cursor: 'pointer',
    opacity: '1',
    ':hover': {
      backgroundColor: tokens.border,
      color: tokens.text,
    },
  },
  body: {
    paddingTop: '0px',
    paddingBottom: '24px',
    paddingLeft: '24px',
    paddingRight: '24px',
    fontSize: tokens.fsBody,
    lineHeight: tokens.lhBody,
    color: tokens.text,
    overflowY: 'auto',
  },
  footer: {
    flexShrink: '0',
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'flex-end',
    alignItems: 'center',
    columnGap: '8px',
    rowGap: '8px',
    paddingTop: '12px',
    paddingBottom: '12px',
    paddingLeft: '24px',
    paddingRight: '24px',
    borderTopWidth: '1px',
    borderTopStyle: 'solid',
    borderTopColor: tokens.border,
  },
  // —— 静态命令式弹窗(Modal.confirm 等)——
  staticBody: {
    paddingTop: '8px',
  },
  staticTitle: {
    fontSize: '16px',
  },
});

const renderFooterButton = (
  props: ModalButtonProps | undefined,
  fallbackType: 'primary' | 'default',
  loading: boolean,
  onClick: () => void,
  text: string,
  key: string,
) => (
  <Button
    key={key}
    type={props?.type ?? fallbackType}
    danger={props?.danger}
    disabled={props?.disabled}
    loading={loading || props?.loading}
    style={props?.style}
    className={props?.className}
    onClick={onClick}
  >
    {text}
  </Button>
);

const ModalRoot: FC<ModalProps> = ({
  open = false,
  title,
  footer,
  width = 520,
  onOk,
  onCancel,
  confirmLoading = false,
  okText,
  cancelText,
  okButtonProps,
  cancelButtonProps,
  maskClosable = true,
  centered = false,
  keyboard = true,
  afterOpenChange,
  className,
  style,
  children,
}) => {
  // onOk 返回 Promise 时自动展示 loading(antd 行为)
  const [innerLoading, setInnerLoading] = useState(false);

  const handleOk = () => {
    const result = onOk?.();
    if (result && typeof result.then === 'function') {
      setInnerLoading(true);
      result.then(
        () => setInnerLoading(false),
        () => setInnerLoading(false),
      );
    }
  };

  const loading = confirmLoading || innerLoading;

  // afterOpenChange:回调经 ref 读取,避免调用方内联函数导致 effect 反复触发
  const afterOpenChangeRef = useRef(afterOpenChange);
  useEffect(() => {
    afterOpenChangeRef.current = afterOpenChange;
  });
  const mountedRef = useRef(false);
  useEffect(() => {
    if (!mountedRef.current) {
      mountedRef.current = true;
      return;
    }
    const id = requestAnimationFrame(() => afterOpenChangeRef.current?.(open));
    return () => cancelAnimationFrame(id);
    // 仅随 open 变化触发(antd afterOpenChange 语义)
  }, [open]);

  return (
    <Dialog.Root
      open={open}
      onOpenChange={(next) => {
        if (!next) onCancel?.();
      }}
    >
      <Dialog.Portal>
        <Dialog.Overlay
          {...stylex.props(styles.overlay, centered && styles.overlayCentered)}
        >
          <Dialog.Content
            {...stylex.props(styles.content)}
            className={[stylex.props(styles.content).className, className].filter(Boolean).join(' ') || undefined}
            // 动态:width 由 props 决定
            style={{ width, ...style }}
            aria-describedby={undefined}
            onEscapeKeyDown={(e) => {
              if (!keyboard) e.preventDefault();
            }}
            onInteractOutside={(e) => {
              if (!maskClosable) e.preventDefault();
            }}
          >
            <div {...stylex.props(styles.header)}>
              {title != null && title !== false && (
                <Dialog.Title {...stylex.props(styles.title)}>{title}</Dialog.Title>
              )}
              <Dialog.Close asChild>
                <button type="button" aria-label="关闭" {...stylex.props(styles.close)}>
                  <span aria-hidden="true">&#10005;</span>
                </button>
              </Dialog.Close>
            </div>
            <div {...stylex.props(styles.body)}>{children}</div>
            {footer !== null && (
              <div {...stylex.props(styles.footer)}>
                {footer ?? (
                  <>
                    {renderFooterButton(
                      cancelButtonProps,
                      'default',
                      false,
                      () => onCancel?.(),
                      cancelText ?? '取消',
                      'cancel',
                    )}
                    {renderFooterButton(
                      okButtonProps,
                      'primary',
                      loading,
                      handleOk,
                      okText ?? '确定',
                      'ok',
                    )}
                  </>
                )}
              </div>
            )}
          </Dialog.Content>
        </Dialog.Overlay>
      </Dialog.Portal>
    </Dialog.Root>
  );
};

/* ===================== 静态命令式 API(Modal.confirm / Modal.warning 等) ===================== */

export interface ModalFuncOptions {
  title?: ReactNode;
  content?: ReactNode;
  onOk?: () => void | Promise<unknown>;
  onCancel?: () => void;
  okText?: string;
  cancelText?: string;
  okType?: 'primary' | 'danger' | 'default';
  width?: number;
  maskClosable?: boolean;
}

interface StaticDialogProps {
  kind: 'confirm' | 'alert';
  options: ModalFuncOptions;
  onClose: () => void;
}

const StaticDialog: FC<StaticDialogProps> = ({ kind, options, onClose }) => {
  const [open, setOpen] = useState(true);
  const [okLoading, setOkLoading] = useState(false);

  const close = () => {
    setOpen(false);
    onClose();
  };

  const handleOk = () => {
    const result = options.onOk?.();
    if (result && typeof result.then === 'function') {
      setOkLoading(true);
      result.then(
        () => close(),
        () => close(),
      );
    } else {
      close();
    }
  };

  const handleCancel = () => {
    options.onCancel?.();
    close();
  };

  return (
    <Dialog.Root
      open={open}
      onOpenChange={(next) => {
        if (!next) close();
      }}
    >
      <Dialog.Portal>
        <Dialog.Overlay {...stylex.props(styles.overlay, styles.overlayCentered)}>
          <Dialog.Content
            {...stylex.props(styles.content, styles.staticBody)}
            // 动态:静态弹窗宽度(antd confirm 默认 416)
            style={{ width: options.width ?? 416 }}
            aria-describedby={undefined}
            onInteractOutside={(e) => {
              if (options.maskClosable === false) e.preventDefault();
            }}
          >
            <div {...stylex.props(styles.header)}>
              <Dialog.Title {...stylex.props(styles.title, styles.staticTitle)}>
                {options.title ?? '确认'}
              </Dialog.Title>
            </div>
            <div {...stylex.props(styles.body)}>
              {options.content}
            </div>
            <div {...stylex.props(styles.footer)}>
              {kind === 'confirm' &&
                renderFooterButton(
                  undefined,
                  'default',
                  false,
                  handleCancel,
                  options.cancelText ?? '取消',
                  'cancel',
                )}
              {renderFooterButton(
                {
                  type: options.okType === 'default' ? 'default' : 'primary',
                  danger: options.okType === 'danger',
                },
                'primary',
                okLoading,
                handleOk,
                options.okText ?? (kind === 'confirm' ? '确定' : '知道了'),
                'ok',
              )}
            </div>
          </Dialog.Content>
        </Dialog.Overlay>
      </Dialog.Portal>
    </Dialog.Root>
  );
};

const openStatic = (kind: 'confirm' | 'alert', options: ModalFuncOptions): { destroy: () => void } => {
  const host = document.createElement('div');
  document.body.appendChild(host);
  const root = createRoot(host);
  const destroy = () => {
    root.unmount();
    host.remove();
  };
  root.render(<StaticDialog kind={kind} options={options} onClose={destroy} />);
  return { destroy };
};

export interface ModalComponent extends FC<ModalProps> {
  confirm: (options: ModalFuncOptions) => { destroy: () => void };
  info: (options: ModalFuncOptions) => { destroy: () => void };
  success: (options: ModalFuncOptions) => { destroy: () => void };
  error: (options: ModalFuncOptions) => { destroy: () => void };
  warning: (options: ModalFuncOptions) => { destroy: () => void };
}

export const Modal: ModalComponent = Object.assign(ModalRoot, {
  confirm: (options: ModalFuncOptions) => openStatic('confirm', options),
  info: (options: ModalFuncOptions) => openStatic('alert', options),
  success: (options: ModalFuncOptions) => openStatic('alert', options),
  error: (options: ModalFuncOptions) => openStatic('alert', options),
  warning: (options: ModalFuncOptions) => openStatic('alert', options),
});
