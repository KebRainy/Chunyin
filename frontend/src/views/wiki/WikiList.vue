<template>
  <div class="wiki-page">
    <el-card class="wiki-search">
      <div class="header">
        <h2>Wiki 知识库</h2>
        <el-button type="primary" @click="goEditor" v-if="userStore.isLoggedIn">新建条目</el-button>
      </div>
      <el-input
        v-model="keyword"
        placeholder="输入关键字检索百科"
        @keyup.enter="loadPages"
      >
        <template #append>
          <el-button @click="loadPages">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="10">
        <el-card v-for="page in pages" :key="page.id" class="wiki-card" @click="selectPage(page)">
          <div class="card-header">
            <h3>{{ page.title }}</h3>
            <el-tag :type="statusType(page.status)">{{ page.status }}</el-tag>
          </div>
          <p class="summary">{{ page.summary }}</p>
          <small>最后更新：{{ formatTime(page.updatedAt) }}</small>
        </el-card>
        <el-pagination
          layout="prev, pager, next"
          :page-size="pageSize"
          :current-page="page"
          :total="total"
          @current-change="handlePageChange"
        />
      </el-col>
      <el-col :span="14">
        <el-card v-if="currentPage">
          <div class="detail-header">
            <h2>{{ currentPage.title }}</h2>
            <div class="actions">
              <el-button
                type="primary"
                size="small"
                @click="editCurrent"
                v-if="userStore.isLoggedIn"
              >编辑</el-button>
            </div>
          </div>
          <p class="detail-summary">{{ currentPage.summary }}</p>
          <div class="detail-content" v-html="formatContent(currentPage.content)"></div>
          <small>最后编辑：{{ currentPage.lastEditorName || '系统' }} ｜ {{ formatTime(currentPage.updatedAt) }}</small>
        </el-card>
        <el-empty v-else description="选择左侧的Wiki条目" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import { fetchWikiPages } from '@/api/wiki'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

const keyword = ref('')
const pages = ref([])
const page = ref(1)
const pageSize = ref(5)
const total = ref(0)
const currentPage = ref(null)

const loadPages = async () => {
  const res = await fetchWikiPages({
    page: page.value,
    pageSize: pageSize.value,
    keyword: keyword.value
  })
  pages.value = res.data.items
  total.value = res.data.total
  currentPage.value = pages.value[0] || null
}

const handlePageChange = (val) => {
  page.value = val
  loadPages()
}

const selectPage = (pageItem) => {
  currentPage.value = pageItem
}

const goEditor = () => {
  router.push('/wiki/edit')
}

const editCurrent = () => {
  router.push(`/wiki/edit/${currentPage.value.id}`)
}

const statusType = (status) => {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'UNDER_REVIEW') return 'warning'
  return 'info'
}

const formatContent = (content) => {
  return content.replace(/\n/g, '<br/>')
}

const formatTime = (time) => dayjs(time).format('YYYY-MM-DD HH:mm')

onMounted(() => {
  loadPages()
})
</script>

<style scoped>
.wiki-page {
  max-width: 1200px;
  margin: 0 auto;
}

.wiki-search {
  margin-bottom: 20px;
}

.wiki-card {
  margin-bottom: 12px;
  cursor: pointer;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary {
  color: #606266;
  margin: 6px 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.detail-summary {
  color: #606266;
  margin-bottom: 10px;
}

.detail-content {
  line-height: 1.8;
  margin-bottom: 12px;
  white-space: pre-wrap;
}
</style>
