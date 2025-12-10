<template>
  <div class="nearby-container">
    <div class="city-selector">
      <label>选择城市:</label>
      <el-select v-model="selectedCity" @change="onCityChange" placeholder="选择城市">
        <el-option value="北京" label="北京" />
        <el-option value="上海" label="上海" />
        <el-option value="广州" label="广州" />
        <el-option value="深圳" label="深圳" />
        <el-option value="杭州" label="杭州" />
        <el-option value="成都" label="成都" />
        <el-option value="武汉" label="武汉" />
        <el-option value="厦门" label="厦门" />
        <el-option value="南京" label="南京" />
      </el-select>
    </div>

    <div v-if="loading && posts.length === 0" class="loading">加载中...</div>
    <div v-else-if="posts.length === 0" class="empty">
      <el-empty :description="`${selectedCity}还没有动态`" />
    </div>
    <div v-else class="posts-grid">
      <div
        v-for="post in posts"
        :key="post.id"
        class="post-card"
        @click="goToPost(post.id)"
      >
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

    <!-- 加载更多 -->
    <div v-if="!loading && hasMore" class="load-more">
      <el-button @click="loadMore">加载更多</el-button>
    </div>
    <div v-if="!hasMore && posts.length > 0" class="no-more">
      已加载全部内容
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { circleApi } from '@/api/circle'
import { Picture } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()

const selectedCity = ref('北京')
const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const hasMore = ref(true)

const loadPosts = async () => {
  loading.value = true
  try {
    const response = await circleApi.listPostsByCity(selectedCity.value, currentPage.value, pageSize.value)
    const pageData = response.data

    if (currentPage.value === 1) {
      posts.value = pageData.items || []
    } else {
      posts.value.push(...(pageData.items || []))
    }
    hasMore.value = (pageData.items || []).length >= pageSize.value
  } catch (error) {
    console.error('Failed to load nearby posts:', error)
  } finally {
    loading.value = false
  }
}

const onCityChange = () => {
  currentPage.value = 1
  posts.value = []
  hasMore.value = true
  loadPosts()
}

const loadMore = () => {
  currentPage.value++
  loadPosts()
}

const goToPost = (postId) => {
  router.push(`/posts/${postId}`)
}

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
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  font-size: 14px;
  color: #333;
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
  margin-bottom: 20px;
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

.load-more {
  text-align: center;
  padding: 20px;
  margin-top: 20px;
}

.no-more {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 12px;
}
</style>

