<template>
  <div class="wiki-landing">
    <section class="wiki-hero">
      <div class="hero-content">
        <p class="eyebrow">醇饮百科</p>
        <h1>搜一搜，就能找到任何关于酒饮的答案</h1>
        <p class="subtitle">社区共建 · 实时更新 · 支持多端访问</p>
      </div>
      <div class="hero-search">
        <el-input v-model="keyword" placeholder="输入关键词查找条目，如“IPA”“威士忌调和”" size="large" @keyup.enter="searchPages"
          clearable>
          <template #append>
            <el-button type="primary" @click="searchPages">
              <el-icon>
                <Search />
              </el-icon>
            </el-button>
          </template>
        </el-input>
        <div class="hero-actions">
          <el-button v-if="userStore.isLoggedIn" type="primary" plain @click="goEditor">
            + 新建条目
          </el-button>
          <el-button v-if="userStore.isLoggedIn" type="warning" @click="handleBatchImport">
            ! 一键导入测试数据
          </el-button>
        </div>
      </div>
      <div class="hero-stats">
        <div class="stat-card">
          <p class="value">{{ wikiStats.entryCount }}</p>
          <p class="label">词条数量</p>
        </div>
        <div class="stat-card">
          <p class="value">{{ wikiStats.editCount }}</p>
          <p class="label">有效编辑</p>
        </div>
        <div class="stat-card">
          <p class="value">{{ wikiStats.contributorCount }}</p>
          <p class="label">贡献者</p>
        </div>
      </div>
    </section>

    <section class="wiki-content">
      <div class="list-panel">
        <div class="section-header">
          <h3>{{ keyword ? '搜索结果' : '精选词条' }}</h3>
          <span class="count">共 {{ total }} 条</span>
        </div>
        <div v-if="loading" class="loading">
          <el-skeleton :rows="4" animated />
        </div>
        <div v-else class="wiki-list">
          <div v-for="item in pages" :key="item.id" :class="['wiki-item', { active: currentPage?.id === item.id }]"
            @click="selectPage(item)">
            <h4>{{ item.title }}</h4>
            <p>{{ item.summary || '这是一条等待补充的条目' }}</p>
            <small>更新 · {{ formatTime(item.updatedAt) }}</small>
            <el-tag size="small" v-if="item.status === 'PUBLISHED'" type="success">已发布</el-tag>
          </div>
        </div>
        <div class="pager" v-if="total > pageSize">
          <el-pagination layout="prev, pager, next" :current-page="currentPageNum" :page-size="pageSize" :total="total"
            @current-change="handlePageChange" />
        </div>
      </div>

      <div class="preview-panel" v-if="currentPage">
        <div class="preview-header">
          <div>
            <p class="eyebrow">条目预览</p>
            <h2>{{ currentPage.title }}</h2>
          </div>
          <div class="preview-actions">
            <el-button size="small" @click="viewDetail(currentPage)">查看详情</el-button>
            <el-button size="small" type="primary" v-if="canEdit && currentPage" @click="editCurrent">
              编辑
            </el-button>
          </div>
        </div>
        <p class="preview-summary">{{ currentPage.summary || '这条词条尚未填写摘要' }}</p>
        <div class="preview-content" v-html="formatContent(currentPage.content)"></div>
        <p class="preview-meta">最后编辑：{{ currentPage.lastEditorName || '系统' }} · {{ formatTime(currentPage.updatedAt) }}
        </p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { fetchWikiPages, fetchWikiStats, createWikiPage } from '@/api/wiki'
import { importData } from "../../assets/WikiListData.js"
import { useUserStore } from '@/store/modules/user'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

const router = useRouter()
const userStore = useUserStore()

const keyword = ref('')
const pages = ref([])
const currentPageNum = ref(1)
const pageSize = ref(6)
const total = ref(0)
const currentPage = ref(null)
const loading = ref(false)
const wikiStats = ref({
  entryCount: 0,
  editCount: 0,
  contributorCount: 0
})
const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true
})

const canEdit = computed(() => {
  return userStore.isLoggedIn
})

const loadPages = async () => {
  loading.value = true
  try {
    const res = await fetchWikiPages({
      page: currentPageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
    })
    pages.value = res.data.items || []
    total.value = res.data.total || 0
    currentPage.value = pages.value[0] || null
  } catch (error) {
    // ignore
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const res = await fetchWikiStats()
    wikiStats.value = res.data || wikiStats.value
  } catch {
    // ignore
  }
}

const searchPages = () => {
  currentPageNum.value = 1
  loadPages()
}

const handlePageChange = (page) => {
  currentPageNum.value = page
  loadPages()
}

const selectPage = (item) => {
  currentPage.value = item
}

const goEditor = () => {
  router.push('/wiki/edit')
}

const editCurrent = () => {
  if (currentPage.value) {
    router.push(`/wiki/edit/${currentPage.value.id}`)
  }
}

