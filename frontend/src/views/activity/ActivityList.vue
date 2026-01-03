<template>
  <div class="activity-list">
    <div class="page-header">
      <h2>{{ isAdminReview ? '活动审核' : '活动中心' }}</h2>
    </div>

    <!-- 管理员审核界面 -->
    <template v-if="isAdminReview">
      <el-card>
        <el-table v-loading="loading" :data="pendingActivities" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="活动信息" min-width="200">
            <template #default="{ row }">
              <div>
                <div class="activity-title">{{ row.beverageName }} 组局</div>
                <div class="activity-time">{{ formatDateTime(row.activityTime) }}</div>
                <div v-if="row.barName" class="activity-bar">酒吧：{{ row.barName }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="发起者" width="120">
            <template #default="{ row }">{{ row.organizerName }}</template>
          </el-table-column>
          <el-table-column label="参与人数" width="120">
            <template #default="{ row }">{{ row.currentParticipants }} / {{ row.maxParticipants }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getReviewStatusType(row.reviewStatus)">{{ getReviewStatusText(row.reviewStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.reviewStatus === 'PENDING'" type="success" size="small" @click="handleApprove(row)">通过</el-button>
              <el-button v-if="row.reviewStatus === 'PENDING'" type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
              <el-button type="primary" size="small" @click="goToDetail(row.id)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="pendingTotal > 0"
          v-model:current-page="pendingPage"
          v-model:page-size="pageSize"
          :total="pendingTotal"
          layout="total, prev, pager, next"
          @current-change="loadPendingActivities"
          style="margin-top: 20px; justify-content: center"
        />
      </el-card>
      
      <!-- 拒绝对话框 -->
      <el-dialog v-model="rejectDialogVisible" title="拒绝活动" width="500px">
        <el-form :model="rejectForm" label-width="100px">
          <el-form-item label="拒绝原因" required>
            <el-input v-model="rejectForm.rejectReason" type="textarea" :rows="4" placeholder="请输入拒绝原因" maxlength="200" show-word-limit />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="rejectDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
        </template>
      </el-dialog>
    </template>

    <!-- 普通用户界面 -->
    <el-tabs v-else v-model="activeTab" @tab-change="handleTabChange">
      <!-- 推荐的活动 -->
      <el-tab-pane label="推荐活动" name="recommended">
        <el-card class="filter-card">
          <el-form :inline="true">
            <el-form-item label="筛选">
              <el-select v-model="filters.barId" placeholder="选择酒吧" clearable style="width: 200px">
                <el-option
                  v-for="bar in barList"
                  :key="bar.id"
                  :label="bar.name"
                  :value="bar.id"
                />
              </el-select>
              <el-select v-model="filters.beverageId" placeholder="选择酒类" clearable style="width: 200px; margin-left: 10px">
                <el-option
                  v-for="beverage in beverageList"
                  :key="beverage.id"
                  :label="beverage.name"
                  :value="beverage.id"
                />
              </el-select>
              <el-button type="primary" @click="loadRecommendedActivities" style="margin-left: 10px">
                筛选
              </el-button>
              <el-button @click="resetFilters">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <div v-loading="loading" class="activity-grid">
          <el-empty v-if="recommendedActivities.length === 0 && !loading" description="暂无推荐活动" />
          <el-card
            v-for="activity in recommendedActivities"
            :key="activity.id"
            class="activity-card"
            shadow="hover"
            @click="goToDetail(activity.id)"
          >
            <div class="activity-info">
              <div class="activity-header">
                <h3 class="activity-title">{{ activity.beverageName }} 组局</h3>
                <el-tag :type="getStatusType(activity.status)">
                  {{ getStatusText(activity.status) }}
                </el-tag>
              </div>
              <div class="activity-meta">
                <p class="activity-time">
                  <el-icon><Clock /></el-icon>
                  {{ formatDateTime(activity.activityTime) }}
                </p>
                <p v-if="activity.barName" class="activity-bar">
                  <el-icon><LocationFilled /></el-icon>
                  {{ activity.barName }}
                </p>
                <p class="activity-participants">
                  <el-icon><User /></el-icon>
                  {{ activity.currentParticipants }} / {{ activity.maxParticipants }} 人
                </p>
                <p v-if="activity.organizerName" class="activity-organizer">
                  发起者：{{ activity.organizerName }}
                </p>
              </div>
              <p v-if="activity.remark" class="activity-remark">{{ activity.remark }}</p>
            </div>
          </el-card>
        </div>

        <el-pagination
          v-if="recommendedTotal > 0"
          v-model:current-page="recommendedPage"
          v-model:page-size="pageSize"
          :total="recommendedTotal"
          layout="total, prev, pager, next"
          @current-change="loadRecommendedActivities"
        />
      </el-tab-pane>

      <!-- 我参与的活动 -->
      <el-tab-pane label="我参与的" name="participated">
        <div v-loading="loading" class="activity-grid">
          <el-empty v-if="participatedActivities.length === 0 && !loading" description="您还没有参与过活动" />
          <el-card
            v-for="activity in participatedActivities"
            :key="activity.id"
            class="activity-card"
            shadow="hover"
            @click="goToDetail(activity.id)"
          >
            <div class="activity-info">
              <div class="activity-header">
                <h3 class="activity-title">{{ activity.beverageName }} 组局</h3>
                <el-tag :type="getStatusType(activity.status)">
                  {{ getStatusText(activity.status) }}
                </el-tag>
              </div>
              <div class="activity-meta">
                <p class="activity-time">
                  <el-icon><Clock /></el-icon>
                  {{ formatDateTime(activity.activityTime) }}
                </p>
                <p v-if="activity.barName" class="activity-bar">
                  <el-icon><LocationFilled /></el-icon>
                  {{ activity.barName }}
                </p>
                <p class="activity-participants">
                  <el-icon><User /></el-icon>
                  {{ activity.currentParticipants }} / {{ activity.maxParticipants }} 人
                </p>
                <p v-if="activity.organizerName" class="activity-organizer">
                  发起者：{{ activity.organizerName }}
                </p>
              </div>
              <p v-if="activity.remark" class="activity-remark">{{ activity.remark }}</p>
            </div>
          </el-card>
        </div>

        <el-pagination
          v-if="participatedTotal > 0"
          v-model:current-page="participatedPage"
          v-model:page-size="pageSize"
          :total="participatedTotal"
          layout="total, prev, pager, next"
          @current-change="loadParticipatedActivities"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, LocationFilled, User } from '@element-plus/icons-vue'
