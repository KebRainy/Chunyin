<template>
  <div class="seller-review">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>商家认证审核</span>
          <el-button type="primary" @click="loadApplications" :loading="loading">刷新列表</el-button>
        </div>
      </template>

      <el-table :data="applications" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="idCardNumber" label="身份证号" width="180" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="createdAt" label="申请时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleReview(row, 'APPROVED')">通过</el-button>
            <el-button type="danger" size="small" @click="openRejectDialog(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 拒绝原因对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝申请" width="30%">
      <el-input v-model="rejectNote" type="textarea" placeholder="请输入拒绝原因" :rows="3" />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="rejectDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmReject">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingSellerApplications, reviewSellerApplication } from '@/api/seller'
import dayjs from 'dayjs'

const loading = ref(false)
const applications = ref([])
const rejectDialogVisible = ref(false)
const rejectNote = ref('')
const currentApp = ref(null)

const loadApplications = async () => {
  loading.value = true
  try {
    const res = await getPendingSellerApplications()
    if (res.code === 200) {
      applications.value = res.data || []
    }
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleReview = async (row, status) => {
  try {
    await ElMessageBox.confirm(`确定要${status === 'APPROVED' ? '通过' : '拒绝'}该申请吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await reviewSellerApplication(row.id, { status, reviewNote: status === 'APPROVED' ? '审核通过' : rejectNote.value })
    if (res.code === 200) {
      ElMessage.success('操作成功')
      loadApplications()
    }
  } catch (error) {
    // cancelled or error
  }
}

const openRejectDialog = (row) => {
  currentApp.value = row
  rejectNote.value = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!rejectNote.value) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  await handleReview(currentApp.value, 'REJECTED')
  rejectDialogVisible.value = false
}

const formatDateTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
}

onMounted(() => {
  loadApplications()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
