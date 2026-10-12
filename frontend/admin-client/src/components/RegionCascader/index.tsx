import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Cascader, Spin } from '@/ui';
import type { CascaderOption } from '@/ui';
import { useApolloClient } from '@apollo/client';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import type { Region } from '../../graphql/queries/region.graphql';
import { GET_REGIONS_QUERY, useRegions } from '../../graphql/queries/region.graphql';

const styles = stylex.create({
  error: {
    color: tokens.error,
  },
  cascader: {
    width: '100%',
  },
});

export interface RegionCascaderValue {
  province: string;
  city: string;
  district: string;
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
}

export interface RegionCascaderProps {
  value?: RegionCascaderValue;
  onChange?: (value: RegionCascaderValue) => void;
  placeholder?: string;
  disabled?: boolean;
}

interface RegionOption {
  value: string;
  label: string;
  code: string;
  level: number;
  children?: RegionOption[];
  isLeaf?: boolean;
  loading?: boolean;
  /** 与 @/ui CascaderOption 的索引签名对齐,便于互相断言/赋值 */
  [key: string]: unknown;
}

const toOption = (region: Region): RegionOption => ({
  value: region.code,
  label: region.name,
  code: region.code,
  level: region.level,
  // 省级/市级可能有下级;区县级为叶子(实际无下级时展开显示空列表)
  isLeaf: region.level >= 3,
  children: region.level >= 3 ? undefined : [],
});

/**
 * 省/市/区级联选择器(逐级懒加载)。
 *
 * 原实现一次性拉全量地区(3429 行)再在浏览器里建树;AGENTS.md 规则 10 禁止整表返回,
 * 改为 Ant Design Cascader 的 loadData 逐级拉取(每次最多 ~34 条),编辑既有地址时
 * 按已选 code 预取路径以便回显名称。
 */
export const RegionCascader: React.FC<RegionCascaderProps> = ({
  value,
  onChange,
  placeholder = '请选择省/市/区',
  disabled = false,
}) => {
  const client = useApolloClient();
  const [options, setOptions] = useState<RegionOption[]>([]);
  const { data, loading, error } = useRegions();
  const prefetchedKey = useRef<string>('');

  // 省级列表(根节点)
  useEffect(() => {
    if (data?.regions) {
      setOptions(data.regions.map(toOption));
    }
  }, [data]);

  const fetchChildren = useCallback(
    async (parentCode: string): Promise<RegionOption[]> => {
      const result = await client.query<{ regions: Region[] }>({
        query: GET_REGIONS_QUERY,
        variables: { parentCode },
        fetchPolicy: 'cache-first',
      });
      return (result.data?.regions ?? []).map(toOption);
    },
    [client]
  );

  // 懒加载:展开某节点时拉取其下级
  const loadData = useCallback(
    async (selectedOptions: (RegionOption | { value: string; label: React.ReactNode })[]) => {
      const target = selectedOptions[selectedOptions.length - 1] as RegionOption;
      target.loading = true;
      setOptions((prev) => [...prev]);
      try {
        target.children = await fetchChildren(target.code);
        target.isLeaf = target.children.length === 0;
      } catch (e) {
        console.error('Failed to load child regions:', e);
        target.children = [];
      } finally {
        target.loading = false;
        setOptions((prev) => [...prev]);
      }
    },
    [fetchChildren]
  );

  // 编辑回显:按已选 code 预取「省 → 市 → 区」路径(最多 2 次请求)
  useEffect(() => {
    const provinceCode = value?.provinceCode;
    const cityCode = value?.cityCode;
    if (!provinceCode || options.length === 0) return;
    const key = `${provinceCode}|${cityCode ?? ''}|${value?.districtCode ?? ''}`;
    if (prefetchedKey.current === key) return;
    prefetchedKey.current = key;

    void (async () => {
      try {
        const province = options.find((o) => o.code === provinceCode);
        if (!province) return;
        if (!province.children?.length) province.children = await fetchChildren(provinceCode);
        if (!cityCode) return;
        const city = province.children?.find((o) => o.code === cityCode);
        if (!city) return;
        if (!city.children?.length) city.children = await fetchChildren(cityCode);
        setOptions((prev) => [...prev]);
      } catch (e) {
        console.error('Failed to prefetch region path:', e);
      }
    })();
  }, [value?.provinceCode, value?.cityCode, value?.districtCode, options, fetchChildren]);

  // Handle region selection change
  // (@/ui Cascader onChange 第二参为 CascaderOption[],运行时即本组件的 RegionOption,收窄后使用)
  const handleChange = useCallback(
    (_selectedValues: string[], selectedOptions: CascaderOption[]) => {
      const regionOptions = selectedOptions as RegionOption[];
      if (!regionOptions || regionOptions.length === 0) {
        onChange?.({
          province: '',
          city: '',
          district: '',
          provinceCode: undefined,
          cityCode: undefined,
          districtCode: undefined,
        });
        return;
      }

      const lastOption = regionOptions[regionOptions.length - 1];
      const provinceOption = regionOptions[0];
      const cityOption = regionOptions[1];

      onChange?.({
        province: provinceOption?.label || '',
        city: cityOption?.label || '',
        district: lastOption?.label || '',
        provinceCode: provinceOption?.code,
        cityCode: cityOption?.code,
        districtCode: lastOption?.code,
      });
    },
    [onChange]
  );

  // Build default value for editing
  const defaultValue = useMemo(() => {
    if (!value?.provinceCode) return undefined;
    const result: string[] = [value.provinceCode];
    if (value.cityCode) {
      result.push(value.cityCode);
    }
    if (value.districtCode) {
      result.push(value.districtCode);
    }
    return result;
  }, [value]);

  if (loading && options.length === 0) {
    return <Spin size="small" />;
  }

  if (error) {
    console.error('Failed to load regions:', error);
    return <span {...stylex.props(styles.error)}>加载地区数据失败</span>;
  }

  return (
    <Cascader
      options={options}
      loadData={loadData}
      onChange={handleChange}
      defaultValue={defaultValue}
      value={defaultValue}
      placeholder={placeholder}
      disabled={disabled || loading}
      changeOnSelect={false}
      fieldNames={{
        label: 'label',
        value: 'value',
        children: 'children',
      }}
      {...stylex.props(styles.cascader)}
      expandTrigger="hover"
    />
  );
};

export default RegionCascader;
