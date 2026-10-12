import { useState } from 'react';
import type { FC, ReactNode } from 'react';
import * as Popover from '@radix-ui/react-popover';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { Button } from './Button';

export type PopconfirmPlacement =
  | 'top'
  | 'topLeft'
  | 'topRight'
  | 'bottom'
  | 'bottomLeft'
  | 'bottomRight'
  | 'left'
  | 'leftTop'
  | 'leftBottom'
  | 'right'
  | 'rightTop'
  | 'rightBottom';

export interface PopconfirmProps {
  title: ReactNode;
  description?: ReactNode;
  onConfirm?: () => void;
  onCancel?: () => void;
  okText?: string;
  cancelText?: string;
  okType?: 'primary' | 'danger' | 'default';
  okButtonProps?: { danger?: boolean; disabled?: boolean };
  disabled?: boolean;
  placement?: PopconfirmPlacement;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  className?: string;
  children: React.ReactElement;
}

const styles = stylex.create({
  root: {
    display: 'inline-block',
  },
  content: {
    zIndex: '1050',
    width: '260px',
    paddingTop: '12px',
    paddingBottom: '12px',
    paddingLeft: '12px',
    paddingRight: '12px',
    backgroundColor: tokens.card,
    borderRadius: tokens.rSm,
    boxShadow: '0 6px 16px rgba(0, 0, 0, 0.08), 0 3px 6px -4px rgba(0, 0, 0, 0.12), 0 9px 28px 8px rgba(0, 0, 0, 0.05)',
  },
  title: {
    fontSize: '14px',
    fontWeight: '600',
    lineHeight: '22px',
    color: tokens.text,
    marginBottom: '4px',
  },
  description: {
    fontSize: '14px',
    lineHeight: '22px',
    color: tokens.textSub,
    marginBottom: '12px',
  },
  buttons: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'flex-end',
    columnGap: '8px',
    rowGap: '8px',
  },
});

/** antd placement → Radix side/align */
const placementToSide = (
  placement: PopconfirmPlacement,
): { side: 'top' | 'bottom' | 'left' | 'right'; align: 'start' | 'center' | 'end' } => {
  switch (placement) {
    case 'top':
      return { side: 'top', align: 'center' };
    case 'topLeft':
      return { side: 'top', align: 'start' };
    case 'topRight':
      return { side: 'top', align: 'end' };
    case 'bottom':
      return { side: 'bottom', align: 'center' };
    case 'bottomLeft':
      return { side: 'bottom', align: 'start' };
    case 'bottomRight':
      return { side: 'bottom', align: 'end' };
    case 'left':
      return { side: 'left', align: 'center' };
    case 'leftTop':
      return { side: 'left', align: 'start' };
    case 'leftBottom':
      return { side: 'left', align: 'end' };
    case 'right':
      return { side: 'right', align: 'center' };
    case 'rightTop':
      return { side: 'right', align: 'start' };
    case 'rightBottom':
      return { side: 'right', align: 'end' };
    default:
      return { side: 'top', align: 'center' };
  }
};

export const Popconfirm: FC<PopconfirmProps> = ({
  title,
  description,
  onConfirm,
  onCancel,
  okText = '确定',
  cancelText = '取消',
  okType = 'primary',
  okButtonProps,
  disabled = false,
  placement = 'top',
  open,
  onOpenChange,
  className,
  children,
}) => {
  const [innerOpen, setInnerOpen] = useState(false);
  const isControlled = open !== undefined;
  const visible = isControlled ? open : innerOpen;

  const changeVisible = (next: boolean) => {
    if (!isControlled) setInnerOpen(next);
    onOpenChange?.(next);
  };

  const { side, align } = placementToSide(placement);

  const handleConfirm = () => {
    changeVisible(false);
    onConfirm?.();
  };

  const handleCancel = () => {
    changeVisible(false);
    onCancel?.();
  };

  return (
    <Popover.Root open={!disabled && visible} onOpenChange={changeVisible}>
      <Popover.Trigger asChild>{children}</Popover.Trigger>
      <Popover.Portal>
        <Popover.Content
          side={side}
          align={align}
          sideOffset={8}
          {...stylex.props(styles.content)}
          className={[stylex.props(styles.content).className, className].filter(Boolean).join(' ') || undefined}
        >
          <div {...stylex.props(styles.title)}>{title}</div>
          {description != null && <div {...stylex.props(styles.description)}>{description}</div>}
          <div {...stylex.props(styles.buttons)}>
            <Button size="small" onClick={handleCancel}>
              {cancelText}
            </Button>
            <Button
              size="small"
              type={okType === 'default' ? 'default' : 'primary'}
              danger={okType === 'danger' || okButtonProps?.danger}
              disabled={okButtonProps?.disabled}
              onClick={handleConfirm}
            >
              {okText}
            </Button>
          </div>
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  );
};
