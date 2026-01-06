<template>
  <div class="activity-detail">
    <div v-loading="loading" class="detail-container">
      <el-button @click="$router.back()" style="margin-bottom: 20px">返回</el-button>

      <el-card v-if="activity" class="activity-card">
        <template #header>
          <div class="card-header">
            <h2>{{ activity.beverageName }} 组局</h2>
            <el-tag :type="getStatusType(activity.status)">
              {{ getStatusText(activity.status) }}
            </el-tag>
          </div>
        </template>

        <div class="activity-info">
          <div class="info-item">
            <el-icon><Clock /></el-icon>
            <span class="label">活动时间：</span>
            <span>{{ formatDateTime(activity.activityTime) }}</span>
          </div>

          <div v-if="activity.barName" class="info-item">
            <el-icon><LocationFilled /></el-icon>
            <span class="label">酒吧：</span>
            <span>{{ activity.barName }}</span>
            <span v-if="activity.barAddress" class="address">（{{ activity.barAddress }}）</span>
          </div>

          <div class="info-item">
            <el-icon><User /></el-icon>
            <span class="label">参与人数：</span>
            <span>{{ activity.currentParticipants }} / {{ activity.maxParticipants }} 人</span>
          </div>

          <div class="info-item">
            <el-icon><UserFilled /></el-icon>
            <span class="label">发起者：</span>
            <span>{{ activity.organizerName }}</span>
          </div>

          <div v-if="activity.remark" class="info-item remark">
            <span class="label">备注：</span>
            <p>{{ activity.remark }}</p>
          </div>

          <div v-if="activity.reviewStatus === 'PENDING'" class="review-status">
            <el-alert type="warning" :closable="false">活动待审核中</el-alert>
          </div>

          <div v-if="activity.reviewStatus === 'REJECTED'" class="review-status">
            <el-alert type="error" :closable="false">活动审核未通过</el-alert>
          </div>
        </div>

        <div class="action-buttons">
          <el-button
            v-if="activity.reviewStatus === 'APPROVED' && !activity.isParticipated && !activity.isOrganizer && !activity.isFinished"
            type="primary"
            :disabled="activity.currentParticipants >= activity.maxParticipants"
            @click="handleJoin"
            :loading="actionLoading"
          >
            {{ activity.currentParticipants >= activity.maxParticipants ? '人数已满' : '参与活动' }}
          </el-button>

          <el-button
            v-if="activity.isParticipated && !activity.isOrganizer && !activity.isFinished"
            type="danger"
            @click="handleCancel"
            :loading="actionLoading"
          >
            取消参与
          </el-button>

          <el-button
            v-if="activity.isOrganizer && activity.status !== 'CANCELLED' && activity.status !== 'FINISHED' && activity.reviewStatus !== 'REJECTED'"
            type="danger"
            @click="handleCancelActivity"
            :loading="actionLoading"
          >
            取消活动
          </el-button>

          <el-button
            v-if="activity.isFinished && activity.isParticipated && activity.barId && !activity.hasReviewed"
            type="success"
            @click="showReviewDialog = true"
          >
            写评价
          </el-button>
          <el-alert
            v-if="activity.isFinished && activity.isParticipated && activity.hasReviewed"
            type="success"
            :closable="false"
            style="margin-top: 10px"
          >
            您已评价过此活动
          </el-alert>
        </div>
      </el-card>

      <!-- 评价对话框 -->
      <el-dialog
        v-model="showReviewDialog"
        title="活动评价"
        width="500px"
        @close="resetReviewForm"
      >
        <el-form :model="reviewForm" :rules="reviewRules" ref="reviewFormRef" label-width="100px">
          <el-form-item label="评分" prop="rating">
            <el-rate
              v-model="reviewForm.rating"
              :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
            />
          </el-form-item>
          <el-form-item label="评价内容" prop="content">
            <el-input
              v-model="reviewForm.content"
              type="textarea"
              :rows="5"
              placeholder="分享您对这次活动的体验..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showReviewDialog = false">取消</el-button>
          <el-button type="primary" @click="submitReview" :loading="submittingReview">
            提交
          </el-button>
        </template>
      </el-dialog>

      <el-card v-if="activity && activity.reviewStatus === 'APPROVED'" class="participants-card">
        <template #header>
          <h3>参与者列表（{{ activity.currentParticipants }}人）</h3>
        </template>
        <div class="participants-list">
          <div v-if="participants.length === 0" class="empty-participants">
            <el-empty description="暂无参与者" :image-size="60" />
          </div>
          <div v-for="participant in participants" :key="participant.id" class="participant-item">
            <el-avatar :src="participant.avatarUrl" :size="40">
              {{ participant.username?.charAt(0) }}
            </el-avatar>
            <span class="participant-name">{{ participant.username }}</span>
            <el-tag v-if="participant.id === activity.organizerId" type="primary" size="small">
              发起者
            </el-tag>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, LocationFilled, User, UserFilled } from '@element-plus/icons-vue'
