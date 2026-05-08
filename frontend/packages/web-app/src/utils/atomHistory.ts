import { LRUCache } from './lruCache'

const ATOM_USAGE_HISTORY_KEY = 'atom_usage_history'

interface AtomUsageRecord {
  key: string
  title: string
  icon?: string
  parentKey?: string
  timestamp?: number
}

// 생성LRU저장, 다중저장50기록
const atomHistoryCache = new LRUCache<AtomUsageRecord[]>(ATOM_USAGE_HISTORY_KEY, 50, [])

export function recordAtomUsage(atom: {
  key: string
  title: string
  icon?: string
  parentKey?: string
}) {
  try {
    const currentHistory = atomHistoryCache.get('history') || []
    const newRecord: AtomUsageRecord = {
      ...atom,
      timestamp: Date.now(),
    }
    const filteredHistory = currentHistory.filter(item => item.key !== atom.key)
    const updatedHistory = [newRecord, ...filteredHistory]
    atomHistoryCache.set('history', updatedHistory)
  }
  catch (error) {
    console.error('Failed to record atom usage:', error)
  }
}

/**
 * 가져오기 사용의기존가능목록
 * @param limit 반환의기록수제한제어, 5
 * @returns 사용의기존가능목록
 */
export function getRecentAtomUsage(limit: number = 5): AtomUsageRecord[] {
  try {
    const history = atomHistoryCache.get('history') || []
    return history.slice(0, limit)
  }
  catch (error) {
    console.error('Failed to get recent atom usage:', error)
    return []
  }
}

/**
 * 지우기기존가능사용
 */
export function clearAtomUsageHistory() {
  try {
    atomHistoryCache.set('history', [])
  }
  catch (error) {
    console.error('Failed to clear atom usage history:', error)
  }
}