import {
  getRecommendedActivities,
  getMyParticipatedActivities,
  getPendingActivities,
  reviewActivity
} from '@/api/activity'
import { getBeverageList } from '@/api/beverage'
import { searchBarsByName } from '@/api/bar'

const router = useRouter()
const route = useRoute()

// 判断是否是管理员审核页面
const isAdminReview = computed(() => route.path === '/admin/activities/review')

const activeTab = ref('recommended')
const loading = ref(false)

// 推荐活动
const recommendedActivities = ref([])
const recommendedPage = ref(1)
const recommendedTotal = ref(0)

// 我参与的活动
const participatedActivities = ref([])
const participatedPage = ref(1)
const participatedTotal = ref(0)

// 待审核活动（管理员）
const pendingActivities = ref([])
const pendingPage = ref(1)
const pendingTotal = ref(0)
const rejectDialogVisible = ref(false)
const currentActivity = ref(null)
const rejectForm = ref({ rejectReason: '' })

const pageSize = ref(10)

// 筛选条件
const filters = ref({
  barId: null,
  beverageId: null
})

const barList = ref([])
const beverageList = ref([])

// 加载推荐活动
const loadRecommendedActivities = async () => {
  loading.value = true
  try {
    const res = await getRecommendedActivities(
      filters.value.barId,
      filters.value.beverageId,
      recommendedPage.value,
      pageSize.value
    )
    if (res.code === 200) {
      recommendedActivities.value = res.data?.items || []
      recommendedTotal.value = res.data?.total || 0
    }
  } catch (error) {
    console.error('加载推荐活动失败:', error)
    // 401错误不应该导致退出登录，只显示错误消息
    if (error.response?.status === 401) {
      ElMessage.warning('请先登录')
    } else {
      ElMessage.error(error.response?.data?.message || '加载推荐活动失败')
    }
  } finally {
    loading.value = false
  }
}

