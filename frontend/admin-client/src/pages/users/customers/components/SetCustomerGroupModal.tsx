import React from 'react';
import { Modal, Select } from '@/ui';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  hint: {
    marginBottom: '8px',
    // 局部色值:提示文案灰,token 无对应色
    color: '#888888',
  },
  select: {
    width: '100%',
  },
});

// 未分组选项占位值(提交时转换为 null)
export const NO_GROUP_VALUE = '__NONE__';

export interface CustomerGroupOption {
  id: string | number;
  name: string;
  memberCount?: number;
}

/** 设置分组弹窗所需的用户最小形状(完整类型见 ../components/types 的 User 别名) */
export interface GroupTargetUser {
  id: string;
  nickname?: string;
  wechatOpenId?: string;
  customerGroupId?: string;
}

export interface SetCustomerGroupModalProps {
  target: GroupTargetUser | null;
  groups: CustomerGroupOption[];
  selectedGroup: string | undefined;
  confirmLoading: boolean;
  onSelectedGroupChange: (value: string | undefined) => void;
  onOk: () => void;
  onCancel: () => void;
}

/** 设置用户所属客户分组弹窗(users/customers/index.tsx 拆分) */
const SetCustomerGroupModal: React.FC<SetCustomerGroupModalProps> = ({
  target,
  groups,
  selectedGroup,
  confirmLoading,
  onSelectedGroupChange,
  onOk,
  onCancel,
}) => (
  <Modal
    title={`设置分组 - ${target?.nickname || target?.wechatOpenId || ''}`}
    open={target !== null}
    onOk={onOk}
    onCancel={onCancel}
    confirmLoading={confirmLoading}
    destroyOnClose
    okText="确定"
  >
    <div {...stylex.props(styles.hint)}>
      设置用户所属客户分组,选择「未分组」将用户移出当前分组(即时生效)。
    </div>
    <Select
      {...stylex.props(styles.select)}
      value={selectedGroup}
      onChange={(value) => onSelectedGroupChange(value as string | undefined)}
      showSearch
      optionFilterProp="label"
      options={[
        { value: NO_GROUP_VALUE, label: '未分组' },
        ...groups.map((g) => ({ value: String(g.id), label: g.name })),
      ]}
      placeholder="选择客户分组"
    />
  </Modal>
);

export default SetCustomerGroupModal;
