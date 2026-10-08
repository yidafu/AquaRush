import { useMemo, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import {
  VictoryAxis,
  VictoryChart,
  VictoryLine,
  VictoryTooltip,
  VictoryVoronoiContainer,
} from 'victory-native';

import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export interface WeekLineChartPoint {
  /** 日期 "2026-08-03"（按时间升序，7 个点） */
  date: string;
  orderCount: number;
  /** 营收，已由调用方转为元 */
  earning: number;
}

interface WeekLineChartProps {
  data: WeekLineChartPoint[];
  height?: number;
}

/** "2026-08-03" → "08-03" */
function formatDay(date: string): string {
  const m = date.match(/(\d{4})-(\d{2})-(\d{2})/);
  if (!m) return date;
  return `${m[2]}-${m[3]}`;
}

/** 近7天统计双折线图：订单数（primary）+ 营收（warning），双纵轴。见 weapp WeekStatistics(F2)。 */
export function WeekLineChart({ data, height = 220 }: WeekLineChartProps) {
  const theme = useTheme();

  // 归一化到 [0,1] 共享 y 域，左右轴各自映射回真实值
  const { orderSeries, earningSeries, maxOrders, maxEarning } = useMemo(() => {
    const maxOrders = Math.max(...data.map((d) => d.orderCount), 0);
    const maxEarning = Math.max(...data.map((d) => d.earning), 0);
    const orderSeries = data.map((d, i) => ({
      x: i,
      y: maxOrders > 0 ? d.orderCount / maxOrders : 0,
      day: formatDay(d.date),
      orderCount: d.orderCount,
      earning: d.earning,
    }));
    const earningSeries = data.map((d, i) => ({
      x: i,
      y: maxEarning > 0 ? d.earning / maxEarning : 0,
      day: formatDay(d.date),
      orderCount: d.orderCount,
      earning: d.earning,
    }));
    return { orderSeries, earningSeries, maxOrders, maxEarning };
  }, [data]);

  const [width, setWidth] = useState(0);

  if (width === 0) {
    return <View style={{ height }} onLayout={(e) => setWidth(e.nativeEvent.layout.width)} />;
  }

  if (!data.length) {
    return (
      <View style={[styles.empty, { height }]}>
        <ThemedText type="small" themeColor="textTertiary">
          暂无数据
        </ThemedText>
      </View>
    );
  }

  const tickValues = [0, 0.25, 0.5, 0.75, 1];
  const lastIndex = data.length - 1;

  const axisBaseStyle = {
    axis: { stroke: 'transparent' },
    ticks: { stroke: 'transparent' },
    tickLabels: { fill: theme.textTertiary, fontSize: 10, fontFamily: 'sans-serif' },
  };

  const tooltip = (
    <VictoryTooltip
      flyoutStyle={{ fill: theme.overlayStrong, stroke: 'none' }}
      flyoutPadding={6}
      cornerRadius={4}
      pointerLength={6}
      style={{ fill: theme.headerText, fontSize: 11 }}
    />
  );

  return (
    <View onLayout={(e) => setWidth(e.nativeEvent.layout.width)}>
      <View style={styles.legend}>
        <View style={[styles.legendDot, { backgroundColor: theme.primary }]} />
        <ThemedText type="caption" themeColor="textSecondary">
          订单数
        </ThemedText>
        <View style={[styles.legendDot, { backgroundColor: theme.warning }]} />
        <ThemedText type="caption" themeColor="textSecondary">
          营收(元)
        </ThemedText>
      </View>
      <VictoryChart
        height={height}
        width={width}
        domain={{ x: [0, lastIndex], y: [0, 1] }}
        padding={{ top: 16, bottom: 28, left: 36, right: 44 }}
        containerComponent={
          <VictoryVoronoiContainer
            labels={({ datum }) =>
              `${datum.day}: 订单${datum.orderCount}单, 营收¥${Number(datum.earning).toFixed(2)}`
            }
            labelComponent={tooltip}
            voronoiPadding={8}
          />
        }>
        <VictoryAxis
          tickValues={orderSeries.map((d) => d.x)}
          tickFormat={(x) => orderSeries[x]?.day ?? ''}
          style={axisBaseStyle}
        />
        <VictoryAxis
          dependentAxis
          tickValues={tickValues}
          tickFormat={(v) => String(Math.round(v * maxOrders))}
          style={{
            ...axisBaseStyle,
            grid: { stroke: theme.border, strokeDasharray: '4,4' },
          }}
        />
        <VictoryAxis
          dependentAxis
          orientation="right"
          tickValues={tickValues}
          tickFormat={(v) => `¥${Math.round(v * maxEarning)}`}
          style={axisBaseStyle}
        />
        <VictoryLine
          data={orderSeries}
          interpolation="natural"
          style={{ data: { stroke: theme.primary, strokeWidth: 2 } }}
        />
        <VictoryLine
          data={earningSeries}
          interpolation="natural"
          style={{ data: { stroke: theme.warning, strokeWidth: 2 } }}
        />
      </VictoryChart>
    </View>
  );
}

const styles = StyleSheet.create({
  empty: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  legend: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.one,
    marginBottom: Spacing.two,
  },
  legendDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginLeft: Spacing.two,
  },
});
