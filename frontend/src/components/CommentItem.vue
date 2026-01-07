<template>
  <div class="comment-item-wrapper">
    <el-avatar
      :src="comment.author?.avatarUrl"
      :size="avatarSize"
      :alt="`${comment.author?.username || '用户'}的头像`"
    />
    <div class="comment-content">
      <div class="comment-header">
        <span class="comment-author">{{ comment.author?.username }}</span>
        <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
      </div>
      <div class="comment-text">
        <span v-if="comment.replyToUser" class="reply-to">
          @{{ comment.replyToUser.username }}
        </span>
        {{ comment.content }}
      </div>
      <div class="comment-actions">
        <el-button text size="small" @click="$emit('reply', comment)">回复</el-button>
        <el-button 
          v-if="canDelete" 
          text 
          size="small" 
          type="danger"
          @click="$emit('delete', comment)"
        >删除</el-button>
        <el-button 
          v-if="canReport" 
          text 
          size="small" 
          type="warning"
          @click="$emit('report', comment)"
        >举报</el-button>
      </div>
      <!-- 回复输入框 -->
      <div v-if="showReplyInput" class="reply-input-container">
        <el-input
          v-model="replyText"
          type="textarea"
          placeholder="写下你的回复..."
          rows="2"
          class="reply-textarea"
        />
        <div class="reply-input-actions">
          <el-button size="small" @click="cancelReply">取消</el-button>
          <el-button type="primary" size="small" @click="submitReply" :loading="replying">
            发布
          </el-button>
        </div>
      </div>
      <!-- 子评论显示逻辑：仅两层（一级评论 -> 二级评论），二级评论默认折叠，由一级评论展开 -->
      <template v-if="level === 0 && comment.replies?.length">
        <div class="collapse-control">
          <el-button text size="small" type="primary" @click="isExpanded = !isExpanded">
            <el-icon><ArrowDown v-if="!isExpanded" /><ArrowUp v-else /></el-icon>
            {{ isExpanded ? '收起回复' : `展开 ${comment.replies.length} 条回复` }}
          </el-button>
        </div>
        <div v-if="isExpanded" class="reply-list">
          <CommentItem
            v-for="reply in comment.replies"
            :key="reply.id"
            :comment="reply"
            :level="1"
            :avatar-size="24"
            :can-delete="canDeleteComment(reply)"
            :can-report="canReportComment(reply)"
            :format-time="formatTime"
            :can-delete-comment="canDeleteComment"
            :can-report-comment="canReportComment"
            :reply-target-id="replyTargetId"
            @reply="handleReply"
            @delete="$emit('delete', $event)"
            @report="$emit('report', $event)"
            @submit-reply="$emit('submit-reply', $event)"
            @cancel-reply="$emit('cancel-reply')"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'
// 递归组件需要显式导入自身
import CommentItem from './CommentItem.vue'

const props = defineProps({
  comment: {
    type: Object,
    required: true
  },
  level: {
    type: Number,
    default: 0
  },
  avatarSize: {
    type: Number,
    default: 32
  },
  canDelete: {
    type: Boolean,
    default: false
  },
  canReport: {
    type: Boolean,
    default: false
  },
  formatTime: {
    type: Function,
    required: true
  },
  canDeleteComment: {
    type: Function,
    required: true
  },
  canReportComment: {
    type: Function,
    required: true
  },
  replyTargetId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['reply', 'delete', 'report', 'submit-reply', 'cancel-reply'])

// 控制折叠/展开状态
const isExpanded = ref(false)

// 控制回复输入框显示
const showReplyInput = computed(() => props.replyTargetId === props.comment.id)
const replyText = ref('')
const replying = ref(false)

// 处理回复按钮点击
const handleReply = (comment) => {
  emit('reply', comment)
}

// 提交回复
const submitReply = async () => {
  if (!replyText.value.trim()) {
    return
  }
  replying.value = true
  try {
    emit('submit-reply', {
      comment: props.comment,
      content: replyText.value
    })
    replyText.value = ''
  } finally {
    replying.value = false
  }
}

// 取消回复
const cancelReply = () => {
  replyText.value = ''
  emit('cancel-reply')
}
</script>

<style scoped>
.comment-item-wrapper {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  /* 确保二级及以下评论在同一竖列对齐 */
  margin-left: 0;
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

.reply-to {
  color: #409eff;
  font-weight: 500;
  margin-right: 4px;
}

.comment-actions {
  margin-top: 6px;
}

.reply-list {
  margin-top: 12px;
  padding-left: 40px;
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* 二级及以下评论不再有缩进，确保在同一竖列 */
.reply-list.no-indent {
  padding-left: 0;
  margin-left: 0;
}

/* 确保展开的回复也在同一竖列 */
.expanded-replies {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin-left: 0;
  padding-left: 0;
}

.collapse-control {
  margin-top: 8px;
  margin-bottom: 8px;
}

.reply-input-container {
  margin-top: 12px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.reply-textarea {
  margin-bottom: 8px;
}

.reply-input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>

