<template>
  <div class="settings-container">
    <h2>个人设置</h2>

    <el-form :model="form" label-width="100px" @submit.prevent="saveSettings">
      <!-- 头像上传 -->
      <el-form-item label="头像">
        <div class="avatar-section">
          <el-avatar :src="avatarPreview" :size="80" :alt="`${form.username || '用户'}的头像预览`" />
          <el-upload
            class="avatar-upload"
            action="/api/files/upload?category=AVATAR"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
            accept="image/*"
          >
            <el-button>更改头像</el-button>
          </el-upload>
        </div>
      </el-form-item>

      <!-- 用户名 -->
      <el-form-item label="用户名">
        <el-input v-model="form.username" placeholder="输入用户名" />
      </el-form-item>

      <!-- 个性签名 -->
      <el-form-item label="个性签名">
        <el-input
          v-model="form.bio"
          type="textarea"
          placeholder="输入个性签名"
          rows="3"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <!-- 性别 -->
      <el-form-item label="性别">
        <el-radio-group v-model="form.gender">
          <el-radio value="MALE">男</el-radio>
          <el-radio value="FEMALE">女</el-radio>
          <el-radio value="SECRET">保密</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 邮箱 -->
      <el-form-item label="邮箱">
        <el-input v-model="form.email" type="email" placeholder="输入邮箱" />
      </el-form-item>

      <!-- 生日 -->
      <el-form-item label="生日">
        <el-date-picker
          v-model="form.birthday"
          type="date"
          placeholder="选择生日"
        />
      </el-form-item>

      <!-- 提交按钮 -->
      <el-form-item>
        <el-button type="primary" @click="saveSettings">保存设置</el-button>
        <el-button @click="resetForm">取消</el-button>
      </el-form-item>
    </el-form>

    <!-- 其他设置 -->
    <el-divider />

    <div class="other-settings">
      <h3>账户安全</h3>
      <div class="setting-item">
        <span>修改密码</span>
        <el-button type="text" @click="showPasswordChange">修改</el-button>
      </div>

      <h3>数据管理</h3>
      <div class="setting-item">
        <span>导出我的数据</span>
        <el-button type="text" @click="exportData">导出</el-button>
      </div>
      <div class="setting-item">
        <span>删除账户</span>
        <el-button type="text" @click="deleteAccount">删除</el-button>
      </div>
    </div>

    <!-- 修改密码对话框 -->
    <el-dialog title="修改密码" v-model="passwordDialogVisible" width="400px">
      <el-form :model="passwordForm" label-width="80px">
        <el-form-item label="原密码">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="输入原密码"
          />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="输入新密码"
          />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="确认新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="changePassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/store/modules/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { updateProfile, changePassword as changePasswordApi } from '@/api/user'

const userStore = useUserStore()
const avatarPreview = ref(userStore.userInfo?.avatarUrl)
const passwordDialogVisible = ref(false)

const form = ref({
  username: userStore.userInfo?.username || '',
  bio: userStore.userInfo?.bio || '',
  gender: userStore.userInfo?.gender || 'SECRET',
  email: userStore.userInfo?.email || '',
  birthday: userStore.userInfo?.birthday ? new Date(userStore.userInfo.birthday) : null,
  avatarImageId: null
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const toDateString = (value) => {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return null
  }
  return date.toISOString().slice(0, 10)
}

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('头像大小不能超过 5MB')
    return false
  }
  return true
}

const handleAvatarSuccess = (response) => {
  if (response.code === 200 && response.data?.id) {
    avatarPreview.value = response.data.url
    form.value.avatarImageId = response.data.id
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error('上传失败，请重试')
  }
}

const saveSettings = async () => {
  try {
    await updateProfile({
      username: form.value.username,
      bio: form.value.bio,
      gender: form.value.gender,
      email: form.value.email,
      birthday: toDateString(form.value.birthday),
      avatarImageId: form.value.avatarImageId
    })
    await userStore.fetchUserInfo()
    ElMessage.success('设置已保存')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const resetForm = () => {
  form.value = {
    username: userStore.userInfo?.username || '',
    bio: userStore.userInfo?.bio || '',
    gender: userStore.userInfo?.gender || 'SECRET',
    email: userStore.userInfo?.email || '',
    birthday: userStore.userInfo?.birthday ? new Date(userStore.userInfo.birthday) : null,
    avatarImageId: null
  }
}

const showPasswordChange = () => {
  passwordDialogVisible.value = true
}

const changePassword = async () => {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    ElMessage.error('密码长度不能少于6位')
    return
  }

  try {
    await changePasswordApi({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordDialogVisible.value = false
    passwordForm.value = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  } catch (error) {
    ElMessage.error('密码修改失败')
  }
}

const exportData = () => {
  ElMessage.info('数据导出功能开发中...')
}

const deleteAccount = () => {
  ElMessageBox.confirm(
    '删除账户后，所有数据将被永久删除，且无法恢复。是否继续？',
    '警告',
    {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // TODO: 调用API删除账户
      ElMessage.success('账户已删除')
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}
</script>

<style scoped>
.settings-container {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px;
}

h2 {
  margin-bottom: 24px;
  color: #333;
}

h3 {
  margin: 24px 0 16px 0;
  color: #333;
  font-size: 14px;
  font-weight: 600;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-upload {
  margin: 0;
}

.other-settings {
  margin-top: 20px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.setting-item span {
  color: #333;
}

:deep(.el-form) {
  max-width: 600px;
}

:deep(.el-form-item__label) {
  color: #606266;
}

:deep(.el-divider) {
  margin: 24px 0;
}
</style>
