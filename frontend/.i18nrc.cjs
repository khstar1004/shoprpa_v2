const { env } = require('node:process')

const { defineConfig } = require('@lobehub/i18n-cli')

const modelName = env.OPENAI_MODEL_NAME || 'azure/gpt-4o-mini'

module.exports = defineConfig({
  modelName,
  entry: 'locales/zh-CN.json',
  entryLocale: 'zh-CN',
  output: 'locales',
  outputLocales: ['en-US'],
  saveImmediately: true, // 매개항목항목성공후항목저장결과
  concurrency: 5,
  temperature: 0.3,
})