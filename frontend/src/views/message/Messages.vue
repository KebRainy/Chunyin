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
            <el-avatar :src="conv.peer.avatarUrl" :size="40" />
            <div class="conv-info">
              <div class="conv-user">{{ conv.peer.username }}</div>
              <div class="conv-preview">{{ conv.lastMessage }}</div>
            </div>
            <div v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount }}</div>
          </div>
        </div>
      </div>

      <!-- 右侧聊天窗口 -->
      <div class="chat-panel">
        <div v-if="!activeConversation" class="no-conversation">
          <el-empty description="选择一个会话开始聊天" />
        </div>
        <div v-else>
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
              <el-avatar v-if="!msg.mine" :src="currentUser?.avatarUrl" :size="32" />
              <div class="message-bubble">
                <p>{{ msg.content }}</p>
                <span>{{ msg.createdAt }}</span>
              </div>
            </div>
          </div>

          <!-- 输入框 -->
          <div class="chat-footer">
            <el-input
              v-model="messageInput"
              placeholder="输入消息..."
              @keyup.enter="sendMessage"
            />
            <el-button type="primary" @click="sendMessage">发送</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { messageApi } from '@/api/message'
import { ElMessage } from 'element-plus'

const searchKeyword = ref('')
const activeConversation = ref(null)
const messageInput = ref('')
const conversations = ref([])
const messages = ref([])
const chatBodyRef = ref(null)

const filteredConversations = computed(() => {
  if (!searchKeyword.value) return conversations.value
  return conversations.value.filter(conv =>
    conv.peer?.username?.includes(searchKeyword.value)
  )
})

const currentUser = computed(() => {
  return conversations.value.find(c => c.peer?.id === activeConversation.value)?.peer
})

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
  height: calc(100vh - 64px);
  display: flex;
}

.messages-layout {
  display: flex;
  width: 100%;
  background-color: #fff;
}

.conversations-panel {
  width: 280px;
  border-right: 1px solid #f0f0f0;
  padding: 16px;
  overflow-y: auto;
}

.conversations-panel h3 {
  margin: 0 0 16px 0;
}

.conversations-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.3s;
  position: relative;
}

.conversation-item:hover {
  background-color: #f5f5f5;
}

.conversation-item.active {
  background-color: #e6f7ff;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-user {
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-preview {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.unread-badge {
  background-color: #f56c6c;
  color: #fff;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.no-conversation {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-header {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.chat-header h3 {
  margin: 0;
}

.messages-list {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.message-item.self {
  justify-content: flex-end;
}

.message-bubble {
  background-color: #f0f0f0;
  padding: 12px 16px;
  border-radius: 12px;
  max-width: 60%;
  word-break: break-word;
  line-height: 1.5;
}

.message-item.self .message-bubble {
  background-color: #409eff;
  color: #fff;
}

.chat-footer {
  padding: 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 8px;
}

:deep(.el-input) {
  flex: 1;
}
</style>
