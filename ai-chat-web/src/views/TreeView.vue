<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import ModalDialog from '../components/ModalDialog.vue'
import TreeBranch from '../components/TreeBranch.vue'
import { api, ApiError, body, downloadJson, formatDate, notify } from '../lib/api'
import type { Candidate, ExportVersion, KnowledgeNode, Suggestion, Tree } from '../types'

const route = useRoute()
const router = useRouter()
const id = String(route.params.id)
// 服务端树、当前选择与候选建议分开保存，生成建议不会直接修改正式节点。
const tree = ref<Tree | null>(null)
const selectedId = ref('')
const pending = ref<Suggestion[]>([])
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const aiReady = ref(true)

// 当前节点的编辑草稿及拆分参数。
const name = ref('')
const description = ref('')
const instruction = ref('')
const count = ref(5)
const search = ref('')
const treeKey = ref(0)

// 手动添加子节点的弹窗状态。
const manual = ref(false)
const childName = ref('')
const childDescription = ref('')
const manualError = ref('')

// 导出预览与不可变历史版本。
const exporting = ref(false)
const exportJson = ref<unknown>(null)
const versions = ref<ExportVersion[]>([])
const jsonBusy = ref(false)
const candidateEdits = ref<(Candidate & { selected: boolean })[]>([])
// 关闭弹窗只收起候选，保留本轮选择和修改，供用户重新查看。
const reviewingCandidates = ref(false)
const candidateError = ref('')

function flatten(node: KnowledgeNode): KnowledgeNode[] {
  return [node, ...node.children.flatMap(flatten)]
}

const nodes = computed(() => (tree.value ? flatten(tree.value.root) : []))
const selected = computed(
  () => nodes.value.find((n) => n.id === selectedId.value) || tree.value?.root,
)
// 表单草稿与已保存节点比较，用于切换节点和离开页面时的丢失提示。
const dirty = computed(
  () =>
    !!selected.value &&
    (name.value !== selected.value.name || description.value !== selected.value.description),
)
const confirmed = computed(() => nodes.value.filter((n) => n.status === 'CONFIRMED').length)
const reviewCount = computed(() => nodes.value.filter((n) => n.status === 'REVIEW').length)
const leaves = computed(() => nodes.value.filter((n) => !n.children.length).length)
const filtered = computed(() =>
  nodes.value.filter((n) => `${n.name}${n.description}`.includes(search.value)),
)
// 只展示与当前知识树版本一致的建议，过期建议由后端同样拦截。
const suggestion = computed(() =>
  pending.value.find(
    (s) => s.nodeId === selectedId.value && s.treeRevision === tree.value?.revision,
  ),
)
const checked = computed(() => candidateEdits.value.filter((c) => c.selected))
const path = computed(() => {
  function find(n: KnowledgeNode, trail: KnowledgeNode[]): KnowledgeNode[] | null {
    if (n.id === selectedId.value) return [...trail, n]
    for (const c of n.children) {
      const result = find(c, [...trail, n])
      if (result) return result
    }
    return null
  }
  return tree.value ? find(tree.value.root, []) || [] : []
})
watch(selected, (node) => {
  name.value = node?.name || ''
  description.value = node?.description || ''
})
watch(suggestion, (value) => {
  candidateEdits.value = value?.children.map((c) => ({ ...c, selected: true })) || []
  candidateError.value = ''
  if (!value) reviewingCandidates.value = false
})

function refresh() {
  if (!dirty.value || window.confirm('刷新会放弃未保存的输入，是否继续？')) load()
}

function openManual() {
  manual.value = true
  manualError.value = ''
}

function select(nodeId: string) {
  if (
    busy.value ||
    (dirty.value && !window.confirm('当前节点有未保存的修改，确定放弃并切换节点吗？'))
  )
    return
  selectedId.value = nodeId
  error.value = ''
  instruction.value = ''
}
onBeforeRouteLeave(
  () => !busy.value && (!dirty.value || window.confirm('当前节点有未保存的修改，确定离开吗？')),
)