// 加载我参与的活动
const loadParticipatedActivities = async () => {
  loading.value = true
  try {
    const res = await getMyParticipatedActivities(participatedPage.value, pageSize.value)
    if (res.code === 200) {
      participatedActivities.value = res.data?.items || []
      participatedTotal.value = res.data?.total || 0
    }
  } catch (error) {
    console.error('加载我参与的活动失败:', error)
    // 401错误不应该导致退出登录，只显示错误消息
    if (error.response?.status === 401) {
      ElMessage.warning('请先登录')
    } else {
      ElMessage.error(error.response?.data?.message || '加载我参与的活动失败')
    }
  } finally {
    loading.value = false
  }
}

// 加载酒吧列表
const loadBarList = async () => {
  try {
    // 不传递name参数，后端会返回所有酒吧（限制数量）
    const res = await searchBarsByName({ limit: 100 })
    if (res.code === 200) {
      barList.value = res.data || []
    }
  } catch (error) {
    console.error('加载酒吧列表失败', error)
    // 400错误不应该导致退出登录，只记录错误
    if (error.response?.status === 400) {
      console.warn('参数错误，但不影响登录状态:', error.response?.data?.message)
    }
  }
}

// 加载酒类列表
const loadBeverageList = async () => {
  try {
    const res = await getBeverageList({ limit: 100 })
    if (res.code === 200) {
      beverageList.value = res.data?.items || res.data || []
    }
  } catch (error) {
    console.error('加载酒类列表失败', error)
  }
}

// Tab切换
const handleTabChange = (tab) => {
  if (tab === 'recommended') {
    loadRecommendedActivities()
  } else if (tab === 'participated') {
    loadParticipatedActivities()
  }
}

// 加载待审核活动（管理员）
const loadPendingActivities = async () => {
  loading.value = true
  try {
    const res = await getPendingActivities(pendingPage.value, pageSize.value)
    if (res.code === 200) {
      pendingActivities.value = res.data?.items || []
      pendingTotal.value = res.data?.total || 0
    }
  } catch (error) {
    console.error('加载待审核活动失败:', error)
    // 401/403错误不应该导致退出登录，只显示错误消息
    if (error.response?.status === 401) {
      ElMessage.warning('请先登录')
    } else if (error.response?.status === 403) {
      ElMessage.warning('需要管理员权限')
    } else {
      ElMessage.error(error.response?.data?.message || '加载待审核活动失败')
    }
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
    
    loading.value = true
    try {
      const res = await reviewActivity(activity.id, { status: 'APPROVED' })
      if (res.code === 200) {
        ElMessage.success('审核通过')
        await loadPendingActivities()
      }
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '审核失败')
    } finally {
      loading.value = false
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
  
  loading.value = true
  try {
    const res = await reviewActivity(currentActivity.value.id, {
      status: 'REJECTED',
      rejectReason: rejectForm.value.rejectReason
    })
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      rejectDialogVisible.value = false
      await loadPendingActivities()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 重置筛选
const resetFilters = () => {
  filters.value = {
    barId: null,
    beverageId: null
  }
  loadRecommendedActivities()
}

// 跳转到详情页
const goToDetail = (id) => {
  router.push(`/activities/${id}`)
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

// 获取审核状态文本
const getReviewStatusText = (status) => {
  const statusMap = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  }
  return statusMap[status] || status
}

// 获取审核状态类型
const getReviewStatusType = (status) => {
  const typeMap = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return typeMap[status] || ''
}

onMounted(() => {
  if (isAdminReview.value) {
    loadPendingActivities()
  } else {
    loadRecommendedActivities()
    loadBarList()
    loadBeverageList()
  }
})
</script>

<style scoped>
.activity-list {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.filter-card {
  margin-bottom: 20px;
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.activity-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.activity-card:hover {
  transform: translateY(-5px);
}

.activity-info {
  padding: 10px;
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.activity-title {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
}

.activity-meta {
  margin: 10px 0;
}

.activity-meta p {
  margin: 5px 0;
  color: #666;
  display: flex;
  align-items: center;
  gap: 5px;
}

.activity-remark {
  margin-top: 10px;
  color: #999;
  font-size: 14px;
}

.activity-review-status {
  margin-top: 10px;
}
</style>

