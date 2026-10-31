// Verifies the packaged application against an isolated on-disk database, including restart.
import { spawn } from 'node:child_process'
import { mkdir, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import assert from 'node:assert/strict'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const serverDir = path.join(root, 'ai-chat-server')
const stateDir = path.join(root, '.local', `persistence-${Date.now()}`)
await mkdir(stateDir, { recursive: true })
const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', 'java.exe') : 'java'
let child
let output = ''
const base = 'http://127.0.0.1:18082/api'
async function start() {
  child = spawn(java, [`-Djdk.net.unixdomain.tmpdir=${stateDir}`, '-Dfile.encoding=UTF-8', '-jar', path.join(serverDir, 'target/ai-chat-server-1.0.0.jar')], {
    cwd: serverDir,
    windowsHide: true,
    env: { ...process.env, SERVER_PORT: '18082', SPRING_PROFILES_ACTIVE: 'default', SPRING_DATASOURCE_URL: `jdbc:h2:file:${path.join(stateDir, 'db').replaceAll('\\', '/')};MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0`, SPRING_JPA_HIBERNATE_DDL_AUTO: 'update', DEEPSEEK_API_KEY: 'not-configured', ADMIN_EMAIL: '', ADMIN_PASSWORD: '' },
    stdio: ['ignore', 'pipe', 'pipe'],
  })
  child.stdout.on('data', data => { output += data.toString() })
  child.stderr.on('data', data => { output += data.toString() })
  for (let i = 0; i < 200; i++) {
    if (child.exitCode !== null) throw new Error(`Server exited unexpectedly: ${child.exitCode}`)
    try { if ((await fetch(`${base}/health`)).ok) return } catch {}
    await new Promise(resolve => setTimeout(resolve, 250))
  }
  throw new Error('Server startup timeout')
}
async function stop() {
  if (child && child.exitCode === null) { const done = new Promise(resolve => child.once('exit', resolve)); child.kill(); await done }
}
async function request(endpoint, token, method = 'GET', value, status = 200) {
  const response = await fetch(`${base}${endpoint}`, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }, ...(value ? { body: JSON.stringify(value) } : {}) })
  const data = await response.json()
  assert.equal(response.status, status, `${method} ${endpoint}: ${data.message || response.status}`)
  return data
}
try {
  await start()
  await request('/trees', null, 'GET', null, 401)
  const account = await request('/auth/register', null, 'POST', { email: 'persist@test.local', name: '持久化验证', password: 'PersistenceTest123!' })
  assert.equal((await request('/ai/status', account.token)).configured, false)
  await request('/ai/extract', account.token, 'POST', { text: '睡眠科学' }, 503)
  const tree = await request('/trees', account.token, 'POST', { name: '持久化知识树', description: '跨进程重启验证' })
  const updated = await request(`/trees/${tree.id}/nodes/${tree.root.id}/children`, account.token, 'POST', { revision: tree.revision, name: '持久化子节点', description: '保留在文件数据库中' })
  await stop()
  await start()
  const restored = await request(`/trees/${tree.id}`, account.token)
  assert.deepEqual(restored.root, updated.root)
  assert.equal(restored.revision, updated.revision)
  const login = await request('/auth/login', null, 'POST', { email: 'persist@test.local', password: 'PersistenceTest123!' })
  assert.equal(login.user.id, account.user.id)
  const topics = await request('/topics', login.token)
  assert.equal(topics.length, 12)
  console.log('PASS: packaged server starts, missing-key errors are explicit, accounts/sessions/tree nodes persist across restart, seed topics remain unique.')
} finally {
  await stop()
  await writeFile(path.join(stateDir, 'server.log'), output, 'utf8')
}
