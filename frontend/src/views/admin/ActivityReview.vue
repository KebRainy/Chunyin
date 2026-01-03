<template>
  <div class="activity-review">
    <div class="page-header">
      <h2>活动审核</h2>
    </div>

    <el-card>
      <el-table v-loading="loading" :data="activities" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="活动信息" min-width="200">
          <template #default="{ row }">
            <div>
              <div class="activity-title">{{ row.beverageName }} 组局</div>
              <div class="activity-time">
                {{ formatDateTime(row.activityTime) }}
              </div>
              <div v-if="row.barName" class="activity-bar">
                酒吧：{{ row.barName }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="发起者" width="120">
          <template #default="{ row }">
            {{ row.organizerName }}
          </template>
        </el-table-column>
        <el-table-column label="参与人数" width="120">
          <template #default="{ row }">
            {{ row.currentParticipants }} / {{ row.maxParticipants }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.reviewStatus)">
              {{ getStatusText(row.reviewStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.reviewStatus === 'PENDING'"
              type="success"
              size="small"
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.reviewStatus === 'PENDING'"
              type="danger"
              size="small"
              @click="handleReject(row)"
            >
              拒绝
            </el-button>
            <el-button
              type="primary"
              size="small"
              @click="viewDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadActivities"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>

    <!-- 拒绝对话框 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="拒绝活动"
      width="500px"
    >
      <el-form :model="rejectForm" label-width="100px">
        <el-form-item label="拒绝原因" required>
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject" :loading="submitting">
          确认拒绝
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingActivities, reviewActivity } from '@/api/activity'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const activities = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const rejectDialogVisible = ref(false)
const currentActivity = ref(null)
const rejectForm = ref({
  rejectReason: ''
})

// 加载待审核活动
const loadActivities = async () => {
  loading.value = true
  try {
    const res = await getPendingActivities(page.value, pageSize.value)
    if (res.code === 200) {
      activities.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载待审核活动失败')
  } finally {
    loading.value = false
  }
}

// 通过审核
const handleApprove = async (activity) => {
  try {
    await ElMessageBox.confirm('确定要通过此活动吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })

    submitting.value = true
    try {
      const res = await reviewActivity(activity.id, {
        status: 'APPROVED'
      })
      if (res.code === 200) {
        ElMessage.success('审核通过')
        await loadActivities()
      }
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '审核失败')
    } finally {
      submitting.value = false
    }
  } catch {
    // 用户取消
  }
}

// 拒绝审核
const handleReject = (activity) => {
  currentActivity.value = activity
  rejectForm.value.rejectReason = ''
  rejectDialogVisible.value = true
}

// 确认拒绝
const confirmReject = async () => {
  if (!rejectForm.value.rejectReason || !rejectForm.value.rejectReason.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }

  submitting.value = true
  try {
    const res = await reviewActivity(currentActivity.value.id, {
      status: 'REJECTED',
      rejectReason: rejectForm.value.rejectReason
    })
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      rejectDialogVisible.value = false
      await loadActivities()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '审核失败')
  } finally {
    submitting.value = false
  }
}

// 查看详情
const viewDetail = (activity) => {
  router.push(`/activities/${activity.id}`)
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
    REJECTED: '已拒绝'
  }
  return statusMap[status] || status
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return typeMap[status] || ''
}

onMounted(() => {
  loadActivities()
})
</script>

<style scoped>
.activity-review {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.activity-title {
  font-weight: bold;
  margin-bottom: 5px;
}

.activity-time {
  color: #666;
  font-size: 14px;
  margin-bottom: 5px;
}

.activity-bar {
  color: #999;
  font-size: 12px;
}
</style>

