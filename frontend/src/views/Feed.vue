<template>
  <div class="feed-container">
    <!-- 关注用户头像列表 -->
    <div class="followees-section">
      <div v-if="followeesLoading" class="followees-loading">加载关注列表...</div>
      <div v-else class="followees-list">
        <div
          v-for="followee in followees"
          :key="followee.id"
          class="followee-avatar"
          :title="followee.username"
          @click="goToUser(followee.id)"
        >
          <el-avatar :src="followee.avatarUrl" :size="48" />
          <span class="followee-name">{{ followee.username }}</span>
        </div>
      </div>
    </div>

    <!-- 动态列表 -->
    <div class="feed-posts">
      <div v-if="loading && posts.length === 0" class="loading">加载中...</div>
      <div v-else-if="posts.length === 0" class="empty">
        <el-empty description="还没有关注的动态" />
      </div>
      <div v-else class="posts-list">
        <div
          v-for="post in posts"
          :key="post.id"
          class="post-card"
          @click="goToPost(post.id)"
        >
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

      <!-- 加载更多 -->
      <div v-if="!loading && hasMore" class="load-more">
        <el-button @click="loadMore">加载更多</el-button>
      </div>
      <div v-if="!hasMore && posts.length > 0" class="no-more">
        已加载全部内容
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { circleApi } from '@/api/circle'
import { getFollowees } from '@/api/user'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()

const posts = ref([])
const loading = ref(true)
const currentPage = ref(1)
const pageSize = ref(12)
const hasMore = ref(true)

const followees = ref([])
const followeesLoading = ref(true)

const loadFollowees = async () => {
  try {
    const response = await getFollowees()
    followees.value = response.data || []
  } catch (error) {
    console.error('Failed to load followees:', error)
  } finally {
    followeesLoading.value = false
  }
}

const loadPosts = async () => {
  loading.value = true
  try {
    const response = await circleApi.listFeed(currentPage.value, pageSize.value)
    const pageData = response.data
    const items = pageData.items || []

    if (currentPage.value === 1) {
      posts.value = items
    } else {
      posts.value.push(...items)
    }
    hasMore.value = items.length >= pageSize.value
  } catch (error) {
    console.error('Failed to load feed:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  currentPage.value++
  loadPosts()
}

const goToPost = (postId) => {
  router.push(`/posts/${postId}`)
}

const goToUser = (userId) => {
  router.push(`/user/${userId}`)
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

onMounted(() => {
  loadFollowees()
  loadPosts()
})
</script>

<style scoped>
.feed-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 关注用户列表 */
.followees-section {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.followees-loading {
  text-align: center;
  padding: 20px;
  color: #999;
}

.followees-list {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding: 10px 0;
}

.followee-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: transform 0.2s;
  flex-shrink: 0;
}

.followee-avatar:hover {
  transform: scale(1.1);
}

.followee-name {
  font-size: 12px;
  color: #666;
  text-align: center;
  max-width: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 动态列表 */
.feed-posts {
  min-height: 400px;
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
  cursor: pointer;
  transition: box-shadow 0.3s;
}

.post-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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
  margin-bottom: 4px;
}

.post-time {
  font-size: 12px;
  color: #999;
}

.post-content {
  margin-bottom: 12px;
  line-height: 1.6;
  color: #555;
  white-space: pre-wrap;
  word-break: break-word;
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
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
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
