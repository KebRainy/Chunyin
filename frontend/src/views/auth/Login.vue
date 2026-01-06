<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <div class="auth-brand">
        <div class="brand-mark">醇饮</div>
        <div class="brand-meta">
          <h2>登录</h2>
          <p>回到你喜爱的酒饮社区</p>
        </div>
      </div>

      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-position="top"
        class="auth-form"
      >
        <el-form-item label="用户名 / 邮箱" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名或邮箱" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item class="actions">
          <el-button class="primary-btn" type="primary" :loading="loading" @click="submitForm">登录</el-button>
          <el-button text class="link-btn" @click="goRegister">注册新账号</el-button>
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
.auth-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 180px);
  padding: 24px;
  position: relative;
}

.auth-card {
  width: min(460px, 92vw);
  padding: 28px 26px;
  border-radius: 24px;
  border: 1px solid var(--app-border, #e6e8ef);
  background: var(--app-surface, #fff);
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.brand-mark {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-weight: 800;
  color: #fff;
  background: var(--app-primary, #2f54eb);
  letter-spacing: 2px;
}

.brand-meta h2 {
  margin: 0;
  font-size: 20px;
  color: var(--app-text, #0f172a);
}

.brand-meta p {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--app-muted, #64748b);
}

.auth-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #334155;
}

.actions :deep(.el-form-item__content) {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.primary-btn {
  width: 100%;
  height: 40px;
}

.link-btn {
  width: 100%;
  justify-content: center;
  font-weight: 600;
}
</style>
