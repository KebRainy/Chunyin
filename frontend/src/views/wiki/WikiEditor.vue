<template>
  <div class="editor-page">
    <el-card>
      <template #header>
        <div class="header">
          <div>
            <p class="eyebrow">{{ isEdit ? '编辑条目' : '新建条目' }}</p>
            <h2>{{ form.title || '未命名词条' }}</h2>
          </div>
          <div class="header-actions">
            <el-button @click="goBack">返回列表</el-button>
            <el-button type="primary" :loading="loading" @click="handleSubmit">
              {{ isEdit ? '提交更新' : '发布词条' }}
            </el-button>
          </div>
        </div>
      </template>
      <el-form :model="form" label-width="80px" class="editor-form">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            placeholder="简要概述条目"
          />
        </el-form-item>
        <el-form-item label="正文">
          <div class="editor-grid">
            <div class="editor-panel">
              <div class="toolbar">
                <el-upload
                  class="upload-btn"
                  action="/api/files/upload?category=WIKI"
                  :show-file-list="false"
                  :before-upload="beforeUpload"
                  :on-success="handleUploadSuccess"
                  :on-error="handleUploadError"
                >
                  <el-button text :loading="uploadingImage">
                    {{ uploadingImage ? '上传中...' : '插入图片' }}
                  </el-button>
                </el-upload>
              </div>
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="18"
                class="markdown-input"
                placeholder="支持 Markdown 语法，使用 # ## ### 定义标题，使用 ![](url) 插入图片"
              />
              <div class="uploaded-images" v-if="uploadedImages.length">
                <p>已上传图片</p>
                <div class="images-list">
                  <div
                    v-for="item in uploadedImages"
                    :key="item.uid"
                    class="image-thumb"
                  >
                    <img :src="item.url" :alt="item.url">
                    <el-button text type="danger" size="small" @click="removeImage(item)">
                      删除
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
            <div class="preview-panel">
              <div class="preview-header">
                <span>实时预览</span>
              </div>
              <div class="preview-content" v-html="previewHtml"></div>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, computed, ref, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createWikiPage, fetchWikiPage, updateWikiPage } from '@/api/wiki'
import { deleteFile } from '@/api/file'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const uploadingImage = ref(false)
const uploadedImages = ref([])
const form = reactive({
  title: '',
  summary: '',
  content: ''
})
let syncTimer = null

const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true
})

const previewHtml = computed(() => {
  if (!form.content) {
    return '<p class="empty">开始输入内容，实时预览会显示在这里</p>'
  }
  return DOMPurify.sanitize(md.render(form.content))
})

const isEdit = computed(() => !!route.params.id)

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const sizeOk = file.size / 1024 / 1024 < 5
  if (!isImage) ElMessage.error('只能上传图片')
  if (!sizeOk) ElMessage.error('图片需小于 5MB')
  if (isImage && sizeOk) {
    uploadingImage.value = true
  }
  return isImage && sizeOk
}

const handleUploadSuccess = (response) => {
  uploadingImage.value = false
  if (response.code === 200 && response.data?.url) {
    const url = response.data.url
    const snippet = `![图片说明](${url})`
    form.content = form.content ? `${form.content}\n\n${snippet}` : snippet
    ElMessage.success('图片已插入')
  } else {
    ElMessage.error('上传失败')
  }
}

const handleUploadError = () => {
  uploadingImage.value = false
  ElMessage.error('上传失败，请稍后再试')
}

const removeImage = async (item) => {
  const escapedUrl = item.url.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`!\\[[^\\]]*\\]\\(${escapedUrl}\\)`, 'g')
  form.content = (form.content || '').replace(regex, '').replace(/\n{3,}/g, '\n\n').trim()
  if (item.uuid) {
    try {
      await deleteFile(item.uuid)
      ElMessage.success('图片已删除')
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  } else {
    ElMessage.success('图片引用已移除')
  }
}

const extractImagesFromContent = () => {
  if (!form.content) {
    uploadedImages.value = []
    return
  }
  const regex = /!\[[^\]]*]\((.*?)\)/g
  const seen = new Set()
  const items = []
  let match
  while ((match = regex.exec(form.content)) !== null) {
    const url = match[1]?.trim()
    if (!url || seen.has(url)) continue
    seen.add(url)
    const uuidMatch = url.match(/\/(?:api\/)?files\/([a-zA-Z0-9-]+)/)
    items.push({
      uid: uuidMatch ? uuidMatch[1] : `${url}-${items.length}`,
      url,
      uuid: uuidMatch ? uuidMatch[1] : null
    })
  }
  uploadedImages.value = items
}

const loadPage = async () => {
  if (!isEdit.value) return
  const res = await fetchWikiPage(route.params.id)
  Object.assign(form, {
    title: res.data.title,
    summary: res.data.summary,
    content: res.data.content
  })
}

const handleSubmit = async () => {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('标题和正文不能为空')
    return
  }
  loading.value = true
  try {
    if (isEdit.value) {
      await updateWikiPage(route.params.id, form)
      ElMessage.success('已提交修改，等待审核')
    } else {
      await createWikiPage(form)
      ElMessage.success('已提交，等待审核')
    }
    router.push('/wiki')
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error(error.response?.data?.message || '提交失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/wiki')
}

onMounted(() => {
  loadPage()
})

watch(
  () => form.content,
  () => {
    if (syncTimer) {
      clearTimeout(syncTimer)
    }
    syncTimer = setTimeout(() => {
      extractImagesFromContent()
    }, 200)
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  if (syncTimer) {
    clearTimeout(syncTimer)
  }
})
</script>

<style scoped>
.editor-page {
  max-width: 1100px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.eyebrow {
  font-size: 12px;
  color: #909399;
  margin: 0 0 4px;
  letter-spacing: 1px;
}

.editor-form {
  margin-top: 12px;
}

.editor-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.editor-panel,
.preview-panel {
  border: 1px solid #ebeef5;
  border-radius: 16px;
  padding: 16px;
  background: #fafafa;
  display: flex;
  flex-direction: column;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.markdown-input :deep(textarea) {
  font-family: 'Fira Code', 'JetBrains Mono', Consolas, monospace;
}

.uploaded-images {
  margin-top: 16px;
}

.images-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.image-thumb {
  width: 120px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.image-thumb img {
  width: 100%;
  height: 80px;
  object-fit: cover;
}

.preview-header {
  font-weight: 600;
  margin-bottom: 8px;
  color: #606266;
}

.preview-content {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  overflow: auto;
  max-height: 520px;
}

.preview-content :deep(h1),
.preview-content :deep(h2),
.preview-content :deep(h3) {
  margin: 16px 0 8px;
}

.preview-content :deep(img) {
  max-width: 100%;
  border-radius: 12px;
}

@media (max-width: 900px) {
  .editor-grid {
    grid-template-columns: 1fr;
  }
}
</style>