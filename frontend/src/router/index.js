import { createRouter, createWebHistory } from 'vue-router'
import pinia from '@/store'
import { useUserStore } from '@/store/modules/user'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/beverages',
    name: 'BeverageList',
    component: () => import('@/views/beverage/BeverageList.vue')
  },
  {
    path: '/beverages/:id',
    name: 'BeverageDetail',
    component: () => import('@/views/beverage/BeverageDetail.vue')
  },
  {
    path: '/announcements',
    name: 'AnnouncementList',
    component: () => import('@/views/announcement/AnnouncementList.vue')
  },
  {
    path: '/wiki',
    name: 'WikiList',
    component: () => import('@/views/wiki/WikiList.vue')
  },
  {
    path: '/wiki/edit/:id?',
    name: 'WikiEditor',
    component: () => import('@/views/wiki/WikiEditor.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/Search.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue')
  },
  {
    path: '/user/profile',
    name: 'UserProfile',
    component: () => import('@/views/user/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/users/:id',
    name: 'UserHome',
    component: () => import('@/views/user/UserHome.vue')
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore(pinia)
  if (!userStore.initialized && !userStore.loading) {
    await userStore.fetchUserInfo().catch(() => {})
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else {
    next()
  }
})

export default router
