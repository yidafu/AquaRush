import React, { useState } from 'react';
import { Card, Button, Space, message } from 'antd';
import { PlusOutlined, UploadOutlined } from '@ant-design/icons';
import { useQuery, useMutation } from '@apollo/client';
import { GET_ADDRESSES_QUERY } from '../../../graphql/queries/address.graphql';
import {
  CREATE_ADDRESS_MUTATION,
  BATCH_IMPORT_ADDRESSES_MUTATION,
  DELETE_ADDRESS_MUTATION,
} from '../../../graphql/mutations/address.graphql';
import { AddressTable } from '../components/AddressTable';
import { AddressForm } from '../components/AddressForm';
import { AddressImport } from '../components/AddressImport';
import type { Address, AddressFormData, AddressInput } from '../components/types';

const AddressListPage: React.FC = () => {
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [formVisible, setFormVisible] = useState(false);
  const [importVisible, setImportVisible] = useState(false);
  const [editingAddress, setEditingAddress] = useState<Address | null>(null);

  // 获取地址列表
  const { data, loading, refetch } = useQuery(GET_ADDRESSES_QUERY, {
    onCompleted: (data) => {
      setAddresses(data.addresses || []);
    },
    onError: (error) => {
      message.error('获取地址列表失败: ' + error.message);
    },
  });

  // 创建地址 mutation
  const [createAddress, { loading: creating }] = useMutation(CREATE_ADDRESS_MUTATION);

  // 批量导入 mutation
  const [batchImport, { loading: importing }] = useMutation(BATCH_IMPORT_ADDRESSES_MUTATION);

  // 删除地址 mutation
  const [deleteAddress] = useMutation(DELETE_ADDRESS_MUTATION);

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
    } catch (error: any) {
      message.error('删除失败: ' + error.message);
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

      await createAddress({
        variables: { input },
      });

      message.success(editingAddress ? '编辑成功' : '创建成功');
      setFormVisible(false);
      refetch();
    } catch (error: any) {
      message.error('操作失败: ' + error.message);
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
    } catch (error: any) {
      message.error('导入失败: ' + error.message);
      throw error;
    }
  };

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <Space style={{ marginBottom: 16 }}>
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
          onEdit={handleEdit}
          onDelete={handleDelete}
        />
      </Card>

      <AddressForm
        visible={formVisible}
        loading={creating}
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
