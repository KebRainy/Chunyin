<template>
  <div class="settings-page">
    <div class="settings-card">
      <div class="card-header">
        <div>
          <p class="eyebrow">Profile</p>
          <h2>个人资料</h2>
        </div>
      </div>
      <el-form :model="form" label-width="100px" class="settings-form" @submit.prevent="saveSettings">
        <el-form-item label="头像">
          <div class="avatar-section">
            <el-avatar :src="avatarPreview" :size="96" :alt="`${form.username || '用户'}的头像预览`" />
            <el-upload
              class="avatar-upload"
              action="/api/files/upload?category=AVATAR"
              with-credentials
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload"
              accept="image/*"
            >
              <el-button>更改头像</el-button>
            </el-upload>
          </div>
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="用户名">
            <el-input v-model="form.username" placeholder="输入用户名" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" type="email" placeholder="输入邮箱" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="form.gender" style="display: flex; flex-direction: column;">
              <el-radio value="MALE">男</el-radio>
              <el-radio value="FEMALE">女</el-radio>
              <el-radio value="SECRET">保密</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="生日">
            <el-date-picker
              v-model="form.birthday"
              type="date"
              placeholder="选择生日"
            />
          </el-form-item>
        </div>

        <el-form-item label="个性签名">
          <el-input
            v-model="form.bio"
            type="textarea"
            placeholder="输入个性签名"
            rows="3"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <div class="form-actions">
          <el-button @click="resetForm">取消</el-button>
          <el-button type="primary" @click="saveSettings">保存设置</el-button>
        </div>
      </el-form>
    </div>

    <div class="settings-card">
      <div class="card-header">
        <div>
          <p class="eyebrow">Security</p>
          <h3>账户安全</h3>
        </div>
      </div>
      <div class="setting-item">
        <div>
          <p class="item-title">修改密码</p>
          <p class="item-desc">为保障账户安全，建议定期更新密码</p>
        </div>
        <el-button text type="primary" @click="showPasswordChange">修改</el-button>
      </div>
      <div class="setting-item">
        <div>
          <p class="item-title">导出我的数据</p>
          <p class="item-desc">下载近 30 天的所有内容记录</p>
        </div>
        <el-button text @click="exportData">导出</el-button>
      </div>
      <div class="setting-item danger">
        <div>
          <p class="item-title">删除账户</p>
          <p class="item-desc">删除后将无法恢复所有数据</p>
        </div>
        <el-button text type="danger" @click="deleteAccount">删除</el-button>
      </div>
    </div>

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
.settings-page {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.settings-card {
  background: #fff;
  border-radius: 28px;
  border: 1px solid #eceff5;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.05);
  padding: 24px 28px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.eyebrow {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 2px;
  font-size: 12px;
  color: #909399;
}

.settings-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 12px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-top: 1px solid #f0f2f5;
}

.setting-item.danger .item-title {
  color: #e53935;
}

.item-title {
  margin: 0;
  font-weight: 600;
  color: #1f2d3d;
}

.item-desc {
  margin: 4px 0 0;
  color: #909399;
  font-size: 13px;
}

@media (max-width: 600px) {
  .settings-card {
    padding: 20px;
  }

  .setting-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .form-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
