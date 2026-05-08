const fs = require('node:fs')
const path = require('node:path')

const packageRoot = path.resolve(__dirname, '..')
const repoRoot = path.resolve(packageRoot, '../../..')
const sourceRoot = path.join(packageRoot, 'dist-bridge-inject')
const targetRoot = path.join(
  repoRoot,
  'engine/servers/astronverse-browser-bridge/src/astronverse/browser_bridge/inject',
)
const files = ['backgroundInject.js', 'contentInject.js']

fs.mkdirSync(targetRoot, { recursive: true })

for (const file of files) {
  const sourcePath = path.join(sourceRoot, file)
  const targetPath = path.join(targetRoot, file)
  if (!fs.existsSync(sourcePath)) {
    throw new Error(`Bridge inject build output is missing: ${sourcePath}`)
  }
  fs.copyFileSync(sourcePath, targetPath)
}

console.log(`Browser bridge inject assets synced to ${targetRoot}`)
