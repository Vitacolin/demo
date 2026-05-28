import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../store/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../layout/AppLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: 'input',
        name: 'ChatInput',
        component: () => import('../views/ChatInput.vue')
      },
      {
        path: 'transactions',
        name: 'Transactions',
        component: () => import('../views/Transactions.vue')
      },
      {
        path: 'subscriptions',
        name: 'Subscriptions',
        component: () => import('../views/Subscriptions.vue')
      },
      {
        path: 'assets',
        name: 'Assets',
        component: () => import('../views/Assets.vue')
      },
      {
        path: 'advisor',
        name: 'Advisor',
        component: () => import('../views/Advisor.vue')
      },
      {
        path: 'families',
        name: 'Families',
        component: () => import('../views/Families.vue')
      },
      {
        path: 'system',
        name: 'System',
        component: () => import('../views/System.vue'),
        meta: { requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth !== false && !authStore.isAuthenticated) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && authStore.isAuthenticated) {
    next('/dashboard')
  } else if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
