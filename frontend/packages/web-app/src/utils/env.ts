const ENV = import.meta.env

export const isDev = ENV.DEV
export const isProd = ENV.PROD

// 게시
export const isPublish = ENV.MODE === 'publish'

export const baseUrl = location.origin