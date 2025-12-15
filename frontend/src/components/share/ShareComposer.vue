<template>
  <div :class="['share-composer', mode]">
    <div class="composer-main">
      <div class="composer-avatar">
        <el-avatar
          :src="userStore.userInfo?.avatarUrl"
          :size="48"
          :alt="`${userStore.userInfo?.username || '用户'}的头像`"
        />
      </div>
      <div class="composer-body">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="mode === 'modal' ? 6 : 3"
          maxlength="2000"
          show-word-limit
          resize="none"
          placeholder="分享此刻，聊聊你正在喝的、看到的、想到的一切..."
        />

        <div class="composer-fields">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            default-first-option
            collapse-tags
            :multiple-limit="5"
            placeholder="添加话题标签，如 上海、精酿、金酒"
          />
          <div class="location-field">
            <el-select
              v-model="selectedCity"
              filterable
              clearable
              :loading="regionsLoading"
              :placeholder="regionsLoading ? '地区加载中…' : '选择地点（可选）'"
              @change="handleCityChange"
            >
              <el-option-group
                v-for="province in provinces"
                :key="province.value"
                :label="province.label"
              >
                <el-option
                  v-for="city in province.cities"
                  :key="city.value"
                  :label="city.label"
                  :value="city.value"
                />
              </el-option-group>
            </el-select>
            <el-button
              class="locate-btn"
              text
              size="small"
              :loading="locating"
              @click="handleAutoLocate(true)"
            >
              {{ locating ? '定位中…' : '自动定位' }}
            </el-button>
          </div>
        </div>

        <div class="composer-upload">
          <el-upload
            v-if="mode === 'modal'"
            v-model:file-list="uploadFileList"
            list-type="picture-card"
            action="/api/files/upload?category=POST"
            multiple
            :on-success="handleUploadSuccess"
            :on-remove="handleRemove"
            :before-upload="beforeUpload"
            :on-error="handleUploadError"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div v-else class="inline-upload">
            <button
              type="button"
              class="inline-plus"
              aria-label="添加图片"
              title="添加图片"
              @click="triggerInlineUpload"
            >
              <el-icon><Plus /></el-icon>
            </button>
            <input
              ref="inlineInputRef"
              type="file"
              accept="image/*"
              multiple
              class="hidden-input"
              @change="handleInlineFiles"
            >
            <div class="inline-preview" v-if="thumbnails.length">
              <div v-for="(item, index) in thumbnails" :key="item.uid" class="thumb">
                <img :src="item.url" :alt="`已选择的图片${index + 1}`">
                <el-icon class="remove" @click="removeThumbnail(item.uid)"><Close /></el-icon>
              </div>
            </div>
          </div>
          <p class="hint">可上传 5 张以内图片，单张不超过 5MB</p>
        </div>

        <div class="composer-footer">
          <div class="meta">
            <span>已添加 {{ form.tags.length }} 个标签</span>
            <span v-if="form.imageIds.length">图片 {{ form.imageIds.length }} 张</span>
          </div>
          <el-button type="primary" size="small" :loading="submitting" @click="handleSubmit">
            立即发布
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useUserStore } from '@/store/modules/user'
import { circleApi } from '@/api/circle'
import { ElMessage } from 'element-plus'
import { Plus, Close } from '@element-plus/icons-vue'
import { useChinaRegions } from '@/composables/useChinaRegions'
import { useCityDetection } from '@/composables/useCityDetection'

const props = defineProps({
  mode: {
    type: String,
    default: 'inline' // inline | modal
  }
})

const emit = defineEmits(['submitted'])

const userStore = useUserStore()
const submitting = ref(false)
const uploadFileList = ref([])
const thumbnails = ref([])
const inlineInputRef = ref(null)

const form = reactive({
  content: '',
  tags: [],
  location: '',
  imageIds: []
})

const selectedCity = ref('')
const { provinces, loading: regionsLoading, ensureLoaded, findCityByName } = useChinaRegions()
const { detecting: locating, detectCity } = useCityDetection()

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

const handleUploadSuccess = (response, file, fileList) => {
  if (response.code === 200 && response.data?.id) {
    form.imageIds.push(response.data.id)
    uploadFileList.value = fileList
  } else {
    ElMessage.error('上传失败，请重试')
  }
}

const handleRemove = (file, fileList) => {
  const uid = file.response?.data?.id
  if (uid) {
    form.imageIds = form.imageIds.filter(id => id !== uid)
  }
  uploadFileList.value = fileList
}

const handleUploadError = () => {
  ElMessage.error('图片上传失败')
}

