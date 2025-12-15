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
import { computed, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import ShareComposer from '@/components/share/ShareComposer.vue'
import { Close } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'posted'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const composerRef = ref(null)

const closeModal = (done) => {
  composerRef.value?.resetForm()
  if (typeof done === 'function') done()
  else visible.value = false
}

const handlePosted = () => {
  visible.value = false
  emit('posted')
}

const handleClose = (done) => {
  const hasUnsaved = composerRef.value?.hasUnsaved?.value
  if (!hasUnsaved) {
    closeModal(done)
    return
  }
  ElMessageBox.confirm('草稿将不会保存，确定要关闭吗？', '提示', {
    confirmButtonText: '仍要关闭',
    cancelButtonText: '继续编辑',
    type: 'warning',
    distinguishCancelAndClose: true
  }).then(() => {
    closeModal(done)
  }).catch(() => {
    // keep editing
  })
}
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
