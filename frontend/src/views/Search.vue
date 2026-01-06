<template>
  <div class="search-page">
    <section class="search-hero">
      <div>
        <p class="eyebrow">Search</p>
        <h1 class="art-heading">探索醇饮社区</h1>
      </div>
      <el-input
        v-model="keyword"
        placeholder="输入关键词，例如 IPA、陈年葡萄酒、用户名"
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button type="primary" :loading="loading" @click="handleSearch">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <div class="search-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          :class="['tab-chip', { active: type === tab.value }]"
          @click="switchType(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>
    </section>

    <section class="result-panel">
      <div v-if="loading" class="loading">
        <el-skeleton :rows="5" animated />
      </div>
      <div v-else>
        <header class="result-header">
          <div>
            <p class="eyebrow">{{ activeTab.label }}</p>
            <h3>{{ activeTab.description }}</h3>
          </div>
          <span class="result-count">{{ activeList.length }} 条结果</span>
        </header>
        <div v-if="activeList.length === 0" class="empty">
          <el-empty description="暂无相关内容" />
        </div>
        <div v-else class="result-list">
          <template v-if="type === 'post'">
            <div class="result-card post" v-for="post in activeList" :key="post.id">
              <div class="card-meta">
                <strong>{{ post.author?.username || '匿名用户' }}</strong>
                <span>{{ formatTime(post.createdAt) }}</span>
              </div>
              <p class="card-content">{{ post.content }}</p>
              <div class="card-actions">
                <el-tag size="small" v-if="post.ipRegion">{{ post.ipRegion }}</el-tag>
                <el-button text type="primary" @click="goPost(post.id)">查看动态</el-button>
              </div>
            </div>
          </template>
          <template v-else-if="type === 'wiki'">
            <div class="result-card wiki" v-for="wiki in activeList" :key="wiki.id">
              <div class="card-meta">
                <h4>{{ wiki.title }}</h4>
                <span>{{ formatTime(wiki.updatedAt) }}</span>
              </div>
              <p class="card-content">{{ wiki.summary || '这条词条暂未添加摘要' }}</p>
              <div class="card-actions">
                <span>{{ wiki.lastEditorName || '系统' }}</span>
                <el-button text type="primary" @click="goWiki(wiki.slug || wiki.id)">查看词条</el-button>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="result-card user" v-for="user in activeList" :key="user.id" @click="goUser(user.id)">
              <el-avatar :src="user.avatarUrl" :alt="`${user.username}的头像`">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div>
                <strong>{{ user.username }}</strong>
                <p>{{ user.bio || '这个人还没有简介' }}</p>
              </div>
              <el-button text type="primary">查看</el-button>
            </div>
          </template>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch } from 'vue'
import dayjs from 'dayjs'
import { useRoute, useRouter } from 'vue-router'
import { searchAll } from '@/api/search'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const keyword = ref(route.query.q || '')
const type = ref('post')
const loading = ref(false)
const results = reactive({
  posts: [],
  wikis: [],
  users: []
})

const tabs = [
  { label: '动态', value: 'post', description: '相关动态' },
  { label: '维基', value: 'wiki', description: '百科词条' },
  { label: '用户', value: 'user', description: '创作者与饮者' }
]

const activeTab = computed(() => tabs.find(tab => tab.value === type.value) || tabs[0])
const activeKeyMap = {
  post: 'posts',
  wiki: 'wikis',
  user: 'users'
}

const activeList = computed(() => results[activeKeyMap[type.value]] || [])

const handleSearch = async () => {
  if (!keyword.value) return
  loading.value = true
  router.replace({ path: '/search', query: { q: keyword.value } })
  const targetKey = activeKeyMap[type.value]
  results[targetKey] = []
  try {
    const res = await searchAll({ keyword: keyword.value, type: type.value })
    const payload = res.data || {}
    if (Array.isArray(payload.items)) {
      results[targetKey] = payload.items
    } else if (Array.isArray(payload[targetKey])) {
      results[targetKey] = payload[targetKey]
    } else {
      results[targetKey] = []
    }
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const switchType = (value) => {
  if (type.value === value) return
  type.value = value
  if (keyword.value) {
    handleSearch()
  }
}

const goPost = (id) => {
  if (!id) return
  window.open(`/posts/${id}`, '_blank')
}

const goWiki = (slug) => {
  if (!slug) return
  window.open(`/wiki/${slug}`, '_blank')
}

const goUser = (id) => {
  if (!id) return
  window.open(`/users/${id}`, '_blank')
}

const formatTime = (time) => {
  if (!time) return ''
  return dayjs(time).format('YYYY.MM.DD HH:mm')
}

watch(
  () => route.query.q,
  (newVal) => {
    keyword.value = newVal || ''
    if (keyword.value) {
      handleSearch()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.search-page {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 2px;
  font-size: 12px;
  color: #909399;
  margin: 0 0 6px;
}

.search-hero {
  border-radius: 28px;
  border: 1px solid #eceff5;
  padding: 24px 28px;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.05);
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-tabs {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.tab-chip {
  border: 1px solid #dcdfe6;
  border-radius: 999px;
  padding: 8px 18px;
  background: transparent;
  cursor: pointer;
  color: #606266;
}

.tab-chip.active {
  border-color: #2f54eb;
  color: #2f54eb;
  background: rgba(47, 84, 235, 0.08);
}

.result-panel {
  border-radius: 32px;
  border: 1px solid #eceff5;
  background: #fff;
  padding: 24px 28px;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.05);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 16px;
}

.result-count {
  color: #909399;
  font-size: 13px;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-card {
  border: 1px solid #f0f2f5;
  border-radius: 20px;
  padding: 16px 20px;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-card.user {
  flex-direction: row;
  align-items: center;
  gap: 16px;
  cursor: pointer;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #909399;
  font-size: 13px;
}

.card-meta h4 {
  margin: 0;
}

.card-content {
  margin: 0;
  color: #1f2d3d;
  line-height: 1.6;
}

.card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.loading,
.empty {
  padding: 40px 0;
}

@media (max-width: 640px) {
  .search-hero,
  .result-panel {
    padding: 20px;
  }

  .result-card.user {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>