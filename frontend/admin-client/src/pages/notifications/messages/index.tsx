import React, { useState, useCallback, useEffect } from 'react';
import { Card, Space, message } from 'antd';
import {
  useNotificationsQuery,
  useNotificationDetail,
  useResendNotification,
} from '../../../services/notification-graphql';
import {
  NotificationFilters,
  NotificationTable,
  NotificationDetailModal,
} from './components';
import type { NotificationRecord, NotificationFiltersState } from './components';

/**
 * 后台消息列表页(F-NTF-006,PRD 13-notification.md)
 * 按类型/接收人/关键字/已读状态/时间范围组合筛选,默认近 30 天;详情弹窗展示全文与状态;
 * 失败/待发送消息可手动重发微信订阅消息(仅超管,PERMISSION_NOTIFICATION_WRITE)。
 * 权限:PERMISSION_NOTIFICATION_READ(管理员/超管,后端强制)。
 */
const NotificationMessages: React.FC = () => {
  const [filters, setFilters] = useState<NotificationFiltersState>({});
  const [pageNum, setPageNum] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [detail, setDetail] = useState<NotificationRecord | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [loadDetail, { data: detailData, loading: detailLoading }] = useNotificationDetail(null);
  const [resendNotification, { loading: resending }] = useResendNotification();

  const { data, loading, error } = useNotificationsQuery({
    messageType: filters.messageType,
    receiverType: filters.receiverType,
    keyword: filters.keyword,
    read: filters.read === 'READ' ? true : filters.read === 'UNREAD' ? false : undefined,
    startAt: filters.range?.[0]?.startOf('day').format('YYYY-MM-DDTHH:mm:ss'),
    endAt: filters.range?.[1]?.endOf('day').format('YYYY-MM-DDTHH:mm:ss'),
    page: pageNum,
    size: pageSize,
  });

  useEffect(() => {
    if (error) {
      message.error(error.message || '消息列表加载失败');
    }
  }, [error]);

  const list: NotificationRecord[] = data?.notifications?.content ?? [];
  const total = data?.notifications?.totalElements ?? 0;

  const handleSearch = () => {
    setPageNum(0);
  };

  const handleReset = () => {
    setFilters({});
    setPageNum(0);
  };

  const openDetail = useCallback(
    (record: NotificationRecord) => {
      setDetail(record);
      setDetailVisible(true);
      loadDetail({ variables: { id: record.id } });
    },
    [loadDetail],
  );

  const detailRecord: NotificationRecord | null = detailData?.notification ?? detail;

  /**
   * 手动重发失败/待发送的微信订阅消息(F-NTF-006,仅超管)
   */
  const handleResend = async (record: NotificationRecord) => {
    try {
      const result = await resendNotification({ variables: { id: record.id } });
      if (result.errors?.length) {
        message.error(result.errors[0].message || '重发失败');
        return;
      }
      message.success('已重新发送,请在通道状态确认结果');
      // 刷新详情与列表(通道状态更新)
      loadDetail({ variables: { id: record.id } });
    } catch (e: any) {
      message.error(e?.message || '重发失败');
    }
  };

  return (
    <Card title='消息列表' extra={<Space>共 {total} 条</Space>}>
      {/* 筛选区 */}
      <NotificationFilters
        filters={filters}
        onChange={(partial) => setFilters((f) => ({ ...f, ...partial }))}
        onSearch={handleSearch}
        onReset={handleReset}
      />

      <NotificationTable
        list={list}
        loading={loading}
        total={total}
        pageNum={pageNum}
        pageSize={pageSize}
        onPageChange={(nextPage, nextSize) => {
          setPageNum(nextPage);
          setPageSize(nextSize);
        }}
        onViewDetail={openDetail}
      />

      {/* 详情弹窗 */}
      <NotificationDetailModal
        record={detailRecord}
        loading={detailLoading}
        open={detailVisible}
        resending={resending}
        onClose={() => setDetailVisible(false)}
        onResend={handleResend}
      />
    </Card>
  );
};

export default NotificationMessages;
