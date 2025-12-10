<template>
  <div class="feed-container">
    <h2>关注的动态</h2>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="posts.length === 0" class="empty">还没有关注的动态</div>
    <div v-else class="posts-list">
      <div v-for="post in posts" :key="post.id" class="post-card">
        <div class="post-header">
          <el-avatar :src="post.author.avatarUrl" :size="40" />
          <div class="post-info">
            <div class="author-name">{{ post.author.username }}</div>
            <div class="post-time">{{ formatTime(post.createdAt) }}</div>
          </div>
        </div>
        <div class="post-content">{{ post.content }}</div>
        <div v-if="post.imageUrls && post.imageUrls.length > 0" class="post-images">
          <img v-for="(url, idx) in post.imageUrls" :key="idx" :src="url" :alt="`image-${idx}`" />
        </div>
        <div class="post-location" v-if="post.location">📍 {{ post.location }}</div>
        <div class="post-footer">
          <span>浏览: 0</span>
          <span>点赞: 0</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/modules/user'
import { circleApi } from '@/api/circle'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const userStore = useUserStore()
const posts = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const response = await circleApi.listPosts(1, 10)
    posts.value = response.data || []
  } catch (error) {
    console.error('Failed to load feed:', error)
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
  if (date.isSame(now, 'year')) return date.format('M月D日')
  return date.format('YYYY年M月D日')
}
</script>

<style scoped>
.feed-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  color: #333;
}

.loading, .empty {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  background-color: #fff;
}

.post-header {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.post-info {
  flex: 1;
}

.author-name {
  font-weight: 500;
  color: #333;
}

.post-time {
  font-size: 12px;
  color: #999;
}

.post-content {
  margin-bottom: 12px;
  line-height: 1.6;
  color: #555;
}

.post-images {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.post-images img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 4px;
  object-fit: cover;
}

.post-location {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.post-footer {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}
</style>
