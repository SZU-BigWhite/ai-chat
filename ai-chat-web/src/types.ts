export interface User {
  id: string
  email: string
  name: string
  role: 'USER' | 'ADMIN'
  enabled: boolean
  createdAt: string
}
export interface Topic {
  id: string
  name: string
  description: string
  icon: string
  color: string
  sortOrder: number
  enabled: boolean
}
export interface Candidate {
  name: string
  description: string
}
export interface KnowledgeNode extends Candidate {
  id: string
  status: 'DRAFT' | 'CONFIRMED' | 'REVIEW'
  children: KnowledgeNode[]
}
export interface TreeSummary {
  id: string
  title: string
  description: string
  revision: number
  updatedAt: string
  nodeCount: number
  confirmedCount: number
}
export interface Tree {
  id: string
  title: string
  description: string
  topicId: string | null
  revision: number
  updatedAt: string
  root: KnowledgeNode
}
export interface Suggestion {
  id: string
  nodeId: string
  treeRevision: number
  instruction: string
  children: Candidate[]
  createdAt: string
}
export interface ExportVersion {
  id: string
  treeRevision: number
  createdAt: string
}
export interface Usage {
  id: number
  userId: string
  operation: string
  model: string
  success: boolean
  totalTokens: number
  durationMs: number
  createdAt: string
}
export interface Stats {
  users: number
  trees: number
  calls: number
  recentUsage: Usage[]
}
