<template>
  <div class="home">
    <div class="grid">
      <div class="left-column">
        <el-card class="welcome-card">
          <h1>欢迎来到醇饮圈</h1>
          <p>记录你的杯中故事，分享最新的线下活动与酒款体验。</p>
          <el-button type="primary" size="large" @click="goToWiki">
            查看 Wiki 精选
          </el-button>
        </el-card>

        <el-card class="share-card" v-if="userStore.isLoggedIn">
          <template #header>
            <div class="share-header">
              <span>发布圈子动态</span>
              <el-tag type="success">可附带图片与位置</el-tag>
            </div>
          </template>
          <el-form :model="shareForm" ref="shareFormRef">
            <el-form-item>
              <el-input
                v-model="shareForm.content"
                type="textarea"
                :rows="4"
                placeholder="此刻的风味、心情或线下活动，尽情分享吧～"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="shareForm.location"
                placeholder="所在城市 / 店铺（可选）"
              >
                <template #prefix>
                  <el-icon><Location /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-upload
              class="upload-list"
              :action="uploadUrl"
              list-type="picture-card"
              :on-success="handleUploadSuccess"
              :on-remove="handleUploadRemove"
              :file-list="uploadList"
              name="file"
              :with-credentials="true"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <el-button type="primary" :loading="shareLoading" @click="publishShare">
              发布
            </el-button>
          </el-form>
        </el-card>
        <el-card v-else class="share-card">
          <p>登录后即可分享图文、参加线下活动，与同好互动。</p>
          <el-button type="primary" @click="goLogin">立即登录</el-button>
        </el-card>

        <el-card class="feed-card">
          <template #header>
            <div class="feed-header">
              <h3>圈子新动态</h3>
              <el-button link @click="refreshFeed">
                <el-icon><Refresh /></el-icon>刷新
              </el-button>
            </div>
          </template>
          <el-skeleton v-if="loading" animated :count="3">
            <template #template>
              <div class="skeleton-item">
                <el-skeleton-item variant="text" style="width: 40%" />
                <el-skeleton-item variant="text" style="width: 80%; margin: 8px 0" />
                <el-skeleton-item variant="text" style="width: 30%" />
              </div>
            </template>
          </el-skeleton>
          <div v-else>
            <el-empty v-if="feeds.length === 0" description="暂无动态，快来发布第一条吧" />
            <div v-for="item in feeds" :key="item.id" class="feed-item">
              <div class="feed-author" @click="goUser(item.author.id)">
                <el-avatar :size="40" :src="item.author.avatarUrl">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <div>
                  <div class="name">{{ item.author.username }}</div>
                  <div class="meta">
                    {{ formatTime(item.createdAt) }}
                    · {{ item.location || '未知地点' }}
                    · {{ item.ipAddressMasked }}
                  </div>
                </div>
              </div>
              <p class="feed-content">{{ item.content }}</p>
              <div v-if="item.imageUrls?.length" class="feed-images">
                <el-image
                  v-for="(img, index) in item.imageUrls"
                  :key="index"
                  :src="img"
                  fit="cover"
                  :preview-src-list="item.imageUrls"
                  hide-on-click-modal
                />
              </div>
            </div>
            <el-pagination
              v-if="feeds.length"
              layout="prev, pager, next"
              :page-size="pageSize"
              :current-page="page"
              :total="total"
              @current-change="handlePageChange"
            />
          </div>
        </el-card>
      </div>

      <div class="right-column">
        <el-card class="quick-card">
          <h3>Wiki 精选</h3>
          <ul>
            <li>经典酒款、调饮方法一文掌握</li>
            <li>用户协作完善条目，记录更多故事</li>
            <li>立即前往编辑自己的条目</li>
          </ul>
          <el-button type="primary" plain @click="goToWiki">打开 Wiki</el-button>
        </el-card>

        <el-card class="quick-card">
          <h3>线下活动 / 购酒情报</h3>
          <p>圈友们会在这里分享线下品鉴、展会信息以及靠谱购酒渠道，别错过。</p>
          <el-button type="success" plain @click="goProfile">我的动态中心</el-button>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import { fetchCirclePosts, createCirclePost } from '@/api/circle'
import { useUserStore } from '@/store/modules/user'
import { ElMessage } from 'element-plus'

dayjs.extend(relativeTime)

const router = useRouter()
const userStore = useUserStore()

const shareForm = reactive({
  content: '',
  location: '',
  images: []
})
const shareFormRef = ref()
const shareLoading = ref(false)
const uploadList = ref([])
const uploadUrl = '/api/files/upload'

const feeds = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(5)
const total = ref(0)

const loadFeeds = async () => {
  loading.value = true
  try {
    const res = await fetchCirclePosts({ page: page.value, pageSize: pageSize.value })
    feeds.value = res.data.items
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const refreshFeed = () => {
  page.value = 1
  loadFeeds()
}

const handlePageChange = (val) => {
  page.value = val
  loadFeeds()
}

const publishShare = () => {
  if (!shareForm.content.trim()) {
    ElMessage.warning('请先输入想分享的内容')
    return
  }
  shareLoading.value = true
  createCirclePost({
    content: shareForm.content,
    location: shareForm.location,
    imageUrls: shareForm.images
  })
    .then(() => {
      ElMessage.success('发布成功')
      shareForm.content = ''
      shareForm.location = ''
      shareForm.images = []
      uploadList.value = []
      refreshFeed()
    })
    .finally(() => {
      shareLoading.value = false
    })
}

const handleUploadSuccess = (response, file, fileList) => {
  shareForm.images.push(response.data.url)
  uploadList.value = fileList
}

const handleUploadRemove = (file, fileList) => {
  const url = file.response?.data?.url || file.url
  shareForm.images = shareForm.images.filter((item) => item !== url)
  uploadList.value = fileList
}

const goToWiki = () => router.push('/wiki')
const goProfile = () => router.push('/user/profile')
const goLogin = () => router.push('/login')
const goUser = (id) => router.push(`/users/${id}`)

const formatTime = (time) => dayjs(time).fromNow()

onMounted(() => {
  loadFeeds()
})
</script>

<style scoped>
.home {
  max-width: 1200px;
  margin: 0 auto;
}

.grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.welcome-card {
  margin-bottom: 20px;
}

.share-card {
  margin-bottom: 20px;
}

.share-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-list {
  margin-bottom: 10px;
}

.feed-card {
  min-height: 300px;
}

.feed-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.feed-item {
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.feed-item:last-child {
  border-bottom: none;
}

.feed-author {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
  cursor: pointer;
}

.feed-author .name {
  font-weight: 600;
}

.feed-author .meta {
  font-size: 12px;
  color: #909399;
}

.feed-content {
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 10px;
}

.feed-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.feed-images .el-image {
  width: 100%;
  height: 120px;
  border-radius: 6px;
  overflow: hidden;
}

.right-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.quick-card ul {
  margin: 10px 0;
  padding-left: 18px;
}

.quick-card li {
  margin: 4px 0;
  color: #606266;
}
</style>
