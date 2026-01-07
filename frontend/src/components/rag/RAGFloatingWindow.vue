<template>
  <Teleport to="body">
    <!-- 只在登录后显示 -->
    <template v-if="isLoggedIn">
      <!-- 触发按钮 -->
      <div
        v-if="!visible"
        class="rag-trigger-button"
        @click="openWindow"
      >
        <el-icon><ChatDotRound /></el-icon>
        <span>智能荐酒</span>
      </div>

    <!-- 悬浮窗 -->
    <Transition name="rag-window">
      <div
        v-if="visible"
        ref="wrapperRef"
        class="rag-floating-window-wrapper"
        :style="windowStyle"
      >
        <div ref="windowRef" class="rag-floating-window" :class="{ 'show-history': showHistory }">
          <!-- 标题栏 -->
          <div class="window-header" @mousedown="startDrag">
            <el-icon><ChatDotRound /></el-icon>
            <span>智能荐酒</span>
            <div class="header-actions">
              <el-button
                text
                :icon="showHistory ? ChatDotRound : Document"
                @click="toggleHistory"
                :title="showHistory ? '返回输入框' : '查看对话历史'"
              />
              <el-button text :icon="Minus" @click="closeWindow" title="关闭" />
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="window-body">
          <!-- 输入框和推荐结果视图 -->
          <div v-if="!showHistory" class="window-content">
            <div class="search-section">
              <el-input
                v-model="query"
                type="textarea"
                :rows="3"
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
                  size="small"
                  @click="query = example"
                >
                  {{ example }}
                </el-tag>
              </div>

              <div class="search-actions">
                <el-button
                  type="primary"
                  :loading="loading"
                  @click="handleSearch"
                >
                  <el-icon><Search /></el-icon>
                  智能推荐
                </el-button>
                <el-button @click="clearSearch">清空</el-button>
              </div>
            </div>

            <!-- 推荐理由 -->
            <div v-if="results?.recommendationReason" class="recommendation-reason">
              <h4>推荐理由</h4>
              <div class="reason-content" v-html="formatMarkdown(results.recommendationReason)"></div>
            </div>

            <!-- 加载状态 -->
            <div v-if="loading && !results" class="loading-section">
              <el-skeleton :rows="3" animated />
              <p class="loading-text">AI正在分析您的需求...</p>
            </div>

            <!-- Wiki内容列表 -->
            <div v-if="results && results.wikiContents && results.wikiContents.length > 0" class="results-section">
              <h4>推荐Wiki内容</h4>
              <div class="wiki-list">
                <div
                  v-for="wiki in results.wikiContents"
                  :key="wiki.id"
                  class="wiki-item"
                  @click="goToWikiPage(wiki)"
                >
                  <div class="wiki-info">
                    <h5>{{ wiki.title }}</h5>
                    <p v-if="wiki.summary" class="wiki-summary">{{ wiki.summary }}</p>
                    <div v-if="wiki.similarity" class="wiki-similarity">
                      相似度: {{ (wiki.similarity * 100).toFixed(1) }}%
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 推荐结果 -->
            <div v-else-if="results && results.beverages && results.beverages.length > 0" class="results-section">
              <h4>推荐结果</h4>
              <div class="beverages-list">
                <div
                  v-for="beverage in results.beverages"
                  :key="beverage.id"
                  class="beverage-item"
                  @click="goToBeverageDetail(beverage.id)"
                >
                  <div v-if="beverage.coverImageId" class="beverage-image">
                    <img :src="getImageUrl(beverage.coverImageId)" :alt="beverage.name" />
                  </div>
                  <div v-else class="beverage-image no-image">
                    <el-icon><Picture /></el-icon>
                  </div>
                  <div class="beverage-info">
                    <h5>{{ beverage.name }}</h5>
                    <p class="beverage-type">{{ beverage.type }}</p>
                    <p v-if="beverage.origin" class="beverage-origin">{{ beverage.origin }}</p>
                    <div v-if="beverage.rating" class="beverage-rating">
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

            <!-- 空结果：只有在没有推荐理由且没有酒类时才显示 -->
            <div v-else-if="results && !results.recommendationReason && (!results.beverages || results.beverages.length === 0) && (!results.wikiContents || results.wikiContents.length === 0)" class="empty-section">
              <el-empty description="没有找到匹配的内容" :image-size="80" />
            </div>
          </div>

          <!-- 对话历史视图 -->
          <div v-else class="history-view">
            <div class="history-header">
              <span>对话历史</span>
              <el-button text size="small" @click="clearHistory">
                <el-icon><Delete /></el-icon>
                清空历史
              </el-button>
            </div>
            <div class="history-content">
              <div v-if="conversationHistory.length === 0" class="empty-history">
                <el-empty description="暂无对话历史" :image-size="80" />
              </div>
              <div v-else class="history-list">
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
                    <span>{{ turn.aiResponse }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    </Transition>
    </template>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ChatDotRound, Minus, Search, Picture, User, Document, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { recommendApi } from '@/api/recommend'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

// 检查是否已登录
const isLoggedIn = computed(() => userStore.isLoggedIn)

const visible = ref(false)
const query = ref('')
const loading = ref(false)
const results = ref(null)
const conversationHistory = ref([]) // 对话历史
const showHistory = ref(false) // 是否显示侧边栏
const wrapperRef = ref(null)
const windowRef = ref(null)

const windowStyle = ref({
  position: 'fixed',
  bottom: '20px',
  right: '20px',
  width: '480px',
  maxWidth: 'calc(100vw - 40px)',
  maxHeight: 'calc(100vh - 40px)',
  zIndex: 2000
})

const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })

