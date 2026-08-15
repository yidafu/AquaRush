import React, { useState, useCallback, useEffect } from 'react';
import { Table, Card, Select, Input, DatePicker, Space, Tag, Button, Modal, Descriptions, Segmented, Pagination, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import type { BusinessLog } from '@aquarush/common';
import { useBusinessLogs } from '../../../services/log-graphql';
import BusinessLogTimeline, { OPERATION_CONFIG, DOMAIN_OPERATIONS } from '../../../components/BusinessLogTimeline';
import JsonViewer from '../../../components/JsonViewer';

const { RangePicker } = DatePicker;
const { Option } = Select;

const LOG_LEVEL_OPTIONS = [
  { value: 'TRACE', label: 'TRACE', color: 'default' },
  { value: 'DEBUG', label: 'DEBUG', color: 'green' },
  { value: 'INFO', label: 'INFO', color: 'blue' },
  { value: 'WARN', label: 'WARN', color: 'orange' },
  { value: 'ERROR', label: 'ERROR', color: 'red' },
];

const DOMAIN_OPTIONS = [
  { value: 'ORDER', label: '订单' },
  { value: 'PAYMENT', label: '支付' },
  { value: 'DELIVERY', label: '配送' },
  { value: 'USER', label: '用户' },
  { value: 'AUTH', label: '认证' },
  { value: 'PRODUCT', label: '产品' },
  { value: 'MARKETING', label: '营销' },
  { value: 'OPS', label: '运营配置' },
  { value: 'RECONCILIATION', label: '对账' },
];

const getLogLevelInfo = (level: string) => {
  return LOG_LEVEL_OPTIONS.find(opt => opt.value === level) || { label: level, color: 'default' };
};

interface BizLogFilters {
  domain?: string;
  operationType?: string;
  level?: string;
  userId?: number;
  startTime?: string;
  endTime?: string;
}

const BusinessLogs: React.FC = () => {
  const [filters, setFilters] = useState<BizLogFilters>({});
  const [view, setView] = useState<'table' | 'timeline'>('table');
  const [pageNum, setPageNum] = useState(0); // 0 基
  const [pageSize, setPageSize] = useState(20); // 表格 20 / 时间线 10
  const [selectedLog, setSelectedLog] = useState<BusinessLog | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);

  const [loadBusinessLogs, { data, loading }] = useBusinessLogs();

  const fetchLogs = useCallback(() => {
    loadBusinessLogs({
      variables: {
        input: {
          domain: filters.domain,
          operationType: filters.operationType,
          level: filters.level,
          userId: filters.userId,
          startTime: filters.startTime,
          endTime: filters.endTime,
          page: pageNum,
          size: pageSize,
        },
      },
    });
  }, [loadBusinessLogs, filters, pageNum, pageSize]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const list = data?.businessLogs?.list ?? [];
  const pageInfo = data?.businessLogs?.pageInfo;

  const handleViewChange = (value: string | number) => {
    setView(value as 'table' | 'timeline');
    setPageNum(0);
    setPageSize(value === 'timeline' ? 10 : 20);
  };

  const handleSearch = () => {
    setPageNum(0);
  };

  const handleReset = () => {
    setFilters({});
    setPageNum(0);
  };

  const handleTableChange = (newPagination: any) => {
    setPageNum(newPagination.current - 1);
    setPageSize(newPagination.pageSize);
  };

  const showDetail = (record: BusinessLog) => {
    setSelectedLog(record);
    setDetailVisible(true);
  };

  // 消息是否以 JSON 对象/数组开头(用于选择 JsonViewer 或纯文本展示)
  const looksLikeJson = (text?: string): boolean => {
    if (!text) return false;
    const trimmed = text.trim();
    return trimmed.startsWith('{') || trimmed.startsWith('[');
  };

  const copyText = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text || '');
      message.success('已复制');
    } catch (error) {
      message.error('复制失败');
    }
  };

  // 操作类型选项:选定业务域后联动为该域类型,未选展示全部
  const allOperations = Object.values(DOMAIN_OPERATIONS).flat();
  const operationOptions = (filters.domain ? DOMAIN_OPERATIONS[filters.domain] ?? [] : allOperations).map(op => ({
    value: op,
    label: OPERATION_CONFIG[op]?.label ?? op,
  }));

  const columns = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (date: string) => date ? new Date(date).toLocaleString('zh-CN') : '-',
    },
    {
      title: '级别',
      dataIndex: 'level',
      key: 'level',
      width: 90,
      render: (level: string) => {
        const info = getLogLevelInfo(level);
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: '操作类型',
      dataIndex: 'operation',
      key: 'operation',
      width: 140,
      render: (operation: string | undefined, record: BusinessLog) => {
        const op = operation || record.message?.match(/Operation:\s*([^,\s]+)/)?.[1];
        return op ? (OPERATION_CONFIG[op]?.label ?? op) : '-';
      },
    },
    {
      title: 'Logger',
      dataIndex: 'loggerName',
      key: 'loggerName',
      width: 140,
      ellipsis: true,
    },
    {
      title: '消息',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true,
    },
    {
      title: '用户',
      dataIndex: 'username',
      key: 'username',
      width: 120,
      render: (name: string, record: BusinessLog) => name || record.userId || '-',
    },
    {
      title: '关联ID',
      dataIndex: 'correlationId',
      key: 'correlationId',
      width: 180,
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: any, record: BusinessLog) => (
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
            placeholder="业务域"
            allowClear
            style={{ width: 120 }}
            value={filters.domain}
            onChange={(value) => {
              // 业务域变更后操作类型联动重置
              setFilters(prev => ({ ...prev, domain: value, operationType: undefined }));
            }}
          >
            {DOMAIN_OPTIONS.map(opt => (
              <Option key={opt.value} value={opt.value}>{opt.label}</Option>
            ))}
          </Select>
          <Select
            placeholder="操作类型"
            allowClear
            style={{ width: 150 }}
            value={filters.operationType}
            onChange={(value) => setFilters(prev => ({ ...prev, operationType: value }))}
          >
            {operationOptions.map(opt => (
              <Option key={opt.value} value={opt.value}>{opt.label}</Option>
            ))}
          </Select>
          <Select
            placeholder="日志级别"
            allowClear
            style={{ width: 120 }}
            value={filters.level}
            onChange={(value) => setFilters(prev => ({ ...prev, level: value }))}
          >
            {LOG_LEVEL_OPTIONS.map(opt => (
              <Option key={opt.value} value={opt.value}>{opt.label}</Option>
            ))}
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
              // 修复 G40:使用本地时间格式(弃用 toISOString 的 UTC 偏移)
              setFilters(prev => ({
                ...prev,
                startTime: dates?.[0]?.format('YYYY-MM-DDTHH:mm:ss'),
                endTime: dates?.[1]?.format('YYYY-MM-DDTHH:mm:ss'),
              }));
            }}
          />
          <Button type="primary" onClick={handleSearch}>搜索</Button>
          <Button onClick={handleReset}>重置</Button>
          <Segmented
            options={[
              { label: '表格', value: 'table' },
              { label: '时间线', value: 'timeline' },
            ]}
            value={view}
            onChange={handleViewChange}
          />
        </Space>
      </Card>

      {view === 'table' ? (
        <Table
          columns={columns}
          dataSource={list}
          loading={loading}
          rowKey="id"
          pagination={{
            current: pageNum + 1,
            pageSize,
            total: pageInfo?.total ?? 0,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
          }}
          onChange={handleTableChange}
          scroll={{ x: 1200 }}
        />
      ) : (
        <>
          <Card>
            <BusinessLogTimeline logs={list} loading={loading} />
          </Card>
          <Pagination
            current={pageNum + 1}
            pageSize={10}
            total={pageInfo?.total ?? 0}
            onChange={(page) => setPageNum(page - 1)}
            showTotal={(total) => `共 ${total} 条记录`}
            style={{ marginTop: 16, textAlign: 'right' }}
          />
        </>
      )}

      <Modal
        title="日志详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={[<Button key="close" onClick={() => setDetailVisible(false)}>关闭</Button>]}
        width={800}
      >
        {selectedLog && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="日志级别">
              <Tag color={getLogLevelInfo(selectedLog.level).color}>{selectedLog.level}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="时间">{selectedLog.createdAt ? new Date(selectedLog.createdAt).toLocaleString('zh-CN') : '-'}</Descriptions.Item>
            <Descriptions.Item label="Logger">{selectedLog.loggerName || '-'}</Descriptions.Item>
            <Descriptions.Item label="关联ID">{selectedLog.correlationId || '-'}</Descriptions.Item>
            <Descriptions.Item label="用户ID">{selectedLog.userId || '-'}</Descriptions.Item>
            <Descriptions.Item label="用户名">{selectedLog.username || '-'}</Descriptions.Item>
            <Descriptions.Item label="消息" span={2}>
              {selectedLog.message ? (
                looksLikeJson(selectedLog.message) ? (
                  <JsonViewer value={selectedLog.message} maxHeight={220} />
                ) : (
                  <pre style={{ maxHeight: 150, overflow: 'auto', fontSize: 12, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                    {selectedLog.message}
                  </pre>
                )
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="堆栈跟踪" span={2}>
              {selectedLog.stackTrace ? (
                <div>
                  <pre style={{ maxHeight: 200, overflow: 'auto', fontSize: 12, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                    {selectedLog.stackTrace}
                  </pre>
                  <Button size="small" type="link" icon={<CopyOutlined />} onClick={() => copyText(selectedLog.stackTrace!)}>
                    复制堆栈
                  </Button>
                </div>
              ) : (
                '-'
              )}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default BusinessLogs;
