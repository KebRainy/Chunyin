<template>
  <el-dialog
    v-model="visible"
    width="540px"
    :close-on-click-modal="false"
    :show-close="false"
    :before-close="handleClose"
  >
    <template #header>
      <div class="modal-header">
        <h3>分享此刻</h3>
        <el-button text @click="handleClose">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
    </template>
    <ShareComposer ref="composerRef" mode="modal" @submitted="handlePosted" />
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import ShareComposer from '@/components/share/ShareComposer.vue'
import { Close } from '@element-plus/icons-vue'

const DRAFT_KEY = 'share_draft'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'posted'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const composerRef = ref(null)

const loadDraft = () => {
  const draft = localStorage.getItem(DRAFT_KEY)
  if (draft) {
    try {
      const parsed = JSON.parse(draft)
      composerRef.value?.fillForm(parsed)
      ElMessage.info('已为你恢复上次的草稿')
    } catch (error) {
      localStorage.removeItem(DRAFT_KEY)
    }
  }
}

const saveDraft = () => {
  const snapshot = composerRef.value?.getSnapshot?.()
  if (!snapshot) return
  localStorage.setItem(DRAFT_KEY, JSON.stringify(snapshot))
}

const clearDraft = () => {
  localStorage.removeItem(DRAFT_KEY)
}

const handlePosted = () => {
  clearDraft()
  visible.value = false
  emit('posted')
}

const handleClose = (done) => {
  const hasUnsaved = composerRef.value?.hasUnsaved?.value
  if (!hasUnsaved) {
    composerRef.value?.resetForm()
    clearDraft()
    if (typeof done === 'function') done()
    else visible.value = false
    return
  }
  ElMessageBox.confirm('要保存当前草稿以便稍后继续吗？', '保存草稿', {
    confirmButtonText: '保存草稿',
    cancelButtonText: '放弃',
    type: 'warning',
    distinguishCancelAndClose: true
  }).then(() => {
    saveDraft()
    composerRef.value?.resetForm()
    if (typeof done === 'function') done()
    else visible.value = false
  }).catch((action) => {
    if (action === 'cancel') {
      composerRef.value?.resetForm()
      clearDraft()
      if (typeof done === 'function') done()
      else visible.value = false
    }
  })
}

onMounted(() => {
  if (visible.value) {
    loadDraft()
  }
})

watch(() => visible.value, (val) => {
  if (val) {
    loadDraft()
  }
})
</script>

<style scoped>
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}
</style>
