<template>
  <div v-if="userStore.userInfo" class="profile-page">
    <section class="profile-hero">
      <div class="cover-panel">
        <div class="hero-layout">
          <div class="avatar-column">
            <div class="avatar-wrapper" @click="goSettings">
              <el-avatar
                :size="104"
                :src="userStore.userInfo.avatarUrl"
                :alt="`${userStore.userInfo.username || '用户'}的头像`"
              />
              <div class="avatar-overlay">编辑信息</div>
            </div>
          </div>
          <div class="meta-column">
            <div class="user-title">
              <h1>{{ userStore.userInfo.username }}</h1>
              <el-tag size="small" effect="dark">Lv.{{ userStore.userInfo.level || 1 }}</el-tag>
            </div>
            <p class="uid">UID {{ userStore.userInfo.id }}</p>
            <div
              class="signature"
              :class="{ editing: editingSignature }"
              @click="!editingSignature && startEditSignature()"
              :style="{ backgroundColor: 'inherit' }"
            >
              <template v-if="editingSignature">
                <el-input
                  ref="signatureInputRef"
                  v-model="signature"
                  size="small"
                  maxlength="200"
                  show-word-limit
                  @keyup.enter="saveSignature"
                  @keydown.esc.prevent="cancelSignature"
                  @blur="saveSignature"
                />
                <!-- <div class="signature-actions">
                  <el-button size="small" type="primary" @click="saveSignature">保存</el-button>
                  <el-button size="small" text @click="cancelSignature">取消</el-button>
                </div> -->
              </template>
              <template v-else>
                <span class="signature-text">{{ signature || '这个人很酷，什么都没有写' }}</span>
              </template>
            </div>
          </div>
          <div class="stats-column">
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
        </div>
      </div>
    </section>

    <nav class="profile-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab', { active: activeTab === tab.key }]"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
      <div class="tab-indicator" :style="indicatorStyle"></div>
    </nav>

    <section class="profile-content">
      <div v-if="activeTab === 'posts'" class="content-card">
        <div class="section-header">
          <div>
            <p class="eyebrow">Recent</p>
            <h3>我的动态</h3>
          </div>
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
            <div class="post-thumbnail">
              <img
                v-if="getPostCover(post)"
                :src="getPostCover(post)"
                :alt="`${post.author?.username || '用户'}的动态配图`"
              >
              <span v-else>文字</span>
            </div>
            <div class="post-content">
              <p class="post-text">{{ truncatePostText(post.content) }}</p>
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

      <div v-else-if="activeTab === 'collections'" class="content-card">
        <div class="section-header">
          <div>
            <p class="eyebrow">Saved</p>
            <h3>收藏夹</h3>
          </div>
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
        </div>
        <div v-if="collections.loading" class="loading">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="collections.items.length === 0" class="empty">
          <el-empty description="暂无收藏" />
        </div>
        <div v-else class="card-grid">
          <div
            v-for="item in filteredCollections"
            :key="item.id"
            class="card"
            @click="openCollection(item)"
          >
            <h4>{{ item.title }}</h4>
            <p>{{ item.summary }}</p>
            <div class="card-meta">
              <span>{{ item.targetType === 'POST' ? '动态' : '维基' }}</span>
              <span>{{ formatTime(item.createdAt) }}</span>
            </div>
            <el-button type="primary" link size="small" @click.stop="openCollection(item)">
              查看详情
            </el-button>
          </div>
        </div>
      </div>

      <div v-else-if="activeTab === 'history'" class="content-card">
        <div class="section-header">
          <div>
            <p class="eyebrow">Footprint</p>
            <h3>我的足迹</h3>
          </div>
          <div class="filter-bar">
            <el-select v-model="footprintFilter.days" size="small" @change="loadFootprints">
              <el-option label="3 天以内" :value="3" />
              <el-option label="7 天以内" :value="7" />
              <el-option label="30 天以内" :value="30" />
            </el-select>
            <el-select v-model="footprintFilter.type" size="small" @change="loadFootprints">
              <el-option label="全部" value="ALL" />
              <el-option label="动态" value="POST" />
              <el-option label="维基" value="WIKI" />
            </el-select>
          </div>
        </div>
        <div v-if="footprints.loading" class="loading">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="footprints.items.length === 0" class="empty">
          <el-empty description="最近没有足迹记录" />
        </div>
        <div v-else class="footprint-list">
          <div 
            v-for="(group, date) in groupedFootprints" 
            :key="date" 
            class="footprint-group"
          >
            <div class="group-date">{{ date }}</div>
            <div class="group-items">
              <div 
                v-for="item in group" 
                :key="item.id" 
                class="footprint-item"
                @click="openFootprint(item)"
              >
                <div class="footprint-cover">
                  <img 
                    v-if="item.coverUrl" 
                    :src="item.coverUrl" 
                    :alt="item.title"
                  />
                  <div v-else class="cover-placeholder">
                    <el-icon v-if="item.targetType === 'POST'"><Document /></el-icon>
                    <el-icon v-else><Notebook /></el-icon>
                  </div>
                </div>
                <div class="footprint-info">
                  <div class="footprint-title">
                    <el-tag 
                      :type="item.targetType === 'POST' ? 'primary' : 'success'" 
                      size="small"
                      effect="plain"
                    >
                      {{ item.targetType === 'POST' ? '动态' : '维基' }}
                    </el-tag>
                    <span class="title-text">{{ item.title || '无标题' }}</span>
                  </div>
                  <p class="footprint-summary">{{ item.summary || '暂无描述' }}</p>
                  <div class="footprint-meta">
                    <span class="visit-time">
                      <el-icon><Clock /></el-icon>
                      {{ formatDetailTime(item.visitedAt) }}
                    </span>
                  </div>
                </div>
                <el-icon class="arrow-icon"><ArrowRight /></el-icon>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="activeTab === 'messages'" class="content-card message-entry">
        <el-empty description="私信已独立为单独页面" />
        <el-button type="primary" @click="goMessages">进入消息中心</el-button>
      </div>
    </section>
  </div>
  <el-empty v-else description="正在加载..." />
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { useUserStore } from '@/store/modules/user'
import { circleApi } from '@/api/circle'
import { getCollections, getFootprints, updateProfile } from '@/api/user'
import { ElMessage } from 'element-plus'
import { Document, Notebook, Clock, ArrowRight } from '@element-plus/icons-vue'

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
const signatureInputRef = ref(null)
const editingSignature = ref(false)

