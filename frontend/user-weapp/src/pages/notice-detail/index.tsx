import React, { useState, useEffect } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useRouter } from '@tarojs/taro'
import { NoticeVo } from '@aquarush/common'
import NoticeService from '../../services/NoticeService'
import StatusTag from '../../components/StatusTag'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import './index.scss'

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
 */
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
    <View className='notice-detail-page'>
      {loading ? (
        <View className='weui-loadmore weui-loadmore_loading notice-loading'>
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
        <View className='notice-detail-body'>
          {/* 公告列表(置顶优先/发布时间倒序由服务端排序) */}
          <View className='weui-cells mt2 notice-list'>
            {notices.map((notice) => (
              <View
                key={notice.id}
                className={`weui-cell notice-list-item ${notice.id === selected?.id ? 'notice-list-item-active' : ''}`}
                trackTarget='notice-select'
                trackElementType='cell'
                trackElementText={notice.title}
                onClick={() => setSelectedId(notice.id)}
              >
                <View className='weui-cell__bd notice-list-item-bd'>
                  {notice.pinned && (
                    <View className='notice-pin'>
                      <StatusTag tone='warning' text='置顶' size='small' />
                    </View>
                  )}
                  <Text className='notice-list-title'>{notice.title}</Text>
                </View>
                <View className='weui-cell__ft weui-cell__ft_in-access' />
              </View>
            ))}
          </View>

          {/* 选中公告全文(WeUI 文章排版) */}
          {selected && (
            <View className='notice-content-card'>
              <View className='notice-content-head'>
                <Text className='weui-tag weui-tag--brand'>
                  {NOTICE_TYPE_LABELS[selected.type] || selected.type}
                </Text>
              </View>
              <Text className='notice-content-title'>{selected.title}</Text>
              {selected.publishedAt && (
                <Text className='notice-publish-time'>
                  {selected.publishedAt.slice(0, 10)}
                </Text>
              )}
              <View className='notice-content-divider hair-b' />
              <Text className='notice-content-text'>{selected.content}</Text>
            </View>
          )}
        </View>
      )}
    </View>
  )
}

export default NoticeDetailPage
