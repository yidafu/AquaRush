import React from 'react';
import { Empty, Tag, Typography } from '@/ui';
import type { ProductFormData } from '../../components/hooks/useProductData';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

// 说明:#e8e8e8 边框、#f0f2f5 封面占位底、#999 库存文字及 0.08 投影为
// 预览卡局部色值,token 无对应值,保留字面量。
const styles = stylex.create({
  emptyBox: {
    height: '640px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  card: {
    width: '320px',
    marginTop: '0px',
    marginRight: 'auto',
    marginBottom: '0px',
    marginLeft: 'auto',
    backgroundColor: tokens.card,
    borderRadius: tokens.rCard,
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: '#e8e8e8',
    overflow: 'hidden',
    boxShadow: '0 4px 16px rgba(0, 0, 0, 0.08)',
  },
  cover: {
    height: '260px',
    backgroundColor: '#f0f2f5',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  coverImg: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  },
  body: {
    paddingTop: '16px',
    paddingRight: '16px',
    paddingBottom: '16px',
    paddingLeft: '16px',
  },
  name: {
    fontSize: '16px',
    fontWeight: 600,
    marginBottom: '4px',
  },
  subtitle: {
    fontSize: '13px',
  },
  priceRow: {
    marginTop: '12px',
    display: 'flex',
    alignItems: 'center',
    rowGap: '8px',
    columnGap: '8px',
  },
  price: {
    fontSize: '20px',
    fontWeight: 700,
    color: tokens.error,
  },
  stock: {
    marginTop: '8px',
    fontSize: '13px',
    color: '#999',
  },
});

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
      <div {...stylex.props(styles.emptyBox)}>
        <Empty description="填写左侧表单后，此处实时预览商品信息" />
      </div>
    );
  }

  const status = data.status ? statusConfig[data.status] : undefined;

  return (
    <div {...stylex.props(styles.card)}>
      <div {...stylex.props(styles.cover)}>
        {data.coverImageUrl ? (
          <img
            src={data.coverImageUrl}
            alt="封面"
            {...stylex.props(styles.coverImg)}
          />
        ) : (
          <Text type="secondary">暂无封面图</Text>
        )}
      </div>
      <div {...stylex.props(styles.body)}>
        <div {...stylex.props(styles.name)}>{data.name}</div>
        {data.subtitle && (
          <Text type="secondary" {...stylex.props(styles.subtitle)}>{data.subtitle}</Text>
        )}
        <div {...stylex.props(styles.priceRow)}>
          <span {...stylex.props(styles.price)}>
            {data.price ? `¥${Number(data.price).toFixed(2)}` : '—'}
          </span>
          {data.specification && <Tag>{data.specification}</Tag>}
          {status && <Tag color={status.color}>{status.text}</Tag>}
        </div>
        <div {...stylex.props(styles.stock)}>
          库存 {data.stock ?? 0} 件
        </div>
      </div>
    </div>
  );
};

export default LiveProductPreview;