const startEditSignature = () => {
  editingSignature.value = true
  nextTick(() => {
    signatureInputRef.value?.focus()
  })
}

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

// 按日期分组足迹
const groupedFootprints = computed(() => {
  const groups = {}
  for (const item of footprints.items) {
    const date = dayjs(item.visitedAt).format('YYYY年MM月DD日')
    if (!groups[date]) {
      groups[date] = []
    }
    groups[date].push(item)
  }
  return groups
})

const indicatorStyle = computed(() => {
  const activeIndex = Math.max(0, tabs.findIndex(item => item.key === activeTab.value))
  const width = 100 / tabs.length
  return {
    width: `${width}%`,
    transform: `translateX(${activeIndex * 100}%)`
  }
})

const truncatePostText = (text) => {
  if (!text) return ''
  return text.length > 29 ? `${text.slice(0, 29)}...` : text
}

const getPostCover = (post) => {
  if (!post || !post.imageUrls || post.imageUrls.length === 0) return null
  return post.imageUrls[0]
}

const formatTime = (time) => dayjs(time).format('YYYY.MM.DD HH:mm')

const formatDetailTime = (time) => {
  const date = dayjs(time)
  const now = dayjs()
  if (date.isSame(now, 'day')) {
    return `今天 ${date.format('HH:mm')}`
  }
  if (date.isSame(now.subtract(1, 'day'), 'day')) {
    return `昨天 ${date.format('HH:mm')}`
  }
  return date.format('MM-DD HH:mm')
}

const openFootprint = (item) => {
  if (item.targetType === 'POST') {
    router.push(`/posts/${item.targetId}`)
  } else if (item.targetType === 'WIKI') {
    if (item.slug) {
      router.push(`/wiki/${item.slug}`)
    } else {
      router.push(`/wiki/${item.targetId}`)
    }
  }
}

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

