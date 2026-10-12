import type { FC, ReactElement, ReactNode } from 'react';
import * as TooltipPrimitive from '@radix-ui/react-tooltip';
import * as stylex from '@stylexjs/stylex';

export type TooltipPlacement =
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

export interface TooltipProps {
  title?: ReactNode;
  placement?: TooltipPlacement;
  mouseEnterDelay?: number;
  open?: boolean;
  className?: string;
  children: ReactElement;
}

const styles = stylex.create({
  content: {
    zIndex: '1070',
    paddingTop: '4px',
    paddingBottom: '4px',
    paddingLeft: '8px',
    paddingRight: '8px',
    maxWidth: '250px',
    // 遮罩色沿用 antd tooltip(设计 token 无对应值,保留字面量)
    backgroundColor: 'rgba(0, 0, 0, 0.88)',
    borderRadius: '4px',
    color: '#FFFFFF',
    fontSize: '12px',
    lineHeight: '20px',
    wordBreak: 'break-word',
  },
  arrow: {
    backgroundColor: 'rgba(0, 0, 0, 0.88)',
  },
});

const placementToSide = (
  placement: TooltipPlacement,
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

export const Tooltip: FC<TooltipProps> = ({
  title,
  placement = 'top',
  mouseEnterDelay = 0.1,
  open,
  className,
  children,
}) => {
  // 空标题不渲染浮层(antd 行为:title='' 时 tooltip 不出现)
  if (title == null || title === '' || title === false) {
    return children;
  }

  const { side, align } = placementToSide(placement);

  return (
    <TooltipPrimitive.Provider delayDuration={mouseEnterDelay * 1000}>
      <TooltipPrimitive.Root open={open}>
        <TooltipPrimitive.Trigger asChild>{children}</TooltipPrimitive.Trigger>
        <TooltipPrimitive.Portal>
          <TooltipPrimitive.Content
            side={side}
            align={align}
            sideOffset={6}
            {...stylex.props(styles.content)}
            className={[stylex.props(styles.content).className, className].filter(Boolean).join(' ') || undefined}
          >
            {title}
            <TooltipPrimitive.Arrow {...stylex.props(styles.arrow)} />
          </TooltipPrimitive.Content>
        </TooltipPrimitive.Portal>
      </TooltipPrimitive.Root>
    </TooltipPrimitive.Provider>
  );
};
