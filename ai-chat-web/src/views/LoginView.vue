<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import { api, body, setSession } from '../lib/api'
import type { User } from '../types'

const router = useRouter()
const register = ref(false)
const email = ref('')
const password = ref('')
const name = ref('')
const busy = ref(false)
const error = ref('')

function toggleMode() {
  register.value = !register.value
  error.value = ''
}

/** 注册与登录返回同一会话结构，成功后统一保存会话并进入工作台。 */
async function submit() {
  busy.value = true
  error.value = ''
  try {
    const result = await api<{ token: string; user: User }>(
      register.value ? '/auth/register' : '/auth/login',
      {
        method: 'POST',
        body: body({
          email: email.value,
          password: password.value,
          ...(register.value ? { name: name.value } : {}),
        }),
      },
    )
    setSession(result.token, result.user)
    await router.push('/')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <main class="login-layout">
    <section class="login-story">
      <div class="brand">
        <span class="brand-mark"><AppIcon name="Network" :size="24" /></span>
        <span>
          Topic Atlas
          <small>知识结构工作台</small>
        </span>
      </div>
      <div class="login-copy">
        <span class="eyebrow">EXPLORE · STRUCTURE · CREATE</span>
        <h1>
          让每一个主题，
          <br />
          长成一棵知识树。
        </h1>
        <p>
          从宽广的领域到细微的知识点，
          <br />
          与 AI 一起探索，由你定义边界。
        </p>
        <div class="login-tree">
          <div class="sample-root">
            <AppIcon name="Sprout" />
            一个主题
          </div>
          <div class="sample-children">
            <span>发现方向</span>
            <span>深入知识</span>
            <span>连接内容</span>
          </div>
        </div>
      </div>
      <footer>为科普内容生产，建立清晰的知识起点。</footer>
    </section>
    <section class="login-form-wrap">
      <form class="login-form" @submit.prevent="submit">
        <span class="pill">
          <span class="tiny-dot"></span>
          你的专属知识空间
        </span>
        <h2>{{ register ? '创建账号' : '欢迎回来' }}</h2>
        <p class="muted">
          {{ register ? '开启你的第一段知识探索。' : '登录后，继续构建你的知识世界。' }}
        </p>
        <label v-if="register">
          你的名字
          <input
            v-model="name"
            required
            maxlength="60"
            autocomplete="name"
            placeholder="如何称呼你"
          />
        </label>
        <label>
          邮箱地址
          <input
            v-model="email"
            type="email"
            required
            maxlength="160"
            autocomplete="email"
            placeholder="name@company.com"
          />
        </label>
        <label>
          密码
          <input
            v-model="password"
            type="password"
            required
            :minlength="register ? 8 : undefined"
            maxlength="64"
            :autocomplete="register ? 'new-password' : 'current-password'"
            :placeholder="register ? '至少 8 位字符' : '请输入密码'"
          />
        </label>
        <p v-if="error" class="inline-error" role="alert">{{ error }}</p>
        <button class="button primary login-submit" :disabled="busy">
          <AppIcon v-if="busy" name="LoaderCircle" class="spin" :size="18" />
          {{ busy ? '正在处理…' : register ? '创建账号并进入' : '进入工作空间' }}
          <AppIcon v-if="!busy" name="ArrowRight" :size="18" />
        </button>
        <p class="login-switch">
          {{ register ? '已经有账号？' : '还没有账号？' }}
          <button type="button" class="text-button" @click="toggleMode">
            {{ register ? '立即登录' : '注册账号' }}
          </button>
        </p>
        <div class="login-safe">
          <AppIcon name="ShieldCheck" :size="16" />
          知识树与历史记录按账号独立保存
        </div>
      </form>
    </section>
  </main>
</template>
