const defaultRequestWhiteUrl = [
  'http://127.0.0.1:1420/*',
  'http://127.0.0.1:8003/*',
  'http://127.0.0.1:8006/*',
]

const extraRequestWhiteUrl = (process.env.SHOPRPA_REQUEST_WHITE_URLS || '')
  .split(/[;,]/)
  .map(url => url.trim())
  .filter(Boolean)

const REQUEST_WHITE_URL = [...defaultRequestWhiteUrl, ...extraRequestWhiteUrl]

export const envJson = {
  REQUEST_WHITE_URL,
  SCHEDULER_NAME: 'astronverse.scheduler',
}
