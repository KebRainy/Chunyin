<template>
  <div class="create-activity">
    <div class="page-header">
      <h2>发起活动</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="活动时间" prop="activityTime">
          <el-date-picker
            v-model="form.activityTime"
            type="datetime"
            placeholder="选择活动时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled-date="disabledDate"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="酒类标签" prop="alcoholIds">
          <el-select
            v-model="form.alcoholIds"
            placeholder="请选择酒类标签（最多2个）"
            filterable
            multiple
            :max-collapse-tags="2"
            style="width: 100%"
            @change="handleAlcoholChange"
          >
            <el-option
              v-for="alcohol in alcoholList"
              :key="alcohol.id"
              :label="alcohol.name"
              :value="alcohol.id"
            />
          </el-select>
          <div style="color: #909399; font-size: 12px; margin-top: 5px;">
            最多可选择2个酒类标签
          </div>
        </el-form-item>

        <el-form-item label="酒吧选择" prop="barId">
          <!-- 如果已选择酒吧，只显示选定的酒吧 -->
          <div v-if="form.barId && selectedBar" class="selected-bar-section">
            <el-card shadow="hover" class="selected-bar-card">
              <div class="bar-option">
                <div class="bar-option-header">
                  <span class="bar-name">{{ selectedBar.name }}</span>
                  <el-rate v-model="selectedBar.avgRating" disabled show-score size="small" />
                </div>
                <div class="bar-option-info">
                  <span class="bar-address">
                    <el-icon><LocationFilled /></el-icon>
                    {{ selectedBar.address }}
                  </span>
                  <span v-if="selectedBar.distance" class="bar-distance">
                    距离: {{ selectedBar.distance.toFixed(2) }} 公里
                  </span>
                </div>
              </div>
            </el-card>
            <el-button
              type="primary"
              style="margin-top: 10px"
              @click="handleReselectBar"
            >
              重新选择酒吧
            </el-button>
          </div>

          <!-- 如果未选择酒吧，显示商家拥有的酒吧列表 -->
          <div v-else>
            <div v-loading="barsLoading" class="bar-select-section">
              <el-empty v-if="myBars.length === 0 && !barsLoading" description="您还没有酒吧，请先注册酒吧" />
              <el-radio-group v-model="form.barId" class="bar-radio-group" @change="handleBarSelect">
                <el-radio
                  v-for="bar in myBars"
                  :key="bar.id"
                  :label="bar.id"
                  class="bar-radio"
                >
                  <div class="bar-option">
                    <div class="bar-option-header">
                      <span class="bar-name">{{ bar.name }}</span>
                      <el-rate v-model="bar.avgRating" disabled show-score size="small" />
                    </div>
                    <div class="bar-option-info">
                      <span class="bar-address">
                        <el-icon><LocationFilled /></el-icon>
                        {{ bar.address }}
                      </span>
                      <span v-if="bar.mainBeverages" class="bar-beverages" style="color: #909399; font-size: 12px;">
                        {{ bar.mainBeverages }}
                      </span>
                    </div>
                  </div>
                </el-radio>
              </el-radio-group>
            </div>
            <el-button
              type="text"
              style="margin-top: 10px"
              @click="form.barId = null; selectedBar = null"
            >
              不选择酒吧
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="参与人数上限" prop="maxParticipants">
          <el-input-number
            v-model="form.maxParticipants"
            :min="2"
            :max="50"
            placeholder="请输入参与人数上限"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="4"
            placeholder="请输入活动备注（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            提交审核
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { LocationFilled } from '@element-plus/icons-vue'
import { createActivity } from '@/api/activity'
import { getAlcoholList } from '@/api/alcohol'
import { getMyBars } from '@/api/bar'

const router = useRouter()

// 获取用户位置
const getUserLocation = () => {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('浏览器不支持地理位置API'))
      return
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude
        })
      },
      (error) => {
        reject(error)
      }
    )
  })
}

const formRef = ref(null)
const submitting = ref(false)
const barsLoading = ref(false)

const form = ref({
  activityTime: null,
  alcoholIds: [],
  barId: null,
  maxParticipants: 2,
  remark: ''
})

const validateAlcoholIds = (rule, value, callback) => {
  if (!value || value.length === 0) {
    callback(new Error('请至少选择一个酒类标签'))
  } else if (value.length > 2) {
    callback(new Error('最多只能选择2个酒类标签'))
  } else {
    callback()
  }
}

