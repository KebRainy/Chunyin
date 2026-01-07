<template>
  <div class="wiki-review">
    <div class="page-header">
      <h2 class="art-heading art-heading-h2">Wiki词条审核</h2>
    </div>

    <el-card>
      <el-table v-loading="loading" :data="wikis" stripe>
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <div class="wiki-title">{{ row.title }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="250" show-overflow-tooltip />
        <el-table-column prop="lastEditorName" label="创建者" width="120" />
        <el-table-column prop="updatedAt" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'UNDER_REVIEW'"
              type="success"
              size="small"
              @click="handleApprove(row)"
              :loading="submitting"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 'UNDER_REVIEW'"
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

      <el-pagination
        v-if="total > 0"
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadWikis"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>

    <!-- 拒绝对话框 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="拒绝词条"
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
import { getPendingWikiPages, reviewWikiPage } from '@/api/wiki'
import dayjs from 'dayjs'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const wikis = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const rejectDialogVisible = ref(false)
const currentWiki = ref(null)
const rejectForm = ref({
  rejectReason: ''
})

// 加载待审核词条
const loadWikis = async () => {
  loading.value = true
  try {
    const res = await getPendingWikiPages(page.value, pageSize.value)
    if (res.code === 200 && res.data) {
      wikis.value = res.data.items || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载待审核词条失败')
  } finally {
    loading.value = false
  }
}

// 通过审核
const handleApprove = async (wiki) => {
  try {
    await ElMessageBox.confirm('确定要通过此词条吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })

    submitting.value = true
    try {
      const res = await reviewWikiPage(wiki.id, {
        status: 'PUBLISHED'
      })
      if (res.code === 200) {
        ElMessage.success('审核通过')
        await loadWikis()
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
const handleReject = (wiki) => {
  currentWiki.value = wiki
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
    const res = await reviewWikiPage(currentWiki.value.id, {
      status: 'REJECTED',
      rejectReason: rejectForm.value.rejectReason
    })
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      rejectDialogVisible.value = false
      await loadWikis()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '审核失败')
  } finally {
    submitting.value = false
  }
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'UNDER_REVIEW': '待审核',
    'PUBLISHED': '已审核',
    'REJECTED': '已拒绝',
    'DRAFT': '草稿'
  }
  return statusMap[status] || status
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    'UNDER_REVIEW': 'warning',
    'PUBLISHED': 'success',
    'REJECTED': 'danger',
    'DRAFT': 'info'
  }
  return typeMap[status] || 'info'
}

// 查看详情
const viewDetail = (wiki) => {
  router.push(`/wiki/${wiki.slug}`)
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

onMounted(() => {
  loadWikis()
})
</script>

<style scoped>
.wiki-review {
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(139, 69, 19, 0.1);
}

.page-header h2 {
  margin: 0;
}

.wiki-title {
  font-weight: bold;
}
</style>
