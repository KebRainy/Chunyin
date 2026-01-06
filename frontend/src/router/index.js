import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
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
    path: '/ranking',
    name: 'Ranking',
    component: () => import('@/views/Ranking.vue'),
    meta: { title: '热门排行' }
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
  },
  {
    path: '/admin/moderation',
    name: 'AdminModeration',
    component: () => import('@/views/admin/AdminModeration.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/seller/activity',
    name: 'SellerActivity',
    component: () => import('@/views/seller/Activity.vue'),
    meta: { requiresAuth: true, requiresSeller: true }
  },
  {
    path: '/activities',
    name: 'ActivityList',
    component: () => import('@/views/activity/ActivityList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/activities/create',
    name: 'CreateActivity',
    component: () => import('@/views/activity/CreateActivity.vue'),
    meta: { requiresAuth: true, requiresSeller: true }
  },
  {
    path: '/activities/:id',
    name: 'ActivityDetail',
    component: () => import('@/views/activity/ActivityDetail.vue')
  },
  {
    path: '/admin/activities/review',
    name: 'ActivityReview',
    component: () => import('@/views/activity/ActivityList.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/wiki/review',
    name: 'WikiReview',
    component: () => import('@/views/admin/WikiReview.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/bars/review',
    name: 'BarReview',
    component: () => import('@/views/admin/BarReview.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

// 处理 chunk 加载失败的问题
router.onError((error) => {
  const pattern = /Loading chunk (\d)+ failed/g
  const isChunkLoadFailed = error.message && error.message.match(pattern)
  if (isChunkLoadFailed) {
    // 如果 chunk 加载失败，重新加载页面
    const targetPath = router.currentRoute.value?.fullPath || window.location.pathname
    if (targetPath && targetPath !== '/login') {
      window.location.href = targetPath
    }
  }
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore(pinia)
  
  // 重要：如果用户已登录（有userInfo），直接使用现有状态，不重新获取
  // 只有在未初始化且不在加载中时才获取用户信息
  if (!userStore.initialized && !userStore.loading) {
    // 如果用户已经有登录信息，直接标记为已初始化，不重新获取
    if (userStore.userInfo && userStore.userInfo.id) {
      userStore.initialized = true
    } else {
      // 只有在没有用户信息时才尝试获取
      try {
        await userStore.fetchUserInfo()
      } catch (error) {
        // 如果获取用户信息失败，检查是否是401错误
        // 401表示token过期或未登录，此时清空用户信息是合理的
        // 其他错误（如网络错误）不影响已登录状态
        if (error?.response?.status === 401) {
          // 401错误，清空用户信息（fetchUserInfo中已处理）
          // 如果页面需要认证，会跳转到登录页
        } else {
          // 其他错误（网络错误等），保留现有状态，不强制跳转
          // 如果用户之前已登录，保留登录状态
          console.log('获取用户信息失败（非401），保留现有状态:', error)
          // 如果用户之前已登录，标记为已初始化，避免重复请求
          if (userStore.userInfo) {
            userStore.initialized = true
          }
        }
      }
    }
  }

  // 等待用户信息加载完成（如果正在加载）
  if (userStore.loading) {
    // 等待加载完成，最多等待2秒
    let waitCount = 0
    while (userStore.loading && waitCount < 40) {
      await new Promise(resolve => setTimeout(resolve, 50))
      waitCount++
    }
  }

  // 如果页面需要认证，检查登录状态
  // 重要：如果用户已登录（有userInfo），即使initialized为false也允许访问
  if (to.meta.requiresAuth) {
    // 如果用户已登录，允许访问
    if (userStore.isLoggedIn) {
      // 检查角色权限
      if (to.meta.requiresAdmin && !userStore.isAdmin) {
        ElMessage.warning('需要管理员权限')
        next('/')
        return
      }
      if (to.meta.requiresSeller && !userStore.isSeller) {
        ElMessage.warning('需要商家权限')
        next('/')
        return
      }
      next()
      return
    }
    
    // 如果正在前往登录页，清除用户信息并允许通过
    if (to.path === '/login') {
      // 确保清除所有用户信息
      if (userStore.userInfo) {
        userStore.clearUserInfo()
      }
      next()
      return
    }
    // 否则跳转到登录页，清除用户信息，并保存目标路径以便登录后跳转
    if (userStore.userInfo) {
      userStore.clearUserInfo()
    }
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    // 不需要认证的页面，直接通过
    next()
  }
})

export default router
