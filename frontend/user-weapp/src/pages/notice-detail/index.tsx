import React, { useState, useEffect } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useRouter } from '@tarojs/taro'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { pageTheme } from '../../styles/theme.stylex'
import { NoticeVo } from '@aquarush/common'
import NoticeService from '../../services/NoticeService'
import StatusTag from '../../components/StatusTag'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'

// 公告类型文案
const NOTICE_TYPE_LABELS: Record<string, string> = {
  PLATFORM_NOTICE: '平台公告',
  WATER_OUTAGE: '停水通知',
  ACTIVITY_PREVIEW: '活动预告'
}

/**
 * 公告详情页(F-OPS-002 用户端,WeUI 文章排版)
 * 展示公告列表(.weui-cells,置顶优先/发布时间倒序) + 选中公告全文
 * (.weui-panel 文章卡:类型 tag / 标题 34px / 时间 / 正文 34px·1.6 行距);
 * id 来自路由参数,缺省选第 1 条。
 *
 * 样式:StyleX(原 index.scss 迁移;750 基准 rpx,weui-* 类保留字符串引用;
 * 原嵌套后代选择器全部拍平到子元素,选中态标题用条件类;
 * 死类 notice-detail-body / notice-list-item 无定义已删除)
 */
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    boxSizing: 'border-box',
    backgroundColor: 'var(--weui-BG-0)',
  },
  noticeLoading: {
    paddingTop: '40vh',
  },
  // 公告列表(白底圆角 cells)
  noticeList: {
    marginLeft: '16rpx',
    marginRight: '16rpx',
    borderRadius: 'var(--weui-radius)',
  },
  noticeListItemBd: {
    display: 'flex',
    alignItems: 'center',
    rowGap: '12rpx',
    columnGap: '12rpx',
    minWidth: 0,
  },
  noticePin: {
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto',
  },
  noticeListTitle: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: 'auto',
    minWidth: 0,
    fontSize: 'var(--weui-fs-cell)',
    color: 'var(--weui-FG-0)',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  // 选中项:标题品牌蓝加粗(原选中态语义保留)
  noticeListTitleActive: {
    color: 'var(--weui-BRAND)',
    fontWeight: 600,
  },
  // 公告全文卡(白底圆角,文章排版)
  noticeContentCard: {
    marginTop: '24rpx',
    marginRight: '16rpx',
    marginBottom: 0,
    marginLeft: '16rpx',
    paddingTop: '40rpx',
    paddingRight: '32rpx',
    paddingBottom: '40rpx',
    paddingLeft: '32rpx',
    backgroundColor: 'var(--weui-BG-2)',
    borderRadius: 'var(--weui-radius)',
  },
  noticeContentHead: {
    marginBottom: '24rpx',
  },
  noticeContentTitle: {
    display: 'block',
    fontSize: '40rpx',
    fontWeight: 600,
    color: 'var(--weui-FG-0)',
    lineHeight: 1.4,
  },
  noticePublishTime: {
    display: 'block',
    marginTop: '16rpx',
    fontSize: '24rpx',
    color: 'var(--weui-FG-2)',
  },
  noticeContentDivider: {
    height: 0,
    marginTop: '32rpx',
    marginBottom: '32rpx',
  },
  // 正文 17px ×2 = 34px,1.6 行距(WeUI 文章排版)
  noticeContentText: {
    display: 'block',
    fontSize: '34rpx',
    color: 'var(--weui-FG-0)',
    lineHeight: 1.6,
    whiteSpace: 'pre-wrap',
    wordBreak: 'break-all',
  },
})
const NoticeDetailPage: React.FC = () => {
  usePageViewTracking()

  const router = useRouter()
  const [notices, setNotices] = useState<NoticeVo[]>([])
  const [selectedId, setSelectedId] = useState<string>('')
  const [loading, setLoading] = useState<boolean>(true)

  useEffect(() => {
    loadNotices()
  }, [])

  // 路由参数变化时切换选中公告
  useEffect(() => {
    const id = router.params.id
    if (id) {
      setSelectedId(id)
    }
  }, [router.params.id])

  const loadNotices = async () => {
    try {
      const result = await NoticeService.getActiveNotices()
      if (result.success) {
        setNotices(result.data)
        if (!router.params.id && result.data.length > 0) {
          setSelectedId(result.data[0].id)
        }
      } else {
        throw new Error(result.error.message)
      }
    } catch (error) {
      console.error('Load notices error:', error)
      Taro.showToast({
        title: '加载失败',
        icon: 'none'
      })
    } finally {
      setLoading(false)
    }
  }

  const selected = notices.find((n) => n.id === selectedId) || null

  return (
    <View {...stylex.props(pageTheme, styles.page)}>
      {loading ? (
        <View {...props('weui-loadmore weui-loadmore_loading', styles.noticeLoading)}>
          <View className='spinner' />
          <Text>加载中...</Text>
        </View>
      ) : notices.length === 0 ? (
        <View className='weui-empty'>
          <View className='weui-empty__icon'>
            <Text>📢</Text>
          </View>
          <View className='weui-empty__text'>暂无公告</View>
        </View>
      ) : (
        <View>
          {/* 公告列表(置顶优先/发布时间倒序由服务端排序) */}
          <View {...props('weui-cells mt2', styles.noticeList)}>
            {notices.map((notice) => {
              const isActive = notice.id === selected?.id
              return (
                <View
                  key={notice.id}
                  className='weui-cell'
                  trackTarget='notice-select'
                  trackElementType='cell'
                  trackElementText={notice.title}
                  onClick={() => setSelectedId(notice.id)}
                >
                  <View {...props('weui-cell__bd', styles.noticeListItemBd)}>
                    {notice.pinned && (
                      <View {...stylex.props(styles.noticePin)}>
                        <StatusTag tone='warning' text='置顶' size='small' />
                      </View>
                    )}
                    <Text {...props(styles.noticeListTitle, isActive && styles.noticeListTitleActive)}>
                      {notice.title}
                    </Text>
                  </View>
                  <View className='weui-cell__ft weui-cell__ft_in-access' />
                </View>
              )
            })}
          </View>

          {/* 选中公告全文(WeUI 文章排版) */}
          {selected && (
            <View {...stylex.props(styles.noticeContentCard)}>
              <View {...stylex.props(styles.noticeContentHead)}>
                <Text className='weui-tag weui-tag--brand'>
                  {NOTICE_TYPE_LABELS[selected.type] || selected.type}
                </Text>
              </View>
              <Text {...stylex.props(styles.noticeContentTitle)}>{selected.title}</Text>
              {selected.publishedAt && (
                <Text {...stylex.props(styles.noticePublishTime)}>
                  {selected.publishedAt.slice(0, 10)}
                </Text>
              )}
              <View {...props('hair-b', styles.noticeContentDivider)} />
              <Text {...stylex.props(styles.noticeContentText)}>{selected.content}</Text>
            </View>
          )}
        </View>
      )}
    </View>
  )
}

export default NoticeDetailPage
