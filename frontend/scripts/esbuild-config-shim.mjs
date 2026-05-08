import { Buffer } from 'node:buffer'
import fs from 'node:fs'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const esbuild = require('esbuild')
const ts = require('typescript')

const CONFIG_RE = /(?:^|\/)(?:vitest|vite(?:\.[^/]+)?|electron\.vite|tsdown)\.config\.[cm]?[jt]s$/

function normalizePath(value = '') {
  return value.replace(/\\/g, '/')
}

function isConfigBundle(options = {}) {
  const entry = Array.isArray(options.entryPoints) ? options.entryPoints[0] : undefined
  return options.write === false && typeof entry === 'string' && CONFIG_RE.test(normalizePath(entry))
}

function toCjs(source) {
  return source
    .replace(/import\s+\{\s*defineConfig\s*\}\s+from\s+['"]vitest\/config['"];?/, 'const { defineConfig } = require(\'vitest/config\')')
    .replace(/import\s+\{\s*defineConfig\s*\}\s+from\s+['"]vite['"];?/, 'const { defineConfig } = require(\'vite\')')
    .replace(/import\s+\{\s*defineConfig\s*\}\s+from\s+['"]electron-vite['"];?/, 'const { defineConfig } = require(\'electron-vite\')')
    .replace(/import\s+\{\s*defineConfig\s*\}\s+from\s+['"]tsdown['"];?/, 'const { defineConfig } = require(\'tsdown\')')
    .replace(/export\s+default\s+defineConfig\s*\(/, 'module.exports = defineConfig(')
    .replace(/export\s+default\s+/, 'module.exports = ')
}

function transpileWithTypeScript(source, options = {}) {
  const loader = options.loader || 'js'
  const isTypescript = loader === 'ts' || loader === 'tsx'
  const isJsx = loader === 'tsx' || loader === 'jsx'
  const emptyMap = JSON.stringify({
    mappings: '',
    sources: options.sourcefile ? [options.sourcefile] : [],
    sourcesContent: options.sourcefile ? [source] : [],
    version: 3,
  })

  if (!isTypescript) {
    return {
      code: source,
      map: options.sourcemap ? emptyMap : '',
      warnings: [],
    }
  }

  const result = ts.transpileModule(source, {
    compilerOptions: {
      jsx: isJsx ? ts.JsxEmit.ReactJSX : ts.JsxEmit.Preserve,
      module: ts.ModuleKind.ESNext,
      sourceMap: Boolean(options.sourcemap),
      target: ts.ScriptTarget.ES2020,
    },
    fileName: options.sourcefile,
  })

  return {
    code: result.outputText,
    map: result.sourceMapText || (options.sourcemap ? emptyMap : ''),
    warnings: [],
  }
}

export async function build(options = {}) {
  if (isConfigBundle(options)) {
    const entry = options.entryPoints[0]
    const source = fs.readFileSync(entry, 'utf8')
    const text = options.format === 'esm' ? source : toCjs(source)

    return {
      outputFiles: [{ text }],
      warnings: [],
      metafile: {
        inputs: {
          [entry]: { bytes: Buffer.byteLength(source) },
        },
      },
    }
  }

  return esbuild.build(options)
}

export async function transform(source, options = {}) {
  return transpileWithTypeScript(source, options)
}

export function transformSync(source, options = {}) {
  return transpileWithTypeScript(source, options)
}

export const analyzeMetafile = esbuild.analyzeMetafile
export const analyzeMetafileSync = esbuild.analyzeMetafileSync
export const buildSync = esbuild.buildSync
export const context = esbuild.context
export default { ...esbuild, build, transform, transformSync }
export const formatMessages = esbuild.formatMessages
export const formatMessagesSync = esbuild.formatMessagesSync
export const initialize = esbuild.initialize
export const version = esbuild.version
