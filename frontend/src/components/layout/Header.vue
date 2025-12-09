<template>
  <div class="header-container">
    <div class="header-content">
      <div class="logo" @click="goHome">
        <el-icon :size="28"><Wine /></el-icon>
        <span>酒饮平台</span>
      </div>

      <el-menu
        mode="horizontal"
        :default-active="activeMenu"
        class="header-menu"
        router
      >
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/beverages">酒品库</el-menu-item>
        <el-menu-item index="/announcements">公告</el-menu-item>
      </el-menu>

      <div class="header-right">
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

<script>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { ElMessage } from 'element-plus'

export default {
  name: 'Header',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const userStore = useUserStore()

    const activeMenu = computed(() => route.path)

    const goHome = () => {
      router.push('/')
    }

    const goLogin = () => {
      router.push('/login')
    }

    const goRegister = () => {
      router.push('/register')
    }

    const handleCommand = (command) => {
      if (command === 'profile') {
        router.push('/user/profile')
      } else if (command === 'logout') {
        userStore.logout()
        ElMessage.success('退出成功')
        router.push('/')
      }
    }

    return {
      userStore,
      activeMenu,
      goHome,
      goLogin,
      goRegister,
      handleCommand
    }
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
  gap: 10px;
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
</style>
