import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

import { RpaResolver } from '@rpa/components/resolver'
import type { SentryVitePluginOptions } from '@sentry/vite-plugin'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
// import Inspect from 'vite-plugin-inspect' 열기발송시조회코드
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import Components from 'unplugin-vue-components/vite'
import { defineConfig } from 'vite'
import type { Plugin } from 'vite'
import { analyzer } from 'vite-bundle-analyzer'
import { lazyImport, VxeResolver } from 'vite-plugin-lazy-import'
import PageHtml from 'vite-plugin-page-html'
import { svg4VuePlugin } from 'vite-plugin-svg4vue'

import { ModalReplacementResolver } from './src/plugins/component-resolver.ts'

const require = createRequire(import.meta.url)
const vueVersion = require('vue/package.json').version
const piniaVersion = require('pinia/package.json').version

const baseSrc = fileURLToPath(new URL('./src', import.meta.url))
const basePublic = fileURLToPath(new URL('../../public', import.meta.url))
const nodeModules = fileURLToPath(new URL('./node_modules', import.meta.url))

const sentryConfig: SentryVitePluginOptions = {
  authToken: process.env.SENTRY_AUTH_TOKEN,
  org: process.env.SENTRY_ORG,
  project: process.env.SENTRY_PROJECT,
  url: process.env.SENTRY_URL,
}

function removeExcelJsEvalShim(): Plugin {
  return {
    name: 'shoprpa-exceljs-no-eval',
    enforce: 'pre',
    transform: {
      filter: {
        id: /@zwight[+/\\]exceljs.*[\\/](?:dist[\\/])?exceljs/,
      },
      handler(code, id) {
        const normalizedId = id.replace(/\\/g, '/')
        if (!normalizedId.includes('@zwight') || !normalizedId.includes('/exceljs/') || !normalizedId.includes('/dist/exceljs')) {
          return null
        }

        const nextCode = code
          .replace(
            'Script.prototype.runInThisContext=function(){return eval(this.code)}',
            'Script.prototype.runInThisContext=function(){throw new Error("vm disabled in browser")}',
          )
          .replace(
            'return eval(this.code); // maybe...',
            'throw new Error("vm disabled in browser");',
          )

        if (nextCode === code) {
          return null
        }

        return { code: nextCode, map: null }
      },
    },
  }
}

function injectBootContentSecurityPolicy(): Plugin {
  const quote = String.fromCharCode(39)
  const cspSelf = [quote, 'self', quote].join('')
  const cspNone = [quote, 'none', quote].join('')
  const cspUnsafeInline = [quote, 'unsafe-inline', quote].join('')
  const csp = [
    `default-src ${cspSelf}`,
    `script-src ${cspSelf}`,
    `style-src ${cspSelf} ${cspUnsafeInline}`,
    `img-src ${cspSelf} data: blob:`,
    `font-src ${cspSelf} data:`,
    `connect-src ${cspSelf} http: https: ws: wss:`,
    `object-src ${cspNone}`,
    `base-uri ${cspSelf}`,
  ].join('; ')

  return {
    name: 'shoprpa-boot-csp',
    apply: 'build',
    closeBundle() {
      const bootHtmlPath = resolve(fileURLToPath(new URL('.', import.meta.url)), 'dist/boot.html')
      if (!existsSync(bootHtmlPath))
        return

      const html = readFileSync(bootHtmlPath, 'utf8')
      if (html.includes('Content-Security-Policy'))
        return

      const meta = `<meta http-equiv="Content-Security-Policy" content="${csp}"/>`
      const nextHtml = html.replace(/<head>/i, `<head>${meta}`)
      if (nextHtml !== html)
        writeFileSync(bootHtmlPath, nextHtml)
    },
  }
}

