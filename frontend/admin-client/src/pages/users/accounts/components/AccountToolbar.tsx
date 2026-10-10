import React from 'react';
import { Button, Space, Select, Input } from 'antd';
import { PlusOutlined } from '@ant-design/icons';

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
  <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
    <Space>
      <h3 style={{ margin: 0 }}>账号列表</h3>
      <Select
        value={activeRole}
        onChange={onRoleChange}
        options={roleFilterOptions}
        style={{ width: 160 }}
      />
      <Input.Search
        allowClear
        placeholder="账号/姓名/手机号"
        defaultValue={keyword}
        onSearch={onKeywordChange}
        style={{ width: 240 }}
      />
    </Space>
    <Button type="primary" icon={<PlusOutlined />} onClick={onAdd}>
      {isSuperAdmin ? '新增账号' : '新增送水员'}
    </Button>
  </div>
);

export default AccountToolbar;
