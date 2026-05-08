const fs = require('node:fs')
const path = require('node:path')
const zlib = require('node:zlib')

const repoRoot = path.resolve(__dirname, '..')
const extensionRoot = path.join(repoRoot, 'frontend', 'packages', 'browser-plugin')
const distRoot = path.join(extensionRoot, 'dist')
const manifestPath = path.join(distRoot, 'manifest.json')
const packageJsonPath = path.join(extensionRoot, 'package.json')
const reportRoot = path.join(repoRoot, 'build', 'browser-extension-static')
const reportPath = path.join(reportRoot, 'browser-extension-static-report.json')

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'))
}

function relativeToRepo(filePath) {
  return path.relative(repoRoot, filePath).replace(/\\/g, '/')
}

function fileExistsNonEmpty(filePath) {
  return fs.existsSync(filePath) && fs.statSync(filePath).isFile() && fs.statSync(filePath).size > 0
}

function addProblem(problems, message) {
  problems.push(message)
}

function assertFile(problems, filePath, label) {
  if (!fileExistsNonEmpty(filePath)) {
    addProblem(problems, `${label} is missing or empty: ${relativeToRepo(filePath)}`)
    return false
  }
  return true
}

function assertNonEmptyString(problems, value, label) {
  if (typeof value !== 'string' || value.trim().length === 0) {
    addProblem(problems, `${label} must be a non-empty string.`)
    return false
  }
  return true
}

function findEndOfCentralDirectory(buffer) {
  const signature = 0x06054b50
  const minOffset = Math.max(0, buffer.length - 0xffff - 22)
  for (let offset = buffer.length - 22; offset >= minOffset; offset -= 1) {
    if (buffer.readUInt32LE(offset) === signature) {
      return offset
    }
  }
  return -1
}

function readZipEntries(zipPath) {
  const buffer = fs.readFileSync(zipPath)
  const eocdOffset = findEndOfCentralDirectory(buffer)
  if (eocdOffset < 0) {
    throw new Error('End of central directory not found.')
  }

  const entryCount = buffer.readUInt16LE(eocdOffset + 10)
  const centralDirectoryOffset = buffer.readUInt32LE(eocdOffset + 16)
  let offset = centralDirectoryOffset
  const entries = new Map()

  for (let index = 0; index < entryCount; index += 1) {
    if (buffer.readUInt32LE(offset) !== 0x02014b50) {
      throw new Error(`Invalid central directory signature at offset ${offset}.`)
    }

    const compressionMethod = buffer.readUInt16LE(offset + 10)
    const compressedSize = buffer.readUInt32LE(offset + 20)
    const fileNameLength = buffer.readUInt16LE(offset + 28)
    const extraLength = buffer.readUInt16LE(offset + 30)
    const commentLength = buffer.readUInt16LE(offset + 32)
    const localHeaderOffset = buffer.readUInt32LE(offset + 42)
    const fileName = buffer.toString('utf8', offset + 46, offset + 46 + fileNameLength)

    entries.set(fileName.replace(/\\/g, '/'), {
      compressionMethod,
      compressedSize,
      localHeaderOffset,
    })
    offset += 46 + fileNameLength + extraLength + commentLength
  }

  return { buffer, entries }
}

function readZipText(zipData, entryName) {
  const entry = zipData.entries.get(entryName)
  if (!entry) {
    throw new Error(`Missing zip entry: ${entryName}`)
  }
  const { buffer } = zipData
  const { compressionMethod, compressedSize, localHeaderOffset } = entry

  if (buffer.readUInt32LE(localHeaderOffset) !== 0x04034b50) {
    throw new Error(`Invalid local header signature for ${entryName}.`)
  }

  const fileNameLength = buffer.readUInt16LE(localHeaderOffset + 26)
  const extraLength = buffer.readUInt16LE(localHeaderOffset + 28)
  const dataOffset = localHeaderOffset + 30 + fileNameLength + extraLength
  const compressed = buffer.subarray(dataOffset, dataOffset + compressedSize)

  if (compressionMethod === 0) {
    return compressed.toString('utf8')
  }
  if (compressionMethod === 8) {
    return zlib.inflateRawSync(compressed).toString('utf8')
  }
  throw new Error(`Unsupported compression method ${compressionMethod} for ${entryName}.`)
}

