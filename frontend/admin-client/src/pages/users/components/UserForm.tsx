import React from 'react';
import { Form, Input, Select, Modal, Upload } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { UserFormData, FormModalProps } from './types';
import type { UploadFile, UploadProps } from 'antd/es/upload/interface';

const UserForm: React.FC<FormModalProps<UserFormData>> = ({
  open,
  onClose,
  record,
  onSubmit,
  loading = false,
}) => {
  const [form] = Form.useForm();
  const [fileList, setFileList] = React.useState<UploadFile[]>([]);

  React.useEffect(() => {
    if (record) {
      form.setFieldsValue({
        nickname: record.nickname,
        phone: record.phone,
        wechatOpenId: record.wechatOpenId,
        avatarUrl: record.avatarUrl,
        status: record.status,
      });

      if (record.avatarUrl) {
        setFileList([
          {
            uid: '-1',
            name: 'avatar.jpg',
            status: 'done',
            url: record.avatarUrl,
          },
        ]);
      }
    } else {
      form.resetFields();
      form.setFieldsValue({
        status: 'ACTIVE',
      });
      setFileList([]);
    }
  }, [record, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();

      // Handle avatar URL from upload if present
      if (fileList.length > 0 && fileList[0].url) {
        values.avatarUrl = fileList[0].url;
      }

      await onSubmit(values);
      form.resetFields();
      setFileList([]);
    } catch (error) {
      console.error('Form validation failed:', error);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    setFileList([]);
    onClose();
  };

  const uploadProps: UploadProps = {
    name: 'file',
    listType: 'picture-card',
    fileList,
    maxCount: 1,
    accept: 'image/*',
    beforeUpload: (file) => {
      const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
      if (!isJpgOrPng) {
        console.error('只能上传 JPG/PNG 格式的图片!');
        return false;
      }
      const isLt2M = file.size / 1024 / 1024 < 2;
      if (!isLt2M) {
        console.error('图片大小不能超过 2MB!');
        return false;
      }
      return false; // Prevent auto upload
    },
    onChange: (info) => {
      setFileList(info.fileList.slice(-1));
    },
    onRemove: () => {
      setFileList([]);
    },
  };

  const uploadButton = (
    <div>
      <PlusOutlined />
      <div style={{ marginTop: 8 }}>上传头像</div>
    </div>
  );

  return (
    <Modal
      title={record ? '编辑用户' : '新增用户'}
      open={open}
      onOk={handleSubmit}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          status: 'ACTIVE',
        }}
      >
        <Form.Item
          label="头像"
          name="avatarUrl"
        >
          <Upload {...uploadProps}>
            {fileList.length >= 1 ? null : uploadButton}
          </Upload>
        </Form.Item>

        <Form.Item
          label="昵称"
          name="nickname"
          rules={[
            { max: 50, message: '昵称最多50个字符' },
          ]}
        >
          <Input placeholder="请输入昵称" />
        </Form.Item>

        <Form.Item
          label="手机号"
          name="phone"
          rules={[
            { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号' },
          ]}
        >
          <Input placeholder="请输入手机号" />
        </Form.Item>

        <Form.Item
          label="微信OpenID"
          name="wechatOpenId"
          rules={[
            { required: true, message: '请输入微信OpenID' },
            { min: 1, max: 100, message: '微信OpenID长度为1-100个字符' },
          ]}
        >
          <Input placeholder="请输入微信OpenID" />
        </Form.Item>

        <Form.Item
          label="状态"
          name="status"
          rules={[{ required: true, message: '请选择状态' }]}
        >
          <Select placeholder="请选择状态">
            <Select.Option value="ACTIVE">正常</Select.Option>
            <Select.Option value="INACTIVE">未激活</Select.Option>
            <Select.Option value="SUSPENDED">已暂停</Select.Option>
            <Select.Option value="DELETED">已删除</Select.Option>
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default UserForm;