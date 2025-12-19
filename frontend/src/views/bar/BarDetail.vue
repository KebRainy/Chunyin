<template>
  <div class="bar-detail" v-loading="loading">
    <div v-if="bar" class="detail-container">
      <!-- 酒吧基本信息 -->
      <el-card class="info-card">
        <div class="bar-header">
          <h1>{{ bar.name }}</h1>
          <el-tag v-if="bar.status === 'PENDING'" type="warning">待审核</el-tag>
          <el-tag v-else-if="bar.status === 'APPROVED'" type="success">已通过</el-tag>
          <el-tag v-else-if="bar.status === 'REJECTED'" type="danger">已拒绝</el-tag>
        </div>

        <div class="rating-section">
          <el-rate 
            v-model="bar.avgRating" 
            disabled 
            show-score 
            :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
          />
          <span class="review-count">{{ bar.reviewCount }}条评价</span>
        </div>

        <el-descriptions :column="1" border class="bar-descriptions">
          <el-descriptions-item label="地址">
            <el-icon><LocationFilled /></el-icon>
            {{ bar.address }}
          </el-descriptions-item>
          
          <el-descriptions-item label="所在地区">
            {{ bar.province }} {{ bar.city }} {{ bar.district || '' }}
          </el-descriptions-item>
          
          <el-descriptions-item v-if="bar.openingTime && bar.closingTime" label="营业时间">
            <el-icon><Clock /></el-icon>
            {{ formatTime(bar.openingTime) }} - {{ formatTime(bar.closingTime) }}
          </el-descriptions-item>
          
          <el-descriptions-item label="联系电话">
            <el-icon><Phone /></el-icon>
            {{ bar.contactPhone }}
          </el-descriptions-item>
          
          <el-descriptions-item v-if="bar.mainBeverages" label="主营酒类">
            {{ bar.mainBeverages }}
          </el-descriptions-item>
          
          <el-descriptions-item v-if="bar.description" label="简介">
            {{ bar.description }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 评价区域 -->
      <el-card class="review-card">
        <template #header>
          <div class="card-header">
            <h3>用户评价</h3>
            <el-button 
              v-if="isLoggedIn" 
              type="primary" 
              @click="showReviewDialog = true"
            >
              写评价
            </el-button>
          </div>
        </template>

        <div v-if="reviews.length === 0" class="empty-reviews">
          <el-empty description="暂无评价" />
        </div>

        <div v-else class="reviews-list">
          <div 
            v-for="review in reviews" 
            :key="review.id" 
            class="review-item"
          >
            <div class="review-header">
              <div class="user-info">
                <el-avatar 
                  :src="review.user?.avatarUrl" 
                  :size="40"
                />
                <div class="user-details">
                  <span class="username">{{ review.user?.username }}</span>
                  <el-rate 
                    v-model="review.rating" 
                    disabled 
                    :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
                  />
                </div>
              </div>
              <div class="review-meta">
                <span class="review-time">{{ formatDate(review.createdAt) }}</span>
                <el-button 
                  v-if="currentUserId === review.userId"
                  type="danger" 
                  size="small" 
                  text
                  @click="handleDeleteReview(review.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
            <p v-if="review.content" class="review-content">{{ review.content }}</p>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 添加评价对话框 -->
    <el-dialog 
      v-model="showReviewDialog" 
      title="写评价" 
      width="500px"
    >
      <el-form :model="reviewForm" :rules="reviewRules" ref="reviewFormRef">
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
            placeholder="分享您的体验..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showReviewDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReview" :loading="submitting">
          提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { getBarDetail, getBarReviews, addBarReview, deleteBarReview } from '@/api/bar'
import { LocationFilled, Clock, Phone } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/modules/user'

export default {
  name: 'BarDetail',
  components: {
    LocationFilled,
    Clock,
    Phone
  },
  setup() {
    const userStore = useUserStore()
    return {
      userStore
    }
  },
  data() {
    return {
      bar: null,
      reviews: [],
      loading: false,
      submitting: false,
      showReviewDialog: false,
      reviewForm: {
        rating: 0,
        content: ''
      },
      reviewRules: {
        rating: [
          { required: true, message: '请选择评分', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    isLoggedIn() {
      return this.userStore.isLoggedIn
    },
    currentUser() {
      return this.userStore.userInfo
    },
    currentUserId() {
      return this.currentUser?.id
    }
  },
  mounted() {
    this.loadBarDetail()
    this.loadReviews()
  },
  methods: {
    async loadBarDetail() {
      this.loading = true
      try {
        const { data } = await getBarDetail(this.$route.params.id)
        this.bar = data
      } catch (error) {
        console.error('加载酒吧详情失败:', error)
        ElMessage.error(error.message || '加载失败')
      } finally {
        this.loading = false
      }
    },
    
    async loadReviews() {
      try {
        const { data } = await getBarReviews(this.$route.params.id)
        this.reviews = data || []
      } catch (error) {
        console.error('加载评价失败:', error)
      }
    },
    
    async submitReview() {
      const valid = await this.$refs.reviewFormRef.validate().catch(() => false)
      if (!valid) return
      
      this.submitting = true
      try {
        await addBarReview({
          barId: this.bar.id,
          rating: this.reviewForm.rating,
          content: this.reviewForm.content
        })
        
        ElMessage.success('评价成功')
        this.showReviewDialog = false
        this.reviewForm = { rating: 0, content: '' }
        
        // 重新加载数据
        await Promise.all([
          this.loadBarDetail(),
          this.loadReviews()
        ])
      } catch (error) {
        console.error('提交评价失败:', error)
        ElMessage.error(error.message || '提交失败')
      } finally {
        this.submitting = false
      }
    },
    
    async handleDeleteReview(reviewId) {
      try {
        await ElMessageBox.confirm('确定要删除这条评价吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        await deleteBarReview(reviewId)
        ElMessage.success('删除成功')
        
        // 重新加载数据
        await Promise.all([
          this.loadBarDetail(),
          this.loadReviews()
        ])
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除评价失败:', error)
          ElMessage.error(error.message || '删除失败')
        }
      }
    },
    
    formatTime(time) {
      if (!time) return ''
      return time.substring(0, 5)
    },
    
    formatDate(dateTime) {
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
  }
}
</script>

<style scoped>
.bar-detail {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.detail-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.bar-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
}

.bar-header h1 {
  margin: 0;
  font-size: 28px;
  color: #333;
}

.rating-section {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.review-count {
  color: #999;
  font-size: 14px;
}

.bar-descriptions {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
}

.empty-reviews {
  padding: 40px 0;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.review-item {
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 8px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.user-info {
  display: flex;
  gap: 10px;
  align-items: center;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.username {
  font-weight: 500;
  color: #333;
}

.review-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-time {
  color: #999;
  font-size: 12px;
}

.review-content {
  margin: 0;
  color: #666;
  line-height: 1.6;
}
</style>

