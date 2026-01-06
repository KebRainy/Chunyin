<template>
  <div class="editor-page">
    <el-card>
      <template #header>
        <div class="header">
          <div>
            <p class="eyebrow">{{ isEdit ? '编辑条目' : '新建条目' }}</p>
            <h2>{{ form.title || '未命名词条' }}</h2>
            <div class="status-bar" v-if="hasUnsavedChanges || autoSaveStatus">
              <el-icon v-if="autoSaveStatus === 'saving'">
                <Loading />
              </el-icon>
              <el-icon v-else-if="autoSaveStatus === 'saved'">
                <CircleCheck />
              </el-icon>
              <span class="status-text">
                {{ autoSaveStatus === 'saving' ? '正在保存...' : autoSaveStatus === 'saved' ? '已自动保存' : '有未保存的更改' }}
              </span>
            </div>
          </div>
          <div class="header-actions">
            <el-button @click="handleGoBack">返回列表</el-button>
            <el-button type="primary" :loading="loading" @click="handleSubmit">
              {{ isEdit ? '提交更新' : '发布词条' }}
            </el-button>
          </div>
        </div>
      </template>
      <el-form :model="form" label-width="80px" class="editor-form">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="500" show-word-limit
            placeholder="简要概述条目" />
        </el-form-item>
        <el-form-item label="正文">
          <div class="editor-grid">
            <div class="editor-panel">
              <div class="toolbar">
                <div class="toolbar-group">
                  <el-button-group>
                    <el-button size="small" @click="insertMarkdown('**', '**', '加粗')" title="加粗 (Ctrl+B)">
                      <strong>B</strong>
                    </el-button>
                    <el-button size="small" @click="insertMarkdown('*', '*', '斜体')" title="斜体 (Ctrl+I)">
                      <em>I</em>
                    </el-button>
                    <el-button size="small" @click="insertMarkdown('`', '`', '行内代码')" title="行内代码">
                      &lt;/&gt;
                    </el-button>
                  </el-button-group>
                </div>
                <div class="toolbar-group">
                  <el-button-group>
                    <el-button size="small" @click="insertMarkdown('# ', '', '一级标题')" title="一级标题">
                      H1
                    </el-button>
                    <el-button size="small" @click="insertMarkdown('## ', '', '二级标题')" title="二级标题">
                      H2
                    </el-button>
                    <el-button size="small" @click="insertMarkdown('### ', '', '三级标题')" title="三级标题">
                      H3
                    </el-button>
                  </el-button-group>
                </div>
                <div class="toolbar-group">
                  <el-button-group>
                    <el-button size="small" @click="insertMarkdown('- ', '', '无序列表')" title="无序列表">
                      •
                    </el-button>
                    <el-button size="small" @click="insertMarkdown('1. ', '', '有序列表')" title="有序列表">
                      1.
                    </el-button>
                    <el-button size="small" @click="insertMarkdown('> ', '', '引用')" title="引用">
                      "
                    </el-button>
                  </el-button-group>
                </div>
                <div class="toolbar-group">
                  <el-button size="small" @click="insertCodeBlock" title="代码块">
                    { }
                  </el-button>
                  <el-button size="small" @click="insertLink" title="插入链接">
                    链接
                  </el-button>
                  <el-upload class="upload-btn" action="/api/files/upload?category=WIKI" :show-file-list="false"
                    :before-upload="beforeUpload" :on-success="handleUploadSuccess" :on-error="handleUploadError">
                    <el-button size="small" :loading="uploadingImage">
                      {{ uploadingImage ? '上传中...' : '图片' }}
                    </el-button>
                  </el-upload>
                </div>
              </div>
              <div class="editor-wrapper">
                <el-input ref="contentInputRef" v-model="form.content" type="textarea" :rows="18" class="markdown-input"
                  placeholder="支持 Markdown 语法，使用工具栏快捷按钮或直接输入 Markdown 语法"
                  @keydown.ctrl.b.prevent="insertMarkdown('**', '**', '加粗')"
                  @keydown.ctrl.i.prevent="insertMarkdown('*', '*', '斜体')" />
                <div class="editor-stats">
                  <span>字数: {{ contentStats.words }}</span>
                  <span>字符: {{ contentStats.chars }}</span>
                  <span>行数: {{ contentStats.lines }}</span>
                </div>
              </div>
              <div class="uploaded-images" v-if="uploadedImages.length">
                <p class="images-title">已上传图片</p>
                <div class="images-list">
                  <div v-for="item in uploadedImages" :key="item.uid" class="image-thumb">
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
                <el-button text size="small" @click="togglePreviewFullscreen" v-if="tocItems.length > 0">
                  {{ showToc ? '隐藏目录' : '显示目录' }}
                </el-button>
              </div>
              <div class="preview-wrapper">
                <div class="preview-toc" v-if="showToc && tocItems.length > 0">
                  <h4>目录</h4>
                  <ul>
                    <li v-for="(item, index) in tocItems" :key="index" :class="`toc-level-${item.level}`"
                      @click="scrollToHeading(item.text)">
                      {{ item.text }}
                    </li>
                  </ul>
                </div>
                <div class="preview-content" ref="previewContentRef" v-html="previewHtml"></div>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="编辑说明" v-if="isEdit">
          <el-input v-model="form.editSummary" type="textarea" :rows="2" maxlength="200" show-word-limit
            placeholder="简要说明本次编辑的变更内容（可选）" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, computed, ref, watch, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createWikiPage, fetchWikiPage, updateWikiPage } from '@/api/wiki'
