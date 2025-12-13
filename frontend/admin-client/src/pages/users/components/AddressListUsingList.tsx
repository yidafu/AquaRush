import React from 'react';
import { List, Tag } from 'antd';
import type { Address as GraphQLAddress } from '../../../types/graphql';

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
      className='bg-white'
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