const queryExamples = [
  '适合晚餐的红酒',
  '清淡口味的啤酒',
  '适合聚会的鸡尾酒',
  '高端的威士忌',
  '适合女性的甜酒'
]

// 从localStorage恢复位置和状态
onMounted(() => {
  const savedPosition = localStorage.getItem('ragWindowPosition')
  const savedVisible = localStorage.getItem('ragWindowVisible')
  
  if (savedPosition) {
    try {
      const { x, y, width } = JSON.parse(savedPosition)
      windowStyle.value = {
        ...windowStyle.value,
        left: `${x}px`,
        top: `${y}px`,
        bottom: 'auto',
        right: 'auto',
        width: width || '600px'
      }
    } catch (e) {
      console.error('恢复窗口位置失败', e)
    }
  }
  
  if (savedVisible === 'true') {
    visible.value = true
  }

  window.addEventListener('resize', handleResize)
})

// 保存可见状态
watch(visible, (newVal) => {
  localStorage.setItem('ragWindowVisible', String(newVal))
  if (newVal) {
    windowStyle.value.zIndex = 2000
    nextTick(() => {
      ensureInViewport()
    })
  }
})

const openWindow = () => {
  visible.value = true
  // 打开窗口时，默认进入输入页面
  showHistory.value = false
}

const closeWindow = () => {
  visible.value = false
}

const clamp = (value, min, max) => Math.min(Math.max(value, min), max)

const ensureInViewport = () => {
  const el = windowRef.value
  if (!el) return

  const rect = el.getBoundingClientRect()
  const padding = 12
  const maxX = Math.max(padding, window.innerWidth - rect.width - padding)
  const maxY = Math.max(padding, window.innerHeight - rect.height - padding)

  const hasLeft = windowStyle.value.left != null
  const hasTop = windowStyle.value.top != null
  let x = hasLeft ? parseInt(windowStyle.value.left) : window.innerWidth - rect.width - 20
  let y = hasTop ? parseInt(windowStyle.value.top) : window.innerHeight - rect.height - 20

  x = clamp(x, padding, maxX)
  y = clamp(y, padding, maxY)

  windowStyle.value = {
    ...windowStyle.value,
    left: `${x}px`,
    top: `${y}px`,
    bottom: 'auto',
    right: 'auto'
  }
  savePosition()
}

