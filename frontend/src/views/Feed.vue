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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { circleApi } from '@/api/circle'
import { getFollowees } from '@/api/user'
import PostCard from '@/components/PostCard.vue'

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
  router.push(`/users/${userId}`)
}

onMounted(() => {
  loadFollowees()
  loadPosts()
})
</script>

<style scoped>
.feed-container {
  max-width: 1300px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 24px;
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
  min-height: 400px;
  background: #fff;
  border-radius: 28px;
  border: 1px solid #eceff5;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.04);
  padding: 24px;
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
