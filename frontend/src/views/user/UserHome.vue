<template>
  <div class="user-home" v-if="profile">
    <div class="profile-hero">
      <div class="hero-left">
        <el-avatar :size="96" :src="profile.avatarUrl" :alt="`${profile.username || '用户'}的头像`">
          <el-icon><User /></el-icon>
        </el-avatar>
        <div>
          <h2>{{ profile.username }}</h2>
          <p class="uid">UID {{ profile.id }} · {{ profile.role }}</p>
          <p class="bio">{{ profile.bio || '这位用户很低调，还没有简介' }}</p>
          <div class="stats">
            <div>
              <strong>{{ profile.followerCount }}</strong>
              <span>粉丝</span>
            </div>
            <div>
              <strong>{{ profile.followingCount }}</strong>
              <span>关注</span>
            </div>
          </div>
        </div>
      </div>
      <div class="hero-right" v-if="!profile.self">
        <el-button
          type="primary"
          :plain="profile.following"
          :loading="followLoading"
          @click="toggleFollow"
        >
          {{ profile.following ? '已关注' : '关注' }}
        </el-button>
        <el-button @click="openMessage">发私信</el-button>
      </div>
    </div>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>最近动态</span>
        </div>
      </template>
      <el-empty description="Ta 的公开动态暂未开放，敬请期待" />
    </el-card>

    <el-dialog v-model="messageVisible" title="发送私信">
      <el-input
        v-model="messageForm.content"
        type="textarea"
        :rows="4"
        placeholder="把想说的话写在这里..."
      />
      <template #footer>
        <el-button @click="messageVisible = false">取消</el-button>
        <el-button type="primary" :loading="messageLoading" @click="sendMessageToUser">
          发送
        </el-button>
      </template>
    </el-dialog>
  </div>
  <el-empty v-else description="正在加载..." />
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { fetchUserProfile, followUser, unfollowUser } from '@/api/user'
import { sendMessage } from '@/api/message'
import { useUserStore } from '@/store/modules/user'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const profile = ref(null)
const followLoading = ref(false)

const messageVisible = ref(false)
const messageForm = reactive({
  content: ''
})
const messageLoading = ref(false)

const loadProfile = async (id) => {
  const res = await fetchUserProfile(id)
  profile.value = res.data
}

const ensureLogin = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return false
  }
  return true
}

const toggleFollow = async () => {
  if (!ensureLogin()) return
  followLoading.value = true
  try {
    if (profile.value.following) {
      await unfollowUser(profile.value.id)
      profile.value.following = false
      profile.value.followerCount -= 1
    } else {
      await followUser(profile.value.id)
      profile.value.following = true
      profile.value.followerCount += 1
    }
  } finally {
    followLoading.value = false
  }
}

const openMessage = () => {
  if (!ensureLogin()) return
  messageVisible.value = true
}

const sendMessageToUser = async () => {
  if (!messageForm.content.trim()) {
    ElMessage.warning('请输入私信内容')
    return
  }
  messageLoading.value = true
  try {
    await sendMessage(profile.value.id, { content: messageForm.content })
    ElMessage.success('私信已发送')
    messageForm.content = ''
    messageVisible.value = false
  } finally {
    messageLoading.value = false
  }
}

watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      loadProfile(newId)
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadProfile(route.params.id)
})
</script>

<style scoped>
.user-home {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-hero {
  background: #fff;
  border-radius: 24px;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  box-shadow: 0 12px 30px rgba(31, 45, 61, 0.08);
}

.hero-left {
  display: flex;
  gap: 20px;
}

.uid {
  color: #909399;
  margin: 4px 0;
}

.bio {
  color: #606266;
  margin: 8px 0;
}

.stats {
  display: flex;
  gap: 24px;
}

.stats div {
  text-align: center;
}

.stats strong {
  font-size: 20px;
  display: block;
}

.stats span {
  font-size: 12px;
  color: #909399;
}

.hero-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
