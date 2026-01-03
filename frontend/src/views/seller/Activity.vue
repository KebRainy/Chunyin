<template>
  <div class="seller-activity">
    <div class="page-header">
      <h2>活动管理</h2>
      <el-button type="primary" @click="$router.push('/activities/create')">
        <el-icon><Plus /></el-icon>
        创建活动
      </el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 我发布的活动 -->
      <el-tab-pane label="我发布的活动" name="created">
        <div v-loading="loading" class="activity-grid">
          <el-empty v-if="createdActivities.length === 0 && !loading" description="您还没有发布过活动" />
          <el-card
            v-for="activity in createdActivities"
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
              </div>
              <p v-if="activity.remark" class="activity-remark">{{ activity.remark }}</p>
              <div v-if="activity.reviewStatus === 'PENDING'" class="activity-review-status">
                <el-alert type="warning" :closable="false">待审核</el-alert>
              </div>
              <div v-else-if="activity.reviewStatus === 'REJECTED'" class="activity-review-status">
                <el-alert type="error" :closable="false">
                  已拒绝：{{ activity.rejectReason || '无原因' }}
                </el-alert>
              </div>
            </div>
          </el-card>
        </div>

        <el-pagination
          v-if="createdTotal > 0"
          v-model:current-page="createdPage"
          v-model:page-size="pageSize"
          :total="createdTotal"
          layout="total, prev, pager, next"
          @current-change="loadCreatedActivities"
        />
      </el-tab-pane>

      <!-- 全部活动 -->
      <el-tab-pane label="全部活动" name="all">
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
              <el-button type="primary" @click="loadAllActivities" style="margin-left: 10px">
                筛选
              </el-button>
              <el-button @click="resetFilters">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <div v-loading="loading" class="activity-grid">
          <el-empty v-if="allActivities.length === 0 && !loading" description="暂无活动" />
          <el-card
            v-for="activity in allActivities"
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
          v-if="allTotal > 0"
          v-model:current-page="allPage"
          v-model:page-size="pageSize"
          :total="allTotal"
          layout="total, prev, pager, next"
          @current-change="loadAllActivities"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Clock, LocationFilled, User, Plus } from '@element-plus/icons-vue'
import {
  getRecommendedActivities,
  getMyCreatedActivities
} from '@/api/activity'
import { getBeverageList } from '@/api/beverage'
import { searchBarsByName } from '@/api/bar'

const router = useRouter()

const activeTab = ref('created')
const loading = ref(false)

// 我发布的活动
const createdActivities = ref([])
const createdPage = ref(1)
const createdTotal = ref(0)

// 全部活动
const allActivities = ref([])
const allPage = ref(1)
const allTotal = ref(0)

const pageSize = ref(10)

// 筛选条件
const filters = ref({
  barId: null,
  beverageId: null
})

const barList = ref([])
const beverageList = ref([])

// 加载我发布的活动
const loadCreatedActivities = async () => {
  loading.value = true
  try {
    const res = await getMyCreatedActivities(createdPage.value, pageSize.value)
    if (res.code === 200) {
      createdActivities.value = res.data?.items || []
      createdTotal.value = res.data?.total || 0
    }
  } catch (error) {
    console.error('加载我发布的活动失败:', error)
    ElMessage.error(error.response?.data?.message || '加载我发布的活动失败')
  } finally {
    loading.value = false
  }
}

// 加载全部活动
const loadAllActivities = async () => {
  loading.value = true
  try {
    const res = await getRecommendedActivities(
      filters.value.barId,
      filters.value.beverageId,
      allPage.value,
      pageSize.value
    )
    if (res.code === 200) {
      allActivities.value = res.data?.items || []
      allTotal.value = res.data?.total || 0
    }
  } catch (error) {
    console.error('加载全部活动失败:', error)
    ElMessage.error(error.response?.data?.message || '加载全部活动失败')
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
  if (tab === 'created') {
    loadCreatedActivities()
  } else if (tab === 'all') {
    loadAllActivities()
  }
}

// 重置筛选
const resetFilters = () => {
  filters.value = {
    barId: null,
    beverageId: null
  }
  loadAllActivities()
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
    ONGOING: 'info',
    FINISHED: 'info',
    CANCELLED: 'info'
  }
  return typeMap[status] || ''
}

onMounted(() => {
  loadCreatedActivities()
  loadBarList()
  loadBeverageList()
})
</script>

<style scoped>
.seller-activity {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
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
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.activity-remark {
  margin-top: 10px;
  color: #999;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.activity-review-status {
  margin-top: 10px;
}

.activity-organizer {
  color: #909399;
  font-size: 12px;
}
</style>
