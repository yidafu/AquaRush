import React from 'react';
import { Empty, Tag, Typography } from 'antd';
import type { ProductFormData } from '../../components/hooks/useProductData';

const { Text } = Typography;

const statusConfig: Record<string, { text: string; color: string }> = {
  ONLINE: { text: '在售', color: 'green' },
  OFFLINE: { text: '下架', color: 'red' },
  OUT_OF_STOCK: { text: '缺货', color: 'orange' },
};

export interface LiveProductPreviewProps {
  data: ProductFormData | null;
}

/**
 * 新增模式下的实时预览卡(products/form/index.tsx 拆分):
 * 展示左侧表单当前填写的商品信息(防抖更新由页面控制)。
 */
const LiveProductPreview: React.FC<LiveProductPreviewProps> = ({ data }) => {
  if (!data || !data.name) {
    return (
      <div style={{ height: 640, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Empty description="填写左侧表单后，此处实时预览商品信息" />
      </div>
    );
  }

  const status = data.status ? statusConfig[data.status] : undefined;

  return (
    <div
      style={{
        width: 320,
        margin: '0 auto',
        background: '#fff',
        borderRadius: 16,
        border: '1px solid #e8e8e8',
        overflow: 'hidden',
        boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
      }}
    >
      <div
        style={{
          height: 260,
          background: '#f0f2f5',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          overflow: 'hidden',
        }}
      >
        {data.coverImageUrl ? (
          <img
            src={data.coverImageUrl}
            alt="封面"
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
          />
        ) : (
          <Text type="secondary">暂无封面图</Text>
        )}
      </div>
      <div style={{ padding: 16 }}>
        <div style={{ fontSize: 16, fontWeight: 600, marginBottom: 4 }}>{data.name}</div>
        {data.subtitle && (
          <Text type="secondary" style={{ fontSize: 13 }}>{data.subtitle}</Text>
        )}
        <div style={{ marginTop: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 20, fontWeight: 700, color: '#ff4d4f' }}>
            {data.price ? `¥${Number(data.price).toFixed(2)}` : '—'}
          </span>
          {data.specification && <Tag>{data.specification}</Tag>}
          {status && <Tag color={status.color}>{status.text}</Tag>}
        </div>
        <div style={{ marginTop: 8, fontSize: 13, color: '#999' }}>
          库存 {data.stock ?? 0} 件
        </div>
      </div>
    </div>
  );
};

export default LiveProductPreview;
