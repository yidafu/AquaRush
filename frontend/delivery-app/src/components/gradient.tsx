import { LinearGradient, type LinearGradientProps } from 'expo-linear-gradient';

/** 135° 品牌渐变薄封装。颜色走 theme 的 headerGradientStart/End 等 token。 */
export function Gradient(props: LinearGradientProps) {
  return <LinearGradient {...props} />;
}