// https://vitejs.dev/config/
export default defineConfig(async (env) => {
  const isPublish = env.mode === 'publish'
  const isDebug = env.mode === 'debug'

  const enableAnalyze = env.mode === 'analyze'
  const enableSentry = isPublish && sentryConfig.authToken && sentryConfig.org && sentryConfig.project
  const sentryPlugin = enableSentry
    ? (await import('@sentry/vite-plugin')).sentryVitePlugin(sentryConfig)
    : null

  return {
    base: env.command === 'build' ? './' : '/',
    publicDir: basePublic,
    define: {
      __VUE_VERSION__: JSON.stringify(vueVersion),
      __PINIA_VERSION__: JSON.stringify(piniaVersion),
      __SENTRY_DEBUG__: JSON.stringify(isDebug),
      ENV_TARGET: JSON.stringify('web'),
    },
    build: {
      sourcemap: isDebug ? 'inline' : isPublish,
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: !isDebug,
          drop_debugger: !isDebug,
        },
      },
    },
    plugins: [
      vue(),
      vueJsx(),
      svg4VuePlugin({
        assetsDirName: false,
        svgoConfig: false,
      }),
      // Inspect(),
      Components({
        dirs: [],
        dts: './src/components.d.ts',
        resolvers: [
          ModalReplacementResolver(), // 단계높음, 사용교체 a-modal
          AntDesignVueResolver({
            importStyle: false, // css in js
          }),
          RpaResolver(),
        ],
      }),
      lazyImport({
        resolvers: [
          VxeResolver({
            libraryName: 'vxe-table',
          }),
          VxeResolver({
            libraryName: 'vxe-pc-ui',
          }),
        ],
      }),
      PageHtml({
        template: 'src/index.html',
        page: { // Note: paths here must be absolute from project root, starting with '/src/...'
          index: '/src/views/Home/index.ts',
          boot: '/src/views/Boot/index.ts',
          logwin: '/src/views/Log/index.ts',
          batch: '/src/views/Batch/index.ts',
          multichat: '/src/views/MultiChat/index.ts',
          userform: '/src/views/UserForm/index.ts',
          record: '/src/views/Record/index.ts',
          recordmenu: '/src/views/RecordMenu/index.ts',
          smartcompickmenu: '/src/views/SmartCompPickMenu/index.ts',
        },
      }),
      injectBootContentSecurityPolicy(),
      removeExcelJsEvalShim(),
      sentryPlugin,
      enableAnalyze ? analyzer() : null,
    ],
    resolve: {
      alias: [
        {
          find: '@',
          replacement: baseSrc,
        },
        {
          find: 'dayjs',
          replacement: 'dayjs/esm',
        },
        {
          find: /^dayjs\/locale/,
          replacement: 'dayjs/esm/locale',
        },
        {
          find: /^dayjs\/plugin/,
          replacement: 'dayjs/esm/plugin',
        },
        {
          find: /^ant-design-vue\/es$/,
          replacement: resolve(nodeModules, 'ant-design-vue/es'),
        },
        {
          find: /^ant-design-vue\/dist$/,
          replacement: resolve(nodeModules, 'ant-design-vue/dist'),
        },
        {
          find: /^ant-design-vue\/lib$/,
          replacement: resolve(nodeModules, 'ant-design-vue/es'),
        },
        {
          find: /^ant-design-vue$/,
          replacement: resolve(nodeModules, 'ant-design-vue/es'),
        },
        {
          find: 'lodash',
          replacement: 'lodash-es',
        },
      ],
    },

    css: {
      preprocessorOptions: {
        scss: {
          additionalData: '@import "@rpa/shared/tokens/variables.scss";',
        },
      },
    },

    clearScreen: false,
    server: {
      port: 1420,
      strictPort: true,
      host: '0.0.0.0', // 지정모든네트워크연결입구
      watch: {
        // 3. tell vite to ignore watching `src-tauri`
        ignored: ['**/src-tauri/**', '**/node_modules/**', '**/src-electron/**', '**/dist/**', '**/dist-electron/**'],
      },
    },
  }
})
