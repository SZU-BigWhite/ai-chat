type RequestLog = {
  method: string
  route: string
  status: number
  durationMs: number
  requestId?: string | null
}

/** 请求日志只接收白名单字段，禁止传入表单、令牌和 AI 内容。 */
export function logRequest(entry: RequestLog) {
  if (entry.status === 0 || entry.status >= 500) {
    console.error('[API] 请求失败', entry)
  } else if (entry.status >= 400) {
    console.warn('[API] 请求被拒绝', entry)
  } else if (import.meta.env.DEV) {
    console.debug('[API] 请求完成', entry)
  }
}

/** 生产环境也保留异常类型；不输出可能包含业务数据的异常对象和消息。 */
export function logClientError(source: string, error: unknown) {
  console.error('[UI] 未处理异常', {
    source,
    errorType: error instanceof Error ? error.name : typeof error,
  })
}
