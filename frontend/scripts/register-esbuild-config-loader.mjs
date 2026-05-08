import childProcess from 'node:child_process'
import { EventEmitter } from 'node:events'
import { register, syncBuiltinESMExports } from 'node:module'
import { PassThrough } from 'node:stream'

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

register('./esbuild-config-loader.mjs', import.meta.url)
syncBuiltinESMExports()
