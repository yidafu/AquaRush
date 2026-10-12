import React, { useState, useCallback, useEffect } from 'react';
import { Card, Pagination } from '@aquarush/admin-ui';
import * as stylex from '@stylexjs/stylex';
import type { BusinessLogVo } from '@aquarush/common';
import { useBusinessLogs } from '../../../services/log-graphql';
import BusinessLogTimeline, { OPERATION_CONFIG, DOMAIN_OPERATIONS } from '../../../components/BusinessLogTimeline';
import { BusinessLogFilters, BusinessLogTable, BusinessLogDetailModal } from './components';
import type { BizLogFilters } from './components/constants';
import { pagination } from '@/utils/table';

// 原内联 padding: '24px' 迁 stylex(stylex 禁简写,拆 longhand)
const styles = stylex.create({
  page: {
    paddingTop: '24px',
    paddingRight: '24px',
    paddingBottom: '24px',
    paddingLeft: '24px',
  },
  timelinePagination: {
    marginTop: '16px',
    textAlign: 'right',
  },
});

const BusinessLogs: React.FC = () => {
  const [filters, setFilters] = useState<BizLogFilters>({});
  const [view, setView] = useState<'table' | 'timeline'>('table');
  const [pageNum, setPageNum] = useState(0); // 0 基
  const [pageSize, setPageSize] = useState(20); // 表格 20 / 时间线 10
  const [selectedLog, setSelectedLog] = useState<BusinessLogVo | null>(null);
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

  const handleViewChange = (value: 'table' | 'timeline') => {
    setView(value);
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

  // 操作类型选项:选定业务域后联动为该域类型,未选展示全部
  const allOperations = Object.values(DOMAIN_OPERATIONS).flat();
  const operationOptions = (filters.domain ? DOMAIN_OPERATIONS[filters.domain] ?? [] : allOperations).map(op => ({
    value: op,
    label: OPERATION_CONFIG[op]?.label ?? op,
  }));

  return (
    <div {...stylex.props(styles.page)}>
      <BusinessLogFilters
        filters={filters}
        operationOptions={operationOptions}
        view={view}
        onChange={(partial) => setFilters(prev => ({ ...prev, ...partial }))}
        onSearch={handleSearch}
        onReset={handleReset}
        onViewChange={handleViewChange}
      />

      {view === 'table' ? (
        <BusinessLogTable
          list={list}
          loading={loading}
          pageNum={pageNum}
          pageSize={pageSize}
          total={pageInfo?.total ?? 0}
          onPageChange={(nextPage, nextSize) => {
            setPageNum(nextPage);
            setPageSize(nextSize);
          }}
          onViewDetail={(log) => {
            setSelectedLog(log);
            setDetailVisible(true);
          }}
        />
      ) : (
        <>
          <Card>
            <BusinessLogTimeline logs={list} loading={loading} />
          </Card>
          <Pagination
            {...pagination(pageInfo?.total ?? 0, {
              current: pageNum + 1,
              pageSize: 10,
              onChange: (page) => setPageNum(page - 1),
            })}
            {...stylex.props(styles.timelinePagination)}
          />
        </>
      )}

      <BusinessLogDetailModal
        log={selectedLog}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
      />
    </div>
  );
};

export default BusinessLogs;
