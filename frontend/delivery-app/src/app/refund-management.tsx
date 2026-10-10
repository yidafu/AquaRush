import { useCallback, useEffect, useRef, useState } from 'react';
import { FlatList, Pressable, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { useRouter } from 'expo-router';
import { toast } from 'sonner-native';
import { RefundAction, RefundStatus } from '@aquarush/common';
import type { RefundRequestVo } from '@aquarush/common';

import { EmptyState } from '@/components/empty-state';
import { ListFooterStatus } from '@/components/list-footer-status';
import { NoteActionSheet } from '@/components/manage/note-action-sheet';
import { RefundRequestCard } from '@/components/manage/refund-request-card';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Radius, Spacing } from '@/constants/theme';
import { useAuth } from '@/hooks/use-auth';
import { useTheme } from '@/hooks/use-theme';
import { getRefundRequests, processRefund } from '@/services/refund';

const PAGE_SIZE = 10;

const STATUS_OPTIONS: { label: string; value?: RefundStatus }[] = [
  { label: '全部' },
  { label: '待审核', value: RefundStatus.PENDING },
  { label: '已通过', value: RefundStatus.APPROVED },
  { label: '已拒绝', value: RefundStatus.REJECTED },
  { label: '已完成', value: RefundStatus.COMPLETED },
  { label: '失败', value: RefundStatus.FAILED },
];

/** 退单处置页(R-PAY-02):退款申请分页 + 通过/驳回/重试审核。 */
export default function RefundManagementScreen() {
  const router = useRouter();
  const theme = useTheme();
  const { isAdmin } = useAuth();
  const [requests, setRequests] = useState<RefundRequestVo[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusFilter, setStatusFilter] = useState<RefundStatus | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [reviewTarget, setReviewTarget] = useState<RefundRequestVo | null>(null);
  const [reviewAction, setReviewAction] = useState<RefundAction>(RefundAction.APPROVE);
  const [submitting, setSubmitting] = useState(false);
  const loadingRef = useRef(false);

  useEffect(() => {
    if (!isAdmin) router.replace('/');
  }, [isAdmin, router]);

  const loadPage = useCallback(
    async (targetPage: number, filter: RefundStatus | undefined, append: boolean) => {
      if (loadingRef.current) return;
      loadingRef.current = true;
      if (append) {
        setLoadingMore(true);
      } else {
        setLoading(true);
      }
      try {
        const res = await getRefundRequests(filter, targetPage, PAGE_SIZE);
        const pageData = res?.refundRequests;
        if (!pageData) return;
        setTotalPages(pageData.totalPages);
        setPage(pageData.number);
        setRequests((prev) => (append ? [...prev, ...pageData.content] : [...pageData.content]));
      } catch (e) {
        console.error('加载退款申请失败:', e);
        toast.error('加载失败');
      } finally {
        loadingRef.current = false;
        if (append) {
          setLoadingMore(false);
        } else {
          setLoading(false);
        }
      }
    },
    [],
  );

  // 首次加载 + 状态筛选变化时回到第一页
  useEffect(() => {
    setRequests([]);
    loadPage(0, statusFilter, false).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  const handleRefresh = useCallback(() => {
    return loadPage(0, statusFilter, false).catch(() => {});
  }, [loadPage, statusFilter]);

  const handleLoadMore = useCallback(() => {
    if (loading || loadingMore || page >= totalPages - 1) return;
    loadPage(page + 1, statusFilter, true).catch(() => {});
  }, [loading, loadingMore, page, totalPages, statusFilter, loadPage]);

  const openReview = useCallback((request: RefundRequestVo, action: RefundAction) => {
    setReviewTarget(request);
    setReviewAction(action);
  }, []);

  const handleReviewSubmit = useCallback(
    async (adminNote: string) => {
      if (!reviewTarget) return;
      setSubmitting(true);
      const toastId = toast.loading(reviewAction === RefundAction.APPROVE ? '审核通过中...' : '驳回中...');
      try {
        await processRefund({
          refundRequestId: reviewTarget.id,
          originalTransactionId: reviewTarget.originalTransactionId,
          orderId: reviewTarget.orderId,
          userId: reviewTarget.userId,
          amountCents: Number(reviewTarget.amountCents),
          reason: reviewTarget.reason,
          action: reviewAction,
          adminNote: adminNote || null,
        });
        toast.success(reviewAction === RefundAction.APPROVE ? '退款审核通过' : '已驳回退款', { id: toastId });
        setReviewTarget(null);
        loadPage(0, statusFilter, false).catch(() => {});
      } catch (e: any) {
        toast.error(e?.message || '审核失败', { id: toastId });
      } finally {
        setSubmitting(false);
      }
    },
    [reviewTarget, reviewAction, statusFilter, loadPage],
  );

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <View style={styles.filterBar}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chips}>
          {STATUS_OPTIONS.map((option) => {
            const active = statusFilter === option.value;
            return (
              <Pressable
                key={option.label}
                onPress={() => setStatusFilter(option.value)}
                style={({ pressed }) => [
                  styles.chip,
                  { borderColor: theme.border, backgroundColor: active ? theme.primaryBg : theme.card },
                  pressed && styles.chipPressed,
                ]}>
                <ThemedText type="small" themeColor={active ? 'primary' : 'textSecondary'}>
                  {option.label}
                </ThemedText>
              </Pressable>
            );
          })}
        </ScrollView>
      </View>

      <FlatList
        data={requests}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <RefundRequestCard
            request={item}
            onApprove={(r) => openReview(r, RefundAction.APPROVE)}
            onReject={(r) => openReview(r, RefundAction.REJECT)}
          />
        )}
        contentContainerStyle={styles.listContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}
        onEndReached={handleLoadMore}
        onEndReachedThreshold={0.3}
        ListEmptyComponent={
          loading ? null : (
            <EmptyState icon="↩️" title="暂无退款申请" description="下拉可刷新" />
          )
        }
        ListFooterComponent={
          <ListFooterStatus
            loading={loading || loadingMore}
            noMore={page >= totalPages - 1}
            hasItems={requests.length > 0}
          />
        }
      />

      {reviewTarget ? (
        <NoteActionSheet
          visible
          title={reviewAction === RefundAction.APPROVE ? (reviewTarget.status === 'FAILED' ? '重试退款' : '审核通过退款') : '驳回退款'}
          description={`订单 #${reviewTarget.orderId} · 金额 ¥${(Number(reviewTarget.amountCents) / 100).toFixed(2)}\n原因: ${reviewTarget.reason}`}
          placeholder={reviewAction === 'REJECT' ? '请输入驳回原因(必填)' : '请输入审核备注(可选)'}
          confirmText={reviewAction === RefundAction.APPROVE ? '确认通过' : '确认驳回'}
          required={reviewAction === 'REJECT'}
          submitting={submitting}
          onConfirm={handleReviewSubmit}
          onClose={() => setReviewTarget(null)}
        />
      ) : null}
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  filterBar: {
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.three,
  },
  chips: {
    gap: Spacing.two,
  },
  chip: {
    borderRadius: Radius.pill,
    borderWidth: 1,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.one,
  },
  chipPressed: {
    opacity: 0.7,
  },
  listContent: {
    paddingHorizontal: Spacing.three,
    paddingBottom: Spacing.six,
    gap: Spacing.three,
    flexGrow: 1,
  },
});
