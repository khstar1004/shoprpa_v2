import fs from 'node:fs'
import path from 'node:path'

import archiver from 'archiver'
import { defineConfig, loadEnv } from 'vite'

import { generateManifest } from './manifest.ts'
import pkg from './package.json'

export default defineConfig((env) => {
  const environment = loadEnv(env.mode, process.cwd(), '')
  const keepConsole = env.mode === 'debug' || env.mode === 'development'
  const outDir = env.mode === 'publish' ? 'dist-publish' : 'dist'
  const publicDir = path.resolve(process.cwd(), 'public')
  const manifest = generateManifest(env.mode, environment)

  const stageExtensionAssets = (distDir) => {
    fs.mkdirSync(path.join(distDir, 'static'), { recursive: true })
    fs.cpSync(path.join(publicDir, 'static'), path.join(distDir, 'static'), {
      recursive: true,
      force: true,
    })
    fs.copyFileSync(path.join(publicDir, 'rpa.css'), path.join(distDir, 'rpa.css'))
    fs.writeFileSync(path.join(distDir, 'manifest.json'), JSON.stringify(manifest, null, 2))
  }

  const zipExtension = async (distDir) => {
    const zipName = `ShopRPA-browser-extension-v3-${pkg.version}-${env.mode}.zip`
    const output = fs.createWriteStream(zipName)
    const archive = archiver('zip', {
      zlib: { level: 9 },
    })

    await new Promise((resolve, reject) => {
      output.on('close', () => resolve(undefined))
      output.on('error', reject)
      archive.on('error', reject)
      archive.pipe(output)
      archive.directory(distDir, false)
      archive.finalize().catch(reject)
    })
  }

  return {
    publicDir: false,
    define: {
      __BUILD_MODE__: JSON.stringify(env.mode),
    },
    resolve: {
      alias: {
        '@': '/src',
      },
    },
    build: {
      minify: 'terser',
      outDir,
      emptyOutDir: true,
      assetsDir: 'src/static',

      terserOptions: {
        module: false,
        compress: {
          drop_debugger: true,
          drop_console: !keepConsole,
        },
        mangle: {
          keep_classnames: true,
          keep_fnames: true,
        },
      },
      rollupOptions: {
        input: {
          background: 'src/background.ts',
          content: 'src/content.ts',
        },
        output: {
          entryFileNames: '[name].js',
          chunkFileNames: '[name].js',
          assetFileNames: '[name].[extname]',
        },
        plugins: [
          {
            name: 'extension-plugin',
            async writeBundle(options) {
              const distDir = path.resolve(process.cwd(), options.dir || outDir)
              stageExtensionAssets(distDir)
              await zipExtension(distDir)
            },
          },
        ],
      },
    },
    test: {
      name: 'background',
      environment: 'node',
      setupFiles: ['./tests/chrome.vitest.js'],
      include: [
        'src/test/background.*.test.js',
        'src/test/background.*.spec.js',
      ],
      exclude: [
        'src/test/content.*.test.js',
        'src/test/content.*.spec.js',
      ],
      coverage: {
        provider: 'v8',
      },
    },
  }
})