const viewDetail = (item) => {
  router.push(`/wiki/${item.slug}`)
}

const formatContent = (content) => {
  if (!content) return '<p class="empty">这个条目暂时没有正文</p>'
  const snippet = content.split('\n').slice(0, 8).join('\n')
  return DOMPurify.sanitize(md.render(snippet))
}

// 不添加数据需要隐藏按钮：
// <el-button v-if="userStore.isLoggedIn" type="warning" @click="handleBatchImport">
//  ! 一键导入测试数据
//  </el-button>

const handleBatchImport = async () => {
  const dataList = importData;
  console.log(`开始批量导入，共 ${dataList.length} 条数据...`);
  
  // 先获取所有现有的wiki页面，用于检查重复
  let existingTitles = new Set();
  try {
    console.log('正在检查现有词条...');
    // 获取足够多的页面来检查重复（假设不超过1000条）
    const res = await fetchWikiPages({
      page: 1,
      pageSize: 1000,
      keyword: undefined
    });
    if (res.data && res.data.items) {
      existingTitles = new Set(res.data.items.map(page => page.title));
      console.log(`已找到 ${existingTitles.size} 个现有词条`);
    }
  } catch (error) {
    console.warn('获取现有词条列表失败，将跳过重复检查', error);
  }

  let successCount = 0;
  let skipCount = 0;
  let failCount = 0;

  for (const item of dataList) {
    try {
      // 检查是否已存在（通过title判断）
      if (existingTitles.has(item.title)) {
        console.warn(`⚠️ 跳过: [${item.title}] 已存在`);
        skipCount++;
        continue;
      }

      // 构建符合API接口的 payload
      // 注意：status、slug、last_editor_name、last_editor_id 由后端自动处理
      // - status 会设置为 UNDER_REVIEW（需要审核）
      // - slug 会基于 title 自动生成
      // - last_editor_id 和 last_editor_name 会使用当前登录用户
      const payload = {
        title: item.title,
        summary: item.description,  // 将 description 映射到 summary
        content: item.content,
      };

      await createWikiPage(payload);
      console.log(`✅ 成功: [${item.title}]`);
      successCount++;
      // 添加到已存在列表，避免同一次导入中重复
      existingTitles.add(item.title);
    } catch (e) {
      console.error(`❌ 失败: [${item.title}]`, e);
      failCount++;
    }
  }

  console.log(`批量导入完成！成功: ${successCount} 条，跳过: ${skipCount} 条，失败: ${failCount} 条。`);
  
  // 刷新列表和统计
  await loadPages();
  await loadStats();
};

const formatTime = (time) => dayjs(time).format('YYYY.MM.DD')

onMounted(() => {
  loadPages()
  loadStats()
})
</script>

<style scoped>
.wiki-landing {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px 60px;
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.wiki-hero {
  background: #fff;
  padding: 32px;
  border-radius: 32px;
  border: 1px solid #eceff5;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.07);
}

.hero-content h1 {
  font-size: 32px;
  margin: 12px 0;
  color: #1f2d3d;
}

.hero-content .eyebrow {
  text-transform: uppercase;
  letter-spacing: 2px;
  font-size: 12px;
  color: #606266;
}

.hero-content .subtitle {
  color: #606266;
  margin-bottom: 20px;
}

.hero-search {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero-stats {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.stat-card {
  background: #fafafa;
  border-radius: 18px;
  padding: 16px;
  text-align: center;
  border: 1px solid #f0f2f5;
}

.stat-card .value {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
  color: #1f2d3d;
}

.stat-card .label {
  margin: 4px 0 0;
  color: #909399;
  font-size: 13px;
}

.wiki-content {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 20px;
}

.list-panel,
.preview-panel {
  background: #fff;
  border-radius: 28px;
  padding: 24px;
  border: 1px solid #eceff5;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
}

.count {
  font-size: 12px;
  color: #909399;
}

.wiki-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.wiki-item {
  padding: 16px 18px;
  border: 1px solid #f0f2f5;
  border-radius: 18px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #fafafa;
}

.wiki-item.active,
.wiki-item:hover {
  border-color: #2f54eb;
  box-shadow: 0 10px 22px rgba(47, 84, 235, 0.12);
}

.wiki-item h4 {
  margin: 0 0 8px 0;
}

.wiki-item p {
  margin: 0;
  color: #606266;
}

.wiki-item small {
  display: block;
  margin-top: 8px;
  color: #909399;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.preview-summary {
  color: #606266;
  margin-bottom: 16px;
}

.preview-content {
  line-height: 1.8;
  color: #303133;
  border-radius: 12px;
  background: #f9fafb;
  padding: 16px;
  margin-bottom: 12px;
}

.preview-meta {
  color: #909399;
  font-size: 12px;
}

.pager {
  margin-top: 16px;
  text-align: center;
}

@media (max-width: 960px) {
  .wiki-content {
    grid-template-columns: 1fr;
  }
}
</style>
