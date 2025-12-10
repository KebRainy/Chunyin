<template>
  <el-dialog
    v-model="visible"
    title="分享此刻"
    width="500px"
    @close="resetForm"
  >
    <div class="share-modal-content">
      <!-- 分享内容输入框 -->
      <el-input
        v-model="form.content"
        type="textarea"
        rows="6"
        placeholder="分享你的所思所想..."
        maxlength="2000"
        show-word-limit
      />

      <!-- 位置选择 -->
      <div class="form-group">
        <label>位置</label>
        <el-input
          v-model="form.location"
          placeholder="添加位置（可选）"
          clearable
        />
      </div>

      <!-- 图片上传 -->
      <div class="form-group">
        <label>图片</label>
        <div class="image-upload-area">
          <el-upload
            v-model:file-list="uploadFileList"
            action="/api/files/upload"
            list-type="picture-card"
            multiple
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            accept="image/*"
          >
            <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
            <template #tip>
              <div class="el-upload__tip">
                单个图片不超过 5MB
              </div>
            </template>
          </el-upload>
        </div>
      </div>

      <!-- 已上传图片ID展示 -->
      <div v-if="form.imageIds.length > 0" class="image-ids">
        已上传 {{ form.imageIds.length }} 张图片
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        分享
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { circleApi } from '@/api/circle'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'posted'])

const userStore = useUserStore()
const loading = ref(false)
const uploadFileList = ref([])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const form = ref({
  content: '',
  location: '',
  imageIds: []
})

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleUploadSuccess = (response) => {
  if (response.code === 0 && response.data && response.data.id) {
    form.value.imageIds.push(response.data.id)
  }
}

const handleUploadError = () => {
  ElMessage.error('图片上传失败')
}

const handleSubmit = async () => {
  if (!form.value.content.trim()) {
    ElMessage.warning('请输入分享内容')
    return
  }

  loading.value = true
  try {
    await circleApi.createPost({
      content: form.value.content,
      location: form.value.location,
      imageIds: form.value.imageIds
    })
    ElMessage.success('分享成功')
    visible.value = false
    emit('posted')
    resetForm()
  } catch (error) {
    ElMessage.error(error.message || '分享失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.value = {
    content: '',
    location: '',
    imageIds: []
  }
  uploadFileList.value = []
}
</script>

<style scoped>
.share-modal-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-weight: 500;
  font-size: 14px;
  color: #606266;
}

.image-upload-area {
  padding: 8px 0;
}

.image-ids {
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
}

:deep(.el-textarea__inner) {
  font-family: inherit;
}

:deep(.el-upload-dragger) {
  border-radius: 4px;
}
</style>
