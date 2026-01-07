<template>
  <div :class="['post-card', layout, { 'is-recommended': post.isRecommended }]" @click="$emit('select', post)">
    <!-- 推荐标识 -->
    <div v-if="post.isRecommended" class="recommend-badge">
      <el-icon><MagicStick /></el-icon>
      <span>猜你喜欢</span>
    </div>
    <div class="card-media" v-if="post.imageUrls?.length">
      <img
        :src="post.imageUrls[0]"
        :alt="post.content || `${post.author?.username || '用户'}的分享图片`"
      >
      <div class="image-overlay">
        <span class="overlay-item"><el-icon><View /></el-icon>{{ post.viewCount || 0 }}</span>
        <span class="overlay-item"><el-icon><GobletFull /></el-icon>{{ post.likeCount || 0 }}</span>
      </div>
      <span v-if="post.imageUrls.length > 1" class="count">+{{ post.imageUrls.length - 1 }}</span>
    </div>
    <div class="card-media no-image" v-else>
      <p class="no-image-text">{{ coverText(post.content) }}</p>
      <div class="image-overlay">
        <span class="overlay-item"><el-icon><View /></el-icon>{{ post.viewCount || 0 }}</span>
        <span class="overlay-item"><el-icon><GobletFull /></el-icon>{{ post.likeCount || 0 }}</span>
      </div>
    </div>
    <div class="card-body">
      <!-- <div class="location">
        <el-icon><Location /></el-icon>
        <span>{{ post.location || '未知地点' }}</span>
      </div> -->
      <p class="content">{{ snippet(post.content) }}</p>
      <div class="tags" v-if="post.tags?.length">
        <el-tag
          v-for="tag in post.tags.slice(0, 2)"
          :key="tag"
          size="small"
          effect="light"
        >
          # {{ tag }}
        </el-tag>
      </div>
    </div>
    <div class="card-meta">
      <div class="author">
        <el-avatar
          :src="post.author?.avatarUrl"
          :size="28"
          :alt="`${post.author?.username || '用户'}的头像`"
        />
        <p class="name">{{ post.author?.username }}</p>
      </div>
      <span class="time">{{ formatTime(post.createdAt) }}</span>
    </div>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import { Picture, View, GobletFull, Heart, Location, MagicStick } from '@element-plus/icons-vue'

dayjs.extend(relativeTime)

defineProps({
  post: {
    type: Object,
    required: true
  },
  layout: {
    type: String,
    default: 'masonry'
  }
})

defineEmits(['select'])

const snippet = (text) => {
  if (!text) return ''
  return text.length > 29 ? `${text.slice(0, 29)}...` : text
}

const coverText = (text) => {
  if (!text) return '分享此刻的灵感'
  return text.length > 60 ? `${text.slice(0, 60)}...` : text
}

const formatTime = (value) => {
  if (!value) return ''
  const date = dayjs(value)
  const now = dayjs()
  const diffMinutes = now.diff(date, 'minute')
  if (diffMinutes < 60) return `${diffMinutes || 1}分钟前`
  const diffHours = now.diff(date, 'hour')
  if (diffHours < 24) return `${diffHours}小时前`
  if (date.isSame(now, 'year')) {
    return date.format('M月D日')
  }
  return date.format('YYYY年M月D日')
}
</script>

<style scoped>
.post-card {
  background: #fff;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #f1f1f1;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  will-change: transform;
  transition:
    transform var(--motion-normal, 220ms) var(--motion-ease, ease),
    box-shadow var(--motion-normal, 220ms) var(--motion-ease, ease);
  break-inside: avoid;
  position: relative;
}

.post-card:hover {
  box-shadow: 0 18px 28px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

/* 推荐卡片样式 */
.post-card.is-recommended {
  border: 1px solid #e6d9f7;
  background: linear-gradient(135deg, #fdfbff 0%, #fff 100%);
}

.post-card.is-recommended:hover {
  box-shadow: 0 18px 28px rgba(139, 92, 246, 0.12);
}

.recommend-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
  color: #fff;
  font-size: 11px;
  font-weight: 500;
  border-radius: 999px;
  box-shadow: 0 2px 8px rgba(139, 92, 246, 0.3);
}

.recommend-badge .el-icon {
  font-size: 12px;
}

.card-media {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-bottom: 1px solid #f5f5f5;
}

.card-media img {
  width: 100%;
  display: block;
  object-fit: cover;
  max-height: 520px;
}

.card-media.no-image {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px 64px;
  background: #f8fafc;
}

.no-image-text {
  font-size: 18px;
  color: #1f2d3d;
  margin: 0;
  text-align: center;
  line-height: 1.8;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.image-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: space-between;
  padding: 10px 14px;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.45) 100%);
  color: #fff;
  font-size: 12px;
}

.overlay-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.card-media .count {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
}

.card-body {
  padding: 14px 16px 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.location {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.content {
  margin: 0;
  color: #1f2d3d;
  line-height: 1.6;
  font-size: 16px;
  font-weight: 500;
}

.tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.card-meta {
  padding: 10px 16px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author .name {
  margin: 0;
  font-weight: 600;
  font-size: 13px;
  color: #1f2d3d;
}

.card-meta .time {
  font-size: 12px;
  color: #a0a3ad;
}
</style>