const handleResize = () => {
  if (!visible.value) return
  nextTick(() => {
    ensureInViewport()
  })
}

// 拖拽功能
const startDrag = (e) => {
  if (e.target.closest('.header-actions')) {
    return
  }
  isDragging.value = true
  const rect = e.currentTarget.getBoundingClientRect()
  dragOffset.value = {
    x: e.clientX - rect.left,
    y: e.clientY - rect.top
  }
  
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e) => {
  if (!isDragging.value) return
  
  const x = e.clientX - dragOffset.value.x
  const y = e.clientY - dragOffset.value.y
  const rect = windowRef.value?.getBoundingClientRect()
  const width = rect?.width || parseInt(windowStyle.value.width) || 480
  const height = rect?.height || 520
  const padding = 12
  
  windowStyle.value = {
    ...windowStyle.value,
    left: `${clamp(x, padding, window.innerWidth - width - padding)}px`,
    top: `${clamp(y, padding, window.innerHeight - height - padding)}px`,
    bottom: 'auto',
    right: 'auto'
  }
}

const stopDrag = () => {
  if (isDragging.value) {
    isDragging.value = false
    savePosition()
  }
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

const savePosition = () => {
  const style = windowStyle.value
  if (style.left && style.top) {
    localStorage.setItem('ragWindowPosition', JSON.stringify({
      x: parseInt(style.left),
      y: parseInt(style.top),
      width: style.width
    }))
  }
}

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  window.removeEventListener('resize', handleResize)
})

const handleSearch = async () => {
  if (!query.value.trim()) {
    ElMessage.warning('请输入查询内容')
    return
  }

  loading.value = true
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
}

const clearHistory = () => {
  conversationHistory.value = []
}

const toggleHistory = () => {
  showHistory.value = !showHistory.value
  nextTick(() => {
    ensureInViewport()
  })
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

const formatMarkdown = (text) => {
  if (!text) return ''
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br/>')
}
</script>

<style scoped>
/* 触发按钮 */
.rag-trigger-button {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1999;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: var(--app-primary, #2f54eb);
  color: #fff;
  border-radius: 25px;
  box-shadow: 0 8px 24px rgba(47, 84, 235, 0.25);
  cursor: pointer;
  will-change: transform;
  transition:
    transform var(--motion-normal, 220ms) var(--motion-ease, ease),
    box-shadow var(--motion-normal, 220ms) var(--motion-ease, ease);
  font-size: 14px;
  font-weight: 500;
}

.rag-trigger-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 25px rgba(102, 126, 234, 0.5);
}

.rag-trigger-button .el-icon {
  font-size: 18px;
}

/* 悬浮窗 */
.rag-floating-window-wrapper {
  position: fixed;
  z-index: 2000;
  max-width: calc(100vw - 40px);
  max-height: calc(100vh - 40px);
}

.rag-floating-window {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  width: 100%;
}

.rag-floating-window.show-history {
  /* 对话历史页面：从页面顶部开始，底部留出空间 */
  height: calc(100vh - 20px);
  max-height: calc(100vh - 20px);
}

.window-body {
  position: relative;
  min-height: 30px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.rag-floating-window:not(.show-history) .window-body {
  /* 输入页面：自适应高度 */
  flex: 0 1 auto;
  max-height: calc(100vh - 100px);
}

.rag-floating-window.show-history .window-body {
  /* 对话历史页面：从页面顶部开始，底部留出空间 */
  flex: 1;
  height: calc(100vh - 80px);
  max-height: calc(100vh - 80px);
}

.window-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  background: var(--app-primary, #2f54eb);
  color: #fff;
  cursor: move;
  user-select: none;
}

.window-header .header-actions {
  margin-left: auto;
  display: flex;
  gap: 4px;
}

.window-header .el-button {
  color: #fff;
}

.window-header .el-button:hover {
  background: rgba(255, 255, 255, 0.2);
}

.window-content {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
  max-height: 100%;
}

.search-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.query-examples {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.example-label {
  color: #606266;
  font-size: 12px;
}

.example-tag {
  cursor: pointer;
  will-change: transform;
  transition:
    transform var(--motion-fast, 160ms) var(--motion-ease, ease),
    background-color var(--motion-fast, 160ms) var(--motion-ease, ease),
    color var(--motion-fast, 160ms) var(--motion-ease, ease);
}

.example-tag:hover {
  background-color: #409eff;
  color: #fff;
  transform: translateY(-1px);
}

.search-actions {
  display: flex;
  gap: 10px;
}

.recommendation-reason {
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  border-left: 4px solid #667eea;
}

.recommendation-reason h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 16px;
}

.reason-content {
  line-height: 1.8;
  color: #606266;
  font-size: 14px;
}

.loading-section {
  padding: 20px 0;
}

.loading-text {
  text-align: center;
  color: #909399;
  margin-top: 10px;
  font-size: 14px;
}

.results-section {
  margin-top: 20px;
}

.results-section h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
}

.beverages-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.beverage-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  will-change: transform;
  transition:
    transform var(--motion-normal, 220ms) var(--motion-ease, ease),
    box-shadow var(--motion-normal, 220ms) var(--motion-ease, ease),
    border-color var(--motion-normal, 220ms) var(--motion-ease, ease);
}

