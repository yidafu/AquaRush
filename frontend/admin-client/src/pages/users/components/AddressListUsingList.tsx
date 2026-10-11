import React from 'react';
import { List, Tag } from 'antd';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import type { AddressVo as GraphQLAddress } from '@aquarush/common';

// 原 tailwind bg-white(类已随 tailwind 移除失效)→ StyleX token
const styles = stylex.create({
  list: {
    backgroundColor: tokens.card,
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
        <List.Item
          extra={address.isDefault ? <Tag color="blue">默认地址</Tag>  : ''}
        >
          <List.Item.Meta
            title={`${address.receiverName}  ${address.phone}`}
            description={`${address.province} ${address.city} ${address.district} ${address.detailAddress}`}
          />
        </List.Item>

      )}
      locale={{
        emptyText: '暂无收货地址',
      }}
    />
  );
};

export default AddressListUsingList;
