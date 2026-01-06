<template>
  <div class="wiki-detail-page">
    <div v-if="loading" class="loading">
      <el-skeleton :rows="6" animated />
    </div>
    <div v-else-if="!wiki" class="not-found">
      <el-empty description="词条不存在或已被删除" />
    </div>
    <div v-else class="wiki-layout">
      <aside class="wiki-sidebar">
        <div class="cover">
          <img src="https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=600" alt="concept">
        </div>
        <div class="toc">
          <h4 class="art-heading-h4">目录</h4>
          <ul>
            <li v-for="(item, index) in tocItems" :key="index">
              <span>{{ item.text }}</span>
            </li>
          </ul>
        </div>
        <div class="sidebar-actions">
          <el-button type="primary" plain size="small" @click="favoritePage">
            {{ wiki.favorited ? '已收藏' : '收藏词条' }}
          </el-button>
          <el-button v-if="userStore.isLoggedIn && canEdit" size="small" @click="editWiki">编辑条目</el-button>
        </div>
      </aside>

      <main class="wiki-main">
        <header class="wiki-header">
          <div>
            <p class="eyebrow">词条</p>
            <h1 class="art-heading">{{ wiki.title }}</h1>
            <p class="meta">
              最后更新 {{ formatTime(wiki.updatedAt) }} · {{ wiki.lastEditorName || '系统' }}
            </p>
          </div>
          <el-tag type="success" v-if="wiki.status === 'PUBLISHED'">已发布</el-tag>
        </header>

        <el-tabs v-model="activeTab" class="wiki-tabs">
          <el-tab-pane label="条目信息" name="overview">
            <section class="wiki-summary" v-if="wiki.summary">
              <h4 class="art-heading-h4">概要</h4>
              <p>{{ wiki.summary }}</p>
            </section>

            <article class="wiki-article" v-html="renderedContent"></article>
          </el-tab-pane>

          <el-tab-pane label="用户讨论" name="discussion">
            <section class="wiki-discussion">
              <div class="section-header">
                <h3>讨论</h3>
                <el-button text size="small" @click="focusDiscussionInput">发起讨论</el-button>
              </div>
              <div v-if="discussionLoading" class="discussion-loading">
                <el-skeleton :rows="3" animated />
              </div>
              <div v-else>
                <div v-if="userStore.isLoggedIn" class="discussion-form">
                  <el-input
                    ref="discussionInputRef"
                    v-model="discussionContent"
                    type="textarea"
                    placeholder="写下你的问题或观点..."
                    :rows="3"
                    maxlength="1000"
                    show-word-limit
                  />
                  <div class="discussion-actions">
                    <el-button
                      type="primary"
                      size="small"
                      :loading="submittingDiscussion"
                      @click="submitDiscussion"
                    >
                      发布讨论
                    </el-button>
                  </div>
                </div>
                <div v-else class="discussion-login">
                  <el-button type="text" @click="goLogin">登录后参与讨论</el-button>
                </div>
                <div v-if="discussions.length === 0" class="discussion-empty">
                  <el-empty description="暂时还没有讨论，快来参与吧" />
                </div>
                <div v-else class="discussion-list">
                  <div v-for="item in discussions" :key="item.id" class="discussion-item">
                    <el-avatar :src="item.author?.avatarUrl" :size="36" />
                    <div class="discussion-body">
                      <div class="discussion-meta">
                        <span class="author">{{ item.author?.username || '匿名用户' }}</span>
                        <span class="time">{{ formatTime(item.createdAt) }}</span>
                      </div>
                      <p class="discussion-text">{{ item.content }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          </el-tab-pane>

          <el-tab-pane label="历史版本" name="history">
            <section class="wiki-history">
              <div class="section-header">
                <h3>历史版本</h3>
              </div>
              <div v-if="history.length === 0" class="history-empty">
                <el-empty description="暂无历史记录" />
              </div>
              <div v-else class="history-list">
                <div v-for="item in history" :key="item.id" class="history-item">
                  <div>
                    <p class="history-editor">{{ item.editorName }}</p>
                    <p class="history-summary">{{ item.summary || '更新了词条内容' }}</p>
                  </div>
                  <div class="history-meta">
                    <span class="history-time">{{ formatTime(item.createdAt) }}</span>
                    <div class="history-actions">
                      <el-button size="small" text @click="viewRevision(item)">查看内容</el-button>
                      <el-button
                        v-if="userStore.isLoggedIn"
                        size="small"
                        type="primary"
                        plain
                        :loading="restoringRevision && restoringRevisionId === item.id"
                        @click="restoreRevisionVersion(item)"
                      >
                        恢复
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          </el-tab-pane>
        </el-tabs>
      </main>
    </div>

    <el-dialog
      v-model="revisionDialogVisible"
      class="revision-dialog"
      width="820px"
      :title="currentRevision ? `版本详情 · ${currentRevision.editorName}` : '版本详情'"
    >
      <div v-if="revisionLoading" class="revision-loading">
        <el-skeleton :rows="6" animated />
      </div>
      <div v-else-if="currentRevision" class="revision-wrapper">
        <p class="revision-meta">
          更新于 {{ formatTime(currentRevision.createdAt) }}
          <span v-if="currentRevision.summary">· {{ currentRevision.summary }}</span>
        </p>
        <div class="revision-preview" v-html="revisionPreviewHtml"></div>
        <div class="revision-actions" v-if="userStore.isLoggedIn">
          <el-button
            type="primary"
            plain
            :loading="restoringRevision"
            @click="restoreRevisionVersion(currentRevision)"
          >
            恢复为当前版本
          </el-button>
        </div>
      </div>
      <div v-else class="revision-empty">
        <el-empty description="无法加载该版本内容" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { fetchWikiPage, fetchWikiHistory, favoriteWikiPage, fetchWikiDiscussions, createWikiDiscussion, fetchWikiRevisionDetail, restoreWikiRevision } from '@/api/wiki'
import { recordFootprint } from '@/api/user'
import { useUserStore } from '@/store/modules/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const wiki = ref(null)
const history = ref([])
const loading = ref(true)
const discussions = ref([])
const discussionLoading = ref(true)
const discussionContent = ref('')
const submittingDiscussion = ref(false)
const discussionInputRef = ref(null)
const activeTab = ref('overview')
const revisionDialogVisible = ref(false)
const revisionLoading = ref(false)
const currentRevision = ref(null)
const restoringRevision = ref(false)
const restoringRevisionId = ref(null)

const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true
})

