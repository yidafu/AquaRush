import React from 'react';
import { Empty, List, Tag } from '@/ui';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import type { AddressVo as GraphQLAddress } from '@aquarush/common';

// 原 tailwind bg-white(类已随 tailwind 移除失效)→ StyleX token
const styles = stylex.create({
  list: {
    backgroundColor: tokens.card,
  },
  // @/ui List 无 List.Item.Meta,行内标题/描述布局等价替代
  row: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: '8px',
  },
  rowMain: {
    display: 'flex',
    flexDirection: 'column',
    gap: '4px',
  },
  title: {
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
    fontWeight: '500',
    color: tokens.text,
  },
  description: {
    fontSize: tokens.fsNote,
    lineHeight: tokens.lhNote,
    color: tokens.textSub,
  },
});

interface AddressListUsingListProps {
  addresses: GraphQLAddress[];
  loading?: boolean;
  onEdit?: (id: string) => void;
  onDelete?: (id: string) => void;
  onSetDefault?: (id: string) => void;
}

const AddressListUsingList: React.FC<AddressListUsingListProps> = ({
  addresses,
  loading = false,
}) => {
  return (
    <List
      dataSource={addresses}
      loading={loading}
      bordered
      {...stylex.props(styles.list)}
      renderItem={(address) => (
        <div {...stylex.props(styles.row)}>
          <div {...stylex.props(styles.rowMain)}>
            <div {...stylex.props(styles.title)}>
              {`${address.receiverName}  ${address.phone}`}
            </div>
            <div {...stylex.props(styles.description)}>
              {`${address.province} ${address.city} ${address.district} ${address.detailAddress}`}
            </div>
          </div>
          {address.isDefault ? <Tag color="blue">默认地址</Tag> : null}
        </div>
      )}
    >
      {/* @/ui List 无 locale.emptyText;children 仅在空数据时给出,非空仍走 dataSource/renderItem */}
      {addresses.length === 0 ? <Empty description="暂无收货地址" /> : null}
    </List>
  );
};

export default AddressListUsingList;
