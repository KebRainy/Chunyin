<template>
  <div class="admin-moderation">
    <div class="admin-header">
      <h1 class="art-heading">管理后台</h1>
    </div>

    <!-- 审核入口卡片 -->
    <div class="review-cards">
      <el-card class="review-card" shadow="hover" @click="goToActivityReview">
        <div class="card-content">
          <el-icon class="card-icon" :size="48" color="#409EFF">
            <Calendar />
          </el-icon>
          <h3 class="art-heading-h3">活动审核</h3>
          <p>审核用户提交的活动申请</p>
        </div>
      </el-card>

      <el-card class="review-card" shadow="hover" @click="goToWikiReview">
        <div class="card-content">
          <el-icon class="card-icon" :size="48" color="#67C23A">
            <Document />
          </el-icon>
          <h3 class="art-heading-h3">Wiki审核</h3>
          <p>审核用户提交的Wiki词条</p>
        </div>
      </el-card>

      <el-card class="review-card" shadow="hover" @click="goToBarReview">
        <div class="card-content">
          <el-icon class="card-icon" :size="48" color="#E6A23C">
            <Shop />
          </el-icon>
          <h3 class="art-heading-h3">酒吧审核</h3>
          <p>审核用户提交的酒吧申请</p>
        </div>
      </el-card>
    </div>

    <!-- 内容审核管理 -->
    <div class="section-header">
      <h2 class="art-heading-h2">内容审核管理</h2>
    </div>

    <div class="filter-bar">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="高风险举报" name="high-risk"></el-tab-pane>
        <el-tab-pane label="待处理" name="pending"></el-tab-pane>
        <el-tab-pane label="全部" name="all"></el-tab-pane>
      </el-tabs>

      <div class="filter-options" v-if="activeTab === 'all'">
        <el-select v-model="filterStatus" placeholder="筛选状态" @change="loadReports" clearable>
          <el-option
            v-for="status in statusOptions"
            :key="status.value"
            :label="status.label"
            :value="status.value"
          ></el-option>
        </el-select>
      </div>

      <div class="batch-actions" v-if="selectedReports.length > 0">
        <span class="selected-count">已选择 {{ selectedReports.length }} 项</span>
        <el-button type="primary" size="small" @click="showBatchHandle = true">
          批量处理
        </el-button>
      </div>
    </div>

    <div class="reports-table">
      <el-table
        :data="reports"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        stripe
      >
        <el-table-column type="selection" width="55"></el-table-column>
        
        <el-table-column label="风险等级" width="100">
          <template #default="scope">
            <el-tag
              :type="getRiskType(scope.row.riskLevel)"
              effect="dark"
            >
              {{ scope.row.riskLevel }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="内容" min-width="300">
          <template #default="scope">
            <div class="content-preview">
              <div class="content-text">{{ scope.row.contentSnapshot }}</div>
              <div class="violations" v-if="scope.row.autoModerationResult">
                <el-tag
                  v-for="(violation, index) in parseViolations(scope.row.autoModerationResult)"
                  :key="index"
                  size="small"
                  type="danger"
                  class="violation-tag"
                >
                  {{ violation }}
                </el-tag>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="举报原因" width="120">
          <template #default="scope">
            <el-tag type="warning" size="small">
              {{ getReasonLabel(scope.row.reason) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="举报人" width="120">
          <template #default="scope">
            <div class="reporter-info">
              <div>{{ scope.row.reporter?.username || '未知' }}</div>
              <div class="reporter-id">ID: {{ scope.row.reporter?.id || '-' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="被举报用户" width="120">
          <template #default="scope">
            <div class="target-user">
              <div>{{ scope.row.contentAuthor?.username || '未知' }}</div>
              <div class="user-id">ID: {{ scope.row.contentAuthor?.id || '-' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="举报时间" width="160">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="viewDetail(scope.row)"
            >
              详情
            </el-button>
            <el-button
              v-if="scope.row.status === 'PENDING'"
              type="success"
              size="small"
              @click="handleSingleReport(scope.row)"
            >
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        class="pagination"
        @current-change="handlePageChange"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next, jumper"
      ></el-pagination>
    </div>

    <!-- 举报详情对话框 -->
    <el-dialog
      v-model="showDetail"
      title="举报详情"
      width="700px"
      destroy-on-close
    >
      <div class="report-detail" v-if="currentReport">
        <div class="detail-section">
          <h3 class="art-heading-h3">内容信息</h3>
          <div class="detail-item">
            <span class="label">内容：</span>
            <div class="content-box">{{ currentReport.contentSnapshot }}</div>
          </div>
          <div class="detail-item">
            <span class="label">风险等级：</span>
            <el-tag :type="getRiskType(currentReport.riskLevel)" effect="dark">
              {{ currentReport.riskLevel }}
            </el-tag>
          </div>
          <div class="detail-item" v-if="currentReport.autoModerationResult">
            <span class="label">违规项：</span>
            <div class="violations-list">
              <el-tag
                v-for="(violation, index) in parseViolations(currentReport.autoModerationResult)"
                :key="index"
                type="danger"
                class="violation-tag"
              >
                {{ violation }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h3 class="art-heading-h3">举报信息</h3>
          <div class="detail-item">
            <span class="label">举报原因：</span>
            <el-tag type="warning">{{ getReasonLabel(currentReport.reason) }}</el-tag>
          </div>
          <div class="detail-item" v-if="currentReport.description">
            <span class="label">详细说明：</span>
            <div class="description-box">{{ currentReport.description }}</div>
          </div>
          <div class="detail-item">
            <span class="label">举报人：</span>
            <span>{{ currentReport.reporter?.username }} (ID: {{ currentReport.reporter?.id }})</span>
          </div>
          <div class="detail-item">
            <span class="label">被举报用户：</span>
            <span>{{ currentReport.contentAuthor?.username }} (ID: {{ currentReport.contentAuthor?.id }})</span>
          </div>
          <div class="detail-item">
            <span class="label">举报时间：</span>
            <span>{{ formatDate(currentReport.createdAt) }}</span>
          </div>
        </div>

        <div class="detail-section" v-if="currentReport.status !== 'PENDING'">
          <h3 class="art-heading-h3">处理信息</h3>
          <div class="detail-item">
            <span class="label">状态：</span>
            <el-tag :type="getStatusType(currentReport.status)">
              {{ getStatusLabel(currentReport.status) }}
            </el-tag>
          </div>
          <div class="detail-item" v-if="currentReport.handleNote">
            <span class="label">处理备注：</span>
            <div class="note-box">{{ currentReport.handleNote }}</div>
          </div>
          <div class="detail-item" v-if="currentReport.handledAt">
            <span class="label">处理时间：</span>
            <span>{{ formatDate(currentReport.handledAt) }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
        <el-button
          v-if="currentReport && currentReport.status === 'PENDING'"
          type="primary"
          @click="handleSingleReport(currentReport)"
        >
          立即处理
        </el-button>
      </template>
    </el-dialog>

    <!-- 单个举报处理对话框 -->
    <el-dialog
      v-model="showHandle"
      title="处理举报"
      width="600px"
      destroy-on-close
    >
      <el-form :model="handleForm" label-width="100px">
        <el-form-item label="处理结果">
          <el-radio-group v-model="handleForm.status">
            <el-radio label="CONFIRMED">确认违规</el-radio>
            <el-radio label="DISMISSED">驳回举报</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="处理动作" v-if="handleForm.status === 'CONFIRMED'">
          <el-select v-model="handleForm.action" placeholder="请选择处理动作">
            <el-option label="仅删除内容" value="DELETE"></el-option>
            <el-option label="删除+警告" value="WARN"></el-option>
            <el-option label="删除+禁言3天" value="MUTE_3"></el-option>
            <el-option label="删除+禁言7天" value="MUTE_7"></el-option>
            <el-option label="删除+禁言30天" value="MUTE_30"></el-option>
            <el-option label="删除+封禁账号" value="BAN"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="处理备注">
          <el-input
            v-model="handleForm.handleNote"
            type="textarea"
            :rows="4"
            placeholder="请输入处理备注（可选）"
          ></el-input>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showHandle = false">取消</el-button>
        <el-button type="primary" @click="submitHandle" :loading="submitting">
          确认处理
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量处理对话框 -->
    <el-dialog
      v-model="showBatchHandle"
      title="批量处理举报"
      width="600px"
      destroy-on-close
    >
      <div class="batch-info">
        <el-alert
          :title="`已选择 ${selectedReports.length} 条举报记录`"
          type="info"
          :closable="false"
          show-icon
        ></el-alert>
      </div>

      <el-form :model="batchHandleForm" label-width="100px" style="margin-top: 20px;">
        <el-form-item label="处理结果">
          <el-radio-group v-model="batchHandleForm.status">
            <el-radio label="CONFIRMED">确认违规</el-radio>
            <el-radio label="DISMISSED">驳回举报</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="处理动作" v-if="batchHandleForm.status === 'CONFIRMED'">
          <el-select v-model="batchHandleForm.action" placeholder="请选择处理动作">
            <el-option label="仅删除内容" value="DELETE"></el-option>
            <el-option label="删除+警告" value="WARN"></el-option>
            <el-option label="删除+禁言3天" value="MUTE_3"></el-option>
            <el-option label="删除+禁言7天" value="MUTE_7"></el-option>
            <el-option label="删除+禁言30天" value="MUTE_30"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="处理备注">
          <el-input
            v-model="batchHandleForm.handleNote"
            type="textarea"
            :rows="4"
            placeholder="请输入处理备注（可选）"
          ></el-input>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showBatchHandle = false">取消</el-button>
        <el-button type="primary" @click="submitBatchHandle" :loading="submitting">
          确认批量处理
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Document, Shop } from '@element-plus/icons-vue'
import { adminApi, REPORT_STATUS } from '@/api/admin'

const router = useRouter()


// 当前标签页
const activeTab = ref('high-risk')

// 筛选状态
const filterStatus = ref(null)

// 状态选项
const statusOptions = [
  { value: 'PENDING', label: '待处理' },
  { value: 'UNDER_REVIEW', label: '审核中' },
  { value: 'CONFIRMED', label: '已确认违规' },
  { value: 'DISMISSED', label: '已驳回' },
  { value: 'PROCESSED', label: '已处理' }
]

// 举报列表
const reports = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 选中的举报
const selectedReports = ref([])

// 当前查看的举报详情
const currentReport = ref(null)
const showDetail = ref(false)

// 处理表单
const showHandle = ref(false)
const handleForm = ref({
  status: 'CONFIRMED',
  action: 'DELETE',
  handleNote: ''
})

// 批量处理
const showBatchHandle = ref(false)
const batchHandleForm = ref({
  status: 'CONFIRMED',
  action: 'DELETE',
  handleNote: ''
})

const submitting = ref(false)

// 加载举报列表
const loadReports = async () => {
  loading.value = true
  try {
    let res
    if (activeTab.value === 'high-risk') {
      res = await adminApi.getHighRiskReports(currentPage.value, pageSize.value)
    } else if (activeTab.value === 'pending') {
      res = await adminApi.getPendingReports(currentPage.value, pageSize.value)
    } else {
      res = await adminApi.getAllReports(currentPage.value, pageSize.value, filterStatus.value)
    }

    if (res.code === 200) {
      reports.value = res.data.items || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载举报列表失败:', error)
    ElMessage.error('加载举报列表失败')
  } finally {
    loading.value = false
  }
}

// 标签页切换
const handleTabChange = () => {
  currentPage.value = 1
  selectedReports.value = []
  loadReports()
}

// 分页切换
const handlePageChange = (page) => {
  currentPage.value = page
  loadReports()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedReports.value = selection
}

// 查看详情
const viewDetail = (report) => {
  currentReport.value = report
  showDetail.value = true
}

// 处理单个举报
const handleSingleReport = (report) => {
  currentReport.value = report
  handleForm.value = {
    status: 'CONFIRMED',
    action: 'DELETE',
    handleNote: ''
  }
  showDetail.value = false
  showHandle.value = true
}

// 提交处理
const submitHandle = async () => {
  if (!currentReport.value) return

  if (handleForm.value.status === 'CONFIRMED' && !handleForm.value.action) {
    ElMessage.warning('请选择处理动作')
    return
  }

  submitting.value = true
  try {
    const res = await adminApi.handleReport(currentReport.value.id, handleForm.value)
    if (res.code === 200) {
      ElMessage.success('处理成功')
      showHandle.value = false
      await loadReports()
    } else {
      ElMessage.error(res.message || '处理失败')
    }
  } catch (error) {
    console.error('处理举报失败:', error)
    ElMessage.error('处理失败')
  } finally {
    submitting.value = false
  }
}

// 提交批量处理
const submitBatchHandle = async () => {
  if (selectedReports.value.length === 0) {
    ElMessage.warning('请选择要处理的举报')
    return
  }

  if (batchHandleForm.value.status === 'CONFIRMED' && !batchHandleForm.value.action) {
    ElMessage.warning('请选择处理动作')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量处理 ${selectedReports.value.length} 条举报吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    submitting.value = true
    const reportIds = selectedReports.value.map(r => r.id)
    const res = await adminApi.batchHandleReports(
      reportIds,
      batchHandleForm.value.status,
      batchHandleForm.value.handleNote,
      batchHandleForm.value.action
    )

    if (res.code === 200) {
      ElMessage.success('批量处理成功')
      showBatchHandle.value = false
      selectedReports.value = []
      await loadReports()
    } else {
      ElMessage.error(res.message || '批量处理失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量处理失败:', error)
      ElMessage.error('批量处理失败')
    }
  } finally {
    submitting.value = false
  }
}

// 解析违规信息
const parseViolations = (autoModerationResult) => {
  if (!autoModerationResult) return []
  try {
    const result = JSON.parse(autoModerationResult)
    return result.violations || []
  } catch (e) {
    return []
  }
}

// 获取风险等级类型
const getRiskType = (level) => {
  if (level >= 80) return 'danger'
  if (level >= 60) return 'warning'
  return 'info'
}

// 获取状态类型
const getStatusType = (status) => {
  const types = {
    'PENDING': 'warning',
    'UNDER_REVIEW': 'primary',
    'CONFIRMED': 'danger',
    'DISMISSED': 'info',
    'PROCESSED': 'success'
  }
  return types[status] || 'info'
}

// 获取状态标签
const getStatusLabel = (status) => {
  const labels = {
    'PENDING': '待处理',
    'UNDER_REVIEW': '审核中',
    'CONFIRMED': '已确认违规',
    'DISMISSED': '已驳回',
    'PROCESSED': '已处理'
  }
  return labels[status] || status
}

// 获取原因标签
const getReasonLabel = (reason) => {
  const labels = {
    'SPAM': '垃圾广告',
    'ABUSE': '辱骂攻击',
    'PORNOGRAPHY': '色情低俗',
    'ILLEGAL': '违法违规',
    'FRAUD': '欺诈信息',
    'MISINFORMATION': '虚假信息',
    'HARASSMENT': '骚扰行为',
    'OTHER': '其他原因'
  }
  return labels[reason] || reason
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 跳转到审核页面
const goToActivityReview = () => {
  router.push('/admin/activities/review')
}

const goToWikiReview = () => {
  router.push('/admin/wiki/review')
}

const goToBarReview = () => {
  router.push('/admin/bars/review')
}

onMounted(() => {
  loadReports()
})
</script>

<style scoped>
.admin-moderation {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.admin-header {
  margin-bottom: 30px;
}

.admin-header h1 {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #303133;
}

.section-header {
  margin: 40px 0 20px 0;
}

.section-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.review-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
}

.review-card {
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 8px;
}

.review-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-content {
  text-align: center;
  padding: 20px;
}

.card-icon {
  margin-bottom: 16px;
}

.card-content h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #303133;
  font-weight: 600;
}

.card-content p {
  margin: 0;
  color: #909399;
  font-size: 14px;
  line-height: 1.5;
}

.filter-bar {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.filter-bar .filter-options {
  margin-top: 15px;
}

.filter-bar .batch-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #EBEEF5;
}

.filter-bar .batch-actions .selected-count {
  color: #409EFF;
  font-weight: 500;
}

.reports-table {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.content-preview .content-text {
  margin-bottom: 8px;
  line-height: 1.6;
  word-break: break-word;
}

.content-preview .violations {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.content-preview .violation-tag {
  font-size: 12px;
}

.reporter-info,
.target-user {
  font-size: 14px;
}

.reporter-info .reporter-id,
.reporter-info .user-id,
.target-user .reporter-id,
.target-user .user-id {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.pagination {
  margin-top: 20px;
  justify-content: center;
}

.report-detail .detail-section {
  margin-bottom: 25px;
}

.report-detail .detail-section h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 15px;
  color: #303133;
  border-bottom: 2px solid #409EFF;
  padding-bottom: 8px;
}

.report-detail .detail-section .detail-item {
  display: flex;
  margin-bottom: 12px;
  font-size: 14px;
}

.report-detail .detail-section .detail-item .label {
  min-width: 100px;
  color: #606266;
  font-weight: 500;
}

.report-detail .detail-section .detail-item .content-box,
.report-detail .detail-section .detail-item .description-box,
.report-detail .detail-section .detail-item .note-box {
  flex: 1;
  padding: 10px;
  background: #F5F7FA;
  border-radius: 4px;
  line-height: 1.6;
  word-break: break-word;
}

.report-detail .detail-section .detail-item .violations-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.batch-info {
  margin-bottom: 20px;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table .cell) {
  padding: 8px 0;
}

:deep(.el-dialog__body) {
  padding: 20px;
}
</style>


