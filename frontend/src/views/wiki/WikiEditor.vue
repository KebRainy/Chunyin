<template>
  <div class="wiki-editor-page">
    <input ref="imageInputRef" class="hidden-file-input" type="file" accept="image/*" @change="handleImageFileChange" />

    <div class="wiki-editor-content">
      <div class="title-row">
        <el-input
          v-model="form.title"
          class="title-input"
          placeholder="请输入标题"
          maxlength="200"
          show-word-limit
        />
      </div>

      <div class="editor-divider"></div>

      <div class="body-row">
        <div ref="vditorContainerRef" class="wiki-vditor"></div>
      </div>

      <div class="meta-row">
        <el-card class="meta-card">
          <template #header>
            <div class="meta-header">摘要</div>
          </template>
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="简要概述（可选）"
          />
        </el-card>

        <el-card v-if="isEdit" class="meta-card">
          <template #header>
            <div class="meta-header">编辑说明</div>
          </template>
          <el-input
            v-model="form.editSummary"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="简要说明本次编辑的变更内容（可选）"
          />
        </el-card>
      </div>
    </div>

    <div class="wiki-editor-bottom-bar">
      <div class="bottom-bar-inner">
        <div class="bottom-left">
          <span>字数 {{ contentStats.words }}</span>
          <span>行数 {{ contentStats.lines }}</span>
          <span class="autosave" v-if="hasUnsavedChanges || autoSaveStatus">
            {{ autoSaveStatus === 'saving' ? '正在保存...' : autoSaveStatus === 'saved' ? '已自动保存' : '有未保存的更改' }}
          </span>
        </div>
        <div class="bottom-right">
          <el-button @click="handleGoBack">返回列表</el-button>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            {{ isEdit ? '提交更新' : '发布词条' }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, computed, ref, watch, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createWikiPage, fetchWikiPage, updateWikiPage } from '@/api/wiki'
import { ElMessage, ElMessageBox } from 'element-plus'
import Vditor from 'vditor'
import 'vditor/dist/index.css'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const imageInputRef = ref(null)
const autoSaveStatus = ref('') // '', 'saving', 'saved'
const hasUnsavedChanges = ref(false)
const originalContent = ref('')
const vditorContainerRef = ref(null)
const vditorInstance = ref(null)

const form = reactive({
  title: '',
  summary: '',
  content: '',
  editSummary: ''
})

let autoSaveTimer = null
let beforeUnloadHandler = null

const contentStats = computed(() => {
  const content = form.content || ''
  const words = content.replace(/\s/g, '').length
  const chars = content.length
  const lines = content.split('\n').length
  return { words, chars, lines }
})

const isEdit = computed(() => !!route.params.id)

watch(
  () => form.title,
  (title) => {
    const t = (title || '').trim()
    document.title = t ? t : (isEdit.value ? '编辑词条' : '新建词条')
  },
  { immediate: true }
)

const beforeUploadImage = (file) => {
  const isImage = file?.type?.startsWith('image/')
  const sizeOk = file && file.size / 1024 / 1024 < 5
  if (!isImage) ElMessage.error('只能上传图片')
  if (!sizeOk) ElMessage.error('图片需小于 5MB')
  return !!isImage && !!sizeOk
}

