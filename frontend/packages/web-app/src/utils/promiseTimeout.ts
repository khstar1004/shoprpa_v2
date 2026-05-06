class TimeoutError extends Error {
  constructor() {
    super('TimeoutError')
  }
}

/**
 *  promise 시간 초과
 * @param promise 필요시간 초과의 promise
 * @param timeoutMillis 시간 초과시간
 * @returns Promise<T>
 */
export function promiseTimeout<T>(promise: Promise<T>, timeoutMillis: number, params?: { default?: T }): Promise<T> {
  const error = new TimeoutError()

  let timeout: ReturnType<typeof setTimeout>

  return Promise.race([
    promise,
    new Promise<T>((resolve, reject) => {
      const defaultResult = params?.default
      timeout = setTimeout(() => defaultResult ? resolve(defaultResult) : reject(error), timeoutMillis)
    }),
  ]).then((v) => {
    clearTimeout(timeout)
    return v
  }, (err) => {
    clearTimeout(timeout)
    throw err
  })
}