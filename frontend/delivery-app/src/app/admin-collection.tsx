import { useCallback, useEffect, useRef, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, View } from 'react-native';
import { useRouter } from 'expo-router';
import { toast } from 'sonner-native';
import { Money } from '@aquarush/common';
import type {
  DailyCollectionVoPage,
  DailyCollectionRecordVo,
  DailyCollectionSummaryVo,
  DailyReconciliationVo,
  DailyReconciliationVoPage,
} from '@aquarush/common';

import { EmptyState } from '@/components/empty-state';
import { ListFooterStatus } from '@/components/list-footer-status';
import { DailyCollectionCard } from '@/components/manage/daily-collection-card';
import { NoteActionSheet } from '@/components/manage/note-action-sheet';
import { ReconciliationCard } from '@/components/reconciliation-card';
import { ReconciliationOrderDetailSheet } from '@/components/reconciliation-order-detail-sheet';
import { Button } from '@/components/button';
import { SegmentedTabs } from '@/components/segmented-tabs';
import { StatCard } from '@/components/stat-card';
import { StatCardGrid } from '@/components/stat-card-grid';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/hooks/use-auth';
import {
  confirmDailyCollection,
  executeDailyReconciliation,
  getAdminCollectionPage,
  rejectDailyCollection,
  reviewReconciliation,
} from '@/services/dailyCollection';

const PAGE_SIZE = 10;
const TABS = ['每日收款记录', '对账记录'];

interface AdminCollectionData {
  summary: DailyCollectionSummaryVo | null;
  collections: DailyCollectionVoPage | null;
  reconciliations: DailyReconciliationVoPage | null;
}

type ActionTarget =
  | { kind: 'confirm'; record: DailyCollectionRecordVo }
  | { kind: 'reject'; record: DailyCollectionRecordVo }
  | { kind: 'review'; record: DailyReconciliationVo };

