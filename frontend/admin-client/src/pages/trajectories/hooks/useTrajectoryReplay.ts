import { useCallback, useEffect, useRef, useState } from 'react';
import type { Dispatch, SetStateAction } from 'react';
import type { TrajectoryPoint } from '../utils';

export interface UseTrajectoryReplayResult {
  playIndex: number;
  playing: boolean;
  replaySpeed: number;
  setPlayIndex: Dispatch<SetStateAction<number>>;
  setPlaying: Dispatch<SetStateAction<boolean>>;
  setReplaySpeed: Dispatch<SetStateAction<number>>;
  stopPlayback: () => void;
  handlePlay: () => void;
}

/**
 * 轨迹回放控制(trajectories/index.tsx 拆分):
 * 播放进度/播放状态/速度 + interval 定时器生命周期(卸载时清理)。
 */
export const useTrajectoryReplay = (points: TrajectoryPoint[]): UseTrajectoryReplayResult => {
  const [playIndex, setPlayIndex] = useState(-1);
  const [playing, setPlaying] = useState(false);
  const [replaySpeed, setReplaySpeed] = useState(2);
  const playTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const stopPlayback = useCallback(() => {
    setPlaying(false);
    if (playTimerRef.current) {
      clearInterval(playTimerRef.current);
      playTimerRef.current = null;
    }
  }, []);

  const handlePlay = useCallback(() => {
    if (points.length === 0) return;
    if (playing) {
      stopPlayback();
      return;
    }
    setPlayIndex(0);
    setPlaying(true);
    playTimerRef.current = setInterval(() => {
      setPlayIndex((prev) => {
        if (prev >= points.length - 1) {
          setPlaying(false);
          if (playTimerRef.current) {
            clearInterval(playTimerRef.current);
            playTimerRef.current = null;
          }
          return prev;
        }
        return prev + 1;
      });
    }, 1000 / replaySpeed);
  }, [points, playing, replaySpeed, stopPlayback]);

  // 组件卸载时停止回放(清理定时器)
  useEffect(() => stopPlayback, [stopPlayback]);

  return { playIndex, playing, replaySpeed, setPlayIndex, setPlaying, setReplaySpeed, stopPlayback, handlePlay };
};
