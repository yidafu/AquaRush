import React, { useState, useCallback } from 'react';
import { Button, message } from 'antd';
import type { TableProps } from 'antd';
import { useQuery, useMutation } from '@apollo/client';
import { SUBSCRIPTIONS_PAGE_DATA_QUERY } from '../../graphql/queries/subscription.graphql';
import {
  CREATE_SUBSCRIPTION_MUTATION,
  PAUSE_SUBSCRIPTION_MUTATION,
  RESUME_SUBSCRIPTION_MUTATION,
  TERMINATE_SUBSCRIPTION_MUTATION,
} from '../../graphql/mutations/subscription.graphql';
import { SubscriptionFilters, SubscriptionFilters as FilterType } from './components/SubscriptionFilters';
import { SubscriptionTable, CreateSubscriptionModal } from './components';
import type { CreateSubscriptionPayload } from './components/CreateSubscriptionModal';
import type { SubscriptionsQueryResult, SubscriptionData } from './types';
import { pagination as createPagination } from '@/utils/table';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  page: {
    paddingTop: '24px',
    paddingRight: '24px',
    paddingBottom: '24px',
    paddingLeft: '24px',
  },
  actionBar: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginBottom: '16px',
  },
});

/** 优先取后端 graphQLError message(表单校验/业务异常),回退 Apollo 错误文案 */
const toErrorMessage = (error: unknown, fallback: string): string => {
  const err = error as { graphQLErrors?: { message?: string }[]; message?: string };
  return err?.graphQLErrors?.[0]?.message || err?.message || fallback;
};

/**
 * 周期订购计划列表页(F-ORD-026/G77,设计稿 spec/design/admin/02-order/subscriptions.html):
 * 筛选(用户关键字/状态/下次配送区间)+ 分页表格 + 新建/暂停/恢复/终止。
 * 单页单请求:计划分页 + 送水工下拉一次取回(规则 #10);用户搜索/地址簿/商品为
 * 新建弹窗内按需懒加载(与营销弹窗同模式)。
 */
const Subscriptions: React.FC = () => {
  const [filters, setFilters] = useState<FilterType>({
    search: '',
    status: undefined,
    nextRunFrom: undefined,
    nextRunTo: undefined,
  });

  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
  });

  const [createOpen, setCreateOpen] = useState(false);
  const [actingId, setActingId] = useState<string | null>(null);

  const { data, loading, refetch } = useQuery<SubscriptionsQueryResult>(SUBSCRIPTIONS_PAGE_DATA_QUERY, {
    variables: {
      input: {
        page: pagination.current - 1,
        size: pagination.pageSize,
        search: filters.search || undefined,
        status: filters.status || undefined,
        nextRunFrom: filters.nextRunFrom || undefined,
        nextRunTo: filters.nextRunTo || undefined,
      },
    },
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
    onError: (error) => {
      console.error('Failed to load subscriptions:', error);
      message.error('加载周期订购计划失败: ' + toErrorMessage(error, error.message));
    },
  });

  const fetchSubscriptions = useCallback(() => {
    refetch();
  }, [refetch]);

  const [createSubscription, { loading: creating }] = useMutation(CREATE_SUBSCRIPTION_MUTATION, {
    onError: (error) => {
      message.error(toErrorMessage(error, '创建计划失败'));
    },
  });
  const [pauseSubscription] = useMutation(PAUSE_SUBSCRIPTION_MUTATION, {
    onError: (error) => message.error(toErrorMessage(error, '暂停计划失败')),
  });
  const [resumeSubscription] = useMutation(RESUME_SUBSCRIPTION_MUTATION, {
    onError: (error) => message.error(toErrorMessage(error, '恢复计划失败')),
  });
  const [terminateSubscription] = useMutation(TERMINATE_SUBSCRIPTION_MUTATION, {
    onError: (error) => message.error(toErrorMessage(error, '终止计划失败')),
  });

  // total 直接由查询结果派生(渲染期读取,避免 effect 同步 setState)
  const total = data?.subscriptions?.totalElements ?? 0;

  const handleFiltersChange = (newFilters: FilterType) => {
    setFilters(newFilters);
    setPagination((prev) => ({ ...prev, current: 1 }));
  };

  const handleTableChange: TableProps<SubscriptionData>['onChange'] = (newPagination) => {
    setPagination((prev) => ({
      ...prev,
      current: newPagination.current ?? 1,
      pageSize: newPagination.pageSize ?? 20,
    }));
  };

  // 暂停/恢复/终止共用执行器:mutation 成功后刷新列表;失败提示由各 mutation onError 负责
  const runStatusAction = async (
    action: (args: { variables: { subscriptionId: string } }) => Promise<unknown>,
    subscriptionId: string,
    successText: string,
  ) => {
    setActingId(subscriptionId);
    try {
      await action({ variables: { subscriptionId } });
      message.success(successText);
      fetchSubscriptions();
    } catch {
      // onError 已提示
    } finally {
      setActingId(null);
    }
  };

  const handlePause = (record: SubscriptionData) =>
    runStatusAction(pauseSubscription, record.id, '计划已暂停');

  const handleResume = (record: SubscriptionData) =>
    runStatusAction(resumeSubscription, record.id, '计划已恢复');

  const handleTerminate = (record: SubscriptionData) =>
    runStatusAction(terminateSubscription, record.id, '计划已终止');

  const handleCreate = async (payload: CreateSubscriptionPayload) => {
    try {
      await createSubscription({ variables: { input: payload } });
      message.success('周期订购计划已创建');
      setCreateOpen(false);
      fetchSubscriptions();
    } catch {
      // onError 已提示
    }
  };

  const workers = data?.deliveryWorkers?.content ?? [];

  return (
    <div {...stylex.props(styles.page)}>
      <SubscriptionFilters filters={filters} onFiltersChange={handleFiltersChange} />
      <div {...stylex.props(styles.actionBar)}>
        <Button type="primary" onClick={() => setCreateOpen(true)}>
          新建计划
        </Button>
      </div>
      <SubscriptionTable
        dataSource={data?.subscriptions?.content || []}
        loading={loading}
        pagination={createPagination(total, {
          current: pagination.current,
          pageSize: pagination.pageSize,
          showSizeChanger: true,
          showQuickJumper: true,
        })}
        actionLoading={actingId != null}
        onPause={handlePause}
        onResume={handleResume}
        onTerminate={handleTerminate}
        onChange={handleTableChange}
      />

      <CreateSubscriptionModal
        open={createOpen}
        workers={workers}
        submitting={creating}
        onSubmit={handleCreate}
        onCancel={() => setCreateOpen(false)}
      />
    </div>
  );
};

export default Subscriptions;
