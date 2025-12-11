<template>
  <div class="home-container">
    <!-- 顶部：发布动态 + 每日一题 -->
    <div class="top-section">
      <div class="share-section">
        <template v-if="userStore.isLoggedIn">
          <div v-if="!shareEditorExpanded" class="share-hint" @click="shareEditorExpanded = true">
            <el-avatar
              :src="userStore.userInfo?.avatarUrl"
              :size="44"
              :alt="`${userStore.userInfo?.username || '用户'}的头像`"
            />
            <div class="hint-text">
              <p>此刻在喝什么？</p>
              <span>点击打开编辑器，支持图文/标签/IP属地</span>
            </div>
            <el-button text type="primary">立即分享</el-button>
          </div>
          <el-card v-else class="share-editor-card">
            <ShareComposer ref="inlineComposerRef" mode="inline" @submitted="handleInlinePosted" />
            <div class="share-editor-actions">
              <el-button text size="small" @click="foldShareEditor">收起</el-button>
            </div>
          </el-card>
        </template>
        <div v-else class="login-prompt">
          <p>登录后可以分享图文、参加活动</p>
          <el-button type="primary" @click="goLogin">登录</el-button>
        </div>
      </div>

      <!-- 右侧：每日一题 -->
      <div class="daily-question-section">
        <el-card class="question-card">
          <template #header>
            <div class="question-header">
              <span>🎯 每日一题</span>
              <span class="question-date">{{ todayDate }}</span>
            </div>
          </template>

          <div v-if="questionLoading" class="question-content">
            <el-skeleton :rows="4" animated />
          </div>
          <div v-else-if="!dailyQuestion" class="question-content">
            <el-empty description="暂未发布今日题目" />
          </div>
          <div v-else class="question-content">
            <h4>{{ dailyQuestion.question }}</h4>
            <div class="options">
              <div
                v-for="option in dailyQuestion.options"
                :key="option.index"
                :class="['option', getOptionClass(option.index), { disabled: submittingAnswer }]"
                @click="selectOption(option.index)"
              >
                <span class="option-text">{{ option.label }}. {{ option.text }}</span>
                <div v-if="answered" class="option-stats">
                  <div class="progress-bar" :style="{ width: option.percentage + '%' }"></div>
                  <span class="option-count">{{ formatCount(option.count) }}</span>
                </div>
              </div>
            </div>

            <div v-if="answered" class="answer-result">
              <span v-if="selectedOption === dailyQuestion.correctOption" class="correct">
                ✓ 回答正确！
              </span>
              <span v-else class="wrong">
                ✗ 回答错误，正确答案是：{{ dailyQuestion.options[dailyQuestion.correctOption].text }}
              </span>
            </div>
            <div v-if="answered && dailyQuestion.explanation" class="answer-explanation">
              <p>{{ dailyQuestion.explanation }}</p>
              <el-button
                v-if="dailyQuestion.wikiLink"
                size="small"
                type="primary"
                link
                @click="goWiki(dailyQuestion.wikiLink)"
              >
                查看相关词条
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 主体：Pinterest 风格的卡片网格 -->
    <div class="posts-container">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="posts.length === 0" class="empty">
        <el-empty description="暂无动态" />
      </div>
      <div v-else class="masonry-grid">
        <PostCard
          v-for="post in posts"
          :key="post.id"
          :post="post"
          @select="goToPost(post.id)"
        />
      </div>

      <!-- 加载更多 -->
      <div v-if="!loading && hasMore" class="load-more">
        <el-button @click="loadMore">加载更多</el-button>
      </div>
      <div v-if="!hasMore && posts.length > 0" class="no-more">
        已加载全部内容
      </div>
    </div>
  </div>

  <ShareModal v-model="shareModalVisible" @posted="refreshPosts" />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { circleApi } from '@/api/circle'
import { fetchTodayQuestion, answerDailyQuestion } from '@/api/dailyQuestion'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import ShareModal from '@/components/ShareModal.vue'
import ShareComposer from '@/components/share/ShareComposer.vue'
import PostCard from '@/components/PostCard.vue'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()
const userStore = useUserStore()

// 动态列表
const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const hasMore = ref(true)
const shareModalVisible = ref(false)
const shareEditorExpanded = ref(false)
const inlineComposerRef = ref(null)

// 每日一题
const dailyQuestion = ref(null)
const questionLoading = ref(false)
const submittingAnswer = ref(false)
const todayDate = computed(() => dayjs().format('M月D日'))
const answered = computed(() => dailyQuestion.value?.answered || false)
const selectedOption = computed(() => dailyQuestion.value?.selectedOption ?? null)

