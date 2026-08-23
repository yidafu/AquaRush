import { useCallback, useEffect, useState } from 'react';
import { Alert, StyleSheet, View } from 'react-native';
import {
  DailyCollectionStatus,
  Money,
  centsToYuan,
  normalizeCents,
  yuanToCents,
} from '@aquarush/common';
import type { MyTodayCollectionVo } from '@aquarush/common';

import { Button } from '@/components/button';
import { Card } from '@/components/card';
import { CollectionInputFields } from '@/components/daily-collection/collection-input-fields';
import { DiffConfirmSheet } from '@/components/daily-collection/diff-confirm-sheet';
import { OrderInfoRow } from '@/components/order-info-row';
import { StatusBadge } from '@/components/status-badge';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import {
  TOLERANCE_CENTS,
  DAILY_COLLECTION_STATUS_MAP,
  DAILY_COLLECTION_STATUS_TONE,
} from '@/constants/collection';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { getTodayCollection, submitDailyCollection } from '@/services/dailyCollection';
import { todayDateString } from '@/utils/date';

export default function DailyCollectionScreen() {
  const theme = useTheme();
  const [cashAmount, setCashAmount] = useState('');
  const [waterTicketCount, setWaterTicketCount] = useState('0');
  const [qrCodeAmount, setQrCodeAmount] = useState('');
  const [orderStats, setOrderStats] = useState<MyTodayCollectionVo | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // 确认弹窗相关状态
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [pendingCashDiff, setPendingCashDiff] = useState(0);
  const [pendingWaterTicketDiff, setPendingWaterTicketDiff] = useState(0);
  const [pendingQrCodeDiff, setPendingQrCodeDiff] = useState(0);
  const [notes, setNotes] = useState('');

  const loadTodayRecord = useCallback(async () => {
    try {
      const res = await getTodayCollection();
      if (res?.myTodayCollection) {
        setOrderStats(res.myTodayCollection);
        // 回填表单（如果有已填写的数据）
        if (res.myTodayCollection.cashAmountCents != null) {
          setCashAmount(String(centsToYuan(normalizeCents(res.myTodayCollection.cashAmountCents) ?? 0)));
          setWaterTicketCount(String(res.myTodayCollection.waterTicketCount ?? 0));
          setQrCodeAmount(String(centsToYuan(normalizeCents(res.myTodayCollection.qrCodeAmountCents) ?? 0)));
        }
      }
    } catch (e) {
      console.error('加载收款记录失败:', e);
    }
  }, []);

  useEffect(() => {
    loadTodayRecord().catch(() => {});
  }, [loadTodayRecord]);

  const doSubmit = useCallback(
    async (submitNotes: string) => {
      const cash = parseFloat(cashAmount) || 0;
      const qr = parseFloat(qrCodeAmount) || 0;
      const tickets = parseInt(waterTicketCount, 10) || 0;

      setSubmitting(true);
      try {
        const input = {
          cashAmountCents: yuanToCents(cash),
          waterTicketCount: tickets,
          qrCodeAmountCents: yuanToCents(qr),
          notes: submitNotes || undefined,
        };
        const res = await submitDailyCollection(input);
        if (res?.submitDailyCollection) {
          // 以服务端落库状态回显(G28:超容差提交后为「有争议」DISPUTED,不再强置「已提交」)
          setOrderStats((prev) =>
            prev
              ? {
                  ...prev,
                  cashAmountCents: input.cashAmountCents,
                  waterTicketCount: input.waterTicketCount,
                  qrCodeAmountCents: input.qrCodeAmountCents,
                  status: res.submitDailyCollection?.status ?? DailyCollectionStatus.SUBMITTED,
                }
              : prev,
          );
          Alert.alert('提交成功');
        }
      } catch (e) {
        console.error('提交收款失败:', e);
        Alert.alert('提交失败');
      } finally {
        setSubmitting(false);
        setShowConfirmModal(false);
        setNotes('');
      }
    },
    [cashAmount, waterTicketCount, qrCodeAmount],
  );

  const handleSubmit = useCallback(async () => {
    const cash = parseFloat(cashAmount) || 0;
    const qr = parseFloat(qrCodeAmount) || 0;
    const tickets = parseInt(waterTicketCount, 10) || 0;

    if (cash === 0 && tickets === 0 && qr === 0) {
      Alert.alert('请填写收款金额');
      return;
    }

    // 分别计算三个分项差异，任一超容差需二次确认（weapp 行为）
    if (orderStats) {
      const cashDiff = yuanToCents(cash) - (normalizeCents(orderStats.calculatedCashAmountCents) ?? 0);
      const waterTicketDiff = tickets - (orderStats.calculatedWaterTicketCount ?? 0);
      const qrCodeDiff = yuanToCents(qr) - (normalizeCents(orderStats.calculatedQrCodeAmountCents) ?? 0);

      if (
        Math.abs(cashDiff) > TOLERANCE_CENTS ||
        waterTicketDiff !== 0 ||
        Math.abs(qrCodeDiff) > TOLERANCE_CENTS
      ) {
        setPendingCashDiff(cashDiff);
        setPendingWaterTicketDiff(waterTicketDiff);
        setPendingQrCodeDiff(qrCodeDiff);
        setShowConfirmModal(true);
        return;
      }
    }

    await doSubmit('');
  }, [cashAmount, waterTicketCount, qrCodeAmount, orderStats, doSubmit]);

  const handleConfirm = useCallback(() => {
    if (!notes.trim()) {
      Alert.alert('请输入情况说明');
      return;
    }
    doSubmit(notes.trim()).catch(() => {});
  }, [notes, doSubmit]);

  const handleCancelConfirm = useCallback(() => {
    setShowConfirmModal(false);
    setNotes('');
    setPendingCashDiff(0);
    setPendingWaterTicketDiff(0);
    setPendingQrCodeDiff(0);
  }, []);

  const totalCents =
    yuanToCents(parseFloat(cashAmount) || 0) + yuanToCents(parseFloat(qrCodeAmount) || 0);
  const status = orderStats?.status;

  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <View style={styles.content}>
        <View style={[styles.notice, { backgroundColor: theme.warningBg }]}>
          <ThemedText type="small" themeColor="textSecondary">
            1. 现金和扫码收款将自动与今日完成订单金额对比
          </ThemedText>
          <ThemedText type="small" themeColor="textSecondary">
            2. 差异在1元以内视为正常
          </ThemedText>
          <ThemedText type="small" themeColor="textSecondary">
            3. 提交后可由管理员确认审核
          </ThemedText>
        </View>

        <View style={styles.sectionHeader}>
          <ThemedText type="smallBold">收款日期</ThemedText>
          <ThemedText type="small" themeColor="textSecondary">
            {todayDateString()}
          </ThemedText>
        </View>

        {status ? (
          <Card title="当前状态">
            <View style={styles.statusRow}>
              <ThemedText type="small" themeColor="textSecondary">
                状态：
              </ThemedText>
              {DAILY_COLLECTION_STATUS_TONE[status] ? (
                <StatusBadge
                  status={status}
                  label={DAILY_COLLECTION_STATUS_MAP[status] ?? status}
                  tone={DAILY_COLLECTION_STATUS_TONE[status]}
                />
              ) : (
                <ThemedText type="smallBold" themeColor="textTertiary">
                  {DAILY_COLLECTION_STATUS_MAP[status] ?? status}
                </ThemedText>
              )}
            </View>
          </Card>
        ) : null}

        <Card title="今日收款录入">
          <ThemedText type="smallBold" style={styles.orderInfoTitle}>
            今日订单情况
          </ThemedText>
          <OrderInfoRow label="订单总数" value={`${orderStats?.orderCount ?? 0} 单`} />
          <OrderInfoRow
            label="现金应收"
            value={<Money cents={orderStats?.calculatedCashAmountCents ?? 0} />}
          />
          <OrderInfoRow
            label="扫码应收"
            value={<Money cents={orderStats?.calculatedQrCodeAmountCents ?? 0} />}
          />
          <OrderInfoRow
            label="水票应收"
            value={`${orderStats?.calculatedWaterTicketCount ?? 0} 张`}
          />

          {/* 收款录入表单(现金/水票/扫码 + 合计) */}
          <CollectionInputFields
            cashAmount={cashAmount}
            waterTicketCount={waterTicketCount}
            qrCodeAmount={qrCodeAmount}
            totalCents={totalCents}
            onCashAmountChange={setCashAmount}
            onWaterTicketCountChange={setWaterTicketCount}
            onQrCodeAmountChange={setQrCodeAmount}
          />
        </Card>

        <Button
          title={submitting ? '提交中...' : '提交收款'}
          loading={submitting}
          size="lg"
          onPress={handleSubmit}
        />
      </View>

      {/* 金额差异确认(任一子项超容差时二次确认) */}
      <DiffConfirmSheet
        visible={showConfirmModal}
        cashDiffCents={pendingCashDiff}
        waterTicketDiff={pendingWaterTicketDiff}
        qrCodeDiffCents={pendingQrCodeDiff}
        notes={notes}
        onNotesChange={setNotes}
        onConfirm={handleConfirm}
        onCancel={handleCancelConfirm}
      />
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    flex: 1,
    padding: Spacing.three,
    gap: Spacing.three,
  },
  notice: {
    borderRadius: Radius.md,
    padding: Spacing.three,
    gap: Spacing.one,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  orderInfoTitle: {
    marginBottom: Spacing.two,
  },
});
