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
            v-for="conv in conversations"
            :key="conv.id"
            :class="['conversation-item', { active: activeConversation === conv.id }]"
            @click="selectConversation(conv.id)"
          >
            <el-avatar :src="conv.avatar" :size="40" />
            <div class="conv-info">
              <div class="conv-user">{{ conv.username }}</div>
              <div class="conv-preview">{{ conv.lastMessage }}</div>
            </div>
            <div v-if="conv.unread > 0" class="unread-badge">{{ conv.unread }}</div>
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
          <div class="messages-list">
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['message-item', msg.isSelf ? 'self' : 'other']"
            >
              <el-avatar
                v-if="!msg.isSelf"
                :src="currentUser?.avatarUrl"
                :size="32"
              />
              <div class="message-bubble">{{ msg.content }}</div>
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
import { ref, computed } from 'vue'
import { messageApi } from '@/api/message'

const searchKeyword = ref('')
const activeConversation = ref(null)
const messageInput = ref('')
const conversations = ref([
  {
    id: 1,
    username: '用户A',
    avatar: 'https://api.dicebear.com/7.x/thumbs/svg?seed=user1',
    lastMessage: '最后一条消息预览...',
    unread: 2
  }
])
const messages = ref([
  { id: 1, content: '你好', isSelf: false },
  { id: 2, content: '你好啊', isSelf: true }
])

const currentUser = computed(() => {
  const conv = conversations.value.find(c => c.id === activeConversation.value)
  return conv
})

const selectConversation = (id) => {
  activeConversation.value = id
}

const sendMessage = () => {
  if (!messageInput.value.trim()) return

  messages.value.push({
    id: messages.value.length + 1,
    content: messageInput.value,
    isSelf: true
  })
  messageInput.value = ''
}
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
