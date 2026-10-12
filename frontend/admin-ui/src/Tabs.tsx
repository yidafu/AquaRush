import { useMemo, useState } from 'react';
import type { CSSProperties, FC, ReactNode } from 'react';
import * as TabsPrimitive from '@radix-ui/react-tabs';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

export interface TabItem {
  key: string;
  label: ReactNode;
  children?: ReactNode;
  disabled?: boolean;
}

export interface TabsProps {
  items?: TabItem[];
  activeKey?: string;
  defaultActiveKey?: string;
  onChange?: (activeKey: string) => void;
  /** antd 兼容:激活后销毁非当前面板内容(默认 false:访问过的面板保持挂载) */
  destroyInactiveTabPane?: boolean;
  tabBarStyle?: CSSProperties;
  /** 标签栏右侧扩展内容(antd tabBarExtraContent) */
  tabBarExtraContent?: ReactNode;
  className?: string;
  style?: CSSProperties;
  children?: ReactNode;
}

export interface TabPaneProps {
  tab?: ReactNode;
  key?: string;
  disabled?: boolean;
  children?: ReactNode;
}

const TabPane: FC<TabPaneProps> = () => null;

const styles = stylex.create({
  root: {
    display: 'flex',
    flexDirection: 'column',
  },
  list: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    columnGap: '32px',
    rowGap: '0px',
    borderBottomWidth: '1px',
    borderBottomStyle: 'solid',
    borderBottomColor: tokens.border,
    // 定位微调由 tabBarStyle 动态注入
  },
  trigger: {
    position: 'relative',
    display: 'inline-flex',
    alignItems: 'center',
    paddingTop: '12px',
    paddingBottom: '12px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderWidth: '0px',
    borderStyle: 'none',
    borderBottomWidth: '2px',
    borderBottomStyle: 'solid',
    borderBottomColor: 'transparent',
    backgroundColor: 'transparent',
    color: tokens.text,
    fontSize: '14px',
    lineHeight: '20px',
    fontWeight: '400',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    // 选中态:品牌色 + 底部指示条(antd line Tabs 基线)
    ':hover': {
      color: tokens.brand,
    },
  },
  triggerActive: {
    color: tokens.brand,
    fontWeight: '600',
    borderBottomColor: tokens.brand,
  },
  triggerDisabled: {
    color: tokens.textWeak,
    cursor: 'not-allowed',
    ':hover': {
      color: tokens.textWeak,
    },
  },
  content: {
    paddingTop: '16px',
    fontSize: tokens.fsBody,
    color: tokens.text,
  },
  extra: {
    marginLeft: 'auto',
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    columnGap: '8px',
    rowGap: '8px',
  },
});

/** JSX children → items(Tabs.TabPane API);key 来自 React element key(Children.toArray 会加 .$ 前缀) */
const extractFromChildren = (children: ReactNode): TabItem[] => {
  const result: TabItem[] = [];
  const walk = (node: ReactNode): void => {
    if (Array.isArray(node)) {
      node.forEach(walk);
      return;
    }
    if (node == null || typeof node !== 'object' || !('key' in node)) return;
    const el = node as { key: string | null; type: unknown; props: TabPaneProps };
    if (el.type === TabPane) {
      const rawKey = (el.key ?? '').replace(/^\.\$?/, '');
      if (!rawKey) return;
      result.push({
        key: rawKey,
        label: el.props.tab,
        disabled: el.props.disabled,
        children: el.props.children,
      });
    }
  };
  walk(children);
  return result;
};

export interface TabsComponent extends FC<TabsProps> {
  TabPane: FC<TabPaneProps>;
}

const TabsRoot: FC<TabsProps> = ({
  items,
  activeKey,
  defaultActiveKey,
  onChange,
  destroyInactiveTabPane = false,
  tabBarStyle,
  tabBarExtraContent,
  className,
  style,
  children,
}) => {
  const resolvedItems = useMemo(
    () => (items && items.length > 0 ? items : extractFromChildren(children)),
    [items, children],
  );

  const [innerActive, setInnerActive] = useState(
    () => defaultActiveKey ?? resolvedItems[0]?.key ?? '',
  );
  const active = activeKey ?? innerActive;

  // antd 语义:面板首次激活后保持挂载(destroyInactiveTabPane=true 时销毁)。
  // 渲染期条件派生(React 文档「adjusting state when a prop changes」模式),
  // 等价于原 effect 同步 setState 但不产生级联渲染。
  const [visited, setVisited] = useState<Set<string>>(() => new Set([defaultActiveKey ?? resolvedItems[0]?.key ?? '']));
  if (active && !visited.has(active)) {
    setVisited((prev) => {
      if (prev.has(active)) return prev;
      const next = new Set(prev);
      next.add(active);
      return next;
    });
  }

  const handleValueChange = (next: string) => {
    if (activeKey === undefined) setInnerActive(next);
    if (destroyInactiveTabPane) {
      setVisited(new Set([next]));
    }
    onChange?.(next);
  };

  const rootProps = stylex.props(styles.root);
  const listProps = stylex.props(styles.list);

  return (
    <TabsPrimitive.Root
      value={active}
      onValueChange={handleValueChange}
      {...rootProps}
      className={[rootProps.className, className].filter(Boolean).join(' ') || undefined}
      style={style}
    >
      <div {...listProps} style={tabBarStyle}>
        {resolvedItems.map((item) => (
          <TabsPrimitive.Trigger
            key={item.key}
            value={item.key}
            disabled={item.disabled}
            {...stylex.props(
              styles.trigger,
              item.key === active && styles.triggerActive,
              item.disabled && styles.triggerDisabled,
            )}
          >
            {item.label}
          </TabsPrimitive.Trigger>
        ))}
        {tabBarExtraContent != null && <div {...stylex.props(styles.extra)}>{tabBarExtraContent}</div>}
      </div>
      {resolvedItems.map((item) =>
        !destroyInactiveTabPane && !visited.has(item.key) ? null : (
          <TabsPrimitive.Content
            key={item.key}
            value={item.key}
            forceMount={destroyInactiveTabPane ? undefined : true}
            {...stylex.props(styles.content)}
            // 动态:forceMount 的非激活面板保持挂载但隐藏(antd destroyInactiveTabPane=false 语义)
            style={item.key === active ? undefined : { display: 'none' }}
          >
            {item.children}
          </TabsPrimitive.Content>
        ),
      )}
    </TabsPrimitive.Root>
  );
};

export const Tabs: TabsComponent = Object.assign(TabsRoot, { TabPane });