function assertZipPackage(problems, zipPath, expectedManifest) {
  let zipData
  try {
    zipData = readZipEntries(zipPath)
  } catch (error) {
    addProblem(problems, `Browser extension zip is unreadable: ${relativeToRepo(zipPath)}; ${error.message}`)
    return
  }

  const requiredEntries = [
    'manifest.json',
    expectedManifest.background && expectedManifest.background.service_worker,
    'content.js',
    'rpa.css',
    expectedManifest.icons && expectedManifest.icons['16'],
    expectedManifest.icons && expectedManifest.icons['48'],
    expectedManifest.icons && expectedManifest.icons['128'],
  ].filter(Boolean)

  for (const entryName of requiredEntries) {
    if (!zipData.entries.has(entryName)) {
      addProblem(problems, `Browser extension zip is missing ${entryName}: ${relativeToRepo(zipPath)}`)
    }
  }

  try {
    const zippedManifest = JSON.parse(readZipText(zipData, 'manifest.json'))
    if (JSON.stringify(zippedManifest) !== JSON.stringify(expectedManifest)) {
      addProblem(problems, `Browser extension zip manifest does not match dist manifest: ${relativeToRepo(zipPath)}`)
    }
  } catch (error) {
    addProblem(problems, `Browser extension zip manifest is unreadable: ${relativeToRepo(zipPath)}; ${error.message}`)
  }
}

