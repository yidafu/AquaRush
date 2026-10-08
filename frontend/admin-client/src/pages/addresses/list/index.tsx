import React, { useState } from 'react';
import { Card, Button, Space, Input, message } from 'antd';
import { PlusOutlined, UploadOutlined } from '@ant-design/icons';
import type { TablePaginationConfig } from 'antd/es/table';
import { useQuery, useMutation } from '@apollo/client';
import { SEARCH_ALL_ADDRESSES_QUERY } from '../../../graphql/queries/address.graphql';
import {
  CREATE_ADDRESS_MUTATION,
  UPDATE_ADDRESS_MUTATION,
  BATCH_IMPORT_ADDRESSES_MUTATION,
  DELETE_ADDRESS_MUTATION,
} from '../../../graphql/mutations/address.graphql';
import { AddressTable } from '../components/AddressTable';
import { AddressForm } from '../components/AddressForm';
import { AddressImport } from '../components/AddressImport';
import type { Address, AddressFormData, AddressInput } from '../components/types';

const AddressListPage: React.FC = () => {
  const [formVisible, setFormVisible] = useState(false);
  const [importVisible, setImportVisible] = useState(false);
  const [editingAddress, setEditingAddress] = useState<Address | null>(null);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  // 分页 + 关键字搜索地址列表(G25: 筛选/分页,禁止全量拉取)
  const { data, loading, refetch } = useQuery(SEARCH_ALL_ADDRESSES_QUERY, {
    variables: { keyword: keyword || null, page, size },
    fetchPolicy: 'network-only',
    onError: (error) => {
      message.error('获取地址列表失败: ' + error.message);
    },
  });

  // 创建地址 mutation
  const [createAddress, { loading: creating }] = useMutation(CREATE_ADDRESS_MUTATION);

  // 更新地址 mutation
  const [updateAddress, { loading: updating }] = useMutation(UPDATE_ADDRESS_MUTATION);

  // 批量导入 mutation
  const [batchImport] = useMutation(BATCH_IMPORT_ADDRESSES_MUTATION);

  // 删除地址 mutation
  const [deleteAddress] = useMutation(DELETE_ADDRESS_MUTATION);

  const addresses = data?.searchAllAddresses?.content ?? [];
  const total = data?.searchAllAddresses?.totalElements ?? 0;

  // 处理新增
  const handleAdd = () => {
    setEditingAddress(null);
    setFormVisible(true);
  };

  // 处理编辑
  const handleEdit = (record: Address) => {
    setEditingAddress(record);
    setFormVisible(true);
  };

  // 处理删除
  const handleDelete = async (record: Address) => {
    try {
      await deleteAddress({
        variables: { id: record.id },
      });
      message.success('删除成功');
      refetch();
    } catch (error) {
      message.error(error instanceof Error ? '删除失败: ' + error.message : '删除失败');
    }
  };

  // 处理表单提交
  const handleFormSubmit = async (values: AddressFormData) => {
    try {
      const input: AddressInput = {
        receiverName: values.receiverName,
        phone: values.phone,
        province: values.province,
        provinceCode: values.provinceCode,
        city: values.city,
        cityCode: values.cityCode,
        district: values.district,
        districtCode: values.districtCode,
        detailAddress: values.detailAddress,
        longitude: values.longitude,
        latitude: values.latitude,
        isDefault: values.isDefault,
      };

      if (editingAddress) {
        await updateAddress({
          variables: { id: editingAddress.id, input },
        });
      } else {
        await createAddress({
          variables: { input },
        });
      }

      message.success(editingAddress ? '编辑成功' : '创建成功');
      setFormVisible(false);
      refetch();
    } catch (error) {
      message.error(error instanceof Error ? '操作失败: ' + error.message : '操作失败');
      throw error;
    }
  };

  // 处理导入
  const handleImport = async (addresses: AddressInput[]) => {
    try {
      await batchImport({
        variables: { input: addresses },
      });
      refetch();
    } catch (error) {
      message.error(error instanceof Error ? '导入失败: ' + error.message : '导入失败');
      throw error;
    }
  };

  // 处理搜索
  const handleSearch = (value: string) => {
    setKeyword(value);
    setPage(0);
  };

  // 处理分页/每页条数变更
  const handleTableChange = (pagination: TablePaginationConfig) => {
    setPage((pagination.current ?? 1) - 1);
    setSize(pagination.pageSize ?? 20);
  };

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Input.Search
            placeholder="搜索收货人/手机号/详细地址"
            allowClear
            style={{ width: 260 }}
            onSearch={handleSearch}
          />
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleAdd}
          >
            新增地址
          </Button>
          <Button
            icon={<UploadOutlined />}
            onClick={() => setImportVisible(true)}
          >
            导入 Excel
          </Button>
        </Space>

        <AddressTable
          addresses={addresses}
          loading={loading}
          total={total}
          current={page + 1}
          pageSize={size}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onChange={handleTableChange}
        />
      </Card>

      <AddressForm
        visible={formVisible}
        loading={creating || updating}
        record={editingAddress}
        onCancel={() => setFormVisible(false)}
        onSubmit={handleFormSubmit}
      />

      <AddressImport
        visible={importVisible}
        onCancel={() => setImportVisible(false)}
        onImport={handleImport}
      />
    </div>
  );
};

export default AddressListPage;