import { getActivityById, joinActivity, cancelJoinActivity, cancelActivity, getActivityParticipants, reviewActivityBar } from '@/api/activity'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const actionLoading = ref(false)
const activity = ref(null)
const participants = ref([])

// 评价相关
const showReviewDialog = ref(false)
const submittingReview = ref(false)
const reviewFormRef = ref(null)
const reviewForm = ref({
  rating: 5,
  content: ''
})

const reviewRules = {
  rating: [
    { required: true, message: '请选择评分', trigger: 'change' }
  ]
}

// 加载活动详情
const loadActivityDetail = async () => {
  loading.value = true
  try {
    const res = await getActivityById(route.params.id)
    if (res.code === 200) {
      activity.value = res.data
      // 加载参与者列表
      if (activity.value.reviewStatus === 'APPROVED') {
        loadParticipants()
      }
    }
  } catch (error) {
    ElMessage.error('加载活动详情失败')
  } finally {
    loading.value = false
  }
}

// 加载参与者列表
const loadParticipants = async () => {
  try {
    const res = await getActivityParticipants(route.params.id)
    if (res.code === 200) {
      participants.value = res.data || []
    }
  } catch (error) {
    console.error('加载参与者列表失败:', error)
  }
}

// 参与活动
const handleJoin = async () => {
  try {
    await ElMessageBox.confirm('确定要参与此活动吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })

    actionLoading.value = true
    try {
      const res = await joinActivity(route.params.id)
      if (res.code === 200) {
        ElMessage.success('参与成功')
        await loadActivityDetail()
      }
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '参与失败')
    } finally {
      actionLoading.value = false
    }
  } catch {
    // 用户取消
  }
}

// 取消参与
const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确定要取消参与此活动吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    actionLoading.value = true
    try {
      const res = await cancelJoinActivity(route.params.id)
      if (res.code === 200) {
        ElMessage.success('已取消参与')
        await loadActivityDetail()
      }
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '取消参与失败')
    } finally {
      actionLoading.value = false
    }
  } catch {
    // 用户取消
  }
}

// 取消活动（发起者）
const handleCancelActivity = async () => {
  try {
    await ElMessageBox.confirm('确定要取消此活动吗？取消后无法恢复，已参与的用户将收到通知。', '取消活动', {
      confirmButtonText: '确定取消',
      cancelButtonText: '我再想想',
      type: 'warning'
    })

    actionLoading.value = true
    try {
      const res = await cancelActivity(route.params.id)
      if (res.code === 200) {
        ElMessage.success('活动已取消')
        await loadActivityDetail()
      }
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '取消活动失败')
    } finally {
      actionLoading.value = false
    }
  } catch {
    // 用户取消
  }
}

// 重置评价表单
const resetReviewForm = () => {
  reviewForm.value = {
    rating: 5,
    content: ''
  }
  reviewFormRef.value?.resetFields()
}

// 提交评价
const submitReview = async () => {
  if (!reviewFormRef.value) return
  
  await reviewFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    if (!activity.value || !activity.value.barId) {
      ElMessage.error('该活动没有关联酒吧，无法评价')
      return
    }
    
    submittingReview.value = true
    try {
      const res = await reviewActivityBar(activity.value.id, {
        rating: reviewForm.value.rating,
        content: reviewForm.value.content || null
      })
      if (res.code === 200) {
        ElMessage.success('评价提交成功')
        showReviewDialog.value = false
        await loadActivityDetail()
      }
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '评价提交失败')
    } finally {
      submittingReview.value = false
    }
  })
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    ONGOING: '进行中',
    FINISHED: '已结束',
    CANCELLED: '已取消'
  }
  return statusMap[status] || status
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    ONGOING: 'primary',
    FINISHED: 'info',
    CANCELLED: 'info'
  }
  return typeMap[status] || ''
}

onMounted(() => {
  loadActivityDetail()
})
</script>

<style scoped>
.activity-detail {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.detail-container {
  min-height: 400px;
}

.activity-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
}

.activity-info {
  padding: 20px 0;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  gap: 10px;
}

.info-item .label {
  font-weight: bold;
  color: #666;
}

.info-item.remark {
  flex-direction: column;
  align-items: flex-start;
}

.info-item.remark p {
  margin: 5px 0 0 0;
  color: #666;
}

.review-status {
  margin-top: 20px;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

.participants-card {
  margin-top: 20px;
}

.participants-list {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.participant-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  min-width: 150px;
}

.participant-name {
  flex: 1;
  font-weight: 500;
}

.empty-participants {
  padding: 20px;
  text-align: center;
}
</style>

