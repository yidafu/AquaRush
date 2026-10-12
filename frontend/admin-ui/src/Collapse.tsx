import { useState } from 'react';
import type { CSSProperties, FC, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { RightOutlined } from './icons';

export interface CollapseItem {
  key: string;
  label: ReactNode;
  children?: ReactNode;
  disabled?: boolean;
  /** 内容始终挂载(关闭时隐藏)——antd forceRender 语义,保证内部表单字段可提交 */
  forceRender?: boolean;
}

export interface CollapsePanelProps {
  header?: ReactNode;
  key?: string;
  disabled?: boolean;
  forceRender?: boolean;
  children?: ReactNode;
}

export interface CollapseProps {
  items?: CollapseItem[];
  activeKey?: string | string[];
  defaultActiveKey?: string | string[];
  onChange?: (activeKey: string[]) => void;
  /** antd 样式变体:无边框透明底 */
  ghost?: boolean;
  size?: 'large' | 'small';
  className?: string;
  style?: CSSProperties;
  children?: ReactNode;
}

const Panel: FC<CollapsePanelProps> = () => null;

const toKeyList = (value: string | string[] | undefined): string[] =>
  value == null ? [] : Array.isArray(value) ? value : [value];

const styles = stylex.create({
  root: {
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: tokens.card,
    borderRadius: tokens.rSm,
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: tokens.border,
    overflow: 'hidden',
  },
  rootGhost: {
    backgroundColor: 'transparent',
    borderWidth: '0px',
    borderRadius: '0px',
  },
  item: {
    borderBottomWidth: '1px',
    borderBottomStyle: 'solid',
    borderBottomColor: tokens.border,
  },
  itemLast: {
    borderBottomWidth: '0px',
  },
  header: {
    width: '100%',
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    columnGap: '8px',
    rowGap: '8px',
    paddingTop: '12px',
    paddingBottom: '12px',
    paddingLeft: '16px',
    paddingRight: '16px',
    borderWidth: '0px',
    borderStyle: 'none',
    backgroundColor: tokens.bg,
    color: tokens.text,
    fontSize: '14px',
    fontWeight: '600',
    lineHeight: '22px',
    textAlign: 'left',
    cursor: 'pointer',
    userSelect: 'none',
    ':hover': {
      backgroundColor: tokens.border,
    },
  },
  headerSmall: {
    paddingTop: '8px',
    paddingBottom: '8px',
    fontSize: '13px',
  },
  headerGhost: {
    backgroundColor: 'transparent',
    paddingLeft: '0px',
    paddingRight: '0px',
    ':hover': {
      backgroundColor: 'transparent',
      color: tokens.brand,
    },
  },
  headerDisabled: {
    color: tokens.textWeak,
    cursor: 'not-allowed',
    ':hover': {
      backgroundColor: tokens.bg,
    },
  },
  arrow: {
    display: 'inline-flex',
    alignItems: 'center',
    color: tokens.textSub,
    fontSize: '12px',
    // 旋转角度依赖展开状态,保留 style={{ }} 动态注入
  },
  panel: {
    paddingTop: '12px',
    paddingBottom: '12px',
    paddingLeft: '16px',
    paddingRight: '16px',
    fontSize: tokens.fsBody,
    lineHeight: tokens.lhBody,
    color: tokens.text,
    backgroundColor: tokens.card,
  },
  panelSmall: {
    paddingTop: '8px',
    paddingBottom: '8px',
    paddingLeft: '12px',
    paddingRight: '12px',
  },
  panelGhost: {
    backgroundColor: 'transparent',
    paddingLeft: '0px',
    paddingRight: '0px',
  },
});

/** children 中的 Collapse.Panel → items(key 取 React element key) */
const extractPanels = (children: ReactNode): CollapseItem[] => {
  const result: CollapseItem[] = [];
  const walk = (node: ReactNode): void => {
    if (Array.isArray(node)) {
      node.forEach(walk);
      return;
    }
    if (node == null || typeof node !== 'object' || !('key' in node)) return;
    const el = node as { key: string | null; type: unknown; props: CollapsePanelProps };
    if (el.type === Panel) {
      const rawKey = (el.key ?? '').replace(/^\.\$?/, '');
      if (!rawKey) return;
      result.push({
        key: rawKey,
        label: el.props.header,
        disabled: el.props.disabled,
        forceRender: el.props.forceRender,
        children: el.props.children,
      });
    }
  };
  walk(children);
  return result;
};

export interface CollapseComponent extends FC<CollapseProps> {
  Panel: FC<CollapsePanelProps>;
}

const CollapseRoot: FC<CollapseProps> = ({
  items,
  activeKey,
  defaultActiveKey,
  onChange,
  ghost = false,
  size = 'large',
  className,
  style,
  children,
}) => {
  const resolvedItems = items && items.length > 0 ? items : extractPanels(children);

  const [innerActive, setInnerActive] = useState<string[]>(() => toKeyList(defaultActiveKey));
  const activeKeys = activeKey !== undefined ? toKeyList(activeKey) : innerActive;

  const toggle = (key: string) => {
    if (activeKeys.includes(key)) {
      const next = activeKeys.filter((k) => k !== key);
      if (activeKey === undefined) setInnerActive(next);
      onChange?.(next);
    } else {
      const next = [...activeKeys, key];
      if (activeKey === undefined) setInnerActive(next);
      onChange?.(next);
    }
  };

  const isSmall = size === 'small';
  const rootCx = stylex.props(styles.root, ghost && styles.rootGhost);

  return (
    <div
      {...stylex.props(styles.root, ghost && styles.rootGhost)}
      className={[rootCx.className, className].filter(Boolean).join(' ') || undefined}
      style={style}
    >
      {resolvedItems.map((item, index) => {
        const expanded = activeKeys.includes(item.key) && !item.disabled;
        const forceMount = item.forceRender === true;
        const showContent = expanded || forceMount;
        return (
          <div
            key={item.key}
            {...stylex.props(
              styles.item,
              index === resolvedItems.length - 1 && styles.itemLast,
            )}
          >
            <button
              type="button"
              aria-expanded={expanded}
              disabled={item.disabled}
              onClick={() => toggle(item.key)}
              {...stylex.props(
                styles.header,
                isSmall && styles.headerSmall,
                ghost && styles.headerGhost,
                item.disabled && styles.headerDisabled,
              )}
            >
              <span
                {...stylex.props(styles.arrow)}
                // 动态:展开态箭头旋转 90°(收起指右,展开指下)
                style={{ transform: expanded ? 'rotate(90deg)' : 'rotate(0deg)' }}
              >
                <RightOutlined />
              </span>
              <span>{item.label}</span>
            </button>
            {showContent && (
              <div
                {...stylex.props(styles.panel, isSmall && styles.panelSmall, ghost && styles.panelGhost)}
                // 动态:forceRender 面板关闭时隐藏但保持挂载
                style={expanded ? undefined : { display: 'none' }}
              >
                {item.children}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};

export const Collapse: CollapseComponent = Object.assign(CollapseRoot, { Panel });
