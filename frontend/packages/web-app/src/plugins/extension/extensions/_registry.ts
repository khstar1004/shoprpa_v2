import type { IPluginSubscription } from '@rpa/shared'

/**
 * 초과회원가입기기
 * 지원직선연결사용, 필요하지 않습니다복사방법법
 */
export class SimpleRegistry<TItem, TKey = string> {
  protected items = new Map<TKey, TItem>()
  protected subscriptions = new Map<TKey, IPluginSubscription>()

  /**
   * 회원가입목록
   */
  register(key: TKey, item: TItem, onDispose?: () => void): IPluginSubscription {
    this.items.set(key, item)

    const subscription: IPluginSubscription = {
      dispose: () => {
        this.unregister(key)
        onDispose?.()
      },
    }

    this.subscriptions.set(key, subscription)
    return subscription
  }

  /**
   * 비고판매목록
   */
  unregister(key: TKey): void {
    this.items.delete(key)
    this.subscriptions.delete(key)
  }

  /**
   * 가져오기 목록
   */
  get(key: TKey): TItem | undefined {
    return this.items.get(key)
  }

  /**
   * 가져오기모든목록
   */
  getAll(): TItem[] {
    return Array.from(this.items.values())
  }

  /**
   * 가져오기모든
   */
  getKeys(): TKey[] {
    return Array.from(this.items.keys())
  }

  /**
   * 조회목록여부저장에서
   */
  has(key: TKey): boolean {
    return this.items.has(key)
  }

  /**
   * 가져오기 목록수
   */
  size(): number {
    return this.items.size
  }

  /**
   * 빈모든목록
   */
  clear(): void {
    this.items.clear()
    this.subscriptions.clear()
  }
}

/**
 * 다중값회원가입기기
 */
export class MultiValueRegistry<TItem, TKey = string> {
  protected items = new Map<TKey, Set<TItem>>()
  protected subscriptions = new Map<string, IPluginSubscription>()

  /**
   * 회원가입목록
   */
  register(key: TKey, item: TItem, onDispose?: () => void): IPluginSubscription {
    if (!this.items.has(key)) {
      this.items.set(key, new Set())
    }
    this.items.get(key)!.add(item)

    const subscriptionId = `${String(key)}-${this.generateItemId(item)}`
    const subscription: IPluginSubscription = {
      dispose: () => {
        this.unregister(key, item)
        onDispose?.()
      },
    }

    this.subscriptions.set(subscriptionId, subscription)
    return subscription
  }

  /**
   * 비고판매목록
   */
  unregister(key: TKey, item: TItem): void {
    const items = this.items.get(key)
    if (items) {
      items.delete(item)
      if (items.size === 0) {
        this.items.delete(key)
      }
    }

    const subscriptionId = `${String(key)}-${this.generateItemId(item)}`
    this.subscriptions.delete(subscriptionId)
  }

  /**
   * 가져오기 목록의모든값
   */
  get(key: TKey): TItem[] {
    return Array.from(this.items.get(key) || [])
  }

  /**
   * 가져오기모든목록
   */
  getAll(): TItem[] {
    const allItems: TItem[] = []
    this.items.forEach((items) => {
      allItems.push(...items)
    })
    return allItems
  }

  /**
   * 가져오기모든
   */
  getKeys(): TKey[] {
    return Array.from(this.items.keys())
  }

  /**
   * 조회목록여부저장에서
   */
  has(key: TKey, item: TItem): boolean {
    return this.items.get(key)?.has(item) || false
  }

  /**
   * 가져오기 목록수
   */
  size(): number {
    let count = 0
    this.items.forEach((items) => {
      count += items.size
    })
    return count
  }

  /**
   * 빈모든목록
   */
  clear(): void {
    this.items.clear()
    this.subscriptions.clear()
  }

  /**
   * 완료목록ID
   */
  private generateItemId(_item: TItem): string {
    return Math.random().toString(36).substr(2, 9)
  }
}

/**
 * 파일회원가입기기
 */
export class EventRegistry<TArgs extends any[] = any[]> {
  protected callbacks = new Map<string, Set<(...args: TArgs) => any>>()
  protected subscriptions = new Map<string, IPluginSubscription>()

  /**
   * 회원가입파일돌아가기조정
   */
  register(event: string, callback: (...args: TArgs) => any): IPluginSubscription {
    if (!this.callbacks.has(event)) {
      this.callbacks.set(event, new Set())
    }
    this.callbacks.get(event)!.add(callback)

    const subscriptionId = `${event}-${this.generateCallbackId(callback)}`
    const subscription: IPluginSubscription = {
      dispose: () => {
        this.unregister(event, callback)
      },
    }

    this.subscriptions.set(subscriptionId, subscription)
    return subscription
  }

  /**
   * 비고판매파일돌아가기조정
   */
  unregister(event: string, callback: (...args: TArgs) => any): void {
    const callbacks = this.callbacks.get(event)
    if (callbacks) {
      callbacks.delete(callback)
      if (callbacks.size === 0) {
        this.callbacks.delete(event)
      }
    }

    const subscriptionId = `${event}-${this.generateCallbackId(callback)}`
    this.subscriptions.delete(subscriptionId)
  }

  /**
   * 트리거파일
   */
  async trigger(event: string, ...args: TArgs): Promise<void> {
    const callbacks = this.callbacks.get(event)
    if (callbacks && callbacks.size > 0) {
      const promises = Array.from(callbacks).map((callback) => {
        try {
          return Promise.resolve(callback(...args))
        }
        catch (error) {
          console.error(`[EventRegistry] Error in callback for event ${event}:`, error)
          return Promise.resolve()
        }
      })

      await Promise.all(promises)
    }
  }

  /**
   * 가져오기모든파일
   */
  getEvents(): string[] {
    return Array.from(this.callbacks.keys())
  }

  /**
   * 가져오기 파일의돌아가기조정수
   */
  getCallbackCount(event: string): number {
    return this.callbacks.get(event)?.size || 0
  }

  /**
   * 빈모든돌아가기조정
   */
  clear(): void {
    this.callbacks.clear()
    this.subscriptions.clear()
  }

  /**
   * 완료돌아가기조정ID
   */
  private generateCallbackId(callback: (...args: TArgs) => any): string {
    return callback.name || 'anonymous'
  }
}
