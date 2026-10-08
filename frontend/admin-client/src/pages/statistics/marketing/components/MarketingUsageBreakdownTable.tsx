import React, { useMemo, useState } from 'react';
import { Card, Segmented, Table, Tooltip, Tag, Spin, Empty, Button } from 'antd';
import { LinkOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import { centsToYuan, MarketingBreakdownKind } from '@aquarush/common';
import type { MarketingUsageBreakdownRow } from '@aquarush/common';
import { BREAKDOWN_DETAIL_PREFIX, BREAKDOWN_TABS, type BreakdownTabConfig } from '../data';

interface MarketingUsageBreakdownTableProps {
  rows: ReadonlyArray<MarketingUsageBreakdownRow>;
  loading: boolean;
}

/**
 * 营销方式使用明细表(F-STA-007):
 * Segmented 按营销方式切换,列随类型变化(券=核销张数,赠品=赠品件数,票种=销售/核销张数);
 * 优惠金额口径:券/满减/首单立减=订单快照列(精确),赠品=件数×当前目录价(估算,tooltip 说明);
 * 行操作跳转对应活动详情页。
 */
const MarketingUsageBreakdownTable: React.FC<MarketingUsageBreakdownTableProps> = ({ rows, loading }) => {
  const navigate = useNavigate();
  const [kind, setKind] = useState<MarketingBreakdownKind>(MarketingBreakdownKind.COUPON_TEMPLATE);

  const tab = useMemo(
    () => BREAKDOWN_TABS.find((t) => t.kind === kind) ?? BREAKDOWN_TABS[0],
    [kind],
  );

  const filtered = useMemo(() => {
    const list = rows.filter((r) => r.kind === kind);
    // 按优惠金额(票种按销售金额)降序
    return [...list].sort((a, b) => b.benefitCents + b.ticketSalesRevenueCents - (a.benefitCents + a.ticketSalesRevenueCents));
  }, [rows, kind]);

  const columns = useMemo<ColumnsType<MarketingUsageBreakdownRow>>(() => {
    const isTicket = kind === 'TICKET_TYPE';
    const cfg: BreakdownTabConfig = tab;
    return [
      {
        title: cfg.nameColumn,
        dataIndex: 'name',
        key: 'name',
        ellipsis: true,
        render: (name: string) =>
          name || <Tag color="default">已删除</Tag>,
      },
      ...(isTicket
        ? [
            {
              title: '销售张数',
              dataIndex: 'ticketSalesCount',
              key: 'ticketSalesCount',
              align: 'right' as const,
              render: (v: number) => `${v} 张`,
            },
            {
              title: '销售金额',
              dataIndex: 'ticketSalesRevenueCents',
              key: 'ticketSalesRevenueCents',
              align: 'right' as const,
              render: (v: number) => `¥${centsToYuan(v).toFixed(2)}`,
            },
          ]
        : [
            {
              title: '使用订单数',
              dataIndex: 'orderCount',
              key: 'orderCount',
              align: 'right' as const,
              render: (v: number) => `${v} 单`,
            },
            {
              title: (
                <Tooltip title="券/满减/首单立减=订单快照列优惠金额(精确);赠品=赠品件数×当前目录价(估算)">
                  {cfg.benefitColumn}(¥)
                </Tooltip>
              ),
              dataIndex: 'benefitCents',
              key: 'benefitCents',
              align: 'right' as const,
              render: (v: number, row: MarketingUsageBreakdownRow) =>
                row.kind === MarketingBreakdownKind.GIFT_ACTIVITY ? (
                  <Tooltip title="估算:赠品件数 × 赠品当前目录价">
                    <span>¥{centsToYuan(v).toFixed(2)} *</span>
                  </Tooltip>
                ) : (
                  `¥${centsToYuan(v).toFixed(2)}`
                ),
            },
            {
              title: cfg.usageColumn,
              dataIndex: 'usageCount',
              key: 'usageCount',
              align: 'right' as const,
              render: (v: number) => `${v} ${cfg.usageSuffix}`,
            },
          ]),
      {
        title: '操作',
        key: 'action',
        width: 90,
        align: 'center' as const,
        render: (_: unknown, row: MarketingUsageBreakdownRow) => {
          const prefix = BREAKDOWN_DETAIL_PREFIX[row.kind];
          if (!prefix) return null;
          const query = row.kind === 'TICKET_TYPE' ? `?name=${encodeURIComponent(row.name)}` : '';
          return (
            <Button type="link" size="small" icon={<LinkOutlined />} onClick={() => navigate(`${prefix}/${row.refId}${query}`)}>
              详情
            </Button>
          );
        },
      },
    ];
  }, [kind, tab, navigate]);

  return (
    <Card
      title="营销方式使用明细"
      style={{ marginTop: 24 }}
      extra={
        <Segmented
          options={BREAKDOWN_TABS.map((t) => ({ label: t.label, value: t.kind }))}
          value={kind}
          onChange={(value) => setKind(value as MarketingBreakdownKind)}
        />
      }
    >
      <Spin spinning={loading}>
        {filtered.length === 0 ? (
          <Empty description="所选范围该营销方式暂无使用记录" style={{ padding: '32px 0' }} />
        ) : (
          <Table<MarketingUsageBreakdownRow>
            rowKey={(row) => `${row.kind}-${row.refId}`}
            columns={columns}
            dataSource={filtered}
            pagination={false}
            size="small"
          />
        )}
      </Spin>
    </Card>
  );
};

export default MarketingUsageBreakdownTable;
