<template>
  <div class="messages-container">
    <div class="messages-layout">
      <!-- 左侧会话列表 -->
      <div class="conversations-panel">
        <h3>消息</h3>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索会话"
          clearable
          style="margin-bottom: 16px"
        />
        <div class="conversations-list">
          <div
            v-for="conv in filteredConversations"
            :key="conv.peer.id"
            :class="['conversation-item', { active: activeConversation === conv.peer.id }]"
            @click="selectConversation(conv.peer.id)"
          >
            <el-avatar
              :src="conv.peer.avatarUrl"
              :size="40"
              :alt="`${conv.peer.username || '用户'}的头像`"
            />
            <div class="conv-info">
              <div class="conv-user">{{ conv.peer.username }}</div>
              <div class="conv-preview">{{ conv.lastMessage }}</div>
            </div>
            <div v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount }}</div>
          </div>
        </div>
      </div>

      <!-- 右侧聊天窗口 -->
      <div class="chat-section">
        <div class="chat-panel">
          <div v-if="!activeConversation" class="no-conversation">
            <el-empty description="选择一个会话开始聊天" />
          </div>
          <div v-else class="chat-body">
            <!-- 聊天头部 -->
            <div class="chat-header">
              <h3>{{ currentUser?.username }}</h3>
            </div>

            <!-- 消息列表 -->
            <div class="messages-list" ref="chatBodyRef">
              <div
                v-for="msg in messages"
                :key="msg.id"
                :class="['message-item', msg.mine ? 'self' : 'other']"
              >
                <div v-if="!msg.mine" class="message-avatar">
                  <el-avatar
                    :src="currentUser?.avatarUrl"
                    :size="32"
                    :alt="`${currentUser?.username || '用户'}的头像`"
                  />
                </div>
                <div class="message-content">
                  <div class="message-bubble">
                    <p>{{ msg.content }}</p>
                  </div>
                  <span class="message-time">{{ formatMessageTime(msg.createdAt) }}</span>
                </div>

                <div v-if="msg.mine" class="message-avatar">
                  <el-avatar
                    :src="userStore.userInfo?.avatarUrl"
                    :size="32"
                    :alt="`${userStore.userInfo?.username || '我的'}的头像`"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入框 -->
        <div class="chat-footer" :class="{ disabled: !activeConversation }">
          <el-input
            v-model="messageInput"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 5 }"
            :placeholder="activeConversation ? '输入消息...' : '选择一个会话以发送消息'"
            :disabled="!activeConversation"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <el-button type="primary" :disabled="!activeConversation" @click="sendMessage">发送</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { messageApi } from '@/api/message'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { useUserStore } from '@/store/modules/user'

const searchKeyword = ref('')
const activeConversation = ref(null)
const messageInput = ref('')
const conversations = ref([])
const messages = ref([])
const chatBodyRef = ref(null)
const userStore = useUserStore()

const filteredConversations = computed(() => {
  if (!searchKeyword.value) return conversations.value
  return conversations.value.filter(conv =>
    conv.peer?.username?.includes(searchKeyword.value)
  )
})

const currentUser = computed(() => {
  return conversations.value.find(c => c.peer?.id === activeConversation.value)?.peer
})

const formatMessageTime = (time) => {
  if (!time) return ''
  // return dayjs(time).format('YYYY.MM.DD HH:mm')
  return dayjs(time).format('MM-DD HH:mm')
}

const scrollToBottom = () => {
  nextTick(() => {
    const el = chatBodyRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

const loadConversations = async () => {
  try {
    const res = await messageApi.fetchConversations()
    conversations.value = res.data || []
    if (!activeConversation.value && conversations.value.length > 0) {
      selectConversation(conversations.value[0].peer.id)
    }
  } catch (error) {
    ElMessage.error('加载私信失败')
  }
}

const selectConversation = async (userId) => {
  activeConversation.value = userId
  try {
    const res = await messageApi.fetchConversationWith(userId)
    messages.value = res.data || []
    scrollToBottom()
  } catch (error) {
    ElMessage.error('加载对话失败')
  }
}

const sendMessage = async () => {
  if (!messageInput.value.trim() || !activeConversation.value) return
  try {
    await messageApi.sendMessage(activeConversation.value, { content: messageInput.value })
    messageInput.value = ''
    await selectConversation(activeConversation.value)
    await loadConversations()
  } catch (error) {
    ElMessage.error('发送失败，请稍后再试')
  }
}

onMounted(() => {
  loadConversations()
})
</script>

<style scoped>
.messages-container {
  height: calc(100vh - 68px);
  padding: 24px;
  background: #fff;
  box-sizing: border-box;
  display: flex;
}

.messages-layout {
  display: flex;
  width: 100%;
  height: 100%;
  border: 1px solid #eceff5;
  border-radius: 32px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.07);
}

.conversations-panel {
  width: 300px;
  border-right: 1px solid #f0f0f0;
  padding: 24px 20px;
  background: #f9fafc;
  display: flex;
  flex-direction: column;
}

.conversations-panel h3 {
  margin: 0 0 16px 0;
}

.chat-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.conversations-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  padding-right: 6px;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 14px;
  cursor: pointer;
  transition: background-color 0.3s;
  position: relative;
}

.conversation-item:hover {
  background-color: #eef2ff;
}

.conversation-item.active {
  background-color: #e5edff;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-user {
  font-weight: 500;
  color: #1f2d3d;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-preview {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.unread-badge {
  background-color: #f56c6c;
  color: #fff;
  border-radius: 50%;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: bold;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  min-height: 0;
}

.chat-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.no-conversation {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-header {
  padding: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.chat-header h3 {
  margin: 0;
}

.messages-list {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #fafafa;
  min-height: 0;
}

.message-item {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  width: 100%;
}

.message-item.self {
  justify-content: flex-end;
  margin-left: auto;
}

.message-avatar {
  flex-shrink: 0;
  display: flex;
  align-items: flex-end;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 70%;
  min-width: 0;
}

.message-item.self .message-content {
  align-items: flex-end;
}

.message-bubble {
  background-color: #fff;
  padding: 12px 16px;
  border-radius: 16px 16px 16px 4px;
  min-width: 60px;
  max-width: 100%;
  word-break: break-word;
  white-space: pre-wrap;
  line-height: 1.5;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
}

.message-bubble p {
  margin: 0;
}

.message-item.self .message-bubble {
  background-color: #2f54eb;
  color: #fff;
  border-radius: 16px 16px 4px 16px;
  box-shadow: none;
  min-width: 60px;
  max-width: 100%;
}

.message-time {
  font-size: 11px;
  color: #a0a3ad;
}

.message-item.self .message-time {
  text-align: right;
  color: #a0a3ad;
}

.chat-footer {
  background: #fff;
  padding: 20px 24px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.chat-footer.disabled {
  opacity: 0.6;
}

:deep(.chat-footer .el-textarea) {
  flex: 1;
}
</style>
