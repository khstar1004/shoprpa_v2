const { existsSync, readFileSync, writeFileSync } = require('node:fs')
const { join } = require('node:path')

const YAML = require('yaml')

const parseLines = text => text.split(/\r?\n/)

// Extract KEY from KEY=VALUE lines, including commented entries.
function extractKeys(lines) {
  const set = new Set()
  lines.forEach((l) => {
    const m = l.match(/^#?\s*([\w\-.]+)\s*=/)
    if (m)
      set.add(m[1])
  })
  return set
}

// Merge template values first, then keep local-only keys.
function mergeEnvLines(tplLines, localLines) {
  const tplKeys = extractKeys(tplLines)
  const merged = []

  tplLines.forEach((line) => {
    const m = line.match(/^#?\s*([\w\-.]+)\s*=(.*)$/)
    if (m) {
      merged.push(`${m[1]}=${m[2]}`)
    }
    else {
      merged.push(line)
    }
  })

  localLines.forEach((line) => {
    const m = line.match(/^#?\s*([\w\-.]+)\s*=/)
    if (m && !tplKeys.has(m[1])) {
      merged.push(line)
    }
    else if (!m) {
      merged.push(line)
    }
  })

  return merged
}

// Convert .env lines into the YAML config payload.
const KEY_MAP = {
  VITE_AUTH_TYPE: 'app_auth_type',
  VITE_EDITION: 'app_edition',
}

function envLinesToObj(lines) {
  const out = {}
  lines.forEach((line) => {
    const m = line.match(/^\s*([^#\s][\w\-.]+)\s*=(.*)$/)
    if (!m)
      return
    let raw = (m[2] || '').trim()
    if ((raw.startsWith('"') && raw.endsWith('"'))
      || (raw.startsWith('\'') && raw.endsWith('\''))) {
      raw = raw.slice(1, -1)
    }
    const lower = raw.toLowerCase()
    let value
    if (lower === 'true' || lower === 'false')
      value = lower === 'true'
    else if (/^-?\d+(\.\d+)?$/.test(raw))
      value = Number(raw)
    else value = raw

    const key = KEY_MAP[m[1].trim()] || m[1].trim()
    out[key] = value
  })
  return out
}

// YAML helpers
function readYamlOrEmpty(path) {
  if (!existsSync(path))
    return {}
  try {
    const parsed = YAML.parse(readFileSync(path, 'utf-8'))
    return parsed && typeof parsed === 'object' ? parsed : {}
  }
  catch (e) {
    console.warn(`⚠️  ${path} 파일을 읽지 못해 빈 설정으로 계속합니다: ${e.message}`)
    return {}
  }
}

function writeYaml(path, obj) {
  writeFileSync(path, YAML.stringify(obj), 'utf-8')
}

function main() {
  const [, , mode = 'opensource'] = process.argv
  const ENV_FILE = join(__dirname, '.env')
  const ENV_CONF = join(__dirname, `.env.${mode}`)
  const CONF_YAML = join(__dirname, '../../../resources/conf.yaml')

  if (!existsSync(ENV_CONF)) {
    console.error(`설정 파일을 찾을 수 없습니다: ${ENV_CONF}`)
    process.exit(1)
  }

  const tplLines = parseLines(readFileSync(ENV_CONF, 'utf-8'))
  const localLines = existsSync(ENV_FILE)
    ? parseLines(readFileSync(ENV_FILE, 'utf-8'))
    : []

  const mergedLines = mergeEnvLines(tplLines, localLines)
  writeFileSync(ENV_FILE, mergedLines.join('\n'), 'utf-8')
  console.info(`.env 업데이트 완료`)

  const confFromEnv = envLinesToObj(tplLines)
  const existingConf = readYamlOrEmpty(CONF_YAML)
  const mergedConf = { ...existingConf, ...confFromEnv }
  writeYaml(CONF_YAML, mergedConf)
  console.info(`${CONF_YAML} 업데이트 완료`)
}

try {
  main()
}
catch (err) {
  console.error(`설정 업데이트 실패: `, err.message)
  process.exit(1)
}
