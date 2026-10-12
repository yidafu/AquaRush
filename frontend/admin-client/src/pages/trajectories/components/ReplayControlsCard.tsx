import React from 'react';
import { Card, Col, Spin, Space, Button, Select, Slider, Typography } from '@/ui';
import { PauseCircleOutlined, PlayCircleOutlined } from '@/ui/icons';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  stack: {
    width: '100%',
  },
  speedSelect: {
    width: '100px',
  },
  hint: {
    fontSize: '12px',
  },
});

export interface ReplayControlsCardProps {
  pointsCount: number;
  playIndex: number;
  playing: boolean;
  replaySpeed: number;
  loading: boolean;
  onPlay: () => void;
  onSpeedChange: (value: number) => void;
  onSeek: (index: number) => void;
  /** 时间文案(播放进度) */
  timeText: (index: number) => string | undefined;
}

/** 轨迹回放控制卡(trajectories/index.tsx 拆分) */
const ReplayControlsCard: React.FC<ReplayControlsCardProps> = ({
  pointsCount,
  playIndex,
  playing,
  replaySpeed,
  loading,
  onPlay,
  onSpeedChange,
  onSeek,
  timeText,
}) => (
  <Col span={8}>
    <Card size="small" title="轨迹回放">
      <Spin spinning={loading}>
        {pointsCount > 0 ? (
          <Space direction="vertical" {...stylex.props(styles.stack)}>
            <Space>
              <Button
                size="small"
                icon={playing ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                onClick={onPlay}
                disabled={pointsCount < 2}
              >
                {playing ? '暂停' : '播放'}
              </Button>
              <Select
                size="small"
                {...stylex.props(styles.speedSelect)}
                value={replaySpeed}
                onChange={(value: unknown) => onSpeedChange(value as number)}
                options={[
                  { value: 1, label: '1x 慢速' },
                  { value: 2, label: '2x 标准' },
                  { value: 5, label: '5x 快速' },
                  { value: 10, label: '10x 极速' },
                ]}
              />
            </Space>
            <Slider
              min={0}
              max={Math.max(pointsCount - 1, 0)}
              value={playIndex >= 0 ? playIndex : 0}
              onChange={onSeek}
              tooltip={{ formatter: (v) => timeText(v ?? 0) }}
            />
            <Typography.Text type="secondary" {...stylex.props(styles.hint)}>
              {playIndex >= 0 ? timeText(playIndex) : '点击「播放」按时间顺序回放轨迹'}
            </Typography.Text>
          </Space>
        ) : (
          <Typography.Text type="secondary">暂无轨迹数据</Typography.Text>
        )}
      </Spin>
    </Card>
  </Col>
);

export default ReplayControlsCard;
