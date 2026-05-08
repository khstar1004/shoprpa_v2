import path from 'node:path'

import { defineConfig } from 'vite'

const packageRoot = process.cwd()

export default defineConfig({
  publicDir: false,
  define: {
    __BUILD_MODE__: JSON.stringify('chromium'),
  },
  resolve: {
    alias: {
      '@': path.join(packageRoot, 'src'),
    },
  },
  build: {
    emptyOutDir: true,
    minify: 'terser',
    outDir: 'dist-bridge-inject',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
      },
      mangle: {
        keep_classnames: true,
        keep_fnames: true,
      },
    },
    rollupOptions: {
      input: {
        backgroundInject: 'src/background/backgroundInject.ts',
        contentInject: 'src/content/contentInject.ts',
      },
      output: {
        assetFileNames: '[name].[extname]',
        chunkFileNames: '[name].js',
        entryFileNames: '[name].js',
      },
    },
  },
})
