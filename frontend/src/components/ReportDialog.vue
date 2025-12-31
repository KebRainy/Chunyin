<template>
  <el-dialog
    v-model="visible"
    title="举报内容"
    width="480px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="report-dialog">
      <div class="report-tip">
        <el-icon><WarningFilled /></el-icon>
        <span>请选择举报原因，我们会尽快处理您的举报</span>
      </div>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
      >
        <el-form-item label="举报原因" prop="reason">
          <el-radio-group v-model="form.reason" class="reason-group">
            <el-radio
              v-for="item in REPORT_REASONS"
              :key="item.value"
              :value="item.value"
              border
            >
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="详细说明（可选）" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请补充说明举报原因，帮助我们更快处理..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleSubmit">
          提交举报
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import { reportApi, REPORT_REASONS } from '@/api/report'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  contentType: {
    type: String,
    required: true
  },
  contentId: {
    type: [Number, String],
    required: true
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  reason: '',
  description: ''
})

const rules = {
  reason: [
    { required: true, message: '请选择举报原因', trigger: 'change' }
  ]
}

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    // 重置表单
    form.reason = ''
    form.description = ''
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const handleClose = () => {
  visible.value = false
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  
  submitting.value = true
  try {
    await reportApi.submitReport({
      contentType: props.contentType,
      contentId: Number(props.contentId),
      reason: form.reason,
      description: form.description || null
    })
    ElMessage.success('举报已提交，我们会尽快处理')
    emit('success')
    handleClose()
  } catch (error) {
    // 错误已在拦截器中处理
    console.error('提交举报失败:', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.report-dialog {
  padding: 0 10px;
}

.report-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fef0f0;
  border-radius: 8px;
  color: #f56c6c;
  font-size: 14px;
  margin-bottom: 20px;
}

.report-tip .el-icon {
  font-size: 18px;
}

.reason-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  width: 100%;
}

.reason-group :deep(.el-radio) {
  margin-right: 0;
  padding: 12px 16px;
  border-radius: 8px;
  height: auto;
}

.reason-group :deep(.el-radio.is-bordered.is-checked) {
  border-color: #f56c6c;
}

.reason-group :deep(.el-radio__input.is-checked .el-radio__inner) {
  background-color: #f56c6c;
  border-color: #f56c6c;
}

.reason-group :deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #f56c6c;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