const triggerInlineUpload = () => {
  inlineInputRef.value?.click()
}

const handleInlineFiles = async (event) => {
  const files = Array.from(event.target.files || [])
  for (const file of files) {
    if (!beforeUpload(file)) continue
    const formData = new FormData()
    formData.append('file', file)
    try {
      const result = await fetch('/api/files/upload?category=POST', {
        method: 'POST',
        body: formData,
        credentials: 'include'
      }).then(res => res.json())
      if (result.code === 200 && result.data?.id) {
        form.imageIds.push(result.data.id)
        thumbnails.value.push({
          uid: `${Date.now()}-${Math.random()}`,
          url: result.data.url
        })
      } else {
        ElMessage.error(result.message || '上传失败')
      }
    } catch (error) {
      ElMessage.error('上传失败')
    }
  }
  if (inlineInputRef.value) {
    inlineInputRef.value.value = ''
  }
}

const removeThumbnail = (uid) => {
  const index = thumbnails.value.findIndex(item => item.uid === uid)
  if (index > -1) {
    thumbnails.value.splice(index, 1)
    form.imageIds.splice(index, 1)
  }
}

const applyCitySelection = (city) => {
  selectedCity.value = city || ''
  form.location = city || ''
}

const syncLocationWithOptions = async (locationValue) => {
  if (!locationValue) {
    applyCitySelection('')
    return
  }
  await ensureLoaded()
  const match = findCityByName(locationValue)
  applyCitySelection(match?.city || locationValue)
}

const handleCityChange = (value) => {
  form.location = value || ''
}

const handleAutoLocate = async (showToast = false) => {
  await ensureLoaded()
  const detected = await detectCity()
  if (!detected) {
    if (showToast) {
      ElMessage.warning('无法自动定位，请手动选择')
    }
    return
  }
  const match = findCityByName(detected)
  if (match?.city) {
    applyCitySelection(match.city)
    if (showToast) {
      ElMessage.success(`已定位到 ${match.city}`)
    }
    return
  }
  if (showToast) {
    ElMessage.warning('未找到匹配城市，请手动选择')
  }
}

const handleSubmit = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('请输入分享内容')
    return
  }
  submitting.value = true
  try {
    await circleApi.createPost({
      content: form.content,
      location: form.location,
      imageIds: form.imageIds,
      tags: form.tags
    })
    ElMessage.success('发布成功')
    emit('submitted')
    resetForm()
  } catch (error) {
    ElMessage.error(error.message || '发布失败')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  form.content = ''
  form.tags = []
  form.location = ''
  form.imageIds = []
  uploadFileList.value = []
  thumbnails.value = []
  selectedCity.value = ''
}

const fillForm = (data = {}) => {
  form.content = data.content || ''
  form.tags = data.tags ? [...data.tags] : []
  form.location = data.location || ''
  form.imageIds = data.imageIds ? [...data.imageIds] : []
  syncLocationWithOptions(form.location)
}

const hasUnsaved = computed(() => {
  return !!(form.content.trim() || form.tags.length || form.location || form.imageIds.length)
})

const getSnapshot = () => ({
  content: form.content,
  tags: form.tags,
  location: form.location,
  imageIds: form.imageIds
})

onMounted(async () => {
  await ensureLoaded()
  if (form.location) {
    await syncLocationWithOptions(form.location)
  } else {
    await handleAutoLocate(false)
  }
})

defineExpose({
  resetForm,
  fillForm,
  hasUnsaved,
  getSnapshot
})
</script>

<style scoped>
.share-composer {
  width: 100%;
}

.composer-main {
  display: flex;
  gap: 16px;
}

.composer-avatar {
  flex-shrink: 0;
}

.composer-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.composer-fields {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.location-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.location-field :deep(.el-select) {
  flex: 1;
}

.locate-btn {
  padding: 0 8px;
}

.composer-upload {
  border: 1px solid #eceff5;
  border-radius: 12px;
  padding: 12px 16px;
  background: #fff;
}

.inline-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.inline-plus {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid #d8dee9;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #fff;
  color: #1f2d3d;
  transition: border-color 0.2s, color 0.2s;
  padding: 0;
  font-size: 16px;
}

.inline-plus:hover {
  border-color: #1f2d3d;
  color: #1f2d3d;
}


.hidden-input {
  display: none;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
}

.inline-preview {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.thumb {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb .remove {
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  border-radius: 50%;
  cursor: pointer;
  font-size: 12px;
  padding: 2px;
}

.hint {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}

.composer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.composer-footer .meta {
  color: #909399;
  font-size: 12px;
  display: flex;
  gap: 12px;
}
</style>
