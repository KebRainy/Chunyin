<template>
  <div class="post-detail-page">
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
        <!-- 更多操作菜单（登录用户可见） -->
        <div v-if="userStore.isLoggedIn" class="post-actions-menu">
          <el-dropdown trigger="click" @command="handlePostCommand">
            <el-button circle size="small">
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="canDeletePost" command="delete" style="color: #f56c6c;">
                  <el-icon><Delete /></el-icon>
                  删除动态
                </el-dropdown-item>
                <el-dropdown-item v-if="!canDeletePost" command="report" style="color: #e6a23c;">
                  <el-icon><WarningFilled /></el-icon>
                  举报动态
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
          <el-tag 
            v-for="(tag, index) in post.tags" 
            :key="tag" 
            effect="dark"
            :type="getTagType(index)"
          >
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
        <!-- 一级评论输入框（只在没有回复目标时显示） -->
        <div v-if="userStore.isLoggedIn && !replyTargetId" class="comment-input">
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
        <div v-else-if="!userStore.isLoggedIn" class="login-prompt">
          <el-empty description="请登录后发表评论" />
        </div>
        <div class="comments-list">
          <div v-if="comments.length === 0" class="no-comments">
            暂无评论
          </div>
          <div v-else>
            <CommentItem
              v-for="comment in comments"
              :key="comment.id"
              :comment="comment"
              :level="0"
              :avatar-size="32"
              :can-delete="canDeleteComment(comment)"
              :can-report="userStore.isLoggedIn && !canDeleteComment(comment)"
              :format-time="formatTime"
              :can-delete-comment="canDeleteComment"
              :can-report-comment="(comment) => userStore.isLoggedIn && !canDeleteComment(comment)"
              :reply-target-id="replyTargetId"
              @reply="setReplyTarget"
              @delete="handleDeleteComment"
              @report="(comment) => openReportDialog('POST_COMMENT', comment.id)"
              @submit-reply="handleSubmitReply"
              @cancel-reply="handleCancelReply"
            />
          </div>
        </div>
      </div>
    </div>
  </div>

    <!-- 举报对话框 -->
    <ReportDialog
      v-model="reportDialogVisible"
      :content-type="reportContentType"
      :content-id="reportContentId"
      @success="handleReportSuccess"
    />

    <!-- 相关推荐侧边栏 -->
    <div v-if="!loading && post" class="similar-posts-sidebar">
      <div class="sidebar-card">
        <h3 class="sidebar-title">
          <el-icon><MagicStick /></el-icon>
          相关推荐
        </h3>
        <div v-if="similarLoading" class="sidebar-loading">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="similarPosts.length === 0" class="sidebar-empty">
          暂无相关推荐
        </div>
        <div v-else class="similar-list">
          <div
            v-for="item in similarPosts"
            :key="item.id"
            class="similar-item"
            @click="goToPost(item.id)"
          >
            <div class="similar-image" v-if="item.imageUrls?.length">
              <img :src="item.imageUrls[0]" :alt="item.content" />
            </div>
            <div class="similar-image no-image" v-else>
              <el-icon><Picture /></el-icon>
            </div>
            <div class="similar-info">
              <p class="similar-content">{{ truncate(item.content, 40) }}</p>
              <div class="similar-meta">
                <span class="similar-author">{{ item.author?.username }}</span>
                <span class="similar-stats">
                  <el-icon><GobletFull /></el-icon>
                  {{ item.likeCount || 0 }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  View,
  GobletFull,
  Heart,
  ChatDotSquare,
  Star,
  MagicStick,
  Picture,
  Delete,
  MoreFilled,
  WarningFilled
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import ReportDialog from '@/components/ReportDialog.vue'
import CommentItem from '@/components/CommentItem.vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import { circleApi } from '@/api/circle'
import { recommendApi } from '@/api/recommend'
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
const replyTargetId = ref(null) // 当前正在回复的评论ID

// 相关推荐
const similarPosts = ref([])
const similarLoading = ref(false)

// 举报相关
const reportDialogVisible = ref(false)
const reportContentType = ref('')
const reportContentId = ref(0)

// 计算是否为动态作者或管理员（可以删除动态）
const canDeletePost = computed(() => {
  if (!userStore.isLoggedIn || !post.value) return false
  const isAuthor = userStore.userInfo?.id === post.value.author?.id
  const isAdmin = userStore.isAdmin
  return isAuthor || isAdmin
})

const loadPost = async () => {
  loading.value = true
  try {
    const response = await circleApi.getPost(route.params.id)
    post.value = response.data
    liked.value = response.data?.liked || false
    favorited.value = response.data?.favorited || false
    await loadComments()
    recordPostFootprint()
    // 加载相关推荐
    loadSimilarPosts()
  } catch (error) {
    ElMessage.error('加载动态失败')
  } finally {
    loading.value = false
  }
}

const loadSimilarPosts = async () => {
  similarLoading.value = true
  try {
    const response = await recommendApi.getSimilarPosts(route.params.id, 6)
    similarPosts.value = response.data || []
  } catch (error) {
    console.error('Failed to load similar posts:', error)
    // 失败时设置为空数组，不影响页面其他功能
    similarPosts.value = []
  } finally {
    similarLoading.value = false
  }
}

const goToPost = (postId) => {
  router.push(`/posts/${postId}`)
}

const truncate = (text, length) => {
  if (!text) return ''
  return text.length > length ? text.slice(0, length) + '...' : text
}

// 根据索引返回标签颜色类型，避免颜色重合
const getTagType = (index) => {
  const types = ['primary', 'success', 'warning', 'danger', 'info']
  return types[index % types.length]
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
      parentId: null
    })
    commentText.value = ''
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
  replyTargetId.value = comment.id
}

