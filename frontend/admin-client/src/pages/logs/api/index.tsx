import React, { useState, useEffect } from 'react';
import * as stylex from '@stylexjs/stylex';
import { queryApiLogs } from '../../../services/logs';
import type { ApiLogItem, ApiLogQueryParams } from '../../../services/logs';
import { ApiLogFilters, ApiLogTable, ApiLogDetailModal } from './components';
import { pagination as createPagination } from '@/utils/table';

// 原内联 padding: '24px' 迁 stylex(stylex 禁简写,拆 longhand)
const styles = stylex.create({
  page: {
    paddingTop: '24px',
    paddingRight: '24px',
    paddingBottom: '24px',
    paddingLeft: '24px',
  },
});

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
    <div {...stylex.props(styles.page)}>
      <ApiLogFilters
        filters={filters}
        onChange={(partial) => setFilters(prev => ({ ...prev, ...partial }))}
        onSearch={handleSearch}
        onReset={handleReset}
      />

      <ApiLogTable
        data={data}
        loading={loading}
        pagination={createPagination(pagination.total, {
          current: pagination.current,
          pageSize: pagination.pageSize,
          showSizeChanger: true,
          showQuickJumper: true,
        })}
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
