import Taro from '@tarojs/taro'

/**
 * 实时推送去重(R-NTF-02):推送与轮询按幂等键合并去重,同一事件仅渲染一次。
 * 每个用户保留最近 100 个已见幂等键;进入消息中心后由服务端幂等键兜底。
 */

const STORAGE_KEY = 'notify_seen_idempotency_keys'
const MAX_KEYS_PER_USER = 100

interface SeenKeys {
  [userId: string]: string[]
}

function loadSeen(): SeenKeys {
  try {
    return (Taro.getStorageSync(STORAGE_KEY) as SeenKeys) || {}
  } catch {
    return {}
  }
}

function saveSeen(seen: SeenKeys): void {
  try {
    Taro.setStorageSync(STORAGE_KEY, seen)
  } catch (e) {
    console.error('save seen keys failed:', e)
  }
}

/** 是否重复(重复返回 true,不重复则记录并返回 false) */
export function isDuplicateNotification(userId: string, idempotencyKey: string): boolean {
  const seen = loadSeen()
  const keys = seen[userId] ?? []
  if (keys.includes(idempotencyKey)) {
    return true
  }
  keys.push(idempotencyKey)
  seen[userId] = keys.slice(-MAX_KEYS_PER_USER)
  saveSeen(seen)
  return false
}

/** 清除某用户的去重记录(登出时调用) */
export function clearNotificationDedup(userId: string): void {
  const seen = loadSeen()
  delete seen[userId]
  saveSeen(seen)
}
