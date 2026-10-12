import React from 'react';
import { Row, Col, Input, Select, DatePicker } from '@aquarush/admin-ui';
import { SearchOutlined } from '@aquarush/admin-ui/icons';
import FilterCard from '@/components/FilterCard';
import { useDebouncedFilters } from '@/hooks/useDebouncedFilters';
import { SUBSCRIPTION_STATUS_TEXT } from '@/utils/subscription';
import type { SubscriptionStatusValue } from '@/utils/subscription';
import * as stylex from '@stylexjs/stylex';

// #666 / #bfbfbf 为筛选区局部灰阶,共享 token 无对应项,保留字面量
const styles = stylex.create({
  fieldWrap: {
    marginBottom: '8px',
  },
  label: {
    fontSize: '14px',
    color: '#666',
    marginBottom: '4px',
    display: 'block',
  },
  prefixIcon: {
    color: '#bfbfbf',
  },
  fullWidth: {
    width: '100%',
  },
});

const { RangePicker } = DatePicker;
const { Option } = Select;

export interface SubscriptionFilters {
  /** 用户关键字(昵称/手机号) */
  search?: string;
  status?: string;
  nextRunFrom?: string;
  nextRunTo?: string;
}

interface SubscriptionFiltersProps {
  filters?: SubscriptionFilters;
  onFiltersChange?: (filters: SubscriptionFilters) => void;
}

/** 计划状态下拉:三态文案统一来自 utils/subscription(与设计稿一致) */
const STATUS_OPTIONS = (Object.keys(SUBSCRIPTION_STATUS_TEXT) as SubscriptionStatusValue[]).map((value) => ({
  value,
  label: SUBSCRIPTION_STATUS_TEXT[value],
}));

/**
 * 周期订购计划筛选卡(设计稿:用户关键字 / 计划状态 / 下次配送时间区间)。
 * 本地状态 + 300ms 防抖(关键字防抖,状态/日期即时);reset 回默认。
 */
export const SubscriptionFilters: React.FC<SubscriptionFiltersProps> = ({
  filters = {},
  onFiltersChange,
}) => {
  const { filters: localFilters, setFilters, reset } = useDebouncedFilters<SubscriptionFilters>({
    initial: filters,
    onFiltersChange,
  });

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFilters({ ...localFilters, search: e.target.value });
  };

  const handleStatusChange = (status: string) => {
    setFilters(
      { ...localFilters, status: status === 'ALL' ? undefined : status },
      { immediate: true },
    );
  };

  const handleNextRunRangeChange = (_dates: unknown, dateStrings: [string, string]) => {
    setFilters(
      {
        ...localFilters,
        nextRunFrom: dateStrings[0] || undefined,
        nextRunTo: dateStrings[1] || undefined,
      },
      { immediate: true },
    );
  };

  return (
    <FilterCard onReset={reset}>
      <Row gutter={16}>
        <Col xs={24} sm={12} md={8}>
          <div {...stylex.props(styles.fieldWrap)}>
            <label {...stylex.props(styles.label)}>
              用户
            </label>
            <Input
              placeholder="姓名 / 手机号"
              value={localFilters.search || ''}
              onChange={handleSearchChange}
              allowClear
              prefix={<SearchOutlined {...stylex.props(styles.prefixIcon)} />}
            />
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div {...stylex.props(styles.fieldWrap)}>
            <label {...stylex.props(styles.label)}>
              计划状态
            </label>
            <Select
              {...stylex.props(styles.fullWidth)}
              value={localFilters.status || 'ALL'}
              onChange={(value: string | string[] | undefined) =>
                handleStatusChange(value as string)
              }
              placeholder="选择状态"
            >
              <Option value="ALL">全部</Option>
              {STATUS_OPTIONS.map((option) => (
                <Option key={option.value} value={option.value}>
                  {option.label}
                </Option>
              ))}
            </Select>
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div {...stylex.props(styles.fieldWrap)}>
            <label {...stylex.props(styles.label)}>
              下次配送时间
            </label>
            <RangePicker
              {...stylex.props(styles.fullWidth)}
              onChange={handleNextRunRangeChange}
              placeholder={['开始日期', '结束日期']}
            />
          </div>
        </Col>
      </Row>
    </FilterCard>
  );
};

export default SubscriptionFilters;
