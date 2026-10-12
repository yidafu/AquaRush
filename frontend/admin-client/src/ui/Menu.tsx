import { useState } from 'react';
import type { CSSProperties, FC, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { DownOutlined } from './icons';

export interface MenuItem {
  key?: string;
  label?: ReactNode;
  icon?: ReactNode;
  children?: MenuItem[];
  /** antd Menu item type:group 分组标题 / divider 分隔线 */
  type?: 'group' | 'divider';
  disabled?: boolean;
}

export interface MenuInfo {
  key: string;
  keyPath: string[];
}

export interface MenuProps {
  items?: MenuItem[];
  mode?: 'inline';
  selectedKeys?: string[];
  defaultSelectedKeys?: string[];
  openKeys?: string[];
  defaultOpenKeys?: string[];
  onSelect?: (info: MenuInfo) => void;
  /** antd Menu onClick(与 onSelect 同形,调用点二者皆用) */
  onClick?: (info: MenuInfo) => void;
  onOpenChange?: (keys: string[]) => void;
  inlineCollapsed?: boolean;
  /** 侧边栏深色主题(antd Menu theme="dark") */
  theme?: 'light' | 'dark';
  className?: string;
  style?: CSSProperties;
}

const styles = stylex.create({
  nav: {
    display: 'block',
    paddingTop: '8px',
    paddingBottom: '8px',
    // 背景由外层 Sider 提供
  },
  list: {
    listStyleType: 'none',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
    marginRight: '0px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    display: 'flex',
    flexDirection: 'column',
  },
  subList: {
    listStyleType: 'none',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
    marginRight: '0px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    display: 'flex',
    flexDirection: 'column',
  },
  item: {
    display: 'block',
  },
  row: {
    width: '100%',
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    columnGap: '8px',
    rowGap: '8px',
    height: '40px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '16px',
    paddingRight: '16px',
    borderWidth: '0px',
    borderStyle: 'none',
    borderRadius: '6px',
    backgroundColor: 'transparent',
    fontSize: '14px',
    lineHeight: '20px',
    textAlign: 'left',
    cursor: 'pointer',
    // 文字/悬停底色依赖 theme,保留 style={{ }} 动态注入
  },
  rowCollapsed: {
    justifyContent: 'center',
    paddingLeft: '0px',
    paddingRight: '0px',
  },
  rowDisabled: {
    opacity: '0.4',
    cursor: 'not-allowed',
  },
  icon: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: '0',
    fontSize: '16px',
  },
  label: {
    flexGrow: '1',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  labelHidden: {
    display: 'none',
  },
  arrow: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: '0',
    fontSize: '10px',
    // 旋转角依赖展开状态,保留 style={{ }} 动态注入
  },
  groupLabel: {
    paddingTop: '12px',
    paddingBottom: '4px',
    paddingLeft: '16px',
    paddingRight: '16px',
    fontSize: '12px',
    lineHeight: '16px',
    fontWeight: '600',
    // 颜色依赖 theme,保留 style={{ }} 动态注入
  },
  divider: {
    height: '1px',
    marginTop: '4px',
    marginBottom: '4px',
    marginLeft: '0px',
    marginRight: '0px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    // 颜色依赖 theme,保留 style={{ }} 动态注入
    listStyleType: 'none',
  },
});

const themeHoverBg = (theme: 'light' | 'dark'): string =>
  theme === 'dark' ? 'rgba(255, 255, 255, 0.08)' : tokens.bg;

const themeGroupColor = (theme: 'light' | 'dark'): string =>
  theme === 'dark' ? 'rgba(255, 255, 255, 0.35)' : tokens.textSub;

const themeDivider = (theme: 'light' | 'dark'): string =>
  theme === 'dark' ? 'rgba(255, 255, 255, 0.12)' : tokens.border;

