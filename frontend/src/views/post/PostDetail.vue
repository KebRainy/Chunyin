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
          :alt="`${post.author.username}的头像`"
          @click="goToUser(post.author.id)"
        />
        <div class="author-info">
          <div class="username" @click="goToUser(post.author.id)">
            {{ post.author.username }}
          </div>
          <div class="post-meta">
            <span class="time">{{ formatTime(post.createdAt) }}</span>
            <span v-if="post.location" class="location">📍 {{ post.location }}</span>
            <span v-if="post.ipRegion" class="ip">IP 属地 {{ post.ipRegion }}</span>
            <span v-else-if="post.ipAddressMasked" class="ip">{{ post.ipAddressMasked }}</span>
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
            :alt="`${post.author?.username || '用户'}的分享图片${idx + 1}`"
            class="post-image"
          />
        </div>

        <!-- 动态标签 -->
        <div v-if="post.tags?.length" class="post-tags">
          <el-tag v-for="tag in post.tags" :key="tag" effect="dark">
            # {{ tag }}
          </el-tag>
        </div>
      </div>

      <!-- 交互按钮 -->
      <div class="post-actions">
        <div class="action-item">
          <el-icon><View /></el-icon>
          <span>浏览 {{ post.viewCount || 0 }}</span>
        </div>
        <div class="action-item" :class="{ active: liked }" @click="toggleLike">
          <el-icon><GobletFull /></el-icon>
          <span>{{ liked ? '已赞' : '点赞' }} {{ post.likeCount || 0 }}</span>
        </div>
        <div class="action-item">
          <el-icon><ChatDotSquare /></el-icon>
          <span>评论 {{ post.commentCount || 0 }}</span>
        </div>
        <div class="action-item" :class="{ active: favorited }" @click="toggleFavorite">
          <el-icon><Star /></el-icon>
          <span>{{ favorited ? '已收藏' : '收藏' }} {{ post.favoriteCount || 0 }}</span>
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
          <div v-if="replyTarget" class="reply-target">
            回复 @{{ replyTarget.author?.username }}
            <el-button text size="small" @click="replyTarget = null">取消</el-button>
          </div>
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
              <el-avatar
                :src="comment.author?.avatarUrl"
                :size="32"
                :alt="`${comment.author?.username || '用户'}的头像`"
              />
              <div class="comment-content">
                <div class="comment-header">
                  <span class="comment-author">{{ comment.author?.username }}</span>
                  <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
                </div>
                <div class="comment-text">{{ comment.content }}</div>
                <div class="comment-actions">
                  <el-button text size="small" @click="setReplyTarget(comment)">回复</el-button>
                </div>
                <div class="reply-list" v-if="comment.replies?.length">
                  <div
                    v-for="reply in comment.replies"
                    :key="reply.id"
                    class="reply-item"
                  >
                    <el-avatar
                      :src="reply.author?.avatarUrl"
                      :size="24"
                      :alt="`${reply.author?.username || '用户'}的头像`"
                    />
                    <div>
                      <div class="comment-header">
                        <span class="comment-author">{{ reply.author?.username }}</span>
                        <span class="comment-time">{{ formatTime(reply.createdAt) }}</span>
                      </div>
                      <div class="comment-text">{{ reply.content }}</div>
                      <el-button text size="small" @click="setReplyTarget(reply)">回复</el-button>
                    </div>
                  </div>
                </div>
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
  GobletFull,
  Heart,
  ChatDotSquare,
  Star
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import { circleApi } from '@/api/circle'
import { recordFootprint } from '@/api/user'
import { useUserStore } from '@/store/modules/user'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const post = ref(null)
const loading = ref(true)
const liked = ref(false)
const favorited = ref(false)
const commentText = ref('')
const commentSubmitting = ref(false)
const comments = ref([])
const replyTarget = ref(null)

