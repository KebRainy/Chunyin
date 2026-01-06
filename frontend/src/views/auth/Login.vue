<template>
  <div class="auth-container">
    <el-card class="auth-card">
      <h2>登录醇饮社区</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-form-item label="用户名 / 邮箱" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名或邮箱" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submitForm">登录</el-button>
          <el-button type="text" @click="goRegister">注册新账号</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/modules/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

// 进入登录页时，清除之前的用户信息
onMounted(() => {
  if (userStore.userInfo) {
    userStore.clearUserInfo()
  }
})

const rules = {
  username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await login(form)
      userStore.setUserInfo(res.data.profile)
      ElMessage.success('登录成功')
      
      // 登录成功后，如果有重定向路径，跳转到该路径，否则跳转到首页
      const redirect = router.currentRoute.value.query.redirect || '/'
      router.push(redirect)
    } catch (error) {
      const message = error?.response?.data?.message || error.message || '登录失败，请检查账号和密码'
      ElMessage.error(message)
    } finally {
      loading.value = false
    }
  })
}

const goRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
  position: relative;
  padding: 40px 20px;
}

.auth-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 30% 40%, rgba(139, 69, 19, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 70% 60%, rgba(212, 175, 55, 0.1) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.auth-card {
  width: 100%;
  max-width: 480px;
  padding: 40px 30px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(139, 69, 19, 0.1);
  box-shadow: var(--shadow-xl);
  position: relative;
  z-index: 1;
  animation: fadeIn 0.6s ease-out;
}

h2 {
  text-align: center;
  margin-bottom: 32px;
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 700;
  background: var(--gradient-wine);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 2px;
}

:deep(.el-form-item__label) {
  font-family: var(--font-serif);
  font-weight: 500;
  color: #4a5568;
}

:deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: var(--shadow-sm);
}

:deep(.el-button--primary) {
  background: var(--gradient-primary);
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-sans);
  font-weight: 500;
  letter-spacing: 0.5px;
  padding: 12px 32px;
  transition: all 0.3s ease;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

:deep(.el-button--text) {
  font-family: var(--font-sans);
  color: var(--primary-color);
  transition: all 0.3s ease;
}

:deep(.el-button--text:hover) {
  color: var(--primary-dark);
}
</style>
