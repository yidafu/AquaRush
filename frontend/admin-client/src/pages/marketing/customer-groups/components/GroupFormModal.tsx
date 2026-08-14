import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select } from 'antd';
import { useLazyQuery } from '@apollo/client';
import type { CustomerGroup } from '@aquarush/common';
import { GET_USERS_QUERY } from '../../../../graphql/queries/user.graphql';

// 提交给 mutation 的表单载荷(memberIds 覆盖式成员列表,后端据此同步成员)
export interface GroupFormPayload {
  name: string;
  description?: string | null;
  memberIds: string[];
}

interface UserOption {
  value: string;
  label: string;
}

interface Props {
  open: boolean;
  record: CustomerGroup | null;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (payload: GroupFormPayload, record: CustomerGroup | null) => void;
}

const GroupFormModal: React.FC<Props> = ({ open, record, submitting, onCancel, onSubmit }) => {
  const [form] = Form.useForm<GroupFormPayload>();
  const [userOptions, setUserOptions] = useState<UserOption[]>([]);
  const [searchUsers, { loading: searching }] = useLazyQuery(GET_USERS_QUERY, {
    fetchPolicy: 'network-only',
    errorPolicy: 'all',
  });
  const [initLoading, setInitLoading] = useState(false);

  const toOption = (u: {
    id: number | string;
    nickname?: string | null;
    phone?: string | null;
  }): UserOption => ({
    value: String(u.id),
    label: `${u.nickname || '未设置昵称'}${u.phone ? `(${u.phone})` : ''}`,
  });

  // 合并搜索结果与已选中成员,避免选中标签因 option 缺失而回退为 id
  const mergeOptions = (options: UserOption[]) => {
    setUserOptions((prev) => {
      const map = new Map(prev.map((o) => [o.value, o]));
      options.forEach((o) => map.set(o.value, o));
      return Array.from(map.values());
    });
  };

  useEffect(() => {
    if (!open) return;
    form.resetFields();
    setUserOptions([]);
    if (!record) {
      setInitLoading(false);
      return;
    }
    // 编辑:加载该分组当前成员,预填成员选择与展示标签
    setInitLoading(true);
    searchUsers({ variables: { input: { customerGroupId: record.id, page: 0, size: 1000 } } })
      .then((result) => {
        const list = result.data?.users?.list ?? [];
        const ids = list.map((u) => String(u.id));
        mergeOptions(list.map(toOption));
        form.setFieldsValue({
          name: record.name,
          description: record.description || '',
          memberIds: ids,
        });
      })
      .finally(() => setInitLoading(false));
  }, [open, record, form, searchUsers]);

  const handleUserSearch = (keyword: string) => {
    searchUsers({ variables: { input: { search: keyword || undefined, page: 0, size: 50 } } }).then((result) => {
      const list = result.data?.users?.list ?? [];
      mergeOptions(list.map(toOption));
    });
  };

  const handleOk = async () => {
    const values = await form.validateFields();
    onSubmit(
      {
        name: values.name,
        description: values.description ?? null,
        memberIds: values.memberIds ?? [],
      },
      record,
    );
  };

  return (
    <Modal
      title={record ? '编辑客户分组' : '新建客户分组'}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={submitting || initLoading}
      destroyOnClose
      width={480}
    >
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          name="name"
          label="分组名称"
          rules={[
            { required: true, message: '请输入分组名称' },
            { max: 50, message: '分组名称不能超过50个字' },
          ]}
        >
          <Input placeholder="如:企业客户、老客户" maxLength={50} showCount />
        </Form.Item>
        <Form.Item
          name="description"
          label="分组描述"
          rules={[{ max: 500, message: '分组描述不能超过500个字' }]}
        >
          <Input.TextArea placeholder="可选,说明分组用途或规则" maxLength={500} rows={3} showCount />
        </Form.Item>
        <Form.Item
          name="memberIds"
          label="选择成员"
          tooltip="将客户划入该分组(营销受众,作用于熟客价 R-MKT-01);未选择则分组暂为空,可稍后在用户列表「设置分组」补充"
        >
          <Select
            mode="multiple"
            showSearch
            filterOption={false}
            onSearch={handleUserSearch}
            onFocus={() => handleUserSearch('')}
            options={userOptions}
            loading={searching}
            placeholder="输入昵称/手机号搜索并选择客户"
            style={{ width: '100%' }}
            maxTagCount="responsive"
            allowClear
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default GroupFormModal;
