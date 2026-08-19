import React from 'react';
import { Card, Table, Tag, Button } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { StockWarningItem } from '../data';

export interface StockWarningTableProps {
  items: StockWarningItem[];
  loading: boolean;
  /** 阈值(展示口径说明) */
  threshold: number;
  onConfigThreshold: () => void;
}

/** 库存预警明细(F-STA-006,G39,statistics/products/index.tsx 拆分):
 *  库存≤阈值(同 R-PRO-02 低库存计数口径),缺货(库存=0)置顶、按库存升序;
 *  阈值配置入口跳转系统设置-库存预警阈值(F-SYS-003) */
const StockWarningTable: React.FC<StockWarningTableProps> = ({
  items,
  loading,
  threshold,
  onConfigThreshold,
}) => {
  const columns: ColumnsType<StockWarningItem> = [
    {
      title: '商品名',
      dataIndex: 'productName',
      key: 'productName',
    },
    {
      title: '规格',
      dataIndex: 'specification',
      key: 'specification',
      render: (spec: string) => (spec || '—'),
    },
    {
      title: '当前库存',
      dataIndex: 'currentStock',
      key: 'currentStock',
      width: 100,
    },
    {
      title: '阈值',
      dataIndex: 'threshold',
      key: 'threshold',
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: StockWarningItem['status']) =>
        status === 'OUT_OF_STOCK' ? (
          <Tag color="red">缺货</Tag>
        ) : (
          <Tag color="orange">预警</Tag>
        ),
    },
  ];

  return (
    <Card
      title="库存预警明细"
      style={{ marginTop: 24 }}
      extra={
        <Button type="link" onClick={onConfigThreshold}>
          阈值配置 ›
        </Button>
      }
    >
      <Table
        rowKey="productId"
        dataSource={items}
        columns={columns}
        pagination={false}
        size="small"
        loading={loading}
        locale={{ emptyText: '暂无库存预警商品' }}
      />
      <div style={{ marginTop: 8, color: '#999', fontSize: 13 }}>
        口径:库存 ≤ 阈值({threshold} 件)进入预警,库存 = 0 标记「缺货」并置顶;与「低库存预警」计数卡同源。
      </div>
    </Card>
  );
};

export default StockWarningTable;