const renderedContent = computed(() => {
  if (!wiki.value?.content) {
    return '<p class="empty">这个条目暂无内容</p>'
  }
  return DOMPurify.sanitize(md.render(wiki.value.content))
})

const tocItems = computed(() => {
  if (!wiki.value?.content) return []
  return wiki.value.content.split('\n')
    .filter(line => /^#{1,6}\s+/.test(line))
    .map(line => {
      const level = line.match(/^#+/)[0].length
      return {
        level,
        text: line.replace(/^#+/, '').trim()
      }
    })
})

const canEdit = computed(() => {
  return userStore.isLoggedIn
})

const revisionPreviewHtml = computed(() => {
  if (!currentRevision.value?.content) {
    return '<p class="empty">该版本暂无正文内容</p>'
  }
  return DOMPurify.sanitize(md.render(currentRevision.value.content))
})

const loadWiki = async () => {
  loading.value = true
  try {
    const response = await fetchWikiPage(route.params.slug)
    wiki.value = response.data
    await loadHistory()
    await loadDiscussions()
    recordWikiFootprint()
  } catch (error) {
    ElMessage.error('加载词条失败')
  } finally {
    loading.value = false
  }
}

const loadHistory = async () => {
  try {
    const res = await fetchWikiHistory(route.params.slug)
    history.value = res.data || []
  } catch (error) {
    history.value = []
  }
}

const loadDiscussions = async () => {
  discussionLoading.value = true
  try {
    const res = await fetchWikiDiscussions(route.params.slug)
    discussions.value = res.data || []
  } catch (error) {
    discussions.value = []
  } finally {
    discussionLoading.value = false
  }
}

const recordWikiFootprint = async () => {
  if (!userStore.isLoggedIn || !wiki.value) return
  try {
    await recordFootprint({
      targetType: 'WIKI',
      targetId: wiki.value.id,
      title: wiki.value.title,
      summary: wiki.value.summary
    })
  } catch (error) {
    // ignore
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return dayjs(time).format('YYYY年M月D日 HH:mm')
}

const editWiki = () => {
  if (wiki.value) {
    router.push(`/wiki/edit/${wiki.value.id}`)
  }
}

const favoritePage = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    const res = await favoriteWikiPage(wiki.value.id)
    wiki.value.favorited = res.data
    ElMessage.success(wiki.value.favorited ? '已收藏' : '已取消收藏')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const focusDiscussionInput = () => {
  if (!userStore.isLoggedIn) {
    goLogin()
    return
  }
  discussionInputRef.value?.focus()
}

const submitDiscussion = async () => {
  if (!userStore.isLoggedIn) {
    goLogin()
    return
  }
  if (!discussionContent.value.trim()) {
    ElMessage.warning('请输入讨论内容')
    return
  }
  submittingDiscussion.value = true
  try {
    const res = await createWikiDiscussion(route.params.slug, { content: discussionContent.value.trim() })
    discussions.value.unshift(res.data)
    discussionContent.value = ''
    ElMessage.success('讨论已发布')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '发布失败')
  } finally {
    submittingDiscussion.value = false
  }
}

const goLogin = () => {
  router.push('/login')
}

const viewRevision = async (revision) => {
  revisionDialogVisible.value = true
  revisionLoading.value = true
  currentRevision.value = { ...revision }
  try {
    const res = await fetchWikiRevisionDetail(route.params.slug, revision.id)
    currentRevision.value = res.data
  } catch (error) {
    revisionDialogVisible.value = false
    ElMessage.error(error.response?.data?.message || '加载历史版本失败')
  } finally {
    revisionLoading.value = false
  }
}

const restoreRevisionVersion = async (revision) => {
  if (!userStore.isLoggedIn) {
    goLogin()
    return
  }
  try {
    await ElMessageBox.confirm(
      '确认将当前词条内容恢复到该历史版本吗？该操作会覆盖当前内容。',
      '确认恢复',
      {
        type: 'warning',
        confirmButtonText: '确认恢复',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }
  restoringRevision.value = true
  restoringRevisionId.value = revision.id
  try {
    await restoreWikiRevision(route.params.slug, revision.id)
    ElMessage.success('词条已恢复到所选版本')
    revisionDialogVisible.value = false
    await loadWiki()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '恢复失败')
  } finally {
    restoringRevision.value = false
    restoringRevisionId.value = null
  }
}

onMounted(() => {
  loadWiki()
})

watch(
  () => route.params.slug,
  (newSlug, oldSlug) => {
    if (newSlug && newSlug !== oldSlug) {
      loadWiki()
    }
  }
)
</script>

<style scoped>
.wiki-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 0 60px;
}

.loading,
.not-found {
  padding: 60px 0;
  text-align: center;
}

.wiki-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
}

.wiki-sidebar {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-xl);
  padding: 20px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.cover img {
  width: 100%;
  border-radius: 16px;
  object-fit: cover;
}

.toc h4 {
  margin-bottom: 10px;
}

.toc ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #606266;
}

.sidebar-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.wiki-main {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-xl);
  padding: 32px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.07);
}

