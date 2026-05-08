const childProcess = require('node:child_process')
const { EventEmitter } = require('node:events')
const { PassThrough } = require('node:stream')

const originalExec = childProcess.exec

function createNoopChildProcess() {
  const child = new EventEmitter()
  child.stdin = new PassThrough()
  child.stdout = new PassThrough()
  child.stderr = new PassThrough()
  child.pid = 0
  child.killed = false
  child.kill = () => {
    child.killed = true
    return true
  }
  return child
}

childProcess.exec = function patchedExec(...args) {
  const [command, options, callback] = args
  const normalized = typeof command === 'string' ? command.trim().toLowerCase() : ''
  if (process.platform === 'win32' && normalized === 'net use') {
    const cb = typeof options === 'function' ? options : callback
    const child = createNoopChildProcess()
    process.nextTick(() => {
      cb?.(null, '', '')
      child.emit('close', 0)
      child.emit('exit', 0)
    })
    return child
  }

  return originalExec.apply(this, args)
}