const loadPosts = async () => {
  loading.value = true
  try {
    const response = await circleApi.listPosts(currentPage.value, pageSize.value)
    const pageData = response.data
    const items = pageData.items || []

    if (currentPage.value === 1) {
      posts.value = items
    } else {
      posts.value.push(...items)
    }
    hasMore.value = items.length >= pageSize.value
  } catch (error) {
    console.error('Failed to load posts:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  currentPage.value++
  loadPosts()
}

const refreshPosts = () => {
  currentPage.value = 1
  hasMore.value = true
  loadPosts()
}

const handleInlinePosted = () => {
  shareEditorExpanded.value = false
  refreshPosts()
}

const foldShareEditor = () => {
  shareEditorExpanded.value = false
  inlineComposerRef.value?.resetForm()
}

const loadDailyQuestion = async () => {
  questionLoading.value = true
  try {
    const res = await fetchTodayQuestion()
    dailyQuestion.value = res.data
  } catch (error) {
    dailyQuestion.value = null
  } finally {
    questionLoading.value = false
  }
}

const goToPost = (postId) => {
  router.push(`/posts/${postId}`)
}

const goLogin = () => {
  router.push('/login')
}

const selectOption = async (idx) => {
  if (!dailyQuestion.value || dailyQuestion.value.answered) return
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  submittingAnswer.value = true
  try {
    const res = await answerDailyQuestion(dailyQuestion.value.id, idx)
    dailyQuestion.value = res.data
    ElMessage.success('已记录你的选择')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '提交失败')
  } finally {
    submittingAnswer.value = false
  }
}

const getOptionClass = (idx) => {
  if (!dailyQuestion.value || !dailyQuestion.value.answered) return 'clickable'

  if (idx === dailyQuestion.value.correctOption) {
    return 'correct'
  }
  if (idx === dailyQuestion.value.selectedOption && idx !== dailyQuestion.value.correctOption) {
    return 'wrong'
  }
  return 'other'
}

const formatCount = (count) => {
  if (count > 10000) {
    return (count / 10000).toFixed(1) + '万'
  }
  return count.toString()
}

const formatTime = (time) => {
  if (!time) return ''
  const date = dayjs(time)
  const now = dayjs()
  const diff = now.diff(date, 'minute')

  if (diff < 1) return '刚刚'
  if (diff < 60) return `${diff}分钟前`
  if (diff < 1440) return `${Math.floor(diff / 60)}小时前`
  if (date.isSame(now, 'year')) return date.format('M月D日')
  return date.format('YYYY年M月D日')
}

const truncateText = (text, length) => {
  return text && text.length > length ? text.substring(0, length) + '...' : text
}

const goWiki = (link) => {
  if (!link) return
  if (link.startsWith('http')) {
    window.open(link, '_blank')
    return
  }
  const normalized = link.startsWith('/') ? link : `/wiki/${link}`
  router.push(normalized)
}

onMounted(() => {
  loadPosts()
  loadDailyQuestion()
})
</script>

<style scoped>
.home-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

/* ============ 顶部区域 ============ */
.top-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 30px;
}

.share-section {
  min-height: 120px;
}

.share-hint {
  display: flex;
  align-items: center;
  gap: 14px;
  background: linear-gradient(120deg, #fdf2ff, #eef6ff);
  border-radius: 18px;
  padding: 18px;
  cursor: pointer;
  border: 1px solid #ebeef5;
}

.share-hint .hint-text p {
  margin: 0;
  font-weight: 600;
  color: #303133;
}

.share-hint .hint-text span {
  font-size: 12px;
  color: #909399;
}

.share-editor-card {
  padding: 18px;
}

.share-editor-actions {
  text-align: right;
  margin-top: 8px;
}

.login-prompt {
  text-align: center;
  padding: 20px;
}

.login-prompt p {
  color: #606266;
  margin-bottom: 12px;
}

.daily-question-section {
  min-height: 80px;
}

.question-card {
  height: 100%;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.question-date {
  font-size: 12px;
  color: #999;
}

.question-content h4 {
  margin: 0 0 16px 0;
  color: #333;
  font-size: 14px;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.option {
  padding: 10px 12px;
  background-color: #f5f5f5;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.option.clickable:hover {
  background-color: #e8e8e8;
}

.option.correct {
  background-color: #f0f9ff;
  border: 1px solid #85ce61;
}

.option.wrong {
  background-color: #fef0f0;
  border: 1px solid #f56c6c;
}

.option.other {
  background-color: #f5f5f5;
  opacity: 0.6;
}

.option.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.option-text {
  flex: 1;
  font-weight: 500;
}

.option-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 150px;
}

.progress-bar {
  height: 4px;
  background-color: currentColor;
  border-radius: 2px;
  opacity: 0.3;
}

.option-count {
  font-size: 11px;
  color: #999;
  white-space: nowrap;
}

.answer-result {
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.answer-result .correct {
  color: #85ce61;
}

.answer-result .wrong {
  color: #f56c6c;
}

.answer-explanation {
  margin-top: 12px;
  padding: 10px 12px;
  background-color: #f5f7fa;
  border-radius: 8px;
  font-size: 13px;
  color: #606266;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* ============ 主体区域 ============ */
.posts-container {
  min-height: 400px;
}

.loading {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty {
  display: flex;
  justify-content: center;
  padding: 60px 20px;
}

.masonry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 18px;
}

/* ============ 加载更多 ============ */
.load-more {
  text-align: center;
  padding: 20px;
  margin-top: 20px;
}

.no-more {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 12px;
}

/* ============ 响应式 ============ */
@media (max-width: 1024px) {
  .top-section {
    grid-template-columns: 1fr;
  }

  .masonry-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  }
}

@media (max-width: 768px) {
  .masonry-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  }
}
</style>
