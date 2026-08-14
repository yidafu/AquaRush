import React, { useEffect, useMemo } from 'react';
import { Select, Spin } from 'antd';
import { useLazyQuery } from '@apollo/client';
import { GET_PRODUCTS_QUERY } from '../graphql/queries/product.graphql';

interface Props {
  value?: string | number | null;
  onChange?: (value: string) => void;
  placeholder?: string;
  style?: React.CSSProperties;
  allowClear?: boolean;
  disabled?: boolean;
}

// 商品下拉选择(营销模块共用):一次性拉取全部上架商品(id → 名称+规格)
const ProductSelect: React.FC<Props> = ({
  value,
  onChange,
  placeholder = '请选择商品',
  style,
  allowClear = true,
  disabled,
}) => {
  const [loadProducts, { data, loading }] = useLazyQuery(GET_PRODUCTS_QUERY, {
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });

  useEffect(() => {
    loadProducts({ variables: { input: { page: 0, size: 1000 } } });
  }, [loadProducts]);

  const options = useMemo(() => {
    const list = data?.productsPaginated?.list ?? [];
    return list.map((product) => ({
      value: String(product.id),
      label: product.specification ? `${product.name}(${product.specification})` : product.name,
    }));
  }, [data]);

  if (loading && options.length === 0) {
    return <Spin size="small" />;
  }

  return (
    <Select
      showSearch
      optionFilterProp="label"
      value={value != null ? String(value) : undefined}
      onChange={(v) => onChange?.(v)}
      options={options}
      placeholder={placeholder}
      allowClear={allowClear}
      disabled={disabled}
      style={style}
    />
  );
};

export default ProductSelect;
