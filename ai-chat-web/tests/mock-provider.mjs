// Local test upstream only. Not imported by the application or used in production.
import http from 'node:http'
const server = http.createServer(async (req, res) => {
  if (req.url === '/health') {
    res.writeHead(200)
    res.end('ok')
    return
  }
  if (req.url !== '/chat/completions') {
    res.writeHead(404)
    res.end()
    return
  }
  const chunks = []
  for await (const chunk of req) chunks.push(chunk)
  const request = JSON.parse(Buffer.concat(chunks).toString('utf8'))
  const system = request.messages.find((m) => m.role === 'system')?.content || ''
  const input = request.messages.find((m) => m.role === 'user')?.content || ''
  const extraction = system.includes('主题提取助手')
  let children
  if (extraction)
    children = [
      { name: '睡眠科学', description: '面向公众，梳理睡眠机制、睡眠习惯和常见误区。' },
      { name: '运动健康', description: '运动原理与日常健康的知识结构。' },
    ]
  else {
    const context = JSON.parse(input)
    children =
      context.path.length === 1
        ? [
            { name: '基础认知', description: '梳理领域的基础概念、定义及知识边界。' },
            { name: '日常管理', description: '梳理与日常生活相关的知识方向。' },
            { name: '检查知识', description: '梳理常见检查的用途和基础概念。' },
          ]
        : [
            { name: '术语释义', description: '解释本分支的关键术语及相互区别。' },
            { name: '常见误区', description: '整理本分支常见的认知误区与适用边界。' },
          ]
  }
  res.writeHead(200, { 'Content-Type': 'application/json' })
  res.end(
    JSON.stringify({
      id: 'e2e-local',
      object: 'chat.completion',
      created: Math.floor(Date.now() / 1000),
      model: 'local-test-model',
      choices: [
        {
          index: 0,
          message: {
            role: 'assistant',
            content: JSON.stringify({ children, reason: '本地测试分类' }),
          },
          finish_reason: 'stop',
        },
      ],
      usage: { prompt_tokens: 100, completion_tokens: 70, total_tokens: 170 },
    }),
  )
})
server.listen(18090, '127.0.0.1')
