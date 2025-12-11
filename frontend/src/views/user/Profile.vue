<template>
  <div v-if="userStore.userInfo" class="space-page">
    <section class="space-hero">
      <div class="hero-left">
        <el-avatar :size="96" :src="userStore.userInfo.avatarUrl" @click="goSettings" />
        <div>
          <div class="user-line">
            <h1>{{ userStore.userInfo.username }}</h1>
            <el-tag size="small">Lv.{{ userStore.userInfo.level || 1 }}</el-tag>
          </div>
          <p class="uid">UID {{ userStore.userInfo.id }}</p>
          <div class="signature">
            <template v-if="editingSignature">
              <el-input
                v-model="signature"
                size="small"
                maxlength="200"
                show-word-limit
                @keyup.enter="saveSignature"
              />
              <div class="signature-actions">
                <el-button size="small" type="primary" @click="saveSignature">保存</el-button>
                <el-button size="small" text @click="cancelSignature">取消</el-button>
              </div>
            </template>
            <template v-else>
              <span>{{ signature || '这个人很酷，什么都没有写' }}</span>
              <el-button text size="small" @click="editingSignature = true">编辑</el-button>
            </template>
          </div>
        </div>
      </div>
      <div class="hero-right">
        <div class="stat">
          <p>{{ userStore.userInfo.followingCount }}</p>
          <span>关注</span>
        </div>
        <div class="stat">
          <p>{{ userStore.userInfo.followerCount }}</p>
          <span>粉丝</span>
        </div>
        <div class="stat">
          <p>{{ userStore.userInfo.likeReceived }}</p>
          <span>获赞</span>
        </div>
      </div>
    </section>

    <nav class="space-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab-item', { active: activeTab === tab.key }]"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </div>
    </nav>

    <section class="tab-content">
      <div v-if="activeTab === 'posts'">
        <div class="section-header">
          <h3>我的动态</h3>
          <el-button text size="small" @click="refreshPosts">刷新</el-button>
        </div>
        <div v-if="posts.loading" class="loading">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="posts.items.length === 0" class="empty">
          <el-empty description="还没有发布任何动态" />
        </div>
        <div v-else class="post-list">
          <div v-for="post in posts.items" :key="post.id" class="post-row">
            <div class="post-content">
              <p class="post-text">{{ post.content }}</p>
              <div class="post-meta">
                <span>{{ formatTime(post.createdAt) }}</span>
                <span>浏览 {{ post.viewCount || 0 }}</span>
                <span>点赞 {{ post.likeCount || 0 }}</span>
              </div>
            </div>
            <el-button text size="small" @click="goPost(post.id)">查看</el-button>
          </div>
        </div>
      </div>

      <div v-else-if="activeTab === 'collections'">
        <div class="filter-bar">
          <el-select v-model="collectionFilter.type" size="small">
            <el-option label="全部内容" value="ALL" />
            <el-option label="动态" value="POST" />
            <el-option label="维基" value="WIKI" />
          </el-select>
          <el-input
            v-model="collectionFilter.keyword"
            placeholder="搜索收藏内容"
            size="small"
            clearable
          />
          <el-button size="small" @click="loadCollections">筛选</el-button>
        </div>
        <div v-if="collections.loading" class="loading">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="collections.items.length === 0" class="empty">
          <el-empty description="暂无收藏" />
        </div>
        <div v-else class="card-grid">
          <div v-for="item in filteredCollections" :key="item.id" class="card">
            <h4>{{ item.title }}</h4>
            <p>{{ item.summary }}</p>
            <div class="card-meta">
              <span>{{ item.targetType === 'POST' ? '动态' : '维基' }}</span>
              <span>{{ formatTime(item.createdAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="activeTab === 'history'">
        <div class="filter-bar">
          <el-select v-model="footprintFilter.days" size="small">
            <el-option label="3 天以内" :value="3" />
            <el-option label="7 天以内" :value="7" />
          </el-select>
          <el-select v-model="footprintFilter.type" size="small">
            <el-option label="全部" value="ALL" />
            <el-option label="动态" value="POST" />
            <el-option label="维基" value="WIKI" />
          </el-select>
          <el-button size="small" @click="loadFootprints">应用</el-button>
        </div>
        <div v-if="footprints.loading" class="loading">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="footprints.items.length === 0" class="empty">
          <el-empty description="最近没有足迹记录" />
        </div>
        <div v-else class="timeline">
          <div v-for="item in footprints.items" :key="item.id" class="timeline-item">
            <div class="dot"></div>
            <div>
              <h4>{{ item.title }}</h4>
              <p>{{ item.summary }}</p>
              <small>{{ formatTime(item.visitedAt) }}</small>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="activeTab === 'messages'" class="message-entry">
        <el-empty description="私信已独立为单独页面" />
        <el-button type="primary" @click="goMessages">进入消息中心</el-button>
      </div>
    </section>
  </div>
  <el-empty v-else description="正在加载..." />
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { useUserStore } from '@/store/modules/user'
import { circleApi } from '@/api/circle'
import { getCollections, getFootprints, updateProfile } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const tabs = [
  { key: 'posts', label: '动态' },
  { key: 'collections', label: '收藏' },
  { key: 'history', label: '足迹' },
  { key: 'messages', label: '消息' }
]

const activeTab = ref('posts')
const signature = ref('')
const editingSignature = ref(false)

const posts = reactive({
  loading: false,
  items: []
})

const collections = reactive({
  loading: false,
  items: []
})

const footprints = reactive({
  loading: false,
  items: []
})

const collectionFilter = reactive({
  type: 'ALL',
  keyword: ''
})

const footprintFilter = reactive({
  days: 3,
  type: 'ALL'
})

const filteredCollections = computed(() => {
  if (!collectionFilter.keyword) return collections.items
  return collections.items.filter(item =>
    item.title?.includes(collectionFilter.keyword) ||
    item.summary?.includes(collectionFilter.keyword)
  )
})

const formatTime = (time) => dayjs(time).format('YYYY.MM.DD HH:mm')

const loadPosts = async () => {
  posts.loading = true
  try {
    const res = await circleApi.listUserPosts(userStore.userInfo.id, 1, 10)
    posts.items = res.data.items || []
  } catch (error) {
    ElMessage.error('加载动态失败')
  } finally {
    posts.loading = false
  }
}

const refreshPosts = () => {
  loadPosts()
}

const loadCollections = async () => {
  collections.loading = true
  try {
    const params = {
      page: 1,
      pageSize: 20
    }
    if (collectionFilter.type !== 'ALL') {
      params.type = collectionFilter.type
    }
    const res = await getCollections(params)
    collections.items = res.data.items || []
  } catch (error) {
    ElMessage.error('加载收藏失败')
  } finally {
    collections.loading = false
  }
}

const loadFootprints = async () => {
  footprints.loading = true
  try {
    const params = {
      days: footprintFilter.days
    }
    if (footprintFilter.type !== 'ALL') {
      params.type = footprintFilter.type
    }
    const res = await getFootprints(params)
    footprints.items = res.data || []
  } catch (error) {
    ElMessage.error('加载足迹失败')
  } finally {
    footprints.loading = false
  }
}

const switchTab = (key) => {
  if (key === 'messages') {
    goMessages()
    return
  }
  activeTab.value = key
  if (key === 'posts') loadPosts()
  if (key === 'collections') loadCollections()
  if (key === 'history') loadFootprints()
}

const goMessages = () => {
  router.push('/messages')
}

const goPost = (id) => {
  router.push(`/posts/${id}`)
}

const goSettings = () => {
  router.push('/user/settings')
}

const saveSignature = async () => {
  try {
    await updateProfile({ bio: signature.value })
    await userStore.fetchUserInfo()
    editingSignature.value = false
    ElMessage.success('签名已更新')
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const cancelSignature = () => {
  signature.value = userStore.userInfo.bio
  editingSignature.value = false
}

onMounted(async () => {
  await userStore.fetchUserInfo().catch(() => {})
  signature.value = userStore.userInfo?.bio
  loadPosts()
})

watch(
  () => route.query.tab,
  (tab) => {
    if (tab && tabs.some(item => item.key === tab)) {
      activeTab.value = tab
      if (tab === 'posts') loadPosts()
      if (tab === 'collections') loadCollections()
      if (tab === 'history') loadFootprints()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.space-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.space-hero {
  display: flex;
  justify-content: space-between;
  background: linear-gradient(135deg, #fef6fb, #f3f7ff);
  border-radius: 24px;
  padding: 24px;
}

.hero-left {
  display: flex;
  gap: 16px;
}

.user-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.uid {
  color: #909399;
  margin: 4px 0;
}

.signature {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
}

.hero-right {
  display: flex;
  gap: 32px;
  align-items: center;
}

.stat {
  text-align: center;
}

.stat p {
  font-size: 24px;
  font-weight: bold;
  margin: 0;
}

.stat span {
  color: #909399;
  font-size: 13px;
}

.space-tabs {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.tab-item {
  padding-bottom: 12px;
  color: #909399;
  cursor: pointer;
}

.tab-item.active {
  color: #303133;
  border-bottom: 2px solid #303133;
}

.tab-content {
  background: #fff;
  border-radius: 18px;
  padding: 20px;
  box-shadow: 0 12px 30px rgba(31, 45, 61, 0.06);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-row {
  display: flex;
  justify-content: space-between;
  border: 1px solid #f5f5f5;
  border-radius: 12px;
  padding: 12px;
}

.post-text {
  margin: 0 0 8px;
  color: #303133;
}

.post-meta {
  display: flex;
  gap: 12px;
  color: #909399;
  font-size: 12px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.card {
  border: 1px solid #f2f2f2;
  border-radius: 12px;
  padding: 16px;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-top: 12px;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.timeline-item {
  display: flex;
  gap: 12px;
  position: relative;
  padding-left: 16px;
}

.timeline-item .dot {
  width: 8px;
  height: 8px;
  background: #409eff;
  border-radius: 50%;
  position: absolute;
  left: 0;
  top: 6px;
}

.loading {
  padding: 20px 0;
}

.empty {
  padding: 40px 0;
}

@media (max-width: 768px) {
  .space-hero {
    flex-direction: column;
    gap: 16px;
  }

  .hero-right {
    justify-content: space-around;
  }
}
</style>
