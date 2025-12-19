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
    path: '/feed',
    name: 'Feed',
    component: () => import('@/views/Feed.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/nearby',
    name: 'Nearby',
    component: () => import('@/views/Nearby.vue')
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
    path: '/bars',
    name: 'BarList',
    component: () => import('@/views/bar/BarList.vue')
  },
  {
    path: '/bars/register',
    name: 'BarRegister',
    component: () => import('@/views/bar/BarRegister.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/bars/:id',
    name: 'BarDetail',
    component: () => import('@/views/bar/BarDetail.vue')
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
    path: '/wiki/:slug',
    name: 'WikiDetail',
    component: () => import('@/views/wiki/WikiDetail.vue')
  },
  {
    path: '/posts/:id',
    name: 'PostDetail',
    component: () => import('@/views/post/PostDetail.vue')
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/Search.vue')
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('@/views/message/Messages.vue'),
    meta: { requiresAuth: true }
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
    path: '/user/settings',
    name: 'UserSettings',
    component: () => import('@/views/user/Settings.vue'),
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
