<template>
  <div class="wiki-container">
    <!-- 页头 -->
    <div class="wiki-header">
      <div class="header-content">
        <h1>Wiki 知识库</h1>
        <p>饮品文化和知识的共建平台</p>
      </div>
      <el-button
        v-if="userStore.isLoggedIn"
        type="primary"
        size="large"
        @click="goEditor"
      >
        + 新建条目
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="wiki-search-section">
      <el-input
        v-model="keyword"
        placeholder="输入关键字搜索百科条目..."
        clearable
        @keyup.enter="searchPages"
      >
        <template #append>
          <el-button @click="searchPages">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
    </div>

    <!-- 主要内容 -->
    <el-row :gutter="20">
      <!-- 左侧：条目列表 -->
      <el-col :xs="24" :sm="24" :md="10">
        <div class="pages-list-section">
          <div class="section-header">
            <h3>{{ isSearching ? '搜索结果' : '所有条目' }}</h3>
            <span class="count">共 {{ total }} 条</span>
          </div>

          <div v-if="loading" class="loading">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else-if="pages.length === 0" class="empty">
            <el-empty description="暂无条目" />
          </div>
          <div v-else class="pages-list">
            <div
              v-for="item in pages"
              :key="item.id"
              class="wiki-card"
              :class="{ active: currentPage?.id === item.id }"
              @click="selectPage(item)"
            >
              <div class="card-header">
                <h4>{{ item.title }}</h4>
                <el-tag :type="statusType(item.status)" size="small">
                  {{ statusLabel(item.status) }}
                </el-tag>
              </div>
              <p class="summary">{{ item.summary || '暂无摘要' }}</p>
              <div class="card-footer">
                <small>{{ formatTime(item.updatedAt) }}</small>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div v-if="total > pageSize" class="pagination">
            <el-pagination
              layout="prev, pager, next"
              :page-size="pageSize"
              :current-page="currentPageNum"
              :total="total"
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </el-col>

      <!-- 右侧：条目详情 -->
      <el-col :xs="24" :sm="24" :md="14">
        <div class="detail-section">
          <div v-if="currentPage" class="wiki-detail">
            <div class="detail-header">
              <h2>{{ currentPage.title }}</h2>
              <div class="actions">
                <el-tag type="info" size="small" v-if="currentPage.author">
                  作者：{{ currentPage.author?.username }}
                </el-tag>
                <el-button
                  v-if="userStore.isLoggedIn && canEdit"
                  type="primary"
                  size="small"
                  @click="editCurrent"
                >
                  编辑
                </el-button>
              </div>
            </div>

            <div v-if="currentPage.summary" class="detail-summary">
              {{ currentPage.summary }}
            </div>

            <div class="detail-meta">
              <small>
                最后编辑：{{ currentPage.lastEditorName || currentPage.author?.username || '系统' }}
                ｜ {{ formatTime(currentPage.updatedAt) }}
              </small>
            </div>

            <el-divider />

            <div class="detail-content" v-html="formatContent(currentPage.content)"></div>
          </div>
          <div v-else class="empty-detail">
            <el-empty description="选择左侧的Wiki条目查看详情" />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { fetchWikiPages } from '@/api/wiki'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

const keyword = ref('')
const pages = ref([])
const currentPageNum = ref(1)
const pageSize = ref(5)
const total = ref(0)
const currentPage = ref(null)
const loading = ref(false)
const isSearching = ref(false)

const canEdit = computed(() => {
  if (!userStore.userInfo || !currentPage.value) return false
  return userStore.userInfo.role === 'ADMIN' ||
         currentPage.value.author?.id === userStore.userInfo.id
})

const loadPages = async () => {
  loading.value = true
  try {
    const res = await fetchWikiPages({
      page: currentPageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
    })
    pages.value = res.data.items || []
    total.value = res.data.total || 0
    if (pages.value.length > 0) {
      currentPage.value = pages.value[0]
    } else {
      currentPage.value = null
    }
  } catch (error) {
    console.error('Failed to load wiki pages:', error)
  } finally {
    loading.value = false
  }
}

const searchPages = async () => {
  isSearching.value = !!keyword.value
  currentPageNum.value = 1
  await loadPages()
}

const handlePageChange = (val) => {
  currentPageNum.value = val
  loadPages()
}

const selectPage = (item) => {
  currentPage.value = item
}

const goEditor = () => {
  router.push('/wiki/edit')
}

const editCurrent = () => {
  if (currentPage.value) {
    router.push(`/wiki/edit/${currentPage.value.id}`)
  }
}

const statusType = (status) => {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'UNDER_REVIEW') return 'warning'
  return 'info'
}

const statusLabel = (status) => {
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'UNDER_REVIEW') return '待审核'
  return '草稿'
}

const formatContent = (content) => {
  if (!content) return ''
  return content
    .replace(/\n/g, '<br/>')
    .replace(/```(.*?)```/gs, '<pre><code>$1</code></pre>')
}

const formatTime = (time) => dayjs(time).format('YYYY-MM-DD')

onMounted(() => {
  loadPages()
})
</script>

<style scoped>
.wiki-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

/* 页头 */
.wiki-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
}

.header-content h1 {
  margin: 0;
  font-size: 32px;
  margin-bottom: 8px;
}

.header-content p {
  margin: 0;
  opacity: 0.9;
  font-size: 14px;
}

/* 搜索区域 */
.wiki-search-section {
  margin-bottom: 24px;
}

:deep(.wiki-search-section .el-input) {
  font-size: 14px;
}

:deep(.wiki-search-section .el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

/* 条目列表 */
.pages-list-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.count {
  font-size: 12px;
  color: #999;
}

.pages-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 600px;
  overflow-y: auto;
}

.wiki-card {
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.wiki-card:hover {
  background-color: #f5f7fa;
  border-color: #d9d9d9;
}

.wiki-card.active {
  background-color: #e6f2ff;
  border-color: #667eea;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}

.card-header h4 {
  margin: 0;
  font-size: 14px;
  color: #333;
  flex: 1;
  word-break: break-word;
}

.summary {
  margin: 0;
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-footer {
  font-size: 11px;
  color: #999;
}

/* 加载状态 */
.loading {
  padding: 20px 0;
}

.empty {
  text-align: center;
  padding: 40px 0;
}

.empty-detail {
  text-align: center;
  padding: 60px 0;
}

/* 分页 */
.pagination {
  margin-top: 16px;
  text-align: center;
}

/* 详情区域 */
.detail-section {
  background: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-header h2 {
  margin: 0;
  flex: 1;
  word-break: break-word;
}

.actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.detail-summary {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
  line-height: 1.6;
}

.detail-meta {
  font-size: 12px;
  color: #999;
  margin-bottom: 16px;
}

.detail-content {
  font-size: 14px;
  color: #333;
  line-height: 1.8;
  word-break: break-word;
}

.detail-content :deep(pre) {
  background-color: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 12px 0;
}

.detail-content :deep(code) {
  font-family: 'Monaco', 'Courier New', monospace;
  font-size: 12px;
}

/* 响应式 */
@media (max-width: 768px) {
  .wiki-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .wiki-header h1 {
    font-size: 24px;
  }

  .detail-section {
    padding: 20px;
  }
}
</style>

