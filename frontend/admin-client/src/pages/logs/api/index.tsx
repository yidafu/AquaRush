import React, { useState, useEffect } from 'react';
import { Table, Card, Select, Input, DatePicker, Space, Tag, Button, Modal, Descriptions, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { queryApiLogs } from '../../../services/logs';
import type { ApiLogItem, ApiLogQueryParams } from '../../../services/logs';
import JsonViewer from '../../../components/JsonViewer';

const { RangePicker } = DatePicker;
const { Option } = Select;

const ApiLogs: React.FC = () => {
  const [data, setData] = useState<ApiLogItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0,
  });
  const [filters, setFilters] = useState<ApiLogQueryParams>({});
  const [selectedLog, setSelectedLog] = useState<ApiLogItem | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const result = await queryApiLogs({
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
      console.error('Failed to fetch API logs:', error);
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

  const showDetail = (record: ApiLogItem) => {
    setSelectedLog(record);
    setDetailVisible(true);
  };

  // 复制文本(带成功反馈)
  const copyText = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text || '');
      message.success('已复制');
    } catch (error) {
      message.error('复制失败');
    }
  };

  const getStatusColor = (status?: number) => {
    if (!status) return 'default';
    if (status >= 200 && status < 300) return 'green';
    if (status >= 400 && status < 500) return 'orange';
    if (status >= 500) return 'red';
    return 'default';
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
      title: '类型',
      dataIndex: 'requestType',
      key: 'requestType',
      width: 100,
      render: (type: string) => (
        <Tag color={type === 'HTTP' ? 'blue' : 'purple'}>{type}</Tag>
      ),
    },
    {
      title: '方法',
      dataIndex: 'method',
      key: 'method',
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'responseStatus',
      key: 'responseStatus',
      width: 80,
      render: (status: number) => (
        <Tag color={getStatusColor(status)}>{status || '-'}</Tag>
      ),
    },
    {
      title: 'URI',
      dataIndex: 'uri',
      key: 'uri',
      ellipsis: true,
    },
    {
      title: '耗时',
      dataIndex: 'durationMs',
      key: 'durationMs',
      width: 80,
      render: (ms: number) => ms ? `${ms}ms` : '-',
    },
    {
      title: '用户',
      dataIndex: 'username',
      key: 'username',
      width: 120,
      render: (name: string, record: ApiLogItem) => name || record.userId || '-',
    },
    {
      title: 'IP',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      width: 130,
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: any, record: ApiLogItem) => (
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
            placeholder="请求类型"
            allowClear
            style={{ width: 120 }}
            value={filters.requestType}
            onChange={(value) => setFilters(prev => ({ ...prev, requestType: value }))}
          >
            <Option value="HTTP">HTTP</Option>
            <Option value="GRAPHQL">GraphQL</Option>
          </Select>
          <Select
            placeholder="请求方法"
            allowClear
            style={{ width: 100 }}
            value={filters.method}
            onChange={(value) => setFilters(prev => ({ ...prev, method: value }))}
          >
            <Option value="GET">GET</Option>
            <Option value="POST">POST</Option>
            <Option value="PUT">PUT</Option>
            <Option value="DELETE">DELETE</Option>
          </Select>
          <Input
            placeholder="用户ID"
            allowClear
            style={{ width: 120 }}
            value={filters.userId}
            onChange={(e) => setFilters(prev => ({ ...prev, userId: e.target.value ? Number(e.target.value) : undefined }))}
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
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条记录`,
        }}
        onChange={handleTableChange}
        scroll={{ x: 1200 }}
      />

      <Modal
        title="请求详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={[<Button key="close" onClick={() => setDetailVisible(false)}>关闭</Button>]}
        width={800}
      >
        {selectedLog && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="请求类型">{selectedLog.requestType}</Descriptions.Item>
            <Descriptions.Item label="方法">{selectedLog.method}</Descriptions.Item>
            <Descriptions.Item label="URI" span={2}>{selectedLog.uri}</Descriptions.Item>
            <Descriptions.Item label="Query" span={2}>
              {selectedLog.query ? (
                <div>
                  <pre style={{ maxHeight: 150, overflow: 'auto', fontSize: 12, whiteSpace: 'pre-wrap', wordBreak: 'break-all', fontFamily: 'Monaco, Menlo, "Ubuntu Mono", monospace' }}>
                    {selectedLog.query}
                  </pre>
                  <Button size="small" type="link" icon={<CopyOutlined />} onClick={() => copyText(selectedLog.query!)}>
                    复制 Query
                  </Button>
                </div>
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="状态码">{selectedLog.responseStatus}</Descriptions.Item>
            <Descriptions.Item label="耗时">{selectedLog.durationMs}ms</Descriptions.Item>
            <Descriptions.Item label="用户ID">{selectedLog.userId}</Descriptions.Item>
            <Descriptions.Item label="用户名">{selectedLog.username}</Descriptions.Item>
            <Descriptions.Item label="IP">{selectedLog.ipAddress}</Descriptions.Item>
            <Descriptions.Item label="UserAgent" span={2}>{selectedLog.userAgent}</Descriptions.Item>
            <Descriptions.Item label="关联ID">{selectedLog.correlationId}</Descriptions.Item>
            <Descriptions.Item label="操作类型">{selectedLog.operationType}</Descriptions.Item>
            <Descriptions.Item label="操作名称">{selectedLog.operationName}</Descriptions.Item>
            <Descriptions.Item label="请求体" span={2}>
              {selectedLog.requestBody ? (
                <JsonViewer value={selectedLog.requestBody} maxHeight={260} />
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="错误信息" span={2}>
              {selectedLog.errorMessage ? (
                <div>
                  <pre style={{ maxHeight: 100, overflow: 'auto', fontSize: 12, color: 'red', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                    {selectedLog.errorMessage}
                  </pre>
                  <Button size="small" type="link" icon={<CopyOutlined />} onClick={() => copyText(selectedLog.errorMessage!)}>
                    复制错误信息
                  </Button>
                </div>
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

export default ApiLogs;
