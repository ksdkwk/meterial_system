import { createRouter, createWebHistory } from 'vue-router'
import AuthEntry from '@/components/AuthEntry.vue'
import DashboardEntry from '@/components/DashboardEntry.vue'
import { isAuthenticated } from '@/utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: AuthEntry,
    props: { startForgot: false },
    meta: { public: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: AuthEntry,
    props: { startForgot: true },
    meta: { public: true }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: DashboardEntry
  },
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const isPublicPage = to.meta?.public === true
  const isLoggedIn = isAuthenticated()

  if (!isPublicPage && !isLoggedIn) {
    return '/login'
  }

  if (isLoggedIn && (to.path === '/login' || to.path === '/forgot-password')) {
    return '/dashboard'
  }

  return true
})

export default router
