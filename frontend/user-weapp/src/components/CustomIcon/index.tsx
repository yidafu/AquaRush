import React from 'react'
import { Image } from '@tarojs/components'
import { AtIcon } from 'taro-ui'

interface CustomIconProps {
  value: string           // Icon identifier
  size?: number | string   // Icon size (default: 20)
  color?: string          // Icon color (for built-in icons)
  className?: string      // CSS class names
  style?: React.CSSProperties   // Inline styles
  onClick?: (event: any) => void
  prefixClass?: string    // Custom prefix class (Taro UI compatibility)
}

const CustomIcon: React.FC<CustomIconProps> = ({
  value,
  size = 20,
  color,
  className,
  style,
  onClick,
  prefixClass
}) => {
  const handleError = (e: any) => {
    console.warn(`Icon not found: ${value}`)
    // Fallback to a default icon
    if (e.target) {
      e.target.src = '/assets/icons/order/all.png'
    }
  }

  switch (true) {
    // Built-in Taro UI icons - use AtIcon directly
    case value === 'bookmark':
    case value === 'chevron-right':
      return (
        <AtIcon
          value={value}
          size={size}
          color={color}
          className={className}
          style={style}
          onClick={onClick}
          prefixClass={prefixClass}
        />
      )

    // Custom PNG icons - use Image component
    case value.startsWith('/assets/icons/'):
      // Automatically add .png suffix if not present
      const iconSrc = value.endsWith('.png') ? value : `${value}.png`
      return (
        <Image
          src={iconSrc}
          className={`inline-block align-middle object-contain ${className || ''}`}
          style={{
            width: typeof size === 'number' ? `${size}px` : size,
            height: typeof size === 'number' ? `${size}px` : size,
            ...style
          }}
          onClick={onClick}
          onError={handleError}
        />
      )

    // Default fallback for unknown icons
    default:
      return (
        <div
          className={`rounded font-mono font-bold select-none cursor-default flex items-center justify-center ${className || ''}`}
          style={{
            width: typeof size === 'number' ? `${size}px` : size,
            height: typeof size === 'number' ? `${size}px` : size,
            backgroundColor: '#f5f5f5',
            border: '1px dashed #ddd',
            color: '#999',
            fontSize: typeof size === 'number' ? `${size * 0.6}px` : '12px',
            ...style
          }}
          onClick={onClick}
        >
          ?
        </div>
      )
  }
}

export default CustomIcon
