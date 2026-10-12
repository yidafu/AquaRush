import React from 'react';
import { Button, Space, Select, Input } from '@aquarush/admin-ui';
import { PlusOutlined } from '@aquarush/admin-ui/icons';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  toolbar: {
    marginBottom: '16px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: {
    marginTop: '0px',
    marginBottom: '0px',
  },
  roleSelect: {
    width: '160px',
  },
  search: {
    width: '240px',
  },
});

export interface AccountToolbarProps {
  activeRole: string;
  roleFilterOptions: { value: string; label: string }[];
  isSuperAdmin: boolean;
  onRoleChange: (value: string) => void;
  /** 关键字搜索(username/realName/phone 模糊,服务端过滤) */
  keyword: string;
  onKeywordChange: (value: string) => void;
  onAdd: () => void;
}

/** 账号列表页头部:标题 + 「用户类型」过滤下拉 + 新增按钮(users/accounts/index.tsx 拆分) */
const AccountToolbar: React.FC<AccountToolbarProps> = ({
  activeRole,
  roleFilterOptions,
  isSuperAdmin,
  onRoleChange,
  keyword,
  onKeywordChange,
  onAdd,
}) => (
  <div {...stylex.props(styles.toolbar)}>
    <Space>
      <h3 {...stylex.props(styles.title)}>账号列表</h3>
      <Select
        value={activeRole}
        onChange={(value) => onRoleChange(value as string)}
        options={roleFilterOptions}
        {...stylex.props(styles.roleSelect)}
      />
      <Input.Search
        allowClear
        placeholder="账号/姓名/手机号"
        defaultValue={keyword}
        onSearch={onKeywordChange}
        {...stylex.props(styles.search)}
      />
    </Space>
    <Button type="primary" icon={<PlusOutlined />} onClick={onAdd}>
      {isSuperAdmin ? '新增账号' : '新增送水员'}
    </Button>
  </div>
);

export default AccountToolbar;
