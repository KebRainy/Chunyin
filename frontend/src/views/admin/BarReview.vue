<template>
  <div class="bar-review">
    <div class="page-header">
      <h2 class="art-heading-h2">酒吧审核</h2>
    </div>

    <el-card>
      <el-table v-loading="loading" :data="applications" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="酒吧信息" min-width="200">
          <template #default="{ row }">
            <div>
              <div class="bar-name">{{ row.name }}</div>
              <div class="bar-address">
                {{ row.province }}{{ row.city }}{{ row.district }}{{ row.address }}
              </div>
              <div v-if="row.contactPhone" class="bar-phone">
                联系电话：{{ row.contactPhone }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="营业时间" width="150">
          <template #default="{ row }">
            <div v-if="row.openingTime && row.closingTime">
              {{ formatTime(row.openingTime) }} - {{ formatTime(row.closingTime) }}
            </div>
            <span v-else class="text-muted">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="主营酒类" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.mainBeverages || '未填写' }}
          </template>
        </el-table-column>
        <el-table-column label="申请人ID" width="100">
          <template #default="{ row }">
            {{ row.applicantId }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="success"
              size="small"
              @click="handleApprove(row)"
              :loading="submitting"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              type="danger"
              size="small"
              @click="handleReject(row)"
              :loading="submitting"
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

      <div v-if="!loading && applications.length === 0" class="empty-state">
        <el-empty description="暂无待审核的酒吧申请" />
      </div>
    </el-card>

    <!-- 拒绝对话框 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="拒绝酒吧申请"
      width="500px"
    >
      <el-form :model="rejectForm" label-width="100px">
        <el-form-item label="拒绝原因" required>
          <el-input
            v-model="rejectForm.reviewNote"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因"
            maxlength="500"
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

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="酒吧申请详情"
      width="600px"
    >
      <div v-if="currentApplication" class="detail-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="酒吧名称">
            {{ currentApplication.name }}
          </el-descriptions-item>
          <el-descriptions-item label="地址">
            {{ currentApplication.province }}{{ currentApplication.city }}{{ currentApplication.district }}{{ currentApplication.address }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ currentApplication.contactPhone }}
          </el-descriptions-item>
          <el-descriptions-item label="营业时间">
            <span v-if="currentApplication.openingTime && currentApplication.closingTime">
              {{ formatTime(currentApplication.openingTime) }} - {{ formatTime(currentApplication.closingTime) }}
            </span>
            <span v-else>未设置</span>
          </el-descriptions-item>
          <el-descriptions-item label="主营酒类">
            {{ currentApplication.mainBeverages || '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="描述">
            {{ currentApplication.description || '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="申请人ID">
            {{ currentApplication.applicantId }}
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">
            {{ formatDateTime(currentApplication.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentApplication.status)">
              {{ getStatusText(currentApplication.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentApplication.reviewNote" label="审核备注">
            {{ currentApplication.reviewNote }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingApplications, reviewBarApplication } from '@/api/bar'
import dayjs from 'dayjs'

const loading = ref(false)
const submitting = ref(false)
const applications = ref([])

const rejectDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const currentApplication = ref(null)
const rejectForm = ref({
  reviewNote: ''
})

// 加载待审核申请
const loadApplications = async () => {
  loading.value = true
  try {
    const res = await getPendingApplications()
    if (res.code === 200) {
      applications.value = res.data || []
    }
  } catch (error) {
    ElMessage.error('加载待审核申请失败')
  } finally {
    loading.value = false
  }
}

// 通过审核
const handleApprove = async (application) => {
  try {
    await ElMessageBox.confirm('确定要通过此酒吧申请吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })

    submitting.value = true
    try {
      const res = await reviewBarApplication(application.id, {
        status: 'APPROVED'
      })
      if (res.code === 200) {
        ElMessage.success('审核通过')
        await loadApplications()
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
const handleReject = (application) => {
  currentApplication.value = application
  rejectForm.value.reviewNote = ''
  rejectDialogVisible.value = true
}

// 确认拒绝
const confirmReject = async () => {
  if (!rejectForm.value.reviewNote || !rejectForm.value.reviewNote.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }

  submitting.value = true
  try {
    const res = await reviewBarApplication(currentApplication.value.id, {
      status: 'REJECTED',
      reviewNote: rejectForm.value.reviewNote
    })
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      rejectDialogVisible.value = false
      await loadApplications()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '审核失败')
  } finally {
    submitting.value = false
  }
}

// 查看详情
const viewDetail = (application) => {
  currentApplication.value = application
  detailDialogVisible.value = true
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm')
}

// 格式化时间（LocalTime）
const formatTime = (time) => {
  if (!time) return ''
  // 如果是字符串格式 "HH:mm:ss" 或 "HH:mm"
  if (typeof time === 'string') {
    return time.substring(0, 5) // 取前5个字符 "HH:mm"
  }
  return time
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
  loadApplications()
})
</script>

<style scoped>
.bar-review {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.bar-name {
  font-weight: bold;
  margin-bottom: 5px;
}

.bar-address {
  color: #666;
  font-size: 14px;
  margin-bottom: 5px;
}

.bar-phone {
  color: #999;
  font-size: 12px;
}

.text-muted {
  color: #999;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
}

.detail-content {
  padding: 10px 0;
}
</style>

