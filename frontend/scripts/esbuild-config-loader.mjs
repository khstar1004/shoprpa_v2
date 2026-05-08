import fs from 'node:fs'
import { createRequire } from 'node:module'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const require = createRequire(import.meta.url)
const ts = require('typescript')

const esbuildShimUrl = new URL('./esbuild-config-shim.mjs', import.meta.url).href
const localExtensions = ['.ts', '.mts', '.mjs', '.js', '.json']

function isLocalSpecifier(specifier) {
  return specifier.startsWith('./') || specifier.startsWith('../') || specifier.startsWith('/') || specifier.startsWith('file:')
}

function tryResolveLocalFile(specifier, parentURL) {
  if (!isLocalSpecifier(specifier)) {
    return null
  }

  const basePath = specifier.startsWith('file:')
    ? fileURLToPath(specifier)
    : specifier.startsWith('/')
      ? specifier
      : path.resolve(path.dirname(fileURLToPath(parentURL)), specifier)

  const candidates = path.extname(basePath)
    ? [basePath]
    : localExtensions.map(extension => `${basePath}${extension}`)

  for (const candidate of candidates) {
    if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
      return pathToFileURL(candidate).href
    }
  }

  return null
}

export async function resolve(specifier, context, nextResolve) {
  if (specifier === 'esbuild') {
    return {
      shortCircuit: true,
      url: esbuildShimUrl,
    }
  }

  if (context.parentURL?.startsWith('file:')) {
    const localURL = tryResolveLocalFile(specifier, context.parentURL)
    if (localURL) {
      return {
        shortCircuit: true,
        url: localURL,
      }
    }
  }

  return nextResolve(specifier, context)
}

export async function load(url, context, nextLoad) {
  if (url.endsWith('.ts') || url.endsWith('.mts')) {
    const filename = fileURLToPath(url)
    const source = fs.readFileSync(filename, 'utf8')
    const result = ts.transpileModule(source, {
      compilerOptions: {
        module: ts.ModuleKind.ESNext,
        sourceMap: false,
        target: ts.ScriptTarget.ES2020,
      },
      fileName: filename,
    })

    return {
      format: 'module',
      shortCircuit: true,
      source: result.outputText,
    }
  }

  if (url.endsWith('.json')) {
    const source = fs.readFileSync(fileURLToPath(url), 'utf8')
    return {
      format: 'module',
      shortCircuit: true,
      source: `export default ${JSON.stringify(JSON.parse(source))}`,
    }
  }

  return nextLoad(url, context)
}
