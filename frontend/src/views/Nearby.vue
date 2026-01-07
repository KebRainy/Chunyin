<template>
  <div class="nearby-container">
    <div class="city-selector">
      <label>选择城市:</label>
      <el-select
        v-model="selectedCity"
        filterable
        clearable
        :loading="regionsLoading"
        placeholder="选择城市"
        @change="onCityChange"
      >
        <el-option-group
          v-for="province in provinces"
          :key="province.value"
          :label="province.label"
        >
          <el-option
            v-for="city in province.cities"
            :key="city.value"
            :label="city.label"
            :value="city.value"
          />
        </el-option-group>
      </el-select>
      <el-button text size="small" class="locate-btn" :loading="locating" @click="handleAutoLocate">
        {{ locating ? '定位中…' : '自动定位' }}
      </el-button>
    </div>

    <div class="content-wrapper">
      <!-- 左侧：动态列表 -->
      <div class="posts-section">
        <div v-if="loading && posts.length === 0" class="loading">加载中...</div>
        <div v-else-if="posts.length === 0" class="empty">
          <el-empty :description="`${selectedCity}还没有动态`" />
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

      <!-- 右侧：酒吧推荐 -->
      <div class="recommendation-section">
        <el-card class="recommendation-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">酒吧推荐</span>
            </div>
          </template>
          <div v-loading="barsLoading" class="bars-list">
            <div v-if="recommendedBars.length === 0 && !barsLoading" class="empty-bars">
              <el-empty description="暂无推荐酒吧" :image-size="80" />
            </div>
            <div
              v-for="bar in recommendedBars"
              :key="bar.id"
              class="bar-item"
              @click="goToBarDetail(bar.id)"
            >
              <div class="bar-info">
                <h3 class="bar-name">{{ bar.name }}</h3>
                <div class="bar-rating">
                  <el-rate
                    v-model="bar.avgRating"
                    disabled
                    show-score
                    :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
                  />
                  <span class="review-count">({{ bar.reviewCount || 0 }}条评价)</span>
                </div>
                <p class="bar-address">
                  <el-icon><LocationFilled /></el-icon>
                  {{ bar.address }}
                </p>
                <p v-if="bar.distance" class="bar-distance">
                  距离: {{ bar.distance.toFixed(2) }} 公里
                </p>
              </div>
            </div>
            <div v-if="recommendedBars.length > 0" class="show-more">
              <el-button type="primary" plain @click="goToBarList">显示更多</el-button>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { LocationFilled } from '@element-plus/icons-vue'
import { circleApi } from '@/api/circle'
import { getRecommendedBars } from '@/api/bar'
import PostCard from '@/components/PostCard.vue'
import { useChinaRegions } from '@/composables/useChinaRegions'
import { useCityDetection } from '@/composables/useCityDetection'

const router = useRouter()

const selectedCity = ref('')
const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const hasMore = ref(true)

const recommendedBars = ref([])
const barsLoading = ref(false)
const userLocation = ref(null)

const { provinces, loading: regionsLoading, ensureLoaded, findCityByName, getDefaultCity } = useChinaRegions()
const { detecting: locating, detectCity } = useCityDetection()

const loadPosts = async (retryCount = 0) => {
  if (!selectedCity.value) return
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
    // 如果是连接错误且重试次数小于3次，则延迟重试
    const isConnectionError = error.code === 'ECONNREFUSED' || 
                             error.message?.includes('ECONNREFUSED') ||
                             error.message?.includes('Network Error') ||
                             error.response === undefined
    
    if (isConnectionError && retryCount < 3) {
      // 延迟重试：第1次等待1秒，第2次等待2秒，第3次等待3秒
      await new Promise(resolve => setTimeout(resolve, (retryCount + 1) * 1000))
      return loadPosts(retryCount + 1)
    }
    
    // 连接错误或重试次数用完，静默处理，不显示错误
    if (!isConnectionError) {
      console.error('Failed to load nearby posts:', error)
    }
  } finally {
    loading.value = false
  }
}

