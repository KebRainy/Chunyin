<template>
  <div class="wiki-detail-container">
    <div v-if="loading" class="loading">
      <el-skeleton :rows="5" animated />
    </div>
    <div v-else-if="!wiki" class="not-found">
      <el-empty description="词条不存在或已被删除" />
    </div>
    <div v-else class="wiki-content">
      <!-- 页头 -->
      <div class="wiki-header">
        <h1>{{ wiki.title }}</h1>
        <div class="wiki-meta">
          <el-tag type="success" size="small" v-if="wiki.status === 'PUBLISHED'">已发布</el-tag>
          <el-tag type="warning" size="small" v-else-if="wiki.status === 'UNDER_REVIEW'">待审核</el-tag>
          <el-tag size="small" v-else>草稿</el-tag>
          <span>最后编辑：{{ formatTime(wiki.updatedAt) }}</span>
          <span v-if="wiki.lastEditorName">编辑者：{{ wiki.lastEditorName }}</span>
        </div>
      </div>

      <!-- 摘要 -->
      <div v-if="wiki.summary" class="wiki-summary">
        {{ wiki.summary }}
      </div>

      <!-- 内容 -->
      <div class="wiki-body">
        <div class="wiki-content-text" v-html="formatContent(wiki.content)"></div>
      </div>

      <!-- 操作按钮 -->
      <div class="wiki-actions">
        <el-button
          v-if="userStore.isLoggedIn && canEdit"
          type="primary"
          @click="editWiki"
        >
          ✎ 编辑此页
        </el-button>
        <el-button @click="viewHistory">📜 查看历史</el-button>
      </div>

      <!-- 页脚信息 -->
      <div class="wiki-footer">
        <div class="footer-info">
          <span>此页由 <strong>{{ wiki.author?.username || '系统' }}</strong> 创建于 {{ formatTime(wiki.createdAt) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { fetchWikiPage } from '@/api/wiki'
import { useUserStore } from '@/store/modules/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const wiki = ref(null)
const loading = ref(true)

const canEdit = computed(() => {
  if (!userStore.userInfo || !wiki.value) return false
  return userStore.userInfo.role === 'ADMIN' ||
         wiki.value.author?.id === userStore.userInfo.id
})

const loadWiki = async () => {
  loading.value = true
  try {
    const response = await fetchWikiPage(route.params.slug)
    wiki.value = response.data
  } catch (error) {
    console.error('Failed to load wiki:', error)
  } finally {
    loading.value = false
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return dayjs(time).format('YYYY年M月D日 HH:mm')
}

const formatContent = (content) => {
  if (!content) return ''
  return content
    .replace(/\n/g, '<br/>')
    .replace(/```(.*?)```/gs, '<pre><code>$1</code></pre>')
}

const editWiki = () => {
  if (wiki.value) {
    router.push(`/wiki/edit/${wiki.value.id}`)
  }
}

const viewHistory = () => {
  // TODO: 实现历史版本查看功能
  console.log('View history for wiki:', wiki.value.id)
}

onMounted(() => {
  loadWiki()
})
</script>

<style scoped>
.wiki-detail-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.loading {
  padding: 60px 20px;
}

.not-found {
  text-align: center;
  padding: 60px 20px;
}

/* 页头 */
.wiki-header {
  margin-bottom: 30px;
  padding-bottom: 24px;
  border-bottom: 2px solid #f0f0f0;
}

.wiki-header h1 {
  margin: 0 0 16px 0;
  font-size: 32px;
  color: #333;
  word-break: break-word;
}

.wiki-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #666;
  align-items: center;
  flex-wrap: wrap;
}

/* 摘要 */
.wiki-summary {
  padding: 16px;
  background-color: #f0f4ff;
  border-left: 4px solid #409eff;
  margin-bottom: 24px;
  border-radius: 4px;
  color: #555;
  line-height: 1.8;
  font-size: 14px;
}

/* 内容 */
.wiki-body {
  margin-bottom: 30px;
}

.wiki-content-text {
  font-size: 15px;
  line-height: 1.9;
  color: #333;
  word-break: break-word;
}

.wiki-content-text :deep(pre) {
  background-color: #f5f5f5;
  padding: 16px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 16px 0;
  border: 1px solid #e8e8e8;
}

.wiki-content-text :deep(code) {
  font-family: 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  background-color: transparent;
}

.wiki-content-text :deep(br) {
  display: block;
  content: '';
}

/* 操作按钮 */
.wiki-actions {
  margin-bottom: 30px;
  padding: 20px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.wiki-actions :deep(.el-button) {
  border-radius: 4px;
}

/* 页脚信息 */
.wiki-footer {
  padding: 20px 0;
  text-align: center;
  color: #999;
  font-size: 12px;
  line-height: 1.6;
}

.footer-info {
  margin-bottom: 8px;
}

/* 响应式 */
@media (max-width: 600px) {
  .wiki-detail-container {
    padding: 12px;
  }

  .wiki-header h1 {
    font-size: 24px;
  }

  .wiki-meta {
    gap: 8px;
  }

  .wiki-actions {
    flex-direction: column;
  }

  .wiki-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>

