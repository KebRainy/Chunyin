<template>
  <div class="ranking-container">
    <div class="ranking-header">
      <h1 class="title">热门</h1>
      <p class="subtitle">发现平台最受欢迎的精彩内容</p>
    </div>

    <!-- 时间维度选择 -->
    <div class="time-tabs">
      <div
        v-for="tab in timeTabs"
        :key="tab.value"
        class="time-tab"
        :class="{ active: currentTimeDimension === tab.value }"
        @click="changeTimeDimension(tab.value)"
      >
        <el-icon class="tab-icon"><component :is="tab.icon" /></el-icon>
        <span class="tab-label">{{ tab.label }}</span>
      </div>
    </div>

    <!-- 动态列表 -->
    <div class="ranking-content">
      <div v-if="loading && posts.length === 0" class="loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      
      <div v-else-if="posts.length === 0" class="empty">
        <el-empty description="暂无热门动态" />
      </div>
      
      <div v-else class="posts-grid">
        <div
          v-for="(post, index) in posts"
          :key="post.id"
          class="post-item"
        >
          <div class="rank-badge" :class="getRankClass(index)">
            <!-- <template v-if="index < 3">
              <el-icon class="rank-icon"><component :is="getRankIcon(index)" /></el-icon>
            </template>
            <span v-else class="rank-number">{{ index + 1 }}</span> -->
            <span class="rank-number">{{ index + 1 }}</span>
          </div>
          <PostCard :post="post" @select="goToPost(post.id)" />
        </div>
      </div>

      <!-- 加载更多 -->
      <div v-if="!loading && hasMore" class="load-more">
        <el-button @click="loadMore" :loading="loading">加载更多</el-button>
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
import { Loading, Calendar, Timer, TrendCharts, Trophy, GoldMedal, Medal } from '@element-plus/icons-vue'
import { circleApi } from '@/api/circle'
import PostCard from '@/components/PostCard.vue'

const router = useRouter()

const timeTabs = [
  { value: 'DAY', label: '今日', icon: Calendar },
  { value: 'WEEK', label: '本周', icon: Timer },
  { value: 'MONTH', label: '本月', icon: TrendCharts },
  { value: 'ALL', label: '全部', icon: Trophy }
]

const currentTimeDimension = ref('WEEK')
const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const hasMore = ref(true)

const loadPosts = async (reset = false) => {
  if (loading.value) return
  
  loading.value = true
  try {
    if (reset) {
      currentPage.value = 1
      posts.value = []
    }
    
    const response = await circleApi.getHotPosts(
      currentTimeDimension.value,
      currentPage.value,
      pageSize.value
    )
    
    const pageData = response.data
    const items = pageData.items || []
    
    if (reset) {
      posts.value = items
    } else {
      posts.value.push(...items)
    }
    
    hasMore.value = items.length >= pageSize.value
  } catch (error) {
    console.error('Failed to load hot posts:', error)
  } finally {
    loading.value = false
  }
}

const changeTimeDimension = (dimension) => {
  if (currentTimeDimension.value === dimension) return
  currentTimeDimension.value = dimension
  loadPosts(true)
}

const loadMore = () => {
  currentPage.value++
  loadPosts(false)
}

const goToPost = (postId) => {
  router.push(`/posts/${postId}`)
}

const getRankClass = (index) => {
  if (index === 0) return 'rank-1'
  if (index === 1) return 'rank-2'
  if (index === 2) return 'rank-3'
  return 'rank-other'
}

const getRankIcon = (index) => {
  if (index === 0) return GoldMedal
  if (index === 1) return Medal
  return Trophy
}

onMounted(() => {
  loadPosts(true)
})
</script>

<style scoped>
.ranking-container {
  max-width: 1300px;
  margin: 0 auto;
  padding: 20px;
}

.ranking-header {
  text-align: center;
  margin-bottom: 30px;
}

.title {
  font-size: 32px;
  font-weight: 700;
  color: var(--app-text, #1a1a1a);
  margin: 0 0 10px 0;
}

.subtitle {
  font-size: 16px;
  color: var(--app-muted, #666);
  margin: 0;
}

/* 时间维度选择 */
.time-tabs {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 30px;
  padding: 20px;
  background: var(--app-surface, #fff);
  border-radius: 16px;
  border: 1px solid var(--app-border, #e6e8ef);
}

.time-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 12px;
  background: var(--app-surface-2, #f5f7fa);
  border: 1px solid transparent;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
  font-weight: 500;
  color: var(--app-muted, #666);
}

.time-tab:hover {
  background: #f2f4fa;
  border-color: rgba(64, 158, 255, 0.25);
  color: var(--app-text, #1a1a1a);
  transform: translateY(-1px);
}

.time-tab.active {
  background: var(--app-primary-soft, rgba(64, 158, 255, 0.12));
  border-color: rgba(64, 158, 255, 0.45);
  color: var(--app-primary, #409eff);
}

.tab-icon {
  font-size: 18px;
}

.tab-label {
  font-size: 15px;
}

/* 内容区域 */
.ranking-content {
  min-height: 400px;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;
  color: #999;
}

.loading .el-icon {
  font-size: 32px;
}

.empty {
  padding: 60px 20px;
}

/* 动态网格 */
.posts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
}

.post-item {
  position: relative;
}

.rank-badge {
  position: absolute;
  top: -10px;
  left: -10px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  z-index: 10;
  border: 1px solid rgba(15, 23, 42, 0.06);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.1);
}

.rank-badge.rank-1 {
  background: #f5c542;
  color: #5a3a00;
}

.rank-badge.rank-2 {
  background: #cbd5e1;
  color: #334155;
}

.rank-badge.rank-3 {
  background: #d99058;
  color: #4a2a16;
}

.rank-badge.rank-other {
  background: rgba(64, 158, 255, 0.14);
  color: var(--app-primary, #409eff);
}

.rank-icon {
  font-size: 20px;
}

.rank-number {
  font-size: 16px;
}

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 20px;
}

.load-more .el-button {
  padding: 12px 40px;
  border-radius: 24px;
  font-weight: 500;
}

.no-more {
  text-align: center;
  padding: 30px;
  color: #999;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ranking-container {
    padding: 15px;
  }

  .title {
    font-size: 24px;
  }

  .subtitle {
    font-size: 14px;
  }

  .time-tabs {
    gap: 8px;
    padding: 15px;
  }

  .time-tab {
    padding: 10px 16px;
    font-size: 14px;
  }

  .tab-icon {
    font-size: 18px;
  }

  .posts-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .rank-badge {
    width: 35px;
    height: 35px;
    font-size: 12px;
  }

  .rank-icon {
    font-size: 18px;
  }
}
</style>

