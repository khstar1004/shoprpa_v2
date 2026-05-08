import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    environment: 'node',
    pool: 'threads',
    setupFiles: ['./packages/browser-plugin/tests/chrome.vitest.js'],
    include: ['packages/browser-plugin/src/test/background.*.{test,spec}.js'],
    exclude: ['packages/browser-plugin/src/test/content.*.{test,spec}.js'],
    coverage: {
      provider: 'v8',
    },
  },
})
