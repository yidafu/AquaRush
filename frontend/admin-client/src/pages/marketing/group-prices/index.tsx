import React, { useState, useEffect, useCallback } from 'react';
import { Card, Table, Button, Space, Select, InputNumber, Modal, Alert, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useLazyQuery } from '@apollo/client';
import { displayCents, centsToYuan, yuanToCents } from '@aquarush/common';
import type { CustomerGroupVo, GroupProductPriceVo, ProductVo } from '@aquarush/common';
import { GET_PRODUCTS_QUERY } from '../../../graphql/queries/product.graphql';
import { pagination } from '@/utils/table';
import {
  useCustomerGroups,
  useGroupProductPrices,
  useSaveGroupProductPrices,
} from '../../../services/marketing-graphql';

interface PriceRow {
  productId: string;
  name: string;
  specification: string;
  catalogPriceCents: number;
  /** 当前分组价(元,null = 不设置,回退目录价) */
  priceYuan: number | null;
}

const { Text } = Typography;

// 分组商品专属价页(F-MKT-002):以全量商品目录为基础,叠加当前分组的已配置价格
const GroupPrices: React.FC = () => {
  const [loadGroups, { data: groupsData }] = useCustomerGroups();
  const [groupId, setGroupId] = useState<string | null>(null);
  const [rows, setRows] = useState<PriceRow[]>([]);
  const [batchOpen, setBatchOpen] = useState(false);
  const [batchPrice, setBatchPrice] = useState<number | null>(null);

  const [loadProducts, { data: productsData, loading: productsLoading }] = useLazyQuery(
    GET_PRODUCTS_QUERY,
    { fetchPolicy: 'cache-and-network', errorPolicy: 'all' },
  );
  const [loadPrices, { data: pricesData, loading: pricesLoading }] = useGroupProductPrices();
  const [savePrices, { loading: saving }] = useSaveGroupProductPrices();

  useEffect(() => {
    loadGroups({ variables: { input: { page: 0, size: 200 } } });
  }, [loadGroups]);

  // 选择分组后:加载全量商品目录 + 该分组已配置价格
  const loadGroupData = useCallback(
    (id: string) => {
      loadProducts({ variables: { input: { page: 0, size: 1000 } } });
      loadPrices({ variables: { input: { customerGroupId: id, page: 0, size: 1000 } } });
    },
    [loadProducts, loadPrices],
  );

  const handleGroupChange = (value: string) => {
    setGroupId(value);
    setRows([]); // 切换分组先清空行,避免旧分组价格串组闪烁
    loadGroupData(value);
  };

  // 由「商品目录 + 已配置价格」构建表格行;价格数据变化(含保存后 refetch)时重建
  useEffect(() => {
    const products = productsData?.productsPaginated?.list ?? [];
    if (products.length === 0) return;
    // 过滤保证价格属于当前分组,避免切换分组时旧分组价格瞬闪
    const prices = (pricesData?.groupProductPrices?.content ?? []).filter(
      (p: GroupProductPriceVo) => groupId == null || String(p.customerGroupId) === groupId,
    );
    const configured = new Map(prices.map((p: GroupProductPriceVo) => [p.productId, p.priceCents]));
    setRows(
      products.map((product: ProductVo) => ({
        productId: String(product.id),
        name: product.name,
        specification: product.specification,
        catalogPriceCents: product.priceCents,
        priceYuan: configured.has(String(product.id))
          ? centsToYuan(configured.get(String(product.id)) as number)
          : null,
      })),
    );
    // 依赖 groupId:切换分组时丢弃旧分组价格,避免串组
  }, [productsData, pricesData, groupId]);

  const updateRowPrice = (productId: string, value: number | null) => {
    setRows((prev) => prev.map((row) => (row.productId === productId ? { ...row, priceYuan: value } : row)));
  };

  const clearRow = (productId: string) => {
    updateRowPrice(productId, null);
  };

  const applyBatchPrice = () => {
    setRows((prev) => prev.map((row) => ({ ...row, priceYuan: batchPrice })));
    setBatchOpen(false);
    setBatchPrice(null);
  };

  const handleSave = async () => {
    if (!groupId) return;
    const items = rows
      .filter((row) => row.priceYuan !== null && row.priceYuan !== undefined)
      .map((row) => ({
        productId: row.productId,
        priceCents: yuanToCents(row.priceYuan as number),
      }));
    if (items.length === 0) {
      message.warning('请至少设置一个商品的分组价');
      return;
    }
    // clearUnlisted=true:本批未包含的商品清空分组价,回退目录价
    await savePrices({
      variables: { input: { customerGroupId: groupId, items, clearUnlisted: true } },
    });
  };

  const columns: ColumnsType<PriceRow> = [
    { title: '商品名', dataIndex: 'name', width: 240, ellipsis: true },
    { title: '规格', dataIndex: 'specification', width: 120 },
    {
      title: '目录价',
      dataIndex: 'catalogPriceCents',
      width: 120,
      render: (cents: number) => displayCents(cents),
    },
    {
      title: '分组价(元)',
      dataIndex: 'priceYuan',
      width: 180,
      render: (_, record) => (
        <InputNumber
          min={0.01}
          max={99999}
          precision={2}
          placeholder="不设置=回退目录价"
          style={{ width: 150 }}
          value={record.priceYuan}
          onChange={(value) => updateRowPrice(record.productId, value)}
        />
      ),
    },
    {
      title: '较目录价',
      width: 140,
      render: (_, record) => {
        if (record.priceYuan === null || record.priceYuan === undefined) return '—';
        const diffYuan = centsToYuan(record.catalogPriceCents) - record.priceYuan;
        if (Math.abs(diffYuan) < 0.005) return <Text type="secondary">无差异</Text>;
        return diffYuan > 0 ? (
          <Text type="success">省 ¥{diffYuan.toFixed(2)}</Text>
        ) : (
          <Text type="danger">贵 ¥{Math.abs(diffYuan).toFixed(2)}</Text>
        );
      },
    },
    {
      title: '操作',
      width: 100,
      render: (_, record) => (
        <Button size="small" onClick={() => clearRow(record.productId)} disabled={record.priceYuan === null}>
          清空
        </Button>
      ),
    },
  ];

  const groups = groupsData?.customerGroups?.content ?? [];

  return (
    <Card
      title="分组商品价格"
      extra={
        <Space>
          <Button onClick={() => setBatchOpen(true)} disabled={!groupId}>
            批量设置
          </Button>
          <Button type="primary" onClick={handleSave} loading={saving} disabled={!groupId}>
            保存
          </Button>
        </Space>
      }
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          type="info"
          showIcon
          message="分组价仅对该客户分组内的用户生效;未设置分组价的商品自动使用目录价。保存时未填写的商品将清空其已配置分组价,回退目录价。"
        />
        <Select
          style={{ width: 320 }}
          placeholder="请选择客户分组"
          value={groupId ?? undefined}
          onChange={handleGroupChange}
          showSearch
          optionFilterProp="label"
          options={groups.map((g: CustomerGroupVo) => ({ value: String(g.id), label: `${g.name}(${g.memberCount}人)` }))}
        />
        <Table<PriceRow>
          rowKey="productId"
          columns={columns}
          dataSource={rows}
          loading={productsLoading || pricesLoading}
          pagination={pagination(rows.length, { pageSize: 20, showSizeChanger: true })}
          scroll={{ x: 900 }}
        />
      </Space>
      <Modal
        title="批量设置分组价"
        open={batchOpen}
        onOk={applyBatchPrice}
        onCancel={() => setBatchOpen(false)}
        okText="应用到全部商品"
        destroyOnClose
        width={420}
      >
        <InputNumber
          min={0.01}
          max={99999}
          precision={2}
          allowClear
          placeholder="留空 = 清空全部分组价"
          style={{ width: '100%' }}
          value={batchPrice}
          onChange={(value) => setBatchPrice(value)}
        />
        <div style={{ marginTop: 8, color: '#888' }}>将把同一分组价应用到当前列表的全部商品。</div>
      </Modal>
    </Card>
  );
};

export default GroupPrices;
