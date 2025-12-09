<template>
  <div class="user-home" v-if="profile">
    <el-card>
      <div class="header">
        <el-avatar :size="80" :src="profile.avatarUrl">
          <el-icon><User /></el-icon>
        </el-avatar>
        <div>
          <h2>{{ profile.username }}</h2>
          <p>ID：{{ profile.id }} ｜ {{ profile.role }}</p>
          <p class="bio">{{ profile.bio }}</p>
          <div class="stats">
            <span>关注者 {{ profile.followerCount }}</span>
            <span>关注中 {{ profile.followingCount }}</span>
          </div>
          <div class="actions" v-if="!profile.self">
            <el-button
              type="primary"
              :plain="profile.following"
              @click="toggleFollow"
              :loading="followLoading"
            >
              {{ profile.following ? '已关注' : '关注' }}
            </el-button>
            <el-button @click="openMessage">发私信</el-button>
          </div>
        </div>
      </div>
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
  }
)

onMounted(() => {
  loadProfile(route.params.id)
})
</script>

<style scoped>
.user-home {
  max-width: 900px;
  margin: 0 auto;
}

.header {
  display: flex;
  gap: 20px;
  align-items: center;
}

.bio {
  color: #606266;
  margin: 6px 0;
}

.stats {
  display: flex;
  gap: 12px;
  font-size: 14px;
  color: #909399;
  margin: 8px 0;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}
</style>
