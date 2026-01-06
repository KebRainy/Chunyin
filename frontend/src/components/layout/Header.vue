<template>
  <div class="header-container">
    <div class="header-content">
      <div class="header-left">
        <div class="logo" @click="goHome">醇饮</div>
        <div class="nav-menu">
          <div
            v-for="nav in navItems"
            :key="nav.path"
            :class="['nav-item', { active: route.path === nav.path || (nav.path === '/activities' && route.path.startsWith('/activities') && !route.path.startsWith('/activities/create')) || (nav.path === '/seller/activity' && route.path.startsWith('/seller/activity')) || (nav.path === '/admin/activities/review' && route.path.startsWith('/admin/activities/review')) }]"
            @click="handleNavClick(nav.path)"
          >
            {{ nav.label }}
          </div>
        </div>
      </div>
      <div class="header-center">
        <el-input
          v-model="searchKeyword"
          placeholder="探索动态 / 酒饮 / 用户"
          class="search-input"
          @keyup.enter="goSearch"
          clearable
        >
          <template #prefix>
            <el-icon class="search-icon"><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="header-right">
        <div class="user-entry">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown @command="handleCommand">
              <span class="user-avatar">
                <el-avatar
                  :size="36"
                  :src="userStore.userInfo.avatarUrl"
                  :alt="`${userStore.userInfo?.username || '用户'}的头像`"
                >
                  <el-icon><User /></el-icon>
                </el-avatar>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="settings">设置</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>
                    <span style="color: #409EFF; font-weight: 600;">管理后台</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text size="small" @click="goLogin">登录</el-button>
            <el-button text size="small" @click="goRegister">注册</el-button>
          </template>
        </div>
        <div class="quick-links">
          <span v-if="userStore.isLoggedIn" @click="goToFeed">关注</span>
          <span @click="goToMessages">消息</span>
          <span @click="goToFeeds">动态</span>
          <span @click="goToCollections">收藏</span>
          <span @click="goToHistory">足迹</span>
          <span v-if="userStore.isLoggedIn && !userStore.isSeller && !userStore.isAdmin" @click="goToUserActivity">活动</span>
          <span v-if="userStore.isSeller" @click="goToSellerActivity">管理</span>
          <span v-if="userStore.isAdmin" @click="goToAdminReview">活动审核</span>
        </div>
        <el-button
          type="primary"
          class="share-btn"
          plain
          @click="triggerShare"
        >
          分享此刻
        </el-button>
      </div>
    </div>
  </div>

  <ShareModal v-model="showShareModal" @posted="refreshAfterShare" />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { ElMessage } from 'element-plus'
import ShareModal from '@/components/ShareModal.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const searchKeyword = ref('')
const showShareModal = ref(false)

// 根据用户角色动态生成导航菜单
const navItems = computed(() => {
  const baseItems = [
    { path: '/', label: '广场' },
    { path: '/nearby', label: '附近' },
    { path: '/ranking', label: '排行榜' },
    { path: '/bars', label: '酒吧' },
    { path: '/wiki', label: '维基' }
  ]
  
  // 根据用户角色添加不同的菜单项
  if (userStore.isAdmin) {
    // 管理员显示"活动审核"和"Wiki审核"
    baseItems.splice(1, 0, 
      { path: '/admin/activities/review', label: '活动审核' },
      { path: '/admin/wiki/review', label: 'Wiki审核' }
    )
  } else if (userStore.isSeller) {
    // seller用户显示"管理"，指向商家活动管理页面
    baseItems.splice(1, 0, { path: '/seller/activity', label: '管理' })
  } else {
    // 普通用户（USER）显示"活动"，指向活动列表页面
    baseItems.splice(1, 0, { path: '/activities', label: '活动' })
  }
  
  return baseItems
})

onMounted(() => {
  if (!userStore.initialized && !userStore.loading) {
    userStore.fetchUserInfo().catch(() => {})
  }
})

const goHome = () => router.push('/')
const goTo = (path) => router.push(path)

// 处理导航菜单点击
const handleNavClick = (path) => {
  // 如果是活动或管理页面，需要检查登录状态
  if (path === '/activities') {
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      router.push('/login')
      return
    }
    router.push('/activities')
  } else if (path === '/seller/activity') {
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      router.push('/login')
      return
    }
    router.push('/seller/activity')
  } else if (path === '/seller/activity') {
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      router.push('/login')
      return
    }
    if (!userStore.isSeller) {
      ElMessage.warning('此功能仅限商家用户')
      return
    }
    router.push('/seller/activity')
  } else if (path === '/admin/activities/review') {
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      router.push('/login')
      return
    }
    if (!userStore.isAdmin) {
      ElMessage.warning('此功能仅限管理员')
      return
    }
    router.push('/admin/activities/review')
  } else {
    router.push(path)
  }
}

const goToFeed = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push('/feed')
}
const goToMessages = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push('/messages')
}
const goToFeeds = () => router.push('/user/profile?tab=posts')
const goToCollections = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push('/user/profile?tab=collections')
}
const goToHistory = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push('/user/profile?tab=history')
}
const goToUserActivity = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push('/activities')
}
const goToSellerActivity = () => {
  if (!userStore.isLoggedIn || !userStore.isSeller) {
    ElMessage.warning('此功能仅限商家用户')
    return
  }
  router.push('/seller/activity')
}
const goToAdminReview = () => {
  if (!userStore.isLoggedIn || !userStore.isAdmin) {
    ElMessage.warning('此功能仅限管理员')
    return
  }
  router.push('/admin/activities/review')
}
const goLogin = () => router.push('/login')
const goRegister = () => router.push('/register')
const goSearch = () => {
  if (!searchKeyword.value) return
  router.push({ path: '/search', query: { q: searchKeyword.value } })
}

const triggerShare = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再分享')
    router.push('/login')
    return
  }
  showShareModal.value = true
}

const refreshAfterShare = () => {
  router.replace({ path: router.currentRoute.value.fullPath })
}

const handleCommand = async (command) => {
  if (command === 'profile') {
    router.push('/user/profile')
  } else if (command === 'settings') {
    router.push('/user/settings')
  } else if (command === 'admin') {
    router.push('/admin/moderation')
  } else if (command === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>

<style scoped>
.header-container {
  height: 68px;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid #f2f2f2;
  position: sticky;
  top: 0;
  z-index: 100;
  -webkit-backdrop-filter: blur(10px);
  backdrop-filter: blur(10px);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  font-size: 22px;
  font-weight: 700;
  color: #2f54eb;
  cursor: pointer;
  letter-spacing: 2px;
}

.nav-menu {
  display: flex;
  gap: 20px;
}

.nav-item {
  font-size: 15px;
  color: #606266;
  cursor: pointer;
  padding: 6px 0;
  position: relative;
}

.nav-item.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  height: 2px;
  background: #303133;
}

.header-center {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 999px;
  background: #f5f6f7;
  border: none;
  box-shadow: none;
}

.search-icon {
  color: #909399;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  border-right: 1px solid #f0f0f0;
  padding-right: 12px;
}

.quick-links {
  display: flex;
  gap: 14px;
  font-size: 14px;
  color: #303133;
}

.quick-links span {
  cursor: pointer;
}

.share-btn {
  border-radius: 999px;
  border-color: #111;
  color: #111;
  padding: 0 18px;
  font-weight: 600;
}

.user-avatar {
  cursor: pointer;
}
</style>
