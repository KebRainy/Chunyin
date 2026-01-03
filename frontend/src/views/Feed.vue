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
          <el-avatar
            :src="followee.avatarUrl"
            :size="48"
            :alt="`${followee.username}的头像`"
          />
          <span class="followee-name">{{ followee.username }}</span>
        </div>
      </div>
    </div>

    <!-- 主要内容区域：动态列表 + 推荐用户 -->
    <div class="main-content">
      <!-- 动态列表 -->
      <div class="feed-posts">
        <div v-if="loading && posts.length === 0" class="loading">加载中...</div>
        <div v-else-if="posts.length === 0" class="empty">
          <el-empty description="还没有关注的动态" />
        </div>
        <div v-else class="masonry-grid">
          <PostCard
            v-for="post in posts"
            :key="post.id"
            :post="post"
            @select="goToPost(post.id)"
          />
        </div>

        <!-- 加载更多 -->
        <div v-if="!loading && hasMore" class="load-more">
          <el-button @click="loadMore">加载更多</el-button>
        </div>
        <div v-if="!hasMore && posts.length > 0" class="no-more">
          已加载全部内容
        </div>
      </div>

      <!-- 推荐用户列 -->
      <div class="recommended-users-sidebar">
        <div class="sidebar-header">
          <h3>你可能感兴趣的人</h3>
        </div>
        <div v-if="recommendedUsersLoading" class="loading-users">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="recommendedUsers.length === 0" class="empty-users">
          <el-empty description="暂无推荐" :image-size="80" />
        </div>
        <div v-else class="users-list">
          <RecommendedUserCard
            v-for="user in recommendedUsers"
            :key="user.id"
            :user="user"
            @followed="handleUserFollowed"
            @blocked="handleUserBlocked"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { circleApi } from '@/api/circle'
import { getFollowees } from '@/api/user'
import { recommendApi } from '@/api/recommend'
import PostCard from '@/components/PostCard.vue'
import RecommendedUserCard from '@/components/RecommendedUserCard.vue'

const router = useRouter()
const userStore = useUserStore()

const posts = ref([])
const loading = ref(true)
const currentPage = ref(1)
const pageSize = ref(12)
const hasMore = ref(true)

const followees = ref([])
const followeesLoading = ref(true)

const recommendedUsers = ref([])
const recommendedUsersLoading = ref(true)

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
  router.push(`/users/${userId}`)
}

const loadRecommendedUsers = async (retryCount = 0) => {
  if (!userStore.isLoggedIn) {
    recommendedUsersLoading.value = false
    return
  }

  recommendedUsersLoading.value = true
  try {
    const response = await recommendApi.getRecommendedUsers(10)
    recommendedUsers.value = response.data || []
  } catch (error) {
    // 如果是连接错误且重试次数小于3次，则延迟重试
    const isConnectionError = error.code === 'ECONNREFUSED' || 
                             error.message?.includes('ECONNREFUSED') ||
                             error.message?.includes('Network Error') ||
                             error.message?.includes('Proxy error') ||
                             error.response === undefined
    
    if (isConnectionError && retryCount < 3) {
      // 延迟重试：第1次等待1秒，第2次等待2秒，第3次等待3秒
      await new Promise(resolve => setTimeout(resolve, (retryCount + 1) * 1000))
      return loadRecommendedUsers(retryCount + 1)
    }
    
    // 连接错误或重试次数用完，静默处理，不显示错误
    if (!isConnectionError) {
      console.error('Failed to load recommended users:', error)
    }
  } finally {
    recommendedUsersLoading.value = false
  }
}

const handleUserFollowed = (userId) => {
  // 更新推荐用户列表中的关注状态
  const user = recommendedUsers.value.find(u => u.id === userId)
  if (user) {
    user.following = true
  }
}

const handleUserBlocked = (userId) => {
  // 从推荐列表中移除被屏蔽的用户
  recommendedUsers.value = recommendedUsers.value.filter(u => u.id !== userId)
}

onMounted(() => {
  // 立即加载关注列表和动态
  loadFollowees()
  loadPosts()
  
  // 延迟加载推荐用户，给后端更多启动时间
  setTimeout(() => {
    loadRecommendedUsers()
  }, 500) // 延迟500ms，避免与动态加载请求同时发起
})
</script>

<style scoped>
.feed-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.main-content {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* 关注用户列表 */
.followees-section {
  padding: 18px 24px;
  border-radius: 24px;
  border: 1px solid #eceff5;
  background: #fff;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.05);
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
  flex: 1;
  min-height: 400px;
  background: #fff;
  border-radius: 28px;
  border: 1px solid #eceff5;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.04);
  padding: 24px;
}

/* 推荐用户侧边栏 */
.recommended-users-sidebar {
  width: 320px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 20px;
  border: 1px solid #eceff5;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
  padding: 20px;
  position: sticky;
  top: 88px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

.sidebar-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.loading-users,
.empty-users {
  padding: 20px 0;
}

.users-list {
  display: flex;
  flex-direction: column;
}

@media (max-width: 1200px) {
  .recommended-users-sidebar {
    display: none;
  }
  
  .main-content {
    flex-direction: column;
  }
}

.loading, .empty {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.masonry-grid {
  column-count: 4;
  column-gap: 20px;
}

.masonry-grid > * {
  break-inside: avoid;
  margin-bottom: 20px;
  display: block;
}

@media (max-width: 1200px) {
  .masonry-grid {
    column-count: 3;
  }
}

@media (max-width: 900px) {
  .masonry-grid {
    column-count: 2;
  }
}

@media (max-width: 600px) {
  .masonry-grid {
    column-count: 1;
  }
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
