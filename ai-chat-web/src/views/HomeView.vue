<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import ModalDialog from '../components/ModalDialog.vue'
import { api, body, formatDate, notify, session } from '../lib/api'
import type { Candidate, Topic, Tree, TreeSummary } from '../types'

const route = useRoute()
const router = useRouter()
const topics = ref<Topic[]>([])
const trees = ref<TreeSummary[]>([])
const loading = ref(true)
const loadError = ref('')
const search = ref('')
const busy = ref(false)
const custom = ref(false)
const input = ref('')
const candidates = ref<Candidate[]>([])
const customName = ref('')
const customDescription = ref('')
const customError = ref('')
const selectedTopic = ref<Topic | null>(null)
const createName = ref('')
const createDescription = ref('')
const aiReady = ref(true)
const treeTab = computed(() => route.query.tab === 'trees')
const visibleTopics = computed(() =>
  topics.value.filter((t) => `${t.name}${t.description}`.includes(search.value)),
)
const visibleTrees = computed(() =>
  trees.value.filter((t) => `${t.title}${t.description}`.includes(search.value)),
)
const nodeCount = computed(() => trees.value.reduce((sum, t) => sum + t.nodeCount, 0))

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const [loadedTopics, loadedTrees, aiStatus] = await Promise.all([
      api<Topic[]>('/topics'),
      api<TreeSummary[]>('/trees'),
      api<{ configured: boolean }>('/ai/status'),
    ])
    topics.value = loadedTopics
    trees.value = loadedTrees
    aiReady.value = aiStatus.configured
  } catch (e) {
    loadError.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

function openCustom() {
  custom.value = true
  customError.value = ''
}

function chooseCandidate(candidate: Candidate) {
  customName.value = candidate.name
  customDescription.value = candidate.description
}

function chooseTopic(topic: Topic) {
  selectedTopic.value = topic
  createName.value = topic.name
  createDescription.value = topic.description
  customError.value = ''
}

async function create(name: string, description: string, topicId?: string) {
  busy.value = true
  customError.value = ''
  try {
    const tree = await api<Tree>('/trees', {
      method: 'POST',
      body: body({ name, description, topicId }),
    })
    router.push(`/trees/${tree.id}`)
  } catch (e) {
    customError.value = (e as Error).message
  } finally {
    busy.value = false
  }
}

/** 输入分析只生成主题候选，用户确认名称与范围后才创建知识树。 */
async function extract() {
  busy.value = true
  customError.value = ''
  try {
    candidates.value = await api<Candidate[]>('/ai/extract', {
      method: 'POST',
      body: body({ text: input.value }),
    })
  } catch (e) {
    customError.value = (e as Error).message
  } finally {
    busy.value = false
  }
}

async function copy(tree: TreeSummary) {
  busy.value = true
  try {
    const result = await api<Tree>(`/trees/${tree.id}/copy`, { method: 'POST' })
    notify('已创建副本')
    router.push(`/trees/${result.id}`)
  } catch (e) {
    notify((e as Error).message, 'error')
  } finally {
    busy.value = false
  }
}
onMounted(load)
</script>
<template>
  <header class="topbar">
    <div class="breadcrumb">
      <AppIcon :name="treeTab ? 'FolderTree' : 'Layers'" :size="17" />
      <span>工作空间</span>
      <AppIcon name="ChevronRight" :size="14" />
      <strong>{{ treeTab ? '我的知识树' : '主题探索' }}</strong>
    </div>
    <span class="topbar-note">
      <span class="tiny-dot"></span>
      独立保存 · 随时继续
    </span>
  </header>
  <div class="home-page">
    <section class="page-heading">
      <div>
        <span class="eyebrow">
          {{ treeTab ? 'MY KNOWLEDGE TREES' : 'A SMALL IDEA. A WORLD OF KNOWLEDGE.' }}
        </span>
        <!-- 标点与正文保持相邻，避免模板换行变成可见空格。 -->
        <!-- prettier-ignore -->
        <h1>
          {{ treeTab ? '我的知识树' : '从一个主题，开始探索' }}<span v-if="!treeTab" class="heading-dot">.</span>
        </h1>
        <p>
          {{
            treeTab
              ? '每一次深入，都让知识结构更清晰。'
              : `${session.user?.name}，选择一个科普方向，或让 AI 帮你梳理新的想法。`
          }}
        </p>
      </div>
      <button class="button primary" @click="openCustom">
        <AppIcon name="Plus" :size="18" />
        创建自定义主题
      </button>
    </section>
    <div v-if="loadError" class="error-panel" role="alert">
      <AppIcon name="AlertCircle" />
      <p>{{ loadError }}</p>
      <button class="button" @click="load">重新加载</button>
    </div>
    <template v-else>
      <div class="stats-strip">
        <div>
          <span class="stat-icon"><AppIcon name="FolderTree" /></span>
          <span>
            <strong>{{ trees.length }}</strong>
            <small>我的知识树</small>
          </span>
        </div>
        <div>
          <span class="stat-icon"><AppIcon name="Network" /></span>
          <span>
            <strong>{{ nodeCount }}</strong>
            <small>已构建节点</small>
          </span>
        </div>
        <div>
          <span class="stat-icon"><AppIcon name="BookOpen" /></span>
          <span>
            <strong>{{ topics.length }}</strong>
            <small>精选科普方向</small>
          </span>
        </div>
        <div class="stats-note">
          <AppIcon name="Sparkles" :size="18" />
          <span>
            AI 协助拆分，
            <br />
            探索深度由你决定。
          </span>
        </div>
      </div>
      <div v-if="!aiReady" class="soft-alert">
        <AppIcon name="CircleHelp" :size="18" />
        AI 服务尚未配置。你可以先选题、手动建立结构；管理员配置后即可使用智能细分。
      </div>
      <section v-if="!treeTab" class="topic-section">
        <div class="section-heading">
          <div>
            <h2>
              发现科普方向
              <span class="count-badge">{{ topics.length }}</span>
            </h2>
            <p>一个宽广的起点，无数值得探索的细节。</p>
          </div>
          <label class="search-field">
            <AppIcon name="Search" :size="17" />
            <input v-model="search" placeholder="搜索主题或关键词" aria-label="搜索主题" />
          </label>
        </div>
        <div v-if="loading" class="loading-state">
          <AppIcon name="LoaderCircle" class="spin" />
          正在加载主题…
        </div>
        <div v-else-if="!visibleTopics.length" class="empty-state">
          <AppIcon name="Search" :size="30" />
          <h3>没有找到相关主题</h3>
          <p>试试其他关键词，或创建自定义主题。</p>
        </div>
        <div v-else class="topic-grid">
          <button
            v-for="(topic, index) in visibleTopics"
            :key="topic.id"
            class="topic-card"
            @click="chooseTopic(topic)"
          >
            <div class="topic-card-top">
              <span
                class="topic-icon"
                :style="{ color: topic.color, backgroundColor: `${topic.color}12` }"
              >
                <AppIcon :name="topic.icon" :size="26" />
              </span>
              <span class="topic-number">{{ String(index + 1).padStart(2, '0') }}</span>
            </div>
            <h3>{{ topic.name }}</h3>
            <p>{{ topic.description }}</p>
            <div class="topic-card-bottom">
              <span>开始构建知识树</span>
              <AppIcon name="ArrowUpRight" :size="18" />
            </div>
          </button>
        </div>
      </section>
      <section class="recent-section">
        <div class="section-heading">
          <div>
            <h2>
              {{ treeTab ? '全部知识树' : '最近的探索' }}
              <span class="count-badge">{{ trees.length }}</span>
            </h2>
            <p>自动保留每一次已保存的修改。</p>
          </div>
          <label v-if="treeTab" class="search-field">
            <AppIcon name="Search" :size="17" />
            <input v-model="search" placeholder="搜索我的知识树" aria-label="搜索知识树" />
          </label>
          <RouterLink v-else-if="trees.length > 3" to="/?tab=trees" class="text-button">
            查看全部
            <AppIcon name="ArrowRight" :size="16" />
          </RouterLink>
        </div>
        <div v-if="!loading && !visibleTrees.length" class="empty-tree">
          <span class="empty-icon"><AppIcon name="Sprout" :size="27" /></span>
          <div>
            <h3>{{ search ? '没有匹配的知识树' : '你的第一棵知识树，正在等待萌芽' }}</h3>
            <p>从一个主题开始，逐步建立属于你的知识结构。</p>
          </div>
          <button class="button" @click="custom = true">
            开始探索
            <AppIcon name="ArrowRight" :size="16" />
          </button>
        </div>
        <div v-else class="tree-list">
          <article
            v-for="tree in treeTab ? visibleTrees : visibleTrees.slice(0, 3)"
            :key="tree.id"
            class="tree-list-row"
          >
            <span class="tree-list-icon"><AppIcon name="FolderTree" /></span>
            <RouterLink :to="`/trees/${tree.id}`" class="tree-list-title">
              <h3>{{ tree.title }}</h3>
              <p>{{ tree.description }}</p>
            </RouterLink>
            <span class="node-count">{{ tree.nodeCount }} 个节点</span>
            <time>{{ formatDate(tree.updatedAt) }}</time>
            <button
              class="icon-button"
              :disabled="busy"
              :aria-label="`复制 ${tree.title}`"
              @click="copy(tree)"
            >
              <AppIcon name="Copy" :size="17" />
            </button>
            <RouterLink
              :to="`/trees/${tree.id}`"
              class="icon-button"
              :aria-label="`打开 ${tree.title}`"
            >
              <AppIcon name="ArrowRight" :size="18" />
            </RouterLink>
          </article>
        </div>
      </section>
    </template>
    <footer class="page-footer">
      <span>TOPIC ATLAS</span>
      让知识有脉络，让内容有起点。
    </footer>
  </div>
  <ModalDialog
    v-if="selectedTopic"
    title="开启新的主题探索"
    @close="!busy && (selectedTopic = null)"
  >
    <form @submit.prevent="create(createName, createDescription, selectedTopic!.id)">
      <p class="muted modal-intro">按你的业务需要调整范围，下一步开始逐节点细分。</p>
      <label>
        主题名称
        <input v-model="createName" required maxlength="100" />
      </label>
      <label>
        主题范围
        <textarea v-model="createDescription" rows="4" maxlength="2000" />
      </label>
      <p v-if="customError" class="inline-error" role="alert">{{ customError }}</p>
      <div class="modal-actions">
        <button type="button" class="button" :disabled="busy" @click="selectedTopic = null">
          取消
        </button>
        <button class="button primary" :disabled="busy">
          <AppIcon :name="busy ? 'LoaderCircle' : 'Sprout'" :class="{ spin: busy }" :size="18" />
          创建知识树
        </button>
      </div>
    </form>
  </ModalDialog>
  <ModalDialog
    v-if="custom"
    title="把一个想法，变成探索的起点"
    wide
    @close="!busy && (custom = false)"
  >
    <div class="custom-topic-layout">
      <section>
        <h3>
          <AppIcon name="Sparkles" :size="18" />
          让 AI 提取主题
        </h3>
        <p class="muted">描述你想梳理的领域、受众或业务目标。</p>
        <textarea
          v-model="input"
          rows="4"
          maxlength="3000"
          placeholder="例如：面向中老年人的慢病科普，希望梳理从疾病认知到日常管理的知识结构。"
          aria-label="描述你的想法"
        />
        <button
          class="button primary"
          :disabled="busy || !input.trim() || !aiReady"
          @click="extract"
        >
          <AppIcon :name="busy ? 'LoaderCircle' : 'Sparkles'" :class="{ spin: busy }" :size="17" />
          {{ busy ? '正在理解你的想法…' : '分析并提取 Topic' }}
        </button>
        <div v-if="candidates.length" class="extracted-list">
          <p class="muted">选择一个方向，右侧可继续修改后创建。</p>
          <button
            v-for="candidate in candidates"
            :key="candidate.name"
            class="candidate-choice"
            @click="chooseCandidate(candidate)"
          >
            <strong>{{ candidate.name }}</strong>
            <span>{{ candidate.description }}</span>
            <AppIcon name="ArrowRight" :size="16" />
          </button>
        </div>
      </section>
      <form @submit.prevent="create(customName, customDescription)">
        <h3>
          <AppIcon name="Pencil" :size="18" />
          确认主题
        </h3>
        <p class="muted">也可以直接填写，自主开始构建。</p>
        <label>
          主题名称
          <input v-model="customName" required maxlength="100" placeholder="例如：睡眠科学" />
        </label>
        <label>
          范围说明
          <textarea
            v-model="customDescription"
            rows="4"
            maxlength="2000"
            placeholder="这棵知识树应该包含什么？面向谁？"
          />
        </label>
        <button class="button primary" :disabled="busy || !customName.trim()">
          创建知识树
          <AppIcon name="ArrowRight" :size="17" />
        </button>
      </form>
    </div>
    <p v-if="customError" class="inline-error" role="alert">{{ customError }}</p>
  </ModalDialog>
</template>
