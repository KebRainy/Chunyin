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
          <h4>目录</h4>
          <ul>
            <li v-for="(item, index) in tocItems" :key="index">
              <span>{{ item }}</span>
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
            <h1>{{ wiki.title }}</h1>
            <p class="meta">
              最后更新 {{ formatTime(wiki.updatedAt) }} · {{ wiki.lastEditorName || '系统' }}
            </p>
          </div>
          <el-tag type="success" v-if="wiki.status === 'PUBLISHED'">已发布</el-tag>
        </header>

        <section class="wiki-summary" v-if="wiki.summary">
          <h4>概要</h4>
          <p>{{ wiki.summary }}</p>
        </section>

        <article class="wiki-article" v-html="formatContent(wiki.content)"></article>

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
                <p class="history-summary">{{ item.summary }}</p>
              </div>
              <span class="history-time">{{ formatTime(item.createdAt) }}</span>
            </div>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { fetchWikiPage, fetchWikiHistory, favoriteWikiPage, fetchWikiDiscussions, createWikiDiscussion } from '@/api/wiki'
import { recordFootprint } from '@/api/user'
import { useUserStore } from '@/store/modules/user'
import { ElMessage } from 'element-plus'

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

const tocItems = computed(() => {
  if (!wiki.value?.content) return []
  return wiki.value.content.split('\n')
    .filter(line => line.startsWith('## '))
    .map(line => line.replace(/#+/, '').trim())
})

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

const formatContent = (content) => {
  if (!content) return '<p class="empty">这个条目暂无内容</p>'
  return content.replace(/\n/g, '<br/>')
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

onMounted(() => {
  loadWiki()
})
</script>

<style scoped>
.wiki-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
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
  background: #fff;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(31, 45, 61, 0.08);
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
  background: #fff;
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 8px 24px rgba(31, 45, 61, 0.08);
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
  background: #f4f6fb;
  border-radius: 16px;
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
  padding: 12px;
  border: 1px solid #f0f2f5;
  border-radius: 12px;
  background-color: #fafbfc;
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
  padding: 12px;
  border: 1px solid #f2f2f2;
  border-radius: 12px;
}

.history-editor {
  margin: 0;
  font-weight: 600;
}

.history-summary {
  margin: 4px 0 0;
  color: #606266;
}

.history-time {
  color: #909399;
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
