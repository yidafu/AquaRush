import React, { useEffect, useState } from 'react';
import { Modal, Row, Col, Statistic, Tag, Form, Input, Button, Table, Empty } from '@aquarush/admin-ui';
import { centsToYuan, displayCents } from '@aquarush/common';
import { useQuery } from '@apollo/client';
import type { ColumnsType } from '@aquarush/admin-ui';
import { GET_RECONCILIATION_ORDER_DETAILS } from '../../../graphql/queries/daily-collection.graphql';
import type {
  DailyReconciliation,
  ReconciliationOrderDetail,
  ReconciliationOrderDetailVoPage,
} from '../../../types/daily-collection';
import { PAYMENT_TYPE_TEXT } from '../../../types/daily-collection';
import { reconciliationStatusColorMap, reconciliationStatusTextMap } from './constants';
import { getOrderStatusConfig } from '../../../constants/order';
import { getToneColor } from '../../../constants/status';
import { COLOR_ERROR, COLOR_SUCCESS } from '../../../config/theme';
import * as stylex from '@stylexjs/stylex';

// 局部色值 #666 为原有辅助文字灰,共享 token 无对应值,保持原样(报告 (a))
const styles = stylex.create({
  blockSpacing: {
    marginTop: '16px',
  },
  detailBlock: {
    marginTop: '24px',
  },
  detailTitle: {
    marginBottom: '8px',
  },
  reviewed: {
    marginTop: '16px',
    color: '#666',
  },
});

export interface ReconciliationDetailModalProps {
  open: boolean;
  record: DailyReconciliation | null;
  onClose: () => void;
  /** 确认复核(提交备注) */
  onReview: (notes: string) => void;
}

const DETAIL_PAGE_SIZE = 10;

/** 对账详情弹窗(daily-collection/index.tsx 拆分)
 *  G29:含对账逐单差异明细 —— 按对账日期分页取回当日已送达订单流水(服务端分页,规则 #13)
 */
const ReconciliationDetailModal: React.FC<ReconciliationDetailModalProps> = ({
  open,
  record,
  onClose,
  onReview,
}) => {
  const [reviewForm] = Form.useForm();
  const [detailPage, setDetailPage] = useState(0);

  // 切换记录时重置复核表单与逐单明细页码
  useEffect(() => {
    if (record) {
      reviewForm.resetFields();
      setDetailPage(0);
    }
  }, [record, reviewForm]);

  const { data: detailData, loading: detailLoading } = useQuery<{
    dailyReconciliationOrderDetails: ReconciliationOrderDetailVoPage;
  }>(GET_RECONCILIATION_ORDER_DETAILS, {
    variables: {
      reconciliationDate: record?.reconciliationDate ?? '',
      deliveryWorkerId: null,
      page: detailPage,
      size: DETAIL_PAGE_SIZE,
    },
    skip: !open || !record,
    fetchPolicy: 'cache-and-network',
  });

  const orderDetails = detailData?.dailyReconciliationOrderDetails?.list ?? [];
  const detailTotal = detailData?.dailyReconciliationOrderDetails?.pageInfo?.total ?? 0;

  const handleReviewSubmit = async () => {
    const values = await reviewForm.validateFields();
    onReview(values.reviewNotes);
  };

  const detailColumns: ColumnsType<ReconciliationOrderDetail> = [
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
    },
    {
      title: '订单金额',
      dataIndex: 'amountCents',
      key: 'amountCents',
      width: 120,
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '收款方式',
      dataIndex: 'paymentType',
      key: 'paymentType',
      width: 100,
      render: (type?: string) => (type ? PAYMENT_TYPE_TEXT[type] ?? type : '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status: string) => {
        const config = getOrderStatusConfig(status);
        return <Tag color={getToneColor(config.tone)}>{config.text}</Tag>;
      },
    },
    {
      title: '送达时间',
      dataIndex: 'deliveredAt',
      key: 'deliveredAt',
      width: 160,
      render: (v?: string) => (v ? new Date(v).toLocaleString('zh-CN') : '-'),
    },
  ];

  return (
    <Modal
      title="对账详情"
      open={open}
      onCancel={onClose}
      width={760}
      footer={[
        <Button key="close" onClick={onClose}>
          关闭
        </Button>,
        record?.status !== 'REVIEWED' && (
          <Button key="review" type="primary" onClick={handleReviewSubmit}>
            确认复核
          </Button>
        ),
      ]}
    >
      {record && (
        <div>
          <Row gutter={16}>
            <Col span={12}>
              <Statistic title="订单总数" value={record.totalOrderCount} />
            </Col>
            <Col span={12}>
              <Statistic
                title="订单金额"
                value={centsToYuan(record.totalOrderAmountCents)}
                precision={2}
                prefix="¥"
              />
            </Col>
          </Row>
          <Row gutter={16} {...stylex.props(styles.blockSpacing)}>
            <Col span={12}>
              <Statistic
                title="现金收款"
                value={centsToYuan(record.totalCashAmountCents)}
                precision={2}
                prefix="¥"
              />
            </Col>
            <Col span={12}>
              <Statistic
                title="扫码收款"
                value={centsToYuan(record.totalQrCodeAmountCents)}
                precision={2}
                prefix="¥"
              />
            </Col>
          </Row>
          <Row gutter={16} {...stylex.props(styles.blockSpacing)}>
            <Col span={12}>
              <Statistic
                title="差异金额"
                value={centsToYuan(record.discrepancyAmountCents)}
                precision={2}
                prefix="¥"
                valueStyle={{ color: Math.abs(record.discrepancyAmountCents) <= 100 ? COLOR_SUCCESS : COLOR_ERROR }}
              />
            </Col>
            <Col span={12}>
              <Tag color={reconciliationStatusColorMap[record.status]}>
                {reconciliationStatusTextMap[record.status]}
              </Tag>
            </Col>
          </Row>

          {/* G29 对账逐单差异明细:当日已送达订单流水,与日报系统计算值同源 */}
          <div {...stylex.props(styles.detailBlock)}>
            <h4 {...stylex.props(styles.detailTitle)}>对账逐单明细(当日已送达订单)</h4>
            <Table<ReconciliationOrderDetail>
              columns={detailColumns}
              dataSource={orderDetails}
              loading={detailLoading}
              rowKey="orderId"
              size="small"
              locale={{ emptyText: <Empty description="当日无已送达订单" /> }}
              pagination={{
                current: detailPage + 1,
                pageSize: DETAIL_PAGE_SIZE,
                total: detailTotal,
                showSizeChanger: false,
                onChange: (page) => setDetailPage(page - 1),
              }}
            />
          </div>

          {record.status !== 'REVIEWED' && (
            <div {...stylex.props(styles.blockSpacing)}>
              <Form form={reviewForm}>
                <Form.Item name="reviewNotes" label="复核备注">
                  <Input.TextArea rows={3} placeholder="请输入复核备注" />
                </Form.Item>
              </Form>
            </div>
          )}
          {record.reviewedAt && (
            <div {...stylex.props(styles.reviewed)}>
              复核时间: {record.reviewedAt}
              <br />
              复核备注: {record.reviewNotes}
            </div>
          )}
        </div>
      )}
    </Modal>
  );
};

export default ReconciliationDetailModal;
