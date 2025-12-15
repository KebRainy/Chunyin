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
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { circleApi } from '@/api/circle'
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

const { provinces, loading: regionsLoading, ensureLoaded, findCityByName, getDefaultCity } = useChinaRegions()
const { detecting: locating, detectCity } = useCityDetection()

const loadPosts = async () => {
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
    console.error('Failed to load nearby posts:', error)
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

onMounted(() => {
  initCitySelection()
})
</script>

<style scoped>
.nearby-container {
  max-width: 1300px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
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
</style>