import { deleteFile } from '@/api/file'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, CircleCheck } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const uploadingImage = ref(false)
const uploadedImages = ref([])
const contentInputRef = ref(null)
const previewContentRef = ref(null)
const showToc = ref(true)
const autoSaveStatus = ref('') // '', 'saving', 'saved'
const hasUnsavedChanges = ref(false)
const originalContent = ref('')

const form = reactive({
  title: '',
  summary: '',
  content: '',
  editSummary: ''
})

let syncTimer = null
let autoSaveTimer = null
let lastSavedContent = ''

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

const tocItems = computed(() => {
  if (!form.content) return []
  return form.content.split('\n')
    .filter(line => /^#{1,6}\s+/.test(line))
    .map(line => {
      const level = line.match(/^#+/)[0].length
      return {
        level,
        text: line.replace(/^#+/, '').trim()
      }
    })
})

const contentStats = computed(() => {
  const content = form.content || ''
  const words = content.trim() ? content.trim().split(/\s+/).length : 0
  const chars = content.length
  const lines = content.split('\n').length
  return { words, chars, lines }
})

const isEdit = computed(() => !!route.params.id)

// Markdown 工具栏功能
const insertMarkdown = (before, after, placeholder = '') => {
  if (!contentInputRef.value) return
  const textarea = contentInputRef.value.$el.querySelector('textarea')
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = form.content.substring(start, end)
  const replacement = selectedText
    ? `${before}${selectedText}${after}`
    : `${before}${placeholder}${after}`

  form.content = form.content.substring(0, start) + replacement + form.content.substring(end)

  nextTick(() => {
    const newPos = start + replacement.length
    textarea.setSelectionRange(newPos, newPos)
    textarea.focus()
  })
}

const insertCodeBlock = () => {
  if (!contentInputRef.value) return
  const textarea = contentInputRef.value.$el.querySelector('textarea')
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = form.content.substring(start, end)
  const replacement = selectedText
    ? `\`\`\`\n${selectedText}\n\`\`\``
    : `\`\`\`\n// 代码内容\n\`\`\``

  form.content = form.content.substring(0, start) + replacement + form.content.substring(end)

  nextTick(() => {
    const newPos = selectedText
      ? start + replacement.length
      : start + replacement.length - 10
    textarea.setSelectionRange(newPos, newPos)
    textarea.focus()
  })
}

const insertLink = () => {
  if (!contentInputRef.value) return
  const textarea = contentInputRef.value.$el.querySelector('textarea')
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = form.content.substring(start, end)
  const replacement = selectedText
    ? `[${selectedText}](https://)`
    : `[链接文本](https://)`

  form.content = form.content.substring(0, start) + replacement + form.content.substring(end)

  nextTick(() => {
    const newPos = selectedText
      ? start + replacement.length - 1
      : start + replacement.length - 9
    textarea.setSelectionRange(newPos, newPos)
    textarea.focus()
  })
}

const scrollToHeading = (text) => {
  if (!previewContentRef.value) return
  const headings = previewContentRef.value.querySelectorAll('h1, h2, h3, h4, h5, h6')
  for (const heading of headings) {
    if (heading.textContent.trim() === text) {
      heading.scrollIntoView({ behavior: 'smooth', block: 'center' })
      heading.style.backgroundColor = '#e6f7ff'
      setTimeout(() => {
        heading.style.backgroundColor = ''
      }, 1000)
      break
    }
  }
}

const togglePreviewFullscreen = () => {
  showToc.value = !showToc.value
}

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
    if (contentInputRef.value) {
      const textarea = contentInputRef.value.$el.querySelector('textarea')
      if (textarea) {
        const start = textarea.selectionStart
        const insertText = form.content ? `\n\n${snippet}\n\n` : snippet
        form.content = form.content.substring(0, start) + insertText + form.content.substring(start)
        nextTick(() => {
          textarea.setSelectionRange(start + insertText.length, start + insertText.length)
          textarea.focus()
        })
      } else {
        form.content = form.content ? `${form.content}\n\n${snippet}` : snippet
      }
    } else {
      form.content = form.content ? `${form.content}\n\n${snippet}` : snippet
    }
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

// 自动保存草稿到 localStorage
const autoSaveDraft = () => {
  if (!form.title && !form.content) return

  const draftKey = isEdit.value ? `wiki_draft_${route.params.id}` : 'wiki_draft_new'
  const draft = {
    title: form.title,
    summary: form.summary,
    content: form.content,
    editSummary: form.editSummary,
    savedAt: new Date().toISOString()
  }
  localStorage.setItem(draftKey, JSON.stringify(draft))
  autoSaveStatus.value = 'saved'
  setTimeout(() => {
    autoSaveStatus.value = ''
  }, 2000)
}

const loadDraft = () => {
  const draftKey = isEdit.value ? `wiki_draft_${route.params.id}` : 'wiki_draft_new'
  const draftStr = localStorage.getItem(draftKey)
  if (draftStr) {
    try {
      const draft = JSON.parse(draftStr)
      return draft
    } catch (e) {
      return null
    }
  }
  return null
}

const clearDraft = () => {
  const draftKey = isEdit.value ? `wiki_draft_${route.params.id}` : 'wiki_draft_new'
  localStorage.removeItem(draftKey)
}

const loadPage = async () => {
  if (!isEdit.value) {
    // 新建时尝试加载草稿
    const draft = loadDraft()
    if (draft) {
      try {
        await ElMessageBox.confirm(
          '检测到未保存的草稿，是否恢复？',
          '恢复草稿',
          {
            confirmButtonText: '恢复',
            cancelButtonText: '放弃',
            type: 'info'
          }
        )
        Object.assign(form, {
          title: draft.title || '',
          summary: draft.summary || '',
          content: draft.content || '',
          editSummary: draft.editSummary || ''
        })
        originalContent.value = form.content
        lastSavedContent = form.content
      } catch {
        clearDraft()
      }
    }
    return
  }

  try {
    const res = await fetchWikiPage(route.params.id)
    Object.assign(form, {
      title: res.data.title || '',
      summary: res.data.summary || '',
      content: res.data.content || '',
      editSummary: ''
    })
    originalContent.value = form.content
    lastSavedContent = form.content
  } catch (error) {
    ElMessage.error('加载词条失败')
  }
}

const handleSubmit = async () => {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('标题和正文不能为空')
    return
  }

  loading.value = true
  try {
    if (isEdit.value) {
      await updateWikiPage(route.params.id, {
        title: form.title,
        summary: form.summary,
        content: form.content,
        editSummary: form.editSummary
      })
      ElMessage.success('已提交修改，等待审核')
    } else {
      await createWikiPage({
        title: form.title,
        summary: form.summary,
        content: form.content,
        editSummary: form.editSummary
      })
      ElMessage.success('已提交，等待审核')
    }
    clearDraft()
    hasUnsavedChanges.value = false
    router.push('/wiki')
  } catch (error) {
    console.error('提交失败:', error)

    // 兼容两种错误形态：
    // 1) 响应拦截器 reject(res)（res 为 {code,message,data}）
    // 2) HTTP 500 等非 2xx（axios error，需要从 error.response.data 取后端 Result）
    const res = error?.response?.data ?? error
    // 注意：这里判断 res.code，而不是 HTTP status
    if (res?.code === 409 && res?.data?.existingWiki) {
      const existingWiki = res.data.existingWiki
      try {
        await ElMessageBox.confirm(
          `词条"${existingWiki.title}"已存在，是否跳转到该词条的编辑页面？`,
          '词条已存在',
          {
            confirmButtonText: '跳转',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        // 使用后端返回的 id 跳转到编辑页
        router.push(`/wiki/edit/${existingWiki.id}`)
      } catch {
        // 用户取消
      }
      return
    }

    ElMessage.error(res?.message || '提交失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const handleGoBack = async () => {
  if (hasUnsavedChanges.value) {
    try {
      await ElMessageBox.confirm(
        '您有未保存的更改，确定要离开吗？',
        '确认离开',
        {
          confirmButtonText: '离开',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      router.push('/wiki')
    } catch {
      // 用户取消
    }
  } else {
    router.push('/wiki')
  }
}

onMounted(() => {
  loadPage()

  // 监听内容变化，自动保存
  watch(
    () => [form.title, form.summary, form.content],
    () => {
      hasUnsavedChanges.value = form.content !== originalContent.value

      if (autoSaveTimer) {
        clearTimeout(autoSaveTimer)
      }

      autoSaveTimer = setTimeout(() => {
        if (hasUnsavedChanges.value) {
          autoSaveStatus.value = 'saving'
          autoSaveDraft()
        }
      }, 2000) // 2秒后自动保存
    },
    { deep: true }
  )

  // 监听内容变化，提取图片
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

  // 页面离开前提示
  window.addEventListener('beforeunload', (e) => {
    if (hasUnsavedChanges.value) {
      e.preventDefault()
      e.returnValue = '您有未保存的更改，确定要离开吗？'
    }
  })
})

onBeforeUnmount(() => {
  if (syncTimer) {
    clearTimeout(syncTimer)
  }
  if (autoSaveTimer) {
    clearTimeout(autoSaveTimer)
  }
  window.removeEventListener('beforeunload', () => { })
})
</script>

<style scoped>
.editor-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.status-text {
  font-size: 12px;
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
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.toolbar-group {
  display: flex;
  gap: 4px;
}

.toolbar .el-button {
  padding: 6px 12px;
}

.editor-wrapper {
  position: relative;
  flex: 1;
}

.markdown-input :deep(textarea) {
  font-family: 'Fira Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 14px;
  line-height: 1.6;
}

.editor-stats {
  position: absolute;
  bottom: 8px;
  right: 12px;
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  background: rgba(255, 255, 255, 0.9);
  padding: 4px 8px;
  border-radius: 4px;
}

.uploaded-images {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.images-title {
  font-size: 12px;
  color: #606266;
  margin: 0 0 8px;
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  margin-bottom: 12px;
  color: #606266;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.preview-wrapper {
  display: flex;
  gap: 16px;
  flex: 1;
  overflow: hidden;
}

.preview-toc {
  width: 200px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  max-height: 520px;
  overflow-y: auto;
}

.preview-toc h4 {
  margin: 0 0 12px;
  font-size: 14px;
  color: #303133;
}

.preview-toc ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.preview-toc li {
  padding: 6px 0;
  cursor: pointer;
  color: #606266;
  font-size: 13px;
  transition: color 0.2s;
}

.preview-toc li:hover {
  color: #409eff;
}

.preview-toc .toc-level-1 {
  font-weight: 600;
  padding-left: 0;
}

.preview-toc .toc-level-2 {
  padding-left: 12px;
}

.preview-toc .toc-level-3 {
  padding-left: 24px;
  font-size: 12px;
}

.preview-content {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  overflow: auto;
  max-height: 520px;
  line-height: 1.8;
}

.preview-content :deep(h1),
.preview-content :deep(h2),
.preview-content :deep(h3),
.preview-content :deep(h4),
.preview-content :deep(h5),
.preview-content :deep(h6) {
  margin: 24px 0 12px;
  font-weight: 600;
  scroll-margin-top: 20px;
}

.preview-content :deep(h1) {
  font-size: 28px;
  border-bottom: 2px solid #ebeef5;
  padding-bottom: 8px;
}

.preview-content :deep(h2) {
  font-size: 24px;
}

.preview-content :deep(h3) {
  font-size: 20px;
}

.preview-content :deep(p) {
  margin: 12px 0;
}

.preview-content :deep(img) {
  max-width: 100%;
  border-radius: 12px;
  margin: 16px 0;
}

.preview-content :deep(code) {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 0.9em;
}

.preview-content :deep(pre) {
  background: #f5f5f5;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 16px 0;
}

.preview-content :deep(pre code) {
  background: none;
  padding: 0;
}

.preview-content :deep(blockquote) {
  border-left: 4px solid #409eff;
  padding-left: 16px;
  margin: 16px 0;
  color: #606266;
  font-style: italic;
}

.preview-content :deep(ul),
.preview-content :deep(ol) {
  margin: 12px 0;
  padding-left: 24px;
}

.preview-content :deep(li) {
  margin: 6px 0;
}

.preview-content :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.preview-content :deep(a:hover) {
  text-decoration: underline;
}

.preview-content :deep(.empty) {
  color: #909399;
  text-align: center;
  padding: 40px 0;
}

@media (max-width: 1200px) {
  .editor-grid {
    grid-template-columns: 1fr;
  }

  .preview-wrapper {
    flex-direction: column;
  }

  .preview-toc {
    width: 100%;
    max-height: 200px;
  }
}
</style>
