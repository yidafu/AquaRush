import React from 'react';
import { Button, Input, Select, Space } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { STATUS_MAPPINGS } from '../../components/types';
import type { UserStatus } from '@aquarush/common';
import type { CustomerGroupOption } from './SetCustomerGroupModal';
import { NO_GROUP_VALUE } from './SetCustomerGroupModal';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  toolbar: {
    marginBottom: '16px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  heading: {
    marginTop: '0px',
    marginRight: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
  },
  search: {
    width: '220px',
  },
  groupSelect: {
    width: '200px',
  },
  statusSelect: {
    width: '160px',
  },
});

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
  <div {...stylex.props(styles.toolbar)}>
    <Space>
      <h3 {...stylex.props(styles.heading)}>用户列表</h3>
      <Input.Search
        placeholder="搜索昵称/手机号"
        allowClear
        {...stylex.props(styles.search)}
        onSearch={onSearch}
      />
      <Select
        allowClear
        placeholder="筛选客户分组"
        {...stylex.props(styles.groupSelect)}
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
        {...stylex.props(styles.statusSelect)}
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
