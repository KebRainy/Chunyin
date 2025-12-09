<template>
  <div class="header-container">
    <div class="header-content">
      <div class="logo" @click="goHome">
        <el-icon :size="28"><Wine /></el-icon>
        <span>醇饮社区</span>
      </div>

      <el-menu
        mode="horizontal"
        :default-active="activeMenu"
        class="header-menu"
        router
      >
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/beverages">酒单</el-menu-item>
        <el-menu-item index="/announcements">活动</el-menu-item>
        <el-menu-item index="/wiki">Wiki</el-menu-item>
        <el-menu-item index="/search">搜索</el-menu-item>
      </el-menu>

      <div class="header-right">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索圈子动态 / 酒款 / 用户"
          class="header-search"
          size="small"
          clearable
          @keyup.enter="goSearch"
        >
          <template #suffix>
            <el-icon class="search-icon" @click="goSearch"><Search /></el-icon>
          </template>
        </el-input>

        <template v-if="userStore.isLoggedIn">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo.avatarUrl">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span class="username">{{ userStore.userInfo.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" @click="goLogin">登录</el-button>
          <el-button @click="goRegister">注册</el-button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const searchKeyword = ref('')

const activeMenu = computed(() => route.path)

onMounted(() => {
  if (!userStore.initialized && !userStore.loading) {
    userStore.fetchUserInfo().catch(() => {})
  }
})

const goHome = () => router.push('/')
const goLogin = () => router.push('/login')
const goRegister = () => router.push('/register')
const goSearch = () => {
  if (!searchKeyword.value) return
  router.push({ path: '/search', query: { q: searchKeyword.value } })
}

const handleCommand = async (command) => {
  if (command === 'profile') {
    router.push('/user/profile')
  } else if (command === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>

<style scoped>
.header-container {
  height: 60px;
  background-color: #fff;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  cursor: pointer;
}

.header-menu {
  flex: 1;
  margin: 0 40px;
  border-bottom: none;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #606266;
}

.header-search {
  width: 220px;
}

.search-icon {
  cursor: pointer;
}
</style>
