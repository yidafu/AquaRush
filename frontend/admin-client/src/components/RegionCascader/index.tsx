import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Cascader, Spin } from 'antd';
import type { Region } from '../../graphql/queries/region.graphql';
import { useAllRegions } from '../../graphql/queries/region.graphql';

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
}

/**
 * 将平铺的地区数据构建为 3 级树形结构
 */
const buildRegionTree = (regions: Region[]): RegionOption[] => {
  // 按 level 分组
  const provinces = regions.filter((r) => r.level === 1);
  const cities = regions.filter((r) => r.level === 2);
  const districts = regions.filter((r) => r.level === 3);

  // 构建城市映射 (parentCode -> cities)
  const citiesMap = new Map<string, Region[]>();
  cities.forEach((city) => {
    if (city.parentCode) {
      const existing = citiesMap.get(city.parentCode) || [];
      existing.push(city);
      citiesMap.set(city.parentCode, existing);
    }
  });

  // 构建区县映射 (parentCode -> districts)
  const districtsMap = new Map<string, Region[]>();
  districts.forEach((district) => {
    if (district.parentCode) {
      const existing = districtsMap.get(district.parentCode) || [];
      existing.push(district);
      districtsMap.set(district.parentCode, existing);
    }
  });

  // 构建树形结构
  return provinces.map((province) => {
    const provinceCities = citiesMap.get(province.code) || [];
    return {
      value: province.code,
      label: province.name,
      code: province.code,
      level: 1,
      isLeaf: false,
      children: provinceCities.map((city) => {
        const cityDistricts = districtsMap.get(city.code) || [];
        return {
          value: city.code,
          label: city.name,
          code: city.code,
          level: 2,
          isLeaf: cityDistricts.length === 0,
          children: cityDistricts.map((district) => ({
            value: district.code,
            label: district.name,
            code: district.code,
            level: 3,
            isLeaf: true,
          })),
        };
      }),
    };
  });
};

export const RegionCascader: React.FC<RegionCascaderProps> = ({
  value,
  onChange,
  placeholder = '请选择省/市/区',
  disabled = false,
}) => {
  const [options, setOptions] = useState<RegionOption[]>([]);
  const { data, loading, error } = useAllRegions();

  // 构建树形结构
  useEffect(() => {
    if (data?.allRegions) {
      const tree = buildRegionTree(data.allRegions);
      setOptions(tree);
    }
  }, [data]);

  // Handle region selection change
  const handleChange = useCallback(
    (_selectedValues: string[], selectedOptions: RegionOption[]) => {
      if (!selectedOptions || selectedOptions.length === 0) {
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

      const lastOption = selectedOptions[selectedOptions.length - 1];
      const provinceOption = selectedOptions[0];
      const cityOption = selectedOptions[1];

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
    return <span style={{ color: 'red' }}>加载地区数据失败</span>;
  }

  return (
    <Cascader
      options={options}
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
      style={{ width: '100%' }}
      expandTrigger="hover"
    />
  );
};

export default RegionCascader;