function unload(event: BeforeUnloadEvent) {
  if (dirty.value || busy.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}
onMounted(() => {
  load()
  window.addEventListener('beforeunload', unload)
})
onUnmounted(() => window.removeEventListener('beforeunload', unload))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [loadedTree, suggestions, aiStatus] = await Promise.all([
      api<Tree>(`/trees/${id}`),
      api<Suggestion[]>(`/trees/${id}/suggestions`),
      api<{ configured: boolean }>('/ai/status'),
    ])
    tree.value = loadedTree
    if (!selectedId.value || !flatten(loadedTree.root).some((n) => n.id === selectedId.value))
      selectedId.value = loadedTree.root.id
    pending.value = suggestions
    aiReady.value = aiStatus.configured
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

/** 统一处理树写入和版本冲突；冲突时保留草稿，供用户复制后刷新。 */
async function mutate(path: string, method: string, payload?: unknown) {
  busy.value = true
  error.value = ''
  try {
    tree.value = await api<Tree>(path, { method, ...(payload ? { body: body(payload) } : {}) })
    if (!nodes.value.some((n) => n.id === selectedId.value)) selectedId.value = tree.value.root.id
    pending.value = []
    return true
  } catch (e) {
    error.value = (e as Error).message
    if (e instanceof ApiError && e.status === 409)
      notify('其他修改已保存，请先复制当前输入，再点击刷新。', 'error')
    return false
  } finally {
    busy.value = false
  }
}

async function save(status = selected.value?.status) {
  if (!tree.value || !selected.value) return
  if (
    await mutate(`/trees/${id}/nodes/${selected.value.id}`, 'PATCH', {
      revision: tree.value.revision,
      name: name.value,
      description: description.value,
      status,
    })
  )
    notify('节点已保存')
}

/** 生成成功后打开候选弹窗，用户选择并采纳后再写入知识树。 */
async function expand() {
  if (!tree.value || !selected.value) return
  busy.value = true
  error.value = ''
  try {
    const result = await api<Suggestion>(`/trees/${id}/nodes/${selected.value.id}/expand`, {
      method: 'POST',
      body: body({
        revision: tree.value.revision,
        instruction: instruction.value,
        count: count.value,
      }),
    })
    pending.value = [result, ...pending.value.filter((s) => s.nodeId !== result.nodeId)]
    reviewingCandidates.value = true
    notify('拆分建议已生成，确认后再加入知识树')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}

async function accept() {
  if (!suggestion.value || !tree.value || busy.value || dirty.value || !checked.value.length) return
  candidateError.value = ''
  if (
    await mutate(`/trees/${id}/suggestions/${suggestion.value.id}/accept`, 'POST', {
      revision: tree.value.revision,
      children: checked.value.map(({ name, description }) => ({ name, description })),
    })
  ) {
    reviewingCandidates.value = false
    treeKey.value++
    notify('已采纳子节点，选择任意分支继续探索')
  } else candidateError.value = error.value
}

async function addChild() {
  if (!tree.value || !selected.value) return
  manualError.value = ''
  const ok = await mutate(`/trees/${id}/nodes/${selected.value.id}/children`, 'POST', {
    revision: tree.value.revision,
    name: childName.value,
    description: childDescription.value,
  })
  if (ok) {
    manual.value = false
    childName.value = ''
    childDescription.value = ''
    treeKey.value++
    notify('子节点已添加')
  } else manualError.value = error.value
}

async function removeNode() {
  if (
    !tree.value ||
    !selected.value ||
    !window.confirm(`删除“${selected.value.name}”及其所有子节点？此操作不可撤销。`)
  )
    return
  if (
    await mutate(
      `/trees/${id}/nodes/${selected.value.id}?revision=${tree.value.revision}`,
      'DELETE',
    )
  )
    notify('分支已删除')
}

async function removeTree() {
  if (
    !tree.value ||
    !window.confirm(`删除“${tree.value.title}”整棵知识树和导出历史？此操作不可撤销。`)
  )
    return
  busy.value = true
  try {
    await api(`/trees/${id}?revision=${tree.value.revision}`, { method: 'DELETE' })
    name.value = selected.value?.name || ''
    description.value = selected.value?.description || ''
    busy.value = false
    notify('知识树已删除')
    router.push('/?tab=trees')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}

async function copyTree() {
  busy.value = true
  try {
    const result = await api<Tree>(`/trees/${id}/copy`, { method: 'POST' })
    busy.value = false
    router.push(`/trees/${result.id}`)
    notify('副本已创建')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}

async function openExport() {
  jsonBusy.value = true
  try {
    ;[exportJson.value, versions.value] = await Promise.all([
      api(`/trees/${id}/export-preview`),
      api<ExportVersion[]>(`/trees/${id}/exports`),
    ])
    exporting.value = true
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    jsonBusy.value = false
  }
}

async function exportTree(version?: ExportVersion) {
  if (!tree.value) return
  jsonBusy.value = true
  try {
    const data = version
      ? await api(`/trees/${id}/exports/${version.id}`)
      : await api(`/trees/${id}/exports?revision=${tree.value.revision}`, { method: 'POST' })
    downloadJson(
      data,
      `${tree.value.title.replace(/[\\/:*?"<>|]/g, '_')}-v${version?.treeRevision ?? tree.value.revision}.json`,
    )
    versions.value = await api<ExportVersion[]>(`/trees/${id}/exports`)
    notify('JSON 已导出，版本已记录')
  } catch (e) {
    notify((e as Error).message, 'error')
  } finally {
    jsonBusy.value = false
  }
}

async function copyJson() {
  try {
    await navigator.clipboard.writeText(JSON.stringify(exportJson.value, null, 2))
    notify('JSON 已复制')
  } catch {
    notify('浏览器无法访问剪贴板，请使用下载', 'error')
  }
}
</script>
<template>
  <header class="topbar">
    <div class="breadcrumb">
      <RouterLink to="/?tab=trees">我的知识树</RouterLink>
      <AppIcon name="ChevronRight" :size="14" />
      <strong>{{ tree?.title || '知识树工作台' }}</strong>
    </div>
    <span v-if="tree" class="topbar-note">
      <AppIcon name="CheckCheck" :size="16" />
      {{ dirty ? '有未保存的修改' : `已保存 · ${formatDate(tree.updatedAt)}` }}
    </span>
  </header>
  <div v-if="loading && !tree" class="loading-state">
    <AppIcon name="LoaderCircle" class="spin" />
    正在恢复知识树…
  </div>
  <div v-else-if="!tree" class="error-panel">
    <p>{{ error }}</p>
    <button class="button" @click="load">重新加载</button>
    <RouterLink to="/" class="button">返回主题探索</RouterLink>
  </div>
  <template v-else>
    <div class="workspace-heading">
      <div>
        <div class="eyebrow">
          KNOWLEDGE TREE
          <span class="version-chip">V{{ tree.revision }}</span>
        </div>
        <h1>{{ tree.title }}</h1>
        <p>{{ tree.description }}</p>
      </div>
      <div class="workspace-actions">
        <button
          class="icon-button"
          :disabled="busy || dirty"
          aria-label="复制知识树"
          title="复制知识树"
          @click="copyTree"
        >
          <AppIcon name="Copy" :size="18" />
        </button>
        <button
          class="icon-button danger-text"
          :disabled="busy"
          aria-label="删除知识树"
          title="删除知识树"
          @click="removeTree"
        >
          <AppIcon name="Trash2" :size="18" />
        </button>
        <button class="button primary" :disabled="busy || dirty || jsonBusy" @click="openExport">
          <AppIcon name="FileJson" :size="18" />
          预览与导出
        </button>
      </div>
    </div>
    <div class="workspace-metrics">
      <span>
        <strong>{{ nodes.length }}</strong>
        个节点
      </span>
      <span>
        <strong>{{ leaves }}</strong>
        个叶子
      </span>
      <span>
        <strong>{{ confirmed }}</strong>
        个已确认
      </span>
      <span v-if="reviewCount" class="review-text">
        <AppIcon name="AlertCircle" :size="14" />
        {{ reviewCount }} 个待复核
      </span>
      <span class="metric-hint">逐层探索 · 颗粒度由你决定</span>
    </div>
    <div class="workbench">
      <section class="tree-panel" aria-label="知识树">
        <div class="tree-toolbar">
          <h2>
            <AppIcon name="Network" :size="18" />
            知识结构
          </h2>
          <div>
            <button
              class="icon-button"
              :disabled="busy"
              aria-label="刷新知识树"
              title="刷新知识树"
              @click="refresh()"
            >
              <AppIcon name="RefreshCw" :size="17" />
            </button>
            <button class="text-button" @click="treeKey++">展开全部</button>
          </div>
        </div>
        <label class="tree-search search-field">
          <AppIcon name="Search" :size="16" />
          <input v-model="search" placeholder="查找节点…" aria-label="查找节点" />
        </label>
        <div class="tree-canvas">
          <ul v-if="!search" class="tree-root">
            <TreeBranch
              :key="treeKey"
              :node="tree.root"
              :selected-id="selectedId"
              @select="select"
            />
          </ul>
          <div v-else class="node-search-results">
            <p class="muted">找到 {{ filtered.length }} 个节点</p>
            <button
              v-for="node in filtered"
              :key="node.id"
              class="search-node"
              @click="select(node.id)"
            >
              <AppIcon name="CornerDownRight" :size="16" />
              {{ node.name }}
              <span>{{ node.children.length ? '分支' : '叶子' }}</span>
            </button>
          </div>
          <div v-if="nodes.length === 1 && !search" class="canvas-onboarding">
            <div class="onboarding-connector"></div>
            <span class="empty-icon"><AppIcon name="Sparkles" :size="24" /></span>
            <h3>第一片分支，从这里开始</h3>
            <p>
              在右侧点击「AI 继续细分」，
              <br />
              或手动添加你已经明确的知识节点。
            </p>
            <span class="small-tag">每次只展开一层，保持结构清晰</span>
          </div>
        </div>
      </section>
      <aside v-if="selected" class="node-inspector" aria-label="节点详情">
        <div class="inspector-heading">
          <span class="eyebrow">NODE DETAILS</span>
          <span class="small-tag">第 {{ path.length }} 层</span>
        </div>
        <div class="node-breadcrumb">
          <template v-for="(node, index) in path" :key="node.id">
            <AppIcon v-if="index" name="ChevronRight" :size="12" />
            <button @click="select(node.id)">
              {{ node.name }}
            </button>
          </template>
        </div>
        <p v-if="selected.status === 'REVIEW'" class="soft-alert">
          <AppIcon name="AlertCircle" :size="16" />
          父级范围发生变化，请核对本节点并重新确认。
        </p>
        <form class="node-form" @submit.prevent="save()">
          <label>
            节点名称
            <input v-model="name" required maxlength="100" :disabled="busy" />
          </label>
          <label>
            范围说明
            <textarea
              v-model="description"
              rows="3"
              maxlength="2000"
              placeholder="说明这个节点包含哪些知识、边界是什么。"
              :disabled="busy"
            />
          </label>
          <div class="node-edit-actions">
            <button class="button small" :disabled="busy || !dirty || !name.trim()">
              保存修改
            </button>
            <button
              type="button"
              class="text-button"
              :disabled="busy || dirty"
              @click="save(selected.status === 'CONFIRMED' ? 'DRAFT' : 'CONFIRMED')"
            >
              <AppIcon
                :name="selected.status === 'CONFIRMED' ? 'CheckCheck' : 'Check'"
                :size="16"
              />
              {{ selected.status === 'CONFIRMED' ? '取消确认' : '确认此节点' }}
            </button>
          </div>
          <p v-if="dirty" class="field-hint">
            先保存修改，再继续细分。修改范围后，已有子节点将提示复核。
          </p>
        </form>
        <section class="expand-section">
          <div class="section-heading compact">
            <h3>
              <AppIcon name="Sparkles" :size="18" />
              继续深入
            </h3>
            <span class="ai-label">AI 辅助</span>
          </div>
          <p class="muted">基于当前路径，生成下一层知识方向。</p>
          <label>
            本轮拆分要求
            <span class="optional">选填</span>
            <textarea
              v-model="instruction"
              rows="3"
              maxlength="2000"
              :disabled="busy"
              placeholder="例如：按疾病类型分类，避免与现有子节点重复。"
            />
          </label>
          <div class="count-select">
            <label for="branch-count">期望子节点数量</label>
            <select id="branch-count" v-model="count" :disabled="busy">
              <option v-for="n in 9" :key="n" :value="n + 1">{{ n + 1 }} 个</option>
            </select>
          </div>
          <button
            class="button primary expand-button"
            :disabled="busy || dirty || !aiReady"
            @click="expand"
          >
            <AppIcon
              :name="busy ? 'LoaderCircle' : 'Sparkles'"
              :class="{ spin: busy }"
              :size="18"
            />
            {{ busy ? '正在处理，请稍候…' : 'AI 继续细分' }}
          </button>
          <p v-if="!aiReady" class="field-hint">AI 服务尚未配置，可以先手动添加节点。</p>
          <button class="button subtle full-width" :disabled="busy || dirty" @click="openManual">
            <AppIcon name="Plus" :size="17" />
            手动添加子节点
          </button>
        </section>
        <p v-if="error" class="inline-error" role="alert">{{ error }}</p>
        <section v-if="suggestion" class="suggestion-section">
          <div class="section-heading compact">
            <h3>待确认的拆分建议</h3>
            <span class="count-badge">{{ candidateEdits.length }}</span>
          </div>
          <p class="muted">已生成 {{ candidateEdits.length }} 个候选节点，确认采纳后加入知识树。</p>
          <button
            class="button full-width"
            :disabled="busy || dirty"
            @click="reviewingCandidates = true"
          >
            查看拆分建议
          </button>
        </section>
        <div class="inspector-bottom">
          <p>
            <AppIcon name="CircleHelp" :size="16" />
            粒度合适时即可停止细分，节点会保留在最终结构中。
          </p>
          <button
            v-if="selected.id !== tree.root.id"
            class="text-button danger-text"
            :disabled="busy"
            @click="removeNode"
          >
            <AppIcon name="Trash2" :size="15" />
            删除此分支
          </button>
        </div>
      </aside>
    </div>
  </template>
  <ModalDialog
    v-if="reviewingCandidates && suggestion"
    title="本轮拆分建议"
    wide
    @close="!busy && (reviewingCandidates = false)"
  >
    <form @submit.prevent="accept">
      <p class="muted modal-intro">
        为「{{ selected?.name }}」选择下一层节点。可以修改名称和范围，确认采纳后加入知识树。
      </p>
      <div class="candidate-toolbar">
        <span class="muted" aria-live="polite">
          已选 {{ checked.length }} / {{ candidateEdits.length }} 个
        </span>
        <div>
          <button
            type="button"
            class="text-button"
            :disabled="busy"
            @click="candidateEdits.forEach((candidate) => (candidate.selected = true))"
          >
            全选
          </button>
          <button
            type="button"
            class="text-button"
            :disabled="busy"
            @click="candidateEdits.forEach((candidate) => (candidate.selected = false))"
          >
            全不选
          </button>
        </div>
      </div>
      <div
        v-for="(candidate, index) in candidateEdits"
        :key="index"
        class="suggestion-card"
        :class="{ unchecked: !candidate.selected }"
      >
        <div>
          <input
            v-model="candidate.selected"
            type="checkbox"
            :aria-label="`采纳建议 ${index + 1}`"
            :disabled="busy"
          />
          <input
            v-model="candidate.name"
            :required="candidate.selected"
            maxlength="100"
            :aria-label="`建议 ${index + 1} 名称`"
            :disabled="busy"
          />
        </div>
        <textarea
          v-model="candidate.description"
          :required="candidate.selected"
          maxlength="2000"
          rows="3"
          :aria-label="`建议 ${index + 1} 范围`"
          :disabled="busy"
        />
      </div>
      <p v-if="candidateError" class="inline-error" role="alert">{{ candidateError }}</p>
      <div class="modal-actions candidate-actions">
        <button type="button" class="button" :disabled="busy" @click="reviewingCandidates = false">
          稍后选择
        </button>
        <button class="button primary" :disabled="busy || !checked.length || dirty">
          <AppIcon :name="busy ? 'LoaderCircle' : 'Check'" :class="{ spin: busy }" :size="17" />
          {{ busy ? '正在采纳…' : `采纳选中的 ${checked.length} 个节点` }}
        </button>
      </div>
    </form>
  </ModalDialog>
  <ModalDialog v-if="manual" title="添加子节点" @close="!busy && (manual = false)">
    <form @submit.prevent="addChild">
      <p class="muted modal-intro">添加到「{{ selected?.name }}」下。</p>
      <label>
        节点名称
        <input v-model="childName" required maxlength="100" placeholder="输入具体知识方向" />
      </label>
      <label>
        范围说明
        <textarea v-model="childDescription" rows="4" maxlength="2000" />
      </label>
      <p v-if="manualError" class="inline-error" role="alert">{{ manualError }}</p>
      <div class="modal-actions">
        <button class="button" type="button" :disabled="busy" @click="manual = false">取消</button>
        <button class="button primary" :disabled="busy">添加节点</button>
      </div>
    </form>
  </ModalDialog>
  <ModalDialog
    v-if="exporting"
    title="JSON 预览与导出"
    wide
    @close="!jsonBusy && (exporting = false)"
  >
    <div class="export-intro">
      <p>按当前树结构导出，叶子节点的 children 为空数组。</p>
      <span class="small-tag">schema 1.0</span>
    </div>
    <p v-if="reviewCount" class="soft-alert">
      当前有 {{ reviewCount }} 个节点待复核。你可以继续导出，也可以返回检查。
    </p>
    <pre class="json-preview">{{ JSON.stringify(exportJson, null, 2) }}</pre>
    <div class="modal-actions">
      <button class="button" @click="copyJson">
        <AppIcon name="Copy" :size="16" />
        复制 JSON
      </button>
      <button class="button primary" :disabled="jsonBusy" @click="exportTree()">
        <AppIcon name="Download" :size="17" />
        下载并保存版本
      </button>
    </div>
    <section v-if="versions.length" class="export-history">
      <h3>导出历史</h3>
      <div v-for="version in versions" :key="version.id">
        <span>结构 V{{ version.treeRevision }}</span>
        <time>{{ formatDate(version.createdAt) }}</time>
        <button class="text-button" :disabled="jsonBusy" @click="exportTree(version)">下载</button>
      </div>
    </section>
  </ModalDialog>
</template>

<style scoped>
.candidate-toolbar,
.candidate-toolbar > div {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.candidate-toolbar {
  justify-content: space-between;
}

.candidate-actions {
  position: sticky;
  bottom: -25px;
  padding: 16px 0;
  background: #fcfdfb;
  border-top: 1px solid var(--border);
}
</style>