const openCollection = (item) => {
  if (!item) return
  if (item.targetType === 'POST') {
    router.push(`/posts/${item.targetId}`)
    return
  }
  if (item.targetType === 'WIKI') {
    if (item.slug) {
      router.push(`/wiki/${item.slug}`)
    } else {
      router.push(`/wiki/${item.targetId}`)
    }
  }
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

<style>
.profile-page {
  max-width: 1100px;
  margin: 0 auto;
  padding-bottom: 60px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-hero {
  position: relative;
}

.cover-panel {
  border-radius: 32px;
  border: 1px solid #eceff5;
  background: #f5f7fb;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
}

.hero-layout {
  display: grid;
  grid-template-columns: 220px 1fr 260px;
  gap: 32px;
  padding: 32px;
  align-items: center;
}

.avatar-column {
  display: flex;
  justify-content: center;
}

.avatar-wrapper {
  position: relative;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.2s;
}

.avatar-wrapper:hover {
  transform: scale(1.02);
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 50%;
  color: #fff;
  font-size: 14px;
  letter-spacing: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.meta-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-title h1 {
  margin: 0;
}

.uid {
  color: #909399;
  margin: 0;
  font-size: 14px;
}

.signature {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #606266;
  padding: 14px 18px;
  border-radius: 18px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.signature:hover {
  border-color: #dcdfe6;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.signature.editing {
  cursor: default;
  /* border-color: #2f54eb; */
  /* box-shadow: 0 12px 30px rgba(47, 84, 235, 0.2); */
  background: #fff;
}

.signature-text {
  font-size: 14px;
  line-height: 1.6;
  color: #1f2d3d;
}

.signature-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.stats-column {
  display: flex;
  gap: 32px;
  justify-content: flex-end;
}

.stat {
  text-align: center;
}

.stat p {
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.stat span {
  color: #909399;
  font-size: 12px;
}

.profile-tabs {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border: 1px solid #eceff5;
  border-radius: 999px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}

.profile-tabs .tab {
  padding: 12px 0;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  color: #555;
  transition: color 0.2s;
  position: relative;
  z-index: 1;
}

.profile-tabs .tab.active {
  color: #111;
  font-weight: 600;
}

.tab-indicator {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 100%;
  background: rgba(47, 84, 235, 0.1);
  border-radius: 999px;
  transition: transform 0.3s, width 0.3s;
  pointer-events: none;
  z-index: 0;
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.content-card {
  background: #fff;
  border-radius: 26px;
  border: 1px solid #eceff5;
  padding: 24px;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.eyebrow {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 2px;
  font-size: 12px;
  color: #909399;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-radius: 18px;
  border: 1px solid #f0f0f0;
}

.post-thumbnail {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 12px;
  color: #a0a3ad;
}

.post-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.post-text {
  margin: 0 0 8px;
  color: #1f2d3d;
  font-weight: 500;
}

.post-content {
  flex: 1;
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
  flex-wrap: wrap;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.card {
  border: 1px solid #f2f2f2;
  border-radius: 18px;
  padding: 16px;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

/* 足迹列表样式 */
.footprint-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.footprint-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.group-date {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  padding-left: 4px;
  border-left: 3px solid #2f54eb;
  padding-left: 12px;
}

.group-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.footprint-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 16px;
  background: #fafbfc;
  border: 1px solid #f0f2f5;
  cursor: pointer;
  transition: all 0.2s ease;
}

.footprint-item:hover {
  background: #f5f7fa;
  border-color: #e4e7ed;
  transform: translateX(4px);
}

.footprint-cover {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  background: #e8eaed;
}

.footprint-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 24px;
}

.footprint-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.footprint-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  font-weight: 500;
  color: #1f2d3d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.footprint-summary {
  margin: 0;
  font-size: 13px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.footprint-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: #a0a3ad;
}

.visit-time {
  display: flex;
  align-items: center;
  gap: 4px;
}

.arrow-icon {
  color: #c0c4cc;
  font-size: 16px;
  flex-shrink: 0;
}

/* 旧的时间线样式（保留兼容） */
.timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.timeline-item {
  display: flex;
  gap: 12px;
  position: relative;
  padding-left: 20px;
}

.timeline-item .dot {
  width: 8px;
  height: 8px;
  background: #2f54eb;
  border-radius: 50%;
  position: absolute;
  left: 0;
  top: 8px;
}

.loading {
  padding: 20px 0;
}

.empty {
  padding: 20px 0;
}

.message-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

@media (max-width: 900px) {
  .hero-layout {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .stats-column {
    justify-content: center;
  }

  .profile-tabs {
    border-radius: 16px;
  }
}
</style>
