<template>
  <div class="post-detail-container">
    <div v-if="loading" class="loading">
      <el-skeleton :rows="5" animated />
    </div>
    <div v-else-if="!post" class="not-found">
      <el-empty description="动态不存在或已被删除" />
    </div>
    <div v-else class="post-detail">
      <!-- 动态头部 -->
      <div class="post-header">
        <el-avatar
          :src="post.author.avatarUrl"
          :size="48"
          class="avatar"
          @click="goToUser(post.author.id)"
        />
        <div class="author-info">
          <div class="username" @click="goToUser(post.author.id)">
            {{ post.author.username }}
          </div>
          <div class="post-meta">
            <span class="time">{{ formatTime(post.createdAt) }}</span>
            <span v-if="post.location" class="location">📍 {{ post.location }}</span>
            <span v-if="post.ipAddressMasked" class="ip">{{ post.ipAddressMasked }}</span>
          </div>
        </div>
      </div>

      <!-- 动态内容 -->
      <div class="post-body">
        <div class="post-content">{{ post.content }}</div>

        <!-- 动态图片 -->
        <div v-if="post.imageUrls && post.imageUrls.length > 0" class="post-images">
          <img
            v-for="(url, idx) in post.imageUrls"
            :key="idx"
            :src="url"
            :alt="`image-${idx}`"
            class="post-image"
          />
        </div>

        <!-- 动态标签 -->
        <div v-if="post.location" class="post-tags">
          <el-tag>{{ post.location }}</el-tag>
        </div>
      </div>

      <!-- 交互按钮 -->
      <div class="post-actions">
        <div class="action-item">
          <el-icon><View /></el-icon>
          <span>浏览 {{ viewCount }}</span>
        </div>
        <div class="action-item" @click="toggleLike">
          <el-icon :class="{ liked: liked }"><Heart /></el-icon>
          <span>点赞 {{ likeCount }}</span>
        </div>
        <div class="action-item">
          <el-icon><ChatDotSquare /></el-icon>
          <span>评论 {{ commentCount }}</span>
        </div>
        <div class="action-item" @click="toggleStar">
          <el-icon :class="{ starred: starred }"><StarFilled /></el-icon>
          <span>收藏</span>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="comments-section">
        <h3>评论</h3>
        <div v-if="userStore.isLoggedIn" class="comment-input">
          <el-input
            v-model="commentText"
            type="textarea"
            placeholder="写下你的评论..."
            rows="3"
          />
          <el-button type="primary" @click="submitComment" :loading="commentSubmitting">
            发布评论
          </el-button>
        </div>
        <div v-else class="login-prompt">
          <el-empty description="请登录后发表评论" />
        </div>
        <div class="comments-list">
          <div v-if="comments.length === 0" class="no-comments">
            暂无评论
          </div>
          <div v-else>
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <el-avatar :src="comment.author?.avatarUrl" :size="32" />
              <div class="comment-content">
                <div class="comment-header">
                  <span class="comment-author">{{ comment.author?.username }}</span>
                  <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
                </div>
                <div class="comment-text">{{ comment.content }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  View,
  Heart,
  ChatDotSquare,
  StarFilled
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import { circleApi } from '@/api/circle'
import { useUserStore } from '@/store/modules/user'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const post = ref(null)
const loading = ref(true)
const liked = ref(false)
const starred = ref(false)
const likeCount = ref(0)
const viewCount = ref(0)
const commentCount = ref(0)
const commentText = ref('')
const commentSubmitting = ref(false)
const comments = ref([])

const loadPost = async () => {
  loading.value = true
  try {
    const response = await circleApi.getPost(route.params.id)
    post.value = response.data
    viewCount.value = 0
    commentCount.value = 0
  } catch (error) {
    console.error('Failed to load post:', error)
    ElMessage.error('加载动态失败')
  } finally {
    loading.value = false
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = dayjs(time)
  const now = dayjs()
  const diff = now.diff(date, 'minute')

  if (diff < 1) return '刚刚'
  if (diff < 60) return `${diff}分钟前`
  if (diff < 1440) return `${Math.floor(diff / 60)}小时前`
  if (date.isSame(now, 'year')) return date.format('M月D日 HH:mm')
  return date.format('YYYY年M月D日 HH:mm')
}

const goToUser = (userId) => {
  router.push(`/users/${userId}`)
}

const toggleLike = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  liked.value = !liked.value
  if (liked.value) likeCount.value++
  else likeCount.value--
}

const toggleStar = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  starred.value = !starred.value
}

const submitComment = async () => {
  if (!commentText.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  commentSubmitting.value = true
  try {
    // TODO: 实现发布评论 API
    // await commentApi.createComment({
    //   postId: post.value.id,
    //   content: commentText.value
    // })
    ElMessage.success('评论发布成功')
    commentText.value = ''
    // await loadPost()
  } catch (error) {
    ElMessage.error('评论发布失败')
  } finally {
    commentSubmitting.value = false
  }
}

onMounted(() => {
  loadPost()
})
</script>

<style scoped>
.post-detail-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.loading, .not-found {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.post-detail {
  background-color: #fff;
  border-radius: 8px;
  padding: 24px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* 头部 */
.post-header {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.avatar {
  cursor: pointer;
  transition: transform 0.3s;
}

.avatar:hover {
  transform: scale(1.1);
}

.author-info {
  flex: 1;
}

.username {
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  cursor: pointer;
  transition: color 0.3s;
}

.username:hover {
  color: #409eff;
}

.post-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #999;
  flex-wrap: wrap;
}

/* 内容 */
.post-body {
  margin-bottom: 20px;
}

.post-content {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  margin-bottom: 16px;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.post-image {
  width: 100%;
  height: auto;
  border-radius: 8px;
  max-height: 500px;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.3s;
}

.post-image:hover {
  transform: scale(1.02);
}

.post-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

/* 交互按钮 */
.post-actions {
  display: flex;
  gap: 24px;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #666;
  transition: color 0.3s;
  font-size: 14px;
}

.action-item:hover {
  color: #409eff;
}

.action-item .el-icon {
  font-size: 18px;
}

.action-item .el-icon.liked {
  color: #f56c6c;
}

.action-item .el-icon.starred {
  color: #ffd666;
}

/* 评论区 */
.comments-section {
  margin-top: 20px;
}

.comments-section h3 {
  margin-bottom: 16px;
  color: #333;
  font-size: 16px;
}

.comment-input {
  margin-bottom: 20px;
}

:deep(.comment-input .el-textarea__inner) {
  margin-bottom: 12px;
}

.login-prompt {
  margin: 20px 0;
}

.comments-list {
  margin-top: 16px;
}

.no-comments {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 14px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-content {
  flex: 1;
  word-break: break-word;
}

.comment-header {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 4px;
}

.comment-author {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-text {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

/* 响应式 */
@media (max-width: 600px) {
  .post-detail-container {
    padding: 12px;
  }

  .post-detail {
    padding: 16px;
  }

  .post-images {
    grid-template-columns: 1fr;
  }

  .post-actions {
    gap: 16px;
  }
}
</style>