/** 收银对账处置页(F-COL-010/011):日报确认/驳回 + 对账复核/执行 + 逐单明细下钻。 */
export default function AdminCollectionScreen() {
  const router = useRouter();
  const { isAdmin } = useAuth();
  const [currentTab, setCurrentTab] = useState(0);
  const [data, setData] = useState<AdminCollectionData>({ summary: null, collections: null, reconciliations: null });
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [collectionsPage, setCollectionsPage] = useState(0);
  const [reconciliationsPage, setReconciliationsPage] = useState(0);
  const [actionTarget, setActionTarget] = useState<ActionTarget | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [detailDate, setDetailDate] = useState<string | null>(null);
  const loadingRef = useRef(false);

  useEffect(() => {
    if (!isAdmin) router.replace('/');
  }, [isAdmin, router]);

  const loadPage = useCallback(
    async (nextCollectionsPage: number, nextReconciliationsPage: number, append: boolean) => {
      if (loadingRef.current) return;
      loadingRef.current = true;
      if (append) {
        setLoadingMore(true);
      } else {
        setLoading(true);
      }
      try {
        const res = await getAdminCollectionPage(
          nextCollectionsPage,
          nextReconciliationsPage,
          PAGE_SIZE,
        );
        setData({
          summary: res?.dailyCollectionSummary ?? null,
          collections: res?.dailyCollections ?? null,
          reconciliations: res?.dailyReconciliations ?? null,
        });
        setCollectionsPage(nextCollectionsPage);
        setReconciliationsPage(nextReconciliationsPage);
      } catch (e) {
        console.error('加载收银对账数据失败:', e);
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

  // 首屏加载
  useEffect(() => {
    loadPage(0, 0, false).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleRefresh = useCallback(() => {
    return loadPage(0, 0, false).catch(() => {});
  }, [loadPage]);

  const handleLoadMore = useCallback(() => {
    if (loading || loadingMore) return;
    if (currentTab === 0) {
      const info = data.collections?.pageInfo;
      if (!info?.hasNext) return;
      loadPage(collectionsPage + 1, reconciliationsPage, true).catch(() => {});
    } else {
      const info = data.reconciliations?.pageInfo;
      if (!info?.hasNext) return;
      loadPage(collectionsPage, reconciliationsPage + 1, true).catch(() => {});
    }
  }, [loading, loadingMore, currentTab, data, collectionsPage, reconciliationsPage, loadPage]);

  const refreshAfterMutation = useCallback(() => {
    loadPage(0, 0, false).catch(() => {});
  }, [loadPage]);

  const handleConfirmSubmit = useCallback(
    async (note: string) => {
      if (actionTarget?.kind !== 'confirm') return;
      setSubmitting(true);
      const toastId = toast.loading('确认中...');
      try {
        await confirmDailyCollection({ recordId: actionTarget.record.id, notes: note || null });
        toast.success('确认成功', { id: toastId });
        setActionTarget(null);
        refreshAfterMutation();
      } catch (e: any) {
        toast.error(e?.message || '确认失败', { id: toastId });
      } finally {
        setSubmitting(false);
      }
    },
    [actionTarget, refreshAfterMutation],
  );

  const handleRejectSubmit = useCallback(
    async (reason: string) => {
      if (actionTarget?.kind !== 'reject') return;
      setSubmitting(true);
      const toastId = toast.loading('驳回中...');
      try {
        await rejectDailyCollection({ recordId: actionTarget.record.id, reason });
        toast.success('已驳回', { id: toastId });
        setActionTarget(null);
        refreshAfterMutation();
      } catch (e: any) {
        toast.error(e?.message || '驳回失败', { id: toastId });
      } finally {
        setSubmitting(false);
      }
    },
    [actionTarget, refreshAfterMutation],
  );

  const handleReviewSubmit = useCallback(
    async (reviewNotes: string) => {
      if (actionTarget?.kind !== 'review') return;
      setSubmitting(true);
      const toastId = toast.loading('复核中...');
      try {
        await reviewReconciliation({ reconciliationId: actionTarget.record.id, reviewNotes });
        toast.success('复核成功', { id: toastId });
        setActionTarget(null);
        refreshAfterMutation();
      } catch (e: any) {
        toast.error(e?.message || '复核失败', { id: toastId });
      } finally {
        setSubmitting(false);
      }
    },
    [actionTarget, refreshAfterMutation],
  );

  const handleExecute = useCallback(() => {
    toast.warning('手动执行对账', {
      description: '确定要执行对账吗？重复执行会更新已有数据',
      duration: Infinity,
      id: 'execute-reconciliation',
      action: {
        label: '执行',
        onClick: async () => {
          toast.dismiss('execute-reconciliation');
          const toastId = toast.loading('对账执行中...');
          try {
            await executeDailyReconciliation();
            toast.success('对账执行成功', { id: toastId });
            refreshAfterMutation();
          } catch (e: any) {
            toast.error(e?.message || '对账执行失败', { id: toastId });
          }
        },
      },
      cancel: {
        label: '取消',
        onClick: () => toast.dismiss('execute-reconciliation'),
      },
    });
  }, [refreshAfterMutation]);

  const summary = data.summary;
  const collections = data.collections?.list ?? [];
  const reconciliations = data.reconciliations?.list ?? [];

  const actionSheetProps = (() => {
    if (!actionTarget) return null;
    if (actionTarget.kind === 'confirm') {
      return {
        title: '确认收款',
        description: `确认送水员 #${actionTarget.record.deliveryWorkerId} ${actionTarget.record.collectionDate} 的收款记录吗？`,
        placeholder: '请输入备注(可选)',
        confirmText: '确认',
        required: false,
      };
    }
    if (actionTarget.kind === 'reject') {
      return {
        title: '驳回收款',
        description: `驳回后该日报退回草稿，送水员可修改后重新提交。\n送水员 #${actionTarget.record.deliveryWorkerId} ${actionTarget.record.collectionDate}`,
        placeholder: '请输入驳回原因(必填)',
        confirmText: '确认驳回',
        required: true,
      };
    }
    return {
      title: '复核对账',
      description: `${actionTarget.record.reconciliationDate} 对账记录`,
      placeholder: '请输入复核备注(必填)',
      confirmText: '确认复核',
      required: true,
    };
  })();

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <View style={styles.header}>
        <StatCardGrid>
          <StatCard
            label="现金收款"
            value={<ThemedText type="subtitle"><Money cents={summary?.totalCashAmountCents ?? 0} /></ThemedText>}
          />
          <StatCard
            label="扫码收款"
            value={<ThemedText type="subtitle"><Money cents={summary?.totalQrCodeAmountCents ?? 0} /></ThemedText>}
          />
          <StatCard
            label="水票"
            value={<ThemedText type="subtitle">{summary?.totalWaterTicketCount ?? 0} 张</ThemedText>}
          />
          <StatCard
            label="订单金额"
            value={<ThemedText type="subtitle"><Money cents={summary?.totalOrderAmountCents ?? 0} /></ThemedText>}
          />
        </StatCardGrid>
        <Button title="手动执行对账" size="sm" variant="ghost" onPress={handleExecute} />
      </View>

      <SegmentedTabs
        current={currentTab}
        tabs={TABS.map((t) => ({ title: t }))}
        onChange={setCurrentTab}
      />

      <FlatList<DailyCollectionRecordVo | DailyReconciliationVo>
        data={currentTab === 0 ? (collections as (DailyCollectionRecordVo | DailyReconciliationVo)[]) : (reconciliations as (DailyCollectionRecordVo | DailyReconciliationVo)[])}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) =>
          currentTab === 0 ? (
            <DailyCollectionCard
              record={item as DailyCollectionRecordVo}
              onConfirm={(r) => setActionTarget({ kind: 'confirm', record: r })}
              onReject={(r) => setActionTarget({ kind: 'reject', record: r })}
            />
          ) : (
            <View style={styles.reconItem}>
              <Pressable onPress={() => setDetailDate((item as DailyReconciliationVo).reconciliationDate)}>
                <ReconciliationCard record={item as DailyReconciliationVo} />
              </Pressable>
              {(item as DailyReconciliationVo).status !== 'REVIEWED' ? (
                <Button
                  title="复核"
                  size="sm"
                  style={styles.reviewButton}
                  onPress={() => setActionTarget({ kind: 'review', record: item as DailyReconciliationVo })}
                />
              ) : null}
            </View>
          )
        }
        contentContainerStyle={styles.listContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}
        onEndReached={handleLoadMore}
        onEndReachedThreshold={0.3}
        ListEmptyComponent={loading ? null : <EmptyState icon="📋" title={currentTab === 0 ? '暂无收款记录' : '暂无对账记录'} description="下拉可刷新" />}
        ListFooterComponent={
          <ListFooterStatus
            loading={loading || loadingMore}
            noMore={
              currentTab === 0
                ? !(data.collections?.pageInfo.hasNext ?? false)
                : !(data.reconciliations?.pageInfo.hasNext ?? false)
            }
            hasItems={(currentTab === 0 ? collections : reconciliations).length > 0}
          />
        }
      />

      {actionSheetProps && actionTarget ? (
        <NoteActionSheet
          visible
          title={actionSheetProps.title}
          description={actionSheetProps.description}
          placeholder={actionSheetProps.placeholder}
          confirmText={actionSheetProps.confirmText}
          required={actionSheetProps.required}
          submitting={submitting}
          onConfirm={
            actionTarget.kind === 'confirm'
              ? handleConfirmSubmit
              : actionTarget.kind === 'reject'
                ? handleRejectSubmit
                : handleReviewSubmit
          }
          onClose={() => setActionTarget(null)}
        />
      ) : null}

      <ReconciliationOrderDetailSheet
        visible={detailDate !== null}
        reconciliationDate={detailDate}
        onClose={() => setDetailDate(null)}
      />
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    gap: Spacing.three,
    padding: Spacing.three,
  },
  header: {
    gap: Spacing.two,
  },
  listContent: {
    gap: Spacing.three,
    paddingBottom: Spacing.six,
    flexGrow: 1,
  },
  reconItem: {
    gap: Spacing.two,
  },
  reviewButton: {
    alignSelf: 'flex-end',
    minWidth: 88,
  },
});