const rules = {
  activityTime: [
    { required: true, message: '请选择活动时间', trigger: 'change' }
  ],
  alcoholIds: [
    { required: true, validator: validateAlcoholIds, trigger: 'change' }
  ],
  maxParticipants: [
    { required: true, message: '请输入参与人数上限', trigger: 'blur' },
    { type: 'number', min: 2, max: 50, message: '参与人数上限应在2-50之间', trigger: 'blur' }
  ]
}

const alcoholList = ref([])
const myBars = ref([])
const selectedBar = ref(null)

// 禁用过去的日期
const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7 // 禁用今天之前的日期
}

// 加载酒类标签列表
const loadAlcoholList = async () => {
  try {
    const res = await getAlcoholList()
    if (res.code === 200) {
      alcoholList.value = res.data || []
    }
  } catch (error) {
    ElMessage.error('加载酒类标签列表失败')
  }
}

// 酒类标签变化时重新加载商家酒吧（用于排序）
const handleAlcoholChange = async () => {
  // 限制最多选择2个
  if (form.value.alcoholIds && form.value.alcoholIds.length > 2) {
    ElMessage.warning('最多只能选择2个酒类标签')
    form.value.alcoholIds = form.value.alcoholIds.slice(0, 2)
    return
  }

  // 重新加载商家酒吧，根据标签排序
  await loadMyBars()
}

// 加载商家拥有的酒吧
const loadMyBars = async () => {
  barsLoading.value = true
  try {
    // 只有当alcoholIds存在且不为空时才传递
    const alcoholIds = (form.value.alcoholIds && form.value.alcoholIds.length > 0) 
      ? form.value.alcoholIds 
      : null
    const res = await getMyBars(alcoholIds)
    if (res.code === 200) {
      myBars.value = res.data || []
      // 如果已经选择了酒吧，更新selectedBar
      if (form.value.barId) {
        const bar = myBars.value.find(b => b.id === form.value.barId)
        if (bar) {
          selectedBar.value = { ...bar }
        }
      }
    }
  } catch (error) {
    console.error('加载商家酒吧失败:', error)
    const errorMessage = error.response?.data?.message || error.message || '加载商家酒吧失败'
    ElMessage.error(errorMessage)
  } finally {
    barsLoading.value = false
  }
}

// 酒吧选择处理
const handleBarSelect = (barId) => {
  if (!barId) {
    selectedBar.value = null
    return
  }
  // 从商家酒吧列表中找到选中的酒吧
  const bar = myBars.value.find(b => b.id === barId)
  if (bar) {
    selectedBar.value = { ...bar }
  }
}

// 重新选择酒吧
const handleReselectBar = () => {
  form.value.barId = null
  selectedBar.value = null
  // 重新加载商家酒吧
  loadMyBars()
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    // 确保alcoholIds有值
    if (!form.value.alcoholIds || form.value.alcoholIds.length === 0) {
      ElMessage.error('请至少选择一个酒类标签')
      return
    }

    submitting.value = true
    try {
      const res = await createActivity({
        activityTime: form.value.activityTime,
        alcoholIds: form.value.alcoholIds,
        barId: form.value.barId || null,
        maxParticipants: form.value.maxParticipants,
        remark: form.value.remark || null
      })

      if (res.code === 200) {
        ElMessage.success('活动创建成功，等待管理员审核')
        router.push('/seller/activity')
      }
    } catch (error) {
      console.error('创建活动失败:', error)
      const errorMessage = error.response?.data?.message || error.message || '创建活动失败'
      ElMessage.error(errorMessage)
    } finally {
      submitting.value = false
    }
  })
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  form.value = {
    activityTime: null,
    alcoholIds: [],
    barId: null,
    maxParticipants: 2,
    remark: ''
  }
  myBars.value = []
  selectedBar.value = null
}

onMounted(() => {
  loadAlcoholList()
  loadMyBars()
})
</script>

<style scoped>
.create-activity {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.bar-select-section {
  max-height: 1500px;
  overflow-y: auto;
  padding: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.bar-radio-group {
  width: 100%;
}

.bar-radio {
  display: block;
  margin-bottom: 15px;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  transition: all 0.3s;
}

.bar-radio:hover {
  border-color: #409eff;
  background-color: #f5f7fa;
}

.bar-option {
  width: 100%;
}

.bar-option-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.bar-name {
  font-weight: bold;
  font-size: 16px;
}

.bar-option-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #666;
  font-size: 14px;
}

.bar-address {
  display: flex;
  align-items: center;
  gap: 5px;
}

.bar-distance {
  color: #409eff;
}

.bar-search-section {
  margin-top: 10px;
}

.selected-bar-section {
  margin-top: 10px;
}

.selected-bar-card {
  margin-bottom: 10px;
}

.selected-bar-card .bar-option {
  padding: 10px;
}
</style>

