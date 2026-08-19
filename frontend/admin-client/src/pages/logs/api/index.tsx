import React, { useState, useEffect } from 'react';
import { queryApiLogs } from '../../../services/logs';
import type { ApiLogItem, ApiLogQueryParams } from '../../../services/logs';
import { ApiLogFilters, ApiLogTable, ApiLogDetailModal } from './components';

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

  const handleTableChange = (next: { current: number; pageSize: number }) => {
    setPagination(prev => ({
      ...prev,
      current: next.current,
      pageSize: next.pageSize,
    }));
  };

  const handleSearch = () => {
    setPagination(prev => ({ ...prev, current: 1 }));
  };

  const handleReset = () => {
    setFilters({});
    setPagination(prev => ({ ...prev, current: 1 }));
  };

  return (
    <div style={{ padding: '24px' }}>
      <ApiLogFilters
        filters={filters}
        onChange={(partial) => setFilters(prev => ({ ...prev, ...partial }))}
        onSearch={handleSearch}
        onReset={handleReset}
      />

      <ApiLogTable
        data={data}
        loading={loading}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条记录`,
        }}
        onChange={handleTableChange}
        onViewDetail={(log) => {
          setSelectedLog(log);
          setDetailVisible(true);
        }}
      />

      <ApiLogDetailModal
        log={selectedLog}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
      />
    </div>
  );
};

export default ApiLogs;
