<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="rag-answer-dialog-wrapper"
      :style="dialogStyle"
    >
      <div class="rag-answer-dialog" :class="{ minimized: isMinimized }">
        <div class="dialog-header" @mousedown="startDrag">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI推荐理由</span>
          <div class="header-actions">
            <el-button
              text
              :icon="isMinimized ? FullScreen : Minus"
              @click="toggleMinimize"
            />
            <el-button text :icon="Close" @click="handleClose" />
          </div>
        </div>

        <div v-if="!isMinimized" class="dialog-content">
          <div v-if="loading" class="loading-container">
            <el-skeleton :rows="3" animated />
            <p class="loading-text">AI正在思考中...</p>
          </div>

          <div v-else-if="recommendationReason" class="reason-content">
            <div class="markdown-content" v-html="formatMarkdown(recommendationReason)"></div>
          </div>

          <!-- Wiki内容列表 -->
          <div v-if="wikiContents && wikiContents.length > 0" class="wiki-list">
            <h4>推荐Wiki内容</h4>
            <div class="wiki-cards">
              <div
                v-for="wiki in wikiContents"
                :key="wiki.id"
                class="wiki-card"
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
          
          <!-- 兼容旧版：酒类列表 -->
          <div v-else-if="beverages && beverages.length > 0" class="beverages-list">
            <h4>推荐酒类</h4>
            <div class="beverage-cards">
              <div
                v-for="beverage in beverages"
                :key="beverage.id"
                class="beverage-card"
                @click="goToBeverageDetail(beverage.id)"
              >
                <div v-if="beverage.coverImageId" class="beverage-image">
                  <img :src="getImageUrl(beverage.coverImageId)" :alt="beverage.name" />
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
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ChatDotRound, Close, Minus, FullScreen } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  recommendationReason: {
    type: String,
    default: ''
  },
  beverages: {
    type: Array,
    default: () => []
  },
  wikiContents: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'close'])

const router = useRouter()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const isMinimized = ref(false)
const dialogWidth = ref('500px')
const dialogStyle = ref({
  position: 'fixed',
  bottom: '20px',
  right: '20px',
  width: '500px',
  zIndex: 2000
})
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })

// 从localStorage恢复位置
onMounted(() => {
  const savedPosition = localStorage.getItem('ragDialogPosition')
  if (savedPosition) {
    try {
      const { x, y } = JSON.parse(savedPosition)
      dialogStyle.value = {
        ...dialogStyle.value,
        left: `${x}px`,
        top: `${y}px`,
        bottom: 'auto',
        right: 'auto'
      }
    } catch (e) {
      console.error('恢复对话框位置失败', e)
    }
  }
})

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
  const width = parseInt(dialogWidth.value)
  
  dialogStyle.value = {
    ...dialogStyle.value,
    left: `${Math.max(0, Math.min(x, window.innerWidth - width))}px`,
    top: `${Math.max(0, Math.min(y, window.innerHeight - 200))}px`,
    bottom: 'auto',
    right: 'auto'
  }
}

const stopDrag = () => {
  if (isDragging.value) {
    isDragging.value = false
    // 保存位置
    const style = dialogStyle.value
    if (style.left && style.top) {
      localStorage.setItem('ragDialogPosition', JSON.stringify({
        x: parseInt(style.left),
        y: parseInt(style.top)
      }))
    }
  }
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// 监听visible变化，确保对话框显示
watch(visible, (newVal) => {
  if (newVal) {
    // 确保z-index足够高
    dialogStyle.value.zIndex = 2000
    // 如果没有保存的位置，使用默认位置
    if (!dialogStyle.value.left) {
      dialogStyle.value = {
        ...dialogStyle.value,
        bottom: '20px',
        right: '20px'
      }
    }
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
})

const toggleMinimize = () => {
  isMinimized.value = !isMinimized.value
  dialogWidth.value = isMinimized.value ? '300px' : '500px'
  dialogStyle.value = {
    ...dialogStyle.value,
    width: dialogWidth.value
  }
}

const handleClose = () => {
  visible.value = false
  emit('close')
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
  // 简单的Markdown渲染
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br/>')
}
</script>

<style scoped>
.rag-answer-dialog-wrapper {
  position: fixed;
  z-index: 2000;
}

.rag-answer-dialog {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.3s;
}

.rag-answer-dialog.minimized {
  width: 300px;
}

.rag-answer-dialog:not(.minimized) {
  width: 500px;
  max-height: 80vh;
}

.dialog-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  cursor: move;
  user-select: none;
}

.dialog-header .header-actions {
  margin-left: auto;
  display: flex;
  gap: 4px;
}

.dialog-content {
  padding: 20px;
  max-height: 600px;
  overflow-y: auto;
}

.loading-container {
  padding: 20px;
}

.loading-text {
  text-align: center;
  color: #909399;
  margin-top: 10px;
}

.reason-content {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.markdown-content {
  line-height: 1.8;
  color: #303133;
}

.beverages-list {
  margin-top: 20px;
}

.beverages-list h4 {
  margin-bottom: 16px;
  color: #303133;
}

.beverage-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.beverage-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.beverage-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.beverage-image {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
}

.beverage-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.beverage-info {
  flex: 1;
}

.beverage-info h5 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #303133;
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

.wiki-list {
  margin-top: 20px;
}

.wiki-list h4 {
  margin-bottom: 16px;
  color: #303133;
}

.wiki-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.wiki-card {
  padding: 12px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.wiki-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.wiki-info h5 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #303133;
}

.wiki-summary {
  margin: 4px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.wiki-similarity {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style>

