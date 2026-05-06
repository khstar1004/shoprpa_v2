import { debounce } from 'lodash-es'

// LRU 저장
export class LRUCache<T = any> {
  private initialData: T
  private storageKey: string
  private capacity: number

  constructor(storageKey: string, capacity: number, initialData: T) {
    this.capacity = capacity
    this.storageKey = storageKey
    this.initialData = initialData
  }

  get(key: string): T {
    if (!key)
      return this.initialData

    try {
      const storageData = localStorage.getItem(this.storageKey)
      if (!storageData)
        return this.initialData

      const parsedData: Record<string, T> = JSON.parse(storageData)
      return parsedData[key] || this.initialData
    }
    catch (error) {
      console.error('Failed to get expanded keys from storage:', error)
      return this.initialData
    }
  }

  set(key: string, value: T) {
    if (!key)
      return

    try {
      const storageData = localStorage.getItem(this.storageKey)
      let parsedData: Record<string, T> = storageData ? JSON.parse(storageData) : {}

      // 업데이트현재 storageId 의데이터
      parsedData[key] = value

      // 사용 LRU 저장, 보관다중 MAX_STORAGE_ITEMS 개목록
      const entries = Object.entries(parsedData)
      if (entries.length > this.capacity) {
        // 제거의목록
        parsedData = Object.fromEntries(entries.slice(-this.capacity))
      }

      localStorage.setItem(this.storageKey, JSON.stringify(parsedData))
    }
    catch (error) {
      console.error('Failed to save expanded keys to storage:', error)
    }
  }

  debouncedSet = debounce(this.set, 300)
}