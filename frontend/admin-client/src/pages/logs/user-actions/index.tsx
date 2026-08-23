import React, { useState, useEffect } from 'react';
import { Table, Card, Select, Input, DatePicker, Space, Tag, Button, Modal, Descriptions } from 'antd';
import { queryUserActionLogs } from '../../../services/logs';
import type { UserActionLogItem, UserActionLogQueryParams } from '../../../services/logs';
import JsonViewer from '../../../components/JsonViewer';
import { pagination as createPagination } from '@/utils/table';

const { RangePicker } = DatePicker;
const { Option } = Select;

const ACTION_TYPE_OPTIONS = [
  { value: 'PAGE_VIEW', label: '页面浏览', color: 'blue' },
  { value: 'CLICK', label: '点击', color: 'green' },
  { value: 'FORM_SUBMIT', label: '表单提交', color: 'orange' },
  { value: 'SEARCH', label: '搜索', color: 'purple' },
  { value: 'SHARE', label: '分享', color: 'cyan' },
  { value: 'SCROLL', label: '滚动', color: 'magenta' },
  { value: 'FILE_OPERATION', label: '文件操作', color: 'red' },
  // 后端自定义/审计类操作类型
  { value: 'USER_LOGIN', label: '用户登录', color: 'geekblue' },
  { value: 'USER_LOGOUT', label: '用户登出', color: 'default' },
  { value: 'SYS_CONFIG_SAVE', label: '系统配置保存', color: 'gold' },
  { value: 'SYS_CONFIG_RESTORE', label: '系统配置恢复', color: 'volcano' },
];

const UserActionLogs: React.FC = () => {
  const [data, setData] = useState<UserActionLogItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0,
  });
  const [filters, setFilters] = useState<UserActionLogQueryParams>({});
  const [selectedLog, setSelectedLog] = useState<UserActionLogItem | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const result = await queryUserActionLogs({
        ...filters,
        page: pagination.current - 1,
        size: pagination.pageSize,
      });
      setData(result.content);
      setPagination(prev => ({
        ...prev,
        total: result.totalElements,
      }));
    } catch (error) {
      console.error('Failed to fetch user action logs:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [filters, pagination.current, pagination.pageSize]);

  const handleTableChange = (newPagination: any) => {
    setPagination({
      current: newPagination.current,
      pageSize: newPagination.pageSize,
      total: pagination.total,
    });
  };

  const handleSearch = () => {
    setPagination(prev => ({ ...prev, current: 1 }));
  };

  const handleReset = () => {
    setFilters({});
    setPagination(prev => ({ ...prev, current: 1 }));
  };

  const showDetail = (record: UserActionLogItem) => {
    setSelectedLog(record);
    setDetailVisible(true);
  };

  const getActionTypeInfo = (actionType: string) => {
    return ACTION_TYPE_OPTIONS.find(opt => opt.value === actionType) || { label: actionType, color: 'default' };
  };

  const columns = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (date: string) => date ? new Date(date).toLocaleString('zh-CN') : '-',
    },
    {
      title: '操作类型',
      dataIndex: 'actionType',
      key: 'actionType',
      width: 120,
      render: (type: string) => {
        const info = getActionTypeInfo(type);
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: '用户',
      dataIndex: 'username',
      key: 'username',
      width: 120,
      render: (name: string, record: UserActionLogItem) => name || record.userId || '-',
    },
    {
      title: '页面',
      dataIndex: 'pageUrl',
      key: 'pageUrl',
      ellipsis: true,
    },
    {
      title: '目标',
      dataIndex: 'target',
      key: 'target',
      ellipsis: true,
    },
    {
      title: '元素ID',
      dataIndex: 'elementId',
      key: 'elementId',
      width: 150,
    },
    {
      title: 'IP',
      dataIndex: 'clientIp',
      key: 'clientIp',
      width: 130,
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: any, record: UserActionLogItem) => (
        <Button type="link" size="small" onClick={() => showDetail(record)}>
          详情
        </Button>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select
            placeholder="操作类型"
            allowClear
            style={{ width: 140 }}
            value={filters.actionType}
            onChange={(value) => setFilters(prev => ({ ...prev, actionType: value }))}
          >
            {ACTION_TYPE_OPTIONS.map(opt => (
              <Option key={opt.value} value={opt.value}>{opt.label}</Option>
            ))}
          </Select>
          <Input
            placeholder="用户ID"
            allowClear
            style={{ width: 120 }}
            value={filters.userId}
            onChange={(e) => setFilters(prev => ({ ...prev, userId: e.target.value || undefined }))}
          />
          <Input
            placeholder="用户名"
            allowClear
            style={{ width: 120 }}
            value={filters.username}
            onChange={(e) => setFilters(prev => ({ ...prev, username: e.target.value || undefined }))}
          />
          <Input
            placeholder="目标"
            allowClear
            style={{ width: 140 }}
            value={filters.target}
            onChange={(e) => setFilters(prev => ({ ...prev, target: e.target.value || undefined }))}
          />
          <RangePicker
            showTime
            onChange={(dates) => {
              setFilters(prev => ({
                ...prev,
                startTime: dates?.[0]?.toISOString(),
                endTime: dates?.[1]?.toISOString(),
              }));
            }}
          />
          <Button type="primary" onClick={handleSearch}>搜索</Button>
          <Button onClick={handleReset}>重置</Button>
        </Space>
      </Card>

      <Table
        columns={columns}
        dataSource={data}
        loading={loading}
        rowKey="id"
        pagination={createPagination(pagination.total, {
          current: pagination.current,
          pageSize: pagination.pageSize,
          showSizeChanger: true,
          showQuickJumper: true,
        })}
        onChange={handleTableChange}
        scroll={{ x: 1200 }}
      />

      <Modal
        title="操作详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={[<Button key="close" onClick={() => setDetailVisible(false)}>关闭</Button>]}
        width={700}
      >
        {selectedLog && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="操作类型">{selectedLog.actionType}</Descriptions.Item>
            <Descriptions.Item label="用户ID">{selectedLog.userId || '-'}</Descriptions.Item>
            <Descriptions.Item label="用户名">{selectedLog.username || '-'}</Descriptions.Item>
            <Descriptions.Item label="IP">{selectedLog.clientIp || '-'}</Descriptions.Item>
            <Descriptions.Item label="页面URL" span={2}>{selectedLog.pageUrl || '-'}</Descriptions.Item>
            <Descriptions.Item label="目标" span={2}>{selectedLog.target || '-'}</Descriptions.Item>
            <Descriptions.Item label="元素ID">{selectedLog.elementId || '-'}</Descriptions.Item>
            <Descriptions.Item label="元素类型">{selectedLog.elementType || '-'}</Descriptions.Item>
            <Descriptions.Item label="元素文本">{selectedLog.elementText || '-'}</Descriptions.Item>
            <Descriptions.Item label="UserAgent" span={2}>
              <div style={{ maxHeight: 80, overflow: 'auto', fontSize: 12 }}>
                {selectedLog.userAgent || '-'}
              </div>
            </Descriptions.Item>
            <Descriptions.Item label="附加属性" span={2}>
              {selectedLog.properties ? (
                <JsonViewer value={selectedLog.properties} maxHeight={260} />
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="时间">{new Date(selectedLog.createdAt).toLocaleString('zh-CN')}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default UserActionLogs;
