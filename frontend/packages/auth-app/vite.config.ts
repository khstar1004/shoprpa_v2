import { fileURLToPath } from 'node:url'

import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import { defineConfig } from 'vite'
import { svg4VuePlugin } from 'vite-plugin-svg4vue'

const basePublic = fileURLToPath(new URL('../../public', import.meta.url))
const proxyTarget = process.env.SHOPRPA_AUTH_PROXY_TARGET || 'http://127.0.0.1:8080'

// https://vite.dev/config/
export default defineConfig({
  publicDir: basePublic,
  base: './',
  plugins: [
    vue({
      include: [/\.vue$/, /\.md$/],
    }),
    vueJsx(),
    svg4VuePlugin({
      assetsDirName: false,
      svgoConfig: false,
    }),
  ],
  build: {
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
      },
    },
  },
  server: {
    hmr: true,
    watch: {
      usePolling: false,
      ignored: ['!**/node_modules/@rpa/**'],
    },
    host: true,
    port: 3000,
    cors: true,
    proxy: {
      '/api': {
        target: proxyTarget,
        changeOrigin: true,
        secure: false,
        // rewrite: path => path.replace(/^\/api/, ''),
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  optimizeDeps: {
    exclude: ['@rpa/components', '@rpa/shared'],
    include: ['vue', 'ant-design-vue'],
    force: true,
  },
  esbuild: {
    target: 'esnext',
  },
})