.beverage-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.beverage-image {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
}

.beverage-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.beverage-image.no-image {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 32px;
}

.beverage-info {
  flex: 1;
  min-width: 0;
}

.beverage-info h5 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.beverage-type {
  margin: 4px 0;
  color: #606266;
  font-size: 14px;
}

.beverage-origin {
  margin: 4px 0;
  color: #909399;
  font-size: 12px;
}

.beverage-rating {
  margin-top: 8px;
}

.empty-section {
  padding: 40px 0;
  text-align: center;
}

/* 对话历史视图 */
.history-view {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  animation: fadeIn 0.3s ease-out;
  overflow: hidden;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
  font-weight: 500;
  color: #303133;
  font-size: 16px;
}

.history-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.empty-history {
  padding: 20px 0;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.history-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.user-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  background: #ecf5ff;
  border-radius: 8px;
  border-left: 4px solid #409eff;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.1);
}

.user-message .el-icon {
  color: #409eff;
  margin-top: 2px;
  font-size: 14px;
}

.ai-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  background: #f0f9ff;
  border-radius: 8px;
  border-left: 4px solid #67c23a;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: 0 2px 4px rgba(103, 194, 58, 0.1);
  white-space: pre-wrap;
}

.ai-message .el-icon {
  color: #67c23a;
  margin-top: 2px;
  font-size: 14px;
}

.wiki-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.wiki-item {
  padding: 10px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  will-change: transform;
  transition:
    transform var(--motion-normal, 220ms) var(--motion-ease, ease),
    box-shadow var(--motion-normal, 220ms) var(--motion-ease, ease),
    border-color var(--motion-normal, 220ms) var(--motion-ease, ease);
}

.wiki-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
  transform: translateY(-1px);
}

.rag-window-enter-active,
.rag-window-leave-active {
  transition:
    opacity var(--motion-normal, 220ms) var(--motion-ease, ease),
    transform var(--motion-normal, 220ms) var(--motion-ease, ease);
}

.rag-window-enter-from,
.rag-window-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.98);
}

.wiki-info h5 {
  margin: 0 0 6px 0;
  font-size: 15px;
  color: #303133;
}

.wiki-summary {
  margin: 4px 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.wiki-similarity {
  margin-top: 6px;
  color: #909399;
  font-size: 11px;
}
</style>