const loadPost = async () => {
  loading.value = true
  try {
    const response = await circleApi.getPost(route.params.id)
    post.value = response.data
    liked.value = response.data?.liked || false
    favorited.value = response.data?.favorited || false
    await loadComments()
    recordPostFootprint()
  } catch (error) {
    ElMessage.error('加载动态失败')
  } finally {
    loading.value = false
  }
}

const loadComments = async () => {
  try {
    const res = await circleApi.getComments(route.params.id)
    comments.value = res.data || []
  } catch (error) {
    ElMessage.error('加载评论失败')
  }
}

const recordPostFootprint = async () => {
  if (!userStore.isLoggedIn || !post.value) return
  try {
    await recordFootprint({
      targetType: 'POST',
      targetId: post.value.id,
      title: post.value.author?.username || '动态',
      summary: post.value.content?.slice(0, 80),
      coverUrl: post.value.imageUrls?.[0]
    })
  } catch (error) {
    // ignore
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = dayjs(time)
  const now = dayjs()
  const diffMinutes = now.diff(date, 'minute')
  if (diffMinutes < 60) return `${Math.max(1, diffMinutes)}分钟前`
  const diffHours = now.diff(date, 'hour')
  if (diffHours < 24) return `${diffHours}小时前`
  if (date.isSame(now, 'year')) return date.format('M月D日 HH:mm')
  return date.format('YYYY年M月D日 HH:mm')
}

const goToUser = (userId) => {
  router.push(`/users/${userId}`)
}

const toggleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await circleApi.likePost(post.value.id)
    liked.value = res.data
    post.value.likeCount = Math.max(0, (post.value.likeCount || 0) + (liked.value ? 1 : -1))
  } catch (error) {
    ElMessage.error('操作失败，请稍后再试')
  }
}

const toggleFavorite = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await circleApi.favoritePost(post.value.id)
    favorited.value = res.data
    post.value.favoriteCount = Math.max(0, (post.value.favoriteCount || 0) + (favorited.value ? 1 : -1))
  } catch (error) {
    ElMessage.error('操作失败，请稍后再试')
  }
}

const submitComment = async () => {
  if (!commentText.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  commentSubmitting.value = true
  try {
    await circleApi.createComment(post.value.id, {
      content: commentText.value,
      parentId: replyTarget.value?.id || null
    })
    commentText.value = ''
    replyTarget.value = null
    await loadComments()
    post.value.commentCount += 1
    ElMessage.success('评论发布成功')
  } catch (error) {
    ElMessage.error('评论发布失败')
  } finally {
    commentSubmitting.value = false
  }
}

const setReplyTarget = (comment) => {
  replyTarget.value = comment
  commentText.value = `@${comment.author?.username} `
}

onMounted(() => {
  loadPost()
})
</script>

<style scoped>
.post-detail-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px 0 60px;
}

.loading, .not-found {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.post-detail {
  background-color: #fff;
  border-radius: 32px;
  padding: 32px;
  border: 1px solid #eceff5;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
}

/* 头部 */
.post-header {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 20px;
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
  margin-bottom: 24px;
}

.post-content {
  font-size: 18px;
  line-height: 1.8;
  color: #1f2d3d;
  margin-bottom: 20px;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.post-image {
  width: 100%;
  height: auto;
  border-radius: 20px;
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
  padding: 18px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 24px;
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

.action-item:hover,
.action-item.active {
  color: #409eff;
}

.action-item .el-icon {
  font-size: 18px;
}

/* 评论区 */
.comments-section {
  margin-top: 20px;
}

.comments-section h3 {
  margin-bottom: 16px;
  color: #1f2d3d;
  font-size: 18px;
}

.comment-input {
  margin-bottom: 20px;
}

:deep(.comment-input .el-textarea__inner) {
  margin-bottom: 12px;
}

.reply-target {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
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
  padding: 16px 0;
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

.comment-actions {
  margin-top: 6px;
}

.reply-list {
  margin-top: 12px;
  padding-left: 40px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reply-item {
  display: flex;
  gap: 8px;
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
