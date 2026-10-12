import { useEffect, useRef, useState } from 'react';
import type { FC, ReactNode } from 'react';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

export type DropdownPlacement =
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

export interface DropdownMenuItem {
  key: string;
  label?: ReactNode;
  icon?: ReactNode;
  onClick?: (info: { key: string }) => void;
  disabled?: boolean;
  danger?: boolean;
  /** antd Menu item type(divider 分隔线 / group 分组标题) */
  type?: 'divider' | 'group';
  children?: DropdownMenuItem[];
}

export interface DropdownProps {
  menu?: { items?: DropdownMenuItem[] };
  /** antd 默认 ['hover'] */
  trigger?: 'hover' | 'click' | Array<'hover' | 'click'>;
  placement?: DropdownPlacement;
  disabled?: boolean;
  className?: string;
  children: React.ReactElement;
}

const styles = stylex.create({
  triggerWrap: {
    display: 'inline-block',
  },
  content: {
    zIndex: '1050',
    minWidth: '120px',
    paddingTop: '4px',
    paddingBottom: '4px',
    paddingLeft: '4px',
    paddingRight: '4px',
    backgroundColor: tokens.card,
    borderRadius: tokens.rSm,
    boxShadow: '0 6px 16px rgba(0, 0, 0, 0.08), 0 3px 6px -4px rgba(0, 0, 0, 0.12), 0 9px 28px 8px rgba(0, 0, 0, 0.05)',
  },
  item: {
    position: 'relative',
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    columnGap: '8px',
    rowGap: '8px',
    paddingTop: '5px',
    paddingBottom: '5px',
    paddingLeft: '12px',
    paddingRight: '12px',
    fontSize: '14px',
    lineHeight: '20px',
    color: tokens.text,
    borderRadius: '6px',
    cursor: 'pointer',
    userSelect: 'none',
    backgroundColor: 'transparent',
    borderStyle: 'none',
    borderWidth: '0px',
    textAlign: 'left',
    width: '100%',
    ':hover': {
      backgroundColor: tokens.bg,
    },
  },
  itemDanger: {
    color: tokens.error,
  },
  itemDisabled: {
    color: tokens.textWeak,
    cursor: 'not-allowed',
    ':hover': {
      backgroundColor: 'transparent',
    },
  },
  divider: {
    height: '1px',
    marginTop: '4px',
    marginBottom: '4px',
    marginLeft: '0px',
    marginRight: '0px',
    backgroundColor: tokens.border,
  },
  groupLabel: {
    paddingTop: '6px',
    paddingBottom: '4px',
    paddingLeft: '12px',
    paddingRight: '12px',
    fontSize: '12px',
    lineHeight: '16px',
    color: tokens.textSub,
    fontWeight: '600',
  },
});

const placementToSide = (
  placement: DropdownPlacement,
): { side: 'top' | 'bottom' | 'left' | 'right'; align: 'start' | 'center' | 'end' } => {
  switch (placement) {
    case 'top':
      return { side: 'top', align: 'center' };
    case 'topLeft':
      return { side: 'top', align: 'start' };
    case 'topRight':
      return { side: 'top', align: 'end' };
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
    case 'bottomLeft':
      return { side: 'bottom', align: 'start' };
    case 'bottomRight':
      return { side: 'bottom', align: 'end' };
    default:
      return { side: 'bottom', align: 'center' };
  }
};

const renderItems = (items: DropdownMenuItem[], onSelect: (key: string) => void): ReactNode[] =>
  items.map((item, index) => {
    if (item.type === 'divider') {
      return <DropdownMenu.Separator key={item.key ?? `divider-${index}`} {...stylex.props(styles.divider)} />;
    }
    if (item.type === 'group') {
      return (
        <DropdownMenu.Group key={item.key}>
          <DropdownMenu.Label {...stylex.props(styles.groupLabel)}>{item.label}</DropdownMenu.Label>
          {item.children && renderItems(item.children, onSelect)}
        </DropdownMenu.Group>
      );
    }
    return (
      <DropdownMenu.Item
        key={item.key}
        disabled={item.disabled}
        onSelect={() => {
          onSelect(item.key);
          item.onClick?.({ key: item.key });
        }}
        {...stylex.props(styles.item, item.danger && styles.itemDanger, item.disabled && styles.itemDisabled)}
      >
        {item.icon}
        <span>{item.label}</span>
      </DropdownMenu.Item>
    );
  });

export const Dropdown: FC<DropdownProps> = ({
  menu,
  trigger = ['hover'],
  placement = 'bottomLeft',
  disabled = false,
  className,
  children,
}) => {
  const [open, setOpen] = useState(false);
  const supportsHover = Array.isArray(trigger) ? trigger.includes('hover') : trigger === 'hover';
  const supportsClick = Array.isArray(trigger) ? trigger.includes('click') : trigger === 'click';
  const closeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const hoveringRef = useRef(false);

  useEffect(
    () => () => {
      if (closeTimer.current) clearTimeout(closeTimer.current);
    },
    [],
  );

  const openMenu = () => {
    if (disabled) return;
    if (closeTimer.current) {
      clearTimeout(closeTimer.current);
      closeTimer.current = null;
    }
    setOpen(true);
  };

  const scheduleClose = () => {
    if (closeTimer.current) clearTimeout(closeTimer.current);
    // 指针离开触发区后延迟关闭,便于移入浮层(Radix 菜单为 portal,需宽限)
    closeTimer.current = setTimeout(() => setOpen(false), 120);
  };

  const handleOpenChange = (next: boolean) => {
    // hover-only:忽略 Radix 触发器点击产生的关闭,统一由悬停逻辑控制
    if (supportsHover && !supportsClick && !next && hoveringRef.current) return;
    setOpen(next);
  };

  const { side, align } = placementToSide(placement);
  const items = menu?.items ?? [];

  return (
    <DropdownMenu.Root open={open} onOpenChange={handleOpenChange}>
      <DropdownMenu.Trigger asChild>
        {supportsHover ? (
          <span
            {...stylex.props(styles.triggerWrap)}
            className={className}
            onMouseEnter={() => {
              hoveringRef.current = true;
              openMenu();
            }}
            onMouseLeave={() => {
              hoveringRef.current = false;
              scheduleClose();
            }}
          >
            {children}
          </span>
        ) : (
          children
        )}
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          side={side}
          align={align}
          sideOffset={6}
          {...stylex.props(styles.content)}
          onMouseEnter={openMenu}
          onMouseLeave={scheduleClose}
        >
          {renderItems(items, () => setOpen(false))}
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
};
