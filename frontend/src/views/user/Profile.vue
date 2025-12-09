<template>
  <div class="profile-page" v-if="userStore.userInfo">
    <div class="profile-grid">
      <el-card>
        <div class="profile-header">
          <el-avatar :size="80" :src="userStore.userInfo.avatarUrl">
            <el-icon><User /></el-icon>
          </el-avatar>
          <div>
            <h2>{{ userStore.userInfo.username }}</h2>
            <p>ID：{{ userStore.userInfo.id }} ｜ {{ userStore.userInfo.role }}</p>
            <p class="bio">{{ userStore.userInfo.bio }}</p>
          </div>
        </div>
        <div class="stats">
          <div>
            <strong>{{ stats.followerCount }}</strong>
            <span>关注者</span>
          </div>
          <div>
            <strong>{{ stats.followingCount }}</strong>
            <span>我关注的</span>
          </div>
        </div>
      </el-card>

      <el-card>
        <template #header>
          <div class="card-header">
            <span>完善个人资料</span>
            <el-tag size="small" type="info">保存后将同步更新在 Wiki、圈子等模块</el-tag>
          </div>
        </template>
        <el-form :model="profileForm" label-width="80px">
          <el-form-item label="头像链接">
            <el-input v-model="profileForm.avatarUrl" placeholder="支持网络图片地址" />
          </el-form-item>
          <el-form-item label="个人简介">
            <el-input
              v-model="profileForm.bio"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <div class="messages">
      <el-card class="conversation-card">
        <template #header>
          <div class="card-header">
            <span>私信列表</span>
            <el-button link @click="loadConversations">
              <el-icon><Refresh /></el-icon>刷新
            </el-button>
          </div>
        </template>
        <el-empty v-if="conversations.length === 0" description="暂时还没有私信" />
        <el-scrollbar v-else height="240px">
          <div
            v-for="item in conversations"
            :key="item.peer.id"
            :class="['conversation-item', { active: currentPeer && currentPeer.id === item.peer.id }]"
            @click="openConversation(item.peer)"
          >
            <div class="conversation-title">
              <span>{{ item.peer.username }}</span>
              <el-tag v-if="item.unreadCount" size="small" type="danger">{{ item.unreadCount }}</el-tag>
            </div>
            <div class="conversation-preview">{{ item.lastMessage }}</div>
            <div class="conversation-time">{{ formatTime(item.lastMessageTime) }}</div>
          </div>
        </el-scrollbar>
      </el-card>

      <el-card class="chat-card">
        <template #header>
          <div class="card-header">
            <span>{{ currentPeer ? `与 ${currentPeer.username} 的私信` : '请选择一位好友开始对话' }}</span>
          </div>
        </template>
        <div class="chat-window">
          <el-scrollbar height="260px" ref="messageScrollRef">
            <el-empty v-if="!currentPeer" description="选择左侧联系人查看消息" />
            <div v-else>
              <div v-for="message in messages" :key="message.id" :class="['message', { mine: message.mine }]">
                <div class="bubble">
                  <p>{{ message.content }}</p>
                  <span>{{ formatTime(message.createdAt) }}</span>
                </div>
              </div>
            </div>
          </el-scrollbar>
          <div class="chat-input" v-if="currentPeer">
            <el-input
              type="textarea"
              :rows="3"
              v-model="messageForm.content"
              placeholder="输入内容并发送"
            />
            <el-button type="primary" class="send-btn" :loading="sending" @click="sendMessageContent">
              发送
            </el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
  <el-empty v-else description="正在加载..." />
</template>

<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import { useUserStore } from '@/store/modules/user'
import { updateProfile } from '@/api/user'
import { fetchConversations, fetchConversationWith, sendMessage } from '@/api/message'
import { ElMessage } from 'element-plus'

dayjs.extend(relativeTime)

const userStore = useUserStore()
const profileForm = reactive({
  avatarUrl: '',
  bio: ''
})
const stats = reactive({
  followerCount: 0,
  followingCount: 0
})
const saving = ref(false)

const conversations = ref([])
const currentPeer = ref(null)
const messages = ref([])
const messageForm = reactive({ content: '' })
const messageScrollRef = ref()
const sending = ref(false)

const loadProfile = async () => {
  await userStore.fetchUserInfo().catch(() => {})
  if (userStore.userInfo) {
    profileForm.avatarUrl = userStore.userInfo.avatarUrl
    profileForm.bio = userStore.userInfo.bio
    stats.followerCount = userStore.userInfo.followerCount
    stats.followingCount = userStore.userInfo.followingCount
  }
}

const saveProfile = async () => {
  saving.value = true
  try {
    await updateProfile(profileForm)
    await loadProfile()
    ElMessage.success('资料已更新')
  } finally {
    saving.value = false
  }
}

const loadConversations = async () => {
  const res = await fetchConversations()
  conversations.value = res.data
}

const openConversation = async (peer) => {
  currentPeer.value = peer
  const res = await fetchConversationWith(peer.id)
  messages.value = res.data
  await nextTick(() => {
    const wrapper = messageScrollRef.value?.wrapRef
    if (wrapper) {
      wrapper.scrollTop = wrapper.scrollHeight
    }
  })
  loadConversations()
}

const sendMessageContent = async () => {
  if (!messageForm.content.trim()) {
    ElMessage.warning('请输入聊天内容')
    return
  }
  sending.value = true
  try {
    await sendMessage(currentPeer.value.id, { content: messageForm.content })
    messageForm.content = ''
    await openConversation(currentPeer.value)
    ElMessage.success('已发送')
  } finally {
    sending.value = false
  }
}

const formatTime = (time) => dayjs(time).fromNow()

onMounted(async () => {
  await loadProfile()
  await loadConversations()
})
</script>

<style scoped>
.profile-page {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.profile-header {
  display: flex;
  gap: 20px;
  align-items: center;
}

.profile-header .bio {
  color: #606266;
  margin-top: 6px;
}

.stats {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
}

.stats div {
  text-align: center;
}

.messages {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 20px;
}

.conversation-item {
  padding: 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  margin-bottom: 10px;
}

.conversation-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.conversation-title {
  display: flex;
  justify-content: space-between;
  font-weight: 600;
}

.conversation-preview {
  color: #606266;
  font-size: 13px;
  margin: 4px 0;
}

.conversation-time {
  font-size: 12px;
  color: #909399;
}

.chat-window {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message {
  display: flex;
  margin-bottom: 12px;
}

.message.mine {
  justify-content: flex-end;
}

.bubble {
  max-width: 70%;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f5f7fa;
}

.message.mine .bubble {
  background: #409eff;
  color: #fff;
}

.bubble span {
  display: block;
  font-size: 12px;
  margin-top: 4px;
  color: rgba(0, 0, 0, 0.45);
}

.message.mine .bubble span {
  color: rgba(255, 255, 255, 0.8);
}

.chat-input {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.send-btn {
  align-self: flex-end;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
