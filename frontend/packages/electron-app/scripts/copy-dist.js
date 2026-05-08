const fs = require('fs');
const path = require('path');

const srcDir = path.resolve(__dirname, '../../web-app/dist');
const destDir = path.resolve(__dirname, '../out/renderer');

console.log(`Copying from ${srcDir} to ${destDir}...`);

if (!fs.existsSync(srcDir)) {
  console.error(`Source directory does not exist: ${srcDir}`);
  console.error('Please build the web-app first.');
  process.exit(1);
}

// Recreate the renderer output so stale hashed assets cannot ship in portable builds.
fs.rmSync(destDir, { force: true, recursive: true });
fs.mkdirSync(destDir, { recursive: true });

// Copy files
// fs.cpSync is available in Node.js >= 16.7.0
try {
  fs.cpSync(srcDir, destDir, { recursive: true, force: true });
  console.log('Copy complete.');
} catch (err) {
  console.error('Failed to copy files:', err);
  process.exit(1);
}
