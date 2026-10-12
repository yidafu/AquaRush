import * as stylex from '@stylexjs/stylex'
import type { SVGProps } from 'react'
import { ICON_DATA, type IconName } from './data'

export type { IconName } from './data'

export interface IconProps extends SVGProps<SVGSVGElement> {
  /** 等效 antd 图标 spin：匀速旋转 */
  spin?: boolean
}

const spinKeyframes = stylex.keyframes({
  from: { transform: 'rotate(0deg)' },
  to: { transform: 'rotate(360deg)' },
})

const svgStyle = stylex.create({
  base: {
    width: '1em',
    height: '1em',
    display: 'inline-block',
    verticalAlign: '-0.125em',
    fill: 'currentColor',
    overflow: 'visible',
  },
  spin: {
    animationName: spinKeyframes,
    animationDuration: '1s',
    animationTimingFunction: 'linear',
    animationIterationCount: 'infinite',
  },
})

function createIcon(name: IconName) {
  function Icon({ spin, style, className, ...rest }: IconProps) {
    const def = ICON_DATA[name]
    const sp = stylex.props(spin ? [svgStyle.base, svgStyle.spin] : svgStyle.base)
    const mergedClass = [sp.className, className].filter(Boolean).join(' ') || undefined
    const mergedStyle = sp.style ? { ...sp.style, ...style } : style
    return (
      <svg
        viewBox={def.viewBox}
        xmlns="http://www.w3.org/2000/svg"
        focusable="false"
        {...rest}
        {...sp}
        className={mergedClass}
        style={mergedStyle}
        dangerouslySetInnerHTML={{ __html: def.inner }}
      />
    )
  }
  Icon.displayName = name
  return Icon
}

export const AimOutlined = createIcon("AimOutlined")
export const ArrowDownOutlined = createIcon("ArrowDownOutlined")
export const ArrowLeftOutlined = createIcon("ArrowLeftOutlined")
export const ArrowUpOutlined = createIcon("ArrowUpOutlined")
export const AuditOutlined = createIcon("AuditOutlined")
export const BarChartOutlined = createIcon("BarChartOutlined")
export const BellOutlined = createIcon("BellOutlined")
export const CalendarOutlined = createIcon("CalendarOutlined")
export const CarOutlined = createIcon("CarOutlined")
export const CarryOutOutlined = createIcon("CarryOutOutlined")
export const CheckCircleOutlined = createIcon("CheckCircleOutlined")
export const CheckOutlined = createIcon("CheckOutlined")
export const ClearOutlined = createIcon("ClearOutlined")
export const ClockCircleOutlined = createIcon("ClockCircleOutlined")
export const CloseCircleOutlined = createIcon("CloseCircleOutlined")
export const CloseOutlined = createIcon("CloseOutlined")
export const ControlOutlined = createIcon("ControlOutlined")
export const CopyOutlined = createIcon("CopyOutlined")
export const DashboardOutlined = createIcon("DashboardOutlined")
export const DatabaseOutlined = createIcon("DatabaseOutlined")
export const DeleteOutlined = createIcon("DeleteOutlined")
export const DollarOutlined = createIcon("DollarOutlined")
export const DownloadOutlined = createIcon("DownloadOutlined")
export const DownOutlined = createIcon("DownOutlined")
export const EditOutlined = createIcon("EditOutlined")
export const EnvironmentOutlined = createIcon("EnvironmentOutlined")
export const EyeOutlined = createIcon("EyeOutlined")
export const FileTextOutlined = createIcon("FileTextOutlined")
export const FullscreenOutlined = createIcon("FullscreenOutlined")
export const GiftOutlined = createIcon("GiftOutlined")
export const GiftTwoTone = createIcon("GiftTwoTone")
export const GroupOutlined = createIcon("GroupOutlined")
export const HistoryOutlined = createIcon("HistoryOutlined")
export const KeyOutlined = createIcon("KeyOutlined")
export const LinkOutlined = createIcon("LinkOutlined")
export const LockOutlined = createIcon("LockOutlined")
export const LoginOutlined = createIcon("LoginOutlined")
export const LogoutOutlined = createIcon("LogoutOutlined")
export const MailOutlined = createIcon("MailOutlined")
export const MessageOutlined = createIcon("MessageOutlined")
export const MinusCircleOutlined = createIcon("MinusCircleOutlined")
export const MinusSquareOutlined = createIcon("MinusSquareOutlined")
export const MobileOutlined = createIcon("MobileOutlined")
export const PauseCircleOutlined = createIcon("PauseCircleOutlined")
export const PercentageOutlined = createIcon("PercentageOutlined")
export const PhoneOutlined = createIcon("PhoneOutlined")
export const PictureOutlined = createIcon("PictureOutlined")
export const PlayCircleOutlined = createIcon("PlayCircleOutlined")
export const PlusOutlined = createIcon("PlusOutlined")
export const PlusSquareOutlined = createIcon("PlusSquareOutlined")
export const ReloadOutlined = createIcon("ReloadOutlined")
export const RightOutlined = createIcon("RightOutlined")
export const RocketOutlined = createIcon("RocketOutlined")
export const SafetyOutlined = createIcon("SafetyOutlined")
export const SaveOutlined = createIcon("SaveOutlined")
export const SearchOutlined = createIcon("SearchOutlined")
export const SettingOutlined = createIcon("SettingOutlined")
export const ShopOutlined = createIcon("ShopOutlined")
export const ShoppingCartOutlined = createIcon("ShoppingCartOutlined")
export const ShoppingOutlined = createIcon("ShoppingOutlined")
export const SkinOutlined = createIcon("SkinOutlined")
export const SoundOutlined = createIcon("SoundOutlined")
export const StarOutlined = createIcon("StarOutlined")
export const StopOutlined = createIcon("StopOutlined")
export const SwapOutlined = createIcon("SwapOutlined")
export const SyncOutlined = createIcon("SyncOutlined")
export const TagsOutlined = createIcon("TagsOutlined")
export const TeamOutlined = createIcon("TeamOutlined")
export const ThunderboltOutlined = createIcon("ThunderboltOutlined")
export const UndoOutlined = createIcon("UndoOutlined")
export const UnlockOutlined = createIcon("UnlockOutlined")
export const UploadOutlined = createIcon("UploadOutlined")
export const UserAddOutlined = createIcon("UserAddOutlined")
export const UsergroupAddOutlined = createIcon("UsergroupAddOutlined")
export const UserOutlined = createIcon("UserOutlined")
export const WarningOutlined = createIcon("WarningOutlined")

export const FormatPainterOutlined = createIcon("FormatPainterOutlined")
