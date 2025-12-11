<template>
  <div class="search-page">
    <el-card class="search-box">
      <el-input
        v-model="keyword"
        placeholder="搜索圈子动态、酒款或用户"
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button type="primary" :loading="loading" @click="handleSearch">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <el-radio-group v-model="type" class="type-tabs" size="small" @change="handleSearch">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="circle">圈子</el-radio-button>
        <el-radio-button label="beverage">酒款</el-radio-button>
        <el-radio-button label="user">用户</el-radio-button>
      </el-radio-group>
    </el-card>

    <div class="result-section" v-if="!loading">
      <div v-if="showPosts">
        <h3>圈子动态 · {{ results.posts.length }} 条</h3>
        <div v-if="results.posts.length === 0">
          <el-empty description="暂未找到相关内容" />
        </div>
        <el-card v-for="post in results.posts" :key="post.id" class="result-card">
          <div class="post-header">
            <div>
              <strong>{{ post.author.username }}</strong>
              <span class="meta">{{ post.ipRegion || post.ipAddressMasked || '未知' }} ｜ {{ formatTime(post.createdAt) }}</span>
            </div>
          </div>
          <p>{{ post.content }}</p>
        </el-card>
      </div>

      <div v-if="showBeverages">
        <h3>酒款</h3>
        <div v-if="results.beverages.length === 0">
          <el-empty description="暂未找到相关酒款" />
        </div>
        <el-card v-for="beverage in results.beverages" :key="beverage.id" class="result-card">
          <div class="beverage">
            <img :src="beverage.coverImageUrl" alt="" v-if="beverage.coverImageUrl" />
            <div>
              <strong>{{ beverage.name }}</strong>
              <p>{{ beverage.type }} ｜ {{ beverage.origin || '产地未知' }}</p>
            </div>
          </div>
        </el-card>
      </div>

      <div v-if="showUsers">
        <h3>用户</h3>
        <div v-if="results.users.length === 0">
          <el-empty description="暂未找到相关用户" />
        </div>
        <el-card
          v-for="user in results.users"
          :key="user.id"
          class="result-card user-card"
          @click="goUser(user.id)"
        >
          <el-avatar :src="user.avatarUrl">
            <el-icon><User /></el-icon>
          </el-avatar>
          <div>
            <strong>{{ user.username }}</strong>
            <p>{{ user.bio }}</p>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch } from 'vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import { useRoute, useRouter } from 'vue-router'
import { searchAll } from '@/api/search'

dayjs.extend(relativeTime)

const route = useRoute()
const router = useRouter()
const keyword = ref(route.query.q || '')
const type = ref('all')
const loading = ref(false)
const results = reactive({
  posts: [],
  beverages: [],
  users: []
})

const showPosts = computed(() => type.value === 'all' || type.value === 'circle')
const showBeverages = computed(() => type.value === 'all' || type.value === 'beverage')
const showUsers = computed(() => type.value === 'all' || type.value === 'user')

const handleSearch = async () => {
  if (!keyword.value) return
  loading.value = true
  router.replace({ path: '/search', query: { q: keyword.value } })
  try {
    const res = await searchAll({ keyword: keyword.value, type: type.value })
    results.posts = res.data.posts || []
    results.beverages = res.data.beverages || []
    results.users = res.data.users || []
  } finally {
    loading.value = false
  }
}

const goUser = (id) => {
  router.push(`/users/${id}`)
}

const formatTime = (time) => dayjs(time).fromNow()

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
  max-width: 1000px;
  margin: 0 auto;
}

.search-box {
  margin-bottom: 20px;
}

.type-tabs {
  margin-top: 12px;
}

.result-card {
  margin-bottom: 12px;
}

.beverage {
  display: flex;
  gap: 15px;
  align-items: center;
}

.beverage img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
}

.user-card {
  display: flex;
  gap: 12px;
  cursor: pointer;
  align-items: center;
}

.meta {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
</style>
