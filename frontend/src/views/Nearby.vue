<template>
  <div class="nearby-container">
    <div class="city-selector">
      <el-select v-model="selectedCity" placeholder="选择城市">
        <el-option value="beijing" label="北京" />
        <el-option value="shanghai" label="上海" />
        <el-option value="guangzhou" label="广州" />
        <el-option value="shenzhen" label="深圳" />
        <el-option value="hangzhou" label="杭州" />
        <el-option value="chengdu" label="成都" />
        <el-option value="wuhan" label="武汉" />
        <el-option value="xiamen" label="厦门" />
        <el-option value="nanjing" label="南京" />
      </el-select>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="posts.length === 0" class="empty">该城市还没有动态</div>
    <div v-else class="posts-grid">
      <div v-for="post in posts" :key="post.id" class="post-card">
        <div v-if="post.imageUrls && post.imageUrls.length > 0" class="post-image">
          <img :src="post.imageUrls[0]" :alt="post.content" />
        </div>
        <div v-else class="post-image-placeholder">
          <el-icon><Picture /></el-icon>
        </div>
        <div class="post-meta">
          <div class="post-author">
            <el-avatar :src="post.author.avatarUrl" :size="24" />
            <span>{{ post.author.username }}</span>
          </div>
          <div class="post-location">📍 {{ post.location }}</div>
          <div class="post-time">{{ formatTime(post.createdAt) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { circleApi } from '@/api/circle'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const selectedCity = ref('beijing')
const posts = ref([])
const loading = ref(false)

const loadPosts = async () => {
  loading.value = true
  try {
    const response = await circleApi.listPosts(1, 20)
    posts.value = (response.data || []).filter(post => post.location)
  } catch (error) {
    console.error('Failed to load nearby posts:', error)
  } finally {
    loading.value = false
  }
}

watch(selectedCity, () => {
  loadPosts()
}, { immediate: true })

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
.nearby-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.city-selector {
  margin-bottom: 24px;
}

:deep(.el-select) {
  width: 200px;
}

.loading, .empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.posts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.post-card {
  border-radius: 8px;
  overflow: hidden;
  background-color: #fff;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.3s;
}

.post-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.post-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background-color: #f5f5f5;
}

.post-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.post-image-placeholder {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  color: #999;
  font-size: 32px;
}

.post-meta {
  padding: 12px;
}

.post-author {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
}

.post-location {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.post-time {
  font-size: 11px;
  color: #999;
}
</style>