const handleImageFileChange = async (event) => {
  const file = event?.target?.files?.[0]
  if (!file) return
  if (!beforeUploadImage(file)) {
    if (imageInputRef.value) imageInputRef.value.value = ''
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  try {
    const result = await fetch('/api/files/upload?category=WIKI', {
      method: 'POST',
      body: formData,
      credentials: 'include'
    }).then((res) => res.json())

    if (result?.code === 200 && result?.data?.url) {
      const url = result.data.url
      const alt = (file.name || '图片').replace(/\.[^.]+$/, '')
      const snippet = `![${alt}](${url})\n`
      if (vditorInstance.value) {
        vditorInstance.value.insertValue(snippet)
        form.content = vditorInstance.value.getValue()
        vditorInstance.value.focus()
      } else {
        form.content = `${form.content || ''}\n${snippet}`
      }
      ElMessage.success('图片已插入')
    } else {
      ElMessage.error(result?.message || '上传失败')
    }
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    if (imageInputRef.value) imageInputRef.value.value = ''
  }
}

const initVditor = async () => {
  if (!vditorContainerRef.value || vditorInstance.value) return

  vditorInstance.value = new Vditor(vditorContainerRef.value, {
    mode: 'ir',
    height: 'auto',
    minHeight: 520,
    placeholder: '支持 Markdown：直接输入即可（即时渲染）',
    cache: { enable: false },
    toolbarConfig: { pin: true },
    toolbar: [
      'headings',
      'bold',
      'italic',
      'strike',
      'quote',
      'list',
      'ordered-list',
      'check',
      'code',
      'inline-code',
      'link',
      {
        name: 'insert-image',
        tip: '插入图片',
        icon:
          '<svg viewBox="0 0 24 24"><path d="M4 5a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5zm2 0v14h12V5H6zm2 10 2.5-3 2 2.5L16 11l2 4H6l2-2z" /></svg>',
        click: () => {
          imageInputRef.value?.click()
        }
      },
      'table',
      '|',
      'undo',
      'redo',
      '|',
      'preview',
      'fullscreen'
    ],
    after: () => {
      vditorInstance.value?.setValue(form.content || '')
    },
    input: (value) => {
      form.content = value
    }
  })

  await nextTick()
}

watch(
  () => form.content,
  (value) => {
    if (!vditorInstance.value) return
    const current = vditorInstance.value.getValue()
    if (value !== current) {
      vditorInstance.value.setValue(value || '')
    }
  }
)

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

onMounted(async () => {
  await loadPage()
  await initVditor()

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

  // 页面离开前提示
  beforeUnloadHandler = (e) => {
    if (hasUnsavedChanges.value) {
      e.preventDefault()
      e.returnValue = '您有未保存的更改，确定要离开吗？'
    }
  }
  window.addEventListener('beforeunload', beforeUnloadHandler)
})

onBeforeUnmount(() => {
  if (autoSaveTimer) {
    clearTimeout(autoSaveTimer)
  }
  if (beforeUnloadHandler) {
    window.removeEventListener('beforeunload', beforeUnloadHandler)
  }
  vditorInstance.value?.destroy()
})
</script>

<style scoped>
.wiki-editor-page {
  width: 100%;
  padding-bottom: 96px;
}

.wiki-editor-content {
  width: 100%;
  margin: 0 auto;
  max-width: 1400px;
}

.title-row {
  margin-top: 6px;
}

.title-input :deep(.el-input__wrapper) {
  box-shadow: none;
  border: none;
  background: transparent;
  padding: 0;
}

.title-input :deep(.el-input__inner) {
  font-size: 34px;
  font-weight: 700;
  line-height: 1.2;
  color: #111827;
}

.editor-divider {
  height: 1px;
  background: #ebeef5;
  margin: 14px 0 12px;
  width: 100%;
}

.hidden-file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.wiki-vditor :deep(.vditor) {
  border: none;
  border-radius: 0;
  overflow: visible;
  background: transparent;
}

.wiki-vditor :deep(.vditor--fullscreen) {
  z-index: 200;
}

.wiki-vditor :deep(.vditor-toolbar--pin) {
  top: 68px;
  z-index: 90;
}

.wiki-vditor :deep(.vditor-content) {
  min-height: 70vh;
  background: transparent;
}

.wiki-vditor :deep(.vditor-ir pre.vditor-reset) {
  height: auto;
  min-height: 70vh;
}

.meta-row {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.meta-header {
  font-weight: 600;
  color: #303133;
}

.wiki-editor-bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.92);
  border-top: 1px solid #ebeef5;
  -webkit-backdrop-filter: blur(10px);
  backdrop-filter: blur(10px);
  z-index: 120;
}

.bottom-bar-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.bottom-left {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  white-space: nowrap;
  color: #606266;
}

.bottom-left .autosave {
  color: #909399;
}

.bottom-right {
  gap: 10px;
  display: flex;
  justify-content: flex-end;
}
</style>