function main() {
  const problems = []
  const checkedFiles = []

  if (!fs.existsSync(manifestPath)) {
    addProblem(problems, `Browser extension manifest is missing: ${relativeToRepo(manifestPath)}`)
  }
  if (!fs.existsSync(packageJsonPath)) {
    addProblem(problems, `Browser extension package.json is missing: ${relativeToRepo(packageJsonPath)}`)
  }

  let manifest = {}
  let packageJson = {}
  if (problems.length === 0) {
    manifest = readJson(manifestPath)
    packageJson = readJson(packageJsonPath)
  }

  if (problems.length === 0) {
    if (manifest.manifest_version !== 3) {
      addProblem(problems, `Expected MV3 manifest, got ${manifest.manifest_version}`)
    }
    assertNonEmptyString(problems, manifest.name, 'Manifest name')
    assertNonEmptyString(problems, manifest.description, 'Manifest description')
    if (assertNonEmptyString(problems, manifest.homepage_url, 'Manifest homepage_url')) {
      try {
        const homepage = new URL(manifest.homepage_url)
        if (!['http:', 'https:'].includes(homepage.protocol)) {
          addProblem(problems, `Manifest homepage_url must use http or https: ${manifest.homepage_url}`)
        }
      } catch {
        addProblem(problems, `Manifest homepage_url is not a valid URL: ${manifest.homepage_url}`)
      }
    }
    if (manifest.name && !manifest.name.includes('ShopRPA')) {
      addProblem(problems, `Manifest name must use ShopRPA branding: ${manifest.name}`)
    }
    if (packageJson.displayName !== 'ShopRPA Browser Extension') {
      addProblem(problems, `Package displayName must be ShopRPA Browser Extension, got ${packageJson.displayName || '<missing>'}`)
    }
    if (manifest.version !== packageJson.version) {
      addProblem(problems, `Manifest version ${manifest.version} does not match package version ${packageJson.version}`)
    }
    if (!manifest.background || !manifest.background.service_worker) {
      addProblem(problems, 'Manifest background.service_worker is required.')
    }
    if (!Array.isArray(manifest.content_scripts) || manifest.content_scripts.length === 0) {
      addProblem(problems, 'Manifest content_scripts must include at least one content script.')
    }

    const requiredPermissions = [
      'alarms',
      'nativeMessaging',
      'debugger',
      'tabs',
      'activeTab',
      'webNavigation',
      'cookies',
      'storage',
      'scripting',
      'management',
    ]
    const permissions = new Set(manifest.permissions || [])
    for (const permission of requiredPermissions) {
      if (!permissions.has(permission)) {
        addProblem(problems, `Manifest permission is missing: ${permission}`)
      }
    }

    if (!Array.isArray(manifest.host_permissions) || !manifest.host_permissions.includes('<all_urls>')) {
      addProblem(problems, 'Manifest host_permissions must include <all_urls>.')
    }

    const extensionCsp = manifest.content_security_policy && manifest.content_security_policy.extension_pages
    if (!extensionCsp || !extensionCsp.includes("script-src 'self'")) {
      addProblem(problems, 'Manifest extension_pages CSP must restrict scripts to self.')
    }
    if (extensionCsp && (extensionCsp.includes("'unsafe-eval'") || extensionCsp.includes("'unsafe-inline'"))) {
      addProblem(problems, 'Manifest extension_pages CSP must not allow unsafe-eval or unsafe-inline.')
    }

    if (manifest.background && manifest.background.service_worker) {
      const backgroundPath = path.join(distRoot, manifest.background.service_worker)
      if (assertFile(problems, backgroundPath, 'Background service worker')) {
        checkedFiles.push(relativeToRepo(backgroundPath))
      }
    }

    for (const [index, script] of (manifest.content_scripts || []).entries()) {
      for (const jsFile of script.js || []) {
        const jsPath = path.join(distRoot, jsFile)
        if (assertFile(problems, jsPath, `Content script ${index} JS`)) {
          checkedFiles.push(relativeToRepo(jsPath))
        }
      }
      for (const cssFile of script.css || []) {
        const cssPath = path.join(distRoot, cssFile)
        if (assertFile(problems, cssPath, `Content script ${index} CSS`)) {
          checkedFiles.push(relativeToRepo(cssPath))
        }
      }
      const matches = script.matches || []
      for (const requiredMatch of ['http://*/*', 'https://*/*', 'file://*/*']) {
        if (!matches.includes(requiredMatch)) {
          addProblem(problems, `Content script ${index} match is missing: ${requiredMatch}`)
        }
      }
    }

    for (const size of ['16', '48', '128']) {
      const iconPath = manifest.icons && manifest.icons[size] ? path.join(distRoot, manifest.icons[size]) : ''
      if (!iconPath) {
        addProblem(problems, `Manifest icon is missing: ${size}`)
        continue
      }
      if (assertFile(problems, iconPath, `Icon ${size}`)) {
        checkedFiles.push(relativeToRepo(iconPath))
      }
    }

    const expectedZipPrefix = `ShopRPA-browser-extension-v3-${packageJson.version}-`
    const legacyZipFiles = fs.readdirSync(extensionRoot).filter(fileName => {
      return /^rpa-extension-v3-.*\.zip$/.test(fileName)
    })
    for (const legacyZipFile of legacyZipFiles) {
      addProblem(problems, `Legacy browser extension zip name must be removed or rebuilt with ShopRPA branding: ${legacyZipFile}`)
    }

    const staleShopRpaZipFiles = fs.readdirSync(extensionRoot).filter(fileName => {
      return /^ShopRPA-browser-extension-v3-.*\.zip$/.test(fileName) && !fileName.startsWith(expectedZipPrefix)
    })
    for (const staleZipFile of staleShopRpaZipFiles) {
      addProblem(problems, `Stale browser extension zip version must be removed or rebuilt for ${packageJson.version}: ${staleZipFile}`)
    }

    const zipFiles = fs.readdirSync(extensionRoot).filter(fileName => {
      return fileName.startsWith(expectedZipPrefix) && fileName.endsWith('.zip')
    })
    if (zipFiles.length === 0) {
      addProblem(problems, `Browser extension zip is missing: ${expectedZipPrefix}*.zip`)
    }
    for (const zipFile of zipFiles) {
      const zipPath = path.join(extensionRoot, zipFile)
      if (assertFile(problems, zipPath, 'Browser extension zip')) {
        assertZipPackage(problems, zipPath, manifest)
        checkedFiles.push(relativeToRepo(zipPath))
      }
    }
  }

  fs.mkdirSync(reportRoot, { recursive: true })
  const report = {
    ok: problems.length === 0,
    generatedAt: new Date().toISOString(),
    extensionPath: distRoot,
    manifestPath,
    manifestVersion: manifest.version || '',
    checkedFiles,
    problems,
  }
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2), 'utf8')

  if (problems.length > 0) {
    console.error(`Browser extension static verification failed: ${reportPath}`)
    for (const problem of problems) {
      console.error(`- ${problem}`)
    }
    process.exit(1)
  }

  console.log(`Browser extension static verification passed: ${reportPath}`)
  console.log(`checkedFiles=${checkedFiles.length}`)
}

main()