.wiki-tabs {
  margin-top: 16px;
}

.wiki-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 2px;
  font-size: 12px;
  color: #909399;
}

.wiki-summary {
  background: #f9fafc;
  border-radius: 18px;
  padding: 16px;
  margin-bottom: 24px;
}

.wiki-summary h4 {
  margin: 0 0 8px;
}

.wiki-article {
  line-height: 1.8;
  font-size: 16px;
  color: #1f2d3d;
  margin-bottom: 30px;
}

.wiki-article :deep(br) {
  content: '';
  display: block;
  margin-bottom: 8px;
}

.wiki-discussion,
.wiki-history {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.discussion-form {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.discussion-actions {
  text-align: right;
}

.discussion-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.discussion-item {
  display: flex;
  gap: 12px;
  padding: 14px;
  border: 1px solid #f0f2f5;
  border-radius: 18px;
  background-color: #fafafa;
}

.discussion-body {
  flex: 1;
}

.discussion-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.discussion-text {
  margin: 0;
  color: #303133;
  line-height: 1.6;
}

.discussion-login {
  margin-bottom: 12px;
  text-align: center;
  color: #909399;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  display: flex;
  justify-content: space-between;
  padding: 14px;
  border: 1px solid #f0f2f5;
  border-radius: 18px;
  gap: 16px;
}

.history-editor {
  margin: 0;
  font-weight: 600;
}

.history-summary {
  margin: 4px 0 0;
  color: #606266;
}

.history-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.history-time {
  color: #909399;
  font-size: 12px;
}

.history-actions {
  display: flex;
  gap: 8px;
}

.revision-loading {
  padding: 12px 0;
}

.revision-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.revision-meta {
  color: #606266;
}

.revision-preview {
  border: 1px solid #f0f2f5;
  border-radius: 16px;
  padding: 16px;
  max-height: 60vh;
  overflow: auto;
}

.revision-preview :deep(img) {
  max-width: 100%;
}

.revision-actions {
  text-align: right;
}

.revision-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}

@media (max-width: 900px) {
  .wiki-layout {
    grid-template-columns: 1fr;
  }

  .wiki-main {
    padding: 20px;
  }
}
</style>
