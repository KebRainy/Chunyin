<template>
  <div class="post-detail-container">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!post" class="not-found">动态不存在</div>
    <div v-else class="post-detail">
      <!-- 动态头部 -->
      <div class="post-header">
        <el-avatar :src="post.author.avatarUrl" :size="48" />
        <div class="author-info">
          <div class="username">{{ post.author.username }}</div>
          <div class="post-meta">
            <span class="time">{{ formatTime(post.createdAt) }}</span>
            <span class="location" v-if="post.location">📍 {{ post.location }}</span>
            <span class="ip-location">{{ post.ipAddressMasked }}</span>
          </div>
        </div>
      </div>

      <!-- 动态内容 -->
      <div class="post-body">
        <div class="post-content">{{ post.content }}</div>

        <!-- 动态图片 -->
        <div v-if="post.imageUrls && post.imageUrls.length > 0" class="post-images">
          <img v-for="(url, idx) in post.imageUrls" :key="idx" :src="url" :alt="`image-${idx}`" />
        </div>

        <!-- 动态标签 -->
        <div class="post-tags">
          <el-tag v-if="post.location">{{ post.location }}</el-tag>
        </div>
      </div>

      <!-- 交互按钮 -->
      <div class="post-actions">
        <div class="action-item">
          <el-icon><View /></el-icon>
          <span>浏览 0</span>
        </div>
        <div class="action-item" @click="toggleLike">
          <el-icon :class="{ liked: liked }"><Heart /></el-icon>
          <span>点赞 {{ likeCount }}</span>
        </div>
        <div class="action-item">
          <el-icon><ChatDotSquare /></el-icon>
          <span>评论 0</span>
        </div>
        <div class="action-item" @click="toggleStar">
          <el-icon :class="{ starred: starred }"><StarFilled /></el-icon>
          <span>收藏</span>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="comments-section">
        <h3>评论</h3>
        <el-input
          v-model="commentText"
          type="textarea"
          placeholder="写下你的评论..."
          rows="3"
        />
        <el-button type="primary" @click="submitComment">发布评论</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { circleApi } from '@/api/circle'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const route = useRoute()
const post = ref(null)
const loading = ref(true)
const liked = ref(false)
const starred = ref(false)
const likeCount = ref(0)
const commentText = ref('')

onMounted(async () => {
  try {
    // TODO: 实现获取单条动态的API
    // const response = await circleApi.getPost(route.params.id)
    // post.value = response.data
  } catch (error) {
    console.error('Failed to load post:', error)
  } finally {
    loading.value = false
  }
})

const formatTime = (time) => {
  if (!time) return ''
  const date = dayjs(time)
  const now = dayjs()
  const diff = now.diff(date, 'minute')

  if (diff < 1) return '刚刚'
  if (diff < 60) return `${diff}分钟前`
  if (diff < 1440) return `${Math.floor(diff / 60)}小时前`
  if (date.isSame(now, 'year')) return date.format('M月D日 HH:mm')
  return date.format('YYYY年M月D日 HH:mm')
}

const toggleLike = () => {
  liked.value = !liked.value
  if (liked.value) likeCount.value++
  else likeCount.value--
}

const toggleStar = () => {
  starred.value = !starred.value
}

const submitComment = () => {
  // TODO: 实现发布评论
  console.log('Comment:', commentText.value)
  commentText.value = ''
}
</script>

<style scoped>
.post-detail-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.loading, .not-found {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.post-detail {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #f0f0f0;
}

.post-header {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.author-info {
  flex: 1;
}

.username {
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.post-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #999;
  flex-wrap: wrap;
}

.post-body {
  margin-bottom: 16px;
}

.post-content {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  margin-bottom: 16px;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 8px;
  margin-bottom: 16px;
}

.post-images img {
  width: 100%;
  height: auto;
  border-radius: 4px;
  max-height: 400px;
  object-fit: cover;
}

.post-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.post-actions {
  display: flex;
  gap: 24px;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #666;
  transition: color 0.3s;
}

.action-item:hover {
  color: #409eff;
}

.action-item .el-icon {
  font-size: 18px;
}

.action-item .el-icon.liked {
  color: #f56c6c;
}

.action-item .el-icon.starred {
  color: #ffd666;
}

.comments-section {
  margin-top: 20px;
}

.comments-section h3 {
  margin-bottom: 16px;
  color: #333;
}

:deep(.el-textarea__inner) {
  margin-bottom: 12px;
}
</style>
