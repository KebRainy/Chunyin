<template>
  <div class="recommended-user-card" v-if="!hidden">
    <div class="card-header">
      <el-avatar
        :src="user.avatarUrl"
        :size="48"
        :alt="`${user.username}的头像`"
        class="user-avatar"
        @click.stop="showUserInfo"
      >
        <el-icon><User /></el-icon>
      </el-avatar>
      <el-icon class="close-icon" @click.stop="handleBlock">
        <Close />
      </el-icon>
    </div>
    <div class="card-body" @click.stop="showUserInfo">
      <div class="username">{{ user.username }}</div>
      <div class="reason">{{ user.reason }}</div>
      <div v-if="user.bio" class="bio">{{ truncateBio(user.bio) }}</div>
    </div>
    <div class="card-footer">
      <el-button
        type="primary"
        size="small"
        :plain="user.following"
        :loading="followLoading"
        @click.stop="handleFollow"
        class="follow-btn"
      >
        {{ user.following ? '已关注' : '关注' }}
      </el-button>
    </div>

    <!-- 用户信息对话框 -->
    <el-dialog
      v-model="userInfoVisible"
      :title="user.username"
      width="500px"
      @close="userInfoVisible = false"
    >
      <div class="user-info-dialog">
        <div class="dialog-header">
          <el-avatar
            :src="user.avatarUrl"
            :size="80"
            :alt="`${user.username}的头像`"
          >
            <el-icon><User /></el-icon>
          </el-avatar>
          <div class="dialog-user-info">
            <h3>{{ user.username }}</h3>
            <p v-if="user.bio" class="bio">{{ user.bio }}</p>
            <p v-else class="bio-placeholder">这位用户很低调，还没有简介</p>
          </div>
        </div>
        <div class="dialog-actions">
          <el-button
            type="primary"
            :plain="user.following"
            :loading="followLoading"
            @click="handleFollow"
          >
            {{ user.following ? '已关注' : '关注' }}
          </el-button>
          <el-button @click="goToUserProfile">查看主页</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
  <div v-else class="blocked-message">
    <el-text type="info" size="small">不会再推荐该用户</el-text>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Close } from '@element-plus/icons-vue'
import { followUser, unfollowUser, blockRecommendedUser } from '@/api/user'
import { useUserStore } from '@/store/modules/user'

const props = defineProps({
  user: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['followed', 'blocked'])

const router = useRouter()
const userStore = useUserStore()

const followLoading = ref(false)
const userInfoVisible = ref(false)
const hidden = ref(false)

const truncateBio = (bio) => {
  if (!bio) return ''
  return bio.length > 30 ? bio.slice(0, 30) + '...' : bio
}

const showUserInfo = () => {
  userInfoVisible.value = true
}

const goToUserProfile = () => {
  router.push(`/users/${props.user.id}`)
  userInfoVisible.value = false
}

const handleFollow = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  followLoading.value = true
  try {
    if (props.user.following) {
      await unfollowUser(props.user.id)
      props.user.following = false
      ElMessage.success('已取消关注')
    } else {
      await followUser(props.user.id)
      props.user.following = true
      ElMessage.success('关注成功')
    }
    emit('followed', props.user.id)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败，请稍后再试')
  } finally {
    followLoading.value = false
  }
}

const handleBlock = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  try {
    await blockRecommendedUser(props.user.id)
    hidden.value = true
    ElMessage.success('不会再推荐该用户')
    emit('blocked', props.user.id)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败，请稍后再试')
  }
}
</script>

<style scoped>
.recommended-user-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid #f0f0f0;
  transition: all 0.2s;
}

.recommended-user-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  position: relative;
}

.user-avatar {
  cursor: pointer;
  transition: transform 0.2s;
}

.user-avatar:hover {
  transform: scale(1.1);
}

.close-icon {
  cursor: pointer;
  color: #909399;
  font-size: 18px;
  padding: 4px;
  border-radius: 50%;
  transition: all 0.2s;
}

.close-icon:hover {
  background: #f5f5f5;
  color: #606266;
}

.card-body {
  margin-bottom: 12px;
  cursor: pointer;
}

.username {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
  margin-bottom: 4px;
}

.reason {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.bio {
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
}

.card-footer {
  display: flex;
  justify-content: center;
}

.follow-btn {
  width: 100%;
  border-radius: 20px;
  font-weight: 500;
}

.blocked-message {
  padding: 12px;
  text-align: center;
  background: #f5f7fa;
  border-radius: 12px;
  margin-bottom: 12px;
}

.user-info-dialog {
  padding: 10px 0;
}

.dialog-header {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.dialog-user-info {
  flex: 1;
}

.dialog-user-info h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #303133;
}

.bio {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.bio-placeholder {
  margin: 0;
  color: #909399;
  font-size: 14px;
  font-style: italic;
}

.dialog-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.dialog-actions .el-button {
  flex: 1;
  max-width: 150px;
}
</style>

