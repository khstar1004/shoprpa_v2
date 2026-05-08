import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

import tokens from './color.js'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)
const distPath = join(__dirname, '..', 'dist', 'tokens')

mkdirSync(distPath, { recursive: true })

const entries = Object.entries(tokens)

function toCssName(name) {
  return name
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .replace(/_/g, '-')
    .toLowerCase()
}

function toExportName(name) {
  return name
    .replace(/-([a-z0-9])/gi, (_, letter) => letter.toUpperCase())
    .replace(/[^a-zA-Z0-9_$]/g, '_')
    .replace(/^([^a-zA-Z_$])/, '_$1')
}

function isColor(token) {
  return token.type === 'color' || token.$type === 'color'
}

function normalizeCssValue(value) {
  return String(value).replace(/\s+/g, ' ').trim()
}

function tokenValue(token, mode = 'light') {
  if (mode === 'dark' && token.dark !== undefined) {
    return token.dark
  }
  return token.value ?? token.$value
}

function renderCssVariables(mode = 'light') {
  return entries
    .filter(([, token]) => mode === 'light' || token.dark !== undefined)
    .map(([name, token]) => `  --${toCssName(name)}: ${normalizeCssValue(tokenValue(token, mode))};`)
    .join('\n')
}

function renderScssVariables() {
  return entries
    .map(([name]) => {
      const cssName = toCssName(name)
      return `$${cssName}: var(--${cssName});`
    })
    .join('\n')
}

function renderEsmVariables(mode = 'light', cssReference = false) {
  const usedNames = new Set()

  return entries
    .map(([name, token]) => {
      let exportName = toExportName(name)
      if (usedNames.has(exportName)) {
        exportName = `${exportName}_`
      }
      usedNames.add(exportName)

      const value = cssReference ? `var(--${toCssName(name)})` : tokenValue(token, mode)
      return `export const ${exportName} = ${JSON.stringify(value)};`
    })
    .join('\n')
}

function renderCjsVariables(mode = 'light') {
  return entries
    .map(([name, token]) => `exports.${toExportName(name)} = ${JSON.stringify(tokenValue(token, mode))};`)
    .join('\n')
}

function renderTypes() {
  return entries
    .map(([name]) => `export const ${toExportName(name)}: string | number;`)
    .join('\n')
}

function renderTailwindPreset() {
  const defaultColors = {
    transparent: 'transparent',
    current: 'currentColor',
    inherit: 'inherit',
  }

  const colors = Object.fromEntries(
    entries
      .filter(([, token]) => isColor(token))
      .map(([name]) => {
        const cssName = toCssName(name)
        const tailwindName = cssName.startsWith('color-') ? cssName.slice(6) : cssName
        return [tailwindName, `var(--${cssName})`]
      }),
  )

  const payload = {
    theme: {
      colors: {
        ...defaultColors,
        ...colors,
      },
    },
  }

  return `/** @type {import('tailwindcss').Config} */\nexport default ${JSON.stringify(payload, null, 2)};\n`
}

const lightCss = `:root {\n${renderCssVariables('light')}\n}\n`
const darkCss = `.dark {\n${renderCssVariables('dark')}\n}\n`
const scss = `${renderScssVariables()}\n`

writeFileSync(join(distPath, 'variables.css'), `${lightCss}\n/* Dark mode variables */\n${darkCss}`)
writeFileSync(join(distPath, 'variables.dark.css'), darkCss)
writeFileSync(join(distPath, 'variables.scss'), scss)
writeFileSync(join(distPath, '_variables.scss'), scss)
writeFileSync(join(distPath, 'variables.js'), `${renderEsmVariables('light')}\n`)
writeFileSync(join(distPath, 'variables.cjs'), `${renderCjsVariables('light')}\n`)
writeFileSync(join(distPath, 'variables.css.js'), `${renderEsmVariables('light', true)}\n`)
writeFileSync(join(distPath, 'variables.d.ts'), `${renderTypes()}\n`)
writeFileSync(join(distPath, 'variables.dark.js'), `${renderEsmVariables('dark')}\n`)
writeFileSync(join(distPath, 'variables.dark.cjs'), `${renderCjsVariables('dark')}\n`)
writeFileSync(join(distPath, 'tailwind-preset.js'), renderTailwindPreset())

console.log(`Generated ${entries.length} design tokens in ${distPath}`)
