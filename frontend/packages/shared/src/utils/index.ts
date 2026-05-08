/**
 * 시간 초과공가능의 Promise 패키지설치기기
 * @param promise 필요패키지설치의 Promise
 * @param timeout 시간 초과시간(초)
 * @param timeoutMessage 시간 초과시의오류메시지, 로 'Operation timed out'
 * @returns 패키지설치후의 Promise, 에서시간 초과시 reject
 */
export function withTimeout<T>(
  promise: Promise<T>,
  timeout: number,
  timeoutMessage: string = 'Operation timed out',
): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    // 시간 초과예약기기
    const timeoutId = setTimeout(() => {
      reject(new Error(timeoutMessage))
    }, timeout)

    // 실행기존 Promise
    promise.then(resolve).catch(reject).finally(() => clearTimeout(timeoutId))
  })
}

/**
 * 시간 초과공가능의예외데이터패키지설치기기
 * @param asyncFn 필요패키지설치의예외데이터
 * @param timeout 시간 초과시간(초)
 * @param timeoutMessage 시간 초과시의오류메시지, 로 'Operation timed out'
 * @returns 패키지설치후의예외데이터
 */
export function createTimeoutWrapper<T extends (...args: any[]) => Promise<any>>(
  asyncFn: T,
  timeout: number,
  timeoutMessage: string = 'Operation timed out',
): (...args: Parameters<T>) => Promise<ReturnType<T>> {
  return (...args: Parameters<T>): Promise<ReturnType<T>> => {
    return withTimeout(asyncFn(...args), timeout, timeoutMessage)
  }
}

export interface RetryOptions {
  timeout?: number
  maxRetries?: number
  retryDelay?: number
  timeoutMessage?: string
  shouldRetry?: (error: Error) => boolean
}

/**
 * 시간 초과및재시도공가능의 Promise 패키지설치기기
 * @param promiseFn 반환 Promise 의데이터
 * @param options 매칭선택
 * @returns 패키지설치후의 Promise
 */
export function withTimeoutAndRetry<T>(
  promiseFn: () => Promise<T>,
  options: RetryOptions = {},
): Promise<T> {
  const {
    timeout = 5000,
    maxRetries = 3,
    retryDelay = 1000,
    timeoutMessage = 'Operation timed out',
    shouldRetry = (error: Error) => !error.message.includes('timeout'),
  } = options

  return new Promise<T>((resolve, reject) => {
    let retries = 0

    const attempt = () => {
      const timeoutPromise = new Promise<T>((_, rejectTimeout) => {
        setTimeout(() => {
          rejectTimeout(new Error(timeoutMessage))
        }, timeout)
      })

      Promise.race([promiseFn(), timeoutPromise])
        .then(resolve)
        .catch((error) => {
          retries++

          if (retries < maxRetries && shouldRetry(error)) {
            setTimeout(attempt, retryDelay)
          }
          else {
            reject(error)
          }
        })
    }

    attempt()
  })
}
