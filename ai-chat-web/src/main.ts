import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import { initSession, session } from './lib/api'
import './styles.css'
import { logClientError } from './lib/logger'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('./views/LoginView.vue') },
    { path: '/', component: () => import('./views/HomeView.vue') },
    { path: '/trees/:id', component: () => import('./views/TreeView.vue') },
    { path: '/admin', component: () => import('./views/AdminView.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})
// 页面导航只负责体验上的跳转；真正的数据权限仍由后端校验。
router.beforeEach(async (to) => {
  await initSession()
  if (!session.user && to.path !== '/login') return '/login'
  if (session.user && to.path === '/login') return '/'
  if (to.path === '/admin' && session.user?.role !== 'ADMIN') return '/'
})
const app = createApp(App)
app.config.errorHandler = (error) => logClientError('vue', error)
router.onError((error) => logClientError('router', error))
window.addEventListener('error', (event) => logClientError('window', event.error))
window.addEventListener('unhandledrejection', (event) => logClientError('promise', event.reason))
app.use(router).mount('#app')
