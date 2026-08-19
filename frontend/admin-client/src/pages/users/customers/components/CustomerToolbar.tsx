import React from 'react';
import { Button, Input, Select, Space } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { STATUS_MAPPINGS } from '../../components/types';
import type { UserStatus } from '@aquarush/common';
import type { CustomerGroupOption } from './SetCustomerGroupModal';
import { NO_GROUP_VALUE } from './SetCustomerGroupModal';

export interface CustomerToolbarProps {
  groupFilter: string | undefined;
  statusFilter: UserStatus | undefined;
  groups: CustomerGroupOption[];
  onGroupFilterChange: (value: string | undefined) => void;
  onStatusFilterChange: (value: UserStatus | undefined) => void;
  onSearch: (value: string) => void;
  onAdd: () => void;
}

/** 客户列表页头部:搜索 + 分组/状态筛选 + 新增按钮(users/customers/index.tsx 拆分) */
const CustomerToolbar: React.FC<CustomerToolbarProps> = ({
  groupFilter,
  statusFilter,
  groups,
  onGroupFilterChange,
  onStatusFilterChange,
  onSearch,
  onAdd,
}) => (
  <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
    <Space>
      <h3 style={{ margin: 0 }}>用户列表</h3>
      <Input.Search
        placeholder="搜索昵称/手机号"
        allowClear
        style={{ width: 220 }}
        onSearch={onSearch}
      />
      <Select
        allowClear
        placeholder="筛选客户分组"
        style={{ width: 200 }}
        value={groupFilter}
        onChange={onGroupFilterChange}
        showSearch
        optionFilterProp="label"
        options={[
          { value: NO_GROUP_VALUE, label: '未分组' },
          ...groups.map((g) => ({ value: String(g.id), label: `${g.name}(${g.memberCount}人)` })),
        ]}
      />
      <Select
        allowClear
        placeholder="筛选账户状态"
        style={{ width: 160 }}
        value={statusFilter}
        onChange={onStatusFilterChange}
        options={(
          ['ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'] as const
        ).map((status) => ({
          value: status,
          label: STATUS_MAPPINGS[status].text,
        }))}
      />
    </Space>
    <Button type="primary" icon={<PlusOutlined />} onClick={onAdd}>
      新增用户
    </Button>
  </div>
);

export default CustomerToolbar;
