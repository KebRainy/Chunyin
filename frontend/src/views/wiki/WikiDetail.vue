<template>
  <div class="wiki-detail-container">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!wiki" class="not-found">词条不存在</div>
    <div v-else class="wiki-content">
      <div class="wiki-header">
        <h1>{{ wiki.title }}</h1>
        <div class="wiki-meta">
          <span>最后编辑: {{ formatTime(wiki.updatedAt) }}</span>
          <span>编辑者: {{ wiki.lastEditorName }}</span>
        </div>
      </div>

      <div class="wiki-body">
        <div class="wiki-summary">{{ wiki.summary }}</div>
        <div class="wiki-content-text">{{ wiki.content }}</div>
      </div>

      <div class="wiki-actions">
        <el-button type="primary" @click="editWiki">编辑此页</el-button>
        <el-button @click="viewHistory">查看历史</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { wikiApi } from '@/api/wiki'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const wiki = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const response = await wikiApi.getWikiBySlug(route.params.slug)
    wiki.value = response.data
  } catch (error) {
    console.error('Failed to load wiki:', error)
  } finally {
    loading.value = false
  }
})

const formatTime = (time) => {
  if (!time) return ''
  return dayjs(time).format('YYYY年M月D日 HH:mm')
}

const editWiki = () => {
  router.push(`/wiki/edit/${wiki.value.id}`)
}

const viewHistory = () => {
  // TODO: 实现历史版本查看
  console.log('View history')
}
</script>

<style scoped>
.wiki-detail-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.loading, .not-found {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.wiki-header {
  margin-bottom: 30px;
  border-bottom: 2px solid #f0f0f0;
  padding-bottom: 20px;
}

.wiki-header h1 {
  margin: 0 0 10px 0;
  color: #333;
}

.wiki-meta {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #999;
}

.wiki-summary {
  padding: 16px;
  background-color: #f9f9f9;
  border-left: 4px solid #409eff;
  margin-bottom: 20px;
  border-radius: 4px;
  color: #666;
  line-height: 1.8;
}

.wiki-content-text {
  line-height: 1.8;
  color: #555;
  white-space: pre-wrap;
  word-break: break-word;
}

.wiki-actions {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 12px;
}
</style>
