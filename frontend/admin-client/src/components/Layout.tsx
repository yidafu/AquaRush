import React, { useEffect, useMemo, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Menu, Dropdown, Avatar, Breadcrumb, message } from '@aquarush/admin-ui';
import type { DropdownMenuItem } from '@aquarush/admin-ui';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { usePageViewTracking } from '../hooks/usePageViewTracking';
import { userActionTracker } from '../utils/tracking';
import { avatarFromName } from '@aquarush/common/utils';
import { logout } from '../services/auth';
import { UserOutlined, LogoutOutlined, SettingOutlined } from '@aquarush/admin-ui/icons';
import { buildMenuItems, collectLeafKeys, findOpenKeys } from './LayoutMenu';
import { resolveCrumbs } from './LayoutCrumbs';

// Layout 骨架:StyleX div 复刻 antd Layout DOM(div > aside.sider + div > header + main),
// 底色/描边原依赖 antd theme token,现映射到共享 token(bg/card/border/side)。
const styles = stylex.create({
  root: {
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'row',
    // 原 antd .ant-layout 底色 colorBgLayout(spec/design/ui-design-system.md §2.3)
    backgroundColor: tokens.bg,
  },
  sider: {
    position: 'relative',
    flexGrow: '0',
    flexShrink: '0',
    flexBasis: '200px',
    width: '200px',
    minWidth: '200px',
    maxWidth: '200px',
    // 原 antd Layout Sider theme="dark" 底色 #001529
    backgroundColor: tokens.side,
    overflowY: 'auto',
  },
  siderChildren: {
    display: 'flex',
    flexDirection: 'column',
    minHeight: '100%',
  },
  main: {
    flexGrow: '1',
    flexShrink: '1',
    flexBasis: '0%',
    minWidth: '0px',
    display: 'flex',
    flexDirection: 'column',
    // 原 antd 内层 Layout style={{ background: token.colorBgLayout }}
    backgroundColor: tokens.bg,
  },
  brand: {
    height: '64px',
    display: 'flex',
    alignItems: 'center',
    columnGap: '10px',
    rowGap: '10px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '16px',
    paddingRight: '16px',
    color: tokens.onBrand,
  },
  brandLogo: {
    width: '26px',
    height: '26px',
  },
  brandName: {
    fontSize: '16px',
    fontWeight: 600,
    whiteSpace: 'nowrap',
  },
  header: {
    height: '64px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '24px',
    paddingRight: '24px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    // 原 antd Header token:background colorBgContainer + borderBottom colorBorderSecondary
    backgroundColor: tokens.card,
    borderBottomWidth: '1px',
    borderBottomStyle: 'solid',
    borderBottomColor: tokens.border,
  },
  userBox: {
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    columnGap: '8px',
    rowGap: '8px',
  },
  content: {
    marginTop: '24px',
    marginBottom: '24px',
    marginLeft: '24px',
    marginRight: '24px',
    paddingTop: '24px',
    paddingBottom: '24px',
    paddingLeft: '24px',
    paddingRight: '24px',
  },
});

const Layout: React.FC = () => {
  const navigate = useNavigate();
  const { pathname } = useLocation();

  // 埋点:路由变化上报 PAGE_VIEW
  usePageViewTracking();

  // 仅超级管理员可管理管理员账号(后端以 PERMISSION_ADMIN_USER_* 强制,前端菜单隐藏为 UX 层)
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  const isSuperAdmin = userInfo.role === 'SUPER_ADMIN';

  // 菜单在渲染期构建,保证登录后( userInfo 已写入 localStorage)系统设置分组立即可见
  const menuItems = useMemo(() => buildMenuItems(isSuperAdmin), [isSuperAdmin]);

  const leafKeys = useMemo(() => collectLeafKeys(menuItems), [menuItems]);

  // 菜单高亮随路由:精确匹配,否则取最长前缀(如 /orders/123 → /orders)
  const selectedKey = useMemo(() => {
    if (leafKeys.includes(pathname)) return pathname;
    return (
      leafKeys
        .filter((key) => pathname.startsWith(`${key}/`))
        .sort((a, b) => b.length - a.length)[0] ?? pathname
    );
  }, [leafKeys, pathname]);

  // 子菜单展开:首次按当前路由展开,路由变化时补开选中项所在子菜单;用户手动收起不强制回弹
  const [openKeys, setOpenKeys] = useState<string[]>(() => findOpenKeys(menuItems, selectedKey) ?? []);
  useEffect(() => {
    const chain = findOpenKeys(menuItems, selectedKey) ?? [];
    setOpenKeys((prev) => (chain.every((key) => prev.includes(key)) ? prev : [...prev, ...chain]));
  }, [selectedKey]);

  const crumbs = useMemo(() => resolveCrumbs(pathname), [pathname]);

  const handleLogout = async () => {
    // 调用后端登出接口(G15 落地:记录登出业务日志;JWT 无状态,服务端仅记账)
    try {
      await logout();
    } catch (e) {
      console.warn('登出接口调用失败，仍将清除本地凭证', e);
    }
    // 埋点:登出(显式携带用户上下文,避免登出清空本地存储后上报丢失归属)
    const logoutUser = JSON.parse(localStorage.getItem('userInfo') || '{}');
    userActionTracker.track('CLICK', 'logout', {
      elementType: 'menu',
      elementText: '退出登录',
      userId: logoutUser?.id != null ? String(logoutUser.id) : undefined,
      username: logoutUser?.username || logoutUser?.nickname || undefined,
    });
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    message.success('已退出登录');
    navigate('/login');
  };

  const userMenuItems: DropdownMenuItem[] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人信息',
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: '设置',
    },
    {
      key: 'divider',
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ];

  return (
    <div {...stylex.props(styles.root)}>
      <aside {...stylex.props(styles.sider)}>
        <div {...stylex.props(styles.siderChildren)}>
          <div {...stylex.props(styles.brand)}>
            <img src="/aqua-rush-mark.svg" alt="奔泉" {...stylex.props(styles.brandLogo)} />
            <span {...stylex.props(styles.brandName)}>奔泉运营后台</span>
          </div>
          <Menu
            theme="dark"
            mode="inline"
            items={menuItems}
            selectedKeys={[selectedKey]}
            openKeys={openKeys}
            onOpenChange={(keys) => setOpenKeys(keys)}
            onClick={({ key }) => navigate(key)}
          />
        </div>
      </aside>
      <div {...stylex.props(styles.main)}>
        <header {...stylex.props(styles.header)}>
          {crumbs.length > 0 && (
            <Breadcrumb items={crumbs.map((title) => ({ title }))} />
          )}
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <div {...stylex.props(styles.userBox)}>
              <Avatar
                src={userInfo.avatar || avatarFromName(userInfo.username || userInfo.nickname || '管理员')}
                icon={<UserOutlined />}
              />
              <span>{userInfo.username || userInfo.nickname || '管理员'}</span>
            </div>
          </Dropdown>
        </header>
        {/* 灰底画布(spec/design/ui-design-system.md §2.3 colorBgLayout),内容白卡由页面内 Card 呈现 */}
        <main {...stylex.props(styles.content)}>
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;
