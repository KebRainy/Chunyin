<template>
  <div class="wiki-review">
    <div class="review-header">
      <h2>Wiki词条审核</h2>
      <p class="subtitle">待审核的词条列表</p>
    </div>

    <div class="review-content">
      <el-table :data="wikis" v-loading="loading" stripe>
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <span class="wiki-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="250" show-overflow-tooltip />
        <el-table-column prop="lastEditorName" label="创建者" width="120" />
        <el-table-column prop="updatedAt" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'">
              {{ row.status === 'PUBLISHED' ? '已审核' : '待审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">查看详情</el-button>
            <el-button size="small" type="success" @click="handleApprove(row)" :loading="submitting"
              :disabled="row.status === 'PUBLISHED'">
              通过
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="total > 0">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]"
          :total="total" layout="total, sizes, prev, pager, next" @size-change="loadWikis"
          @current-change="loadWikis" />
      </div>

      <div v-if="!loading && wikis.length === 0" class="empty-state">
        <p>暂无待审核的词条</p>
      </div>
    </div>
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

// 查看详情
const viewDetail = (wiki) => {
  router.push(`/wiki/${wiki.slug}`)
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadWikis()
})
</script>

<style scoped>
.wiki-review {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.review-header {
  margin-bottom: 24px;
}

.review-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #1f2d3d;
}

.review-header .subtitle {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.review-content {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.wiki-title {
  font-weight: 500;
  color: #303133;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}
</style>
