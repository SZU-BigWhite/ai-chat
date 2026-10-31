import { reactive } from 'vue'
import type { User } from '../types'
import { logRequest } from './logger'

export const session = reactive({ user: null as User | null, ready: false })

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public requestId?: string | null,
  ) {
    super(message)
  }
}

/** 统一注入会话、超时和错误处理，日志仅记录请求元信息。 */
export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const started = performance.now()
  const method = options.method || 'GET'
  // 丢弃查询参数并用占位符替换资源 ID，日志不携带用户输入。
  const route = path
    .split('?')[0]!
    .replace(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/gi, ':id')
  const token = sessionStorage.getItem('topic-atlas-token')
  let response: Response
  try {
    response = await fetch(`/api${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
      signal: options.signal ?? AbortSignal.timeout(145_000),
    })
  } catch (error) {
    logRequest({ method, route, status: 0, durationMs: Math.round(performance.now() - started) })
    throw new ApiError(
      error instanceof Error && (error.name === 'TimeoutError' || error.name === 'AbortError')
        ? '请求超时，请重试。已保存的数据不受影响。'
        : '无法连接服务，请确认后端已启动后重试。',
      0,
    )
  }
  const requestId = response.headers.get('X-Request-Id')
  logRequest({
    method,
    route,
    status: response.status,
    durationMs: Math.round(performance.now() - started),
    requestId,
  })
  const data = await response.json().catch(() => null)
  if (!response.ok) {
    if (
      response.status === 401 &&
      !path.startsWith('/auth/login') &&
      !path.startsWith('/auth/register')
    ) {
      clearSession()
      window.dispatchEvent(new Event('session-expired'))
    }
    throw new ApiError(
      data?.message || `请求失败（${response.status}）`,
      response.status,
      requestId,
    )
  }
  return data as T
}

export const body = (value: unknown) => JSON.stringify(value)

export function setSession(token: string, user: User) {
  sessionStorage.setItem('topic-atlas-token', token)
  session.user = user
  session.ready = true
}

export function clearSession() {
  sessionStorage.removeItem('topic-atlas-token')
  session.user = null
  session.ready = true
}

// 刷新页面后恢复用户信息；仅认证失败时清除令牌，网络故障不主动注销。
export async function initSession() {
  if (session.ready) return
  if (sessionStorage.getItem('topic-atlas-token')) {
    try {
      session.user = await api<User>('/auth/me')
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) clearSession()
    }
  }
  session.ready = true
}

export const notice = reactive({ text: '', kind: 'success' as 'success' | 'error' })
let noticeTimer: ReturnType<typeof setTimeout>

export function notify(text: string, kind: 'success' | 'error' = 'success') {
  clearTimeout(noticeTimer)
  notice.text = text
  notice.kind = kind
  noticeTimer = setTimeout(() => {
    notice.text = ''
  }, 6000)
}

export function downloadJson(value: unknown, filename: string) {
  const url = URL.createObjectURL(
    new Blob([JSON.stringify(value, null, 2)], { type: 'application/json;charset=utf-8' }),
  )
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
