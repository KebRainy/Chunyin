<template>
  <div :class="['post-card', layout]" @click="$emit('select', post)">
    <div class="card-media" v-if="post.imageUrls?.length">
      <img
        :src="post.imageUrls[0]"
        :alt="post.content || `${post.author?.username || '用户'}的分享图片`"
      >
      <span v-if="post.imageUrls.length > 1" class="count">+{{ post.imageUrls.length - 1 }}</span>
    </div>
    <div class="card-media no-image" v-else>
      <p class="no-image-text">{{ coverText(post.content) }}</p>
    </div>
    <div class="card-body">
      <div class="author">
        <el-avatar
          :src="post.author?.avatarUrl"
          :size="32"
          :alt="`${post.author?.username || '用户'}的头像`"
        />
        <div>
          <p class="name">{{ post.author?.username }}</p>
          <p class="time">{{ formatTime(post.createdAt) }}</p>
        </div>
      </div>
      <p class="content">{{ snippet(post.content) }}</p>
      <div class="tags" v-if="post.tags?.length">
        <el-tag
          v-for="tag in post.tags.slice(0, 3)"
          :key="tag"
          size="small"
          effect="light"
        >
          # {{ tag }}
        </el-tag>
      </div>
    </div>
    <div class="card-footer">
      <span><el-icon><Location /></el-icon>{{ post.location || '未知地点' }}</span>
      <span><el-icon><View /></el-icon>{{ post.viewCount || 0 }}</span>
      <span><el-icon><Heart /></el-icon>{{ post.likeCount || 0 }}</span>
    </div>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import { Picture, View, Heart, Location } from '@element-plus/icons-vue'

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
  return text.length > 120 ? `${text.slice(0, 120)}...` : text
}

const coverText = (text) => {
  if (!text) return '分享此刻的灵感'
  return text.length > 80 ? `${text.slice(0, 80)}...` : text
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
  transition: transform 0.2s, box-shadow 0.2s;
  break-inside: avoid;
}

.post-card:hover {
  box-shadow: 0 18px 28px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
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
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f8fafc;
}

.no-image-text {
  font-size: 16px;
  color: #1f2d3d;
  margin: 0;
  text-align: center;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-media .count {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
}

.card-body {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.author {
  display: flex;
  gap: 10px;
  align-items: center;
}

.author .name {
  margin: 0;
  font-weight: 600;
}

.author .time {
  margin: 0;
  color: #909399;
  font-size: 12px;
}

.content {
  margin: 0;
  color: #444;
  line-height: 1.5;
  font-size: 14px;
}

.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.card-footer {
  padding: 12px 16px 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #606266;
}

.card-footer span {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