const onCityChange = () => {
  if (!selectedCity.value) return
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

const initCitySelection = async () => {
  await ensureLoaded()
  const detected = await detectCity()
  const match = detected ? findCityByName(detected) : null
  selectedCity.value = match?.city || getDefaultCity() || '北京市'
  onCityChange()
}

const handleAutoLocate = async () => {
  await ensureLoaded()
  const detected = await detectCity()
  if (!detected) {
    ElMessage.warning('无法定位，请手动选择城市')
    return
  }
  const match = findCityByName(detected)
  if (match?.city) {
    selectedCity.value = match.city
    ElMessage.success(`已定位到 ${match.city}`)
    onCityChange()
  } else {
    ElMessage.warning('未找到匹配的城市，请手动选择')
  }
}

// 获取用户位置
const getCurrentLocation = () => {
  if (!navigator.geolocation) {
    return Promise.resolve(null)
  }
  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const location = {
          latitude: position.coords.latitude,
          longitude: position.coords.longitude
        }
        userLocation.value = location
        resolve(location)
      },
      () => {
        resolve(null)
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      }
    )
  })
}

// 加载推荐的酒吧
const loadRecommendedBars = async (retryCount = 0) => {
  if (!userLocation.value) {
    return
  }
  barsLoading.value = true
  try {
    const { data } = await getRecommendedBars({
      latitude: userLocation.value.latitude,
      longitude: userLocation.value.longitude,
      limit: 5
    })
    recommendedBars.value = data || []
  } catch (error) {
    // 如果是连接错误且重试次数小于3次，则延迟重试
    const isConnectionError = error.code === 'ECONNREFUSED' || 
                             error.message?.includes('ECONNREFUSED') ||
                             error.message?.includes('Network Error') ||
                             error.response === undefined
    
    if (isConnectionError && retryCount < 3) {
      // 延迟重试：第1次等待1秒，第2次等待2秒，第3次等待3秒
      await new Promise(resolve => setTimeout(resolve, (retryCount + 1) * 1000))
      return loadRecommendedBars(retryCount + 1)
    }
    
    // 连接错误或重试次数用完，静默处理，不显示错误
    if (!isConnectionError) {
      console.error('Failed to load recommended bars:', error)
    }
  } finally {
    barsLoading.value = false
  }
}

// 跳转到酒吧详情
const goToBarDetail = (barId) => {
  router.push(`/bars/${barId}`)
}

// 跳转到酒吧列表
const goToBarList = () => {
  router.push('/bars')
}

onMounted(async () => {
  // 先初始化城市选择，这会触发动态加载
  await initCitySelection()
  
  // 延迟加载推荐酒吧，给后端更多启动时间
  setTimeout(async () => {
    await getCurrentLocation()
    if (userLocation.value) {
      loadRecommendedBars()
    }
  }, 500) // 延迟500ms，避免与动态加载请求同时发起
})
</script>

<style scoped>
.nearby-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.content-wrapper {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.posts-section {
  flex: 1;
  min-width: 0;
}

.recommendation-section {
  width: 350px;
  flex-shrink: 0;
}

.city-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #333;
  border: 1px solid #eceff5;
  padding: 14px 20px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.04);
}

:deep(.el-select) {
  width: 220px;
}

.locate-btn {
  padding: 0 8px;
}

.loading, .empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  background: #fff;
  border-radius: 28px;
  border: 1px solid #eceff5;
}

.masonry-grid {
  column-count: 4;
  column-gap: 20px;
  background: #fff;
  border-radius: 28px;
  border: 1px solid #eceff5;
  padding: 24px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.04);
  margin-bottom: 20px;
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

.recommendation-card {
  position: sticky;
  top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.bars-list {
  min-height: 200px;
}

.empty-bars {
  padding: 40px 0;
}

.bar-item {
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid #eceff5;
  border-radius: 12px;
  cursor: pointer;
  will-change: transform;
  transition:
    transform var(--motion-normal, 220ms) var(--motion-ease, ease),
    box-shadow var(--motion-normal, 220ms) var(--motion-ease, ease),
    border-color var(--motion-normal, 220ms) var(--motion-ease, ease),
    background-color var(--motion-normal, 220ms) var(--motion-ease, ease);
}

.bar-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.bar-info {
  width: 100%;
}

.bar-name {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bar-rating {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  gap: 8px;
}

.review-count {
  color: #999;
  font-size: 12px;
}

.bar-address {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 13px;
  margin: 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bar-distance {
  color: #409eff;
  font-size: 13px;
  margin: 4px 0 0 0;
}

.show-more {
  text-align: center;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px solid #eceff5;
}

@media (max-width: 1200px) {
  .content-wrapper {
    flex-direction: column;
  }
  
  .recommendation-section {
    width: 100%;
  }
  
  .recommendation-card {
    position: static;
  }
}
</style>