export const Menu: FC<MenuProps> = ({
  items,
  mode = 'inline',
  selectedKeys,
  defaultSelectedKeys,
  openKeys,
  defaultOpenKeys,
  onSelect,
  onClick,
  onOpenChange,
  inlineCollapsed = false,
  theme = 'light',
  className,
  style,
}) => {
  const [innerSelected, setInnerSelected] = useState<string[]>(() => defaultSelectedKeys ?? []);
  const [innerOpen, setInnerOpen] = useState<string[]>(() => defaultOpenKeys ?? []);
  const currentSelected = selectedKeys ?? innerSelected;
  const currentOpen = openKeys ?? innerOpen;

  const setOpen = (next: string[]) => {
    if (openKeys === undefined) setInnerOpen(next);
    onOpenChange?.(next);
  };

  const handleSelect = (key: string, keyPath: string[]) => {
    if (selectedKeys === undefined) setInnerSelected([key]);
    const info: MenuInfo = { key, keyPath };
    onSelect?.(info);
    onClick?.(info);
  };

  const toggleOpen = (key: string) => {
    const next = currentOpen.includes(key)
      ? currentOpen.filter((k) => k !== key)
      : [...currentOpen, key];
    setOpen(next);
  };

  const isDark = theme === 'dark';
  const baseRowColor = isDark ? 'rgba(255, 255, 255, 0.65)' : tokens.text;

  const renderItems = (list: MenuItem[], parentPath: string[]): ReactNode =>
    list.map((item, index) => {
      if (item.type === 'divider') {
        return (
          <li key={`divider-${index}`} {...stylex.props(styles.divider)} style={{ backgroundColor: themeDivider(theme) }} />
        );
      }
      if (item.type === 'group') {
        return (
          <li key={item.key ?? `group-${index}`} {...stylex.props(styles.item)}>
            <div {...stylex.props(styles.groupLabel)} style={{ color: themeGroupColor(theme) }}>
              {item.label}
            </div>
            {item.children && item.children.length > 0 && renderItems(item.children, parentPath)}
          </li>
        );
      }

      const key = item.key ?? '';
      const children = item.children ?? [];
      const hasChildren = children.length > 0;
      const selected = key !== '' && currentSelected.includes(key);
      const expanded = hasChildren && currentOpen.includes(key);
      const keyPath = hasChildren ? parentPath : [...parentPath, key];

      const rowStyle: CSSProperties = {
        // 动态:主题文字色 / 选中底色 / 悬停底色
        color: selected && isDark ? tokens.onBrand : selected ? tokens.brand : baseRowColor,
        backgroundColor: selected
          ? isDark
            ? tokens.sideActive
            : tokens.brandTint
          : 'transparent',
      };

      return (
        <li key={key || `item-${index}`} {...stylex.props(styles.item)}>
          <button
            type="button"
            disabled={item.disabled}
            aria-current={selected ? 'page' : undefined}
            aria-expanded={hasChildren ? expanded : undefined}
            onClick={() => {
              if (item.disabled) return;
              if (hasChildren) toggleOpen(key);
              if (key && !hasChildren) handleSelect(key, keyPath);
            }}
            onMouseEnter={(e) => {
              if (!selected) e.currentTarget.style.backgroundColor = themeHoverBg(theme);
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = selected
                ? isDark
                  ? tokens.sideActive
                  : tokens.brandTint
                : 'transparent';
            }}
            {...stylex.props(styles.row, inlineCollapsed && styles.rowCollapsed, item.disabled && styles.rowDisabled)}
            style={rowStyle}
          >
            {item.icon && (
              <span {...stylex.props(styles.icon)}>{item.icon}</span>
            )}
            <span {...stylex.props(styles.label, inlineCollapsed && styles.labelHidden)}>{item.label}</span>
            {hasChildren && (
              <span
                {...stylex.props(styles.arrow)}
                // 动态:子菜单展开态旋转 90°
                style={{ transform: expanded ? 'rotate(90deg)' : 'rotate(0deg)' }}
              >
                <DownOutlined />
              </span>
            )}
          </button>
          {hasChildren && expanded && (
            <ul {...stylex.props(styles.subList)}>{renderItems(children, [...parentPath, key])}</ul>
          )}
        </li>
      );
    });

  void mode; // 目前仅实现 inline(现网调用点均为 mode="inline")

  const navCx = stylex.props(styles.nav);

  return (
    <nav
      {...navCx}
      className={[navCx.className, className].filter(Boolean).join(' ') || undefined}
      style={style}
    >
      <ul {...stylex.props(styles.list)}>{items ? renderItems(items, []) : null}</ul>
    </nav>
  );
};
