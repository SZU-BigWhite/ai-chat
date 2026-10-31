<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from './components/AppIcon.vue'
import { api, clearSession, notice, notify, session } from './lib/api'

const route = useRoute()
const router = useRouter()
const mobileNav = ref(false)

async function logout() {
  try {
    await api('/auth/logout', { method: 'POST' })
    clearSession()
    router.push('/login')
  } catch (e) {
    notify((e as Error).message, 'error')
  }
}

function expired() {
  router.push('/login')
  notify('登录已失效，请重新登录', 'error')
}
onMounted(() => window.addEventListener('session-expired', expired))
onUnmounted(() => window.removeEventListener('session-expired', expired))
</script>
<template>
  <RouterView v-if="route.path === '/login'" />
  <div v-else class="app-layout">
    <button class="mobile-menu icon-button" aria-label="打开导航" @click="mobileNav = !mobileNav">
      <AppIcon name="Menu" />
    </button>
    <aside class="sidebar" :class="{ 'mobile-open': mobileNav }">
      <RouterLink to="/" class="brand" @click="mobileNav = false">
        <span class="brand-mark"><AppIcon name="Network" :size="23" /></span>
        <span>
          Topic Atlas
          <small>知识结构工作台</small>
        </span>
      </RouterLink>
      <div class="workspace-label">
        我的工作空间
        <span class="tiny-dot"></span>
      </div>
      <nav aria-label="主导航">
        <RouterLink
          to="/"
          class="nav-item"
          :class="{ active: route.path === '/' && !route.query.tab }"
          @click="mobileNav = false"
        >
          <AppIcon name="Layers" />
          主题探索
        </RouterLink>
        <RouterLink
          to="/?tab=trees"
          class="nav-item"
          :class="{ active: route.query.tab === 'trees' || route.path.startsWith('/trees/') }"
          @click="mobileNav = false"
        >
          <AppIcon name="FolderTree" />
          我的知识树
        </RouterLink>
        <RouterLink
          v-if="session.user?.role === 'ADMIN'"
          to="/admin"
          class="nav-item"
          :class="{ active: route.path === '/admin' }"
          @click="mobileNav = false"
        >
          <AppIcon name="Settings2" />
          管理中心
        </RouterLink>
      </nav>
      <div class="sidebar-guide">
        <span class="eyebrow">从一个想法，向下生长</span>
        <div class="mini-branches">
          <i></i>
          <i></i>
          <i></i>
        </div>
        <p>让复杂知识拥有清晰结构。</p>
        <p class="muted">由你决定每一个分支的深度。</p>
      </div>
      <div class="sidebar-bottom">
        <div class="privacy-note">
          <AppIcon name="ShieldCheck" :size="16" />
          你的知识树仅自己可见
        </div>
        <div class="profile">
          <span class="avatar">{{ session.user?.name.slice(0, 1) }}</span>
          <span class="profile-name">
            {{ session.user?.name }}
            <small>{{ session.user?.role === 'ADMIN' ? '管理员' : '个人工作空间' }}</small>
          </span>
          <button class="icon-button" aria-label="退出登录" @click="logout">
            <AppIcon name="LogOut" :size="18" />
          </button>
        </div>
      </div>
    </aside>
    <main class="main-content"><RouterView :key="route.path" /></main>
  </div>
  <Transition name="toast">
    <div
      v-if="notice.text"
      class="toast-message"
      :class="notice.kind"
      :role="notice.kind === 'error' ? 'alert' : 'status'"
    >
      <AppIcon :name="notice.kind === 'error' ? 'AlertCircle' : 'Check'" :size="19" />
      {{ notice.text }}
      <button class="icon-button" aria-label="关闭提示" @click="notice.text = ''">
        <AppIcon name="X" :size="16" />
      </button>
    </div>
  </Transition>
</template>
