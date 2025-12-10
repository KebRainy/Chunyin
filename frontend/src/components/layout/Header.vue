<template>
  <div class="header-container">
    <div class="header-content">
      <!-- Logo / 品牌标题 -->
      <div class="logo" @click="goHome">
        <el-icon :size="24"><Wine /></el-icon>
        <span>醇饮</span>
      </div>

      <!-- 左侧导航菜单 -->
      <div class="nav-menu">
        <div
          v-for="nav in navItems"
          :key="nav.path"
          :class="['nav-item', { active: route.path === nav.path }]"
          @click="goTo(nav.path)"
        >
          {{ nav.label }}
        </div>
      </div>

      <!-- 中间搜索栏 -->
      <div class="search-area">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索"
          class="search-input"
          @keyup.enter="goSearch"
          clearable
        >
          <template #prefix>
            <el-icon class="search-icon"><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 右侧功能区 -->
      <div class="header-right">
        <!-- 消息 -->
        <el-tooltip content="消息">
          <el-icon class="action-icon" @click="goToMessages"><ChatDotSquare /></el-icon>
        </el-tooltip>

        <!-- 动态 -->
        <el-tooltip content="动态">
          <el-icon class="action-icon" @click="goToFeeds"><DocumentCopy /></el-icon>
        </el-tooltip>

        <!-- 收藏 -->
        <el-tooltip content="收藏">
          <el-icon class="action-icon" @click="goToCollections"><StarFilled /></el-icon>
        </el-tooltip>

        <!-- 足迹 -->
        <el-tooltip content="足迹">
          <el-icon class="action-icon" @click="goToHistory"><Clock /></el-icon>
        </el-tooltip>

        <!-- 分享此刻按钮 -->
        <el-button
          v-if="userStore.isLoggedIn"
          type="primary"
          size="small"
          @click="showShareModal = true"
        >
          分享此刻
        </el-button>

        <!-- 用户菜单或登录注册 -->
        <template v-if="userStore.isLoggedIn">
          <el-dropdown @command="handleCommand">
            <span class="user-avatar">
              <el-avatar
                :size="32"
                :src="userStore.userInfo.avatarUrl"
              >
                <el-icon><User /></el-icon>
              </el-avatar>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" size="small" @click="goLogin">登录</el-button>
          <el-button size="small" @click="goRegister">注册</el-button>
        </template>
      </div>
    </div>
  </div>

  <!-- 分享此刻模态框 -->
  <ShareModal v-model="showShareModal" />
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

const navItems = [
  { path: '/', label: '广场' },
  { path: '/feed', label: '关注' },
  { path: '/nearby', label: '附近' },
  { path: '/wiki', label: '维基' }
]

onMounted(() => {
  if (!userStore.initialized && !userStore.loading) {
    userStore.fetchUserInfo().catch(() => {})
  }
})

const goHome = () => router.push('/')
const goTo = (path) => router.push(path)
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
const goLogin = () => router.push('/login')
const goRegister = () => router.push('/register')
const goSearch = () => {
  if (!searchKeyword.value) return
  router.push({ path: '/search', query: { q: searchKeyword.value } })
}

const handleCommand = async (command) => {
  if (command === 'profile') {
    router.push('/user/profile')
  } else if (command === 'settings') {
    router.push('/user/settings')
  } else if (command === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>

<style scoped>
.header-container {
  height: 64px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}

.nav-menu {
  display: flex;
  gap: 24px;
  flex-shrink: 0;
}

.nav-item {
  padding: 8px 4px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
  transition: all 0.3s;
  border-bottom: 2px solid transparent;
}

.nav-item:hover {
  color: #409eff;
}

.nav-item.active {
  color: #409eff;
  border-bottom-color: #409eff;
}

.search-area {
  flex: 1;
  min-width: 200px;
  max-width: 300px;
}

.search-input {
  width: 100%;
}

.search-icon {
  color: #909399;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.action-icon {
  font-size: 18px;
  cursor: pointer;
  color: #606266;
  transition: color 0.3s;
}

.action-icon:hover {
  color: #409eff;
}

.user-avatar {
  cursor: pointer;
}

:deep(.el-input__wrapper) {
  border-radius: 20px;
}
</style>