// 处理回复提交（从CommentItem组件触发）
const handleSubmitReply = async ({ comment, content }) => {
  if (!content.trim()) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  try {
    await circleApi.createComment(post.value.id, {
      content: content,
      parentId: comment.id
    })
    replyTargetId.value = null
    await loadComments()
    post.value.commentCount += 1
    ElMessage.success('回复发布成功')
  } catch (error) {
    ElMessage.error('回复发布失败')
  }
}

// 处理取消回复
const handleCancelReply = () => {
  replyTargetId.value = null
}

// 处理动态操作命令
const handlePostCommand = async (command) => {
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm(
        '确定要删除这条动态吗？删除后不可恢复。',
        '删除确认',
        {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }
      )
      await circleApi.deletePost(post.value.id)
      ElMessage.success('动态已删除')
      router.push('/') // 返回首页
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error('删除失败，请稍后再试')
      }
    }
  } else if (command === 'report') {
    openReportDialog('POST', post.value.id)
  }
}

// 打开举报对话框
const openReportDialog = (contentType, contentId) => {
  reportContentType.value = contentType
  reportContentId.value = contentId
  reportDialogVisible.value = true
}

// 举报成功回调
const handleReportSuccess = () => {
  // 可以在这里做一些成功后的处理，比如显示已举报标记
}

// 判断是否可以删除评论（评论作者、动态作者或管理员）
const canDeleteComment = (comment) => {
  if (!userStore.isLoggedIn) return false
  const currentUserId = userStore.userInfo?.id
  const isCommentAuthor = comment.author?.id === currentUserId
  const isPostOwner = post.value?.author?.id === currentUserId
  const isAdmin = userStore.isAdmin
  return isCommentAuthor || isPostOwner || isAdmin
}

// 删除评论
const handleDeleteComment = async (comment) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条评论吗？',
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    await circleApi.deleteComment(comment.id)
    ElMessage.success('评论已删除')
    await loadComments()
    if (post.value) {
      post.value.commentCount = Math.max(0, (post.value.commentCount || 0) - 1)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败，请稍后再试')
    }
  }
}

onMounted(() => {
  loadPost()
})

// 监听路由变化，重新加载
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    loadPost()
  }
})
</script>

<style scoped>
.post-detail-page {
  display: flex;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.post-detail-container {
  flex: 1;
  max-width: 800px;
  min-width: 0;
}

.loading, .not-found {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.post-detail {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-xl);
  padding: 32px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: var(--shadow-lg);
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

.post-actions-menu {
  flex-shrink: 0;
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

.post-tags :deep(.el-tag) {
  font-weight: 500;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  border: none;
}

.post-tags :deep(.el-tag--dark) {
  color: #fff !important;
  border-color: transparent !important;
}

.post-tags :deep(.el-tag--dark.el-tag--primary) {
  background-color: #409eff !important;
  color: #fff !important;
}

.post-tags :deep(.el-tag--dark.el-tag--success) {
  background-color: #67c23a !important;
  color: #fff !important;
}

.post-tags :deep(.el-tag--dark.el-tag--warning) {
  background-color: #e6a23c !important;
  color: #fff !important;
}

.post-tags :deep(.el-tag--dark.el-tag--danger) {
  background-color: #f56c6c !important;
  color: #fff !important;
}

.post-tags :deep(.el-tag--dark.el-tag--info) {
  background-color: #909399 !important;
  color: #fff !important;
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

.comments-list :deep(.comment-item-wrapper) {
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.comments-list :deep(.comment-item-wrapper:last-child) {
  border-bottom: none;
}

/* 相关推荐侧边栏 */
.similar-posts-sidebar {
  width: 320px;
  flex-shrink: 0;
}

.sidebar-card {
  background: #fff;
  border-radius: 20px;
  padding: 20px;
  border: 1px solid #eceff5;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  position: sticky;
  top: 80px;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  color: #1f2d3d;
  margin: 0 0 16px 0;
}

.sidebar-title .el-icon {
  color: #8b5cf6;
}

.sidebar-loading,
.sidebar-empty {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 14px;
}

.similar-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.similar-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: rgba(248, 250, 252, 0.8);
  backdrop-filter: blur(5px);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.similar-item:hover {
  background: rgba(241, 245, 249, 0.8);
  backdrop-filter: blur(5px);
  border: 1px solid rgba(0, 0, 0, 0.05);
  transform: translateX(4px);
}

.similar-image {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.similar-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.similar-image.no-image {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(226, 232, 240, 0.8);
  backdrop-filter: blur(5px);
  border: 1px solid rgba(0, 0, 0, 0.05);
  color: #94a3b8;
}

.similar-image.no-image .el-icon {
  font-size: 24px;
}

.similar-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.similar-content {
  margin: 0;
  font-size: 13px;
  color: #334155;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.similar-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #94a3b8;
}

.similar-author {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.similar-stats {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 响应式 */
@media (max-width: 1024px) {
  .post-detail-page {
    flex-direction: column;
  }

  .similar-posts-sidebar {
    width: 100%;
  }

  .sidebar-card {
    position: static;
  }

  .similar-list {
    flex-direction: row;
    overflow-x: auto;
    padding-bottom: 8px;
  }

  .similar-item {
    flex-direction: column;
    min-width: 160px;
    max-width: 180px;
  }

  .similar-image {
    width: 100%;
    height: 100px;
  }
}

@media (max-width: 600px) {
  .post-detail-page {
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