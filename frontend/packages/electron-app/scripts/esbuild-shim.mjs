import { Buffer } from 'node:buffer'
import fs from 'node:fs'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const esbuild = require('esbuild')

function transpileConfig(source) {
  return source
    .replace(/import\s+\{\s*defineConfig\s*\}\s+from\s+['"]electron-vite['"];?/, 'const { defineConfig } = require(\'electron-vite\')')
    .replace(/export\s+default\s+defineConfig\s*\(/, 'module.exports = defineConfig(')
    .replace(/export\s+default\s+/, 'module.exports = ')
}

export async function build(options = {}) {
  const entry = Array.isArray(options.entryPoints) ? options.entryPoints[0] : undefined
  if (
    options.write === false
    && typeof entry === 'string'
    && /electron\.vite\.config\.[cm]?[jt]s$/.test(entry.replace(/\\/g, '/'))
  ) {
    const source = fs.readFileSync(entry, 'utf8')
    const text = options.format === 'esm' ? source : transpileConfig(source)
    return {
      outputFiles: [{ text }],
      metafile: {
        inputs: {
          [entry]: { bytes: Buffer.byteLength(source) },
        },
      },
    }
  }

  return esbuild.build(options)
}

export const analyzeMetafile = esbuild.analyzeMetafile
export const analyzeMetafileSync = esbuild.analyzeMetafileSync
export const buildSync = esbuild.buildSync
export const context = esbuild.context
export default { ...esbuild, build }
export const formatMessages = esbuild.formatMessages
export const formatMessagesSync = esbuild.formatMessagesSync
export const initialize = esbuild.initialize
export const transform = esbuild.transform
export const transformSync = esbuild.transformSync
export const version = esbuild.version
