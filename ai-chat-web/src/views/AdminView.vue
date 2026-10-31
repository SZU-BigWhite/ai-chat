<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppIcon from '../components/AppIcon.vue'
import ModalDialog from '../components/ModalDialog.vue'
import { api, body, formatDate, notify } from '../lib/api'
import type { Stats, Topic, User } from '../types'

const tab = ref('topics')
const topics = ref<Topic[]>([])
const users = ref<User[]>([])
const stats = ref<Stats | null>(null)
const error = ref('')
const loading = ref(true)
const busy = ref(false)
const editing = ref<Partial<Topic> | null>(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [loadedTopics, loadedUsers, loadedStats] = await Promise.all([
      api<Topic[]>('/admin/topics'),
      api<User[]>('/admin/users'),
      api<Stats>('/admin/stats'),
    ])
    topics.value = loadedTopics
    users.value = loadedUsers
    stats.value = loadedStats
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

// 使用独立副本编辑，取消弹窗不会污染列表里已保存的主题。
function edit(topic?: Topic) {
  editing.value = topic
    ? { ...topic }
    : {
        name: '',
        description: '',
        icon: 'BookOpen',
        color: '#548b9d',
        sortOrder: topics.value.length,
        enabled: true,
      }
}

async function save() {
  if (!editing.value) return
  busy.value = true
  try {
    const { id, ...data } = editing.value
    await api(`/admin/topics${id ? `/${id}` : ''}`, {
      method: id ? 'PUT' : 'POST',
      body: body(data),
    })
    editing.value = null
    await load()
    notify('主题已保存')
  } catch (e) {
    notify((e as Error).message, 'error')
  } finally {
    busy.value = false
  }
}

async function toggle(user: User) {
  if (!window.confirm(`${user.enabled ? '停用' : '启用'}账号 ${user.email}？`)) return
  busy.value = true
  try {
    await api(`/admin/users/${user.id}`, {
      method: 'PATCH',
      body: body({ enabled: !user.enabled }),
    })
    await load()
    notify('账号状态已更新')
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
      <AppIcon name="Settings2" :size="17" />
      <span>工作空间</span>
      <AppIcon name="ChevronRight" :size="14" />
      <strong>管理中心</strong>
    </div>
    <span class="small-tag">管理员</span>
  </header>
  <div class="home-page">
    <section class="page-heading">
      <div>
        <span class="eyebrow">WORKSPACE MANAGEMENT</span>
        <h1>管理中心</h1>
        <p>维护主题入口，管理账号，了解 AI 使用情况。</p>
      </div>
      <button class="button" :disabled="loading" @click="load">
        <AppIcon name="RefreshCw" :size="17" />
        刷新数据
      </button>
    </section>
    <div v-if="stats" class="stats-strip">
      <div>
        <span class="stat-icon"><AppIcon name="Users" /></span>
        <span>
          <strong>{{ stats.users }}</strong>
          <small>注册账号</small>
        </span>
      </div>
      <div>
        <span class="stat-icon"><AppIcon name="FolderTree" /></span>
        <span>
          <strong>{{ stats.trees }}</strong>
          <small>知识树总数</small>
        </span>
      </div>
      <div>
        <span class="stat-icon"><AppIcon name="Sparkles" /></span>
        <span>
          <strong>{{ stats.calls }}</strong>
          <small>AI 调用记录</small>
        </span>
      </div>
    </div>
    <div class="tabs">
      <button
        v-for="item in [
          { key: 'topics', label: '内置主题' },
          { key: 'users', label: '账号管理' },
          { key: 'usage', label: 'AI 调用记录' },
        ]"
        :key="item.key"
        :class="{ active: tab === item.key }"
        @click="tab = item.key"
      >
        {{ item.label }}
      </button>
    </div>
    <p v-if="error" class="inline-error" role="alert">{{ error }}</p>
    <div v-if="loading" class="loading-state">
      <AppIcon name="LoaderCircle" class="spin" />
      正在加载管理数据…
    </div>
    <template v-else>
      <section v-if="tab === 'topics'">
        <div class="section-heading">
          <h2>内置科普主题</h2>
          <button class="button primary" @click="edit()">
            <AppIcon name="Plus" :size="17" />
            添加主题
          </button>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>主题</th>
                <th>范围说明</th>
                <th>排序</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="topic in topics" :key="topic.id">
                <td>
                  <span class="table-topic">
                    <AppIcon :name="topic.icon" :style="{ color: topic.color }" />
                    {{ topic.name }}
                  </span>
                </td>
                <td class="description-cell">{{ topic.description }}</td>
                <td>{{ topic.sortOrder }}</td>
                <td>
                  <span class="status-pill" :class="{ disabled: !topic.enabled }">
                    {{ topic.enabled ? '已启用' : '已停用' }}
                  </span>
                </td>
                <td><button class="text-button" @click="edit(topic)">编辑</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
      <section v-else-if="tab === 'users'">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>用户</th>
                <th>邮箱</th>
                <th>角色</th>
                <th>注册时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.id">
                <td>{{ user.name }}</td>
                <td>{{ user.email }}</td>
                <td>{{ user.role === 'ADMIN' ? '管理员' : '用户' }}</td>
                <td>{{ formatDate(user.createdAt) }}</td>
                <td>{{ user.enabled ? '正常' : '已停用' }}</td>
                <td>
                  <button
                    v-if="user.role !== 'ADMIN'"
                    class="text-button"
                    :disabled="busy"
                    @click="toggle(user)"
                  >
                    {{ user.enabled ? '停用' : '启用' }}
                  </button>
                  <span v-else class="muted">—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
      <section v-else>
        <p class="muted">最近 100 条调用。仅记录使用量、耗时和结果，不展示用户的提示词内容。</p>
        <div v-if="!stats?.recentUsage.length" class="empty-state">
          <AppIcon name="Sparkles" :size="30" />
          <h3>还没有 AI 调用记录</h3>
          <p>用户开始分析主题或细分节点后，记录会出现在这里。</p>
        </div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>时间</th>
                <th>账号</th>
                <th>操作</th>
                <th>模型</th>
                <th>Tokens</th>
                <th>耗时</th>
                <th>结果</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in stats?.recentUsage" :key="item.id">
                <td>{{ formatDate(item.createdAt) }}</td>
                <td>{{ users.find((u) => u.id === item.userId)?.email || item.userId }}</td>
                <td>{{ item.operation === 'EXPAND' ? '节点细分' : '主题提取' }}</td>
                <td>{{ item.model }}</td>
                <td>{{ item.totalTokens }}</td>
                <td>{{ (item.durationMs / 1000).toFixed(1) }}s</td>
                <td>
                  <span class="status-pill" :class="{ disabled: !item.success }">
                    {{ item.success ? '成功' : '失败' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </div>
  <ModalDialog
    v-if="editing"
    :title="editing.id ? '编辑内置主题' : '添加内置主题'"
    @close="!busy && (editing = null)"
  >
    <form @submit.prevent="save">
      <label>
        名称
        <input v-model="editing.name" required maxlength="100" />
      </label>
      <label>
        范围说明
        <textarea v-model="editing.description" required maxlength="2000" rows="4" />
      </label>
      <div class="form-row">
        <label>
          图标
          <select v-model="editing.icon">
            <option
              v-for="icon in [
                'BookOpen',
                'HeartPulse',
                'Apple',
                'Brain',
                'Dna',
                'Sprout',
                'Earth',
                'Orbit',
                'Atom',
                'FlaskConical',
                'Cpu',
                'Zap',
                'Microscope',
              ]"
              :key="icon"
            >
              {{ icon }}
            </option>
          </select>
        </label>
        <label>
          颜色
          <input v-model="editing.color" type="color" />
        </label>
        <label>
          排序
          <input v-model.number="editing.sortOrder" type="number" />
        </label>
      </div>
      <label class="checkbox-label">
        <input v-model="editing.enabled" type="checkbox" />
        启用该主题
      </label>
      <div class="modal-actions">
        <button type="button" class="button" :disabled="busy" @click="editing = null">取消</button>
        <button class="button primary" :disabled="busy">保存主题</button>
      </div>
    </form>
  </ModalDialog>
</template>
