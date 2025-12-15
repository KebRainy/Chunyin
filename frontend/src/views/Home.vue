<template>
  <div class="home-container">
    <!-- 顶部：发布动态 + 每日一题 -->
    <div class="top-section">
      <div class="share-section">
        <el-card v-if="userStore.isLoggedIn" class="share-editor-card equal-card">
          <div class="share-editor-header">
            <div>
              <p class="eyebrow">此刻发布</p>
              <h3>分享你的灵感</h3>
              <span>支持图文、标签、地点，一次完成</span>
            </div>
          </div>
          <ShareComposer mode="inline" @submitted="handleInlinePosted" />
        </el-card>
        <div v-else class="login-prompt equal-card">
          <p>登录后即可分享图文、参加活动</p>
          <el-button type="primary" @click="goLogin">登录</el-button>
        </div>
      </div>

      <!-- 右侧：每日一题 -->
      <div class="daily-question-section">
        <el-card class="question-card equal-card">
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
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { circleApi } from '@/api/circle'
import { fetchTodayQuestion, answerDailyQuestion } from '@/api/dailyQuestion'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import ShareComposer from '@/components/share/ShareComposer.vue'
import PostCard from '@/components/PostCard.vue'

dayjs.locale('zh-cn')

const router = useRouter()
const userStore = useUserStore()

// 动态列表
const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const hasMore = ref(true)

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
  refreshPosts()
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
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.65fr);
  gap: 24px;
  margin-bottom: 32px;
  align-items: stretch;
}

.equal-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.share-editor-card {
  border-radius: 24px;
  border: 1px solid #eceff5;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.05);
}

.share-editor-card :deep(.el-card__body),
.question-card :deep(.el-card__body) {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
}

.share-editor-header {
  margin-bottom: 16px;
}

.share-editor-header .eyebrow {
  font-size: 12px;
  letter-spacing: 2px;
  color: #909399;
  text-transform: uppercase;
  margin: 0 0 4px;
}

.share-editor-header h3 {
  margin: 0;
  font-size: 20px;
  color: #1f2d3d;
}

.share-editor-header span {
  color: #909399;
  font-size: 13px;
}

.login-prompt {
  border-radius: 24px;
  border: 1px solid #eceff5;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.4);
  padding: 24px;
  text-align: center;
  justify-content: center;
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
}

.login-prompt p {
  color: #303133;
  margin-bottom: 16px;
  font-size: 16px;
}

.daily-question-section {
  min-height: 80px;
}

.question-card {
  border-radius: 24px;
  border: 1px solid #eceff5;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
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
  padding: 12px 14px;
  background-color: #f8f9fb;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  border: 1px solid transparent;
}

.option.clickable:hover {
  border-color: #e0e7ff;
}

.option.correct {
  background-color: #f0f9ff;
  border-color: #85ce61;
}

.option.wrong {
  background-color: #fef0f0;
  border-color: #f56c6c;
}

.option.other {
  background-color: #f8f9fb;
  color: #c0c4cc;
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
  background: #fff;
  border-radius: 28px;
  border: 1px solid #eceff5;
  padding: 24px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.04);
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
  column-count: 4;
  column-gap: 20px;
}

.masonry-grid > * {
  break-inside: avoid;
  display: block;
  margin-bottom: 20px;
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
    column-count: 3;
  }
}

@media (max-width: 768px) {
  .masonry-grid {
    column-count: 2;
  }
}

@media (max-width: 520px) {
  .masonry-grid {
    column-count: 1;
  }
}
</style>
