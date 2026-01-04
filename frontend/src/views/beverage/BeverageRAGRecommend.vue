<template>
  <div class="rag-recommend-container">
    <el-card class="search-card">
      <h2>智能荐酒</h2>
      <p class="subtitle">告诉我您的需求，AI为您推荐合适的酒类</p>

      <!-- 对话历史显示 -->
      <div v-if="conversationHistory.length > 0" class="conversation-history">
        <div class="history-header">
          <span>对话历史</span>
          <el-button text size="small" @click="conversationHistory = []">清空历史</el-button>
        </div>
        <div class="history-list">
          <div
            v-for="(turn, index) in conversationHistory"
            :key="index"
            class="history-item"
          >
            <div class="user-message">
              <el-icon><User /></el-icon>
              <span>{{ turn.userQuery }}</span>
            </div>
            <div v-if="turn.aiResponse" class="ai-message">
              <el-icon><ChatDotRound /></el-icon>
              <span>{{ turn.aiResponse.substring(0, 100) }}{{ turn.aiResponse.length > 100 ? '...' : '' }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="search-section">
        <el-input
          v-model="query"
          type="textarea"
          :rows="4"
          placeholder="例如：我想要一款适合晚餐的清淡红酒"
          :maxlength="200"
          show-word-limit
          @keyup.ctrl.enter="handleSearch"
        />

        <div class="query-examples">
          <span class="example-label">查询示例：</span>
          <el-tag
            v-for="example in queryExamples"
            :key="example"
            class="example-tag"
            @click="query = example"
          >
            {{ example }}
          </el-tag>
        </div>

        <div class="search-actions">
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleSearch"
          >
            <el-icon><Search /></el-icon>
            智能推荐
          </el-button>
          <el-button size="large" @click="clearSearch">清空</el-button>
        </div>
      </div>
    </el-card>

    <div v-if="loading && !results" class="loading-section">
      <el-skeleton :rows="5" animated />
      <p class="loading-text">AI正在分析您的需求...</p>
    </div>

    <!-- Wiki内容列表 -->
    <div v-if="results && results.wikiContents && results.wikiContents.length > 0" class="results-section">
      <h3>推荐Wiki内容</h3>
      <div class="wiki-grid">
        <div
          v-for="wiki in results.wikiContents"
          :key="wiki.id"
          class="wiki-card"
          @click="goToWikiPage(wiki)"
        >
          <div class="card-body">
            <h4>{{ wiki.title }}</h4>
            <p v-if="wiki.summary" class="card-summary">{{ wiki.summary }}</p>
            <div v-if="wiki.similarity" class="card-similarity">
              相似度: {{ (wiki.similarity * 100).toFixed(1) }}%
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 兼容旧版：酒类列表 -->
    <div v-else-if="results && results.beverages && results.beverages.length > 0" class="results-section">
      <h3>推荐结果</h3>
      <div class="beverages-grid">
        <div
          v-for="beverage in results.beverages"
          :key="beverage.id"
          class="beverage-card"
          @click="goToBeverageDetail(beverage.id)"
        >
          <div v-if="beverage.coverImageId" class="card-image">
            <img :src="getImageUrl(beverage.coverImageId)" :alt="beverage.name" />
          </div>
          <div v-else class="card-image no-image">
            <el-icon><Picture /></el-icon>
          </div>
          <div class="card-body">
            <h4>{{ beverage.name }}</h4>
            <p class="card-type">{{ beverage.type }}</p>
            <p v-if="beverage.origin" class="card-origin">{{ beverage.origin }}</p>
            <div v-if="beverage.rating" class="card-rating">
              <el-rate
                :model-value="beverage.rating"
                disabled
                show-score
                text-color="#ff9900"
                score-template="{value}"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="results && !results.recommendationReason && (!results.beverages || results.beverages.length === 0) && (!results.wikiContents || results.wikiContents.length === 0)" class="empty-section">
      <el-empty description="没有找到匹配的内容，请尝试其他查询" />
    </div>

    <!-- 悬浮回答框 -->
    <RAGAnswerDialog
      v-model="showDialog"
      :recommendation-reason="results?.recommendationReason"
      :beverages="results?.beverages"
      :wiki-contents="results?.wikiContents"
      :loading="loading"
      @close="handleDialogClose"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Picture, User, ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { recommendApi } from '@/api/recommend'
import RAGAnswerDialog from '@/components/rag/RAGAnswerDialog.vue'

const router = useRouter()

const query = ref('')
const loading = ref(false)
const results = ref(null)
const showDialog = ref(false)
const conversationHistory = ref([]) // 对话历史

const queryExamples = [
  '适合晚餐的红酒',
  '清淡口味的啤酒',
  '适合聚会的鸡尾酒',
  '高端的威士忌',
  '适合女性的甜酒'
]

const handleSearch = async () => {
  if (!query.value.trim()) {
    ElMessage.warning('请输入查询内容')
    return
  }

  loading.value = true
  showDialog.value = true
  results.value = null

  try {
    // 构建请求数据，包含对话历史
    const requestData = {
      query: query.value,
      topK: 10,
      includeReason: true,
      conversationHistory: conversationHistory.value.map(turn => ({
        userQuery: turn.userQuery,
        aiResponse: turn.aiResponse
      }))
    }
    
    const response = await recommendApi.ragRecommendBeveragesWithHistory(requestData)
    
    if (response.code === 200 && response.data) {
      results.value = response.data
      
      // 保存到对话历史
      conversationHistory.value.push({
        userQuery: query.value,
        aiResponse: results.value.recommendationReason || ''
      })
      
      // 限制对话历史长度（最多保留10轮）
      if (conversationHistory.value.length > 10) {
        conversationHistory.value = conversationHistory.value.slice(-10)
      }
      
      if (results.value.wikiContents && results.value.wikiContents.length > 0) {
        ElMessage.success(`为您找到 ${results.value.wikiContents.length} 条Wiki内容`)
      } else if (results.value.beverages && results.value.beverages.length > 0) {
        ElMessage.success(`为您找到 ${results.value.beverages.length} 款推荐酒类`)
      }
    } else {
      ElMessage.error(response.message || '推荐失败，请稍后重试')
    }
  } catch (error) {
    console.error('推荐失败', error)
    ElMessage.error('推荐失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const clearSearch = () => {
  query.value = ''
  results.value = null
  showDialog.value = false
  conversationHistory.value = [] // 清空对话历史
}

const handleDialogClose = () => {
  showDialog.value = false
}

const goToBeverageDetail = (beverageId) => {
  router.push(`/beverage/${beverageId}`)
}

const goToWikiPage = (wiki) => {
  if (wiki.slug) {
    router.push(`/wiki/${encodeURIComponent(wiki.slug)}`)
  } else if (wiki.title) {
    // 将标题转换为URL友好的slug
    const slug = wiki.title
      .toLowerCase()
      .replace(/\s+/g, '-')
      .replace(/[^\w\u4e00-\u9fa5-]/g, '')
    router.push(`/wiki/${encodeURIComponent(slug)}`)
  } else {
    router.push('/wiki')
  }
}

const getImageUrl = (imageId) => {
  return `/api/file/${imageId}`
}
</script>

<style scoped>
.rag-recommend-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.search-card {
  margin-bottom: 30px;
}

.search-card h2 {
  margin: 0 0 8px 0;
  color: #303133;
}

.subtitle {
  margin: 0 0 24px 0;
  color: #909399;
  font-size: 14px;
}

.conversation-history {
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  max-height: 300px;
  overflow-y: auto;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 500;
  color: #303133;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-message {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 8px;
  border-left: 3px solid #409eff;
}

.user-message .el-icon {
  color: #409eff;
  margin-top: 2px;
}

.ai-message {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 8px;
  border-left: 3px solid #67c23a;
  color: #606266;
  font-size: 14px;
}

.ai-message .el-icon {
  color: #67c23a;
  margin-top: 2px;
}

.search-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.query-examples {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.example-label {
  color: #606266;
  font-size: 14px;
}

.example-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.example-tag:hover {
  background-color: #409eff;
  color: #fff;
}

.search-actions {
  display: flex;
  gap: 12px;
}

.loading-section {
  padding: 40px 0;
}

.loading-text {
  text-align: center;
  color: #909399;
  margin-top: 20px;
}

.results-section {
  margin-top: 30px;
}

.results-section h3 {
  margin-bottom: 20px;
  color: #303133;
}

.beverages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.wiki-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.wiki-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.wiki-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.wiki-card .card-body h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 18px;
}

.wiki-card .card-summary {
  margin: 8px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.wiki-card .card-similarity {
  margin-top: 12px;
  color: #909399;
  font-size: 12px;
}

.beverage-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.beverage-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.15);
  transform: translateY(-4px);
}

.card-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: #f5f7fa;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-image.no-image {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 48px;
}

.card-body {
  padding: 16px;
}

.card-body h4 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-type {
  margin: 4px 0;
  color: #606266;
  font-size: 14px;
}

.card-origin {
  margin: 4px 0;
  color: #909399;
  font-size: 12px;
}

.card-rating {
  margin-top: 12px;
}

.empty-section {
  padding: 60px 0;
  text-align: center;
}
</style>

